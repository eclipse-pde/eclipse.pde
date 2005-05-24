/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.view;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.OpenAction;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.EntryFileAdapter;
import org.eclipse.pde.internal.core.FileAdapter;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModelBase;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.JarEntryEditorInput;
import org.eclipse.pde.internal.ui.editor.SystemFileEditorInput;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.preferences.MainPreferencePage;
import org.eclipse.pde.internal.ui.search.PluginSearchActionGroup;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.pde.internal.ui.wizards.imports.PluginImportOperation;
import org.eclipse.pde.internal.ui.wizards.imports.PluginImportWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;

public class PluginsView extends ViewPart {
	private static final String DEFAULT_EDITOR_ID = "org.eclipse.ui.DefaultTextEditor"; //$NON-NLS-1$
	private TreeViewer treeViewer;
	private DrillDownAdapter drillDownAdapter;
	private IPropertyChangeListener propertyListener;
	private Action openAction;
	private Action importBinaryAction;
	private Action importSourceAction;
	private Action disabledFilterAction;
	private Action workspaceFilterAction;
	private Action openManifestAction;
	private Action openSchemaAction;
	private Action openSystemEditorAction;
	private Action openClassFileAction;
	private Action openDependenciesAdapter;
	private OpenDependenciesAction openDependenciesAction;
	private Action openTextEditorAction;
	private Action selectDependentAction;
	private Action selectInJavaSearchAction;
	private Action addToJavaSearchAction;
	private Action removeFromJavaSearchAction;
    private CollapseAllAction collapseAllAction;
	private ShowInWorkspaceAction showInNavigatorAction;
	private ShowInWorkspaceAction showInPackagesAction;
	private DisabledFilter disabledFilter = new DisabledFilter();
	private WorkspaceFilter workspaceFilter = new WorkspaceFilter();
	private JavaFilter javaFilter = new JavaFilter();
	private CopyToClipboardAction copyAction;
	private Clipboard clipboard;

	class DisabledFilter extends ViewerFilter {
		public boolean select(Viewer v, Object parent, Object element) {
			if (element instanceof ModelEntry) {
				ModelEntry entry = (ModelEntry) element;
				if (entry.getWorkspaceModel() == null) {
					IPluginModelBase externalModel = entry.getExternalModel();
					if (externalModel != null)
						return externalModel.isEnabled();
				}
			}
			return true;
		}
	}

	class WorkspaceFilter extends ViewerFilter {
		public boolean select(Viewer v, Object parent, Object element) {
			if (element instanceof ModelEntry) {
				ModelEntry entry = (ModelEntry) element;
				return entry.getWorkspaceModel() == null;
			}
			return true;
		}
	}

	class JavaFilter extends ViewerFilter {
		public boolean select(Viewer v, Object parent, Object element) {
			if (element instanceof IPackageFragment) {
				IPackageFragment packageFragment = (IPackageFragment) element;
				try {
					return packageFragment.hasChildren();
				} catch (JavaModelException e) {
					return false;
				}
			}
			return true;
		}
	}

