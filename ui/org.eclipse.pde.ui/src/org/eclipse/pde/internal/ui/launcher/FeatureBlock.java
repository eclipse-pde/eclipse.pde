/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.util.*;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.ui.launcher.AbstractLauncherTab;
import org.eclipse.pde.ui.launcher.PluginsTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class FeatureBlock {

	private ILaunchConfiguration fLaunchConfig;
	private AbstractLauncherTab fTab;
	private CheckboxTableViewer fViewer;
	private Table fTable;
	private Button fSelectAllButton;
	private Button fDeselectAllButton;
	private Button fUseWrkSpcFeaturesButton;
	private Button fUseExtrnlFeaturesButton;
	private Button fDefaultsButton;
	private Button fFilterButton;
	private Label fCounter;
	private Listener fListener;
	private HashMap fWorkspaceFeatureMap;
	private HashMap fExternalFeatureMap;
	private HashMap fFeatureLocationMap;

	private static final String COLUMN_ID = "columnID"; //$NON-NLS-1$

	class FeatureTableLabelProvider extends PDELabelProvider {
		public Image getColumnImage(Object obj, int index) {
			return index == 0 ? super.getColumnImage(obj, index) : null;
		}

		public String getColumnText(Object obj, int index) {
			switch (index) {
				case 0 :
					return super.getObjectText((IFeatureModel) obj, false);
				case 1 :
					IFeature feature = ((IFeatureModel) obj).getFeature();
					return feature.getVersion();

				case 2 :
					String location = (String) fFeatureLocationMap.get(((IFeatureModel) obj).getFeature().getId());
					if (IPDELauncherConstants.LOCATION_WORKSPACE.equalsIgnoreCase(location)) {
						return PDEUIMessages.FeatureBlock_workspace;
					} else if (IPDELauncherConstants.LOCATION_EXTERNAL.equalsIgnoreCase(location)) {
						return PDEUIMessages.FeatureBlock_external;
					}
					return ""; //$NON-NLS-1$
				default :
					return ""; //$NON-NLS-1$
			}
		}
	}

	class PluginContentProvider extends DefaultContentProvider implements IStructuredContentProvider {

		public Object[] getElements(Object input) {
			return (IFeatureModel[]) input;
		}
	}

	class Listener extends SelectionAdapter {

		public void widgetSelected(SelectionEvent e) {
			Object source = e.getSource();
			if (source == fSelectAllButton) {
				handleSelectAll(true);
			} else if (source == fDeselectAllButton) {
				handleSelectAll(false);
			} else if (source == fUseWrkSpcFeaturesButton) {
				handleUseLocation(fWorkspaceFeatureMap, IPDELauncherConstants.LOCATION_WORKSPACE);
			} else if (source == fUseExtrnlFeaturesButton) {
				handleUseLocation(fExternalFeatureMap, IPDELauncherConstants.LOCATION_EXTERNAL);
			} else if (source == fDefaultsButton) {
				handleRestoreDefaults();
			} else if (source instanceof TableColumn) {
				handleColumn((TableColumn) source);
			}
			fTab.updateLaunchConfigurationDialog();
		}

		private void handleColumn(TableColumn tc) {
			fTable.setSortColumn(tc);
			int sortDirn;
			switch (fTable.getSortDirection()) {
				case SWT.DOWN :
					sortDirn = SWT.UP;
					break;
				case SWT.UP :
				default :
					sortDirn = SWT.DOWN;
			}
			fTable.setSortDirection(sortDirn);
			int sortOrder = sortDirn == SWT.UP ? 1 : -1;
			int sortColumn = ((Integer) tc.getData(COLUMN_ID)).intValue();
			fViewer.setSorter(new TableSorter(sortColumn, sortOrder));
		}

		private void handleRestoreDefaults() {
			fViewer.setInput(getFeatures());
		}

		private void handleUseLocation(HashMap featureMap, String location) {
			IStructuredSelection selection = (IStructuredSelection) fViewer.getSelection();
			if (selection.isEmpty())
				return;
			Object[] models = selection.toArray();
			for (int i = 0; i < models.length; i++) {
				String id = ((IFeatureModel) models[i]).getFeature().getId();
				if (featureMap.containsKey(id)) {
					fFeatureLocationMap.put(id, location);
					fViewer.refresh(models[i], true);
				}
			}
		}

		private void handleSelectAll(boolean state) {
			fViewer.setAllChecked(state);
			updateCounter();
		}
	}

	private class TableSorter extends ViewerSorter {
		int sortColumn;
		int sortOrder;

		public TableSorter(int sortColumn, int sortOrder) {
			this.sortColumn = sortColumn;
			this.sortOrder = sortOrder;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ViewerComparator#compare(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		public int compare(Viewer viewer, Object e1, Object e2) {
			FeatureTableLabelProvider labelProvider = (FeatureTableLabelProvider) fViewer.getLabelProvider();
			return sortOrder * super.compare(viewer, labelProvider.getColumnText(e1, sortColumn), labelProvider.getColumnText(e2, sortColumn));
		}

	}

	private class LocationCellModifier implements ICellModifier {

		public boolean canModify(Object element, String property) {
			if (element instanceof IFeatureModel) {
				IFeature feature = ((IFeatureModel) element).getFeature();
				if (IPDELauncherConstants.LOCATION.equalsIgnoreCase(property)) {
					if (fWorkspaceFeatureMap.containsKey(feature.getId()) && fExternalFeatureMap.containsKey(feature.getId())) {
						return true;
					}
				}
			}
			return false;
		}

		public Object getValue(Object element, String property) {
			if (element instanceof IFeatureModel) {
				if (IPDELauncherConstants.LOCATION.equalsIgnoreCase(property)) {
					IFeature feature = ((IFeatureModel) element).getFeature();
					String location = (String) fFeatureLocationMap.get(feature.getId());
					if (IPDELauncherConstants.LOCATION_WORKSPACE.equalsIgnoreCase(location)) {
						return new Integer(0);
					} else if (IPDELauncherConstants.LOCATION_EXTERNAL.equalsIgnoreCase(location)) {
						return new Integer(1);
					}
				}
			}
			return new Integer(0);
		}

		public void modify(Object element, String property, Object value) {
			final IStructuredSelection selection = (IStructuredSelection) fViewer.getSelection();
			final IFeatureModel model = (IFeatureModel) selection.getFirstElement();
			if (model == null)
				return;

			String id = model.getFeature().getId();
			final int comboIndex = ((Integer) value).intValue();

			if (IPDELauncherConstants.LOCATION.equalsIgnoreCase(property)) {
				if (comboIndex == 0)
					fFeatureLocationMap.put(id, IPDELauncherConstants.LOCATION_WORKSPACE);
				if (comboIndex == 1)
					fFeatureLocationMap.put(id, IPDELauncherConstants.LOCATION_EXTERNAL);
			}

			fViewer.refresh(model);
			fTab.updateLaunchConfigurationDialog();
		}
	}

	public FeatureBlock(PluginsTab pluginsTab) {
		Assert.isNotNull(pluginsTab);
		fTab = pluginsTab;
	}

	public void createControl(Composite parent, int span, int indent) {
		fListener = new Listener();
		createCheckBoxTable(parent, span - 1, indent);
		createButtonContainer(parent, 10);
	}

	private void createButtonContainer(Composite parent, int vOffset) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		layout.marginTop = vOffset;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_VERTICAL));

		fSelectAllButton = createButton(composite, PDEUIMessages.AdvancedLauncherTab_selectAll, SWT.PUSH);
		fDeselectAllButton = createButton(composite, PDEUIMessages.AdvancedLauncherTab_deselectAll, SWT.PUSH);

		//fAddRequiredFeaturesButton = createButton(composite, PDEUIMessages.FeatureBlock_addRequiredFeatues, SWT.PUSH);
		//fAddRequiredBundlesButton = createButton(composite, PDEUIMessages.FeatureBlock_addRequiedBundles, SWT.PUSH);
		fUseWrkSpcFeaturesButton = createButton(composite, PDEUIMessages.FeatureBlock_useWorkspaceFeatures, SWT.PUSH);
		fUseExtrnlFeaturesButton = createButton(composite, PDEUIMessages.FeatureBlock_useExternalFeatures, SWT.PUSH);
		fDefaultsButton = createButton(composite, PDEUIMessages.AdvancedLauncherTab_defaults, SWT.PUSH);
		fFilterButton = createButton(composite, PDEUIMessages.FeatureBlock_showSelected, SWT.CHECK);
		GridData filterButtonGridData = new GridData(GridData.FILL_BOTH | GridData.VERTICAL_ALIGN_END);
		fFilterButton.setLayoutData(filterButtonGridData);

		fCounter = new Label(composite, SWT.NONE);
		fCounter.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_END));
		updateCounter();
	}

	private Button createButton(Composite composite, String text, int style) {
		Button button = new Button(composite, style);
		button.setText(text);
		button.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		SWTUtil.setButtonDimensionHint(button);
		button.addSelectionListener(fListener);
		return button;
	}

	private void createCheckBoxTable(Composite parent, int span, int indent) {
		fViewer = CheckboxTableViewer.newCheckList(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		fTable = fViewer.getTable();

		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = span;
		gd.horizontalIndent = indent;
		fTable.setLayoutData(gd);

		TableColumn column1 = new TableColumn(fTable, SWT.LEFT);
		column1.setText(PDEUIMessages.FeatureBlock_features);
		column1.setWidth(300);
		column1.addSelectionListener(fListener);
		column1.setData(COLUMN_ID, new Integer(0));

		TableColumn column2 = new TableColumn(fTable, SWT.LEFT);
		column2.setText(PDEUIMessages.FeatureBlock_version);
		column2.setWidth(100);
		column2.addSelectionListener(fListener);
		column2.setData(COLUMN_ID, new Integer(1));

		TableColumn column3 = new TableColumn(fTable, SWT.LEFT);
		column3.setText(PDEUIMessages.FeatureBlock_location);
		column3.setWidth(80);
		column3.addSelectionListener(fListener);
		column3.setData(COLUMN_ID, new Integer(2));

		fTable.setHeaderVisible(true);

		fViewer.setLabelProvider(new FeatureTableLabelProvider());
		fViewer.setContentProvider(new PluginContentProvider());
		fViewer.setInput(getFeatures());

		fViewer.addCheckStateListener(new ICheckStateListener() {

			public void checkStateChanged(CheckStateChangedEvent event) {
				fTab.updateLaunchConfigurationDialog();
			}
		});

		fListener.handleColumn(column3);

		String[] items = new String[] {PDEUIMessages.FeatureBlock_workspace, PDEUIMessages.FeatureBlock_external};
		fViewer.setCellEditors(new CellEditor[] {null, null, new ComboBoxCellEditor(fTable, items)});
		fViewer.setColumnProperties(new String[] {null, null, IPDELauncherConstants.LOCATION});

		fViewer.setCellModifier(new LocationCellModifier());
		fViewer.addDoubleClickListener(new IDoubleClickListener() {

			public void doubleClick(DoubleClickEvent event) {
				final ISelection selection = event.getSelection();
				if (selection == null || !(selection instanceof IStructuredSelection)) {
					return;
				}
				fViewer.editElement(((IStructuredSelection) selection).getFirstElement(), 2);
			}
		});

	}

	private Object getFeatures() {
		FeatureModelManager fmm = new FeatureModelManager();
		fWorkspaceFeatureMap = new HashMap();
		fExternalFeatureMap = new HashMap();
		fFeatureLocationMap = new HashMap();
		ArrayList features = new ArrayList();
		IFeatureModel[] workspaceModels = fmm.getWorkspaceModels();
		for (int i = 0; i < workspaceModels.length; i++) {
			String id = workspaceModels[i].getFeature().getId();
			fWorkspaceFeatureMap.put(id, workspaceModels[i]);
			fFeatureLocationMap.put(id, IPDELauncherConstants.LOCATION_WORKSPACE);
			features.add(workspaceModels[i]);
		}

		IFeatureModel[] externalModels = fmm.getModels();
		for (int i = 0; i < externalModels.length; i++) {
			String id = externalModels[i].getFeature().getId();
			fExternalFeatureMap.put(id, externalModels[i]);
			if (!fWorkspaceFeatureMap.containsKey(id)) {
				features.add(externalModels[i]);
				fFeatureLocationMap.put(id, IPDELauncherConstants.LOCATION_EXTERNAL);
			}
		}

		return features.toArray(new IFeatureModel[features.size()]);
	}

	public void initializeFrom(ILaunchConfiguration config) throws CoreException {
		if (fLaunchConfig != null && fLaunchConfig.equals(config)) {
			// Do nothing
			return;
		}

		fLaunchConfig = config;
		fViewer.setInput(getFeatures());
		HashMap featureMap = BundleLauncherHelper.getFeatureLocationMap(config);
		for (Iterator iterator = featureMap.keySet().iterator(); iterator.hasNext();) {
			String id = (String) iterator.next();
			String location = (String) featureMap.get(id);
			fFeatureLocationMap.put(id, location);
			Object model = null;
			if (IPDELauncherConstants.LOCATION_WORKSPACE.equalsIgnoreCase(location)) {
				model = fWorkspaceFeatureMap.get(id);
			} else if (IPDELauncherConstants.LOCATION_EXTERNAL.equalsIgnoreCase(location)) {
				model = fExternalFeatureMap.get(id);
			}
			if (model != null) {
				fViewer.setChecked(model, true);
				fViewer.refresh(model, true);
			}
		}

		// If the workspace plug-in state has changed (project closed, etc.) the launch config needs to be updated without making the tab dirty
		if (fLaunchConfig.isWorkingCopy()) {
			savePluginState((ILaunchConfigurationWorkingCopy) fLaunchConfig);
		}

		updateCounter();
		fTab.updateLaunchConfigurationDialog();

	}

	private void updateCounter() {
		if (fCounter != null) {
			int checked = fViewer.getCheckedElements().length;
			int total = fFeatureLocationMap.keySet().size();
			fCounter.setText(NLS.bind(PDEUIMessages.AbstractPluginBlock_counter, new Integer(checked), new Integer(total)));
		}
	}

	public void performApply(ILaunchConfigurationWorkingCopy config) {
		//TODO update these when real buttons are added to UI
		config.setAttribute(IPDELauncherConstants.INCLUDE_OPTIONAL, true);
		config.setAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true);
		config.setAttribute(IPDELauncherConstants.AUTOMATIC_VALIDATE, false);
		config.setAttribute(IPDELauncherConstants.SHOW_SELECTED_ONLY, false);
		savePluginState(config);
		updateCounter();
	}

	private void savePluginState(ILaunchConfigurationWorkingCopy config) {
		StringBuffer featuresEntry = new StringBuffer();
		if (fViewer.getCheckedElements() != null && fViewer.getCheckedElements().length > 0) {
			Object[] featureModels = fViewer.getCheckedElements();
			for (int i = 0; i < featureModels.length; i++) {
				IFeature feature = ((IFeatureModel) featureModels[i]).getFeature();
				String location = (String) fFeatureLocationMap.get(feature.getId());
				String value = BundleLauncherHelper.writeFeatureEntry(feature.getId(), feature.getVersion(), location);
				featuresEntry.append(value);
			}
		}
		config.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, featuresEntry.length() == 0 ? (String) null : featuresEntry.toString());
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IPDELauncherConstants.INCLUDE_OPTIONAL, true);
		config.setAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true);
		config.setAttribute(IPDELauncherConstants.AUTOMATIC_VALIDATE, false);
		config.setAttribute(IPDELauncherConstants.SHOW_SELECTED_ONLY, false);
	}

	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}

	public void initialize() throws CoreException {
		initializeFrom(fLaunchConfig);
	}
}
