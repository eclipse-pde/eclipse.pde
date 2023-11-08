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
 *     Fabian Miehe - Bug 440420
 *     Simon Scholz <simon.scholz@vogella.com> - Bug 444808
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487943
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 351356
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.IPluginModelListener;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelDelta;
import org.eclipse.pde.internal.core.feature.FeaturePlugin;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.dialogs.PluginSelectionDialog;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.ModelDataTransfer;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.editor.actions.SortAction;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class PluginSection extends TableSection implements IPluginModelListener {

	private static final int NEW = 0;
	private static final int REMOVE = 1;
	private static final int SYNC = 2;

	private OpenReferenceAction fOpenAction;

	private TableViewer fPluginViewer;

	private Action fNewAction;

	private Action fDeleteAction;

	private SortAction fSortAction;

	static class PluginContentProvider implements IStructuredContentProvider {
		@Override
		public Object[] getElements(Object parent) {
			if (parent instanceof IFeature) {
				return ((IFeature) parent).getPlugins();
			}
			return new Object[0];
		}
	}

	public PluginSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[] {PDEUIMessages.FeatureEditor_PluginSection_new, PDEUIMessages.FeatureEditor_PluginSection_remove, PDEUIMessages.FeatureEditor_SpecSection_synchronize});
		getSection().setText(PDEUIMessages.FeatureEditor_PluginSection_pluginTitle);
		getSection().setDescription(PDEUIMessages.FeatureEditor_PluginSection_pluginDesc);
		getTablePart().setEditable(false);
	}

	@Override
	public void commit(boolean onSave) {
		super.commit(onSave);
	}

	@Override
	public void createClient(Section section, FormToolkit toolkit) {

		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData data = new GridData(GridData.FILL_BOTH);
		section.setLayoutData(data);

		Composite container = createClientContainer(section, 2, toolkit);

		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		TablePart tablePart = getTablePart();
		fPluginViewer = tablePart.getTableViewer();
		fPluginViewer.setContentProvider(new PluginContentProvider());
		fPluginViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		fPluginViewer.setComparator(ListUtil.NAME_COMPARATOR);
		toolkit.paintBordersFor(container);
		makeActions();
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
		fSortAction = new SortAction(getStructuredViewerPart().getViewer(), PDEUIMessages.FeatureEditor_PluginSection_sortAlpha, ListUtil.NAME_COMPARATOR, null, null);

		toolBarManager.add(fSortAction);

		toolBarManager.update(true);

		section.setTextClient(toolbar);
	}

	@Override
	protected void handleDoubleClick(IStructuredSelection selection) {
		fOpenAction.run();
	}

	@Override
	protected void buttonSelected(int index) {
		switch (index) {
			case NEW -> handleNew();
			case REMOVE -> handleDelete();
			case SYNC -> handleSynchronize();
		}
	}

	@Override
	public void dispose() {
		PDECore.getDefault().getModelManager().removePluginModelListener(this);
		super.dispose();
	}

	@Override
	public boolean setFormInput(Object object) {
		if (object instanceof IFeaturePlugin) {
			fPluginViewer.setSelection(new StructuredSelection(object), true);
			return true;
		}
		return false;
	}

	@Override
	protected void fillContextMenu(IMenuManager manager) {
		manager.add(fOpenAction);
		// add new
		manager.add(new Separator());
		manager.add(fNewAction);
		manager.add(fDeleteAction);
		// add delete

		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
	}

	private void handleNew() {
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

	private void doAdd(Object[] candidates) throws CoreException {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();
		IFeaturePlugin[] added = new IFeaturePlugin[candidates.length];
		for (int i = 0; i < candidates.length; i++) {
			IPluginModelBase candidate = (IPluginModelBase) candidates[i];
			FeaturePlugin fplugin = (FeaturePlugin) model.getFactory().createPlugin();
			fplugin.loadFrom(candidate.getPluginBase());
			fplugin.setVersion(ICoreConstants.DEFAULT_VERSION);
			added[i] = fplugin;
		}
		feature.addPlugins(added);
	}

	private boolean canAdd(IPluginModelBase candidate) {
		IPluginBase plugin = candidate.getPluginBase();

		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeaturePlugin[] fplugins = model.getFeature().getPlugins();

		for (IFeaturePlugin featurePlugin : fplugins) {
			if (featurePlugin.getId().equals(plugin.getId()))
				return false;
		}
		return true;
	}

	private void handleDelete() {
		IStructuredSelection ssel = fPluginViewer.getStructuredSelection();

		if (ssel.isEmpty())
			return;
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		if (!model.isEditable()) {
			return;
		}
		IFeature feature = model.getFeature();

		try {
			IFeaturePlugin[] removed = new IFeaturePlugin[ssel.size()];
			int i = 0;
			for (Iterator<?> iter = ssel.iterator(); iter.hasNext();) {
				IFeaturePlugin iobj = (IFeaturePlugin) iter.next();
				removed[i++] = iobj;
			}
			feature.removePlugins(removed);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void handleSynchronize() {
		final FeatureEditorContributor contributor = (FeatureEditorContributor) getPage().getPDEEditor().getContributor();
		BusyIndicator.showWhile(fPluginViewer.getControl().getDisplay(), () -> contributor.getSynchronizeAction().run());
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

	@Override
	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
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
		getTablePart().setButtonEnabled(0, model.isEditable());
		getTablePart().setButtonEnabled(2, model.isEditable());
		model.addModelChangedListener(this);
		PDECore.getDefault().getModelManager().addPluginModelListener(this);
	}

	@Override
	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		Object obj = e.getChangedObjects()[0];
		if (obj instanceof IFeaturePlugin) {
			if (e.getChangeType() == IModelChangedEvent.CHANGE) {
				fPluginViewer.update(obj, null);
			} else if (e.getChangeType() == IModelChangedEvent.INSERT) {
				fPluginViewer.add(e.getChangedObjects());
				if (e.getChangedObjects().length > 0) {
					fPluginViewer.setSelection(new StructuredSelection(e.getChangedObjects()[0]));
				}
			} else if (e.getChangeType() == IModelChangedEvent.REMOVE) {
				fPluginViewer.remove(e.getChangedObjects());
			}
		}
	}

	private void makeActions() {
		IModel model = (IModel) getPage().getModel();
		fNewAction = new Action() {
			@Override
			public void run() {
				handleNew();
			}
		};
		fNewAction.setText(PDEUIMessages.Menus_new_label);
		fNewAction.setEnabled(model.isEditable());

		fDeleteAction = new Action() {
			@Override
			public void run() {
				BusyIndicator.showWhile(fPluginViewer.getTable().getDisplay(), () -> handleDelete());
			}
		};
		fDeleteAction.setText(PDEUIMessages.Actions_delete_label);
		fDeleteAction.setEnabled(model.isEditable());
		fOpenAction = new OpenReferenceAction(fPluginViewer);
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
			if (hasPluginModels(added) || hasPluginModels(removed) || hasPluginModels(changed))
				markStale();
		});
	}

	private boolean hasPluginModels(ModelEntry[] entries) {
		if (entries == null)
			return false;
		return true;
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
			if (!(objects[i] instanceof FeaturePlugin))
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
		if (!model.isEditable()) {
			return;
		}
		IFeature feature = model.getFeature();
		FeaturePlugin[] fPlugins = new FeaturePlugin[objects.length];
		try {
			for (int i = 0; i < objects.length; i++) {
				FeaturePlugin fPlugin = (FeaturePlugin) objects[i];
				fPlugin.setModel(model);
				fPlugin.setParent(feature);
				fPlugins[i] = fPlugin;
			}
			feature.addPlugins(fPlugins);
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
