package org.eclipse.pde.internal.view;

import org.eclipse.swt.widgets.*;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.*;
import org.eclipse.jface.action.*;
import org.eclipse.pde.internal.PDEPlugin;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.preferences.MainPreferencePage;
import org.eclipse.jface.util.*;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.pde.internal.core.*;
import java.util.*;
import org.eclipse.pde.model.plugin.IPluginModelBase;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.internal.wizards.imports.PluginImportWizard;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.pde.internal.wizards.ListUtil;

public class PluginsView extends ViewPart {
	private TreeViewer treeViewer;
	private DrillDownAdapter drillDownAdapter;
	private IPropertyChangeListener propertyListener;
	private Action openAction;
	private Action importAction;
	private Action disabledFilterAction;
	private Action workspaceFilterAction;
	private DisabledFilter disabledFilter = new DisabledFilter();	
	private WorkspaceFilter workspaceFilter = new WorkspaceFilter();
	
	class DisabledFilter extends ViewerFilter {
		public boolean select(Viewer v, Object parent, Object element) {
			if (element instanceof ModelEntry) {
				ModelEntry entry = (ModelEntry)element;
				if (entry.getWorkspaceModel()==null) {
					IPluginModelBase externalModel = entry.getExternalModel();
					if (externalModel!=null)
						return externalModel.isEnabled();
				}
			}
			return true;
		}
	}
	
	class WorkspaceFilter extends ViewerFilter {
		public boolean select(Viewer v, Object parent, Object element) {
			if (element instanceof ModelEntry) {
				ModelEntry entry = (ModelEntry)element;
				return entry.getWorkspaceModel()==null;
			}
			return true;
		}
	}

	/**
	 * Constructor for PluginsView.
	 */
	public PluginsView() {
		propertyListener = new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				String property = event.getProperty();
				if (property.equals(MainPreferencePage.PROP_SHOW_OBJECTS)) {
					treeViewer.refresh();
				}
			}
		};
	}

	public void dispose() {
		PDEPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(
			propertyListener);
		super.dispose();
	}

	/**
	 * @see IWorkbenchPart#createPartControl(Composite)
	 */
	public void createPartControl(Composite parent) {
		treeViewer = new TreeViewer(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		drillDownAdapter = new DrillDownAdapter(treeViewer);
		PluginModelManager manager = PDEPlugin.getDefault().getModelManager();
		treeViewer.setContentProvider(new PluginsContentProvider(manager));
		treeViewer.setLabelProvider(new PluginsLabelProvider());
		treeViewer.addFilter(disabledFilter);
		treeViewer.setSorter(ListUtil.NAME_SORTER);
		makeActions();
		IActionBars actionBars = getViewSite().getActionBars();
		contributeToActionBars(actionBars);
		hookContextMenu();
		hookDoubleClickAction();
		treeViewer.setInput(manager);
		PDEPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(
			propertyListener);
	}

	private void contributeToActionBars(IActionBars actionBars) {
		contributeToLocalToolBar(actionBars.getToolBarManager());
		contributeToDropDownMenu(actionBars.getMenuManager());
	}

	private void contributeToDropDownMenu(IMenuManager manager) {
		manager.add(workspaceFilterAction);
		manager.add(disabledFilterAction);
	}

	private void contributeToLocalToolBar(IToolBarManager manager) {
		drillDownAdapter.addNavigationActions(manager);
	}
	private void makeActions() {
		openAction = new Action() {
			public void run() {
				handleDoubleClick();
			}
		};
		openAction.setText("Open");
		importAction = new Action() {
			public void run() {
				handleImport();
			}
		};
		importAction.setText("Import as Project...");
		disabledFilterAction = new Action() {
			public void run() {
				boolean checked = disabledFilterAction.isChecked();
				if (checked)
					treeViewer.removeFilter(disabledFilter);
				else
					treeViewer.addFilter(disabledFilter);
			}
		};
		disabledFilterAction.setText("Show disabled external plug-ins");
		disabledFilterAction.setChecked(false);
		workspaceFilterAction = new Action() {
			public void run() {
				boolean checked = workspaceFilterAction.isChecked();
				if (checked)
					treeViewer.removeFilter(workspaceFilter);
				else
					treeViewer.addFilter(workspaceFilter);
			}
		};
		workspaceFilterAction.setText("Show workspace plug-ins");
		workspaceFilterAction.setChecked(true);
	}
	private void fillContextMenu(IMenuManager manager) {
		IStructuredSelection selection =
			(IStructuredSelection) treeViewer.getSelection();

		if (selection.size() == 1) {
			Object sobj = selection.getFirstElement();
			if (sobj instanceof FileAdapter
				&& ((FileAdapter) sobj).isDirectory() == false) {
				manager.add(openAction);
				MenuManager openWith = new MenuManager("Open With");
				manager.add(openWith);
				manager.add(new Separator());
			}
		}
		if (selection.size() > 0) {
			if (canImport(selection)) {
				manager.add(importAction);
				manager.add(new Separator());
			}
		}
		drillDownAdapter.addNavigationActions(manager);
		manager.add(new Separator("Additions"));
	}

	private boolean canImport(IStructuredSelection selection) {
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			Object obj = iter.next();
			if (obj instanceof ModelEntry) {
				ModelEntry entry = (ModelEntry) obj;
				if (entry.getWorkspaceModel() != null)
					return false;
			} else
				return false;
		}
		return true;
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				PluginsView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(treeViewer.getControl());
		treeViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, treeViewer);
	}

	private void handleDoubleClick() {
	}

	private void handleImport() {
		IStructuredSelection selection = (IStructuredSelection)treeViewer.getSelection();
		IPluginModelBase [] models = new IPluginModelBase[selection.size()];
		int i=0;
		for (Iterator iter=selection.iterator(); iter.hasNext();) {
			ModelEntry entry = (ModelEntry)iter.next();
			models[i++] = entry.getExternalModel();
		}
		try {
			Shell shell = treeViewer.getTree().getShell();
			IRunnableWithProgress op =
				PluginImportWizard.getImportOperation(
					shell,
					true,
					false,
					models);
			ProgressMonitorDialog pmd = new ProgressMonitorDialog(shell);
			pmd.run(true, true, op);
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
		}
	}

	private void hookDoubleClickAction() {
		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				handleDoubleClick();
			}
		});
	}

	/**
	 * @see IWorkbenchPart#setFocus()
	 */
	public void setFocus() {
		treeViewer.getTree().setFocus();
	}
}