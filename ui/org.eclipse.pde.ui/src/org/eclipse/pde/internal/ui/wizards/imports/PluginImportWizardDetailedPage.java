/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Created on May 31, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package org.eclipse.pde.internal.ui.wizards.imports;



import java.util.ArrayList;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.util.PatternConstructor;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public class PluginImportWizardDetailedPage extends BaseImportWizardSecondPage {

	
	class ContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object element) {
			return fModels;
		}
	}

	private Label fCountLabel;
	private TableViewer fAvailableListViewer;
	private Text fFilterText;
	
	public PluginImportWizardDetailedPage(String pageName, PluginImportWizardFirstPage firstPage) {
		super(pageName, firstPage);
		setTitle(PDEUIMessages.ImportWizard_DetailedPage_title); 
		setMessage(PDEUIMessages.ImportWizard_DetailedPage_desc); 
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.makeColumnsEqualWidth = false;
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 10;
		container.setLayout(layout);
				
		createScrollArea(container);
		createAvailableList(container).setLayoutData(new GridData(GridData.FILL_BOTH));
		createButtonArea(container);
		createImportList(container).setLayoutData(new GridData(GridData.FILL_BOTH));
		updateCount();
		createComputationsOption(container, 3);		
		addViewerListeners();
		
		initialize();
		setControl(container);
		Dialog.applyDialogFont(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.PLUGIN_IMPORT_SECOND_PAGE);
	}
	
	private void initialize(){
		setPageComplete(false);
	}
	
	private void addViewerListeners() {
		fAvailableListViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				handleAdd();
			}
		});
				
		fImportListViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				handleRemove();
			}
		});
		
		fFilterText.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				handleFilter();
			}
		});
		
	}

	private Composite createAvailableList(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		container.setLayout(layout);
		container.setLayoutData(new GridData());

		Label label = new Label(container, SWT.NONE);
		label.setText(PDEUIMessages.ImportWizard_DetailedPage_availableList); 

		Table table = new Table(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 225;
		table.setLayoutData(gd);

		fAvailableListViewer = new TableViewer(table);
		fAvailableListViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		fAvailableListViewer.setContentProvider(new ContentProvider());
		fAvailableListViewer.setInput(PDECore.getDefault().getExternalModelManager());
		fAvailableListViewer.setSorter(ListUtil.PLUGIN_SORTER);

		return container;
	}
	
	
	private Composite createButtonArea(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		comp.setLayout(layout);
		comp.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		
		Composite container = new Composite(comp, SWT.NONE);
		layout = new GridLayout();
		layout.marginWidth = 0;
		container.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_VERTICAL);
		gd.verticalIndent = 15;
		container.setLayoutData(gd);
		
		Button button = new Button(container, SWT.PUSH);
		button.setText(PDEUIMessages.ImportWizard_DetailedPage_existing); 
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleExistingProjects();
			}
		});
		SWTUtil.setButtonDimensionHint(button);

		button = new Button(container, SWT.PUSH);
		button.setText(PDEUIMessages.ImportWizard_DetailedPage_existingUnshared); 
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleExistingUnshared();
			}
		});
		SWTUtil.setButtonDimensionHint(button);
		
		button = new Button(container, SWT.PUSH);
		button.setText(PDEUIMessages.ImportWizard_DetailedPage_add); 
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAdd();
			}
		});
		SWTUtil.setButtonDimensionHint(button);
		
		button = new Button(container, SWT.PUSH);
		button.setText(PDEUIMessages.ImportWizard_DetailedPage_addAll); 
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAddAll();
			}
		});
		SWTUtil.setButtonDimensionHint(button);
		
		button = new Button(container, SWT.PUSH);
		button.setText(PDEUIMessages.ImportWizard_DetailedPage_remove); 
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemove();
			}
		});
		SWTUtil.setButtonDimensionHint(button);
		
		button = new Button(container, SWT.PUSH);
		button.setText(PDEUIMessages.ImportWizard_DetailedPage_removeAll); 
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemoveAll();
			}
		});
		SWTUtil.setButtonDimensionHint(button);
		
		button = new Button(container, SWT.PUSH);
		button.setText(PDEUIMessages.ImportWizard_DetailedPage_swap); 
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleSwap();
			}
		});
		SWTUtil.setButtonDimensionHint(button);
		
		button = new Button(container, SWT.PUSH);
		button.setText(PDEUIMessages.ImportWizard_DetailedPage_addRequired); 
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAddRequiredPlugins();
			}

		});
		SWTUtil.setButtonDimensionHint(button);
		
		fCountLabel = new Label(comp, SWT.NONE);
		fCountLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));		
		return container;
	}
	
	private Composite createScrollArea(Composite parent){
		Group container = new Group(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2,false);
		layout.marginWidth = layout.marginHeight = 6;
		container.setLayout(layout);
		
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan=3;
		container.setLayoutData(gd);
		container.setText(PDEUIMessages.ImportWizard_DetailedPage_locate); 
	
		Label filterLabel = new Label(container, SWT.NONE);
		filterLabel.setText(PDEUIMessages.ImportWizard_DetailedPage_search); 
		
		fFilterText = new Text(container, SWT.BORDER);
		fFilterText.setText(""); //$NON-NLS-1$
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fFilterText.setLayoutData(gd);
			
		return container;
	}
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible)
			fFilterText.setFocus();
		setPageComplete(visible && fImportListViewer.getTable().getItemCount() > 0);
		
	}
	protected void refreshPage() {
		fAvailableListViewer.refresh();
		fImportListViewer.getTable().removeAll();		
		pageChanged();
	}
	protected void pageChanged() {
		updateCount();
		handleFilter();
		setPageComplete(fImportListViewer.getTable().getItemCount() > 0);
	}
	private void updateCount() {
		fCountLabel.setText(
			NLS.bind(PDEUIMessages.ImportWizard_DetailedPage_count, (new String[] {
			new Integer(fImportListViewer.getTable().getItemCount()).toString(),
			new Integer(fModels.length).toString()})));
		fCountLabel.getParent().layout();
	}
	
	private void handleAdd() {
		IStructuredSelection ssel = (IStructuredSelection)fAvailableListViewer.getSelection();
		if (ssel.size() > 0) {
			Table table = fAvailableListViewer.getTable();
			int index = table.getSelectionIndices()[0];
			fAvailableListViewer.remove(ssel.toArray());
			fImportListViewer.add(ssel.toArray());
			table.setSelection(index < table.getItemCount() ? index : table.getItemCount() -1);
			pageChanged();
		}		
	}

	private void handleAddAll() {
		TableItem[] items = fAvailableListViewer.getTable().getItems();

		ArrayList data = new ArrayList();
		for (int i = 0; i < items.length; i++) {
			data.add(items[i].getData());
		}
		if (data.size() > 0) {
			fImportListViewer.add(data.toArray());
			fAvailableListViewer.remove(data.toArray());
			pageChanged();
		}
	}
	
	private void handleFilter() {
		if (fFilterText == null ||fFilterText.getText().trim().length() == 0)
			return;

		String text = fFilterText.getText().trim();
		if (!text.endsWith("*")) //$NON-NLS-1$
			text += "*"; //$NON-NLS-1$
		Pattern pattern = PatternConstructor.createPattern(text, true);
		TableItem[] tableItems = fAvailableListViewer.getTable().getItems();
		ArrayList results = new ArrayList();
		for (int i = 0; i<tableItems.length; i++){
			Object data = tableItems[i].getData();
			if (data instanceof IPluginModelBase){
				IPluginModelBase model = (IPluginModelBase)data;
				if (pattern.matcher(model.getPluginBase().getId()).matches())
					results.add(tableItems[i]);
			}
		}
		if (results.size()>0){
			TableItem[] selectionList = (TableItem[])results.toArray(new TableItem[results.size()]);
			fAvailableListViewer.getTable().setSelection(selectionList);
		} else {
			fAvailableListViewer.setSelection(null);
		}
	}
	
	private void handleRemove() {
		IStructuredSelection ssel = (IStructuredSelection)fImportListViewer.getSelection();
		if (ssel.size() > 0) {
			Table table = fImportListViewer.getTable();
			int index = table.getSelectionIndices()[0];
			fImportListViewer.remove(ssel.toArray());
			fAvailableListViewer.add(ssel.toArray());
			table.setSelection(index < table.getItemCount() ? index : table.getItemCount() -1);
			pageChanged();
		}		
	}
	
	private void handleRemoveAll() {
		handleRemoveAll(true);
	}
	
	private void handleRemoveAll(boolean refresh) {
		TableItem[] items = fImportListViewer.getTable().getItems();
		
		ArrayList data = new ArrayList();
		for (int i = 0; i < items.length; i++) {
			data.add(items[i].getData());
		}
		if (data.size() > 0) {
			fAvailableListViewer.add(data.toArray());
			fImportListViewer.remove(data.toArray());
			pageChanged();
		}		
	}
	
	private void handleSwap() {
		TableItem[] aItems = fAvailableListViewer.getTable().getItems();
		TableItem[] iItems = fImportListViewer.getTable().getItems();
		
		ArrayList data = new ArrayList();
		for (int i = 0; i < iItems.length; i++) {
			data.add(iItems[i].getData());
		}
		if (data.size() > 0) {
			fAvailableListViewer.add(data.toArray());
			fImportListViewer.remove(data.toArray());
		}
		
		data.clear();
		for (int i = 0; i < aItems.length; i++) {
			data.add(aItems[i].getData());
		}
		if (data.size() > 0) {
			fImportListViewer.add(data.toArray());
			fAvailableListViewer.remove(data.toArray());
		}
		pageChanged();		
	}
	
	private void handleExistingProjects() {
		handleRemoveAll(false);
		ArrayList result = new ArrayList();
		for (int i = 0; i < fModels.length; i++) {
			String id = fModels[i].getPluginBase().getId();
			IProject project =
				(IProject) PDEPlugin.getWorkspace().getRoot().findMember(id);
			if (project != null
				&& project.isOpen()
				&& WorkspaceModelManager.isPluginProject(project)) {
				result.add(fModels[i]);
			}
		}
		if (result.size() > 0) {
			fImportListViewer.add(result.toArray());
			fAvailableListViewer.remove(result.toArray());
		}
		pageChanged();		
	}
	
	private void handleExistingUnshared() {
		handleRemoveAll(false);
		ArrayList result = new ArrayList();
		for (int i = 0; i < fModels.length; i++) {
			String id = fModels[i].getPluginBase().getId();
			IProject project =
				(IProject) PDEPlugin.getWorkspace().getRoot().findMember(id);
			if (project != null
				&& project.isOpen()
				&& WorkspaceModelManager.isUnsharedPluginProject(project)) {
				result.add(fModels[i]);
			}
		}
		if (result.size() > 0) {
			fImportListViewer.add(result.toArray());
			fAvailableListViewer.remove(result.toArray());
		}
		pageChanged();		
	}
	
	private void handleAddRequiredPlugins() {
		TableItem[] items = fImportListViewer.getTable().getItems();
		if (items.length == 0)
			return;
		if (items.length == 1) {
			IPluginModelBase model = (IPluginModelBase) items[0].getData();
			if (model.getPluginBase().getId().equals("org.eclipse.core.boot")) { //$NON-NLS-1$
				return;
			}
		}
						
		ArrayList result = new ArrayList();
		for (int i = 0; i < items.length; i++) {
			addPluginAndDependencies((IPluginModelBase) items[i].getData(), result, fAddFragmentsButton.getSelection());
		}

		handleRemoveAll(false);
		fImportListViewer.add(result.toArray());
		fAvailableListViewer.remove(result.toArray());
		pageChanged();		
	}
	

}
