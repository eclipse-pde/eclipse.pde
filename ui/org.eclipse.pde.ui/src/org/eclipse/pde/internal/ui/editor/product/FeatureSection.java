/*******************************************************************************
 * Copyright (c) 2005, 2023 IBM Corporation and others.
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
 *     Hannes Wellmann - Bug 570760 - Option to automatically add requirements to product-launch
 *     Hannes Wellmann - Unify and clean-up Product Editor's PluginSection and FeatureSection
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureChild;
import org.eclipse.pde.internal.core.ifeature.IFeatureImport;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductFeature;
import org.eclipse.pde.internal.core.iproduct.IProductModelFactory;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.dialogs.FeatureSelectionDialog;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.actions.SortAction;
import org.eclipse.pde.internal.ui.editor.feature.FeatureEditor;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.feature.NewFeatureProjectWizard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Section of the product editor on the {@code Contents} page that lists all
 * required Features of this feature-based product.
 */
public class FeatureSection extends AbstractProductContentSection<FeatureSection> implements IPropertyChangeListener {

	private static final List<String> BUTTON_LABELS;
	private static final List<Consumer<FeatureSection>> BUTTON_HANDLERS;

	private static final int BTN_ADD;
	private static final int BTN_ADD_REQ;
	private static final int BTN_REMOVE;
	private static final int BTN_REMOVE_ALL;
	private static final int BTN_PROPS;
	private static final int BTN_ROOT;
	private static final int BTN_UP;
	private static final int BTN_DWN;

	static {
		List<String> labels = new ArrayList<>();
		List<Consumer<FeatureSection>> handlers = new ArrayList<>();

		BTN_ADD = addButton(PDEUIMessages.Product_FeatureSection_add, FeatureSection::handleAdd, labels, handlers);
		BTN_ADD_REQ = addButton(PDEUIMessages.FeatureSection_addRequired, FeatureSection::handleAddRequired, labels,
				handlers);
		BTN_REMOVE = addButton(PDEUIMessages.PluginSection_remove, FeatureSection::handleRemove, labels, handlers);
		BTN_REMOVE_ALL = addButton(PDEUIMessages.Product_PluginSection_removeAll, FeatureSection::handleRemoveAll,
				labels, handlers);
		BTN_PROPS = addButton(PDEUIMessages.Product_FeatureSection_properties, FeatureSection::handleProperties, labels,
				handlers);
		BTN_ROOT = addButton(PDEUIMessages.FeatureSection_toggleRoot, FeatureSection::handleRootToggle, labels,
				handlers);
		BTN_UP = addButton(PDEUIMessages.Product_FeatureSection_up, FeatureSection::handleUp, labels, handlers);
		BTN_DWN = addButton(PDEUIMessages.Product_FeatureSection_down, FeatureSection::handleDown, labels, handlers);

		BUTTON_LABELS = List.copyOf(labels);
		BUTTON_HANDLERS = List.copyOf(handlers);
	}

	private SortAction fSortAction;

	public FeatureSection(PDEFormPage formPage, Composite parent) {
		super(formPage, parent, BUTTON_LABELS, BUTTON_HANDLERS, IProductFeature.class::isInstance);
	}

	@Override
	void populateSection(Section section, Composite container, FormToolkit toolkit) {

		createAutoIncludeRequirementsButton(container, PDEUIMessages.Product_FeatureSection_autoIncludeRequirements);

		configureTable(IProduct::getFeatures, null);

		enableTableButtons(BTN_ADD, BTN_ADD_REQ, BTN_PROPS, BTN_ROOT, BTN_UP, BTN_DWN);
		// remove buttons updated on refresh

		section.setText(PDEUIMessages.Product_FeatureSection_title);
		section.setDescription(PDEUIMessages.Product_FeatureSection_desc);
	}

	@Override
	List<Action> getToolbarActions() {
		Action newFeatureAction = createPushAction(PDEUIMessages.Product_FeatureSection_newFeature,
				PDEPluginImages.DESC_NEWFTRPRJ_TOOL, () -> handleNewFeature());
		fSortAction = new SortAction(getTableViewer(), PDEUIMessages.Product_FeatureSection_sortAlpha, null, null,
				this);
		return List.of(newFeatureAction, fSortAction);
	}

	private void handleRootToggle() {
		List<IProductFeature> selection = getTableViewer().getStructuredSelection().toList();
		boolean nonRootSelected = selection.stream().anyMatch(o -> !o.isRootInstallMode());
		selection.forEach(o -> o.setRootInstallMode(nonRootSelected));
	}

