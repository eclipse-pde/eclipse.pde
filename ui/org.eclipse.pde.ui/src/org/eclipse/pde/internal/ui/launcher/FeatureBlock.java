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

import java.util.ArrayList;
import java.util.Arrays;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.PDEPreferencesManager;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.shared.CachedCheckboxTreeViewer;
import org.eclipse.pde.internal.ui.shared.FilteredCheckboxTree;
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

/**
 * Provides the UI that is displayed in the Plug-ins tab of PDE launch configs when the user
 * has chosen to launch using "features selected below".  Provides a filterable tree that
 * the user can select which features to launch, as well as where the feature should be taken
 * from (Workspace or External) and where plug-ins should be collected from (Workspace first
 * or External first).
 */
public class FeatureBlock {

	class FeatureTreeLabelProvider extends PDELabelProvider {
		public Image getColumnImage(Object obj, int index) {
			return index == 0 ? super.getColumnImage(obj, index) : null;
		}

		public String getColumnText(Object obj, int index) {
			String id = (String) obj;
			switch (index) {
				case COLUMN_FEATURE_NAME :
					return super.getObjectText(fModelProvider.getFeatureModel(id), false);
				case COLUMN_FEATURE_VERSION :
					return fModelProvider.getVersion(id);
				case COLUMN_FEATURE_LOCATION :
					return getLocationText(fModelProvider.getLocation(id));
				case COLUMN_PLUGIN_RESOLUTION :
					return getLocationText(fModelProvider.getPluginResolution(id));
				default :
					return ""; //$NON-NLS-1$
			}
		}
	}

	class ButtonSelectionListener extends SelectionAdapter {

		public void widgetSelected(SelectionEvent e) {
			Object source = e.getSource();
			if (source == fSelectAllButton) {
				handleSelectAll(true);
			} else if (source == fDeselectAllButton) {
				handleSelectAll(false);
			} else if (source == fAddRequiredFeaturesButton) {
				handleAddRequired();
			} else if (source == fDefaultsButton) {
				handleRestoreDefaults();
			} else if (source == fDefaultFeatureLocationCombo) {
				handleDefaultLocationChange();
			} else if (source instanceof TreeColumn) {
				handleColumn((TreeColumn) source, 0);
			}
			fTab.updateLaunchConfigurationDialog();
		}

		private void handleAddRequired() {
			if (fTree.getCheckedElements() != null && fTree.getCheckedElements().length > 0) {
				Object[] featureModelIDs = fTree.getCheckedElements();
				ArrayList requiredFeatureList = new ArrayList();

				for (int i = 0; i < featureModelIDs.length; i++) {
					requiredFeatureList.add(featureModelIDs[i]);
					getFeatureDependencies(fModelProvider.getFeatureModel((String) featureModelIDs[i]), requiredFeatureList);
				}
				fTree.setCheckedElements(requiredFeatureList.toArray());
			}
		}

		private void getFeatureDependencies(IFeatureModel model, ArrayList requiredFeatureList) {
			IFeature feature = model.getFeature();
			IFeatureImport[] featureImports = feature.getImports();
			for (int i = 0; i < featureImports.length; i++) {
				if (featureImports[i].getType() == IFeatureImport.FEATURE) {
					addFeature(requiredFeatureList, featureImports[i].getId());
				}
			}

			IFeatureChild[] featureIncludes = feature.getIncludedFeatures();
			for (int i = 0; i < featureIncludes.length; i++) {
				addFeature(requiredFeatureList, featureIncludes[i].getId());
			}
		}

		private void addFeature(ArrayList requiredFeatureList, String id) {
			if (!requiredFeatureList.contains(id)) {
				IFeatureModel model = fModelProvider.getFeatureModel(id);
				if (model != null) {
					requiredFeatureList.add(id);
					getFeatureDependencies(model, requiredFeatureList);
				}
			}
		}

		private void handleColumn(TreeColumn tc, int sortDirn) {
			Tree tree = fTree.getTree();
			tree.setSortColumn(tc);
			if (sortDirn == 0) {
				switch (tree.getSortDirection()) {
					case SWT.DOWN :
						sortDirn = SWT.UP;
						break;
					case SWT.UP :
					default :
						sortDirn = SWT.DOWN;
				}
			}
			tree.setSortDirection(sortDirn);
			int sortOrder = sortDirn == SWT.UP ? 1 : -1;
			int sortColumn = ((Integer) tc.getData(COLUMN_ID)).intValue();
			fTree.setSorter(new TreeSorter(sortColumn, sortOrder));
			saveSortOrder();
		}

