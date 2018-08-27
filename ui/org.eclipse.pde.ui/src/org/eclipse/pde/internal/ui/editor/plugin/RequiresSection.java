/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *     Alexander Kurtakov <akurtako@redhat.com> - bug 415649
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487988
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 351356
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.eclipse.pde.internal.core.bundle.BundlePluginModelBase;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.core.text.plugin.PluginBaseNode;
import org.eclipse.pde.internal.core.text.plugin.PluginDocumentNodeFactory;
import org.eclipse.pde.internal.core.util.VersionUtil;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.dialogs.PluginSelectionDialog;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.editor.actions.SortAction;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.search.PluginSearchActionGroup;
import org.eclipse.pde.internal.ui.search.dependencies.UnusedDependenciesAction;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.progress.UIJob;

public class RequiresSection extends TableSection implements IPluginModelListener, IPropertyChangeListener {

	private static final int ADD_INDEX = 0;
	private static final int REMOVE_INDEX = 1;
	private static final int UP_INDEX = 2;
	private static final int DOWN_INDEX = 3;
	private static final int PROPERTIES_INDEX = 4;

	private TableViewer fImportViewer;
	private Vector<ImportObject> fImports;
	private Action fOpenAction;
	private Action fAddAction;
	private Action fRemoveAction;
	private Action fPropertiesAction;
	private Action fSortAction;

	private int fImportInsertIndex;

	class ImportContentProvider implements IStructuredContentProvider {
		@Override
		public Object[] getElements(Object parent) {
			if (fImports == null)
				createImportObjects();
			return fImports.toArray();
		}
	}

	public RequiresSection(DependenciesPage page, Composite parent, String[] labels) {
		super(page, parent, Section.DESCRIPTION, labels);
		getSection().setText(PDEUIMessages.RequiresSection_title);
		boolean fragment = ((IPluginModelBase) getPage().getModel()).isFragmentModel();
		if (fragment)
			getSection().setDescription(PDEUIMessages.RequiresSection_fDesc);
		else
			getSection().setDescription(PDEUIMessages.RequiresSection_desc);
		getTablePart().setEditable(false);
		resetImportInsertIndex();
	}

	@Override
	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = createClientContainer(section, 2, toolkit);
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		TablePart tablePart = getTablePart();
		fImportViewer = tablePart.getTableViewer();