	private void handleProperties() {
		IStructuredSelection ssel = getTableSelection();
		if (ssel.size() == 1 && ssel.getFirstElement() instanceof IProductFeature feature) {
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
		getTableViewer().setSelection(new StructuredSelection(feature));
	}

	@Override
	void handleRemoveAll() {
		IProduct product = getProduct();
		product.removeFeatures(product.getFeatures());
	}

	@Override
	protected void doPaste(Object target, Object[] objects) {
		IProductFeature[] features = filterToArray(Stream.of(objects), IProductFeature.class);
		getProduct().addFeatures(features);
	}

	@Override
	void removeElements(IProduct product, List<Object> elements) {
		IProductFeature[] features = filterToArray(elements.stream(), IProductFeature.class);
		getProduct().removeFeatures(features);
	}

	@Override
	protected void handleDoubleClick(IStructuredSelection selection) {
		if (selection.getFirstElement() instanceof IProductFeature feature) {
			FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
			IFeatureModel model = manager.findFeatureModel(feature.getId(), feature.getVersion());
			FeatureEditor.openFeatureEditor(model);
		}
	}

	private void handleAdd() {
		FeatureSelectionDialog dialog = new FeatureSelectionDialog(PDEPlugin.getActiveWorkbenchShell(),
				getAvailableChoices(getProduct()), true);
		if (dialog.open() == Window.OK) {
			Object[] models = dialog.getResult();
			for (Object model : models) {
				if (model instanceof IFeatureModel featureModel) {
					IFeature feature = featureModel.getFeature();
					addFeature(feature.getId());
				}
			}
		}
	}

	private void handleAddRequired() {
		FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
		IProduct product = getProduct();
		IProductFeature[] currentFeatures = product.getFeatures();
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
			if (!product.containsFeature(id)) {
				addFeature(id);
			}
		}
	}

	private void getFeatureDependencies(IFeatureModel model, Set<String> requiredFeatures) {
		FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
		IFeature feature = model.getFeature();
		for (IFeatureImport featureImport : feature.getImports()) {
			if (featureImport.getType() == IFeatureImport.FEATURE
					&& !requiredFeatures.contains(featureImport.getId())) {
				requiredFeatures.add(featureImport.getId());
				IFeatureModel currentModel = manager.findFeatureModel(featureImport.getId());
				if (currentModel != null) {
					getFeatureDependencies(currentModel, requiredFeatures);
				}
			}
		}
		for (IFeatureChild featureInclude : feature.getIncludedFeatures()) {
			if (!requiredFeatures.contains(featureInclude.getId())) {
				requiredFeatures.add(featureInclude.getId());
				IFeatureModel currentModel = manager.findFeatureModel(featureInclude.getId());
				if (currentModel != null) {
					getFeatureDependencies(currentModel, requiredFeatures);
				}
			}
		}
	}

	private static IFeatureModel[] getAvailableChoices(IProduct product) {
		IFeatureModel[] models = PDECore.getDefault().getFeatureModelManager().getModels();
		ArrayList<IFeatureModel> list = new ArrayList<>();
		for (IFeatureModel model : models) {
			String id = model.getFeature().getId();
			if (id != null && !product.containsFeature(id)) {
				list.add(model);
			}
		}
		return list.toArray(new IFeatureModel[list.size()]);
	}

	@Override
	void updateButtons(boolean updateRemove, boolean updateRemoveAll) {

		updateRemoveButtons(updateRemove ? BTN_REMOVE : -1, updateRemoveAll ? BTN_REMOVE_ALL : -1);

		TablePart tablePart = getTablePart();
		Table table = tablePart.getTableViewer().getTable();
		TableItem[] tableSelection = table.getSelection();

		boolean isSingleSelection = isEditable() && tableSelection.length == 1;
		boolean canMove = isSingleSelection && table.getItemCount() > 1 && !fSortAction.isChecked();

		tablePart.setButtonEnabled(BTN_PROPS, isSingleSelection);
		tablePart.setButtonEnabled(BTN_ROOT, isEditable() && tableSelection.length > 0);
		tablePart.setButtonEnabled(BTN_UP, canMove && table.getSelectionIndex() > 0);
		tablePart.setButtonEnabled(BTN_DWN, canMove && table.getSelectionIndex() < table.getItemCount() - 1);
	}

	private void handleUp() {
		Table table = getTable();
		int index = table.getSelectionIndex();
		if (index > 0) {
			swap(index, index - 1, table, getProduct());
		}
	}

	private void handleDown() {
		Table table = getTable();
		int index = table.getSelectionIndex();
		if (index < table.getItemCount() - 1) {
			swap(index, index + 1, table, getProduct());
		}
	}

	static void swap(int index1, int index2, Table table, IProduct product) {
		IProductFeature feature1 = ((IProductFeature) table.getItem(index1).getData());
		IProductFeature feature2 = ((IProductFeature) table.getItem(index2).getData());
		product.swap(feature1, feature2);
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (fSortAction.equals(event.getSource()) && IAction.RESULT.equals(event.getProperty())) {
			updateButtons(true, true);
		}
	}
}
