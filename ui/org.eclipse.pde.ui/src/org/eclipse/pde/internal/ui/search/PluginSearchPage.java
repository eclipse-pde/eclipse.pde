/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.core.search.PluginSearchInput;
import org.eclipse.pde.internal.core.search.PluginSearchScope;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;



public class PluginSearchPage extends DialogPage implements ISearchPage {
	
	class QueryData {
		public String text;
		public boolean isCaseSensitive;
		public int searchElement;
		public int limit;
		public int externalScope;
		public int workspaceScope;
		public IWorkingSet[] workingSets;
		
		public boolean equals(Object obj) {
			if (obj instanceof QueryData) {
				if (((QueryData)obj).text.equals(text))
					return true;
			}
			return false;
		}
		
	}
	
	private static ArrayList previousQueries = new ArrayList();
	
	private Button caseSensitive;
	private ISearchPageContainer container;
	private Button[] externalScopeButtons = new Button[3];
	private boolean firstTime = true;
	private Button[] limitToButtons = new Button[3];
	private Combo patternCombo;
	private Button[] searchForButtons = new Button[3];

	public void createControl(Composite parent)  {
		Composite result = new Composite(parent,SWT.NONE);
		GridLayout layout = new GridLayout(1, true);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		result.setLayout(layout);
		result.setLayoutData(new GridData(GridData.FILL_HORIZONTAL|GridData.GRAB_HORIZONTAL));
		
		createPatternSection(result);
		createSettingsSection(result);
		
		hookListeners();	
		
		setControl(result);
		Dialog.applyDialogFont(result);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(result, IHelpContextIds.SEARCH_PAGE);
	}
		
	private void createGroup(
		Composite parent,
		Button[] buttons,
		String groupLabel,
		String[] buttonLabels,
		int defaultEnabled) {
		Group group = new Group(parent, SWT.NONE);
		group.setLayout(new GridLayout(1, true));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		group.setText(groupLabel);
		for (int i = 0; i < buttonLabels.length; i++) {
			buttons[i] = new Button(group, SWT.RADIO);
			buttons[i].setText(buttonLabels[i]);
			buttons[i].setSelection(i == defaultEnabled);
		}
	}
		
	private void createPatternSection(Composite parent) {
		Composite result = new Composite(parent, SWT.NONE);
		result.setLayout(new GridLayout(2, false));
		result.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label label = new Label(result, SWT.NONE);
		GridData data = new GridData();
		data.horizontalSpan = 2;
		label.setLayoutData(data);
		label.setText(PDEUIMessages.SearchPage_searchString);

		patternCombo = new Combo(result, SWT.SINGLE | SWT.BORDER);
		patternCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		caseSensitive = new Button(result, SWT.CHECK);
		caseSensitive.setText(PDEUIMessages.SearchPage_caseSensitive);
	}
	
	private void createSettingsSection(Composite parent) {
		Composite result = new Composite(parent, SWT.NONE);
		result.setLayout(new GridLayout(3, true));
		result.setLayoutData(new GridData(GridData.FILL_BOTH));

		createGroup(
			result,
			searchForButtons,
			PDEUIMessages.SearchPage_searchFor,
			new String[] {
				PDEUIMessages.SearchPage_plugin,
				PDEUIMessages.SearchPage_fragment,
				PDEUIMessages.SearchPage_extPt},
			2);
		createGroup(
			result,
			limitToButtons,
			PDEUIMessages.SearchPage_limitTo,
			new String[] {
				PDEUIMessages.SearchPage_declarations,
				PDEUIMessages.SearchPage_references,
				PDEUIMessages.SearchPage_allOccurrences},
			1);
		createGroup(
			result,
			externalScopeButtons,
			PDEUIMessages.SearchPage_externalScope,
			new String[] {
				PDEUIMessages.SearchPage_all,
				PDEUIMessages.SearchPage_enabledOnly,
				PDEUIMessages.SearchPage_none},
			1);
	}
	
	private int getExternalScope() {
		if (externalScopeButtons[0].getSelection())
			return PluginSearchScope.EXTERNAL_SCOPE_ALL;
		if (externalScopeButtons[1].getSelection())
			return PluginSearchScope.EXTERNAL_SCOPE_ENABLED;
		return PluginSearchScope.EXTERNAL_SCOPE_NONE;
	}
	
	private PluginSearchInput getInput() {
		PluginSearchScope scope =
			new PluginSearchScope(
				getWorkspaceScope(),
				getExternalScope(),
				getSelectedResources());

		PluginSearchInput input = new PluginSearchInput();
		int searchFor = getSearchFor();
		input.setSearchElement(searchFor);
		input.setSearchLimit(getLimitTo());
		input.setSearchScope(scope);
		String searchString = patternCombo.getText().trim();
		if (searchFor == PluginSearchInput.ELEMENT_EXTENSION_POINT
			&& searchString.indexOf('.') == -1) {
			searchString = "*." + searchString; //$NON-NLS-1$
		}
		input.setSearchString(searchString);
		input.setCaseSensitive(caseSensitive.getSelection());
		return input;
	}
	
	private int getLimitTo() {
		if (limitToButtons[0].getSelection())
			return PluginSearchInput.LIMIT_DECLARATIONS;
		if (limitToButtons[1].getSelection())
			return PluginSearchInput.LIMIT_REFERENCES;
		return PluginSearchInput.LIMIT_ALL;
	}
	
	private int getSearchFor() {
		if (searchForButtons[0].getSelection())
			return PluginSearchInput.ELEMENT_PLUGIN;
		if (searchForButtons[1].getSelection())
			return PluginSearchInput.ELEMENT_FRAGMENT;
		return PluginSearchInput.ELEMENT_EXTENSION_POINT;
	}
	
