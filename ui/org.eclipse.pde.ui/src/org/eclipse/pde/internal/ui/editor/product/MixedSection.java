/*******************************************************************************
 * Copyright (c) 2023, 2023 Hannes Wellmann and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hannes Wellmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductFeature;
import org.eclipse.pde.internal.core.iproduct.IProductPlugin;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Section of the Product editor on the {@code Contents} page that lists all
 * included features and plug-ins of this mixed product.
 */
public class MixedSection extends AbstractProductContentSection<MixedSection> {

	private static final List<String> BUTTON_LABELS;
	private static final List<Consumer<MixedSection>> BUTTON_HANDLERS;

	private static final int BTN_ADD_PLUGIN;
	private static final int BTN_ADD_FEATURE;
	private static final int BTN_REMOVE;
	private static final int BTN_REMOVE_ALL;
	private static final int BTN_PROPS;
	private static final int BTN_ROOT;
	private static final int BTN_UP;
	private static final int BTN_DWN;

	static {
		List<String> labels = new ArrayList<>();
		List<Consumer<MixedSection>> handlers = new ArrayList<>();

		BTN_ADD_PLUGIN = addButton(PDEUIMessages.Product_MixedSection_AddPlugins, PluginSection::handleAdd, labels,
				handlers);
		BTN_ADD_FEATURE = addButton(PDEUIMessages.Product_MixedSection_AddFeatures, FeatureSection::handleAdd, labels,
				handlers);

		BTN_REMOVE = addButton(PDEUIMessages.PluginSection_remove, MixedSection::handleRemove, labels, handlers);
		BTN_REMOVE_ALL = addButton(PDEUIMessages.Product_PluginSection_removeAll, MixedSection::handleRemoveAll, labels,
				handlers);
		BTN_PROPS = addButton(PDEUIMessages.Product_FeatureSection_properties, MixedSection::handleProperties, labels,
				handlers);
		BTN_ROOT = addButton(PDEUIMessages.FeatureSection_toggleRoot, FeatureSection::handleRootToggle, labels,
				handlers);
		BTN_UP = addButton(PDEUIMessages.Product_FeatureSection_up, MixedSection::handleUp, labels, handlers);
		BTN_DWN = addButton(PDEUIMessages.Product_FeatureSection_down, MixedSection::handleDown, labels, handlers);

		BUTTON_LABELS = List.copyOf(labels);
		BUTTON_HANDLERS = List.copyOf(handlers);
	}

	public MixedSection(PDEFormPage formPage, Composite parent) {
		super(formPage, parent, BUTTON_LABELS, BUTTON_HANDLERS,
				e -> e instanceof IProductFeature || e instanceof IProductPlugin);
	}

	@Override
	void populateSection(Section section, Composite container, FormToolkit toolkit) {

		createAutoIncludeRequirementsButton(container, PDEUIMessages.Product_FeatureSection_autoIncludeRequirements);

		configureTable( // Features first, Plug-ins second
				p -> Stream.of(p.getFeatures(), p.getPlugins()).flatMap(Arrays::stream).toArray(Object[]::new),
				new ViewerComparator() {
					@Override
					public int category(Object element) {
						if (element instanceof IProductFeature) {
							return 1;
						} else if (element instanceof IProductPlugin) {
							return 2;
						} else {
							throw new IllegalArgumentException();
						}
					}

					@Override
					public int compare(Viewer viewer, Object e1, Object e2) {
						return e1 instanceof IProductFeature && e2 instanceof IProductFeature //
								? 0 // use encounter order for Features
								// sort Plug-ins alphabetically
								: super.compare(viewer, e1, e2);
					}
				});

		ColumnViewerToolTipSupport.enableFor(getTableViewer(), ToolTip.NO_RECREATE);
		getTableViewer().setLabelProvider(new ColumnLabelProvider() {
			PDELabelProvider pdeLabelProvider = PDEPlugin.getDefault().getLabelProvider();

			@Override
			public String getText(Object e) {
				return pdeLabelProvider.getText(e);
			}

			@Override
			public Image getImage(Object e) {
				return pdeLabelProvider.getImage(e);
			}

			@Override
			public String getToolTipText(Object element) {
				if (element instanceof IProductPlugin plugin) {
					return NLS.bind(PDEUIMessages.Product_MixedSection_Tooltip_Plugins, plugin.getId());
				} else if (element instanceof IProductFeature feature) {
					return NLS.bind(PDEUIMessages.Product_MixedSection_Tooltip_Features, feature.getId());
				}
				return null;
			}
		});
		enableTableButtons(BTN_ADD_PLUGIN, BTN_ADD_FEATURE, BTN_PROPS, BTN_ROOT, BTN_UP, BTN_DWN);
		// remove buttons updated on refresh

		section.setText(PDEUIMessages.Product_MixedSection_title);
		section.setDescription(PDEUIMessages.Product_MixedSection_desc);
	}

