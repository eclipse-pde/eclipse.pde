/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
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
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.JarEntryEditorInput;
import org.eclipse.pde.internal.ui.editor.SystemFileEditorInput;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.search.PluginSearchActionGroup;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;

public class PluginsView extends ViewPart {
	private static final String DEFAULT_EDITOR_ID = "org.eclipse.ui.DefaultTextEditor";  //$NON-NLS-1$
	private static final String HIDE_WRKSPC = "hideWorkspace"; //$NON-NLS-1$
	private static final String HIDE_EXENABLED = "hideEnabledExternal"; //$NON-NLS-1$
	private static final String SHOW_EXDISABLED = "showDisabledExternal"; //$NON-NLS-1$
	private TreeViewer fTreeViewer;
	private DrillDownAdapter fDrillDownAdapter;
	private IPropertyChangeListener fPropertyListener;
	private Action fOpenAction;
	private Action fHideExtDisabledFilterAction;
	private Action fHideExtEnabledFilterAction;
	private Action fHideWorkspaceFilterAction;
	private Action fOpenManifestAction;
	private Action fOpenSchemaAction;
	private Action fOpenSystemEditorAction;
	private Action fOpenClassFileAction;
	private Action fOpenDependenciesAdapter;
	private OpenDependenciesAction fOpenDependenciesAction;
	private Action fOpenTextEditorAction;
	private Action fSelectDependentAction;
	private Action fSelectInJavaSearchAction;
	private Action fSelectAllAction;
    private CollapseAllAction fCollapseAllAction;
	private ShowInWorkspaceAction fShowInNavigatorAction;
	private ShowInWorkspaceAction fShowInPackagesAction;
	private DisabledFilter fHideExtEnabledFilter = new DisabledFilter(true);
	private DisabledFilter fHideExtDisabledFilter = new DisabledFilter(false);
	private WorkspaceFilter fHideWorkspaceFilter = new WorkspaceFilter();
	private JavaFilter fJavaFilter = new JavaFilter();
	private CopyToClipboardAction fCopyAction;
	private Clipboard fClipboard;

	class DisabledFilter extends ViewerFilter {
		
		boolean fEnabled;
		DisabledFilter(boolean enabled) {
			fEnabled = enabled;
		}
		public boolean select(Viewer v, Object parent, Object element) {
			if (element instanceof ModelEntry) {
				ModelEntry entry = (ModelEntry) element;
				if (entry.getWorkspaceModel() == null) {
					IPluginModelBase externalModel = entry.getExternalModel();
					if (externalModel != null)
						return externalModel.isEnabled() != fEnabled;
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
            setText(PDEUIMessages.PluginsView_CollapseAllAction_label); 
            setDescription(PDEUIMessages.PluginsView_CollapseAllAction_description); 
            setToolTipText(PDEUIMessages.PluginsView_CollapseAllAction_tooltip); 
            setImageDescriptor(PDEPluginImages.DESC_COLLAPSE_ALL);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.jface.action.Action#run()
         */
        public void run() {
            super.run();
            fTreeViewer.collapseAll();
        }
    }
    
	/**
	 * Constructor for PluginsView.
	 */
	public PluginsView() {
		fPropertyListener = new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				String property = event.getProperty();
				if (property.equals(IPreferenceConstants.PROP_SHOW_OBJECTS)) {
					fTreeViewer.refresh();
				}
			}
		};
	}

	public void dispose() {
		PDEPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(
				fPropertyListener);
		fOpenDependenciesAction.dispose();
		if (fClipboard != null) {
			fClipboard.dispose();
			fClipboard = null;
		}
		super.dispose();
	}

