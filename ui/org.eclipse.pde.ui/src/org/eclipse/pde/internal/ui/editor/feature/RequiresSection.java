/*******************************************************************************
 *  Copyright (c) 2000, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alexander Kurtakov <akurtako@redhat.com> - bug 415649
 *     Marc-Andre Laperle (Ericsson) - Handle double click (Bug 328467)
 *     Fabian Miehe - Bug 440420
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487943
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 351356
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.feature.FeatureImport;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.dialogs.FeatureSelectionDialog;
import org.eclipse.pde.internal.ui.dialogs.PluginSelectionDialog;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.actions.SortAction;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.osgi.service.prefs.Preferences;

public class RequiresSection extends TableSection implements IPluginModelListener, IFeatureModelListener {

	private static final int RECOMPUTE_IMPORT = 3;
	private static final int REMOVE = 2;
	private static final int NEW_FEATURE = 1;
	private static final int NEW_PLUGIN = 0;

	private Button fSyncButton;

	private TableViewer fPluginViewer;

	private Action fDeleteAction;

	private SortAction fSortAction;
	private Action fOpenAction;

	class ImportContentProvider implements IStructuredContentProvider {
		@Override
		public Object[] getElements(Object parent) {
			if (parent instanceof IFeature) {
				IFeatureImport[] imports = ((IFeature) parent).getImports();
				ArrayList<IFeatureImport> displayable = new ArrayList<>();
				for (IFeatureImport featureImport : imports) {
					if (featureImport.isPatch())
						continue;
					displayable.add(featureImport);
				}

				return displayable.toArray();
			}
			return new Object[0];
		}
	}

	public RequiresSection(FeatureDependenciesPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[] {PDEUIMessages.FeatureEditor_RequiresSection_plugin, PDEUIMessages.FeatureEditor_RequiresSection_feature, PDEUIMessages.FeatureEditor_RequiresSection_remove, PDEUIMessages.FeatureEditor_RequiresSection_compute});
		getSection().setText(PDEUIMessages.FeatureEditor_RequiresSection_title);
		getSection().setDescription(PDEUIMessages.FeatureEditor_RequiresSection_desc);
		getTablePart().setEditable(false);
	}

	@Override
	public void commit(boolean onSave) {
		super.commit(onSave);
	}

	@Override
	public void createClient(Section section, FormToolkit toolkit) {

		final IFeatureModel model = (IFeatureModel) getPage().getModel();
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData data = new GridData(GridData.FILL_BOTH);
		section.setLayoutData(data);

		Composite container = createClientContainer(section, 2, toolkit);

		fSyncButton = toolkit.createButton(container, PDEUIMessages.FeatureEditor_RequiresSection_sync, SWT.CHECK);
		// syncButton.setSelection(true);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 2;
		fSyncButton.setLayoutData(gd);

		fSyncButton.addSelectionListener(widgetSelectedAdapter(e -> {
			IEclipsePreferences eclipsePrefs = Platform.getPreferencesService().getRootNode();
			Preferences prefs = eclipsePrefs.node(Plugin.PLUGIN_PREFERENCE_SCOPE).node(IPDEUIConstants.PLUGIN_ID);
			prefs.putBoolean(model.getFeature().getLabel(), fSyncButton.getSelection());
		}));

		createViewerPartControl(container, SWT.MULTI, 2, toolkit);

		TablePart tablePart = getTablePart();
		fPluginViewer = tablePart.getTableViewer();
		fPluginViewer.setContentProvider(new ImportContentProvider());
		fPluginViewer.setComparator(ListUtil.NAME_COMPARATOR);
		fPluginViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());

		fDeleteAction = new Action() {
			@Override
			public void run() {
				handleDelete();
			}
		};
		fDeleteAction.setText(PDEUIMessages.Actions_delete_label);

		fOpenAction = new Action(PDEUIMessages.Actions_open_label) {
			@Override
			public void run() {
				handleOpen();
			}
		};
		toolkit.paintBordersFor(container);
		section.setClient(container);
		initialize();
		createSectionToolbar(section, toolkit);
	}

	/**
	 * @param section
	 * @param toolkit
	 */
	private void createSectionToolbar(Section section, FormToolkit toolkit) {

		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
		ToolBar toolbar = toolBarManager.createControl(section);
		final Cursor handCursor = Display.getCurrent().getSystemCursor(SWT.CURSOR_HAND);
		toolbar.setCursor(handCursor);
		// Add sort action to the tool bar
		fSortAction = new SortAction(getStructuredViewerPart().getViewer(), PDEUIMessages.FeatureEditor_RequiresSection_sortAlpha, ListUtil.NAME_COMPARATOR, null, null);

		toolBarManager.add(fSortAction);

		toolBarManager.update(true);

		section.setTextClient(toolbar);
	}

	@Override
	protected void buttonSelected(int index) {
		switch (index) {
			case NEW_PLUGIN :
				handleNewPlugin();
				break;
			case NEW_FEATURE :
				handleNewFeature();
				break;
			case REMOVE :
				handleDelete();
				break;
			case RECOMPUTE_IMPORT :
				recomputeImports();
				break;
		}
	}

	private void handleNewPlugin() {
		BusyIndicator.showWhile(fPluginViewer.getTable().getDisplay(), () -> {
			IPluginModelBase[] allModels = PluginRegistry.getActiveModels();
			ArrayList<IPluginModelBase> newModels = new ArrayList<>();
			for (IPluginModelBase model : allModels) {
				if (canAdd(model))
					newModels.add(model);
			}
			IPluginModelBase[] candidateModels = newModels.toArray(new IPluginModelBase[newModels.size()]);
			PluginSelectionDialog dialog = new PluginSelectionDialog(fPluginViewer.getTable().getShell(), candidateModels, true);
			if (dialog.open() == Window.OK) {
				Object[] models = dialog.getResult();
				try {
					doAdd(models);
				} catch (CoreException e) {
					PDEPlugin.log(e);
				}
			}
		});
	}

	private boolean canAdd(IPluginModelBase candidate) {
		IPluginBase plugin = candidate.getPluginBase();
		if (candidate.isFragmentModel())
			return false;

		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeatureImport[] imports = model.getFeature().getImports();

		for (IFeatureImport featureImport : imports) {
			if (plugin.getId().equals(featureImport.getId()))
				return false;
		}
		// don't show plug-ins that are listed in this feature
		IFeaturePlugin[] fplugins = model.getFeature().getPlugins();
		for (IFeaturePlugin featurePlugin : fplugins) {
			if (plugin.getId().equals(featurePlugin.getId()))
				return false;
		}
		return true;
	}

	private void handleNewFeature() {
		BusyIndicator.showWhile(fPluginViewer.getTable().getDisplay(), () -> {
			IFeatureModel[] allModels = PDECore.getDefault().getFeatureModelManager().getModels();
			ArrayList<IFeatureModel> newModels = new ArrayList<>();
			for (IFeatureModel model : allModels) {
				if (canAdd(model))
					newModels.add(model);
			}
			IFeatureModel[] candidateModels = newModels.toArray(new IFeatureModel[newModels.size()]);
			FeatureSelectionDialog dialog = new FeatureSelectionDialog(fPluginViewer.getTable().getShell(), candidateModels, true);
			if (dialog.open() == Window.OK) {
				Object[] models = dialog.getResult();
				try {
					doAdd(models);
				} catch (CoreException e) {
					PDECore.log(e);
				}
			}
		});
	}

	private void doAdd(Object[] candidates) throws CoreException {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();
		IFeatureImport[] added = new IFeatureImport[candidates.length];
		for (int i = 0; i < candidates.length; i++) {
			FeatureImport fimport = (FeatureImport) model.getFactory().createImport();
			if (candidates[i] instanceof IFeatureModel) {
				IFeatureModel candidate = (IFeatureModel) candidates[i];
				fimport.loadFrom(candidate.getFeature());
			} else { // instanceof IPluginModelBase
				IPluginModelBase candidate = (IPluginModelBase) candidates[i];
				IPluginBase pluginBase = candidate.getPluginBase();
				fimport.setId(pluginBase.getId());
			}
			added[i] = fimport;
		}
		feature.addImports(added);
	}

	private boolean canAdd(IFeatureModel candidate) {
		IFeature cfeature = candidate.getFeature();

		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();

		if (cfeature.getId().equals(feature.getId()) && cfeature.getVersion().equals(feature.getVersion())) {
			return false;
		}

		IFeatureImport[] features = feature.getImports();

		for (IFeatureImport featureImport : features) {
			if (featureImport.getId().equals(cfeature.getId()) && featureImport.getVersion() != null && featureImport.getVersion().equals(cfeature.getVersion()))
				return false;
		}
		return true;
	}

	private void handleDelete() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		if (!model.isEditable()) {
			return;
		}
		IFeature feature = model.getFeature();
		IStructuredSelection selection = fPluginViewer.getStructuredSelection();
		if (selection.isEmpty())
			return;

		try {
			IFeatureImport[] deleted = new IFeatureImport[selection.size()];
			int i = 0;
			for (Iterator<?> iter = selection.iterator(); iter.hasNext();) {
				IFeatureImport iimport = (IFeatureImport) iter.next();
				deleted[i++] = iimport;
			}
			feature.removeImports(deleted);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	@Override
	protected void handleDoubleClick(IStructuredSelection selection) {
		handleOpen();
	}

	private void handleOpen() {
		IStructuredSelection sel = fPluginViewer.getStructuredSelection();
		Object obj = sel.getFirstElement();

		if (obj instanceof FeatureImport) {
			FeatureImport featureImport = (FeatureImport) obj;
			if (featureImport.getType() == IFeatureImport.PLUGIN)
				ManifestEditor.open(featureImport.getPlugin(), false);
			else if (featureImport.getType() == IFeatureImport.FEATURE) {
				IFeature feature = featureImport.getFeature();
				FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
				IFeatureModel model = manager.findFeatureModel(feature.getId(), feature.getVersion());
				FeatureEditor.openFeatureEditor(model);
			}
		}
	}

	@Override
	public void dispose() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		PDECore.getDefault().getModelManager().removePluginModelListener(this);
		PDECore.getDefault().getFeatureModelManager().removeFeatureModelListener(this);
		super.dispose();
	}

	@Override
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			BusyIndicator.showWhile(fPluginViewer.getTable().getDisplay(), () -> handleDelete());
			return true;
		}
		if (actionId.equals(ActionFactory.CUT.getId())) {
			// delete here and let the editor transfer
			// the selection to the clipboard
			handleDelete();
			return false;
		}
		if (actionId.equals(ActionFactory.PASTE.getId())) {
			doPaste();
			return true;
		}
		if (actionId.equals(ActionFactory.SELECT_ALL.getId())) {
			BusyIndicator.showWhile(fPluginViewer.getTable().getDisplay(), () -> handleSelectAll());
			return true;
		}
		return false;
	}

	public void expandTo(Object object) {
		if (object instanceof IFeatureImport) {
			StructuredSelection ssel = new StructuredSelection(object);
			fPluginViewer.setSelection(ssel);
		}
	}

	@Override
	protected void fillContextMenu(IMenuManager manager) {
		IStructuredSelection selection = fPluginViewer.getStructuredSelection();
		if (!selection.isEmpty()) {
			manager.add(fOpenAction);
			manager.add(fDeleteAction);
			manager.add(new Separator());
		}
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
		manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	@Override
	protected void registerPopupMenu(MenuManager popupMenuManager) {
		IEditorSite site = (IEditorSite) getPage().getSite();
		site.registerContextMenu(site.getId() + ".plugins", popupMenuManager, fViewerPart.getViewer(), false); //$NON-NLS-1$
	}

	@Override
	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
		getPage().getManagedForm().fireSelectionChanged(this, selection);
		updateButtons();
	}

	private void updateButtons() {
		TablePart tablePart = getTablePart();
		Table table = tablePart.getTableViewer().getTable();
		TableItem[] tableSelection = table.getSelection();
		boolean hasSelection = tableSelection.length > 0;
		//delete
		tablePart.setButtonEnabled(REMOVE, isEditable() && hasSelection);
	}

	public void initialize() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		refresh();
		if (model.isEditable() == false) {
			getTablePart().setButtonEnabled(NEW_PLUGIN, false);
			getTablePart().setButtonEnabled(NEW_FEATURE, false);
			getTablePart().setButtonEnabled(REMOVE, false);
			getTablePart().setButtonEnabled(RECOMPUTE_IMPORT, false);
			fSyncButton.setEnabled(false);
		}
		IEclipsePreferences eclipsePrefs = Platform.getPreferencesService().getRootNode();
		Preferences prefs = eclipsePrefs.node(Plugin.PLUGIN_PREFERENCE_SCOPE).node(IPDEUIConstants.PLUGIN_ID);
		fSyncButton.setSelection(prefs.getBoolean(model.getFeature().getLabel(), false));
		model.addModelChangedListener(this);
		PDECore.getDefault().getModelManager().addPluginModelListener(this);
		PDECore.getDefault().getFeatureModelManager().addFeatureModelListener(this);
	}

	@Override
	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		} else if (e.getChangeType() == IModelChangedEvent.CHANGE) {
			Object obj = e.getChangedObjects()[0];
			if (obj instanceof IFeatureImport) {
				fPluginViewer.refresh(obj);
			}
		} else {
			Object obj = e.getChangedObjects()[0];
			if (obj instanceof IFeatureImport) {
				if (e.getChangeType() == IModelChangedEvent.INSERT) {
					fPluginViewer.add(e.getChangedObjects());
					if (e.getChangedObjects().length > 0) {
						fPluginViewer.setSelection(new StructuredSelection(e.getChangedObjects()[0]));
					}
				} else
					fPluginViewer.remove(e.getChangedObjects());
			} else if (obj instanceof IFeaturePlugin) {
				if (fSyncButton.getSelection()) {
					recomputeImports();
				}
			}
		}
	}

	private void recomputeImports() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();
		try {
			feature.computeImports();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	@Override
	public void modelsChanged(final PluginModelDelta delta) {
		getSection().getDisplay().asyncExec(() -> {
			if (getSection().isDisposed()) {
				return;
			}
			ModelEntry[] added = delta.getAddedEntries();
			ModelEntry[] removed = delta.getRemovedEntries();
			ModelEntry[] changed = delta.getChangedEntries();
			if (hasModels(added) || hasModels(removed) || hasModels(changed))
				markStale();
		});
	}

	private boolean hasModels(ModelEntry[] entries) {
		if (entries == null)
			return false;
		return entries.length > 0;
	}

	@Override
	public void modelsChanged(final IFeatureModelDelta delta) {
		getSection().getDisplay().asyncExec(() -> {
			if (getSection().isDisposed()) {
				return;
			}
			IFeatureModel[] added = delta.getAdded();
			IFeatureModel[] removed = delta.getRemoved();
			IFeatureModel[] changed = delta.getChanged();
			if (hasModels(added) || hasModels(removed) || hasModels(changed))
				markStale();
		});
	}

	private boolean hasModels(IFeatureModel[] models) {
		if (models == null)
			return false;
		IFeatureModel thisModel = (IFeatureModel) getPage().getModel();
		for (IFeatureModel model : models) {
			if (model != thisModel) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void setFocus() {
		if (fPluginViewer != null)
			fPluginViewer.getTable().setFocus();
	}

	@Override
	public void refresh() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();
		fPluginViewer.setInput(feature);
		updateButtons();
		super.refresh();
	}

	/**
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canPaste(Clipboard)
	 */
	@Override
	public boolean canPaste(Clipboard clipboard) {
		Object[] objects = (Object[]) clipboard.getContents(ModelDataTransfer.getInstance());
		if (objects != null && objects.length > 0) {
			return canPaste(null, objects);
		}
		return false;
	}

	/**
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canPaste(Object,
	 *      Object[])
	 */
	@Override
	protected boolean canPaste(Object target, Object[] objects) {
		for (int i = 0; i < objects.length; i++) {
			if (!(objects[i] instanceof FeatureImport))
				return false;
		}
		return true;
	}

	/**
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doPaste()
	 */
	@Override
	protected void doPaste() {
		Clipboard clipboard = getPage().getPDEEditor().getClipboard();
		Object[] objects = (Object[]) clipboard.getContents(ModelDataTransfer.getInstance());
		if (objects != null && canPaste(null, objects))
			doPaste(null, objects);
	}

	/**
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doPaste(Object,
	 *      Object[])
	 */
	@Override
	protected void doPaste(Object target, Object[] objects) {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();
		if (!model.isEditable()) {
			return;
		}

		IFeatureImport[] imports = new IFeatureImport[objects.length];
		try {
			for (int i = 0; i < objects.length; i++) {
				FeatureImport fImport = (FeatureImport) objects[i];
				fImport.setModel(model);
				fImport.setParent(feature);
				imports[i] = fImport;
			}
			feature.addImports(imports);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}

	}

	void fireSelection() {
		IStructuredSelection sel = fPluginViewer.getStructuredSelection();
		if (!sel.isEmpty()) {
			fPluginViewer.setSelection(fPluginViewer.getStructuredSelection());
		} else if (fPluginViewer.getElementAt(0) != null) {
			fPluginViewer.setSelection(new StructuredSelection(fPluginViewer.getElementAt(0)));
		}
	}

	@Override
	protected boolean createCount() {
		return true;
	}

}
