package org.eclipse.pde.internal.ui.view;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModelBase;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.SystemFileEditorInput;
import org.eclipse.pde.internal.ui.preferences.*;
import org.eclipse.pde.internal.ui.preferences.MainPreferencePage;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.pde.internal.ui.wizards.imports.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.part.*;

public class PluginsView extends ViewPart {
	private static final int TEMP_FILE_LIMIT = 10;
	private static final String DEFAULT_EDITOR_ID =
		"org.eclipse.ui.DefaultTextEditor";
	private TreeViewer treeViewer;
	private DrillDownAdapter drillDownAdapter;
	private IPropertyChangeListener propertyListener;
	private Action openAction;
	private Action importBinaryAction;
	private Action importSourceAction;
	private Action disabledFilterAction;
	private Action workspaceFilterAction;
	private Action openManifestAction;
	private Action openSystemEditorAction;
	private Action openTextEditorAction;
	private Action selectDependentAction;
	private Action addToJavaSearchAction;
	private Action removeFromJavaSearchAction;
	private ShowInWorkspaceAction showInNavigatorAction;
	private ShowInWorkspaceAction showInPackagesAction;
	private DisabledFilter disabledFilter = new DisabledFilter();
	private WorkspaceFilter workspaceFilter = new WorkspaceFilter();
	private CopyToClipboardAction copyAction;
	private ArrayList tempFiles;
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
		purgeTempFiles();
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
		;
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
		clipboard = new Clipboard(treeViewer.getTree().getDisplay());
		openAction = new Action() {
			public void run() {
				handleDoubleClick();
			}
		};
		openAction.setText("Open");

		importBinaryAction = new Action() {
			public void run() {
				handleImport(false);
			}
		};
		importBinaryAction.setText("As Binary Project");
		importSourceAction = new Action() {
			public void run() {
				handleImport(true);
			}
		};
		importSourceAction.setText("As Source Project");
		disabledFilterAction = new Action() {
			public void run() {
				boolean checked = disabledFilterAction.isChecked();
				if (checked)
					treeViewer.removeFilter(disabledFilter);
				else
					treeViewer.addFilter(disabledFilter);
				getSettings().put("disabledFilter", !checked);
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
				getSettings().put("workspaceFilter", !checked);
			}
		};
		workspaceFilterAction.setText("Show workspace plug-ins");
		workspaceFilterAction.setChecked(true);