	/**
	 * @see IWorkbenchPart#createPartControl(Composite)
	 */
	public void createPartControl(Composite parent) {
		fTreeViewer = new TreeViewer(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		fDrillDownAdapter = new DrillDownAdapter(fTreeViewer);
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		fTreeViewer.setContentProvider(new PluginsContentProvider(this, manager));
		fTreeViewer.setLabelProvider(new PluginsLabelProvider());
		fTreeViewer.setSorter(ListUtil.PLUGIN_SORTER);
		initDragAndDrop();
		makeActions();
		initFilters();
		IActionBars actionBars = getViewSite().getActionBars();
		contributeToActionBars(actionBars);
		registerGlobalActions(actionBars);
		hookContextMenu();
		hookDoubleClickAction();
		fTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				handleSelectionChanged(e.getSelection());
			}
		});
		fTreeViewer.setInput(manager);
		updateContentDescription();
		PDEPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(
			fPropertyListener);
		getViewSite().setSelectionProvider(fTreeViewer);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(
			fTreeViewer.getControl(),
			IHelpContextIds.PLUGINS_VIEW);
	}

	private void contributeToActionBars(IActionBars actionBars) {
		contributeToLocalToolBar(actionBars.getToolBarManager());
		contributeToDropDownMenu(actionBars.getMenuManager());
	}

	private void contributeToDropDownMenu(IMenuManager manager) {
		manager.add(fHideWorkspaceFilterAction);
		manager.add(fHideExtEnabledFilterAction);
		manager.add(fHideExtDisabledFilterAction);
	}

	private void contributeToLocalToolBar(IToolBarManager manager) {
 		fDrillDownAdapter.addNavigationActions(manager);
        manager.add(new Separator());
        manager.add(fCollapseAllAction);
	}
	
	private void registerGlobalActions(IActionBars actionBars) {
		actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(),
            fSelectAllAction);
	}
	private void makeActions() {
		fClipboard = new Clipboard(fTreeViewer.getTree().getDisplay());
		fOpenAction = new Action() {
			public void run() {
				handleDoubleClick();
			}
		};
		fOpenAction.setText(PDEUIMessages.PluginsView_open); 

		fOpenDependenciesAction = new OpenDependenciesAction();
		fOpenDependenciesAction.init(PDEPlugin.getActiveWorkbenchWindow());
		fOpenDependenciesAdapter = new Action() {
			public void run() {
				ModelEntry entry = getEnclosingEntry();
				IPluginModelBase model = entry.getActiveModel();
				fOpenDependenciesAction.selectionChanged(
					this, new StructuredSelection(model));
				fOpenDependenciesAction.run(this);
			}
		};
		fOpenDependenciesAdapter.setText(PDEUIMessages.PluginsView_openDependencies); 
		fHideExtDisabledFilterAction = new Action() {
			public void run() {
				boolean checked = fHideExtDisabledFilterAction.isChecked();
				if (checked)
					fTreeViewer.removeFilter(fHideExtDisabledFilter);
				else
					fTreeViewer.addFilter(fHideExtDisabledFilter);
				getSettings().put(SHOW_EXDISABLED, checked); 
				updateContentDescription();
			}
		};
		fHideExtDisabledFilterAction.setText(PDEUIMessages.PluginsView_showDisabled); 
		fHideExtEnabledFilterAction = new Action() {
			public void run() {
				boolean checked = fHideExtEnabledFilterAction.isChecked();
				if (checked)
					fTreeViewer.removeFilter(fHideExtEnabledFilter);
				else
					fTreeViewer.addFilter(fHideExtEnabledFilter);
				getSettings().put(HIDE_EXENABLED, !checked); 
				updateContentDescription();
			}
		};
		fHideExtEnabledFilterAction.setText(PDEUIMessages.PluginsView_showEnabled); 
		fHideWorkspaceFilterAction = new Action() {
			public void run() {
				boolean checked = fHideWorkspaceFilterAction.isChecked();
				if (checked)
					fTreeViewer.removeFilter(fHideWorkspaceFilter);
				else
					fTreeViewer.addFilter(fHideWorkspaceFilter);
				getSettings().put(HIDE_WRKSPC, !checked); 
				updateContentDescription();
			}
		};
		fHideWorkspaceFilterAction.setText(PDEUIMessages.PluginsView_showWorkspace); 

		fOpenTextEditorAction = new Action() {
			public void run() {
				handleOpenTextEditor(getSelectedFile(), null);
			}
		};
		fOpenTextEditorAction.setText(PDEUIMessages.PluginsView_textEditor); 
		fOpenTextEditorAction.setImageDescriptor(
			PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
				ISharedImages.IMG_OBJ_FILE));

		fOpenSystemEditorAction = new Action() {
			public void run() {
				handleOpenSystemEditor(getSelectedFile());
			}
		};
		fOpenSystemEditorAction.setText(PDEUIMessages.PluginsView_systemEditor); 
		fOpenManifestAction = new Action() {
			public void run() {
				handleOpenManifestEditor(getSelectedFile());
			}
		};
		fOpenManifestAction.setText(PDEUIMessages.PluginsView_manifestEditor); 
		
		fOpenSchemaAction = new Action() {
			public void run() {
				handleOpenSchemaEditor(getSelectedFile());
			}
		};
		fOpenSchemaAction.setText(PDEUIMessages.PluginsView_schemaEditor); 

		fCopyAction = new CopyToClipboardAction(fClipboard);
		fCopyAction.setText(PDEUIMessages.PluginsView_copy); 

		fSelectDependentAction = new Action() {
			public void run() {
				handleSelectDependent();
			}
		};
		fSelectDependentAction.setText(PDEUIMessages.PluginsView_dependentPlugins); 
		
		fSelectInJavaSearchAction = new Action() {
			public void run() {
				handleSelectInJavaSearch();
			}
		};
		fSelectInJavaSearchAction.setText(PDEUIMessages.PluginsView_pluginsInJavaSearch); 
		
		fSelectAllAction = new Action() {
	        public void run() {
	            super.run();
	            fTreeViewer.getTree().selectAll();
	        }
		};
		fSelectAllAction.setText(PDEUIMessages.PluginsView_SelectAllAction_label); 

		fShowInNavigatorAction =
			new ShowInWorkspaceAction(IPageLayout.ID_RES_NAV, fTreeViewer);
		fShowInNavigatorAction.setText(PDEUIMessages.PluginsView_showInNavigator); 
		fShowInPackagesAction =
			new ShowInWorkspaceAction(JavaUI.ID_PACKAGES, fTreeViewer);
		fShowInPackagesAction.setText(PDEUIMessages.PluginsView_showInPackageExplorer); 
        
        fCollapseAllAction = new CollapseAllAction();

		fOpenClassFileAction = new OpenAction(getViewSite());
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
			(IStructuredSelection) fTreeViewer.getSelection();
		if (selection.isEmpty() || selection.size() != 1)
			return null;
		return selection.getFirstElement();
	}
	private void fillContextMenu(IMenuManager manager) {
		IStructuredSelection selection =
			(IStructuredSelection) fTreeViewer.getSelection();

		if (selection.size() == 1) {
			Object sobj = selection.getFirstElement();
			boolean addSeparator = false;
            if (sobj instanceof ModelEntry) {
                ModelEntry entry = (ModelEntry) sobj;
                IPluginModelBase model = entry.getActiveModel();
                File file = new File(model.getInstallLocation());
                if (file.isFile() || model.getUnderlyingResource() != null) {
                    manager.add(fOpenAction);
                }
            }
			if (sobj instanceof FileAdapter
				&& ((FileAdapter) sobj).isDirectory() == false) {
				manager.add(fOpenAction);
				MenuManager openWithMenu = new MenuManager(PDEUIMessages.PluginsView_openWith); 
				fillOpenWithMenu(openWithMenu, sobj);
				manager.add(openWithMenu);
				addSeparator = true;
			}
			if (sobj instanceof IStorage) {
				manager.add(fOpenAction);
				addSeparator = true;
			}
			if (sobj instanceof IClassFile) {
				manager.add(fOpenClassFileAction);
				addSeparator = true;
			}
			ModelEntry entry = getEnclosingEntry();
			if (entry != null) {
				manager.add(fOpenDependenciesAdapter);
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
			if (fShowInNavigatorAction.isApplicable()) {
				manager.add(fShowInNavigatorAction);
				addSeparator = true;
			}
			if (fShowInPackagesAction.isApplicable()) {
				manager.add(fShowInPackagesAction);
				addSeparator = true;
			}
			if (addSeparator) {
				manager.add(new Separator());
			}
			if (ImportActionGroup.canImport(selection)) {
				ImportActionGroup actionGroup = new ImportActionGroup();
				actionGroup.setContext(new ActionContext(selection));
				actionGroup.fillContextMenu(manager);
				manager.add(new Separator());
			}
			
			JavaSearchActionGroup actionGroup = 
				new JavaSearchActionGroup();
			actionGroup.setContext(new ActionContext(selection));
			actionGroup.fillContextMenu(manager);
		}
		fCopyAction.setSelection(selection);
		manager.add(fCopyAction);
		IMenuManager selectionMenu = new MenuManager(PDEUIMessages.PluginsView_select); 
		manager.add(selectionMenu);
		if (selection.size() > 0)
			selectionMenu.add(fSelectDependentAction);
		selectionMenu.add(fSelectInJavaSearchAction);
		selectionMenu.add(fSelectAllAction);
		manager.add(new Separator());
		fDrillDownAdapter.addNavigationActions(manager);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	private void fillOpenWithMenu(IMenuManager manager, Object obj) {
		FileAdapter adapter = (FileAdapter) obj;
		String editorId = adapter.getEditorId();

		String fileName = adapter.getFile().getName();
		String lcFileName = fileName.toLowerCase(Locale.ENGLISH);
		ImageDescriptor desc =
			PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(
				fileName);
		if (lcFileName.equals("plugin.xml")  //$NON-NLS-1$
		|| lcFileName.equals("fragment.xml")  //$NON-NLS-1$
		|| lcFileName.equals("manifest.mf")) {  //$NON-NLS-1$
			fOpenManifestAction.setImageDescriptor(desc);
			manager.add(fOpenManifestAction);
			manager.add(new Separator());
			fOpenManifestAction.setChecked(
				editorId != null
					&& editorId.equals(IPDEUIConstants.MANIFEST_EDITOR_ID));
		}
		if (lcFileName.endsWith(".mxsd") || lcFileName.endsWith(".exsd")) { //$NON-NLS-1$ //$NON-NLS-2$
			fOpenSchemaAction.setImageDescriptor(desc);
			manager.add(fOpenSchemaAction);
			manager.add(new Separator());
			fOpenSchemaAction.setChecked(
				editorId != null 
					&& editorId.equals(IPDEUIConstants.SCHEMA_EDITOR_ID));
		}
		manager.add(fOpenTextEditorAction);
		fOpenTextEditorAction.setChecked(
			editorId == null || editorId.equals(DEFAULT_EDITOR_ID));
		fOpenSystemEditorAction.setImageDescriptor(desc);
		fOpenSystemEditorAction.setChecked(editorId != null && editorId.equals("@system"));  //$NON-NLS-1$
		manager.add(fOpenSystemEditorAction);
	}

	protected void initDragAndDrop() {
		int ops = DND.DROP_COPY | DND.DROP_MOVE;
		Transfer[] transfers = new Transfer[] { FileTransfer.getInstance()};
		fTreeViewer.addDragSupport(
			ops,
			transfers,
			new PluginsDragAdapter(fTreeViewer));
	}

	private IDialogSettings getSettings() {
		IDialogSettings master = PDEPlugin.getDefault().getDialogSettings();
		IDialogSettings section = master.getSection("pluginsView");  //$NON-NLS-1$
		if (section == null) {
			section = master.addNewSection("pluginsView");  //$NON-NLS-1$
		}
		return section;
	}

	private void initFilters() {
		IDialogSettings settings = getSettings();
		fTreeViewer.addFilter(fJavaFilter);
		boolean hideWorkspace = settings.getBoolean(HIDE_WRKSPC);
		boolean hideEnabledExternal = settings.getBoolean(HIDE_EXENABLED);
		boolean hideDisabledExternal = !settings.getBoolean(SHOW_EXDISABLED);
		if (hideWorkspace)
			fTreeViewer.addFilter(fHideWorkspaceFilter);
		if (hideEnabledExternal)
			fTreeViewer.addFilter(fHideExtEnabledFilter);
		if (hideDisabledExternal)
			fTreeViewer.addFilter(fHideExtDisabledFilter);
		fHideWorkspaceFilterAction.setChecked(!hideWorkspace);
		fHideExtEnabledFilterAction.setChecked(!hideEnabledExternal);
		fHideExtDisabledFilterAction.setChecked(!hideDisabledExternal);
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");  //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				PluginsView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(fTreeViewer.getControl());
		fTreeViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, fTreeViewer);
	}

	private void handleDoubleClick() {
		Object obj = getSelectedObject();
		if (obj instanceof ModelEntry) {
            boolean expanded = fTreeViewer.getExpandedState(obj);
            fTreeViewer.setExpandedState(obj, !expanded);
            if (fTreeViewer.getExpandedState(obj) == expanded) {
                // not expandable, open editor
                ModelEntry modelEntry = (ModelEntry) obj;
                ManifestEditor.openPluginEditor(modelEntry.getId());
            }
		}
		if (obj instanceof FileAdapter) {
			FileAdapter adapter = (FileAdapter) obj;
			if (adapter.isDirectory()) {
				fTreeViewer.setExpandedState(
					adapter,
					!fTreeViewer.getExpandedState(adapter));
				return;
			}
			String editorId = adapter.getEditorId();
			if (editorId != null && editorId.equals("@system"))  //$NON-NLS-1$
				handleOpenSystemEditor(adapter);
			else
				handleOpenTextEditor(adapter, editorId);
		}
		if (obj instanceof IClassFile) {
			fOpenClassFileAction.run();
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

	private void handleSelectDependent() {
		IStructuredSelection selection =
			(IStructuredSelection) fTreeViewer.getSelection();
		if (selection.size() == 0)
			return;
		HashSet set = new HashSet();
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			ModelEntry entry = (ModelEntry) iter.next();
			set.add(entry);
			addDependentEntries(entry, set);
		}
		fTreeViewer.setSelection(new StructuredSelection(set.toArray()));
	}
	private void handleSelectInJavaSearch() {
		PluginsContentProvider provider =
			(PluginsContentProvider) fTreeViewer.getContentProvider();
		Object[] elements = provider.getElements(fTreeViewer.getInput());
		ArrayList result = new ArrayList();
		for (int i = 0; i < elements.length; i++) {
			Object element = elements[i];
			if (element instanceof ModelEntry) {
				ModelEntry entry = (ModelEntry) element;
				if (entry.isInJavaSearch())
					result.add(entry);
			}
		}
		fTreeViewer.setSelection(new StructuredSelection(result.toArray()));
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
				editorId = IPDEUIConstants.MANIFEST_EDITOR_ID;
			else if (adapter.isSchema())
				editorId = IPDEUIConstants.SCHEMA_EDITOR_ID;
		}
		try {
			if (editorId == null || editorId.equals("@system"))  //$NON-NLS-1$
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
		handleOpenTextEditor(adapter, IPDEUIConstants.MANIFEST_EDITOR_ID);
	}
	
	private void handleOpenSchemaEditor(FileAdapter adapter) {
		handleOpenTextEditor(adapter, IPDEUIConstants.SCHEMA_EDITOR_ID);
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
			.showWhile(fTreeViewer.getTree().getDisplay(), new Runnable() {
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
			adapter.setEditorId("@system");  //$NON-NLS-1$
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
		String text = "";  //$NON-NLS-1$
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
		fTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				handleDoubleClick();
			}
		});
	}

	/**
	 * @see IWorkbenchPart#setFocus()
	 */
	public void setFocus() {
		fTreeViewer.getTree().setFocus();
	}
	void updateTitle(Object newInput) {
		IConfigurationElement config = getConfigurationElement();
		if (config == null)
			return;
		
		if (newInput == null
				|| newInput.equals(PDECore.getDefault().getModelManager())) {
			setContentDescription("");  //$NON-NLS-1$
			setTitleToolTip(getTitle());
		} else {
			String viewName = config.getAttribute("name");  //$NON-NLS-1$
			String name = ((LabelProvider) fTreeViewer.getLabelProvider()).getText(newInput);
			setContentDescription(viewName + ": " + name);  //$NON-NLS-1$
			setTitleToolTip(getInputPath(newInput));
		}
	}
	private String getInputPath(Object input) {
		if (input instanceof FileAdapter) {
			return "file: " + ((FileAdapter) input).getFile().getAbsolutePath();  //$NON-NLS-1$
		}
		if (input instanceof ModelEntry) {
			IPluginModelBase model = ((ModelEntry) input).getActiveModel();
			return "plugin: " + model.getInstallLocation();  //$NON-NLS-1$
		}
		return "";  //$NON-NLS-1$
	}
	
	private void updateContentDescription() {
		String total = Integer.toString(((PluginModelManager)fTreeViewer.getInput()).getEntries().length);
		String visible = Integer.toString(fTreeViewer.getTree().getItemCount());
		setContentDescription(NLS.bind(PDEUIMessages.PluginsView_description, visible, total)); 
	}

}