	@Override
	List<Action> getToolbarActions() {
		Action newPluginAction = createPushAction(PDEUIMessages.Product_PluginSection_newPlugin,
				PDEPluginImages.DESC_NEWPPRJ_TOOL, () -> PluginSection.handleNewPlugin(this));
		Action newFragmentAction = createPushAction(PDEUIMessages.Product_PluginSection_newFragment,
				PDEPluginImages.DESC_NEWFRAGPRJ_TOOL, () -> PluginSection.handleNewFragment(this));
		Action newFeatureAction = createPushAction(PDEUIMessages.Product_FeatureSection_newFeature,
				PDEPluginImages.DESC_NEWFTRPRJ_TOOL, () -> FeatureSection.handleNewFeature(this));
		return List.of(newPluginAction, newFragmentAction, newFeatureAction);
	}

	private void handleProperties() {
		FeatureSection.handleProperties(this);
		PluginSection.handleProperties(this);
	}

	@Override
	void removeElements(IProduct product, List<Object> elements) {
		IProductFeature[] features = filterToArray(elements.stream(), IProductFeature.class);
		IProductPlugin[] plugins = filterToArray(elements.stream(), IProductPlugin.class);
		product.removeFeatures(features);
		product.removePlugins(plugins);
	}

	@Override
	void handleRemoveAll() {
		IProduct product = getProduct();
		product.removeFeatures(product.getFeatures());
		product.removePlugins(product.getPlugins());
	}

	@Override
	protected void doPaste(Object target, Object[] objects) {
		IProduct product = getProduct();
		IProductFeature[] features = filterToArray(Stream.of(objects), IProductFeature.class);
		IProductPlugin[] plugins = filterToArray(Stream.of(objects), IProductPlugin.class);
		product.addFeatures(features);
		product.addPlugins(plugins);
	}

	@Override
	void updateButtons(boolean updateRemove, boolean updateRemoveAll) {

		updateRemoveButtons(updateRemove ? BTN_REMOVE : -1, updateRemoveAll ? BTN_REMOVE_ALL : -1);

		TablePart tablePart = getTablePart();
		Table table = tablePart.getTableViewer().getTable();
		List<Object> selection = getTableSelection().toList();

		boolean canMove = isEditable() && selection.size() == 1 && selection.get(0) instanceof IProductFeature
				&& getProduct().getFeatures().length > 1;

		tablePart.setButtonEnabled(BTN_PROPS,
				isEditable() && selection.size() == 1 && !(selection.get(0) instanceof String));
		tablePart.setButtonEnabled(BTN_ROOT,
				isEditable() && !selection.isEmpty() && selection.stream().allMatch(IProductFeature.class::isInstance));
		tablePart.setButtonEnabled(BTN_UP, canMove && canMoveUp(table, table.getSelectionIndex()));
		tablePart.setButtonEnabled(BTN_DWN, canMove && canMoveDown(table, table.getSelectionIndex()));

	}

	private void handleUp() {
		Table table = getTable();
		int index = table.getSelectionIndex();
		if (canMoveUp(table, index)) {
			FeatureSection.swap(index, index - 1, table, getProduct());
		}
	}

	private static boolean canMoveUp(Table table, int index) {
		return 0 < index && table.getItem(index).getData() instanceof IProductFeature;
	}

	private void handleDown() {
		Table table = getTable();
		int index = table.getSelectionIndex();
		if (canMoveDown(table, index)) {
			FeatureSection.swap(index, index + 1, table, getProduct());
		}
	}

	private static boolean canMoveDown(Table table, int index) {
		return index + 1 < table.getItemCount() && table.getItem(index + 1).getData() instanceof IProductFeature;
	}

}
