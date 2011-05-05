/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.feature.FeatureImport;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.dialogs.FeatureSelectionDialog;
import org.eclipse.pde.internal.ui.dialogs.PluginSelectionDialog;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.actions.SortAction;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class RequiresSection extends TableSection implements IPluginModelListener, IFeatureModelListener {
	private Button fSyncButton;

	private TableViewer fPluginViewer;

	private Action fDeleteAction;

	private SortAction fSortAction;

	class ImportContentProvider extends DefaultContentProvider implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (parent instanceof IFeature) {
				IFeatureImport[] imports = ((IFeature) parent).getImports();
				ArrayList displayable = new ArrayList();
				for (int i = 0; i < imports.length; i++) {
					if (imports[i].isPatch())
						continue;
					displayable.add(imports[i]);
				}

				return displayable.toArray();
			}
			return new Object[0];
		}
	}

	public RequiresSection(FeatureDependenciesPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[] {PDEUIMessages.FeatureEditor_RequiresSection_plugin, PDEUIMessages.FeatureEditor_RequiresSection_feature, null, PDEUIMessages.FeatureEditor_RequiresSection_compute});
		getSection().setText(PDEUIMessages.FeatureEditor_RequiresSection_title);
		getSection().setDescription(PDEUIMessages.FeatureEditor_RequiresSection_desc);
		getTablePart().setEditable(false);
	}

	public void commit(boolean onSave) {
		super.commit(onSave);
	}

	public void createClient(Section section, FormToolkit toolkit) {

		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData data = new GridData(GridData.FILL_BOTH);
		section.setLayoutData(data);

		Composite container = createClientContainer(section, 2, toolkit);

		fSyncButton = toolkit.createButton(container, PDEUIMessages.FeatureEditor_RequiresSection_sync, SWT.CHECK);
		// syncButton.setSelection(true);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 2;
		fSyncButton.setLayoutData(gd);

		createViewerPartControl(container, SWT.MULTI, 2, toolkit);

		TablePart tablePart = getTablePart();
		fPluginViewer = tablePart.getTableViewer();
		fPluginViewer.setContentProvider(new ImportContentProvider());
		fPluginViewer.setComparator(ListUtil.NAME_COMPARATOR);
		fPluginViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());

		fDeleteAction = new Action() {
			public void run() {
				handleDelete();
			}
		};
		fDeleteAction.setText(PDEUIMessages.Actions_delete_label);
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
		final Cursor handCursor = new Cursor(Display.getCurrent(), SWT.CURSOR_HAND);
		toolbar.setCursor(handCursor);
		// Cursor needs to be explicitly disposed
		toolbar.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if ((handCursor != null) && (handCursor.isDisposed() == false)) {
					handCursor.dispose();
				}
			}
		});
		// Add sort action to the tool bar
		fSortAction = new SortAction(getStructuredViewerPart().getViewer(), PDEUIMessages.FeatureEditor_RequiresSection_sortAlpha, ListUtil.NAME_COMPARATOR, null, null);

		toolBarManager.add(fSortAction);

		toolBarManager.update(true);

		section.setTextClient(toolbar);
	}

	protected void buttonSelected(int index) {
		switch (index) {
			case 0 :
				handleNewPlugin();
				break;
			case 1 :
				handleNewFeature();
				break;
			case 2 :
				break;
			case 3 :
				recomputeImports();
				break;
		}
	}

	private void handleNewPlugin() {
		BusyIndicator.showWhile(fPluginViewer.getTable().getDisplay(), new Runnable() {
			public void run() {
				IPluginModelBase[] allModels = PluginRegistry.getActiveModels();
				ArrayList newModels = new ArrayList();
				for (int i = 0; i < allModels.length; i++) {
					if (canAdd(allModels[i]))
						newModels.add(allModels[i]);
				}
				IPluginModelBase[] candidateModels = (IPluginModelBase[]) newModels.toArray(new IPluginModelBase[newModels.size()]);
				PluginSelectionDialog dialog = new PluginSelectionDialog(fPluginViewer.getTable().getShell(), candidateModels, true);
				if (dialog.open() == Window.OK) {
					Object[] models = dialog.getResult();
					try {
						doAdd(models);
					} catch (CoreException e) {
						PDEPlugin.log(e);
					}
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

		for (int i = 0; i < imports.length; i++) {
			IFeatureImport fimport = imports[i];
			if (plugin.getId().equals(fimport.getId()))
				return false;
		}
		// don't show plug-ins that are listed in this feature
		IFeaturePlugin[] fplugins = model.getFeature().getPlugins();
		for (int i = 0; i < fplugins.length; i++) {
			IFeaturePlugin fplugin = fplugins[i];
			if (plugin.getId().equals(fplugin.getId()))
				return false;
		}
		return true;
	}

	private void handleNewFeature() {
		BusyIndicator.showWhile(fPluginViewer.getTable().getDisplay(), new Runnable() {
			public void run() {
				IFeatureModel[] allModels = PDECore.getDefault().getFeatureModelManager().getModels();
				ArrayList newModels = new ArrayList();
				for (int i = 0; i < allModels.length; i++) {
					if (canAdd(allModels[i]))
						newModels.add(allModels[i]);
				}
				IFeatureModel[] candidateModels = (IFeatureModel[]) newModels.toArray(new IFeatureModel[newModels.size()]);
				FeatureSelectionDialog dialog = new FeatureSelectionDialog(fPluginViewer.getTable().getShell(), candidateModels, true);
				if (dialog.open() == Window.OK) {
					Object[] models = dialog.getResult();
					try {
						doAdd(models);
					} catch (CoreException e) {
						PDECore.log(e);
					}
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

		for (int i = 0; i < features.length; i++) {
			if (features[i].getId().equals(cfeature.getId()) && features[i].getVersion() != null && features[i].getVersion().equals(cfeature.getVersion()))
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
		IStructuredSelection selection = (IStructuredSelection) fPluginViewer.getSelection();
		if (selection.isEmpty())
			return;

		try {
			IFeatureImport[] deleted = new IFeatureImport[selection.size()];
			int i = 0;
			for (Iterator iter = selection.iterator(); iter.hasNext();) {
				IFeatureImport iimport = (IFeatureImport) iter.next();
				deleted[i++] = iimport;
			}
			feature.removeImports(deleted);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void handleSelectAll() {
		IStructuredContentProvider provider = (IStructuredContentProvider) fPluginViewer.getContentProvider();
		Object[] elements = provider.getElements(fPluginViewer.getInput());
		StructuredSelection ssel = new StructuredSelection(elements);
		fPluginViewer.setSelection(ssel);
	}

	public void dispose() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		PDECore.getDefault().getModelManager().removePluginModelListener(this);
		PDECore.getDefault().getFeatureModelManager().removeFeatureModelListener(this);
		super.dispose();
	}

	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			BusyIndicator.showWhile(fPluginViewer.getTable().getDisplay(), new Runnable() {
				public void run() {
					handleDelete();
				}
			});
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
			BusyIndicator.showWhile(fPluginViewer.getTable().getDisplay(), new Runnable() {
				public void run() {
					handleSelectAll();
				}
			});
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

	protected void fillContextMenu(IMenuManager manager) {
		IStructuredSelection selection = (StructuredSelection) fPluginViewer.getSelection();
		if (!selection.isEmpty()) {
			manager.add(fDeleteAction);
			manager.add(new Separator());
		}
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
		manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	protected void registerPopupMenu(MenuManager popupMenuManager) {
		IEditorSite site = (IEditorSite) getPage().getSite();
		site.registerContextMenu(site.getId() + ".plugins", popupMenuManager, fViewerPart.getViewer(), false); //$NON-NLS-1$
	}

	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
		getPage().getManagedForm().fireSelectionChanged(this, selection);
	}

	public void initialize() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		refresh();
		if (model.isEditable() == false) {
			getTablePart().setButtonEnabled(0, false);
			getTablePart().setButtonEnabled(1, false);
			getTablePart().setButtonEnabled(3, false);
			fSyncButton.setEnabled(false);
		}
		model.addModelChangedListener(this);
		PDECore.getDefault().getModelManager().addPluginModelListener(this);
		PDECore.getDefault().getFeatureModelManager().addFeatureModelListener(this);
	}

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

	public void modelsChanged(final PluginModelDelta delta) {
		getSection().getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (getSection().isDisposed()) {
					return;
				}
				ModelEntry[] added = delta.getAddedEntries();
				ModelEntry[] removed = delta.getRemovedEntries();
				ModelEntry[] changed = delta.getChangedEntries();
				if (hasModels(added) || hasModels(removed) || hasModels(changed))
					markStale();
			}
		});
	}

	private boolean hasModels(ModelEntry[] entries) {
		if (entries == null)
			return false;
		return entries.length > 0;
	}

	public void modelsChanged(final IFeatureModelDelta delta) {
		getSection().getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (getSection().isDisposed()) {
					return;
				}
				IFeatureModel[] added = delta.getAdded();
				IFeatureModel[] removed = delta.getRemoved();
				IFeatureModel[] changed = delta.getChanged();
				if (hasModels(added) || hasModels(removed) || hasModels(changed))
					markStale();
			}
		});
	}

	private boolean hasModels(IFeatureModel[] models) {
		if (models == null)
			return false;
		IFeatureModel thisModel = (IFeatureModel) getPage().getModel();
		for (int i = 0; i < models.length; i++) {
			if (models[i] != thisModel) {
				return true;
			}
		}
		return false;
	}

	public void setFocus() {
		if (fPluginViewer != null)
			fPluginViewer.getTable().setFocus();
	}

	public void refresh() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();
		fPluginViewer.setInput(feature);
		super.refresh();
	}

	/**
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canPaste(Clipboard)
	 */
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
		ISelection sel = fPluginViewer.getSelection();
		if (!sel.isEmpty()) {
			fPluginViewer.setSelection(fPluginViewer.getSelection());
		} else if (fPluginViewer.getElementAt(0) != null) {
			fPluginViewer.setSelection(new StructuredSelection(fPluginViewer.getElementAt(0)));
		}
	}

	protected boolean createCount() {
		return true;
	}

}
