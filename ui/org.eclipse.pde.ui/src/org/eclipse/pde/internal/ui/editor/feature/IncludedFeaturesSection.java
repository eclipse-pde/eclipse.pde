/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *     Fabian Miehe - Bug 440420
 *     Simon Scholz <simon.scholz@vogella.com> - Bug 444808
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487943
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 351356
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.feature.FeatureChild;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.dialogs.FeatureSelectionDialog;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.actions.SortAction;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class IncludedFeaturesSection extends TableSection implements IFeatureModelListener, IPropertyChangeListener {

	private static final int NEW = 0;
	private static final int REMOVE = 1;
	private static final int SYNC = 2;
	private static final int UP = 3;
	private static final int DOWN = 4;

	private TableViewer fIncludesViewer;

	private Action fNewAction;

	private Action fOpenAction;

	private Action fDeleteAction;

	private SortAction fSortAction;

	class IncludedFeaturesContentProvider implements IStructuredContentProvider {
		@Override
		public Object[] getElements(Object parent) {
			if (parent instanceof IFeature) {
				return ((IFeature) parent).getIncludedFeatures();
			}
			return new Object[0];
		}
	}

	public IncludedFeaturesSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[] { PDEUIMessages.FeatureEditor_IncludedFeatures_new,
				PDEUIMessages.FeatureEditor_IncludedFeatures_remove,
				PDEUIMessages.FeatureEditor_SpecSection_synchronize, PDEUIMessages.FeatureEditor_IncludedFeatures_up,
				PDEUIMessages.FeatureEditor_IncludedFeatures_down });
		getSection().setText(PDEUIMessages.FeatureEditor_IncludedFeatures_title);
		getSection().setDescription(PDEUIMessages.FeatureEditor_IncludedFeatures_desc);
		getTablePart().setEditable(false);
		getSection().setLayoutData(new GridData(GridData.FILL_BOTH));
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
		fIncludesViewer = tablePart.getTableViewer();
		fIncludesViewer.setContentProvider(new IncludedFeaturesContentProvider());
		fIncludesViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		fIncludesViewer.setComparator(ListUtil.NAME_COMPARATOR);
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
		fSortAction = new SortAction(getStructuredViewerPart().getViewer(), PDEUIMessages.FeatureEditor_IncludedFeatures_sortAlpha, ListUtil.NAME_COMPARATOR, null, this);
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
			case NEW :
				handleNew();
				break;
			case REMOVE :
				handleDelete();
				break;
			case SYNC:
				handleSynchronize();
				break;
			case UP :
				handleUp();
				break;
			case DOWN :
				handleDown();
				break;
		}
	}

	private void handleSynchronize() {
		final FeatureEditorContributor contributor = (FeatureEditorContributor) getPage().getPDEEditor()
				.getContributor();
		BusyIndicator.showWhile(fIncludesViewer.getControl().getDisplay(),
				() -> contributor.getSynchronizeAction().run());
	}

	@Override
	public void dispose() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		FeatureModelManager mng = PDECore.getDefault().getFeatureModelManager();
		mng.removeFeatureModelListener(this);
		super.dispose();
	}

	@Override
	public boolean setFormInput(Object object) {
		if (object instanceof IFeatureChild) {
			fIncludesViewer.setSelection(new StructuredSelection(object), true);
			return true;
		}
		return false;
	}

	@Override
	protected void fillContextMenu(IMenuManager manager) {
		manager.add(fOpenAction);
		manager.add(new Separator());
		manager.add(fNewAction);
		manager.add(fDeleteAction);
		manager.add(new Separator());
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
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

	private void handleNew() {
		BusyIndicator.showWhile(fIncludesViewer.getTable().getDisplay(), () -> {
			IFeatureModel[] allModels = PDECore.getDefault().getFeatureModelManager().getModels();
			ArrayList<IFeatureModel> newModels = new ArrayList<>();
			for (IFeatureModel model : allModels) {
				if (canAdd(model))
					newModels.add(model);
			}
			IFeatureModel[] candidateModels = newModels.toArray(new IFeatureModel[newModels.size()]);
			FeatureSelectionDialog dialog = new FeatureSelectionDialog(fIncludesViewer.getTable().getShell(),
					candidateModels, true);
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

	public void swap(int index1, int index2) {
		Table table = getTablePart().getTableViewer().getTable();
		IFeatureChild feature1 = ((IFeatureChild) table.getItem(index1).getData());
		IFeatureChild feature2 = ((IFeatureChild) table.getItem(index2).getData());

		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();
		feature.swap(feature1, feature2);
	}

	private void doAdd(Object[] candidates) throws CoreException {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();
		IFeatureChild[] added = new IFeatureChild[candidates.length];
		for (int i = 0; i < candidates.length; i++) {
			IFeatureModel candidate = (IFeatureModel) candidates[i];
			FeatureChild child = (FeatureChild) model.getFactory().createChild();
			child.loadFrom(candidate.getFeature());
			child.setVersion(ICoreConstants.DEFAULT_VERSION);
			added[i] = child;
		}
		feature.addIncludedFeatures(added);
	}

	private boolean canAdd(IFeatureModel candidate) {
		IFeature cfeature = candidate.getFeature();

		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();

		if (cfeature.getId().equals(feature.getId()) && cfeature.getVersion().equals(feature.getVersion())) {
			return false;
		}

		boolean isPatchEditor = ((FeatureEditor) getPage().getEditor()).isPatchEditor();
		if (isPatchEditor && !isFeaturePatch(candidate.getFeature())) {
			return false;
		}

		IFeatureChild[] features = feature.getIncludedFeatures();

		for (IFeatureChild featureChild : features) {
			if (featureChild.getId().equals(cfeature.getId()) && featureChild.getVersion().equals(cfeature.getVersion()))
				return false;
		}
		return true;
	}

	private static boolean isFeaturePatch(IFeature feature) {
		IFeatureImport[] imports = feature.getImports();
		for (IFeatureImport featureImport : imports) {
			if (featureImport.isPatch())
				return true;
		}
		return false;
	}

	private void handleDelete() {
		IStructuredSelection ssel = fIncludesViewer.getStructuredSelection();

		if (ssel.isEmpty())
			return;
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		if (!model.isEditable()) {
			return;
		}
		IFeature feature = model.getFeature();

		try {
			IFeatureChild[] removed = new IFeatureChild[ssel.size()];
			int i = 0;
			for (Iterator<?> iter = ssel.iterator(); iter.hasNext();) {
				IFeatureChild iobj = (IFeatureChild) iter.next();
				removed[i++] = iobj;
			}
			feature.removeIncludedFeatures(removed);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	@Override
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			BusyIndicator.showWhile(fIncludesViewer.getTable().getDisplay(), () -> handleDelete());
			return true;
		}
		if (actionId.equals(ActionFactory.SELECT_ALL.getId())) {
			BusyIndicator.showWhile(fIncludesViewer.getTable().getDisplay(), () -> handleSelectAll());
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
		return false;
	}

	@Override
	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
		updateButtons();
	}

	public void initialize() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		refresh();
		getTablePart().setButtonEnabled(0, model.isEditable());
		model.addModelChangedListener(this);
		FeatureModelManager mng = PDECore.getDefault().getFeatureModelManager();
		mng.addFeatureModelListener(this);
	}

	@Override
	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		Object obj = e.getChangedObjects()[0];
		if (obj instanceof IFeatureChild) {
			if (e.getChangeType() == IModelChangedEvent.CHANGE) {
				fIncludesViewer.refresh();
			} else if (e.getChangeType() == IModelChangedEvent.INSERT) {
				fIncludesViewer.add(e.getChangedObjects());
				if (e.getChangedObjects().length > 0) {
					fIncludesViewer.setSelection(new StructuredSelection(e.getChangedObjects()[0]));
				}
			} else if (e.getChangeType() == IModelChangedEvent.REMOVE) {
				fIncludesViewer.remove(e.getChangedObjects());
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
				BusyIndicator.showWhile(fIncludesViewer.getTable().getDisplay(), () -> handleDelete());
			}
		};
		fDeleteAction.setEnabled(model.isEditable());
		fDeleteAction.setText(PDEUIMessages.Actions_delete_label);
		fOpenAction = new OpenReferenceAction(fIncludesViewer);
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
		if (thisModel == null)
			return false;
		for (IFeatureModel model : models) {
			if (model != thisModel) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void setFocus() {
		if (fIncludesViewer != null)
			fIncludesViewer.getTable().setFocus();
	}

	@Override
	public void refresh() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();
		fIncludesViewer.setInput(feature);
		updateButtons();
		super.refresh();
	}

	private void updateButtons() {
		TablePart tablePart = getTablePart();
		Table table = tablePart.getTableViewer().getTable();
		TableItem[] tableSelection = table.getSelection();
		boolean hasSelection = tableSelection.length > 0;
		//delete
		tablePart.setButtonEnabled(REMOVE, isEditable() && hasSelection);

		// up/down buttons
		boolean canMove = table.getItemCount() > 1 && tableSelection.length == 1 && !fSortAction.isChecked();
		tablePart.setButtonEnabled(UP, canMove && isEditable() && hasSelection && table.getSelectionIndex() > 0);
		tablePart.setButtonEnabled(DOWN, canMove && hasSelection && isEditable() && table.getSelectionIndex() < table.getItemCount() - 1);
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
			if (!(objects[i] instanceof FeatureChild))
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
		ModelDataTransfer modelTransfer = ModelDataTransfer.getInstance();
		Object[] objects = (Object[]) clipboard.getContents(modelTransfer);
		if (objects != null) {
			doPaste(null, objects);
		}
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

		FeatureChild[] fChildren = new FeatureChild[objects.length];
		try {
			for (int i = 0; i < objects.length; i++) {
				if (objects[i] instanceof FeatureChild) {
					FeatureChild fChild = (FeatureChild) objects[i];
					fChild.setModel(model);
					fChild.setParent(feature);
					fChildren[i] = fChild;
				}
			}
			feature.addIncludedFeatures(fChildren);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	void fireSelection() {
		IStructuredSelection sel = fIncludesViewer.getStructuredSelection();
		if (!sel.isEmpty()) {
			fIncludesViewer.setSelection(fIncludesViewer.getStructuredSelection());
		} else if (fIncludesViewer.getElementAt(0) != null) {
			fIncludesViewer.setSelection(new StructuredSelection(fIncludesViewer.getElementAt(0)));
		}
	}

	@Override
	protected boolean createCount() {
		return true;
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (fSortAction.equals(event.getSource()) && IAction.RESULT.equals(event.getProperty())) {
			updateButtons();
		}
	}

}
