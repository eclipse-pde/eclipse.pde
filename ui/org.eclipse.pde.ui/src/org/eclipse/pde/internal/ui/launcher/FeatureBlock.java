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

import org.eclipse.pde.internal.ui.PDEUIMessages;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.launching.PDELaunchingPlugin;
import org.eclipse.pde.internal.launching.launcher.EclipsePluginValidationOperation;
import org.eclipse.pde.internal.launching.launcher.LaunchValidationOperation;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.shared.CachedCheckboxTreeViewer;
import org.eclipse.pde.internal.ui.shared.FilteredCheckboxTree;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.ui.launcher.AbstractLauncherTab;
import org.eclipse.pde.ui.launcher.PluginsTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.PatternFilter;

/**
 * Provides the UI that is displayed in the Plug-ins tab of PDE launch configs when the user
 * has chosen to launch using "features selected below".  Provides a filterable tree that
 * the user can select which features to launch, as well as where the feature should be taken
 * from (Workspace or External) and where plug-ins should be collected from (Workspace first
 * or External first).
 */
public class FeatureBlock {

	class FeatureTreeLabelProvider extends PDELabelProvider {
		/* (non-Javadoc)
		 * @see org.eclipse.pde.internal.ui.util.SharedLabelProvider#getColumnImage(java.lang.Object, int)
		 */
		public Image getColumnImage(Object obj, int index) {
			// If there is a workspace feature available, display the workspace feature icon, even if the user has selected external
			if (index == COLUMN_FEATURE_NAME) {
				FeatureLaunchModel model = (FeatureLaunchModel) obj;
				return getImage(model.getModel(true));
			}
			return null;
		}

		public String getColumnText(Object obj, int index) {
			FeatureLaunchModel model = (FeatureLaunchModel) obj;
			switch (index) {
				case COLUMN_FEATURE_NAME :
					return model.getId();
				case COLUMN_FEATURE_VERSION :
					return model.getVersion();
				case COLUMN_PLUGIN_RESOLUTION :
					return model.getResolutionLabel();
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
			} else if (source == fValidateButton) {
				handleValidate();
			} else if (source instanceof TreeColumn) {
				handleColumn((TreeColumn) source, 0);
			}
			fTab.updateLaunchConfigurationDialog();
		}

		private void handleValidate() {
			if (fOperation == null)
				fOperation = new EclipsePluginValidationOperation(fLaunchConfig);
			try {
				fOperation.run(new NullProgressMonitor());
			} catch (CoreException e) {
				PDEPlugin.log(e);
			}
			if (fOperation.hasErrors()) {
				PluginStatusDialog dialog = new PluginStatusDialog(getShell(), SWT.APPLICATION_MODAL | SWT.CLOSE | SWT.BORDER | SWT.TITLE | SWT.RESIZE);
				dialog.setInput(fOperation.getInput());
				dialog.open();
				dialog = null;
			} else if (fOperation.isEmpty()) {
				MessageDialog.openInformation(getShell(), PDEUIMessages.PluginStatusDialog_pluginValidation, NLS.bind(PDEUIMessages.AbstractLauncherToolbar_noSelection, fTab.getName().toLowerCase(Locale.ENGLISH)));
			} else {
				MessageDialog.openInformation(getShell(), PDEUIMessages.PluginStatusDialog_pluginValidation, PDEUIMessages.AbstractLauncherToolbar_noProblems);
			}
		}

		private Shell getShell() {
			try {
				Control c = fTab.getControl();
				if (!c.isDisposed())
					return c.getShell();
			} catch (SWTException e) {
			}
			return PDEPlugin.getActiveWorkbenchShell();
		}

		private void handleAddRequired() {
			if (fTree.getCheckedElements() != null && fTree.getCheckedElements().length > 0) {
				Object[] features = fTree.getCheckedElements();
				Set requiredFeatureIDs = new HashSet();
				for (int i = 0; i < features.length; i++) {
					requiredFeatureIDs.add(((FeatureLaunchModel) features[i]).getId());
					getFeatureDependencies(((FeatureLaunchModel) features[i]).getModel(fFeatureWorkspaceButton.getSelection()), requiredFeatureIDs);
				}

				Set toCheck = new HashSet();
				for (Iterator iterator = requiredFeatureIDs.iterator(); iterator.hasNext();) {
					Object featureModel = fFeatureModels.get(iterator.next());
					if (featureModel != null) {
						toCheck.add(featureModel);
					}
				}
				fTree.setCheckedElements(toCheck.toArray());
			}
		}