		private void handleRestoreDefaults() {
			fDefaultFeatureLocationCombo.setText(getLocationText(LOCATION_WORKSPACE));
			fDefaultPluginResolutionCombo.setText(getLocationText(LOCATION_WORKSPACE));
			fModelProvider.init(LOCATION_WORKSPACE);
			fTree.refresh(true);
		}

		private void handleSelectAll(boolean state) {
			fTree.setAllChecked(state);
			updateCounter();
		}

		private void handleDefaultLocationChange() {
			String defaultLocation = getLocationConstant(fDefaultFeatureLocationCombo.getText());
			fModelProvider.setDefaultFeatureLocation(defaultLocation);
			String[] idArray = fModelProvider.getIDArray();
			for (int i = 0; i < idArray.length; ++i) {
				String id = idArray[i];
				String location = fModelProvider.getLocation(id);
				if (LOCATION_DEFAULT.equalsIgnoreCase(location)) {
					fModelProvider.setModelFromLocation(id, location);
					fTree.refresh(id, true);
				}
			}
		}
	}

	class LocationCellModifier implements ICellModifier {

		public boolean canModify(Object id, String property) {
			if (PROPERTY_LOCATION.equalsIgnoreCase(property)) {
				if (fModelProvider.isCommon((String) id)) {
					return fTree.getChecked(id);
				}

			} else if (PROPERTY_RESOLUTION.equalsIgnoreCase(property)) {
				return fTree.getChecked(id);
			}
			return false;
		}

		public Object getValue(Object id, String property) {
			String location = ""; //$NON-NLS-1$
			if (PROPERTY_LOCATION.equalsIgnoreCase(property)) {
				location = fModelProvider.getLocation((String) id);
			} else if (PROPERTY_RESOLUTION.equalsIgnoreCase(property)) {
				location = fModelProvider.getPluginResolution(location);
			}
			if (LOCATION_DEFAULT.equalsIgnoreCase(location)) {
				return new Integer(0);
			} else if (LOCATION_WORKSPACE.equalsIgnoreCase(location)) {
				return new Integer(1);
			} else if (LOCATION_EXTERNAL.equalsIgnoreCase(location)) {
				return new Integer(2);
			}

			return new Integer(0);
		}

		public void modify(Object item, String property, Object value) {
			String id = (String) ((TreeItem) item).getData();
			int comboIndex = ((Integer) value).intValue();
			String location = null;
			switch (comboIndex) {
				case 0 :
					location = LOCATION_DEFAULT;
					break;
				case 1 :
					location = LOCATION_WORKSPACE;
					break;
				case 2 :
					location = LOCATION_EXTERNAL;
			}

			if (PROPERTY_LOCATION.equalsIgnoreCase(property)) {
				fModelProvider.setModelFromLocation(id, location);
			} else if (PROPERTY_RESOLUTION.equalsIgnoreCase(property)) {
				fModelProvider.setPluginResolution(id, location);
			} else {
				return; // nothing to do
			}

			fTree.refresh(id, true);
			fTab.updateLaunchConfigurationDialog();
		}
	}

	class PluginContentProvider implements ITreeContentProvider {

		public Object[] getElements(Object input) {
			if (input instanceof Object[]) {
				return (Object[]) input;
			}
			return new Object[0];
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		public Object[] getChildren(Object parentElement) {
			return new Object[0];
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			return false;
		}
	}

	private class TreeSorter extends ViewerSorter {
		int sortColumn;
		int sortOrder;

		public TreeSorter(int sortColumn, int sortOrder) {
			this.sortColumn = sortColumn;
			this.sortOrder = sortOrder;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ViewerComparator#compare(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		public int compare(Viewer viewer, Object e1, Object e2) {
			FeatureTreeLabelProvider labelProvider = (FeatureTreeLabelProvider) fTree.getLabelProvider();
			return sortOrder * super.compare(viewer, labelProvider.getColumnText(e1, sortColumn), labelProvider.getColumnText(e2, sortColumn));
		}

	}

	private FeatureBlockModelProvider fModelProvider;

	private static final int COLUMN_FEATURE_NAME = 0;
	private static final int COLUMN_FEATURE_VERSION = 1;
	private static final int COLUMN_FEATURE_LOCATION = 2;
	private static final int COLUMN_PLUGIN_RESOLUTION = 3;

