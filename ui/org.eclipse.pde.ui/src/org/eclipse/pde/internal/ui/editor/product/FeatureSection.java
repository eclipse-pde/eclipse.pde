/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.editor.product;

import java.util.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.dialogs.FeatureSelectionDialog;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.actions.SortAction;
import org.eclipse.pde.internal.ui.editor.feature.FeatureEditor;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.feature.NewFeatureProjectWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Section of the product editor on the depedencies page that lists all required
 * features of this feature-based product.
 */
public class FeatureSection extends TableSection implements IPropertyChangeListener {

	private static final int BTN_DWN = 7;
	private static final int BTN_UP = 6;
	private static final int BTN_ROOT = 5;
	private static final int BTN_PROPS = 4;
	private static final int BTN_REMOVE_ALL = 3;
	private static final int BTN_REMOVE = 2;
	private static final int BTN_ADD_REQ = 1;
	private static final int BTN_ADD = 0;
	private SortAction fSortAction;
	private Action fNewFeatureAction;

	class ContentProvider implements IStructuredContentProvider {
		@Override
		public Object[] getElements(Object parent) {
			return getProduct().getFeatures();
		}
	}

	class NewFeatureAction extends Action {

		public NewFeatureAction() {
			super(PDEUIMessages.Product_FeatureSection_newFeature, IAction.AS_PUSH_BUTTON);
			setImageDescriptor(PDEPluginImages.DESC_NEWFTRPRJ_TOOL);
		}

		@Override
		public void run() {
			handleNewFeature();
		}
	}

	private TableViewer fFeatureTable;

	public FeatureSection(PDEFormPage formPage, Composite parent) {
		super(formPage, parent, Section.DESCRIPTION, getButtonLabels());
	}

	private static String[] getButtonLabels() {
		String[] labels = new String[8];
		labels[BTN_ADD] = PDEUIMessages.Product_FeatureSection_add;
		labels[BTN_ADD_REQ] = PDEUIMessages.FeatureSection_addRequired;
		labels[BTN_REMOVE] = PDEUIMessages.Product_FeatureSection_remove;
		labels[BTN_REMOVE_ALL] = PDEUIMessages.Product_PluginSection_removeAll;
		labels[BTN_ROOT] = PDEUIMessages.FeatureSection_toggleRoot;
		labels[BTN_PROPS] = PDEUIMessages.Product_FeatureSection_properties;
		labels[BTN_UP] = PDEUIMessages.Product_FeatureSection_up;
		labels[BTN_DWN] = PDEUIMessages.Product_FeatureSection_down;
		return labels;
	}

	@Override
	protected void createClient(Section section, FormToolkit toolkit) {

		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData sectionData = new GridData(GridData.FILL_BOTH);
		sectionData.verticalSpan = 2;
		section.setLayoutData(sectionData);

		Composite container = createClientContainer(section, 2, toolkit);
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		TablePart tablePart = getTablePart();
		fFeatureTable = tablePart.getTableViewer();
		fFeatureTable.setContentProvider(new ContentProvider());
		fFeatureTable.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		fFeatureTable.setSorter(null);
		GridData data = (GridData) tablePart.getControl().getLayoutData();
		data.minimumWidth = 200;
		fFeatureTable.setInput(PDECore.getDefault().getFeatureModelManager());

		tablePart.setButtonEnabled(0, isEditable());
		tablePart.setButtonEnabled(1, isEditable());
		// remove buttons updated on refresh
		tablePart.setButtonEnabled(4, isEditable());
		tablePart.setButtonEnabled(5, isEditable());
		tablePart.setButtonEnabled(6, isEditable());

		toolkit.paintBordersFor(container);
		section.setClient(container);

		section.setText(PDEUIMessages.Product_FeatureSection_title);
		section.setDescription(PDEUIMessages.Product_FeatureSection_desc); //

		getModel().addModelChangedListener(this);
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
		fNewFeatureAction = new NewFeatureAction();
		toolBarManager.add(fNewFeatureAction);
		fSortAction = new SortAction(fFeatureTable, PDEUIMessages.Product_FeatureSection_sortAlpha, null, null, this);
		toolBarManager.add(fSortAction);

		toolBarManager.update(true);
		section.setTextClient(toolbar);
	}