		openTextEditorAction = new Action() {
			public void run() {
				handleOpenTextEditor(getSelectedFile(), null);
			}
		};
		openTextEditorAction.setText("Text Editor");
		openTextEditorAction.setImageDescriptor(
			PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
				ISharedImages.IMG_OBJ_FILE));

		openSystemEditorAction = new Action() {
			public void run() {
				handleOpenSystemEditor(getSelectedFile());
			}
		};
		openSystemEditorAction.setText("System Editor");
		openManifestAction = new Action() {
			public void run() {
				handleOpenManifestEditor(getSelectedFile());
			}
		};
		openManifestAction.setText("PDE Manifest Editor");

		copyAction = new CopyToClipboardAction(clipboard);
		copyAction.setText("Copy");

		selectDependentAction = new Action() {
			public void run() {
				handleSelectDependent();
			}
		};
		selectDependentAction.setText("Select Dependent Plug-ins");

		addToJavaSearchAction = new Action() {
			public void run() {
				handleJavaSearch(true);
			}
		};
		addToJavaSearchAction.setText("Add to Java Search");

		removeFromJavaSearchAction = new Action() {
			public void run() {
				handleJavaSearch(false);
			}
		};
		removeFromJavaSearchAction.setText("Remove from Java Search");

		showInNavigatorAction =
			new ShowInWorkspaceAction(IPageLayout.ID_RES_NAV, treeViewer);
		showInNavigatorAction.setText("Show In Resource Navigator");
		showInPackagesAction =
			new ShowInWorkspaceAction(JavaUI.ID_PACKAGES, treeViewer);
		showInPackagesAction.setText("Show In Packages View");
	}
	private FileAdapter getSelectedFile() {
		Object obj = getSelectedObject();
		if (obj instanceof FileAdapter)
			return (FileAdapter) obj;
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
			if (sobj instanceof FileAdapter
				&& ((FileAdapter) sobj).isDirectory() == false) {
				manager.add(openAction);
				MenuManager openWithMenu = new MenuManager("Open With");
				fillOpenWithMenu(openWithMenu, sobj);
				manager.add(openWithMenu);
				manager.add(new Separator());
			}
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
				MenuManager importMenu = new MenuManager("Import");
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
		if (selection.size() > 0)
			manager.add(selectDependentAction);
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
		manager.add(new Separator("Additions"));
	}
	private void fillOpenWithMenu(IMenuManager manager, Object obj) {
		FileAdapter adapter = (FileAdapter) obj;
		String editorId = adapter.getEditorId();

		String fileName = adapter.getFile().getName();
		ImageDescriptor desc =
			PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(
				fileName);
		if (fileName.equalsIgnoreCase("plugin.xml")
			|| fileName.equalsIgnoreCase("fragment.xml")) {
			openManifestAction.setImageDescriptor(desc);
			manager.add(openManifestAction);
			manager.add(new Separator());
			openManifestAction.setChecked(
				editorId != null
					&& editorId.equals(PDEPlugin.MANIFEST_EDITOR_ID));
		}
		manager.add(openTextEditorAction);
		openTextEditorAction.setChecked(
			editorId == null || editorId.equals(DEFAULT_EDITOR_ID));
		openSystemEditorAction.setImageDescriptor(desc);
		openSystemEditorAction.setChecked(
			editorId != null && editorId.equals("@system"));
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
			new PluginsDragAdapter((ISelectionProvider) treeViewer));
	}

	private IDialogSettings getSettings() {
		IDialogSettings master = PDEPlugin.getDefault().getDialogSettings();
		IDialogSettings section = master.getSection("pluginsView");
		if (section == null) {
			section = master.addNewSection("pluginsView");
		}
		return section;
	}

	private void initFilters() {
		boolean workspace = false;
		boolean disabled = true;
		IDialogSettings settings = getSettings();
		workspace = settings.getBoolean("workspaceFilter");
		disabled = !settings.getBoolean("disabledFilter");
		if (workspace)
			treeViewer.addFilter(workspaceFilter);
		if (disabled)
			treeViewer.addFilter(disabledFilter);
		workspaceFilterAction.setChecked(!workspace);
		disabledFilterAction.setChecked(!disabled);
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
		Object obj = getSelectedObject();
		if (obj instanceof ModelEntry) {
			treeViewer.setExpandedState(obj, !treeViewer.getExpandedState(obj));
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
			if (editorId != null && editorId.equals("@system"))
				handleOpenSystemEditor(adapter);
			else
				handleOpenTextEditor(adapter, editorId);
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
			ArrayList modelIds = new ArrayList();
			IRunnableWithProgress op =
				PluginImportWizard.getImportOperation(
					shell,
					true,
					extractSource,
					models,
					modelIds);
			ProgressMonitorDialog pmd = new ProgressMonitorDialog(shell);
			pmd.run(true, true, op);
			UpdateClasspathAction.doUpdateClasspath(
				new NullProgressMonitor(),
				getWorkspaceCounterparts(modelIds));
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
					boolean useContainers =
						BuildpathPreferencePage.getUseClasspathContainers();
					manager.setInJavaSearch(array, add, useContainers, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};

		ProgressMonitorDialog pmd =
			new ProgressMonitorDialog(treeViewer.getTree().getShell());
		try {
			pmd.run(false, false, op);
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
				manager.findEntry(
					iimport.getId(),
					iimport.getVersion(),
					iimport.getMatch());
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
		if (editorId == null && adapter.isManifest())
			editorId = PDEPlugin.MANIFEST_EDITOR_ID;
		try {
			if (editorId == null || editorId.equals("@system"))
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
					"Unable to open external editor: " + file.getName()));
		} else {
			adapter.setEditorId("@system");
		}
	}

	private File getLocalCopy(File file) throws IOException {
		// create a tmp. copy of this file and make it
		// read-only. This is to ensure that the original
		// file belonging to the external plug-in directories
		// will not be modified. 
		String fileName = file.getName();
		String suffix;
		int dotLoc = fileName.indexOf('.');
		if (dotLoc != -1)
			suffix = fileName.substring(dotLoc);
		else
			suffix = fileName;

		File tmpFile = File.createTempFile("pde", suffix);
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
		registerTempFile(tmpFile);
		return tmpFile;
	}

	private void registerTempFile(File tempFile) {
		if (tempFiles == null)
			tempFiles = new ArrayList();
		else if (tempFiles.size() > TEMP_FILE_LIMIT)
			purgeTempFiles();
		tempFiles.add(tempFile);
	}

	private void purgeTempFiles() {
		if (tempFiles == null)
			return;
		File[] files = (File[]) tempFiles.toArray(new File[tempFiles.size()]);
		for (int i = 0; i < files.length; i++) {
			File tempFile = files[i];
			if (tempFile.delete())
				tempFiles.remove(tempFile);
		}
	}

	private void handleSelectionChanged(ISelection selection) {
		String text = "";
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
		String viewName = config.getAttribute("name");
		if (newInput == null
			|| newInput.equals(PDECore.getDefault().getModelManager())) {
			// restore old
			setTitle(viewName);
			setTitleToolTip(getTitle());
		} else {
			String name =
				((LabelProvider) treeViewer.getLabelProvider()).getText(
					newInput);
			setTitle(viewName + ": " + name);
			setTitleToolTip(getInputPath(newInput));
		}
	}
	private String getInputPath(Object input) {
		if (input instanceof FileAdapter) {
			return "file: " + ((FileAdapter) input).getFile().getAbsolutePath();
		}
		if (input instanceof ModelEntry) {
			IPluginModelBase model = ((ModelEntry) input).getActiveModel();
			return "plugin: " + model.getInstallLocation();
		}
		return "";
	}
	private IPluginModelBase[] getWorkspaceCounterparts(ArrayList modelIds) {

		IPluginModelBase[] allModels =
			PDECore.getDefault().getWorkspaceModelManager().getAllModels();
		ArrayList desiredModels = new ArrayList();
		for (int i = 0; i < allModels.length; i++) {
			if (modelIds.contains(allModels[i].getPluginBase().getId()))
				desiredModels.add(allModels[i]);
		}

		return (IPluginModelBase[]) desiredModels.toArray(
			new IPluginModelBase[desiredModels.size()]);
	}
}