package org.eclipse.pde.internal.ui.search;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.core.search.PluginSearchInput;
import org.eclipse.pde.internal.core.search.PluginSearchScope;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.search.ui.SearchUI;
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
	
	private static final String KEY_SEARCH_STRING = "SearchPage.searchString";
	private static final String KEY_CASE_SENSITIVE = "SearchPage.caseSensitive";
	private static final String KEY_SEARCH_FOR = "SearchPage.searchFor";
	private static final String KEY_LIMIT_TO = "SearchPage.limitTo";
	private static final String KEY_EXTERNAL_SCOPE = "SearchPage.externalScope";
	private static final String KEY_PLUGIN = "SearchPage.plugin";
	private static final String KEY_FRAGMENT = "SearchPage.fragment";
	private static final String KEY_EXT_PT = "SearchPage.extPt";
	private static final String KEY_DECLARATIONS = "SearchPage.declarations";
	private static final String KEY_REFERENCES = "SearchPage.references";
	private static final String KEY_ALL_OCCURRENCES = "SearchPage.allOccurrences";
	private static final String KEY_ALL = "SearchPage.all";
	private static final String KEY_ENABLED = "SearchPage.enabledOnly";
	private static final String KEY_NONE = "SearchPage.none";
	
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
		result.setLayout(new GridLayout(1,true));
		result.setLayoutData(new GridData(GridData.FILL_HORIZONTAL|GridData.GRAB_HORIZONTAL));
		
		createPatternSection(result);
		createSettingsSection(result);
		
		hookListeners();	
		
		setControl(result);
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
		Label label = new Label(parent, SWT.LEFT);
		label.setText(PDEPlugin.getResourceString(KEY_SEARCH_STRING));
		
		Composite result = new Composite(parent, SWT.NONE);
		result.setLayout(new GridLayout(2, false));
		result.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));


		patternCombo = new Combo(result, SWT.SINGLE | SWT.BORDER);
		patternCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));

		caseSensitive = new Button(result, SWT.CHECK);
		caseSensitive.setText(PDEPlugin.getResourceString(KEY_CASE_SENSITIVE));
	}
	
	private void createSettingsSection(Composite parent) {
		Composite result = new Composite(parent, SWT.NONE);
		result.setLayout(new GridLayout(3, true));
		result.setLayoutData(new GridData(GridData.FILL_BOTH));

		createGroup(
			result,
			searchForButtons,
			PDEPlugin.getResourceString(KEY_SEARCH_FOR),
			new String[] {
				PDEPlugin.getResourceString(KEY_PLUGIN),
				PDEPlugin.getResourceString(KEY_FRAGMENT),
				PDEPlugin.getResourceString(KEY_EXT_PT)},
			0);
		createGroup(
			result,
			limitToButtons,
			PDEPlugin.getResourceString(KEY_LIMIT_TO),
			new String[] {
				PDEPlugin.getResourceString(KEY_DECLARATIONS),
				PDEPlugin.getResourceString(KEY_REFERENCES),
				PDEPlugin.getResourceString(KEY_ALL_OCCURRENCES)},
			2);
		createGroup(
			result,
			externalScopeButtons,
			PDEPlugin.getResourceString(KEY_EXTERNAL_SCOPE),
			new String[] {
				PDEPlugin.getResourceString(KEY_ALL),
				PDEPlugin.getResourceString(KEY_ENABLED),
				PDEPlugin.getResourceString(KEY_NONE)},
			1);
	}
	
	private IFile findManifestFile(Object item) {
		if (item instanceof JavaProject)
			item = ((JavaProject)item).getProject();
			
		if (item instanceof IProject) {
			IFile file = ((IProject) item).getFile("plugin.xml");
			if (file.exists())
				return file;
			file = ((IProject) item).getFile("fragment.xml");
			if (file.exists())
				return file;
		} else if (item instanceof IFile) {
			IFile file = (IFile)item;
			if (file.getName().equals("plugin.xml")
				|| file.getName().equals("fragment.xml")) {
				return file;
			}
		}
		return null;
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
		input.setSearchElement(getSearchFor());
		input.setSearchLimit(getLimitTo());
		input.setSearchScope(scope);
		input.setSearchString(patternCombo.getText());
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
		if (scope == ISearchPageContainer.SELECTION_SCOPE) {
			if (container.getSelection() instanceof IStructuredSelection) {
				IStructuredSelection selection =
					(IStructuredSelection) container.getSelection();
				Iterator iter = selection.iterator();
				while (iter.hasNext()) {
					IFile file = findManifestFile(iter.next());
					if (file != null)
						result.add(file);
				}
			}
		} else if (scope == ISearchPageContainer.WORKING_SET_SCOPE) {
			IWorkingSet[] workingSets = container.getSelectedWorkingSets();
			for (int i = 0; i < workingSets.length; i++) {
				IAdaptable[] elements = workingSets[i].getElements();
				for (int j = 0; j < elements.length; j++) {
					IFile file = findManifestFile(elements[j]);
					if (file != null)
						result.add(file);
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
				container.setPerformActionEnabled(patternCombo.getText().length() > 0);
			}
		});	
	}
	
	public boolean performAction() {
		saveQueryData();
		try {
			SearchUI.activateSearchResultView();

			PluginSearchUIOperation op =
				new PluginSearchUIOperation(getInput(), new PluginSearchResultCollector());
			container.getRunnableContext().run(true, true, op);
		} catch (InvocationTargetException e) {
			return false;
		} catch (InterruptedException e) {
			return false;
		}
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
		}
		super.setVisible(visible);
	}

}