	@Override
	protected void buttonSelected(int index) {
		switch (index)
			{
			case BTN_ADD:
				handleAdd();
				break;
			case BTN_ADD_REQ:
				handleAddRequired();
				break;
			case BTN_REMOVE:
				handleDelete();
				break;
			case BTN_REMOVE_ALL:
				handleRemoveAll();
				break;
			case BTN_PROPS:
				handleProperties();
				break;
			case BTN_ROOT:
				handleRootToggle();
				break;
			case BTN_UP:
				handleUp();
				break;
			case BTN_DWN:
				handleDown();
				break;
			}
	}

	private void handleRootToggle() {
		boolean nonRootSelected = getViewerSelection().toList().stream()
				.anyMatch(o -> !((IProductFeature) o).isRootInstallMode());
		getViewerSelection().toList().forEach(o -> ((IProductFeature) o).setRootInstallMode(nonRootSelected));
	}

	private void handleProperties() {
		IStructuredSelection ssel = fFeatureTable.getStructuredSelection();
		if (ssel.size() == 1) {
			IProductFeature feature = (IProductFeature) ssel.toArray()[0];
			FeatureProperties dialog = new FeatureProperties(PDEPlugin.getActiveWorkbenchShell(), isEditable(),
					feature.getVersion(), feature.isRootInstallMode());
			dialog.create();
			SWTUtil.setDialogSize(dialog, 400, 200);
			if (dialog.open() == Window.OK) {
				feature.setVersion(dialog.getVersion());
				feature.setRootInstallMode(dialog.getRootInstallMode());
			}
		}
	}

	private void handleNewFeature() {
		NewFeatureProjectWizard wizard = new NewFeatureProjectWizard();
		WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
		dialog.create();
		SWTUtil.setDialogSize(dialog, 400, 500);
		if (dialog.open() == Window.OK) {
			addFeature(wizard.getFeatureId());
		}
	}

	private void addFeature(String id) {
		IProduct product = getProduct();
		IProductModelFactory factory = product.getModel().getFactory();
		IProductFeature feature = factory.createFeature();
		feature.setId(id);
		feature.setVersion(""); //$NON-NLS-1$
		feature.setRootInstallMode(true);
		product.addFeatures(new IProductFeature[] { feature });
	}

	private void handleRemoveAll() {
		IProduct product = getProduct();
		product.removeFeatures(product.getFeatures());
	}

	@Override
	protected void handleDoubleClick(IStructuredSelection selection) {
		handleOpen(selection);
	}

	@Override
	public void dispose() {
		IProductModel model = getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		super.dispose();
	}

