/*
 * Created on May 31, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package org.eclipse.pde.internal.ui.wizards.imports;



import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/**
 * @author Wassim Melhem
 */
public class PluginImportWizardDetailedPage extends BaseImportWizardSecondPage {

	
	class ContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object element) {
			return models;
		}
	}

	private Label countLabel;
	private TableViewer availableListViewer;
	
	public PluginImportWizardDetailedPage(String pageName, PluginImportWizardFirstPage firstPage) {
		super(pageName, firstPage);
		setTitle(PDEPlugin.getResourceString("ImportWizard.DetailedPage.title"));
		setMessage(PDEPlugin.getResourceString("ImportWizard.DetailedPage.desc"));
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 10;
		container.setLayout(layout);
		
		createAvailableList(container).setLayoutData(new GridData(GridData.FILL_BOTH));
		createButtonArea(container);
		createImportList(container).setLayoutData(new GridData(GridData.FILL_BOTH));
		updateCount();
		
		addViewerListeners();
		
		implicitButton = new Button(container, SWT.CHECK);
		implicitButton.setText(PDEPlugin.getResourceString("ImportWizard.SecondPage.implicit"));
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		implicitButton.setLayoutData(gd);
		if (getDialogSettings().get(SETTINGS_IMPLICIT) != null)
			implicitButton.setSelection(getDialogSettings().getBoolean(SETTINGS_IMPLICIT));
		else 
			implicitButton.setSelection(true);
			
		setPageComplete(false);
		setControl(container);
		Dialog.applyDialogFont(container);
		
	}
	
	private void addViewerListeners() {
		availableListViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				handleAdd();
			}
		});
				
		importListViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				handleRemove();
			}
		});
	}

	private Composite createAvailableList(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		Label label = new Label(container, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("ImportWizard.DetailedPage.availableList"));

		Table table = new Table(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 180;
		gd.heightHint = 310;
		table.setLayoutData(gd);

		availableListViewer = new TableViewer(table);
		availableListViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		availableListViewer.setContentProvider(new ContentProvider());
		availableListViewer.setInput(PDECore.getDefault().getExternalModelManager());
		availableListViewer.setSorter(ListUtil.PLUGIN_SORTER);

		return container;
	}
	
	
	private Composite createButtonArea(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout());
		comp.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		
		Composite container = new Composite(comp, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 30;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Button button = new Button(container, SWT.PUSH);
		button.setText(PDEPlugin.getResourceString("ImportWizard.DetailedPage.add"));
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAdd();
			}
		});
		SWTUtil.setButtonDimensionHint(button);
		
		button = new Button(container, SWT.PUSH);
		button.setText(PDEPlugin.getResourceString("ImportWizard.DetailedPage.addAll"));
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAddAll();
			}
		});
		SWTUtil.setButtonDimensionHint(button);
		
		button = new Button(container, SWT.PUSH);
		button.setText(PDEPlugin.getResourceString("ImportWizard.DetailedPage.remove"));
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemove();
			}
		});
		SWTUtil.setButtonDimensionHint(button);
		
		button = new Button(container, SWT.PUSH);
		button.setText(PDEPlugin.getResourceString("ImportWizard.DetailedPage.removeAll"));
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemoveAll();
			}
		});
		SWTUtil.setButtonDimensionHint(button);
		
		button = new Button(container, SWT.PUSH);
		button.setText(PDEPlugin.getResourceString("ImportWizard.DetailedPage.swap"));
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleSwap();
			}
		});
		SWTUtil.setButtonDimensionHint(button);
		
		button = new Button(container, SWT.PUSH);
		button.setText(PDEPlugin.getResourceString("ImportWizard.DetailedPage.existingUnshared"));
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleExistingUnshared();
			}
		});
		SWTUtil.setButtonDimensionHint(button);
		
		button = new Button(container, SWT.PUSH);
		button.setText(PDEPlugin.getResourceString("ImportWizard.DetailedPage.addRequired"));
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAddRequiredPlugins();
			}

		});
		SWTUtil.setButtonDimensionHint(button);
		
		countLabel = new Label(comp, SWT.NONE);
		countLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));		
		return container;
	}
	
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		setPageComplete(visible && importListViewer.getTable().getItemCount() > 0);
		
	}
	protected void refreshPage() {
		availableListViewer.refresh();
		importListViewer.getTable().removeAll();		
		super.refreshPage();
	}
	protected void pageChanged() {
		updateCount();
		setPageComplete(importListViewer.getTable().getItemCount() > 0);
	}
	private void updateCount() {
		countLabel.setText(
			PDEPlugin.getFormattedMessage(
				"ImportWizard.DetailedPage.count",
				new String[] {
					new Integer(importListViewer.getTable().getItemCount()).toString(),
					new Integer(models.length).toString()}));
		countLabel.getParent().layout();
	}
	
	private void handleAdd() {
		IStructuredSelection ssel = (IStructuredSelection)availableListViewer.getSelection();
		if (ssel.size() > 0) {
			availableListViewer.remove(ssel.toArray());
			importListViewer.add(ssel.toArray());
			pageChanged();
		}		
	}
	
	private void handleAddAll() {
		TableItem[] items = availableListViewer.getTable().getItems();

		ArrayList data = new ArrayList();
		for (int i = 0; i < items.length; i++) {
			data.add(items[i].getData());
		}
		if (data.size() > 0) {
			importListViewer.add(data.toArray());
			availableListViewer.remove(data.toArray());
			pageChanged();
		}
	}
	
	private void handleRemove() {
		IStructuredSelection ssel = (IStructuredSelection)importListViewer.getSelection();
		if (ssel.size() > 0) {
			importListViewer.remove(ssel.toArray());
			availableListViewer.add(ssel.toArray());
			pageChanged();
		}		
	}
	
	private void handleRemoveAll() {
		handleRemoveAll(true);
	}
	
	private void handleRemoveAll(boolean refresh) {
		TableItem[] items = importListViewer.getTable().getItems();
		
		ArrayList data = new ArrayList();
		for (int i = 0; i < items.length; i++) {
			data.add(items[i].getData());
		}
		if (data.size() > 0) {
			availableListViewer.add(data.toArray());
			importListViewer.remove(data.toArray());
			pageChanged();
		}		
	}
	
	private void handleSwap() {
		TableItem[] aItems = availableListViewer.getTable().getItems();
		TableItem[] iItems = importListViewer.getTable().getItems();
		
		ArrayList data = new ArrayList();
		for (int i = 0; i < iItems.length; i++) {
			data.add(iItems[i].getData());
		}
		if (data.size() > 0) {
			availableListViewer.add(data.toArray());
			importListViewer.remove(data.toArray());
		}
		
		data.clear();
		for (int i = 0; i < aItems.length; i++) {
			data.add(aItems[i].getData());
		}
		if (data.size() > 0) {
			importListViewer.add(data.toArray());
			availableListViewer.remove(data.toArray());
		}
		pageChanged();		
	}
	
	private void handleExistingUnshared() {
		handleRemoveAll(false);
		ArrayList result = new ArrayList();
		for (int i = 0; i < models.length; i++) {
			String id = models[i].getPluginBase().getId();
			IProject project =
				(IProject) PDEPlugin.getWorkspace().getRoot().findMember(id);
			if (project != null
				&& project.isOpen()
				&& WorkspaceModelManager.isUnsharedPluginProject(project)) {
				result.add(models[i]);
			}
		}
		if (result.size() > 0) {
			importListViewer.add(result.toArray());
			availableListViewer.remove(result.toArray());
		}
		pageChanged();		
	}
	
	private void handleAddRequiredPlugins() {
		TableItem[] items = importListViewer.getTable().getItems();
		if (items.length == 0)
			return;
		if (items.length == 1) {
			IPluginModelBase model = (IPluginModelBase) items[0].getData();
			if (model.getPluginBase().getId().equals("org.eclipse.core.boot")) {
				return;
			}
		}
				
		
		ArrayList result = new ArrayList();
		for (int i = 0; i < items.length; i++) {
			addPluginAndDependencies((IPluginModelBase) items[i].getData(), result);
		}
		if (implicitButton.isVisible() && implicitButton.getSelection()) {
			addImplicitDependencies(result);
		}
		
		handleRemoveAll(false);
		importListViewer.add(result.toArray());
		availableListViewer.remove(result.toArray());
		pageChanged();		
	}
	

}