    class CollapseAllAction extends Action {
        public CollapseAllAction() {
            super();
            setText(PDEUIMessages.PluginsView_CollapseAllAction_label); //$NON-NLS-1$
            setDescription(PDEUIMessages.PluginsView_CollapseAllAction_description); //$NON-NLS-1$
            setToolTipText(PDEUIMessages.PluginsView_CollapseAllAction_tooltip); //$NON-NLS-1$
            setImageDescriptor(PDEPluginImages.DESC_COLLAPSE_ALL);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.jface.action.Action#run()
         */
        public void run() {
            super.run();
            treeViewer.collapseAll();
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
		PDEPlugin
			.getDefault()
			.getPreferenceStore()
			.removePropertyChangeListener(
			propertyListener);
		openDependenciesAction.dispose();
		if (clipboard!=null) {
			clipboard.dispose();
			clipboard=null;
		}
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
			new PluginsContentProvider(this, manager));
		treeViewer.setLabelProvider(new PluginsLabelProvider());
		treeViewer.setSorter(ListUtil.PLUGIN_SORTER);
		initDragAndDrop();
		makeActions();
		initFilters();
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
		treeViewer.setInput(manager);
		PDEPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(
			propertyListener);
		getViewSite().setSelectionProvider(treeViewer);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(
			treeViewer.getControl(),
			IHelpContextIds.PLUGINS_VIEW);
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
        manager.add(new Separator());
        manager.add(collapseAllAction);
	}
	private void makeActions() {
		clipboard = new Clipboard(treeViewer.getTree().getDisplay());
		openAction = new Action() {
			public void run() {
				handleDoubleClick();
			}
		};
		openAction.setText(PDEUIMessages.PluginsView_open); //$NON-NLS-1$

		openDependenciesAction = new OpenDependenciesAction();
		openDependenciesAction.init(PDEPlugin.getActiveWorkbenchWindow());
		openDependenciesAdapter = new Action() {
			public void run() {
				ModelEntry entry = getEnclosingEntry();
				IPluginModelBase model = entry.getActiveModel();
				openDependenciesAction.selectionChanged(
					this,
					new StructuredSelection(model));
				openDependenciesAction.run(this);
			}
		};
		openDependenciesAdapter.setText(PDEUIMessages.PluginsView_openDependencies); //$NON-NLS-1$

		importBinaryAction = new Action() {
			public void run() {
				handleImport(false);
			}
		};
		importBinaryAction.setText(PDEUIMessages.PluginsView_asBinaryProject); //$NON-NLS-1$
		importSourceAction = new Action() {
			public void run() {
				handleImport(true);
			}
		};
		importSourceAction.setText(PDEUIMessages.PluginsView_asSourceProject); //$NON-NLS-1$
		disabledFilterAction = new Action() {
			public void run() {
				boolean checked = disabledFilterAction.isChecked();
				if (checked)
					treeViewer.removeFilter(disabledFilter);
				else
					treeViewer.addFilter(disabledFilter);
				getSettings().put("disabledFilter", !checked); //$NON-NLS-1$
			}
		};
		disabledFilterAction.setText(PDEUIMessages.PluginsView_showDisabled); //$NON-NLS-1$
		disabledFilterAction.setChecked(false);
		workspaceFilterAction = new Action() {
			public void run() {
				boolean checked = workspaceFilterAction.isChecked();
				if (checked)
					treeViewer.removeFilter(workspaceFilter);
				else
					treeViewer.addFilter(workspaceFilter);
				getSettings().put("workspaceFilter", !checked); //$NON-NLS-1$
			}
		};
		workspaceFilterAction.setText(PDEUIMessages.PluginsView_showWorkspace); //$NON-NLS-1$
		workspaceFilterAction.setChecked(true);

		openTextEditorAction = new Action() {
			public void run() {
				handleOpenTextEditor(getSelectedFile(), null);
			}
		};
		openTextEditorAction.setText(PDEUIMessages.PluginsView_textEditor); //$NON-NLS-1$
		openTextEditorAction.setImageDescriptor(
			PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
				ISharedImages.IMG_OBJ_FILE));

		openSystemEditorAction = new Action() {
			public void run() {
				handleOpenSystemEditor(getSelectedFile());
			}
		};
		openSystemEditorAction.setText(PDEUIMessages.PluginsView_systemEditor); //$NON-NLS-1$
		openManifestAction = new Action() {
			public void run() {
				handleOpenManifestEditor(getSelectedFile());
			}
		};
		openManifestAction.setText(PDEUIMessages.PluginsView_manifestEditor); //$NON-NLS-1$
		
		openSchemaAction = new Action() {
			public void run() {
				handleOpenSchemaEditor(getSelectedFile());
			}
		};
		openSchemaAction.setText(PDEUIMessages.PluginsView_schemaEditor); //$NON-NLS-1$

		copyAction = new CopyToClipboardAction(clipboard);
		copyAction.setText(PDEUIMessages.PluginsView_copy); //$NON-NLS-1$

		selectDependentAction = new Action() {
			public void run() {
				handleSelectDependent();
			}
		};
		selectDependentAction.setText(PDEUIMessages.PluginsView_dependentPlugins); //$NON-NLS-1$
		selectInJavaSearchAction = new Action() {
			public void run() {
				handleSelectInJavaSearch();
			}
		};
		selectInJavaSearchAction.setText(PDEUIMessages.PluginsView_pluginsInJavaSearch); //$NON-NLS-1$

		addToJavaSearchAction = new Action() {
			public void run() {
				handleJavaSearch(true);
			}
		};
		addToJavaSearchAction.setText(PDEUIMessages.PluginsView_addToJavaSearch); //$NON-NLS-1$

