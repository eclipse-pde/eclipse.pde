/*
 * Created on May 31, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package org.eclipse.pde.internal.ui.wizards.imports;



import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
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
	private TableViewer importListViewer;
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
		container.setLayout(layout);
		
		createAvailableList(container).setLayoutData(new GridData(GridData.FILL_BOTH));
		createButtonArea(container);
		createImportList(container).setLayoutData(new GridData(GridData.FILL_BOTH));
		updateCount();
		

		setPageComplete(false);
		setControl(container);
		Dialog.applyDialogFont(container);
		
	}
	
	private Composite createAvailableList(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		container.setLayout(layout);

		Label label = new Label(container, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("ImportWizard.DetailedPage.availableList"));

		Table table = new Table(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 180;
		gd.heightHint = 300;
		table.setLayoutData(gd);

		availableListViewer = new TableViewer(table);
		availableListViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		availableListViewer.setContentProvider(new ContentProvider());
		availableListViewer.setInput(PDECore.getDefault().getExternalModelManager());
		availableListViewer.setSorter(ListUtil.PLUGIN_SORTER);
		availableListViewer.addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				return !selected.contains(element);
			}
		});
		return container;
	}
	
	private Composite createImportList(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		container.setLayout(layout);
		
		Label label = new Label(container, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("ImportWizard.DetailedPage.importList"));

		Table table = new Table(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 180;
		gd.heightHint = 300;
		table.setLayoutData(gd);

		importListViewer = new TableViewer(table);
		importListViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		importListViewer.setContentProvider(new ContentProvider());
		importListViewer.setInput(PDECore.getDefault().getExternalModelManager());
		importListViewer.setSorter(ListUtil.PLUGIN_SORTER);
		importListViewer.addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				return selected.contains(element);
			}
		});
				
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
				IStructuredSelection selection = (IStructuredSelection)availableListViewer.getSelection();
				if (selection.size() > 0) {
					selected.addAll(selection.toList());
				} else {
					TableItem[] items = availableListViewer.getTable().getItems();
					for (int i = 0; i < items.length; i++) {
						selected.add(items[i].getData());
					}
				}
				pageChanged();
			}
		});
		
		button = new Button(container, SWT.PUSH);
		button.setText(PDEPlugin.getResourceString("ImportWizard.DetailedPage.remove"));
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection selection = (IStructuredSelection)importListViewer.getSelection();
				if (selection.size() > 0) {
					selected.removeAll(selection.toList());
				} else {
					selected.clear();
				}
				pageChanged();
			}
		});
		
		button = new Button(container, SWT.PUSH);
		button.setText(PDEPlugin.getResourceString("ImportWizard.DetailedPage.swap"));
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				selected.clear();
				TableItem[] items = availableListViewer.getTable().getItems();
				for (int i = 0; i < items.length; i++) {
					selected.add(items[i].getData());
				}
				pageChanged();
			}
		});
		
		button = new Button(container, SWT.PUSH);
		button.setText(PDEPlugin.getResourceString("ImportWizard.DetailedPage.existing"));
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				selected.clear();
				for (int i = 0; i < models.length; i++) {
					String id = models[i].getPluginBase().getId();
					IProject project = (IProject) PDEPlugin.getWorkspace().getRoot().findMember(id);
					if (project != null && project.isOpen() && WorkspaceModelManager.isPluginProject(project)) {
						selected.add(models[i]);
					}
				}
				pageChanged();
			}
		});
		
		button = new Button(container, SWT.PUSH);
		button.setText(PDEPlugin.getResourceString("ImportWizard.DetailedPage.existingBinary"));
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				selected.clear();
				for (int i = 0; i < models.length; i++) {
					String id = models[i].getPluginBase().getId();
					IProject project = (IProject) PDEPlugin.getWorkspace().getRoot().findMember(id);
					if (project != null && project.isOpen() && WorkspaceModelManager.isBinaryPluginProject(project)) {
						selected.add(models[i]);
					}
				}
				pageChanged();
			}
		});
		
		button = new Button(container, SWT.PUSH);
		button.setText(PDEPlugin.getResourceString("ImportWizard.DetailedPage.addRequired"));
		button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				selected.clear();
				TableItem[] items = importListViewer.getTable().getItems();
				if (items.length == 1) {
					IPluginModelBase model = (IPluginModelBase) items[0].getData();
					if (model.getPluginBase().getId().equals("org.eclipse.core.boot")) {
						selected.add(model);
						return;
					}
				}
				for (int i = 0; i < items.length; i++) {
					addPluginAndDependencies((IPluginModelBase) items[i].getData());
				}
				pageChanged();
			}
		});
		
		countLabel = new Label(comp, SWT.NONE);
		countLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));		
		return container;
	}
	
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		setPageComplete(visible && importListViewer.getTable().getItemCount() > 0);
		
	}
	protected void refreshPage() {
		selected.clear();
		pageChanged();
	}
	
	private void pageChanged() {
		availableListViewer.refresh();
		importListViewer.refresh();
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
		
	}
	

}
