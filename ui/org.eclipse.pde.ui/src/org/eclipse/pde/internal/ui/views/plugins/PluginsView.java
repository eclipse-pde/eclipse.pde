/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Les Jones <lesojones@gmail.com> - bug 191365
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.OpenAction;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.DependencyManager;
import org.eclipse.pde.internal.core.FileAdapter;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.IPluginModelListener;
import org.eclipse.pde.internal.core.ModelFileAdapter;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDEState;
import org.eclipse.pde.internal.core.PluginModelDelta;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.JarEntryEditorInput;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.refactoring.PDERefactoringAction;
import org.eclipse.pde.internal.ui.refactoring.RefactoringActionFactory;
import org.eclipse.pde.internal.ui.util.SourcePluginFilter;
import org.eclipse.pde.internal.ui.views.dependencies.OpenPluginDependenciesAction;
import org.eclipse.pde.internal.ui.views.dependencies.OpenPluginReferencesAction;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.eclipse.ui.progress.IElementCollector;
import org.eclipse.ui.progress.PendingUpdateAdapter;
import org.osgi.resource.Resource;

public class PluginsView extends ViewPart implements IPluginModelListener {

	private static final String DEFAULT_EDITOR_ID = "org.eclipse.ui.DefaultTextEditor"; //$NON-NLS-1$
	private static final String HIDE_WRKSPC = "hideWorkspace"; //$NON-NLS-1$
	private static final String HIDE_EXENABLED = "hideEnabledExternal"; //$NON-NLS-1$
	private static final String SHOW_EXDISABLED = "showDisabledExternal"; //$NON-NLS-1$
	private TreeViewer fTreeViewer;
	private DrillDownAdapter fDrillDownAdapter;
	private final IPropertyChangeListener fPropertyListener;
	private Action fOpenAction;
	private Action fHideExtDisabledFilterAction;
	private Action fHideExtEnabledFilterAction;
	private Action fHideWorkspaceFilterAction;
	private Action fOpenManifestAction;
	private Action fOpenSchemaAction;
	private Action fOpenSystemEditorAction;
	private Action fOpenClassFileAction;
	private Action fOpenTextEditorAction;
	private Action fSelectDependentAction;
	private Action fSelectInJavaSearchAction;
	private Action fSelectAllAction;
	private PDERefactoringAction fRefactorAction;
	private CollapseAllAction fCollapseAllAction;
	private final DisabledFilter fHideExtEnabledFilter = new DisabledFilter(true);
	private final DisabledFilter fHideExtDisabledFilter = new DisabledFilter(false);
	private final WorkspaceFilter fHideWorkspaceFilter = new WorkspaceFilter();
	private final JavaFilter fJavaFilter = new JavaFilter();
	private CopyToClipboardAction fCopyAction;
	private Clipboard fClipboard;
	private Object fRoot = null;

	static class DisabledFilter extends ViewerFilter {

		boolean fEnabled;

		DisabledFilter(boolean enabled) {
			fEnabled = enabled;
		}

		@Override
		public boolean select(Viewer v, Object parent, Object element) {
			if (element instanceof IPluginModelBase model) {
				return model.getUnderlyingResource() != null || model.isEnabled() != fEnabled;
			}
			return true;
		}
	}

	static class WorkspaceFilter extends ViewerFilter {
		@Override
		public boolean select(Viewer v, Object parent, Object element) {
			return !(element instanceof IPluginModelBase model) || model.getUnderlyingResource() == null;
		}
	}

	static class JavaFilter extends ViewerFilter {
		@Override
		public boolean select(Viewer v, Object parent, Object element) {
			if (element instanceof IPackageFragment packageFragment) {
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
			setText(PDEUIMessages.PluginsView_CollapseAllAction_label);
			setDescription(PDEUIMessages.PluginsView_CollapseAllAction_description);
			setToolTipText(PDEUIMessages.PluginsView_CollapseAllAction_tooltip);
			setImageDescriptor(PDEPluginImages.DESC_COLLAPSE_ALL);
		}

		@Override
		public void run() {
			fTreeViewer.collapseAll();
		}
	}