	private HashSet getSelectedResources() {
		HashSet result = new HashSet();
		int scope = container.getSelectedScope();
		if (scope == ISearchPageContainer.WORKSPACE_SCOPE)
			return null;
		if (scope == ISearchPageContainer.SELECTION_SCOPE || scope == ISearchPageContainer.SELECTED_PROJECTS_SCOPE) {
			if (container.getSelection() instanceof IStructuredSelection) {
				IStructuredSelection selection =
					(IStructuredSelection) container.getSelection();
				Iterator iter = selection.iterator();
				while (iter.hasNext()) {
					Object item = iter.next();
					if (item instanceof IResource)
						result.add(((IResource)item).getProject());
				}
			}
		} else if (scope == ISearchPageContainer.WORKING_SET_SCOPE) {
			IWorkingSet[] workingSets = container.getSelectedWorkingSets();
			if (workingSets != null) {
				for (int i = 0; i < workingSets.length; i++) {
					IAdaptable[] elements = workingSets[i].getElements();
					for (int j = 0; j < elements.length; j++) {
						IResource resource = (IResource)elements[j].getAdapter(IResource.class);
						if (resource != null)
							result.add(resource.getProject());
					}
				}
			}
		}
		return result;
	}
	
	private int getWorkspaceScope() {
		switch(container.getSelectedScope()) {
			case ISearchPageContainer.SELECTION_SCOPE:
				return PluginSearchScope.SCOPE_SELECTION;
			case ISearchPageContainer.WORKING_SET_SCOPE:
				return PluginSearchScope.SCOPE_WORKING_SETS;
			default:
				return PluginSearchScope.SCOPE_WORKSPACE;
		}
	}
		
	private void hookListeners() {
		searchForButtons[1].addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean selected = searchForButtons[1].getSelection();
				if (selected) {
					limitToButtons[0].setSelection(true);
					limitToButtons[1].setSelection(false);
					limitToButtons[2].setSelection(false);
				}
				limitToButtons[1].setEnabled(!selected);
				limitToButtons[2].setEnabled(!selected);
			} 
		});
		
		patternCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int index = previousQueries.size() - patternCombo.getSelectionIndex() - 1;
				QueryData data = (QueryData)previousQueries.get(index);
				resetPage(data);
				container.setPerformActionEnabled(patternCombo.getText().length() > 0);
			}
		});
		
		patternCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				container.setPerformActionEnabled(patternCombo.getText().trim().length() > 0);
			}
		});	
	}
	
	public boolean performAction() {
		saveQueryData();
		NewSearchUI.activateSearchResultView();
		NewSearchUI.runQueryInBackground(new PluginSearchQuery(getInput()));
		return true;
	}
	
	private void resetPage (QueryData data) {
		caseSensitive.setSelection(data.isCaseSensitive);
		
		searchForButtons[0].setSelection(data.searchElement == PluginSearchInput.ELEMENT_PLUGIN);
		searchForButtons[1].setSelection(data.searchElement == PluginSearchInput.ELEMENT_FRAGMENT);
		searchForButtons[2].setSelection(data.searchElement == PluginSearchInput.ELEMENT_EXTENSION_POINT);

		limitToButtons[0].setSelection(data.limit == PluginSearchInput.LIMIT_DECLARATIONS);
		limitToButtons[1].setSelection(data.limit == PluginSearchInput.LIMIT_REFERENCES);
		limitToButtons[1].setEnabled(!searchForButtons[1].getSelection());
		limitToButtons[2].setSelection(data.limit == PluginSearchInput.LIMIT_ALL);
		limitToButtons[2].setEnabled(!searchForButtons[1].getSelection());
		
		externalScopeButtons[0].setSelection(data.externalScope == PluginSearchScope.EXTERNAL_SCOPE_ALL);
		externalScopeButtons[1].setSelection(data.externalScope == PluginSearchScope.EXTERNAL_SCOPE_ENABLED);
		externalScopeButtons[2].setSelection(data.externalScope == PluginSearchScope.EXTERNAL_SCOPE_NONE);
		
		container.setSelectedScope(data.workspaceScope);
		if (data.workingSets != null)
			container.setSelectedWorkingSets(data.workingSets);		
	}
		
	private void saveQueryData() {
		QueryData data = new QueryData();
		data.text = patternCombo.getText();
		data.isCaseSensitive = caseSensitive.getSelection();
		data.searchElement = getSearchFor();
		data.limit = getLimitTo();
		data.externalScope = getExternalScope();
		data.workspaceScope = container.getSelectedScope();
		data.workingSets = container.getSelectedWorkingSets();
		
		if (previousQueries.contains(data))
			previousQueries.remove(data);
			
		previousQueries.add(data);			
		if (previousQueries.size() > 10)
			previousQueries.remove(0);
	}
	
	public void setContainer(ISearchPageContainer container) {
		this.container = container;
	}
	
	public void setVisible(boolean visible) {
		if (visible && patternCombo != null) {
			if (firstTime) {
				firstTime = false;
				String[] patterns = new String[previousQueries.size()];
				for (int i = previousQueries.size() - 1, j = 0;
					i >= 0;
					i--, j++) {
					patterns[j] = ((QueryData) previousQueries.get(i)).text;
				}
				patternCombo.setItems(patterns);
				container.setPerformActionEnabled(
					patternCombo.getText().length() > 0);
			}
			patternCombo.setFocus();
		}
		super.setVisible(visible);
	}

}