	private static final String COLUMN_ID = "columnID"; //$NON-NLS-1$

	private static final String LOCATION_DEFAULT = "Default"; //$NON-NLS-1$
	private static final String LOCATION_EXTERNAL = "External"; //$NON-NLS-1$
	private static final String LOCATION_WORKSPACE = "Workspace"; //$NON-NLS-1$
	private static final String PROPERTY_LOCATION = "location"; //$NON-NLS-1$
	private static final String PROPERTY_RESOLUTION = "resolution"; //$NON-NLS-1$

	private Label fCounter;
	private Button fAddRequiredFeaturesButton;
	private Button fDefaultsButton;
	private Button fSelectAllButton;
	private Button fDeselectAllButton;

	private Combo fDefaultFeatureLocationCombo;
	private Combo fDefaultPluginResolutionCombo;

	private ILaunchConfiguration fLaunchConfig;
	private ButtonSelectionListener fListener;
	private AbstractLauncherTab fTab;
	private CachedCheckboxTreeViewer fTree;

	public FeatureBlock(PluginsTab pluginsTab) {
		Assert.isNotNull(pluginsTab);
		fTab = pluginsTab;
		fModelProvider = new FeatureBlockModelProvider();
	}

	public void createControl(Composite parent, int span, int indent) {
		fListener = new ButtonSelectionListener();

		Composite treeComposite = SWTFactory.createComposite(parent, 2, span, GridData.FILL_BOTH, 0, 0);
		createCheckBoxTree(treeComposite, indent);
		createButtonContainer(treeComposite, 10);

		Composite defaultsCombo = SWTFactory.createComposite(parent, 2, span, GridData.FILL_HORIZONTAL, 0, 0);

		Label label = SWTFactory.createLabel(defaultsCombo, PDEUIMessages.FeatureBlock_defaultFeatureLocation, 1);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent = indent;
		label.setLayoutData(gd);
		fDefaultFeatureLocationCombo = SWTFactory.createCombo(defaultsCombo, SWT.READ_ONLY | SWT.BORDER, 1, GridData.HORIZONTAL_ALIGN_BEGINNING, new String[] {PDEUIMessages.FeatureBlock_workspaceBefore, PDEUIMessages.FeatureBlock_externalBefore});

		label = SWTFactory.createLabel(defaultsCombo, PDEUIMessages.FeatureBlock_defaultPluginResolution, 1);
		label.setLayoutData(gd);
		fDefaultPluginResolutionCombo = SWTFactory.createCombo(defaultsCombo, SWT.READ_ONLY | SWT.BORDER, 1, GridData.HORIZONTAL_ALIGN_BEGINNING, new String[] {PDEUIMessages.FeatureBlock_workspaceBefore, PDEUIMessages.FeatureBlock_externalBefore});

		fDefaultFeatureLocationCombo.addSelectionListener(fListener);
		fDefaultPluginResolutionCombo.addSelectionListener(fListener);
	}

	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}

