/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.view;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.action.*;
import org.eclipse.jface.util.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.plugin.*;
import org.eclipse.pde.internal.ui.preferences.MainPreferencePage;
import org.eclipse.pde.internal.ui.search.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.part.*;

public class DependenciesView extends ViewPart {
	private TreeViewer treeViewer;
	private DrillDownAdapter drillDownAdapter;
	private Action openAction;
	private FocusOnSelectionAction focusOnSelectionAction;
	private Action focusOnAction;
	private IPropertyChangeListener propertyListener;
	
	class FocusOnSelectionAction extends Action {
		public void run() {
			handleFocusOn(getSelectedObject());
		}
		public void update(Object object) {
			setEnabled(object!=null);
			String name = ((LabelProvider)treeViewer.getLabelProvider()).getText(object);
			setText(PDEPlugin.getFormattedMessage("DependenciesView.focusOnSelection", name)); //$NON-NLS-1$
		}
	}

	/**
	 * Constructor for PluginsView.
	 */
	public DependenciesView() {
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
		PDEPlugin
			.getDefault()
			.getPreferenceStore()
			.removePropertyChangeListener(
			propertyListener);
		super.dispose();
	}

	/**
	 * @see IWorkbenchPart#createPartControl(Composite)
	 */
	public void createPartControl(Composite parent) {
		treeViewer =
			new TreeViewer(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		drillDownAdapter = new DrillDownAdapter(treeViewer);
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		treeViewer.setContentProvider(
			new DependenciesContentProvider(this, manager));
		treeViewer.setLabelProvider(new DependenciesLabelProvider());
		treeViewer.setSorter(ListUtil.PLUGIN_SORTER);
		treeViewer.setAutoExpandLevel(2);
		makeActions();
		IActionBars actionBars = getViewSite().getActionBars();
		contributeToActionBars(actionBars);
		hookContextMenu();
		hookDoubleClickAction();
		treeViewer
			.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				handleSelectionChanged(e.getSelection());
			}
		});
		PDEPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(
			propertyListener);
		getViewSite().setSelectionProvider(treeViewer);
		
		WorkbenchHelp.setHelp(treeViewer.getControl(),IHelpContextIds.DEPENDENCIES_VIEW);
	}

	private void contributeToActionBars(IActionBars actionBars) {
		contributeToLocalToolBar(actionBars.getToolBarManager());
		contributeToDropDownMenu(actionBars.getMenuManager());
	}

	private void contributeToDropDownMenu(IMenuManager manager) {
	}

	private void contributeToLocalToolBar(IToolBarManager manager) {
		drillDownAdapter.addNavigationActions(manager);
	}
	private void makeActions() {
		openAction = new Action() {
			public void run() {
				handleDoubleClick();
			}
			public void update(Object object) {
			}
		};
		openAction.setText(PDEPlugin.getResourceString("DependenciesView.open")); //$NON-NLS-1$
		
		focusOnSelectionAction = new FocusOnSelectionAction();

		focusOnAction = new Action() {
			public void run() {
				handleFocusOn();
			}
		};
		focusOnAction.setText(PDEPlugin.getResourceString("DependenciesView.focusOn")); //$NON-NLS-1$
	}
	
	private Object getSelectedObject() {
		IStructuredSelection selection =
			(IStructuredSelection) treeViewer.getSelection();
		if (selection.isEmpty() || selection.size() != 1)
			return null;
		return selection.getFirstElement();
	}
	
	private void fillContextMenu(IMenuManager manager) {
		IStructuredSelection selection =
			(IStructuredSelection) treeViewer.getSelection();

		if (selection.size() == 1) {
			manager.add(openAction);
			manager.add(new Separator());
		}
		focusOnSelectionAction.update(getSelectedObject());
		if (focusOnSelectionAction.isEnabled())
			manager.add(focusOnSelectionAction);		
		manager.add(focusOnAction);
		if (selection.size() == 1) {
			manager.add(new Separator());
			PluginSearchActionGroup actionGroup = new PluginSearchActionGroup();
			actionGroup.setContext(new ActionContext(selection));
			actionGroup.fillContextMenu(manager);
		}
		if (treeViewer.getInput() instanceof WorkspacePluginModelBase) {
			manager.add(new UnusedDependenciesAction((WorkspacePluginModelBase)treeViewer.getInput()));
		}
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				DependenciesView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(treeViewer.getControl());
		treeViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, treeViewer);
	}

	private void handleDoubleClick() {
		Object obj = getSelectedObject();
		if (obj instanceof ImportObject) {
			IPlugin plugin = ((ImportObject)obj).getPlugin();
			if (plugin!=null) {
				obj = plugin;
			}
		}
		if (obj instanceof IPluginBase)
			ManifestEditor.openPluginEditor((IPluginBase)obj);
	}
	
	private void handleFocusOn(Object newFocus) {
		if (newFocus instanceof IPluginModelBase) {
			openTo(newFocus);
		}
		if (newFocus instanceof IPluginBase) {
			openTo(((IPluginBase)newFocus).getModel());
		}
		if (newFocus instanceof ImportObject) {
			ImportObject iimport = (ImportObject)newFocus;
			IPlugin plugin = iimport.getPlugin();
			if (plugin != null) {
				openTo(plugin.getModel());
			}
		}
	}
	
	private void handleFocusOn() {
		PluginSelectionDialog dialog = new PluginSelectionDialog(treeViewer.getControl().getShell(), true, false);
		dialog.create();
		if (dialog.open()==PluginSelectionDialog.OK) {
			handleFocusOn(dialog.getFirstResult());
		}
	}
	
	private void handleSelectionChanged(ISelection selection) {
		//String text = "";
		//Object obj = getSelectedObject();
		//getViewSite().getActionBars().getStatusLineManager().setMessage(text);
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

	void updateTitle(Object newInput) {
		IConfigurationElement config = getConfigurationElement();
		if (config == null)
			return;
		String viewName = config.getAttribute("name"); //$NON-NLS-1$
		if (newInput == null
			|| newInput.equals(PDECore.getDefault().getModelManager())) {
			// restore old
			setTitle(viewName);
			setTitleToolTip(getTitle());
		} else {
			String name =
				((LabelProvider) treeViewer.getLabelProvider()).getText(
					newInput);
			setTitle(viewName + ": " + name); //$NON-NLS-1$
			setTitleToolTip(getTitle());
			//setTitleToolTip(getInputPath(newInput));
		}
	}
	
	public void openTo(Object object) {
		treeViewer.setInput(object);
	}
}
