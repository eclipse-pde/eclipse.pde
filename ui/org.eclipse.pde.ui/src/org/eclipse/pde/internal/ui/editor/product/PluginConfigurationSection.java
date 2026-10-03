/*******************************************************************************
 * Copyright (c) 2008, 2022 Code 9 Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     Bartosz Michalik <bartosz.michalik@gmail.com> - bug 240737
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 265931
 *     Simon Scholz <simon.scholz@vogella.com> - bug 440275
 *     Karsten Thoms <karsten.thoms@itemis.de> - bug 535554
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.Util;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.iproduct.IPluginConfiguration;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.iproduct.IProductModelFactory;
import org.eclipse.pde.internal.core.iproduct.IProductPlugin;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.dialogs.PluginSelectionDialog;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.launcher.LaunchAction;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class PluginConfigurationSection extends TableSection {
	private static final String DEFAULT = "default"; //$NON-NLS-1$

	private class LabelProvider extends PDELabelProvider {

		@Override
		public Image getColumnImage(Object obj, int index) {
			if (index == 0 && obj instanceof IPluginConfiguration pluginConfig) {
				return super.getColumnImage(PluginRegistry.findModel(pluginConfig.getId()), index);
			}
			return null;
		}

		@Override
		public String getColumnText(Object obj, int index) {
			IPluginConfiguration configuration = (IPluginConfiguration) obj;
			return switch (index) {
				case 0 -> configuration.getId();
				case 1 -> {
					int startLevel = configuration.getStartLevel();
					yield startLevel == 0 ? DEFAULT : Integer.toString(startLevel);
				}
				case 2 -> Boolean.toString(configuration.isAutoStart());
				default -> null;
			};
		}
	}

	private TableViewer fConfigurationsTable;
	private TableEditor fLevelColumnEditor;
	private TableEditor fAutoColumnEditor;

	public PluginConfigurationSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, getButtonLabels());
	}

	private static String[] getButtonLabels() {
		String[] labels = new String[4];
		labels[0] = PDEUIMessages.Product_PluginSection_add;
		labels[1] = PDEUIMessages.Product_PluginSection_recommended;
		labels[2] = PDEUIMessages.PluginSection_remove;
		labels[3] = PDEUIMessages.Product_PluginSection_removeAll;
		return labels;
	}

	@Override
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEUIMessages.ConfigurationPageMock_sectionTitle);
		section.setDescription(PDEUIMessages.ConfigurationPageMock_sectionDesc);
		GridData sectionData = new GridData(SWT.FILL, SWT.FILL, true, true);
		sectionData.horizontalSpan = 2;
		section.setLayoutData(sectionData);
		Composite container = createClientContainer(section, 3, toolkit);
		createViewerPartControl(container, SWT.SINGLE | SWT.FULL_SELECTION, 3, toolkit);
		fConfigurationsTable = getTablePart().getTableViewer();

		final Table table = fConfigurationsTable.getTable();
		final TableColumn column1 = new TableColumn(table, SWT.LEFT);
		column1.setText(PDEUIMessages.PluginConfigurationSection_tablePluginTitle);
		column1.setWidth(300);

		final TableColumn levelColumnEditor = new TableColumn(table, SWT.LEFT);
		levelColumnEditor.setText(PDEUIMessages.EquinoxPluginBlock_levelColumn);

		final TableColumn autoColumnEditor = new TableColumn(table, SWT.LEFT);
		autoColumnEditor.setText(PDEUIMessages.EquinoxPluginBlock_autoColumn);

		table.addControlListener(ControlListener.controlResizedAdapter(e -> {
			int size = table.getSize().x;
			column1.setWidth(size / 7 * 4);
			levelColumnEditor.setWidth(size / 7 * 2);
			autoColumnEditor.setWidth(size / 7 * 1);
		}));

		table.setHeaderVisible(true);
		toolkit.paintBordersFor(container);
		fConfigurationsTable.setLabelProvider(new LabelProvider());
		IProduct product = getProduct();
		IStructuredContentProvider productConfigurationContent = i -> getProduct().getPluginConfigurations();
		fConfigurationsTable.setContentProvider(productConfigurationContent);
		fConfigurationsTable.setInput(product);
		createEditors();

		section.setClient(container);
		getModel().addModelChangedListener(this);
		getTablePart().setButtonEnabled(0, isEditable());
		updateRemoveButtons(true, true);
	}

	@Override
	protected void buttonSelected(int index) {
		switch (index) {
			case 0:
				handleAdd();
				break;
			case 1:
				handleAddDefaults();
				break;
			case 2:
				handleRemove();
				break;
			case 3:
				handleRemoveAll();
				break;
		}
	}

	private void handleAdd() {
		IProduct product = getProduct();
		Set<String> configuredPlugins = getConfiguredPlugins(product);
		IPluginModelBase[] selectablePlugins = LaunchAction.getAllLaunchedPlugins(product)
				.filter(p -> !(p instanceof IFragmentModel) && !configuredPlugins.contains(p.getPluginBase().getId()))
				.toArray(IPluginModelBase[]::new);
		var dialog = new PluginSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), selectablePlugins, true);
		if (dialog.open() == Window.OK) {
			for (Object object : dialog.getResult()) {
				IPluginModelBase pluginModelBase = (IPluginModelBase) object;
				addPlugin(pluginModelBase.getPluginBase().getId());
			}
		}
	}

	private void handleRemove() {
		IStructuredSelection ssel = fConfigurationsTable.getStructuredSelection();
		if (!ssel.isEmpty()) {
			List<IPluginConfiguration> configs = ssel.toList();
			getProduct().removePluginConfigurations(configs.toArray(IPluginConfiguration[]::new));
		}
		clearEditors();
	}

	private void handleRemoveAll() {
		IProduct product = getProduct();
		product.removePluginConfigurations(product.getPluginConfigurations());
		clearEditors();
	}

	private void handleAddDefaults() {
		IProduct product = getProduct();
		Set<String> configuredPluginIDs = getConfiguredPlugins(product);
		Set<String> allPlugins = LaunchAction.getAllLaunchedPlugins(product) //
				.map(IPluginModelBase::getPluginBase).map(IPluginBase::getId).collect(Collectors.toSet());
		// Build a user-presentable description of the plugins and start levels.
		StringBuilder bundlesList = new StringBuilder();
		BundleLauncherHelper.RECOMMENDED_AUTO_START_BUNDLE_LEVELS.forEach((pluginID, autoStartLevel) -> {
			if (!configuredPluginIDs.contains(pluginID) && allPlugins.contains(pluginID)) {
				bundlesList.append('\t');
				bundlesList.append(pluginID);
				bundlesList.append(", "); //$NON-NLS-1$
				if (autoStartLevel > 0) {
					bundlesList.append(PDEUIMessages.EquinoxPluginBlock_levelColumn);
					bundlesList.append(' ');
					bundlesList.append(autoStartLevel);
				} else {
					bundlesList.append(NLS.bind(PDEUIMessages.EquinoxPluginBlock_defaultLevelColumn, DEFAULT));
				}
				bundlesList.append('\n');
			}
		});
		if (!bundlesList.isEmpty()) {
			// Confirm with user
			String message = NLS.bind(PDEUIMessages.Product_PluginSection_RecommendedBundles_message, bundlesList);
			if (MessageDialog.openConfirm(PDEPlugin.getActiveWorkbenchShell(),
					PDEUIMessages.Product_PluginSection_RecommendedBundles_title, message)) {
				List<IPluginConfiguration> pluginConfigs = new ArrayList<>();
				IProductModelFactory factory = product.getModel().getFactory();
				// Build the model objects for the plugins and add to the
				// product model.
				BundleLauncherHelper.RECOMMENDED_AUTO_START_BUNDLE_LEVELS.forEach((pluginID, autoStartLevel) -> {
					IPluginConfiguration configuration = factory.createPluginConfiguration();
					configuration.setId(pluginID);
					if (autoStartLevel > 0) {
						configuration.setStartLevel(autoStartLevel);
					}
					configuration.setAutoStart(true);
					pluginConfigs.add(configuration);
				});
				product.addPluginConfigurations(pluginConfigs.toArray(IPluginConfiguration[]::new));
				showControls();
			}
		} else {
			// The user already had all the recommended bundles
			MessageDialog.openInformation(PDEPlugin.getActiveWorkbenchShell(),
					PDEUIMessages.Product_PluginSection_RecommendedBundles_title,
					PDEUIMessages.Product_PluginSection_NoRecommendedBundles_message);
		}
	}

	@Override
	protected void selectionChanged(IStructuredSelection selection) {
		updateRemoveButtons(true, false);
	}

	private Set<String> getConfiguredPlugins(IProduct product) {
		IPluginConfiguration[] configurations = product.getPluginConfigurations();
		return Arrays.stream(configurations).map(IPluginConfiguration::getId).collect(Collectors.toSet());
	}

	private void addPlugin(String id) {
		IProduct product = getProduct();
		IProductModelFactory factory = product.getModel().getFactory();
		IPluginConfiguration configuration = factory.createPluginConfiguration();
		configuration.setId(id);
		product.addPluginConfigurations(new IPluginConfiguration[] { configuration });
		fConfigurationsTable.setSelection(new StructuredSelection(configuration));
		showControls();
	}

	private void clearEditors() {
		Control oldEditor = fLevelColumnEditor.getEditor();
		if (oldEditor != null && !oldEditor.isDisposed()) {
			oldEditor.dispose();
		}
		oldEditor = fAutoColumnEditor.getEditor();
		if (oldEditor != null && !oldEditor.isDisposed()) {
			oldEditor.dispose();
		}
	}

	private void createEditors() {
		final Table table = fConfigurationsTable.getTable();

		fLevelColumnEditor = new TableEditor(table);
		fLevelColumnEditor.horizontalAlignment = SWT.CENTER;
		fLevelColumnEditor.minimumWidth = 40;
		fLevelColumnEditor.grabHorizontal = true;
		if (Util.isMac()) {
			fLevelColumnEditor.minimumHeight = 27;
		}

		fAutoColumnEditor = new TableEditor(table);
		fAutoColumnEditor.horizontalAlignment = SWT.CENTER;
		fAutoColumnEditor.grabHorizontal = true;
		fAutoColumnEditor.minimumWidth = 50;

		table.addSelectionListener(widgetSelectedAdapter(e -> showControls()));

	}

	private void showControls() {
		// Clean up any previous editor control
		clearEditors();

		// Identify the selected row
		Table table = fConfigurationsTable.getTable();
		IStructuredSelection selection = fConfigurationsTable.getStructuredSelection();
		if (selection.isEmpty()) {
			return;
		}
		final TableItem item = table.getSelection()[0];
		if (item != null && !isEditable()) {
			return;
		}
		if (item != null) {
			final IPluginConfiguration ppc = (IPluginConfiguration) selection.getFirstElement();
			final Spinner spinner = new Spinner(table, SWT.BORDER);

			spinner.setMinimum(0);
			String level = item.getText(1);
			int defaultLevel = BundleLauncherHelper.parseAutoStartLevel(level);
			spinner.setSelection(defaultLevel);
			spinner.addModifyListener(e -> {
				int selection1 = spinner.getSelection();
				item.setText(1, BundleLauncherHelper.autoStartLevelToString(selection1));
				ppc.setStartLevel(selection1);
			});
			fLevelColumnEditor.setEditor(spinner, item, 1);

			final CCombo combo = new CCombo(table, SWT.BORDER | SWT.READ_ONLY);
			// TODO is there need for the default options ??
			combo.setItems(new String[] { Boolean.toString(true), Boolean.toString(false) });
			combo.setText(item.getText(2));
			combo.pack();
			combo.addSelectionListener(widgetSelectedAdapter(e -> {
				item.setText(2, combo.getText());
				ppc.setAutoStart(Boolean.parseBoolean(combo.getText()));
			}));
			fAutoColumnEditor.setEditor(combo, item, 2);
		}
	}

	private IProduct getProduct() {
		return getModel().getProduct();
	}

	private IProductModel getModel() {
		return (IProductModel) getPage().getPDEEditor().getAggregateModel();
	}

	@Override
	public void modelChanged(IModelChangedEvent e) {
		//TODO update modelChanged method
		// No need to call super, handling world changed event here
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			handleGlobalRefresh();
			return;
		}
		Table table = fConfigurationsTable.getTable();
		boolean refreshRemove = false;
		boolean refreshRemoveAll = false;
		if (e.getChangeType() == IModelChangedEvent.INSERT) {
			if (table.getItemCount() == 0) {
				refreshRemoveAll = true;
			}
			for (Object object : e.getChangedObjects()) {
				if (object instanceof IPluginConfiguration) {
					fConfigurationsTable.add(object);
				}
			}
		} else if (e.getChangeType() == IModelChangedEvent.REMOVE) {
			refreshRemove = refreshRemoveAll = true;
			boolean global = false;
			for (Object object : e.getChangedObjects()) {
				if (object instanceof IPluginConfiguration) {
					fConfigurationsTable.remove(object);
				} else if (object instanceof IProductPlugin) {
					global = true;
					break;
				}
			}
			if (global) {
				handleGlobalRefresh();
			}
			// Update Selection
			int count = table.getItemCount();
			if (count == 0) {
				table.deselectAll();
				clearEditors();
			} else {
				int index = table.getSelectionIndex();
				table.setSelection(Math.min(index, count - 1));
			}

		}
		getTablePart().setButtonEnabled(0, isEditable());
		updateRemoveButtons(refreshRemove, refreshRemoveAll);
	}

	private void handleGlobalRefresh() {
		fConfigurationsTable.setInput(getProduct());
		fConfigurationsTable.refresh();
	}

	private void updateRemoveButtons(boolean updateRemove, boolean updateRemoveAll) {
		TablePart tablePart = getTablePart();
		if (updateRemove) {
			ISelection selection = getViewerSelection();
			tablePart.setButtonEnabled(2, isEditable() && !selection.isEmpty());
		}
		int count = fConfigurationsTable.getTable().getItemCount();
		if (updateRemoveAll) {
			tablePart.setButtonEnabled(3, isEditable() && count > 0);
		}
	}

}