		private void getFeatureDependencies(IFeatureModel model, Set requiredFeatureIDs) {
			IFeature feature = model.getFeature();
			IFeatureImport[] featureImports = feature.getImports();
			for (int i = 0; i < featureImports.length; i++) {
				if (featureImports[i].getType() == IFeatureImport.FEATURE) {
					addFeature(requiredFeatureIDs, featureImports[i].getId());
				}
			}

			IFeatureChild[] featureIncludes = feature.getIncludedFeatures();
			for (int i = 0; i < featureIncludes.length; i++) {
				addFeature(requiredFeatureIDs, featureIncludes[i].getId());
			}
		}

		private void addFeature(Set requiredFeatureIDs, String id) {
			if (!requiredFeatureIDs.contains(id)) {
				FeatureLaunchModel model = (FeatureLaunchModel) fFeatureModels.get(id);
				if (model != null) {
					requiredFeatureIDs.add(id);
					getFeatureDependencies(model.getModel(fFeatureWorkspaceButton.getSelection()), requiredFeatureIDs);
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
			fWorkspacePluginButton.setSelection(true);
			fExternalPluginButton.setSelection(false);
			for (Iterator iterator = fFeatureModels.values().iterator(); iterator.hasNext();) {
				FeatureLaunchModel model = (FeatureLaunchModel) iterator.next();
				model.setPluginResolution(IPDELauncherConstants.LOCATION_DEFAULT);
			}
			fTree.setAllChecked(true);
			fTree.refresh();
			updateCounter();
		}

		private void handleSelectAll(boolean state) {
			fTree.setAllChecked(state);
			updateCounter();
		}
	}

	class LocationCellModifier implements ICellModifier {
		public boolean canModify(Object element, String property) {
			return fTree.getChecked(element);
		}

		public Object getValue(Object element, String property) {
			if (property == PROPERTY_RESOLUTION) {
				String location = ((FeatureLaunchModel) element).getResolutionValue();
				if (location.equalsIgnoreCase(IPDELauncherConstants.LOCATION_DEFAULT)) {
					return new Integer(0);
				} else if (location.equalsIgnoreCase(IPDELauncherConstants.LOCATION_WORKSPACE)) {
					return new Integer(1);
				} else if (location.equalsIgnoreCase(IPDELauncherConstants.LOCATION_EXTERNAL)) {
					return new Integer(2);
				}
			}
			return null;
		}

		public void modify(Object item, String property, Object value) {
			if (property == PROPERTY_RESOLUTION) {
				FeatureLaunchModel model = (FeatureLaunchModel) ((TreeItem) item).getData();
				int comboIndex = ((Integer) value).intValue();
				String location = null;
				switch (comboIndex) {
					case 0 :
						location = IPDELauncherConstants.LOCATION_DEFAULT;
						break;
					case 1 :
						location = IPDELauncherConstants.LOCATION_WORKSPACE;
						break;
					case 2 :
						location = IPDELauncherConstants.LOCATION_EXTERNAL;
				}

				model.setPluginResolution(location);
				fTree.refresh(model, true);
				fTab.updateLaunchConfigurationDialog();
			}
		}
	}

	class PluginContentProvider implements ITreeContentProvider {

		public Object[] getElements(Object input) {
			if (input instanceof Collection) {
				return ((Collection) input).toArray();
			}
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

	class TreeSorter extends ViewerSorter {
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

	class FeatureLaunchModel {
		public IFeatureModel fWorkspaceModel;
		public IFeatureModel fTargetModel;
		public String fPluginResolution;

		public FeatureLaunchModel(IFeatureModel workspaceModel, IFeatureModel targetModel) {
			fWorkspaceModel = workspaceModel;
			fTargetModel = targetModel;
			fPluginResolution = IPDELauncherConstants.LOCATION_DEFAULT;
		}

		public void setWorkspaceModel(IFeatureModel workspaceModel) {
			fWorkspaceModel = workspaceModel;
		}

		public void setTargetModel(IFeatureModel targetModel) {
			fTargetModel = targetModel;
		}

		public void setPluginResolution(String resolution) {
			fPluginResolution = resolution;
		}

		public String getId() {
			if (fWorkspaceModel != null) {
				return fWorkspaceModel.getFeature().getId();
			} else if (fTargetModel != null) {
				return fTargetModel.getFeature().getId();
			}
			return ""; //$NON-NLS-1$
		}

		public String getVersion() {
			if (fWorkspaceModel != null) {
				return fWorkspaceModel.getFeature().getVersion();
			} else if (fTargetModel != null) {
				return fTargetModel.getFeature().getVersion();
			}
			return ""; //$NON-NLS-1$
		}

		public IFeatureModel getModel(boolean workspaceFirst) {
			if (fWorkspaceModel == null) {
				return fTargetModel;
			}
			if (fTargetModel == null) {
				return fWorkspaceModel;
			}
			if (workspaceFirst) {
				return fWorkspaceModel;
			}
			return fTargetModel;
		}

		/**
		 * @return the translated string label for the current resolution value
		 */
		public String getResolutionLabel() {
			if (fPluginResolution.equalsIgnoreCase(IPDELauncherConstants.LOCATION_DEFAULT)) {
				return PDEUIMessages.FeatureBlock_default;
			} else if (fPluginResolution.equalsIgnoreCase(IPDELauncherConstants.LOCATION_WORKSPACE)) {
				return PDEUIMessages.FeatureBlock_workspaceBefore;
			} else if (fPluginResolution.equalsIgnoreCase(IPDELauncherConstants.LOCATION_EXTERNAL)) {
				return PDEUIMessages.FeatureBlock_externalBefore;
			}
			return ""; //$NON-NLS-1$
		}

		/**
		 * @return resolution value that can be stored in the config, one of {@link IPDELauncherConstants#LOCATION_DEFAULT}, {@link IPDELauncherConstants#LOCATION_WORKSPACE} or {@link IPDELauncherConstants#LOCATION_EXTERNAL}
		 */
		public String getResolutionValue() {
			return fPluginResolution;
		}
	}

	private static final int COLUMN_FEATURE_NAME = 0;
	private static final int COLUMN_FEATURE_VERSION = 1;
	private static final int COLUMN_PLUGIN_RESOLUTION = 2;

	private static final String COLUMN_ID = "columnID"; //$NON-NLS-1$
	private static final String PROPERTY_RESOLUTION = "resolution"; //$NON-NLS-1$

	private Button fAddRequiredFeaturesButton;
	private Button fDefaultsButton;
	private Button fSelectAllButton;
	private Button fDeselectAllButton;

	private Button fWorkspacePluginButton;
	private Button fExternalPluginButton;
	private Label fCounter;

	private Button fFeatureWorkspaceButton;
	private Button fAutoValidate;
	private Button fValidateButton;

	private ILaunchConfiguration fLaunchConfig;
	private ButtonSelectionListener fListener;
	private AbstractLauncherTab fTab;
	private CachedCheckboxTreeViewer fTree;
	private LaunchValidationOperation fOperation;

	/**
	 * Maps feature ID to the FeatureLaunchModel that represents the feature in the tree
	 */
	private Map fFeatureModels;

	public FeatureBlock(PluginsTab pluginsTab) {
		Assert.isNotNull(pluginsTab);
		fTab = pluginsTab;
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	public void createControl(Composite parent, int span, int indent) {
		fListener = new ButtonSelectionListener();

		Composite composite = SWTFactory.createComposite(parent, 1, span, GridData.FILL_BOTH, 0, 0);

		Composite treeGroup = SWTFactory.createComposite(composite, 2, 1, GridData.FILL_BOTH, 0, 0);
		SWTFactory.createLabel(treeGroup, PDEUIMessages.FeatureBlock_FeatureGroupDescription, 2);
		createCheckBoxTree(treeGroup);
		createButtonContainer(treeGroup, 10);

		fFeatureWorkspaceButton = SWTFactory.createCheckButton(treeGroup, PDEUIMessages.FeatureBlock_UseWorkspaceFeatures, null, true, 2);
		fFeatureWorkspaceButton.addSelectionListener(fListener);

		Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite validatecomp = SWTFactory.createComposite(composite, 2, 1, GridData.FILL_HORIZONTAL, 0, 0);
		fAutoValidate = SWTFactory.createCheckButton(validatecomp, NLS.bind(PDEUIMessages.PluginsTabToolBar_auto_validate, fTab.getName().replaceAll("&", "").toLowerCase(Locale.ENGLISH)), null, false, 1); //$NON-NLS-1$ //$NON-NLS-2$
		fAutoValidate.addSelectionListener(fListener);
		Composite rightAlignComp = SWTFactory.createComposite(validatecomp, 1, 1, SWT.NONE, 0, 0);
		rightAlignComp.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, true, true));
		fValidateButton = SWTFactory.createPushButton(rightAlignComp, NLS.bind(PDEUIMessages.PluginsTabToolBar_validate, fTab.getName().replaceAll("&", "")), null); //$NON-NLS-1$//$NON-NLS-2$
		fValidateButton.addSelectionListener(fListener);
	}

	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}

	private void createCheckBoxTree(Composite parent) {
		ITreeContentProvider contentProvider = new PluginContentProvider();
		PatternFilter filter = new PatternFilter() {
			public boolean isElementVisible(Viewer viewer, Object element) {
				if (element instanceof FeatureLaunchModel) {
					return super.isElementVisible(viewer, ((FeatureLaunchModel) element).getId());
				}
				return super.isElementVisible(viewer, element);
			}
		};
		filter.setIncludeLeadingWildcard(true);
		FilteredCheckboxTree tree = new FilteredCheckboxTree(parent, contentProvider, null, SWT.FULL_SELECTION, filter);

		GridData gd = new GridData(GridData.FILL_BOTH);
		tree.setLayoutData(gd);
		fTree = tree.getCheckboxTreeViewer();

		TreeColumn column1 = new TreeColumn(fTree.getTree(), SWT.LEFT);
		column1.setText(PDEUIMessages.FeatureBlock_features);
		column1.setWidth(300);
		column1.addSelectionListener(fListener);
		column1.setData(COLUMN_ID, new Integer(0));

		TreeColumn column2 = new TreeColumn(fTree.getTree(), SWT.LEFT);
		column2.setText(PDEUIMessages.FeatureBlock_version);
		column2.setWidth(250);
		column2.addSelectionListener(fListener);
		column2.setData(COLUMN_ID, new Integer(1));

		TreeColumn column3 = new TreeColumn(fTree.getTree(), SWT.CENTER);
		column3.setText(PDEUIMessages.FeatureBlock_pluginResolution);
		column3.setWidth(100);
		column3.addSelectionListener(fListener);
		column3.setData(COLUMN_ID, new Integer(3));

		fTree.getTree().setHeaderVisible(true);
		fTree.setLabelProvider(new FeatureTreeLabelProvider());
		fTree.setContentProvider(contentProvider);
		fTree.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				updateCounter();
				fTab.updateLaunchConfigurationDialog();
			}
		});
		String[] items = new String[] {PDEUIMessages.FeatureBlock_default, PDEUIMessages.FeatureBlock_workspaceBefore, PDEUIMessages.FeatureBlock_externalBefore};
		ComboBoxCellEditor cellEditor = new ComboBoxCellEditor(fTree.getTree(), items);
		cellEditor.getControl().pack();
		fTree.setCellEditors(new CellEditor[] {null, null, cellEditor, cellEditor});
		fTree.setColumnProperties(new String[] {null, null, PROPERTY_RESOLUTION});
		fTree.setCellModifier(new LocationCellModifier());
		fTree.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				ISelection selection = event.getSelection();
				if (selection == null || !(selection instanceof IStructuredSelection)) {
					return;
				}
				Object element = ((IStructuredSelection) selection).getFirstElement();
				fTree.setChecked(element, !fTree.getChecked(element));
				fTab.updateLaunchConfigurationDialog();
			}
		});
	}

	private void createButtonContainer(Composite parent, int vOffset) {
		Composite buttonComp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		layout.marginTop = vOffset;
		buttonComp.setLayout(layout);
		buttonComp.setLayoutData(new GridData(GridData.FILL_VERTICAL));

		fSelectAllButton = SWTFactory.createPushButton(buttonComp, PDEUIMessages.AdvancedLauncherTab_selectAll, null);
		fSelectAllButton.addSelectionListener(fListener);
		fDeselectAllButton = SWTFactory.createPushButton(buttonComp, PDEUIMessages.AdvancedLauncherTab_deselectAll, null);
		fDeselectAllButton.addSelectionListener(fListener);
		fAddRequiredFeaturesButton = SWTFactory.createPushButton(buttonComp, PDEUIMessages.FeatureBlock_addRequiredFeatues, null);
		fAddRequiredFeaturesButton.addSelectionListener(fListener);
		fDefaultsButton = SWTFactory.createPushButton(buttonComp, PDEUIMessages.AdvancedLauncherTab_defaults, null);
		fDefaultsButton.addSelectionListener(fListener);

		SWTFactory.createHorizontalSpacer(buttonComp, 1);

		SWTFactory.createLabel(buttonComp, PDEUIMessages.FeatureBlock_defaultPluginResolution, 1);
		fWorkspacePluginButton = SWTFactory.createRadioButton(buttonComp, PDEUIMessages.FeatureBlock_workspaceBefore);
		fWorkspacePluginButton.addSelectionListener(fListener);
		fExternalPluginButton = SWTFactory.createRadioButton(buttonComp, PDEUIMessages.FeatureBlock_externalBefore);
		fExternalPluginButton.addSelectionListener(fListener);

		Composite countComp = SWTFactory.createComposite(buttonComp, 1, 1, SWT.NONE, 0, 0);
		countComp.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, true, true));
		fCounter = new Label(countComp, SWT.NONE);
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

		setInput(config, fTree);
		updateCounter();

		// Setup other buttons
		String pluginResolution = config.getAttribute(IPDELauncherConstants.FEATURE_PLUGIN_RESOLUTION, IPDELauncherConstants.LOCATION_WORKSPACE);
		if (pluginResolution.equalsIgnoreCase(IPDELauncherConstants.LOCATION_WORKSPACE)) {
			fWorkspacePluginButton.setSelection(true);
			fExternalPluginButton.setSelection(false);
		} else {
			fWorkspacePluginButton.setSelection(false);
			fExternalPluginButton.setSelection(true);
		}
		String featureLocation = config.getAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, IPDELauncherConstants.LOCATION_WORKSPACE);
		fFeatureWorkspaceButton.setSelection(featureLocation.equalsIgnoreCase(IPDELauncherConstants.LOCATION_WORKSPACE));
		fAutoValidate.setSelection(config.getAttribute(IPDELauncherConstants.AUTOMATIC_VALIDATE, false));

		// If the workspace plug-in state has changed (project closed, etc.) the launch config needs to be updated without making the tab dirty
		if (fLaunchConfig.isWorkingCopy()) {
			savePluginState((ILaunchConfigurationWorkingCopy) fLaunchConfig);
		}

		fTab.updateLaunchConfigurationDialog();

		PDEPreferencesManager prefs = new PDEPreferencesManager(IPDEUIConstants.PLUGIN_ID);
		int index = prefs.getInt(IPreferenceConstants.FEATURE_SORT_COLUMN);
		TreeColumn column = fTree.getTree().getColumn(index == 0 ? COLUMN_FEATURE_NAME : index - 1);
		fListener.handleColumn(column, prefs.getInt(IPreferenceConstants.FEATURE_SORT_ORDER));
		fTree.refresh(true);
	}

	public void performApply(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IPDELauncherConstants.SHOW_SELECTED_ONLY, false);
		config.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, fFeatureWorkspaceButton.getSelection() ? IPDELauncherConstants.LOCATION_WORKSPACE : IPDELauncherConstants.LOCATION_EXTERNAL);
		config.setAttribute(IPDELauncherConstants.FEATURE_PLUGIN_RESOLUTION, fWorkspacePluginButton.getSelection() ? IPDELauncherConstants.LOCATION_WORKSPACE : IPDELauncherConstants.LOCATION_EXTERNAL);
		config.setAttribute(IPDELauncherConstants.AUTOMATIC_VALIDATE, fAutoValidate.getSelection());
		savePluginState(config);
		saveSortOrder();
		updateCounter();
	}

	private void savePluginState(ILaunchConfigurationWorkingCopy config) {
		Set featuresEntry = new HashSet(); // By using a set, debug will sort the attribute for us
		Object[] checked = fTree.getCheckedElements();
		for (int i = 0; i < checked.length; i++) {
			FeatureLaunchModel model = (FeatureLaunchModel) checked[i];
			StringBuffer buffer = new StringBuffer();
			buffer.append(model.getId());
			buffer.append(':');
			buffer.append(model.getResolutionValue());
			featuresEntry.add(buffer.toString());
		}
		config.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, featuresEntry);
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
		config.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, IPDELauncherConstants.LOCATION_WORKSPACE);
		config.setAttribute(IPDELauncherConstants.FEATURE_PLUGIN_RESOLUTION, IPDELauncherConstants.LOCATION_WORKSPACE);
		config.setAttribute(IPDELauncherConstants.AUTOMATIC_VALIDATE, false);
	}

	private void updateCounter() {
		if (fCounter != null) {
			int checked = fTree.getCheckedElements().length;
			int total = fFeatureModels.values().size();
			fCounter.setText(NLS.bind(PDEUIMessages.AbstractPluginBlock_counter, new Integer(checked), new Integer(total)));
		}
	}

	/**
	 * Sets input to the tree, including location selection and checked features
	 * 
	 * @param config configuration to get attributes from
	 * @param tree tree to set input and checked items on
	 */
	protected void setInput(ILaunchConfiguration config, CheckboxTreeViewer tree) {
		// Maps feature IDs to their models
		Map featureModels = new HashMap();
		FeatureModelManager fmm = new FeatureModelManager();
		IFeatureModel[] workspaceModels = fmm.getWorkspaceModels();
		for (int i = 0; i < workspaceModels.length; i++) {
			String id = workspaceModels[i].getFeature().getId();
			featureModels.put(id, new FeatureLaunchModel(workspaceModels[i], null));
		}
		fmm.shutdown();

		// If there is both a workspace and a target model with the same id, combine them into the same launch model
		ExternalFeatureModelManager efmm = new ExternalFeatureModelManager();
		efmm.startup();
		IFeatureModel[] externalModels = efmm.getModels();
		for (int i = 0; i < externalModels.length; i++) {
			String id = externalModels[i].getFeature().getId();
			if (featureModels.containsKey(id)) {
				FeatureLaunchModel launchModel = (FeatureLaunchModel) featureModels.get(id);
				launchModel.setTargetModel(externalModels[i]);
			} else {
				featureModels.put(id, new FeatureLaunchModel(null, externalModels[i]));
			}
		}
		efmm.shutdown();

		fFeatureModels = featureModels;
		tree.setInput(fFeatureModels.values());

		// Loop through the saved config to determine location settings and selection
		try {
			Set selected = config.getAttribute(IPDELauncherConstants.SELECTED_FEATURES, (Set) null);
			if (selected == null) {
				tree.setCheckedElements(fFeatureModels.values().toArray());
			} else {
				ArrayList selectedFeatureList = new ArrayList();
				for (Iterator iterator = selected.iterator(); iterator.hasNext();) {
					String currentSelected = (String) iterator.next();
					String[] attributes = currentSelected.split(":"); //$NON-NLS-1$
					if (attributes.length > 0) {
						String id = attributes[0];
						FeatureLaunchModel model = (FeatureLaunchModel) fFeatureModels.get(id);
						if (model != null) {
							selectedFeatureList.add(model);
							if (attributes.length > 1) {
								model.setPluginResolution(attributes[1]);
							}
						}
					}
				}
				tree.setCheckedElements(selectedFeatureList.toArray());
			}
		} catch (CoreException e) {
			PDELaunchingPlugin.log(e);
		}

		updateCounter();
	}

}