		removeFromJavaSearchAction = new Action() {
			public void run() {
				handleJavaSearch(false);
			}
		};
		removeFromJavaSearchAction.setText(PDEUIMessages.PluginsView_removeFromJavaSearch); //$NON-NLS-1$

		showInNavigatorAction =
			new ShowInWorkspaceAction(IPageLayout.ID_RES_NAV, treeViewer);
		showInNavigatorAction.setText(PDEUIMessages.PluginsView_showInNavigator); //$NON-NLS-1$
		showInPackagesAction =
			new ShowInWorkspaceAction(JavaUI.ID_PACKAGES, treeViewer);
		showInPackagesAction.setText(PDEUIMessages.PluginsView_showInPackageExplorer); //$NON-NLS-1$
        
        collapseAllAction = new CollapseAllAction();

		openClassFileAction = new OpenAction(getViewSite());
	}
	private FileAdapter getSelectedFile() {
		Object obj = getSelectedObject();
		if (obj instanceof FileAdapter)
			return (FileAdapter) obj;
		return null;
	}

	private ModelEntry getEnclosingEntry() {
		Object obj = getSelectedObject();
		if (obj == null)
			return null;
		if (obj instanceof ModelEntry)
			return (ModelEntry) obj;
		if (obj instanceof FileAdapter) {
			FileAdapter file = (FileAdapter) obj;
			if (file.isManifest()) {
				FileAdapter parent = file.getParent();
				if (parent instanceof EntryFileAdapter)
					return ((EntryFileAdapter) parent).getEntry();
			}
		}
		return null;
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
			Object sobj = selection.getFirstElement();
			boolean addSeparator = false;
            if (sobj instanceof ModelEntry) {
                ModelEntry entry = (ModelEntry) sobj;
                IPluginModelBase model = entry.getActiveModel();
                File file = new File(model.getInstallLocation());
                if (file.isFile() || model.getUnderlyingResource() != null) {
                    manager.add(openAction);
                }
            }
			if (sobj instanceof FileAdapter
				&& ((FileAdapter) sobj).isDirectory() == false) {
				manager.add(openAction);
				MenuManager openWithMenu = new MenuManager(PDEUIMessages.PluginsView_openWith); //$NON-NLS-1$
				fillOpenWithMenu(openWithMenu, sobj);
				manager.add(openWithMenu);
				addSeparator = true;
			}
			if (sobj instanceof IStorage) {
				manager.add(openAction);
				addSeparator = true;
			}
			if (sobj instanceof IClassFile) {
				manager.add(openClassFileAction);
				addSeparator = true;
			}
			ModelEntry entry = getEnclosingEntry();
			if (entry != null) {
				manager.add(openDependenciesAdapter);
				manager.add(new Separator());
				PluginSearchActionGroup actionGroup =
					new PluginSearchActionGroup();
				actionGroup.setContext(new ActionContext(selection));
				actionGroup.fillContextMenu(manager);
				addSeparator = true;
			}
			if (addSeparator)
				manager.add(new Separator());
		}
		if (selection.size() > 0) {
			boolean addSeparator = false;
			if (showInNavigatorAction.isApplicable()) {
				manager.add(showInNavigatorAction);
				addSeparator = true;
			}
			if (showInPackagesAction.isApplicable()) {
				manager.add(showInPackagesAction);
				addSeparator = true;
			}
			if (addSeparator) {
				manager.add(new Separator());
			}
			if (canImport(selection)) {
				MenuManager importMenu = new MenuManager(PDEUIMessages.PluginsView_import); //$NON-NLS-1$
				importMenu.add(importBinaryAction);
				importMenu.add(importSourceAction);
				manager.add(importMenu);
				manager.add(new Separator());
			}
			addSeparator = false;
			if (canDoJavaSearchOperation(selection, true)) {
				manager.add(addToJavaSearchAction);
				addSeparator = true;
			}
			if (canDoJavaSearchOperation(selection, false)) {
				manager.add(removeFromJavaSearchAction);
				addSeparator = true;
			}
			if (addSeparator) {
				manager.add(new Separator());
			}
		}
		copyAction.setSelection(selection);
		manager.add(copyAction);
		IMenuManager selectionMenu = new MenuManager(PDEUIMessages.PluginsView_select); //$NON-NLS-1$
		manager.add(selectionMenu);
		if (selection.size() > 0)
			selectionMenu.add(selectDependentAction);
		selectionMenu.add(selectInJavaSearchAction);
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	private void fillOpenWithMenu(IMenuManager manager, Object obj) {
		FileAdapter adapter = (FileAdapter) obj;
		String editorId = adapter.getEditorId();

		String fileName = adapter.getFile().getName();
		String lcFileName = fileName.toLowerCase();
		ImageDescriptor desc =
			PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(
				fileName);
		if (lcFileName.equals("plugin.xml") //$NON-NLS-1$
		|| lcFileName.equals("fragment.xml") //$NON-NLS-1$
		|| lcFileName.equals("manifest.mf")) { //$NON-NLS-1$
			openManifestAction.setImageDescriptor(desc);
			manager.add(openManifestAction);
			manager.add(new Separator());
			openManifestAction.setChecked(
				editorId != null
					&& editorId.equals(PDEPlugin.MANIFEST_EDITOR_ID));
		}
		if (lcFileName.endsWith(".mxsd") || lcFileName.endsWith(".exsd")) { //$NON-NLS-1$ //$NON-NLS-2$
			openSchemaAction.setImageDescriptor(desc);
			manager.add(openSchemaAction);
			manager.add(new Separator());
			openSchemaAction.setChecked(
				editorId != null 
					&& editorId.equals(PDEPlugin.SCHEMA_EDITOR_ID));
		}
		manager.add(openTextEditorAction);
		openTextEditorAction.setChecked(
			editorId == null || editorId.equals(DEFAULT_EDITOR_ID));
		openSystemEditorAction.setImageDescriptor(desc);
		openSystemEditorAction.setChecked(editorId != null && editorId.equals("@system")); //$NON-NLS-1$
		manager.add(openSystemEditorAction);
	}

