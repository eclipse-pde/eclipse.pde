/*******************************************************************************
 * Copyright (c) 2008, 2010 Code 9 Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     Bartosz Michalik <bartosz.michalik@gmail.com> - bug 240737
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 265931
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import java.util.ArrayList;
import org.eclipse.jface.util.Util;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class PluginConfigurationSection extends TableSection {

	private class ContentProvider implements IStructuredContentProvider {

		private IProduct fProduct;

		ContentProvider() {
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
			return fProduct.getPluginConfigurations();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
		 */
		public void dispose() {

		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (oldInput == newInput)
				return;
			fProduct = (IProduct) newInput;
			//TODO refresh
		}

	}

	private class LabelProvider extends PDELabelProvider {

		public Image getColumnImage(Object obj, int index) {
			if (index == 0)
				return super.getColumnImage(PluginRegistry.findModel(((IPluginConfiguration) obj).getId()), index);
			return null;
		}

		public String getColumnText(Object obj, int index) {
			IPluginConfiguration configuration = (IPluginConfiguration) obj;
			switch (index) {
				case 0 :
					return configuration.getId();
					//return super.getColumnText(PluginRegistry.findModel(configuration.getId()), index);
				case 1 :
					return (configuration.getStartLevel() == 0) ? "default" : Integer.toString(configuration.getStartLevel()); //$NON-NLS-1$
				case 2 :
					return Boolean.toString(configuration.isAutoStart());
			}
			return null;
		}

	}

	private TableViewer fConfigurationsTable;
	private TableEditor fLevelColumnEditor;
	private TableEditor fAutoColumnEditor;

	//private IStructuredSelection fLastSelection = null;

	/**
	 * @param page
	 * @param parent
	 */
	public PluginConfigurationSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, getButtonLabels());
	}

	private static String[] getButtonLabels() {
		String[] labels = new String[3];
		labels[0] = PDEUIMessages.Product_PluginSection_add;
		labels[1] = PDEUIMessages.PluginSection_remove;
		labels[2] = PDEUIMessages.Product_PluginSection_removeAll;
		return labels;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
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

		final TableColumn levelColumnEditor = new TableColumn(table, SWT.CENTER);
		levelColumnEditor.setText(PDEUIMessages.EquinoxPluginBlock_levelColumn);

		final TableColumn autoColumnEditor = new TableColumn(table, SWT.CENTER);
		autoColumnEditor.setText(PDEUIMessages.EquinoxPluginBlock_autoColumn);

		table.addControlListener(new ControlListener() {

			public void controlMoved(ControlEvent e) {
			}

			public void controlResized(ControlEvent e) {
				int size = table.getSize().x;
				column1.setWidth(size / 7 * 4);
				levelColumnEditor.setWidth(size / 7 * 2);
				autoColumnEditor.setWidth(size / 7 * 1);
			}

		});

		table.setHeaderVisible(true);
		toolkit.paintBordersFor(container);
		fConfigurationsTable.setLabelProvider(getLabelProvider());
		fConfigurationsTable.setContentProvider(new ContentProvider());
		fConfigurationsTable.setInput(getProduct());
		createEditors();

		section.setClient(container);
		getModel().addModelChangedListener(this);
		getTablePart().setButtonEnabled(0, isEditable());
		updateRemoveButtons(true, true);
	}

	protected void buttonSelected(int index) {
		switch (index) {
			case 0 :
				handleAdd();
				break;
			case 1 :
				handleRemove();
				break;
			case 2 :
				handleRemoveAll();
				break;
		}
	}

	private void handleAdd() {
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), PDEPlugin.getDefault().getLabelProvider());
		ArrayList plugins = new ArrayList();

		// TODO there must be a better way to do this!
		if (getProduct().useFeatures()) {
			IProductFeature[] features = getProduct().getFeatures();
			for (int i = 0; i < features.length; i++) {
				IProductFeature feature = features[i];
				FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
				IFeatureModel fModel = manager.findFeatureModelRelaxed(feature.getId(), feature.getVersion());
				if (fModel == null)
					fModel = manager.findFeatureModel(feature.getId());
				if (fModel == null)
					continue;
				IFeaturePlugin[] fPlugins = fModel.getFeature().getPlugins();
				for (int j = 0; j < fPlugins.length; j++) {
					IFeaturePlugin fPlugin = fPlugins[j];
					if (!fPlugin.isFragment())
						plugins.add(fPlugin);
				}
			}
			dialog.setElements(plugins.toArray(new IFeaturePlugin[plugins.size()]));
		} else {
			IProductPlugin[] allPlugins = getProduct().getPlugins();
			IPluginConfiguration[] configs = getProduct().getPluginConfigurations();
			for (int i = 0; i < allPlugins.length; ++i) {
				boolean match = false;
				for (int j = 0; j < configs.length; ++j) {
					String id = allPlugins[i].getId();
					if (id.equals(configs[j].getId())) {
						match = true;
						break;
					}
				}
				if (!match) {
					// ensure we don't add fragments
					if (!(PluginRegistry.findModel(allPlugins[i].getId()) instanceof IFragmentModel))
						plugins.add(allPlugins[i]);
				}
			}
			dialog.setElements(plugins.toArray(new IProductPlugin[plugins.size()]));
		}

		dialog.setTitle(PDEUIMessages.PluginSelectionDialog_title);
		dialog.setMessage(PDEUIMessages.PluginSelectionDialog_message);
		dialog.setMultipleSelection(true);
		if (dialog.open() == Window.OK) {
			Object[] results = dialog.getResult();
			for (int i = 0; i < results.length; i++) {
				Object result = results[i];
				if (result instanceof IProductPlugin) {
					addPlugin(((IProductPlugin) result).getId());
				} else if (result instanceof IFeaturePlugin) {
					addPlugin(((IFeaturePlugin) result).getId());
				}
			}
		}

	}

	private void handleRemove() {
		IStructuredSelection ssel = (IStructuredSelection) fConfigurationsTable.getSelection();
		if (ssel.size() > 0) {
			Object[] objects = ssel.toArray();
			IPluginConfiguration[] configurations = new IPluginConfiguration[objects.length];
			System.arraycopy(objects, 0, configurations, 0, objects.length);
			getProduct().removePluginConfigurations(configurations);
		}
		clearEditors();
	}

	private void handleRemoveAll() {
		IProduct product = getProduct();
		product.removePluginConfigurations(product.getPluginConfigurations());
		clearEditors();
	}

	protected void selectionChanged(IStructuredSelection selection) {
		updateRemoveButtons(true, false);
	}

	private void addPlugin(String id) {
		IProduct product = getProduct();
		IProductModelFactory factory = product.getModel().getFactory();
		IPluginConfiguration configuration = factory.createPluginConfiguration();
		configuration.setId(id);
		product.addPluginConfigurations(new IPluginConfiguration[] {configuration});
		fConfigurationsTable.setSelection(new StructuredSelection(configuration));
		showControls();
	}

	private ILabelProvider getLabelProvider() {
		return new LabelProvider();
	}

	private void clearEditors() {
		Control oldEditor = fLevelColumnEditor.getEditor();
		if (oldEditor != null && !oldEditor.isDisposed())
			oldEditor.dispose();

		oldEditor = fAutoColumnEditor.getEditor();
		if (oldEditor != null && !oldEditor.isDisposed())
			oldEditor.dispose();
	}

	private void createEditors() {
		final Table table = fConfigurationsTable.getTable();

		fLevelColumnEditor = new TableEditor(table);
		fLevelColumnEditor.horizontalAlignment = SWT.CENTER;
		fLevelColumnEditor.minimumWidth = 40;
		if (Util.isMac())
			fLevelColumnEditor.minimumHeight = 27;

		fAutoColumnEditor = new TableEditor(table);
		fAutoColumnEditor.horizontalAlignment = SWT.CENTER;
		fAutoColumnEditor.grabHorizontal = true;
		fAutoColumnEditor.minimumWidth = 50;

		table.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				showControls();
			}
		});

	}

	private void showControls() {
		// Clean up any previous editor control
		clearEditors();

		// Identify the selected row
		Table table = fConfigurationsTable.getTable();
		IStructuredSelection selection = (IStructuredSelection) fConfigurationsTable.getSelection();
		if (selection.isEmpty())
			return;
		final TableItem item = table.getSelection()[0];
		if (item != null && !isEditable())
			return;

		if (item != null) {
			final IPluginConfiguration ppc = (IPluginConfiguration) selection.getFirstElement();
			final Spinner spinner = new Spinner(table, SWT.BORDER);

			spinner.setMinimum(0);
			String level = item.getText(1);
			int defaultLevel = level.length() == 0 || "default".equals(level) ? 0 : Integer.parseInt(level); //$NON-NLS-1$
			spinner.setSelection(defaultLevel);
			spinner.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					int selection = spinner.getSelection();
					item.setText(1, selection == 0 ? "default" //$NON-NLS-1$
							: Integer.toString(selection));
					ppc.setStartLevel(selection);
				}
			});
			fLevelColumnEditor.setEditor(spinner, item, 1);

			final CCombo combo = new CCombo(table, SWT.BORDER | SWT.READ_ONLY);
			//TODO is there need for the default options ??
			combo.setItems(new String[] {Boolean.toString(true), Boolean.toString(false)});
			combo.setText(item.getText(2));
			combo.pack();
			combo.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					item.setText(2, combo.getText());
					ppc.setAutoStart(Boolean.valueOf(combo.getText()).booleanValue());
				}
			});
			fAutoColumnEditor.setEditor(combo, item, 2);
		}
	}

	private IProduct getProduct() {
		return getModel().getProduct();
	}

	private IProductModel getModel() {
		return (IProductModel) getPage().getPDEEditor().getAggregateModel();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.IPluginModelListener#modelsChanged(org.eclipse.pde.internal.core.PluginModelDelta)
	 */
	public void modelChanged(IModelChangedEvent e) {
		//TODO update modelChanged method
		// No need to call super, handling world changed event here
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			handleGlobalRefresh();
			return;
		}
		Table table = fConfigurationsTable.getTable();
		int count = table.getItemCount();
		Object[] objects = e.getChangedObjects();
		boolean refreshRemove = false;
		boolean refreshRemoveAll = false;
		if (e.getChangeType() == IModelChangedEvent.INSERT) {
			if (count == 0) {
				refreshRemoveAll = true;
			}
			for (int i = 0; i < objects.length; i++) {
				if (objects[i] instanceof IPluginConfiguration)
					fConfigurationsTable.add(objects[i]);
			}
		} else if (e.getChangeType() == IModelChangedEvent.REMOVE) {
			refreshRemove = refreshRemoveAll = true;
			int index = table.getSelectionIndex();
			boolean global = false;
			for (int i = 0; i < objects.length; i++) {
				if (objects[i] instanceof IPluginConfiguration)
					fConfigurationsTable.remove(objects[i]);
				else if (objects[i] instanceof IProductPlugin) {
					global = true;
					break;
				}
			}

			if (global)
				handleGlobalRefresh();

			// Update Selection

			if (count == 0) {
				table.deselectAll();
				clearEditors();
			} else if (index < count) {
				table.setSelection(index);
			} else {
				table.setSelection(count - 1);
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
			tablePart.setButtonEnabled(1, isEditable() && !selection.isEmpty());
		}
		int count = fConfigurationsTable.getTable().getItemCount();
		if (updateRemoveAll)
			tablePart.setButtonEnabled(2, isEditable() && count > 0);
	}

}