	@Override
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleDelete();
			return true;
		}
		if (actionId.equals(ActionFactory.CUT.getId())) {
			handleDelete();
			return false;
		}
		if (actionId.equals(ActionFactory.PASTE.getId())) {
			doPaste();
			return true;
		}
		return super.doGlobalAction(actionId);
	}

	@Override
	protected boolean canPaste(Object target, Object[] objects) {
		for (Object object : objects) {
			if (object instanceof IProductFeature)
				return true;
		}
		return false;
	}

	@Override
	protected void doPaste(Object target, Object[] objects) {
		IProductFeature[] features;
		if (objects instanceof IProductFeature[])
			features = (IProductFeature[]) objects;
		else {
			features = new IProductFeature[objects.length];
			for (int i = 0; i < objects.length; i++)
				if (objects[i] instanceof IProductFeature)
					features[i] = (IProductFeature) objects[i];
		}
		getProduct().addFeatures(features);
	}

	private void handleDelete() {
		IStructuredSelection ssel = fFeatureTable.getStructuredSelection();
		if (!ssel.isEmpty()) {
			Object[] objects = ssel.toArray();
			IProductFeature[] features = new IProductFeature[objects.length];
			System.arraycopy(objects, 0, features, 0, objects.length);
			getProduct().removeFeatures(features);
		}
	}

	@Override
	protected void fillContextMenu(IMenuManager manager) {
		IStructuredSelection ssel = fFeatureTable.getStructuredSelection();
		if (ssel == null)
			return;

		Action openAction = new Action(PDEUIMessages.Product_FeatureSection_open) {
			@Override
			public void run() {
				handleDoubleClick(fFeatureTable.getStructuredSelection());
			}
		};
		openAction.setEnabled(isEditable() && ssel.size() == 1);
		manager.add(openAction);

		manager.add(new Separator());

		Action removeAction = new Action(PDEUIMessages.Product_FeatureSection_remove) {
			@Override
			public void run() {
				handleDelete();
			}
		};
		removeAction.setEnabled(isEditable() && !ssel.isEmpty());
		manager.add(removeAction);

		Action removeAll = new Action(PDEUIMessages.FeatureSection_removeAll) {
			@Override
			public void run() {
				handleRemoveAll();
			}
		};
		removeAll.setEnabled(isEditable());
		manager.add(removeAll);

		manager.add(new Separator());

		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
	}

	private void handleOpen(IStructuredSelection selection) {
		if (!selection.isEmpty()) {
			IProductFeature feature = (IProductFeature) selection.getFirstElement();
			FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
			IFeatureModel model = manager.findFeatureModel(feature.getId(), feature.getVersion());
			FeatureEditor.openFeatureEditor(model);
		}
	}

	private void handleAdd() {
		FeatureSelectionDialog dialog = new FeatureSelectionDialog(PDEPlugin.getActiveWorkbenchShell(),
				getAvailableChoices(), true);
		if (dialog.open() == Window.OK) {
			Object[] models = dialog.getResult();
			for (Object model : models) {
				IFeature feature = ((IFeatureModel) model).getFeature();
				addFeature(feature.getId());
			}
		}
	}

	private void handleAddRequired() {
		FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
		IProductFeature[] currentFeatures = getProduct().getFeatures();
		Set<String> requiredFeatures = new HashSet<>();
		for (IProductFeature feature : currentFeatures) {
			IFeatureModel model = manager.findFeatureModel(feature.getId(), feature.getVersion());
			if (model != null) {
				requiredFeatures.add(feature.getId());
				getFeatureDependencies(model, requiredFeatures);
			}
		}

		for (String id : requiredFeatures) {
			// Do not add features that already exist
			if (!getProduct().containsFeature(id)) {
				addFeature(id);
			}
		}
	}

	private void getFeatureDependencies(IFeatureModel model, Set<String> requiredFeatures) {
		FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
		IFeature feature = model.getFeature();
		IFeatureImport[] featureImports = feature.getImports();
		for (int i = 0; i < featureImports.length; i++) {
			if (featureImports[i].getType() == IFeatureImport.FEATURE) {
				if (!requiredFeatures.contains(featureImports[i].getId())) {
					requiredFeatures.add(featureImports[i].getId());
					IFeatureModel currentModel = manager.findFeatureModel(featureImports[i].getId());
					if (currentModel != null) {
						getFeatureDependencies(currentModel, requiredFeatures);
					}
				}
			}
		}

		IFeatureChild[] featureIncludes = feature.getIncludedFeatures();
		for (int i = 0; i < featureIncludes.length; i++) {
			if (!requiredFeatures.contains(featureIncludes[i].getId())) {
				requiredFeatures.add(featureIncludes[i].getId());
				IFeatureModel currentModel = manager.findFeatureModel(featureIncludes[i].getId());
				if (currentModel != null) {
					getFeatureDependencies(currentModel, requiredFeatures);
				}
			}
		}
	}

	private IFeatureModel[] getAvailableChoices() {
		IFeatureModel[] models = PDECore.getDefault().getFeatureModelManager().getModels();
		IProduct product = getProduct();
		ArrayList<IFeatureModel> list = new ArrayList<>();
		for (IFeatureModel model : models) {
			String id = model.getFeature().getId();
			if (id != null && !product.containsFeature(id)) {
				list.add(model);
			}
		}
		return list.toArray(new IFeatureModel[list.size()]);
	}

	private IProduct getProduct() {
		return getModel().getProduct();
	}

	private IProductModel getModel() {
		return (IProductModel) getPage().getPDEEditor().getAggregateModel();
	}

	@Override
	public void modelChanged(IModelChangedEvent e) {
		Object[] objects = e.getChangedObjects();
		// No need to call super, handling world changed event here
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			handleModelEventWorldChanged(e);
			return;
		} else if (e.getChangeType() == IModelChangedEvent.INSERT) {
			for (Object object : objects) {
				if (object instanceof IProductFeature)
					fFeatureTable.add(object);
			}
		} else if (e.getChangeType() == IModelChangedEvent.REMOVE) {

			Table table = fFeatureTable.getTable();
			int index = table.getSelectionIndex();

			for (Object object : objects) {
				if (object instanceof IProductFeature)
					fFeatureTable.remove(object);
			}

			// Update Selection

			int count = table.getItemCount();

			if (count == 0) {
				// Nothing to select
			} else if (index < count) {
				table.setSelection(index);
			} else {
				table.setSelection(count - 1);
			}

		} else if (e.getChangeType() == IModelChangedEvent.CHANGE) {
			fFeatureTable.refresh();
		}
		updateButtons(false, true);
	}

	/**
	 * @param event
	 */
	private void handleModelEventWorldChanged(IModelChangedEvent event) {
		// This section can get disposed if the configuration is changed from
		// plugins to features or vice versa. Subsequently, the configuration
		// page is removed and readded. In this circumstance, abort the
		// refresh
		if (fFeatureTable.getTable().isDisposed()) {
			return;
		}
		// Reload the input
		fFeatureTable.setInput(PDECore.getDefault().getFeatureModelManager());
		// Perform the refresh
		refresh();
	}

	@Override
	public void refresh() {
		fFeatureTable.refresh();
		updateButtons(true, true);
		super.refresh();
	}

	@Override
	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
		updateButtons(true, false);
	}

	@Override
	public boolean setFormInput(Object input) {
		if (input instanceof IProductFeature) {
			fFeatureTable.setSelection(new StructuredSelection(input), true);
			return true;
		}
		return super.setFormInput(input);
	}

	private void updateButtons(boolean updateRemove, boolean updateRemoveAll) {
		TablePart tablePart = getTablePart();
		Table table = tablePart.getTableViewer().getTable();
		TableItem[] tableSelection = table.getSelection();
		boolean hasSelection = tableSelection.length > 0;
		if (updateRemove) {
			ISelection selection = getViewerSelection();
			tablePart.setButtonEnabled(BTN_REMOVE_ALL, isEditable() && !selection.isEmpty() && selection instanceof IStructuredSelection && ((IStructuredSelection) selection).getFirstElement() instanceof IProductFeature);
		}
		if (updateRemoveAll)
			tablePart.setButtonEnabled(BTN_REMOVE_ALL, isEditable() && fFeatureTable.getTable().getItemCount() > 0);

		tablePart.setButtonEnabled(BTN_PROPS, isEditable() && tableSelection.length == 1);

		tablePart.setButtonEnabled(BTN_ROOT, isEditable() && hasSelection);

		boolean canMove = table.getItemCount() > 1 && tableSelection.length == 1 && !fSortAction.isChecked();
		tablePart.setButtonEnabled(BTN_UP, canMove && isEditable() && hasSelection && table.getSelectionIndex() > 0);
		tablePart.setButtonEnabled(BTN_DWN, canMove && hasSelection && isEditable() && table.getSelectionIndex() < table.getItemCount() - 1);
	}

	@Override
	protected boolean createCount() {
		return true;
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
		IProductFeature feature1 = ((IProductFeature) table.getItem(index1).getData());
		IProductFeature feature2 = ((IProductFeature) table.getItem(index2).getData());

		IProduct product = getProduct();
		product.swap(feature1, feature2);
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (fSortAction.equals(event.getSource()) && IAction.RESULT.equals(event.getProperty())) {
			updateButtons(true, true);
		}
	}
}