	/**
	 * Constructor for PluginsView.
	 */
	public PluginsView() {
		fPropertyListener = event -> {
			String property = event.getProperty();
			if (property.equals(IPreferenceConstants.PROP_SHOW_OBJECTS)) {
				fTreeViewer.refresh();
			}
		};
	}

	@Override
	public void dispose() {
		PDECore.getDefault().getModelManager().removePluginModelListener(this);
		PDECore.getDefault().getSearchablePluginsManager().removePluginModelListener(this);
		PDEPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(fPropertyListener);
		if (fClipboard != null) {
			fClipboard.dispose();
			fClipboard = null;
		}
		super.dispose();
	}

	@Override
	public void createPartControl(Composite parent) {
		fTreeViewer = new TreeViewer(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		fTreeViewer.setUseHashlookup(true);
		fDrillDownAdapter = new DrillDownAdapter(fTreeViewer);
		fTreeViewer.setContentProvider(new PluginsContentProvider(this));
		fTreeViewer.setLabelProvider(new PluginsLabelProvider());
		// need custom comparator so that way PendingUpdateAdapter is at the top.  Using regular PluginComparator the PendingUpdateAdapter
		// will be sorted to the bottom.  When it is removed after the table is initialized, the focus will go to the last item in the table (bug 216339)
		fTreeViewer.setComparator(new ListUtil.PluginComparator() {

			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				if (e1 instanceof PendingUpdateAdapter)
					return -1;
				else if (e2 instanceof PendingUpdateAdapter)
					return 1;
				return super.compare(viewer, e1, e2);
			}

		});
		initDragAndDrop();
		makeActions();
		initFilters();
		IActionBars actionBars = getViewSite().getActionBars();
		contributeToActionBars(actionBars);
		registerGlobalActions(actionBars);
		hookContextMenu();
		hookDoubleClickAction();
		fTreeViewer.addSelectionChangedListener(e -> handleSelectionChanged(e.getSelection()));
		PDECore.getDefault().getSearchablePluginsManager().addPluginModelListener(this);
		fTreeViewer.setInput(fRoot = getDeferredTreeRoot());

		PDECore.getDefault().getModelManager().addPluginModelListener(this);
		PDEPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(fPropertyListener);
		getViewSite().setSelectionProvider(fTreeViewer);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(fTreeViewer.getControl(), IHelpContextIds.PLUGINS_VIEW);
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
		actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), fSelectAllAction);
		actionBars.setGlobalActionHandler(ActionFactory.RENAME.getId(), new Action() {
			@Override
			public void run() {
				IStructuredSelection selection = fTreeViewer.getStructuredSelection();
				if (selection.size() == 1) {
					Object element = selection.getFirstElement();
					if (element instanceof IPluginModelBase) {
						fRefactorAction.setSelection(element);
						fRefactorAction.run();
						return;
					}
				}
				Display.getDefault().beep();
			}
		});
	}

	private void makeActions() {
		fClipboard = new Clipboard(fTreeViewer.getTree().getDisplay());
		fOpenAction = new Action() {
			@Override
			public void run() {
				handleDoubleClick();
			}
		};
		fOpenAction.setText(PDEUIMessages.PluginsView_open);

		fHideExtDisabledFilterAction = new Action() {
			@Override
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
			@Override
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
			@Override
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
			@Override
			public void run() {
				handleOpenTextEditor(getSelectedFile(), null);
			}
		};
		fOpenTextEditorAction.setText(PDEUIMessages.PluginsView_textEditor);
		fOpenTextEditorAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FILE));

		fOpenSystemEditorAction = new Action() {
			@Override
			public void run() {
				handleOpenSystemEditor(getSelectedFile());
			}
		};
		fOpenSystemEditorAction.setText(PDEUIMessages.PluginsView_systemEditor);

		fOpenManifestAction = new Action() {
			@Override
			public void run() {
				handleOpenManifestEditor(getSelectedFile());
			}
		};
		fOpenManifestAction.setText(PDEUIMessages.PluginsView_manifestEditor);

		fOpenSchemaAction = new Action() {
			@Override
			public void run() {
				handleOpenSchemaEditor(getSelectedFile());
			}
		};
		fOpenSchemaAction.setText(PDEUIMessages.PluginsView_schemaEditor);

		fCopyAction = new CopyToClipboardAction(fClipboard);
		fCopyAction.setText(PDEUIMessages.PluginsView_copy);

		fSelectDependentAction = new Action() {
			@Override
			public void run() {
				handleSelectDependencies();
			}
		};
		fSelectDependentAction.setText(PDEUIMessages.PluginsView_dependentPlugins);

		fSelectInJavaSearchAction = new Action() {
			@Override
			public void run() {
				handleSelectInJavaSearch();
			}
		};
		fSelectInJavaSearchAction.setText(PDEUIMessages.PluginsView_pluginsInJavaSearch);

		fSelectAllAction = new Action() {
			@Override
			public void run() {
				super.run();
				fTreeViewer.getTree().selectAll();
			}
		};
		fSelectAllAction.setText(PDEUIMessages.PluginsView_SelectAllAction_label);

		fCollapseAllAction = new CollapseAllAction();

		fOpenClassFileAction = new OpenAction(getViewSite());

		fRefactorAction = RefactoringActionFactory.createRefactorPluginIdAction();
	}

	private FileAdapter getSelectedFile() {
		Object obj = getSelectedObject();
		return obj instanceof FileAdapter fileAdapter ? fileAdapter : null;
	}

	private IPluginModelBase getEnclosingModel() {
		Object obj = getSelectedObject();
		if (obj instanceof IPluginModelBase pluginModel) {
			return pluginModel;
		}
		if (obj instanceof FileAdapter file && file.isManifest()
				&& file.getParent() instanceof ModelFileAdapter modelFileAdapter) {
			return modelFileAdapter.getModel();
		}
		return null;
	}

	private Object getSelectedObject() {
		IStructuredSelection selection = fTreeViewer.getStructuredSelection();
		if (selection.isEmpty() || selection.size() != 1) {
			return null;
		}
		return selection.getFirstElement();
	}

	private void fillContextMenu(IMenuManager manager) {
		IStructuredSelection selection = fTreeViewer.getStructuredSelection();

		boolean allowRefactoring = false;
		if (selection.size() == 1) {
			Object sobj = selection.getFirstElement();
			boolean addSeparator = false;
			if (sobj instanceof IPluginModelBase model) {
				File file = new File(model.getInstallLocation());
				if (file.isFile() || model.getUnderlyingResource() != null) {
					manager.add(fOpenAction);
				}
				if (model.getUnderlyingResource() != null)
					allowRefactoring = true;
			}
			if (sobj instanceof FileAdapter fileAdapter && !fileAdapter.isDirectory()) {
				manager.add(fOpenAction);
				MenuManager openWithMenu = new MenuManager(PDEUIMessages.PluginsView_openWith);
				fillOpenWithMenu(openWithMenu, sobj);
				manager.add(openWithMenu);
				addSeparator = true;
			}
			if (isOpenableStorage(sobj)) {
				manager.add(fOpenAction);
				addSeparator = true;
			}
			if (sobj instanceof IClassFile) {
				manager.add(fOpenClassFileAction);
				addSeparator = true;
			}
			IPluginModelBase entry = getEnclosingModel();
			if (entry != null) {
				Action action = new OpenPluginDependenciesAction(entry);
				manager.add(action);
				manager.add(new Separator());

				action = new OpenPluginReferencesAction(entry);
				action.setText(PDEUIMessages.SearchAction_references);
				action.setImageDescriptor(PDEPluginImages.DESC_CALLERS);
				manager.add(action);
				addSeparator = true;
			}
			if (addSeparator)
				manager.add(new Separator());
		}
		if (!selection.isEmpty()) {
			if (isShowInApplicable()) {
				String showInLabel = PDEUIMessages.PluginsView_showIn;
				IBindingService bindingService = PlatformUI.getWorkbench().getAdapter(IBindingService.class);
				if (bindingService != null) {
					String keyBinding = bindingService.getBestActiveBindingFormattedFor(IWorkbenchCommandConstants.NAVIGATE_SHOW_IN_QUICK_MENU);
					if (keyBinding != null) {
						showInLabel += '\t' + keyBinding;
					}
				}
				IMenuManager showInMenu = new MenuManager(showInLabel);
				showInMenu.add(ContributionItemFactory.VIEWS_SHOW_IN.create(getViewSite().getWorkbenchWindow()));

				manager.add(showInMenu);
				manager.add(new Separator());
			}
			if (ImportActionGroup.canImport(selection)) {
				ImportActionGroup actionGroup = new ImportActionGroup();
				actionGroup.setContext(new ActionContext(selection));
				actionGroup.fillContextMenu(manager);
				manager.add(new Separator());
			}

			JavaSearchActionGroup actionGroup = new JavaSearchActionGroup();
			actionGroup.setContext(new ActionContext(selection));
			actionGroup.fillContextMenu(manager);
		}
		fCopyAction.setSelection(selection);
		manager.add(fCopyAction);
		IMenuManager selectionMenu = new MenuManager(PDEUIMessages.PluginsView_select);
		manager.add(selectionMenu);
		if (!selection.isEmpty())
			selectionMenu.add(fSelectDependentAction);
		selectionMenu.add(fSelectInJavaSearchAction);
		selectionMenu.add(fSelectAllAction);
		manager.add(new Separator());
		if (allowRefactoring) {
			fRefactorAction.setSelection(selection.getFirstElement());
			manager.add(fRefactorAction);
			manager.add(new Separator());
		}
		fDrillDownAdapter.addNavigationActions(manager);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	public boolean isShowInApplicable() {
		IStructuredSelection selection = fTreeViewer.getStructuredSelection();
		if (selection.isEmpty()) {
			return false;
		}
		return selection.toList().stream()
				.allMatch(e -> e instanceof IPluginModelBase plugin && plugin.getUnderlyingResource() != null);
	}

	private void fillOpenWithMenu(IMenuManager manager, Object obj) {
		FileAdapter adapter = (FileAdapter) obj;
		String editorId = adapter.getEditorId();

		String fileName = adapter.getFile().getName();
		String lcFileName = fileName.toLowerCase(Locale.ENGLISH);
		ImageDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(fileName);
		if (lcFileName.equals(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR) || lcFileName.equals(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR) || lcFileName.equals(ICoreConstants.MANIFEST_FILENAME_LOWER_CASE)) {
			fOpenManifestAction.setImageDescriptor(desc);
			manager.add(fOpenManifestAction);
			manager.add(new Separator());
			fOpenManifestAction.setChecked(editorId != null && editorId.equals(IPDEUIConstants.MANIFEST_EDITOR_ID));
		}
		if (lcFileName.endsWith(".mxsd") || lcFileName.endsWith(".exsd")) { //$NON-NLS-1$ //$NON-NLS-2$
			fOpenSchemaAction.setImageDescriptor(desc);
			manager.add(fOpenSchemaAction);
			manager.add(new Separator());
			fOpenSchemaAction.setChecked(editorId != null && editorId.equals(IPDEUIConstants.SCHEMA_EDITOR_ID));
		}
		manager.add(fOpenTextEditorAction);
		fOpenTextEditorAction.setChecked(editorId == null || editorId.equals(DEFAULT_EDITOR_ID));
		fOpenSystemEditorAction.setImageDescriptor(desc);
		fOpenSystemEditorAction.setChecked(editorId != null && editorId.equals("@system")); //$NON-NLS-1$
		manager.add(fOpenSystemEditorAction);
	}

	protected void initDragAndDrop() {
		int ops = DND.DROP_COPY | DND.DROP_MOVE;
		Transfer[] transfers = new Transfer[] {FileTransfer.getInstance()};
		fTreeViewer.addDragSupport(ops, transfers, new PluginsDragAdapter(fTreeViewer));
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

		if (PDECore.getDefault().getModelManager().isInitialized()) {
			PDEState state = PDECore.getDefault().getModelManager().getState();
			fTreeViewer.addFilter(new SourcePluginFilter(state));
		} else {
			// when TP state is not initialized yet defer computation to
			// background
			// job and apply the filter when it is available
			Job.createSystem("Initialize PDE State", monitor -> { //$NON-NLS-1$
				PDEState state = TargetPlatformHelper.getPDEState();
				Tree tree = fTreeViewer.getTree();
				if (!tree.isDisposed()) {
					tree.getDisplay().asyncExec(() -> fTreeViewer.addFilter(new SourcePluginFilter(state)));
				}
			}).schedule();
		}
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(PluginsView.this::fillContextMenu);
		Menu menu = menuMgr.createContextMenu(fTreeViewer.getControl());
		fTreeViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, fTreeViewer);
	}

	private void handleDoubleClick() {
		Object obj = getSelectedObject();
		if (obj instanceof IPluginModelBase pluginModel) {
			boolean expanded = false;
			// only expand target models
			if (pluginModel.getUnderlyingResource() == null) {
				expanded = fTreeViewer.getExpandedState(obj);
				fTreeViewer.setExpandedState(obj, !expanded);
			}
			if (fTreeViewer.getExpandedState(obj) == expanded) {
				// not expandable, open editor
				ManifestEditor.openPluginEditor((IPluginModelBase) obj);
			}
		} else if (obj instanceof FileAdapter adapter) {
			if (adapter.isDirectory()) {
				fTreeViewer.setExpandedState(adapter, !fTreeViewer.getExpandedState(adapter));
				return;
			}
			String editorId = adapter.getEditorId();
			if (editorId != null && editorId.equals("@system")) //$NON-NLS-1$
				handleOpenSystemEditor(adapter);
			else
				handleOpenTextEditor(adapter, editorId);
		} else if (obj instanceof IClassFile) {
			fOpenClassFileAction.run();
		} else if (isOpenableStorage(obj)) {
			handleOpenStorage((IStorage) obj);
		}
	}

	private static boolean isOpenableStorage(Object storage) {
		if (storage instanceof IJarEntryResource resource) {
			return resource.isFile();
		}
		return storage instanceof IStorage;
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

	private void handleSelectDependencies() {
		IStructuredSelection selection = fTreeViewer.getStructuredSelection();
		if (selection.isEmpty()) {
			return;
		}
		List<IPluginModelBase> models = Arrays.stream(selection.toArray()).filter(IPluginModelBase.class::isInstance)
				.map(IPluginModelBase.class::cast).toList();
		Set<BundleDescription> set = DependencyManager.getSelfAndDependencies(models);
		ArrayList<IPluginModelBase> result = new ArrayList<>(set.size());
		for (Resource bundle : set) {
			IPluginModelBase model = PluginRegistry.findModel(bundle);
			if (model != null)
				result.add(model);
		}
		fTreeViewer.setSelection(new StructuredSelection(result.toArray()));
	}

	private void handleSelectInJavaSearch() {
		PluginsContentProvider provider = (PluginsContentProvider) fTreeViewer.getContentProvider();
		Object[] elements = provider.getElements(fTreeViewer.getInput());
		ArrayList<Object> result = new ArrayList<>();
		for (Object element : elements) {
			if (element instanceof IPluginModelBase pluginModel) {
				String id = pluginModel.getPluginBase().getId();
				if (PDECore.getDefault().getSearchablePluginsManager().isInJavaSearch(id))
					result.add(element);
			}
		}
		fTreeViewer.setSelection(new StructuredSelection(result.toArray()));
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
			if (editorId == null || editorId.equals("@system")) //$NON-NLS-1$
				editorId = DEFAULT_EDITOR_ID;
			IFileStore store = EFS.getStore(adapter.getFile().toURI());
			IEditorInput in = new FileStoreEditorInput(store);
			page.openEditor(in, editorId);
			adapter.setEditorId(editorId);
		} catch (CoreException e) {
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
		}
		// Start busy indicator.
		final File file = localFile;
		final boolean[] result = new boolean[] { false };
		BusyIndicator.showWhile(fTreeViewer.getTree().getDisplay(), () -> {
			// Open file using shell.
			String path = file.getAbsolutePath();
			result[0] = Program.launch(path);
		});

		// ShellExecute returns whether call was successful
		if (!result[0]) {
			PDEPlugin.logException(new PartInitException(NLS.bind(PDEUIMessages.PluginsView_unableToOpen, file.getName())));
		} else {
			adapter.setEditorId("@system"); //$NON-NLS-1$
		}
	}

	private File getLocalCopy(File file) throws IOException {
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
		try (FileOutputStream fos = new FileOutputStream(tmpFile); FileInputStream fis = new FileInputStream(file)) {
			byte[] cbuffer = new byte[1024];
			int read = 0;

			while (read != -1) {
				read = fis.read(cbuffer);
				if (read != -1)
					fos.write(cbuffer, 0, read);
			}
			fos.flush();
		}
		tmpFile.setReadOnly();
		return tmpFile;
	}

	private void handleSelectionChanged(ISelection selection) {
		String text = ""; //$NON-NLS-1$
		Object obj = getSelectedObject();
		if (obj instanceof IPluginModelBase pluginModel) {
			text = pluginModel.getInstallLocation();
		}
		if (obj instanceof FileAdapter fileAdapter) {
			text = fileAdapter.getFile().getAbsolutePath();
		}
		getViewSite().getActionBars().getStatusLineManager().setMessage(text);
	}

	private void hookDoubleClickAction() {
		fTreeViewer.addDoubleClickListener(event -> handleDoubleClick());
	}

	@Override
	public void setFocus() {
		fTreeViewer.getTree().setFocus();
	}

	void updateTitle(Object newInput) {
		IConfigurationElement config = getConfigurationElement();
		if (config == null)
			return;

		if (newInput == null || newInput.equals(PDECore.getDefault().getModelManager())) {
			updateContentDescription();
			setTitleToolTip(getTitle());
		} else {
			setTitleToolTip(getInputPath(newInput));
		}
	}

	private String getInputPath(Object input) {
		if (input instanceof FileAdapter fileAdapter) {
			return "file: " + fileAdapter.getFile().getAbsolutePath(); //$NON-NLS-1$
		}
		if (input instanceof IPluginModelBase model) {
			return "plugin: " + model.getInstallLocation(); //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$
	}

	protected void updateContentDescription() {
		String total = null;
		String visible = null;

		PluginModelManager manager = PDECore.getDefault().getModelManager();
		if (manager.isInitialized()) {
			// Only show the correct values if the PDE is already initialized
			// (N.B. PluginRegistry.getAllModels() would call the init if allowed to execute)
			total = Integer.toString(PluginRegistry.getAllModels().length);
			visible = Integer.toString(fTreeViewer.getTree().getItemCount());
		} else {
			// defaults to be shown if the PDE isn't initialized
			total = PDEUIMessages.PluginsView_TotalPlugins_unknown;
			visible = "0"; //$NON-NLS-1$
		}
		setContentDescription(NLS.bind(PDEUIMessages.PluginsView_description, visible, total));
	}

	@Override
	public void modelsChanged(final PluginModelDelta delta) {
		if (fTreeViewer == null || fTreeViewer.getTree().isDisposed())
			return;

		fTreeViewer.getTree().getDisplay().asyncExec(() -> {
			int kind = delta.getKind();
			if (fTreeViewer.getTree().isDisposed())
				return;
			if ((kind & PluginModelDelta.CHANGED) != 0 || (kind & PluginModelDelta.REMOVED) != 0) {
				// Don't know exactly what change -
				// the safest way out is to refresh
				fTreeViewer.refresh();
			} else if ((kind & PluginModelDelta.ADDED) != 0) {
				ModelEntry[] added = delta.getAddedEntries();
				for (ModelEntry element : added) {
					IPluginModelBase[] models = getModels(element);
					for (IPluginModelBase model : models) {
						if (isVisible(model))
							fTreeViewer.add(fRoot, model);
					}
				}
			}
			updateContentDescription();
		});
	}

	private IPluginModelBase[] getModels(ModelEntry entry) {
		return (entry.hasWorkspaceModels()) ? entry.getWorkspaceModels() : entry.getExternalModels();
	}

	private boolean isVisible(IPluginModelBase entry) {
		ViewerFilter[] filters = fTreeViewer.getFilters();
		return Stream.of(filters).allMatch(f -> f.select(fTreeViewer, fRoot, entry));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> adapter) {
		if (isShowInApplicable()) {
			if (adapter == IShowInSource.class && isShowInApplicable()) {
				return (T) getShowInSource();
			} else if (adapter == IShowInTargetList.class) {
				return (T) getShowInTargetList();
			}
		}

		return super.getAdapter(adapter);
	}

	/**
	 * Returns the <code>IShowInSource</code> for this view.
	 * @return the <code>IShowInSource</code>
	 */
	protected IShowInSource getShowInSource() {
		return () -> {
			IStructuredSelection selection = fTreeViewer.getStructuredSelection();
			IStructuredSelection resources;
			if (selection.isEmpty()) {
				resources = null;
			} else {
				Stream<IPluginModelBase> plugins = selection.toList().stream()
						.filter(IPluginModelBase.class::isInstance).map(IPluginModelBase.class::cast);
				List<IResource> resourceList = plugins.map(IPluginModelBase::getUnderlyingResource).toList();
				resources = new StructuredSelection(resourceList);
			}
			return new ShowInContext(fTreeViewer.getInput(), resources);
		};
	}

	/**
	 * Returns the <code>IShowInTargetList</code> for this view.
	 * @return the <code>IShowInTargetList</code>
	 */
	protected IShowInTargetList getShowInTargetList() {
		return () -> new String[] {JavaUI.ID_PACKAGES, IPageLayout.ID_PROJECT_EXPLORER};
	}

	/*
	 * Returns an IDeferredWorkbenchAdapater which will be used to load this view in the background if the ModelManager is not fully initialized
	 */
	private IDeferredWorkbenchAdapter getDeferredTreeRoot() {
		return new IDeferredWorkbenchAdapter() {

			@Override
			public void fetchDeferredChildren(Object object, IElementCollector collector, IProgressMonitor monitor) {
				Object[] bases = getChildren(object);
				collector.add(bases, monitor);
				monitor.done();
			}

			@Override
			public ISchedulingRule getRule(Object object) {
				return null;
			}

			@Override
			public boolean isContainer() {
				return true;
			}

			@Override
			public Object[] getChildren(Object o) {
				return PDECore.getDefault().getModelManager().getAllModels();
			}

			@Override
			public ImageDescriptor getImageDescriptor(Object object) {
				return null;
			}

			@Override
			public String getLabel(Object o) {
				return PDEUIMessages.PluginsView_deferredLabel0;
			}

			@Override
			public Object getParent(Object o) {
				return null;
			}

		};
	}
}