	private boolean canImport(IStructuredSelection selection) {
		int nexternal = 0;
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			Object obj = iter.next();
			if (obj instanceof ModelEntry) {
				ModelEntry entry = (ModelEntry) obj;
				if (entry.getWorkspaceModel() == null)
					nexternal++;
			} else
				return false;
		}
		return nexternal > 0;
	}

	private boolean canDoJavaSearchOperation(
		IStructuredSelection selection,
		boolean add) {
		int nhits = 0;
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			Object obj = iter.next();
			if (obj instanceof ModelEntry) {
				ModelEntry entry = (ModelEntry) obj;
				if (entry.getWorkspaceModel() == null) {
					if (add && entry.isInJavaSearch() == false)
						nhits++;
					if (!add && entry.isInJavaSearch())
						nhits++;
				}
			}
		}
		return nhits > 0;
	}

	protected void initDragAndDrop() {
		int ops = DND.DROP_COPY | DND.DROP_MOVE;
		Transfer[] transfers = new Transfer[] { FileTransfer.getInstance()};
		treeViewer.addDragSupport(
			ops,
			transfers,
			new PluginsDragAdapter(treeViewer));
	}

	private IDialogSettings getSettings() {
		IDialogSettings master = PDEPlugin.getDefault().getDialogSettings();
		IDialogSettings section = master.getSection("pluginsView"); //$NON-NLS-1$
		if (section == null) {
			section = master.addNewSection("pluginsView"); //$NON-NLS-1$
		}
		return section;
	}

	private void initFilters() {
		boolean workspace = false;
		boolean disabled = true;
		IDialogSettings settings = getSettings();
		workspace = settings.getBoolean("workspaceFilter"); //$NON-NLS-1$
		disabled = !settings.getBoolean("disabledFilter"); //$NON-NLS-1$
		if (workspace)
			treeViewer.addFilter(workspaceFilter);
		if (disabled)
			treeViewer.addFilter(disabledFilter);
		treeViewer.addFilter(javaFilter);
		workspaceFilterAction.setChecked(!workspace);
		disabledFilterAction.setChecked(!disabled);
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
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
		Object obj = getSelectedObject();
		if (obj instanceof ModelEntry) {
            boolean expanded = treeViewer.getExpandedState(obj);
            treeViewer.setExpandedState(obj, !expanded);
            if (treeViewer.getExpandedState(obj) == expanded) {
                // not expandable, open editor
                ModelEntry modelEntry = (ModelEntry) obj;
                ManifestEditor.openPluginEditor(modelEntry.getId());
            }
		}
		if (obj instanceof FileAdapter) {
			FileAdapter adapter = (FileAdapter) obj;
			if (adapter.isDirectory()) {
				treeViewer.setExpandedState(
					adapter,
					!treeViewer.getExpandedState(adapter));
				return;
			}
			String editorId = adapter.getEditorId();
			if (editorId != null && editorId.equals("@system")) //$NON-NLS-1$
				handleOpenSystemEditor(adapter);
			else
				handleOpenTextEditor(adapter, editorId);
		}
		if (obj instanceof IClassFile) {
			openClassFileAction.run();
		}
		if (obj instanceof IStorage) {
			handleOpenStorage((IStorage) obj);
		}
	}

	private void handleOpenStorage(IStorage obj) {
		IWorkbenchPage page = PDEPlugin.getActivePage();
		IEditorInput input = new JarEntryEditorInput(obj);
		try {
			page.openEditor(input, DEFAULT_EDITOR_ID);
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
	}

	private void handleImport(boolean extractSource) {
		IStructuredSelection selection =
			(IStructuredSelection) treeViewer.getSelection();
		ArrayList externalModels = new ArrayList();
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			ModelEntry entry = (ModelEntry) iter.next();
			if (entry.getWorkspaceModel() != null)
				continue;
			externalModels.add(entry.getExternalModel());
		}
		IPluginModelBase[] models =
			(IPluginModelBase[]) externalModels.toArray(
				new IPluginModelBase[externalModels.size()]);
		try {
			Shell shell = treeViewer.getTree().getShell();
			int importType =
				extractSource
					? PluginImportOperation.IMPORT_WITH_SOURCE
					: PluginImportOperation.IMPORT_BINARY;
					
			IRunnableWithProgress op =
				PluginImportWizard.getImportOperation(
					shell,
					importType,
					models,
					false);
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(op);
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
		} catch (Exception e) {
		}
	}

	private void handleJavaSearch(final boolean add) {
		IStructuredSelection selection =
			(IStructuredSelection) treeViewer.getSelection();
		if (selection.size() == 0)
			return;

		ArrayList result = new ArrayList();

		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			ModelEntry entry = (ModelEntry) iter.next();
			if (entry.getWorkspaceModel() != null)
				continue;
			if (entry.isInJavaSearch() == !add)
				result.add(entry);
		}
		if (result.size() == 0)
			return;
		final ModelEntry[] array =
			(ModelEntry[]) result.toArray(new ModelEntry[result.size()]);

		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
				throws InvocationTargetException {
				PluginModelManager manager =
					PDECore.getDefault().getModelManager();
				try {
					manager.setInJavaSearch(array, add, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};

		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(op);
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
		}
	}

	private void handleSelectDependent() {
		IStructuredSelection selection =
			(IStructuredSelection) treeViewer.getSelection();
		if (selection.size() == 0)
			return;
		HashSet set = new HashSet();
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			ModelEntry entry = (ModelEntry) iter.next();
			set.add(entry);
			addDependentEntries(entry, set);
		}
		treeViewer.setSelection(new StructuredSelection(set.toArray()));
	}
	private void handleSelectInJavaSearch() {
		PluginsContentProvider provider =
			(PluginsContentProvider) treeViewer.getContentProvider();
		Object[] elements = provider.getElements(treeViewer.getInput());
		ArrayList result = new ArrayList();
		for (int i = 0; i < elements.length; i++) {
			Object element = elements[i];
			if (element instanceof ModelEntry) {
				ModelEntry entry = (ModelEntry) element;
				if (entry.isInJavaSearch())
					result.add(entry);
			}
		}
		treeViewer.setSelection(new StructuredSelection(result.toArray()));
	}

	private void addDependentEntries(ModelEntry entry, Set set) {
		if (entry.isEmpty()
			|| entry.getActiveModel() instanceof WorkspacePluginModelBase)
			return;
		IPluginModelBase model = entry.getExternalModel();
		if (model == null)
			return;
		IPluginBase plugin = model.getPluginBase();
		if (plugin == null)
			return;
		IPluginImport[] iimports = plugin.getImports();
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		for (int i = 0; i < iimports.length; i++) {
			IPluginImport iimport = iimports[i];
			ModelEntry ientry =
				manager.findEntry(iimport.getId());
			if (ientry != null) {
				set.add(ientry);
				addDependentEntries(ientry, set);
			}
		}
	}

	private void handleOpenTextEditor(FileAdapter adapter, String editorId) {
		if (adapter == null)
			return;
		IWorkbenchPage page = PDEPlugin.getActivePage();
		if (editorId == null) {
			if (adapter.isManifest())
				editorId = PDEPlugin.MANIFEST_EDITOR_ID;
			else if (adapter.isSchema())
				editorId = PDEPlugin.SCHEMA_EDITOR_ID;
		}
		try {
			if (editorId == null || editorId.equals("@system")) //$NON-NLS-1$
				editorId = DEFAULT_EDITOR_ID;
			page.openEditor(
				new SystemFileEditorInput(adapter.getFile()),
				editorId);
			adapter.setEditorId(editorId);
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
	}

	private void handleOpenManifestEditor(FileAdapter adapter) {
		handleOpenTextEditor(adapter, PDEPlugin.MANIFEST_EDITOR_ID);
	}
	
	private void handleOpenSchemaEditor(FileAdapter adapter) {
		handleOpenTextEditor(adapter, PDEPlugin.SCHEMA_EDITOR_ID);
	}

	private void handleOpenSystemEditor(FileAdapter adapter) {
		if (adapter == null)
			return;
		File localFile = null;

		try {
			localFile = getLocalCopy(adapter.getFile());
		} catch (IOException e) {
			PDEPlugin.logException(e);
			return;
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		// Start busy indicator.
		final File file = localFile;
		final boolean result[] = new boolean[1];
		BusyIndicator
			.showWhile(treeViewer.getTree().getDisplay(), new Runnable() {
			public void run() {
				// Open file using shell.
				String path = file.getAbsolutePath();
				result[0] = Program.launch(path);
			}
		});

		// ShellExecute returns whether call was successful
		if (!result[0]) {
			PDEPlugin.logException(
				new PartInitException(
					NLS.bind(PDEUIMessages.PluginsView_unableToOpen, file.getName())));
		} else {
			adapter.setEditorId("@system"); //$NON-NLS-1$
		}
	}

	private File getLocalCopy(File file) throws IOException, CoreException {
		// create a tmp. copy of this file and make it
		// read-only. This is to ensure that the original
		// file belonging to the external plug-in directories
		// will not be modified. 
		String fileName = file.getName();
		String prefix;
		String suffix = null;
		int dotLoc = fileName.indexOf('.');
		if (dotLoc != -1) {
			prefix = fileName.substring(0, dotLoc);
			suffix = fileName.substring(dotLoc);
		} else {
			prefix = fileName;
		}

		File tmpFile = File.createTempFile(prefix, suffix);
		tmpFile.deleteOnExit();
		FileOutputStream fos = new FileOutputStream(tmpFile);
		FileInputStream fis = new FileInputStream(file);
		byte[] cbuffer = new byte[1024];
		int read = 0;

		while (read != -1) {
			read = fis.read(cbuffer);
			if (read != -1)
				fos.write(cbuffer, 0, read);
		}
		fos.flush();
		fos.close();
		fis.close();
		tmpFile.setReadOnly();
		return tmpFile;
	}

	private void handleSelectionChanged(ISelection selection) {
		String text = ""; //$NON-NLS-1$
		Object obj = getSelectedObject();
		if (obj instanceof ModelEntry) {
			IPluginModelBase model = ((ModelEntry) obj).getActiveModel();
			text = model.getInstallLocation();
		}
		if (obj instanceof FileAdapter) {
			text = ((FileAdapter) obj).getFile().getAbsolutePath();
		}
		getViewSite().getActionBars().getStatusLineManager().setMessage(text);
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
		
		if (newInput == null
			|| newInput.equals(PDECore.getDefault().getModelManager())) {
			setContentDescription("");
			// restore old
			setTitleToolTip(getTitle());
		} else {
			String viewName = config.getAttribute("name"); //$NON-NLS-1$
			String name = ((LabelProvider) treeViewer.getLabelProvider()).getText(newInput);
			setContentDescription(viewName + ": " + name); //$NON-NLS-1$
			setTitleToolTip(getInputPath(newInput));
		}
	}
	private String getInputPath(Object input) {
		if (input instanceof FileAdapter) {
			return "file: " + ((FileAdapter) input).getFile().getAbsolutePath(); //$NON-NLS-1$
		}
		if (input instanceof ModelEntry) {
			IPluginModelBase model = ((ModelEntry) input).getActiveModel();
			return "plugin: " + model.getInstallLocation(); //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$
	}

}