		fImportViewer.setContentProvider(new ImportContentProvider());
		fImportViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		toolkit.paintBordersFor(container);
		makeActions();
		section.setClient(container);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.minimumWidth = 250;
		gd.grabExcessVerticalSpace = true;
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		section.setLayoutData(gd);
		section.setText(PDEUIMessages.RequiresSection_title);
		createSectionToolbar(section, toolkit);
		initialize();
	}

	private void createSectionToolbar(Section section, FormToolkit toolkit) {
		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
		ToolBar toolbar = toolBarManager.createControl(section);
		final Cursor handCursor = Display.getCurrent().getSystemCursor(SWT.CURSOR_HAND);
		toolbar.setCursor(handCursor);
		// Add sort action to the tool bar
		fSortAction = new SortAction(fImportViewer, PDEUIMessages.RequiresSection_sortAlpha, null, null, this);
		toolBarManager.add(fSortAction);

		toolBarManager.update(true);

		section.setTextClient(toolbar);
	}

	@Override
	protected void selectionChanged(IStructuredSelection sel) {
		getPage().getPDEEditor().setSelection(sel);
		updateButtons();
	}

	private void updateButtons() {
		Table table = getTablePart().getTableViewer().getTable();
		TableItem[] selection = table.getSelection();
		boolean hasSelection = selection.length > 0;
		TablePart tablePart = getTablePart();
		tablePart.setButtonEnabled(ADD_INDEX, isEditable());
		updateUpDownButtons();
		if (isBundle())
			tablePart.setButtonEnabled(PROPERTIES_INDEX, selection.length == 1);
		tablePart.setButtonEnabled(REMOVE_INDEX, isEditable() && hasSelection);
	}

	private void updateUpDownButtons() {
		TablePart tablePart = getTablePart();
		if (fSortAction.isChecked()) {
			tablePart.setButtonEnabled(UP_INDEX, false);
			tablePart.setButtonEnabled(DOWN_INDEX, false);
			return;
		}
		Table table = getTablePart().getTableViewer().getTable();
		TableItem[] selection = table.getSelection();
		boolean hasSelection = selection.length > 0;
		boolean canMove = table.getItemCount() > 1 && selection.length == 1;

		tablePart.setButtonEnabled(UP_INDEX, canMove && isEditable() && hasSelection && table.getSelectionIndex() > 0);
		tablePart.setButtonEnabled(DOWN_INDEX, canMove && hasSelection && isEditable() && table.getSelectionIndex() < table.getItemCount() - 1);
	}

	@Override
	protected void handleDoubleClick(IStructuredSelection sel) {
		handleOpen(sel);
	}

	@Override
	protected void buttonSelected(int index) {
		switch (index) {
			case ADD_INDEX :
				handleAdd();
				break;
			case REMOVE_INDEX :
				handleRemove();
				break;
			case UP_INDEX :
				handleUp();
				break;
			case DOWN_INDEX :
				handleDown();
				break;
			case PROPERTIES_INDEX :
				handleOpenProperties();
				break;
		}
	}

	private void handleOpenProperties() {
		Object changeObject = fImportViewer.getStructuredSelection().getFirstElement();
		IPluginImport importObject = ((ImportObject) changeObject).getImport();

		DependencyPropertiesDialog dialog = new DependencyPropertiesDialog(isEditable(), importObject);
		dialog.create();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IHelpContextIds.IMPORTED_PLUGIN_PROPERTIES);
		SWTUtil.setDialogSize(dialog, 400, -1);
		dialog.setTitle(importObject.getId());
		if (dialog.open() == Window.OK && isEditable()) {
			try {
				importObject.setOptional(dialog.isOptional());
				importObject.setReexported(dialog.isReexported());
				importObject.setVersion(dialog.getVersion());
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}

	@Override
	public void dispose() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		PDECore.getDefault().getModelManager().removePluginModelListener(this);
		super.dispose();
	}

	@Override
	public boolean doGlobalAction(String actionId) {

		if (!isEditable()) {
			return false;
		}

		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleRemove();
			return true;
		}
		if (actionId.equals(ActionFactory.CUT.getId())) {
			// delete here and let the editor transfer
			// the selection to the clipboard
			handleRemove();
			return false;
		}
		if (actionId.equals(ActionFactory.PASTE.getId())) {
			doPaste();
			return true;
		}
		return super.doGlobalAction(actionId);
	}

	@Override
	protected boolean canPaste(Object targetObject, Object[] sourceObjects) {
		HashSet<?> existingImportsSet = null;
		// Only import objects that are not already existing imports can be
		// pasted
		for (Object sourceObject : sourceObjects) {
			// Only import objects allowed
			if ((sourceObject instanceof ImportObject) == false) {
				return false;
			}
			// Get the current import objects and store them for searching
			// purposes
			if (existingImportsSet == null) {
				existingImportsSet = PluginSelectionDialog.getExistingImports(getModel(), false);
			}
			// Only import object that do not exist are allowed
			ImportObject importObject = (ImportObject) sourceObject;
			if (existingImportsSet.contains(importObject.getImport().getId())) {
				return false;
			}
		}
		return true;
	}

	@Override
	protected void doPaste(Object targetObject, Object[] sourceObjects) {
		// Get the model
		IPluginModelBase model = getModel();
		IPluginBase pluginBase = model.getPluginBase();
		try {
			// Paste all source objects
			for (Object sourceObject : sourceObjects) {
				if (sourceObject instanceof ImportObject) {
					// Import object
					ImportObject importObject = (ImportObject) sourceObject;
					// Adjust all the source object transient field values to
					// acceptable values
					// TODO: MP: CCP: Remove unnecessary reconnected Plugin attributes
					// This may not be necessary.  The model object is discarded when
					// the import object wrapping the plugin import object is converted
					// into a require bundle object
					importObject.reconnect(model);
					// Add the import object to the plugin
					pluginBase.add(importObject.getImport());
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private IPluginModelBase getModel() {
		return (IPluginModelBase) getPage().getModel();
	}

	@Override
	public boolean setFormInput(Object object) {
		if (object instanceof IPluginImport) {
			ImportObject iobj = new ImportObject((IPluginImport) object);
			fImportViewer.setSelection(new StructuredSelection(iobj), true);
			return true;
		}
		return false;
	}

	@Override
	protected void fillContextMenu(IMenuManager manager) {
		IStructuredSelection selection = fImportViewer.getStructuredSelection();
		manager.add(fAddAction);
		if (!selection.isEmpty()) {
			manager.add(fOpenAction);
		}
		manager.add(new Separator());
		getPage().contextMenuAboutToShow(manager);

		if (!selection.isEmpty())
			manager.add(fRemoveAction);
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
		manager.add(new Separator());

		PluginSearchActionGroup actionGroup = new PluginSearchActionGroup();
		actionGroup.setContext(new ActionContext(selection));
		actionGroup.fillContextMenu(manager);
		if (((IModel) getPage().getModel()).getUnderlyingResource() != null) {
			manager.add(new UnusedDependenciesAction((IPluginModelBase) getPage().getModel(), false));
		}
		if (fPropertiesAction != null && !fImportViewer.getStructuredSelection().isEmpty()) {
			manager.add(new Separator());
			manager.add(fPropertiesAction);
		}
		manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	@Override
	protected void registerPopupMenu(MenuManager popupMenuManager) {
		IEditorSite site = (IEditorSite) getPage().getSite();
		site.registerContextMenu(site.getId() + ".requires", popupMenuManager, fViewerPart.getViewer(), false); //$NON-NLS-1$
	}

	private void handleOpen(ISelection sel) {
		if (sel instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) sel;
			if (ssel.size() == 1) {
				Object obj = ssel.getFirstElement();
				if (obj instanceof ImportObject) {
					IPlugin plugin = ((ImportObject) obj).getPlugin();
					if (plugin != null)
						ManifestEditor.open(plugin, false);
				}
			}
		}
	}

	private void handleRemove() {
		IStructuredSelection ssel = fImportViewer.getStructuredSelection();
		if (!ssel.isEmpty()) {
			IPluginModelBase model = (IPluginModelBase) getPage().getModel();
			IPluginBase pluginBase = model.getPluginBase();
			IPluginImport[] imports = new IPluginImport[ssel.size()];
			int i = 0;
			for (Iterator<?> iter = ssel.iterator(); iter.hasNext(); i++)
				imports[i] = ((ImportObject) iter.next()).getImport();

			try {
				removeImports(pluginBase, imports);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
			updateButtons();
		}
	}

	private void removeImports(IPluginBase base, IPluginImport[] imports) throws CoreException {
		if (base instanceof BundlePluginBase)
			((BundlePluginBase) base).remove(imports);
		else if (base instanceof PluginBase)
			((PluginBase) base).remove(imports);
		else if (base instanceof PluginBaseNode)
			((PluginBaseNode) base).remove(imports);
	}

	private void handleAdd() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		PluginSelectionDialog dialog = new PluginSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), getAvailablePlugins(model), true);
		dialog.create();
		if (dialog.open() == Window.OK) {
			Object[] models = dialog.getResult();
			IPluginImport[] imports = new IPluginImport[models.length];
			try {
				for (int i = 0; i < models.length; i++) {
					IPluginModel candidate = (IPluginModel) models[i];
					String pluginId = candidate.getPlugin().getId();
					IPluginImport importNode = createImport(model.getPluginFactory(), pluginId);
					String version = VersionUtil.computeInitialPluginVersion(candidate.getPlugin().getVersion());
					importNode.setVersion(version);
					imports[i] = importNode;
				}
				addImports(model.getPluginBase(), imports);
			} catch (CoreException e) {
			}
		}
	}

	private IPluginImport createImport(IPluginModelFactory factory, String id) {
		if (factory instanceof AbstractPluginModelBase)
			return ((AbstractPluginModelBase) factory).createImport(id);
		else if (factory instanceof BundlePluginModelBase)
			return ((BundlePluginModelBase) factory).createImport(id);
		else if (factory instanceof PluginDocumentNodeFactory)
			return ((PluginDocumentNodeFactory) factory).createImport(id);
		return null;
	}

	private void addImports(IPluginBase base, IPluginImport[] imports) throws CoreException {
		if (base instanceof BundlePluginBase)
			((BundlePluginBase) base).add(imports);
		else if (base instanceof PluginBase)
			((PluginBase) base).add(imports);
		else if (base instanceof PluginBaseNode)
			((PluginBaseNode) base).add(imports);
	}

	private void handleUp() {
		int index = getTablePart().getTableViewer().getTable().getSelectionIndex();
		if (index < 1)
			return;
		swap(index, index - 1);
	}

	private void handleDown() {
		Table table = getTablePart().getTableViewer().getTable();
		int index = table.getSelectionIndex();
		if (index == table.getItemCount() - 1)
			return;
		swap(index, index + 1);
	}

	public void swap(int index1, int index2) {
		Table table = getTablePart().getTableViewer().getTable();
		IPluginImport dep1 = ((ImportObject) table.getItem(index1).getData()).getImport();
		IPluginImport dep2 = ((ImportObject) table.getItem(index2).getData()).getImport();

		try {
			IPluginModelBase model = (IPluginModelBase) getPage().getModel();
			IPluginBase pluginBase = model.getPluginBase();
			pluginBase.swap(dep1, dep2);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private IPluginModelBase[] getAvailablePlugins(IPluginModelBase model) {
		IPluginModelBase[] plugins = PluginRegistry.getActiveModels(false);
		HashSet<?> existingImports = PluginSelectionDialog.getExistingImports(model, false);
		ArrayList<IPluginModelBase> result = new ArrayList<>();
		for (int i = 0; i < plugins.length; i++) {
			if (!existingImports.contains(plugins[i].getPluginBase().getId())) {
				result.add(plugins[i]);
			}
		}

		if (!existingImports.contains("system.bundle")) //$NON-NLS-1$
			addSystemBundle(result);
		return result.toArray(new IPluginModelBase[result.size()]);
	}

	private void addSystemBundle(java.util.List<IPluginModelBase> list) {
		try {
			ExternalPluginModel model = new ExternalPluginModel();

			// Need Install Location to load model.  Giving it org.eclipse.osgi's install location
			IPluginModelBase osgi = PluginRegistry.findModel("system.bundle"); //$NON-NLS-1$
			if (osgi == null)
				return;
			model.setInstallLocation(osgi.getInstallLocation());

			// Load model from a String representing the contents of an equivalent plugin.xml file
			String pluginInfo = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><plugin id=\"system.bundle\" name=\"System Bundle\"></plugin>"; //$NON-NLS-1$
			InputStream is = new BufferedInputStream(new ByteArrayInputStream(pluginInfo.getBytes()));
			model.load(is, false);

			list.add(model);

		} catch (CoreException e) {
		}
	}

	public void initialize() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		if (model == null)
			return;
		fImportViewer.setInput(model.getPluginBase());
		updateButtons();
		model.addModelChangedListener(this);
		PDECore.getDefault().getModelManager().addPluginModelListener(this);
		fAddAction.setEnabled(model.isEditable());
		fRemoveAction.setEnabled(model.isEditable());
	}

	private void makeActions() {
		fAddAction = new Action(PDEUIMessages.RequiresSection_add) {
			@Override
			public void run() {
				handleAdd();
			}
		};
		fOpenAction = new Action(PDEUIMessages.RequiresSection_open) {
			@Override
			public void run() {
				handleOpen(fImportViewer.getStructuredSelection());
			}
		};
		fRemoveAction = new Action(PDEUIMessages.RequiresSection_delete) {
			@Override
			public void run() {
				handleRemove();
			}
		};
		if (isBundle()) {
			fPropertiesAction = new Action(PDEUIMessages.RequiresSection_properties) {
				@Override
				public void run() {
					handleOpenProperties();
				}
			};
		}
	}

	@Override
	public void refresh() {
		fImports = null;
		fImportViewer.refresh();
		super.refresh();
	}

	@Override
	public void modelChanged(final IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}

		// Model change may have come from a non UI thread such as the auto add dependencies operation. See bug 333533
		UIJob job = new UIJob("Update required bundles") { //$NON-NLS-1$
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {

				if (event.getChangedProperty() == IPluginBase.P_IMPORT_ORDER) {
					refresh();
					updateButtons();
					return Status.OK_STATUS;
				}

				Object[] changedObjects = event.getChangedObjects();
				if (changedObjects[0] instanceof IPluginImport) {
					int index = 0;
					for (Object changedObject : changedObjects) {
						IPluginImport iimport = (IPluginImport) changedObject;
						if (event.getChangeType() == IModelChangedEvent.INSERT) {
							ImportObject iobj = new ImportObject(iimport);
							if (fImports == null) {
								// createImportObjects method will find new addition
								createImportObjects();
							} else {
								int insertIndex = getImportInsertIndex();
								if (insertIndex < 0) {
									// Add Button
									fImports.add(iobj);
								} else {
									// DND
									fImports.add(insertIndex, iobj);
								}
							}
						} else {
							ImportObject iobj = findImportObject(iimport);
							if (iobj != null) {
								if (event.getChangeType() == IModelChangedEvent.REMOVE) {
									if (fImports == null)
										// createImportObjects method will not include the removed import
										createImportObjects();
									else
										fImports.remove(iobj);
									Table table = fImportViewer.getTable();
									index = table.getSelectionIndex();
									fImportViewer.remove(iobj);
								} else {
									fImportViewer.update(iobj, null);
								}
							}
						}
					}
					if (event.getChangeType() == IModelChangedEvent.INSERT) {
						if (changedObjects.length > 0) {
							// Refresh the viewer
							fImportViewer.refresh();
							// Get the last import added to the viewer
							IPluginImport lastImport = (IPluginImport) changedObjects[changedObjects.length - 1];
							// Find the corresponding bundle object for the plug-in import
							ImportObject lastImportObject = findImportObject(lastImport);
							if (lastImportObject != null) {
								fImportViewer.setSelection(new StructuredSelection(lastImportObject));
							}
							fImportViewer.getTable().setFocus();
						}
					} else if (event.getChangeType() == IModelChangedEvent.REMOVE) {
						Table table = fImportViewer.getTable();
						table.setSelection(index < table.getItemCount() ? index : table.getItemCount() - 1);
						updateButtons();
					}
				} else {
					fImportViewer.update(fImportViewer.getStructuredSelection().toArray(), null);
				}

				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.schedule();
	}

	@Override
	public void modelsChanged(PluginModelDelta delta) {
		fImports = null;
		final Control control = fImportViewer.getControl();
		if (!control.isDisposed()) {
			control.getDisplay().asyncExec(() -> {
				if (!control.isDisposed())
					fImportViewer.refresh();
			});
		}
	}

	private ImportObject findImportObject(IPluginImport iimport) {
		if (fImports == null)
			return null;
		for (int i = 0; i < fImports.size(); i++) {
			ImportObject iobj = fImports.get(i);
			if (iobj.getImport().equals(iimport))
				return iobj;
		}
		return null;
	}

	private void createImportObjects() {
		fImports = new Vector<>();
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		IPluginImport[] iimports = model.getPluginBase().getImports();
		for (IPluginImport iimport : iimports) {
			fImports.add(new ImportObject(iimport));
		}
	}

	@Override
	public void setFocus() {
		if (fImportViewer != null)
			fImportViewer.getTable().setFocus();
	}

	private boolean isBundle() {
		return getPage().getPDEEditor().getContextManager().findContext(BundleInputContext.CONTEXT_ID) != null;
	}

	@Override
	protected boolean createCount() {
		return true;
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (fSortAction.equals(event.getSource()) && IAction.RESULT.equals(event.getProperty())) {
			updateUpDownButtons();
		}
	}

	@Override
	protected boolean isDragAndDropEnabled() {
		return true;
	}

	@Override
	public boolean canDragMove(Object[] sourceObjects) {
		if (validateDragMoveSanity(sourceObjects) == false) {
			return false;
		} else if (isTreeViewerSorted()) {
			return false;
		}
		return true;
	}

	/**
	 * @param sourceObjects
	 */
	private boolean validateDragMoveSanity(Object[] sourceObjects) {
		// Validate source
		if (sourceObjects == null) {
			// No objects
			return false;
		} else if (sourceObjects.length != 1) {
			// Multiple selection not supported
			return false;
		} else if ((sourceObjects[0] instanceof ImportObject) == false) {
			// Must be the right type
			return false;
		}
		return true;
	}

	/**
	 * @param targetObject
	 * @param sourceObjects
	 */
	private boolean validateDropMoveSanity(Object targetObject, Object[] sourceObjects) {
		// Validate target object
		if ((targetObject instanceof ImportObject) == false) {
			return false;
		}
		// Validate source objects
		if (validateDragMoveSanity(sourceObjects) == false) {
			return false;
		}
		return true;
	}

	/**
	 * @param sourceImportObject
	 * @param targetImportObject
	 */
	private boolean validateDropMoveModel(ImportObject sourceImportObject, ImportObject targetImportObject) {
		// Objects have to be from the same model
		IPluginModelBase sourceModel = sourceImportObject.getImport().getPluginModel();
		IPluginModelBase targetModel = targetImportObject.getImport().getPluginModel();
		if (sourceModel.equals(targetModel) == false) {
			return false;
		} else if ((getModel().getPluginBase() instanceof BundlePluginBase) == false) {
			return false;
		}
		return true;
	}

	@Override
	public boolean canDropMove(Object targetObject, Object[] sourceObjects, int targetLocation) {
		// Sanity check
		if (validateDropMoveSanity(targetObject, sourceObjects) == false) {
			return false;
		}
		// Multiple selection not supported
		ImportObject sourceImportObject = (ImportObject) sourceObjects[0];
		ImportObject targetImportObject = (ImportObject) targetObject;
		IPluginImport sourcePluginImport = sourceImportObject.getImport();
		IPluginImport targetPluginImport = targetImportObject.getImport();
		// Validate model
		if (validateDropMoveModel(sourceImportObject, targetImportObject) == false) {
			return false;
		}
		// Get the bundle plug-in base
		BundlePluginBase bundlePluginBase = (BundlePluginBase) getModel().getPluginBase();
		// Validate move
		if (targetLocation == ViewerDropAdapter.LOCATION_BEFORE) {
			// Get the previous import of the target
			IPluginImport previousImport = bundlePluginBase.getPreviousImport(targetPluginImport);
			// Ensure the previous element is not the source
			if (sourcePluginImport.equals(previousImport)) {
				return false;
			}
			return true;
		} else if (targetLocation == ViewerDropAdapter.LOCATION_AFTER) {
			// Get the next import of the target
			IPluginImport nextImport = bundlePluginBase.getNextImport(targetPluginImport);
			// Ensure the next import is not the source
			if (sourcePluginImport.equals(nextImport)) {
				return false;
			}
			return true;
		} else if (targetLocation == ViewerDropAdapter.LOCATION_ON) {
			// Not supported
			return false;
		}
		return false;
	}

	@Override
	public void doDropMove(Object targetObject, Object[] sourceObjects, int targetLocation) {
		// Sanity check
		if (validateDropMoveSanity(targetObject, sourceObjects) == false) {
			Display.getDefault().beep();
			return;
		}
		// Multiple selection not supported
		ImportObject sourceImportObject = (ImportObject) sourceObjects[0];
		ImportObject targetImportObject = (ImportObject) targetObject;
		IPluginImport sourcePluginImport = sourceImportObject.getImport();
		IPluginImport targetPluginImport = targetImportObject.getImport();
		// Validate move
		if ((targetLocation == ViewerDropAdapter.LOCATION_BEFORE) || (targetLocation == ViewerDropAdapter.LOCATION_AFTER)) {
			// Do move
			doDropMove(sourceImportObject, sourcePluginImport, targetPluginImport, targetLocation);
		} else if (targetLocation == ViewerDropAdapter.LOCATION_ON) {
			// Not supported
		}
	}

	/**
	 * @param sourceImportObject
	 * @param sourcePluginImport
	 * @param targetPluginImport
	 * @param targetLocation
	 */
	private void doDropMove(ImportObject sourceImportObject, IPluginImport sourcePluginImport, IPluginImport targetPluginImport, int targetLocation) {
		// Remove the original source object
		// Normally we remove the original source object after inserting the
		// serialized source object; however, the imports are removed via ID
		// and having both objects with the same ID co-existing will confound
		// the remove operation
		doDragRemove();
		// Get the bundle plug-in base
		BundlePluginBase bundlePluginBase = (BundlePluginBase) getModel().getPluginBase();
		// Get the index of the target
		int index = bundlePluginBase.getIndexOf(targetPluginImport);
		// Ensure the target index was found
		if (index == -1) {
			return;
		}
		// Determine the location index
		int targetIndex = index;
		if (targetLocation == ViewerDropAdapter.LOCATION_AFTER) {
			targetIndex++;
		}
		// Adjust all the source object transient field values to
		// acceptable values
		sourceImportObject.reconnect(getModel());
		// Store index so that the import can be inserted properly into
		// the table viewer
		setImportInsertIndex(targetIndex);
		// Add source as sibling of target
		try {
			bundlePluginBase.add(sourcePluginImport, targetIndex);
		} catch (CoreException e) {
			// CoreException if model is not editable, which should never be the case
		}
		// Reset the index
		resetImportInsertIndex();
	}

	/**
	 *
	 */
	private void resetImportInsertIndex() {
		fImportInsertIndex = -1;
	}

	/**
	 * @param index
	 */
	private void setImportInsertIndex(int index) {
		fImportInsertIndex = index;
	}

	private int getImportInsertIndex() {
		return fImportInsertIndex;
	}

	/**
	 *
	 */
	private void doDragRemove() {
		// Get the bundle plug-in base
		BundlePluginBase bundlePluginBase = (BundlePluginBase) getModel().getPluginBase();
		// Retrieve the original non-serialized source objects dragged initially
		Object[] sourceObjects = getDragSourceObjects();
		// Validate source objects
		if (validateDragMoveSanity(sourceObjects) == false) {
			return;
		}
		IPluginImport sourcePluginImport = ((ImportObject) sourceObjects[0]).getImport();
		try {
			bundlePluginBase.remove(sourcePluginImport);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private boolean isTreeViewerSorted() {
		if (fSortAction == null) {
			return false;
		}
		return fSortAction.isChecked();
	}

}