	private void createCheckBoxTree(Composite parent, int indent) {
		ITreeContentProvider contentProvider = new PluginContentProvider();
		FilteredCheckboxTree tree = new FilteredCheckboxTree(parent, contentProvider, null, SWT.FULL_SELECTION);
		tree.getPatternFilter().setIncludeLeadingWildcard(true);

		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalIndent = indent;
		tree.setLayoutData(gd);
		fTree = tree.getCheckboxTreeViewer();

		TreeColumn column1 = new TreeColumn(fTree.getTree(), SWT.LEFT);
		column1.setText(PDEUIMessages.FeatureBlock_features);
		column1.setWidth(250);
		column1.addSelectionListener(fListener);
		column1.setData(COLUMN_ID, new Integer(0));

		TreeColumn column2 = new TreeColumn(fTree.getTree(), SWT.LEFT);
		column2.setText(PDEUIMessages.FeatureBlock_version);
		column2.setWidth(100);
		column2.addSelectionListener(fListener);
		column2.setData(COLUMN_ID, new Integer(1));

		TreeColumn column3 = new TreeColumn(fTree.getTree(), SWT.LEFT);
		column3.setText(PDEUIMessages.FeatureBlock_featureLocation);
		column3.setWidth(100);
		column3.addSelectionListener(fListener);
		column3.setData(COLUMN_ID, new Integer(2));

		TreeColumn column4 = new TreeColumn(fTree.getTree(), SWT.LEFT);
		column4.setText(PDEUIMessages.FeatureBlock_pluginResolution);
		column4.setWidth(100);
		column4.addSelectionListener(fListener);
		column4.setData(COLUMN_ID, new Integer(3));

		fTree.getTree().setHeaderVisible(true);
		fTree.setLabelProvider(new FeatureTreeLabelProvider());
		fTree.setContentProvider(contentProvider);
		fTree.setInput(fModelProvider.getIDArray());
		fTree.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				String id = (String) event.getElement();
				if (event.getChecked() == false) {
					fModelProvider.setModelFromLocation(id, LOCATION_DEFAULT);
				}
				fTree.refresh(id, true);
				fTab.updateLaunchConfigurationDialog();
			}
		});

		String[] items = new String[] {PDEUIMessages.FeatureBlock_default, PDEUIMessages.FeatureBlock_workspaceBefore, PDEUIMessages.FeatureBlock_externalBefore};
		ComboBoxCellEditor cellEditor = new ComboBoxCellEditor(fTree.getTree(), items);
		cellEditor.getControl().pack();
		fTree.setCellEditors(new CellEditor[] {null, null, cellEditor, cellEditor});
		fTree.setColumnProperties(new String[] {null, null, PROPERTY_LOCATION, PROPERTY_RESOLUTION});
		fTree.setCellModifier(new LocationCellModifier());
		fTree.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				ISelection selection = event.getSelection();
				if (selection == null || !(selection instanceof IStructuredSelection)) {
					return;
				}
				Object element = ((IStructuredSelection) selection).getFirstElement();
				if (fTree.getChecked(element) == true) {
					fTree.editElement(element, COLUMN_FEATURE_LOCATION);
				} else {
					fTree.setChecked(element, true);
				}
			}
		});
		fTree.setAllChecked(true);

		/*		TreeViewerColumn tvc = new TreeViewerColumn(fTree, column3);
				tvc.setLabelProvider(new CellLabelProvider() {
					public void update(ViewerCell cell) {
						String id = (String) cell.getElement();
						Display display = fTree.getTree().getDisplay();
						Color gray = display.getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW);
						Color white = display.getSystemColor(SWT.COLOR_WHITE);
						if (fModelProvider.isCommon(id) && fTree.getChecked(id)) {
							cell.setBackground(white);
						} else {
							cell.setBackground(gray);
						}
						FeatureTreeLabelProvider provider = (FeatureTreeLabelProvider) fTree.getLabelProvider();
						cell.setText(provider.getColumnText(id, COLUMN_FEATURE_LOCATION));
					}
				});*/
	}

	private void createButtonContainer(Composite parent, int vOffset) {
		Composite buttonComp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		layout.marginTop = vOffset;
		buttonComp.setLayout(layout);
		buttonComp.setLayoutData(new GridData(GridData.FILL_VERTICAL));

		fSelectAllButton = createButton(buttonComp, PDEUIMessages.AdvancedLauncherTab_selectAll, SWT.PUSH);
		fDeselectAllButton = createButton(buttonComp, PDEUIMessages.AdvancedLauncherTab_deselectAll, SWT.PUSH);

		fAddRequiredFeaturesButton = createButton(buttonComp, PDEUIMessages.FeatureBlock_addRequiredFeatues, SWT.PUSH);
		fDefaultsButton = createButton(buttonComp, PDEUIMessages.AdvancedLauncherTab_defaults, SWT.PUSH);

		Composite countComp = SWTFactory.createComposite(buttonComp, 1, 1, SWT.NONE, 0, 0);
		countComp.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, true, true));
		fCounter = new Label(countComp, SWT.NONE);
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

	/*	private Object getFeatures(String defaultLocation) {
			fModelProvider = new FeatureLaunchModelProvider();
			return fModelProvider.getIDArray();
		}*/

	private String getLocationConstant(String value) {
		if (PDEUIMessages.FeatureBlock_workspaceBefore.equalsIgnoreCase(value)) {
			return LOCATION_WORKSPACE;
		} else if (PDEUIMessages.FeatureBlock_externalBefore.equalsIgnoreCase(value)) {
			return LOCATION_EXTERNAL;
		}
		return value;
	}

	private String getLocationText(String location) {
		if (LOCATION_DEFAULT.equalsIgnoreCase(location)) {
			return PDEUIMessages.FeatureBlock_default;
		} else if (LOCATION_WORKSPACE.equalsIgnoreCase(location)) {
			return PDEUIMessages.FeatureBlock_workspaceBefore;
		} else if (LOCATION_EXTERNAL.equalsIgnoreCase(location)) {
			return PDEUIMessages.FeatureBlock_externalBefore;
		}
		return ""; //$NON-NLS-1$
	}

	public void initialize() throws CoreException {
		initializeFrom(fLaunchConfig);
	}

	public void initializeFrom(ILaunchConfiguration config) throws CoreException {
		if (fLaunchConfig != null && fLaunchConfig.equals(config)) {
			// Do nothing
			return;
		}

		fLaunchConfig = config;
		fModelProvider.init(fModelProvider.getDefaultFeatureLocation());
		ArrayList selectedFeatureList = fModelProvider.parseConfig(config);

		fDefaultFeatureLocationCombo.setText(getLocationText(fModelProvider.getDefaultFeatureLocation()));
		fDefaultPluginResolutionCombo.setText(getLocationText(fModelProvider.getDefaultPluginResolution()));
		if (selectedFeatureList.size() > 0) {
			fTree.setAllChecked(false);
			for (int index = 0; index < selectedFeatureList.size(); index++) {
				String id = (String) selectedFeatureList.get(index);
				fTree.setChecked(id, true);
			}
		}

		// If the workspace plug-in state has changed (project closed, etc.) the launch config needs to be updated without making the tab dirty
		if (fLaunchConfig.isWorkingCopy()) {
			savePluginState((ILaunchConfigurationWorkingCopy) fLaunchConfig);
		}

		updateCounter();
		fTab.updateLaunchConfigurationDialog();

		PDEPreferencesManager prefs = new PDEPreferencesManager(IPDEUIConstants.PLUGIN_ID);
		int index = prefs.getInt(IPreferenceConstants.FEATURE_SORT_COLUMN);
		TreeColumn column = fTree.getTree().getColumn(index == 0 ? COLUMN_FEATURE_LOCATION : index - 1);
		fListener.handleColumn(column, prefs.getInt(IPreferenceConstants.FEATURE_SORT_ORDER));
		fTree.refresh(true);
	}

	public void performApply(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IPDELauncherConstants.SHOW_SELECTED_ONLY, false);
		savePluginState(config);
		saveSortOrder();
		updateCounter();
	}

	private void savePluginState(ILaunchConfigurationWorkingCopy config) {
		StringBuffer featuresEntry = new StringBuffer();
		if (fTree.getCheckedElements() != null && fTree.getCheckedElements().length > 0) {
			Object[] selectedFeatureModels = fTree.getCheckedElements();
			Arrays.sort(selectedFeatureModels); // So that Tab is not marked dirty due to Sorting changes
			for (int i = 0; i < selectedFeatureModels.length; i++) {
				String id = (String) selectedFeatureModels[i];
				String location = fModelProvider.getLocation(id);
				String resolution = fModelProvider.getPluginResolution(id);

				String value = BundleLauncherHelper.writeFeatureEntry(id, location, resolution);
				featuresEntry.append(value);
			}
		}
		config.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, featuresEntry.length() == 0 ? (String) null : featuresEntry.toString());
		config.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, getLocationConstant(fDefaultFeatureLocationCombo.getText()));
		config.setAttribute(IPDELauncherConstants.FEATURE_PLUGIN_RESOLUTION, getLocationConstant(fDefaultPluginResolutionCombo.getText()));
	}

	private void saveSortOrder() {
		PDEPreferencesManager prefs = new PDEPreferencesManager(IPDEUIConstants.PLUGIN_ID);
		Tree tree = fTree.getTree();
		TreeColumn column = tree.getSortColumn();
		int index = column == null ? 0 : ((Integer) tree.getSortColumn().getData(COLUMN_ID)).intValue();
		prefs.setValue(IPreferenceConstants.FEATURE_SORT_COLUMN, index + 1);
		int sortOrder = column == null ? 0 : tree.getSortDirection();
		prefs.setValue(IPreferenceConstants.FEATURE_SORT_ORDER, sortOrder);
		prefs.savePluginPreferences();
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IPDELauncherConstants.SHOW_SELECTED_ONLY, false);
	}

	private void updateCounter() {
		if (fCounter != null) {
			int checked = fTree.getCheckedElements().length;
			int total = fModelProvider.size();
			fCounter.setText(NLS.bind(PDEUIMessages.AbstractPluginBlock_counter, new Integer(checked), new Integer(total)));
		}
	}
}
