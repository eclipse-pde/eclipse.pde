/*******************************************************************************
 * Copyright (c) 2010, 2012 IBM Corporation and others.
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
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.launching.PDELaunchingPlugin;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.eclipse.pde.internal.launching.launcher.LaunchValidationOperation;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.dialogs.FeatureSelectionDialog;
import org.eclipse.pde.internal.ui.dialogs.PluginSelectionDialog;
import org.eclipse.pde.internal.ui.elements.NamedElement;
import org.eclipse.pde.internal.ui.shared.CachedCheckboxTreeViewer;
import org.eclipse.pde.internal.ui.shared.FilteredCheckboxTree;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.ui.launcher.AbstractLauncherTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * Provides the UI that is displayed in the Plug-ins tab of PDE launch configs when the user
 * has chosen to launch using "features selected below".  Provides a filterable tree that
 * the user can select which features to launch, as well as where the feature should be taken
 * from (Workspace or External) and where plug-ins should be collected from (Workspace first
 * or External first).
 */
public class FeatureBlock {

	/**
	 * Label provider for the tree.  Implements ILabelProvider so it can support the text filter (see PatternFilter)
	 */
	class FeatureTreeLabelProvider extends StyledCellLabelProvider implements ILabelProvider {

		PDELabelProvider pdeLabelProvider = new PDELabelProvider();

		/* (non-Javadoc)
		 * @see org.eclipse.pde.internal.ui.util.SharedLabelProvider#getColumnImage(java.lang.Object, int)
		 */
		public Image getColumnImage(Object obj, int index) {
			// If there is a workspace feature available, display the workspace feature icon, even if the user has selected external
			if (index == COLUMN_FEATURE_NAME) {
				if (obj instanceof FeatureLaunchModel) {
					FeatureLaunchModel model = (FeatureLaunchModel) obj;
					return pdeLabelProvider.getImage(model.getModel(true));
				} else if (obj instanceof NamedElement) {
					return ((NamedElement) obj).getImage();
				} else if (obj instanceof PluginLaunchModel) {
					IPluginModelBase pluginModelBase = ((PluginLaunchModel) obj).getPluginModelBase();
					return pdeLabelProvider.getColumnImage(pluginModelBase, index);
				}
			} else if (index == COLUMN_PLUGIN_RESOLUTION) {
				if (obj instanceof PluginLaunchModel) {
					PluginLaunchModel pluginLaunchModel = ((PluginLaunchModel) obj);
					if (IPDELauncherConstants.LOCATION_WORKSPACE.equalsIgnoreCase(pluginLaunchModel.getPluginResolution())) {
						ModelEntry modelEntry = PluginRegistry.findEntry(pluginLaunchModel.getPluginModelId());
						if (!modelEntry.hasWorkspaceModels()) {
							return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);
						}
					} else if (IPDELauncherConstants.LOCATION_EXTERNAL.equalsIgnoreCase(pluginLaunchModel.getPluginResolution())) {
						ModelEntry modelEntry = PluginRegistry.findEntry(pluginLaunchModel.getPluginModelId());
						if (!modelEntry.hasExternalModels()) {
							return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);
						}
					}
				}
			}
			return null;
		}

		public String getColumnText(Object obj, int index) {
			if (obj instanceof NamedElement && index == COLUMN_FEATURE_NAME)
				return ((NamedElement) obj).getLabel();
			if (obj instanceof PluginLaunchModel) {
				PluginLaunchModel pluginLaunchModel = (PluginLaunchModel) obj;
				switch (index) {
					case COLUMN_FEATURE_NAME :
						return pluginLaunchModel.getPluginModelId();
					case COLUMN_PLUGIN_RESOLUTION :
						return getResolutionLabel(pluginLaunchModel.getPluginResolution());
				}
			}
			if (obj instanceof FeatureLaunchModel) {
				FeatureLaunchModel model = (FeatureLaunchModel) obj;
				switch (index) {
					case COLUMN_FEATURE_NAME :
						return model.getId();
					case COLUMN_PLUGIN_RESOLUTION :
						return getResolutionLabel(model.getResolutionValue());
					default :
						return ""; //$NON-NLS-1$
				}
			}
			return ""; //$NON-NLS-1$
		}

		public void update(ViewerCell cell) {
			switch (cell.getColumnIndex()) {
				case COLUMN_FEATURE_NAME :
					StyledString label = getStyledText(cell.getElement());
					cell.setStyleRanges(label.getStyleRanges());
					cell.setText(label.toString());
					cell.setImage(getColumnImage(cell.getElement(), COLUMN_FEATURE_NAME));
					break;
				case COLUMN_PLUGIN_RESOLUTION :
					cell.setText(getColumnText(cell.getElement(), COLUMN_PLUGIN_RESOLUTION));
					cell.setImage(getColumnImage(cell.getElement(), COLUMN_PLUGIN_RESOLUTION));
					break;
			}
			super.update(cell);
		}

		private StyledString getStyledText(Object element) {
			StyledString styledString = new StyledString(getColumnText(element, COLUMN_FEATURE_NAME));
			if (element instanceof FeatureLaunchModel) {
				FeatureLaunchModel featureModel = (FeatureLaunchModel) element;
				styledString.append(" (", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
				String version = featureModel.getVersion();
				int index = version.indexOf('-');
				if (index > -1)
					version = version.substring(0, index);
				styledString.append(version, StyledString.QUALIFIER_STYLER);
				styledString.append(")", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
			} else if (element instanceof PluginLaunchModel) {
				PluginLaunchModel pluginLaunchModel = (PluginLaunchModel) element;
				styledString.append(" (", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
				String version = pluginLaunchModel.getPluginModelBase().getPluginBase().getVersion();
				int index = version.indexOf('-');
				if (index > -1)
					version = version.substring(0, index);
				styledString.append(version, StyledString.QUALIFIER_STYLER);
				styledString.append(")", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
			}
			return styledString;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
		 */
		public Image getImage(Object element) {
			// Only the name column gets an image, see getColumnImage()
			return null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object element) {
			// If the label provider implement ILabelProvider the ViewerComparator calls getText() with whatever was passed to it, in our case we are already passing the label text based on sort order
			if (element instanceof String) {
				return (String) element;
			}
			return getColumnText(element, 0);
		}
	}

	class ButtonSelectionListener extends SelectionAdapter {

		public void widgetSelected(SelectionEvent e) {
			Object source = e.getSource();
			if (source == fValidateButton) {
				handleValidate();
			} else if (source == fSelectFeaturesButton) {
				handleSelectFeatures();
			} else if (source == fAddPluginButton) {
				handleAddPlugin();
			} else if (source == fSelectAllButton) {
				handleSelectAll(true);
			} else if (source == fDeselectAllButton) {
				handleSelectAll(false);
			} else if (source == fAddRequiredFeaturesButton) {
				handleAddRequired();
			} else if (source == fDefaultsButton) {
				handleRestoreDefaults();
			} else if (source == fRemovePluginButton) {
				handleRemovePlugin();
			} else if (source == fFilterButton) {
				handleFilterButton();
			} else if (source instanceof TreeColumn) {
				handleColumn((TreeColumn) source, 0);
			}
			if (!fIsDisposed)
				fTab.updateLaunchConfigurationDialog();
		}

		private void handleSelectFeatures() {
			ArrayList featureModels = new ArrayList();
			for (Iterator iterator = fFeatureModels.values().iterator(); iterator.hasNext();) {
				FeatureLaunchModel featureLaunchModel = (FeatureLaunchModel) iterator.next();
				if (!fTree.getChecked(featureLaunchModel)) {
					featureModels.add(featureLaunchModel.getModel(true));
				}
			}
			if (featureModels.size() == 0) {
				MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.FeatureSelectionDialog_title, PDEUIMessages.FeatureBlock_AllFeatureSelected);
				return;
			}
			FeatureSelectionDialog dialog = new FeatureSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), (IFeatureModel[]) featureModels.toArray(new IFeatureModel[featureModels.size()]), true);
			dialog.create();
			if (dialog.open() == Window.OK) {
				fTree.getControl().setRedraw(false);
				fTree.removeFilter(fSelectedOnlyFilter);
				Object[] selectedModels = dialog.getResult();
				for (int i = 0; i < selectedModels.length; i++) {
					String id = ((IFeatureModel) selectedModels[i]).getFeature().getId();
					fTree.setChecked(fFeatureModels.get(id), true);
				}
				if (fFilterButton.getSelection()) {
					fTree.addFilter(fSelectedOnlyFilter);
				}
				fTree.getControl().setRedraw(true);
				updateCounter();
			}
		}

		private void handleFilterButton() {
			if (fFilterButton.getSelection()) {
				fTree.addFilter(fSelectedOnlyFilter);
			} else {
				fTree.removeFilter(fSelectedOnlyFilter);
			}
			fTree.expandAll();
		}

		private void handleRemovePlugin() {
			// Any changes here need to be reflected in the SWT.DEL key remove handling
			IStructuredSelection selection = (IStructuredSelection) fTree.getSelection();
			int index = fAdditionalPlugins.indexOf(selection.getFirstElement());
			fAdditionalPlugins.removeAll(selection.toList());
			fTree.remove(selection.toArray());
			List input = (List) fTree.getInput();
			input.removeAll(selection.toList());
			if (fAdditionalPlugins.size() == 0) {
				fTree.remove(fAdditionalPluginsParentElement);
				input.remove(fAdditionalPluginsParentElement);
				fRemovePluginButton.setEnabled(false);
			} else {
				index--;
				fTree.setSelection(new StructuredSelection(fAdditionalPlugins.get(index > 0 ? index : 0)), true);
				fRemovePluginButton.setEnabled(true);
			}
			updateCounter();
		}

		private void handleAddPlugin() {
			PluginSelectionDialog dialog = new PluginSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), getAvailablePlugins(), true);
			if (dialog.open() == Window.OK) {

				Object[] models = dialog.getResult();
				ArrayList modelList = new ArrayList(models.length);
				for (int i = 0; i < models.length; i++) {
					PluginLaunchModel pluginLaunchModel = new PluginLaunchModel((IPluginModelBase) models[i], IPDELauncherConstants.LOCATION_DEFAULT);
					modelList.add(pluginLaunchModel);
				}

				List input = (List) fTree.getInput();
				if (!input.contains(fAdditionalPluginsParentElement)) {
					input.add(fAdditionalPluginsParentElement);
				}
				fAdditionalPlugins.addAll(modelList);

				fTree.getControl().setRedraw(false);
				fTree.removeFilter(fSelectedOnlyFilter);
				fTree.refresh();
				for (Iterator iterator = modelList.iterator(); iterator.hasNext();) {
					fTree.setChecked(iterator.next(), true);
				}
				fTree.setSelection(new StructuredSelection(modelList.get(modelList.size() - 1)), true);
				fTree.getTree().setSortColumn(fTree.getTree().getSortColumn());
				fTree.getTree().setSortDirection(fTree.getTree().getSortDirection());
				if (fFilterButton.getSelection()) {
					fTree.addFilter(fSelectedOnlyFilter);
				}
				fTree.getControl().setRedraw(true);
				updateCounter();
			}
		}

		private IPluginModelBase[] getAvailablePlugins() {
			IPluginModelBase[] plugins = PluginRegistry.getActiveModels(false);
			if (fAdditionalPlugins.isEmpty()) {
				return plugins;
			}
			Set additionalPlugins = new HashSet();
			for (Iterator iterator = fAdditionalPlugins.iterator(); iterator.hasNext();) {
				PluginLaunchModel model = (PluginLaunchModel) iterator.next();
				additionalPlugins.add(model.getPluginModelBase());
			}
			List result = new ArrayList();
			for (int i = 0; i < plugins.length; i++) {
				if (!additionalPlugins.contains(plugins[i])) {
					result.add(plugins[i]);
				}
			}
			return (IPluginModelBase[]) result.toArray(new IPluginModelBase[result.size()]);
		}

		private void handleValidate() {
			if (fOperation == null)
				// Unlike PluginBlock, we don't want to validate the application/product requirements because we will grab them automatically at launch time
				fOperation = new LaunchValidationOperation(fLaunchConfig) {
					protected IPluginModelBase[] getModels() throws CoreException {
						// The feature block is used in both the OSGi config and Eclipse configs, use the tab id to determine which we are using
						boolean isOSGiTab = fTab.getId().equals(IPDELauncherConstants.TAB_BUNDLES_ID);
						return BundleLauncherHelper.getMergedBundles(fLaunchConfiguration, isOSGiTab);
					}
				};
			try {
				fOperation.run(new NullProgressMonitor());
			} catch (CoreException e) {
				PDEPlugin.log(e);
			}
			if (fDialog == null) {
				if (fOperation.hasErrors()) {
					fDialog = new PluginStatusDialog(getShell(), SWT.MODELESS | SWT.CLOSE | SWT.BORDER | SWT.TITLE | SWT.RESIZE);
					fDialog.setInput(fOperation.getInput());
					fDialog.open();
					fDialog = null;
				} else if (fOperation.isEmpty()) {
					MessageDialog.openInformation(getShell(), PDEUIMessages.PluginStatusDialog_pluginValidation, NLS.bind(PDEUIMessages.AbstractLauncherToolbar_noSelection, fTab.getName().toLowerCase(Locale.ENGLISH)));
				} else {
					MessageDialog.openInformation(getShell(), PDEUIMessages.PluginStatusDialog_pluginValidation, PDEUIMessages.AbstractLauncherToolbar_noProblems);
				}
			} else {
				if (fOperation.getInput().size() > 0)
					fDialog.refresh(fOperation.getInput());
				else {
					Map input = new HashMap(1);
					input.put(PDEUIMessages.AbstractLauncherToolbar_noProblems, Status.OK_STATUS);
					fDialog.refresh(input);
				}
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
					if (features[i] instanceof FeatureLaunchModel) {
						requiredFeatureIDs.add(((FeatureLaunchModel) features[i]).getId());
						getFeatureDependencies(((FeatureLaunchModel) features[i]).getModel(fFeatureWorkspaceButton.getSelection()), requiredFeatureIDs);
					}
				}

				fTree.getControl().setRedraw(false);
				fTree.removeFilter(fSelectedOnlyFilter);
				for (Iterator iterator = requiredFeatureIDs.iterator(); iterator.hasNext();) {
					Object featureModel = fFeatureModels.get(iterator.next());
					if (featureModel != null) {
						fTree.setChecked(featureModel, true);
					}
				}
				if (fFilterButton.getSelection()) {
					fTree.addFilter(fSelectedOnlyFilter);
				}
				fTree.getControl().setRedraw(true);
				updateCounter();
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
			int sortOrder = sortDirn == SWT.UP ? -1 : 1;
			int sortColumn = ((Integer) tc.getData(COLUMN_ID)).intValue();
			fTree.setSorter(new TreeSorter(sortColumn, sortOrder));
			saveSortOrder();
		}

		private void handleRestoreDefaults() {
			fWorkspacePluginButton.setSelection(true);
			fExternalPluginButton.setSelection(false);

			List input = (List) fTree.getInput();
			input.removeAll(fAdditionalPlugins);
			input.remove(fAdditionalPluginsParentElement);
			fAdditionalPlugins.clear();

			fRemovePluginButton.setEnabled(false);
			for (Iterator iterator = fFeatureModels.values().iterator(); iterator.hasNext();) {
				FeatureLaunchModel model = (FeatureLaunchModel) iterator.next();
				model.setPluginResolution(IPDELauncherConstants.LOCATION_DEFAULT);
			}

			fTree.getControl().setRedraw(false);
			fTree.removeFilter(fSelectedOnlyFilter);
			fTree.setCheckedElements(new Object[0]); // Make sure the check state cache is cleared
			fTree.refresh();
			fTree.setAllChecked(true);
			if (fFilterButton.getSelection()) {
				fTree.addFilter(fSelectedOnlyFilter);
			}
			fTree.getControl().setRedraw(true);
			updateCounter();
		}

		private void handleSelectAll(boolean state) {
			fTree.getControl().setRedraw(false);
			fTree.removeFilter(fSelectedOnlyFilter);
			fTree.setAllChecked(state);
			if (fFilterButton.getSelection()) {
				fTree.addFilter(fSelectedOnlyFilter);
			}
			fTree.getControl().setRedraw(true);
			updateCounter();
		}
	}

	class LocationCellModifier implements ICellModifier {
		public boolean canModify(Object element, String property) {
			if (element instanceof FeatureLaunchModel || element instanceof PluginLaunchModel) {
				return fTree.getChecked(element);
			}
			return false;
		}

		public Object getValue(Object element, String property) {
			if (property == PROPERTY_RESOLUTION) {
				if (element instanceof FeatureLaunchModel) {
					String location = ((FeatureLaunchModel) element).getResolutionValue();
					return getLocationIndex(location);
				} else if (element instanceof PluginLaunchModel) {
					String location = ((PluginLaunchModel) element).getPluginResolution();
					return getLocationIndex(location);
				}
			}
			return null;
		}

		private Object getLocationIndex(String location) {
			if (location.equalsIgnoreCase(IPDELauncherConstants.LOCATION_DEFAULT)) {
				return new Integer(0);
			} else if (location.equalsIgnoreCase(IPDELauncherConstants.LOCATION_WORKSPACE)) {
				return new Integer(1);
			} else if (location.equalsIgnoreCase(IPDELauncherConstants.LOCATION_EXTERNAL)) {
				return new Integer(2);
			}
			return null;
		}

		public void modify(Object item, String property, Object value) {
			if (property == PROPERTY_RESOLUTION && value != null) {
				Object data = ((TreeItem) item).getData();
				int comboIndex = ((Integer) value).intValue();
				String location = getLocation(comboIndex);
				if (data instanceof FeatureLaunchModel) {
					FeatureLaunchModel model = (FeatureLaunchModel) data;
					if (!location.equalsIgnoreCase(model.getResolutionValue())) {
						model.setPluginResolution(location);
						fTree.refresh(model, true);
						fTab.updateLaunchConfigurationDialog();
					}
				} else if (data instanceof PluginLaunchModel) {
					PluginLaunchModel pluginLaunchModel = (PluginLaunchModel) data;
					if (!location.equalsIgnoreCase(pluginLaunchModel.getPluginResolution())) {
						pluginLaunchModel.setPluginResolution(location);
						fTree.refresh(pluginLaunchModel, true);
						fTab.updateLaunchConfigurationDialog();
					}
				}

			}
		}

		private String getLocation(int comboIndex) {
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
			return location;
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
			if (parentElement == fAdditionalPluginsParentElement) {
				return fAdditionalPlugins.toArray();
			}
			return new Object[0];
		}

		public Object getParent(Object element) {
			if (element instanceof PluginLaunchModel)
				return fAdditionalPluginsParentElement;
			return null;
		}

		public boolean hasChildren(Object element) {
			if (element instanceof NamedElement)
				return true;
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
			if (e1 == fAdditionalPluginsParentElement) {
				return 1;
			}
			if (e2 == fAdditionalPluginsParentElement) {
				return -1;
			}
			FeatureTreeLabelProvider labelProvider = (FeatureTreeLabelProvider) fTree.getLabelProvider();
			return sortOrder * super.compare(viewer, labelProvider.getColumnText(e1, sortColumn), labelProvider.getColumnText(e2, sortColumn));
		}
	}

	class PluginLaunchModel {
		private IPluginModelBase fPluginModelBase;
		private String fPluginResolution;

		public PluginLaunchModel(IPluginModelBase pluginModelBase, String pluginResolution) {
			fPluginModelBase = pluginModelBase;
			fPluginResolution = pluginResolution;
		}

		public IPluginModelBase getPluginModelBase() {
			return fPluginModelBase;
		}

		public String getPluginResolution() {
			return fPluginResolution;
		}

		public void setPluginResolution(String pluginResolution) {
			fPluginResolution = pluginResolution;
		}

		public String getPluginModelId() {
			return fPluginModelBase.getPluginBase().getId();
		}

		public String getPluginModelVersion() {
			return fPluginModelBase.getPluginBase().getVersion();
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
		 * @return resolution value that can be stored in the config, one of {@link IPDELauncherConstants#LOCATION_DEFAULT}, {@link IPDELauncherConstants#LOCATION_WORKSPACE} or {@link IPDELauncherConstants#LOCATION_EXTERNAL}
		 */
		public String getResolutionValue() {
			return fPluginResolution;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			if (fWorkspaceModel != null) {
				return fWorkspaceModel.getFeature().getId() + " " + fPluginResolution; //$NON-NLS-1$
			}
			if (fTargetModel != null) {
				return fTargetModel.getFeature().getId() + " " + fPluginResolution; //$NON-NLS-1$
			}
			return fPluginResolution;
		}
	}

	/**
	 * @return the translated string label for the current resolution value
	 */
	public String getResolutionLabel(String pluginResolution) {
		if (pluginResolution.equalsIgnoreCase(IPDELauncherConstants.LOCATION_DEFAULT)) {
			return PDEUIMessages.FeatureBlock_default;
		} else if (pluginResolution.equalsIgnoreCase(IPDELauncherConstants.LOCATION_WORKSPACE)) {
			return PDEUIMessages.FeatureBlock_WorkspaceResolutionLabel;
		} else if (pluginResolution.equalsIgnoreCase(IPDELauncherConstants.LOCATION_EXTERNAL)) {
			return PDEUIMessages.FeatureBlock_ExternalResolutionLabel;
		}
		return ""; //$NON-NLS-1$
	}

	private static final int COLUMN_FEATURE_NAME = 0;
	private static final int COLUMN_PLUGIN_RESOLUTION = 1;

	private static final String COLUMN_ID = "columnID"; //$NON-NLS-1$
	private static final String PROPERTY_RESOLUTION = "resolution"; //$NON-NLS-1$

	private Button fAddRequiredFeaturesButton;
	private Button fDefaultsButton;
	private Button fSelectAllButton;
	private Button fDeselectAllButton;

	private Button fWorkspacePluginButton;
	private Button fExternalPluginButton;
	private Button fFilterButton;
	private Label fCounter;

	private Button fFeatureWorkspaceButton;
	private Button fAutoValidate;
	private Button fValidateButton;
	private Button fSelectFeaturesButton;
	private Button fAddPluginButton;
	private Button fRemovePluginButton;

	private ILaunchConfiguration fLaunchConfig;
	private ButtonSelectionListener fListener;
	private AbstractLauncherTab fTab;
	private CachedCheckboxTreeViewer fTree;
	private LaunchValidationOperation fOperation;

	private ViewerFilter fSelectedOnlyFilter;
	private boolean fIsDisposed = false;
	private PluginStatusDialog fDialog;

	/**
	 * Maps feature ID to the FeatureLaunchModel that represents the feature in the tree
	 */
	private Map fFeatureModels;
	private List fAdditionalPlugins;
	private NamedElement fAdditionalPluginsParentElement;

	public FeatureBlock(AbstractLauncherTab pluginsTab) {
		Assert.isNotNull(pluginsTab);
		fTab = pluginsTab;
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	public void createControl(Composite parent, int span, int indent) {
		fListener = new ButtonSelectionListener();
		fSelectedOnlyFilter = new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				return fTree.getChecked(element);
			}
		};

		Composite composite = SWTFactory.createComposite(parent, 1, span, GridData.FILL_BOTH, 0, 0);

		Composite treeGroup = SWTFactory.createComposite(composite, 2, 1, GridData.FILL_BOTH, 0, 0);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalIndent = indent;
		treeGroup.setLayoutData(gd);
		SWTFactory.createLabel(treeGroup, PDEUIMessages.FeatureBlock_FeatureGroupDescription, 2);
		createCheckBoxTree(treeGroup);
		createButtonContainer(treeGroup, 10);

		fFeatureWorkspaceButton = SWTFactory.createCheckButton(treeGroup, PDEUIMessages.FeatureBlock_UseWorkspaceFeatures, null, true, 2);
		fFeatureWorkspaceButton.addSelectionListener(fListener);

		Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite validatecomp = SWTFactory.createComposite(composite, 2, 1, GridData.FILL_HORIZONTAL, 0, 0);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent = indent;
		validatecomp.setLayoutData(gd);

		fAutoValidate = SWTFactory.createCheckButton(validatecomp, NLS.bind(PDEUIMessages.PluginsTabToolBar_auto_validate, fTab.getName().replaceAll("&", "").toLowerCase(Locale.ENGLISH)), null, false, 1); //$NON-NLS-1$ //$NON-NLS-2$
		fAutoValidate.addSelectionListener(fListener);
		Composite rightAlignComp = SWTFactory.createComposite(validatecomp, 1, 1, SWT.NONE, 0, 0);
		rightAlignComp.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, true, true));
		fValidateButton = SWTFactory.createPushButton(rightAlignComp, NLS.bind(PDEUIMessages.PluginsTabToolBar_validate, fTab.getName().replaceAll("&", "")), null); //$NON-NLS-1$//$NON-NLS-2$
		fValidateButton.addSelectionListener(fListener);
	}

	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		fIsDisposed = true;
	}

	private void createCheckBoxTree(Composite parent) {
		PatternFilter filter = new PatternFilter() {
			public boolean isElementVisible(Viewer viewer, Object element) {
				if (element instanceof FeatureLaunchModel) {
					return super.isElementVisible(viewer, ((FeatureLaunchModel) element).getId());
				} else if (element instanceof PluginLaunchModel) {
					return super.isElementVisible(viewer, ((PluginLaunchModel) element).getPluginModelId());
				}
				return super.isElementVisible(viewer, element);
			}
		};
		filter.setIncludeLeadingWildcard(true);
		FilteredCheckboxTree tree = new FilteredCheckboxTree(parent, null, SWT.FULL_SELECTION, filter) {
			/* (non-Javadoc)
			 * @see org.eclipse.pde.internal.ui.shared.FilteredCheckboxTree#doCreateRefreshJob()
			 */
			protected WorkbenchJob doCreateRefreshJob() {
				// If we are only showing selected items, we need to redo the filter after text filtering is applied.  The only selected filter uses the tree's check state, which hasn't been restored correctly at filter time. 
				WorkbenchJob job = super.doCreateRefreshJob();
				job.addJobChangeListener(new JobChangeAdapter() {
					public void done(IJobChangeEvent event) {
						if (event.getResult().isOK()) {
							getDisplay().asyncExec(new Runnable() {
								public void run() {
									fTree.getControl().setRedraw(false);
									fTree.removeFilter(fSelectedOnlyFilter);
									fTree.restoreLeafCheckState();
									fTree.addFilter(fSelectedOnlyFilter);
									fTree.getControl().setRedraw(true);
								}
							});
						}
					}
				});
				return job;
			}
		};

		GridData gd = new GridData(GridData.FILL_BOTH);
		tree.setLayoutData(gd);
		fTree = tree.getCheckboxTreeViewer();

		fTree.getControl().setFont(parent.getFont());
		tree.getFilterControl().setFont(parent.getFont());

		TreeColumn column1 = new TreeColumn(fTree.getTree(), SWT.LEFT);
		column1.setText(PDEUIMessages.FeatureBlock_features);
		column1.setWidth(400);
		column1.addSelectionListener(fListener);
		column1.setData(COLUMN_ID, new Integer(COLUMN_FEATURE_NAME));

		TreeColumn column2 = new TreeColumn(fTree.getTree(), SWT.CENTER);
		column2.setText(PDEUIMessages.FeatureBlock_pluginResolution);
		column2.setWidth(100);
		column2.addSelectionListener(fListener);
		column2.setData(COLUMN_ID, new Integer(COLUMN_PLUGIN_RESOLUTION));

		fTree.getTree().setHeaderVisible(true);
		fTree.setLabelProvider(new FeatureTreeLabelProvider());
		fTree.setContentProvider(new PluginContentProvider());
		fTree.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				updateCounter();
				fTab.updateLaunchConfigurationDialog();
			}
		});
		String[] items = new String[] {PDEUIMessages.FeatureBlock_default, PDEUIMessages.FeatureBlock_WorkspaceResolutionLabel, PDEUIMessages.FeatureBlock_ExternalResolutionLabel};
		ComboBoxCellEditor cellEditor = new ComboBoxCellEditor(fTree.getTree(), items);
		cellEditor.getControl().pack();
		fTree.setCellEditors(new CellEditor[] {null, cellEditor});
		fTree.setColumnProperties(new String[] {null, PROPERTY_RESOLUTION});
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
		fTree.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) fTree.getSelection();
				boolean allPlugins = true;
				for (Iterator iterator = selection.iterator(); iterator.hasNext();) {
					Object element = iterator.next();
					if (!(element instanceof PluginLaunchModel)) {
						allPlugins = false;
					}
				}
				fRemovePluginButton.setEnabled(allPlugins);
			}
		});
		fTree.getTree().addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				if (e.keyCode == SWT.DEL) {
					// Any changes here need to be reflected in the remove button handling
					IStructuredSelection selection = (IStructuredSelection) fTree.getSelection();
					int index = fAdditionalPlugins.indexOf(selection.getFirstElement());
					List input = (List) fTree.getInput();
					for (Iterator iterator = selection.iterator(); iterator.hasNext();) {
						Object element = iterator.next();
						if (element instanceof PluginLaunchModel) {
							fAdditionalPlugins.remove(element);
							fTree.remove(element);
							input.remove(element);
						}
					}
					if (fAdditionalPlugins.size() == 0) {
						fTree.remove(fAdditionalPluginsParentElement);
						input.remove(fAdditionalPluginsParentElement);
						fRemovePluginButton.setEnabled(false);
					} else {
						index--;
						fTree.setSelection(new StructuredSelection(fAdditionalPlugins.get(index > 0 ? index : 0)), true);
						fRemovePluginButton.setEnabled(true);
					}
					updateCounter();
					fTab.updateLaunchConfigurationDialog();
				}
			}
		});
	}

	private void createButtonContainer(Composite parent, int vOffset) {
		Composite buttonComp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_VERTICAL);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		layout.marginTop = vOffset;
		buttonComp.setLayout(layout);
		buttonComp.setLayoutData(new GridData(GridData.FILL_VERTICAL));

		fSelectAllButton = SWTFactory.createPushButton(buttonComp, PDEUIMessages.AdvancedLauncherTab_selectAll, null);
		fSelectAllButton.addSelectionListener(fListener);
		fDeselectAllButton = SWTFactory.createPushButton(buttonComp, PDEUIMessages.AdvancedLauncherTab_deselectAll, null);
		fDeselectAllButton.addSelectionListener(fListener);
		fSelectFeaturesButton = SWTFactory.createPushButton(buttonComp, PDEUIMessages.FeatureBlock_SelectFeatures, null);
		fSelectFeaturesButton.addSelectionListener(fListener);
		fAddRequiredFeaturesButton = SWTFactory.createPushButton(buttonComp, PDEUIMessages.FeatureBlock_addRequiredFeatues, null);
		fAddRequiredFeaturesButton.addSelectionListener(fListener);
		fAddPluginButton = SWTFactory.createPushButton(buttonComp, NLS.bind(PDEUIMessages.FeatureBlock_AddPluginsLabel, fTab.getName().replaceAll("&", "")), null); //$NON-NLS-1$//$NON-NLS-2$
		fAddPluginButton.addSelectionListener(fListener);
		fRemovePluginButton = SWTFactory.createPushButton(buttonComp, NLS.bind(PDEUIMessages.FeatureBlock_RemovePluginsLabel, fTab.getName().replaceAll("&", "")), null); //$NON-NLS-1$//$NON-NLS-2$
		fRemovePluginButton.addSelectionListener(fListener);
		fRemovePluginButton.setEnabled(false);
		fDefaultsButton = SWTFactory.createPushButton(buttonComp, PDEUIMessages.AdvancedLauncherTab_defaults, null);
		fDefaultsButton.addSelectionListener(fListener);

		SWTFactory.createLabel(buttonComp, PDEUIMessages.FeatureBlock_defaultPluginResolution, 1);
		fWorkspacePluginButton = SWTFactory.createRadioButton(buttonComp, PDEUIMessages.FeatureBlock_workspaceBefore);
		fWorkspacePluginButton.addSelectionListener(fListener);
		fExternalPluginButton = SWTFactory.createRadioButton(buttonComp, PDEUIMessages.FeatureBlock_externalBefore);
		fExternalPluginButton.addSelectionListener(fListener);

		Composite countComp = SWTFactory.createComposite(buttonComp, 1, 1, SWT.NONE, 0, 0);
		countComp.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, true, true));

		fFilterButton = SWTFactory.createCheckButton(countComp, NLS.bind(PDEUIMessages.AdvancedLauncherTab_selectedBundles, ""), null, false, 1); //$NON-NLS-1$
		fFilterButton.addSelectionListener(fListener);

		fCounter = SWTFactory.createLabel(countComp, "", 1); //$NON-NLS-1$

		Image siteImage = PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_SITE_OBJ);
		fAdditionalPluginsParentElement = new NamedElement(NLS.bind(PDEUIMessages.FeatureBlock_AdditionalPluginsEntry, fTab.getName().replaceAll("&", "")), siteImage); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void initialize() throws CoreException {
		initializeFrom(fLaunchConfig);
	}

	public void initializeFrom(ILaunchConfiguration config) throws CoreException {
		fLaunchConfig = config;
		fTree.removeFilter(fSelectedOnlyFilter);
		setInput(config, fTree);

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

		PDEPreferencesManager prefs = new PDEPreferencesManager(IPDEUIConstants.PLUGIN_ID);
		int index = prefs.getInt(IPreferenceConstants.FEATURE_SORT_COLUMN);
		TreeColumn column = fTree.getTree().getColumn(index == 0 ? COLUMN_FEATURE_NAME : index - 1);
		fListener.handleColumn(column, prefs.getInt(IPreferenceConstants.FEATURE_SORT_ORDER));
		fRemovePluginButton.setEnabled(false);
		fFilterButton.setSelection(config.getAttribute(IPDELauncherConstants.SHOW_SELECTED_ONLY, false));
		if (fFilterButton.getSelection()) {
			fTree.addFilter(fSelectedOnlyFilter);
		}
		fTree.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
		fTree.expandAll();
		fTree.refresh(true);
		fTab.updateLaunchConfigurationDialog();
	}

	public void performApply(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IPDELauncherConstants.SHOW_SELECTED_ONLY, fFilterButton.getSelection());
		config.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, fFeatureWorkspaceButton.getSelection() ? IPDELauncherConstants.LOCATION_WORKSPACE : IPDELauncherConstants.LOCATION_EXTERNAL);
		config.setAttribute(IPDELauncherConstants.FEATURE_PLUGIN_RESOLUTION, fWorkspacePluginButton.getSelection() ? IPDELauncherConstants.LOCATION_WORKSPACE : IPDELauncherConstants.LOCATION_EXTERNAL);
		config.setAttribute(IPDELauncherConstants.AUTOMATIC_VALIDATE, fAutoValidate.getSelection());
		savePluginState(config);
		saveSortOrder();
		updateCounter();
	}

	private void savePluginState(ILaunchConfigurationWorkingCopy config) {
		Set featuresEntry = new HashSet(); // By using a set, debug will sort the attribute for us
		Set pluginsEntry = new HashSet();
		ArrayList checkPluginLaunchModels = new ArrayList();

		Object[] models = fTree.getCheckedElements();

		for (int i = 0; i < models.length; i++) {
			if (models[i] instanceof FeatureLaunchModel) {
				FeatureLaunchModel featureModel = (FeatureLaunchModel) models[i];
				StringBuffer buffer = new StringBuffer();
				buffer.append(featureModel.getId());
				buffer.append(':');
				buffer.append(featureModel.getResolutionValue());
				featuresEntry.add(buffer.toString());
			} else if (models[i] instanceof PluginLaunchModel) {
				PluginLaunchModel pluginLaunchModel = (PluginLaunchModel) models[i];
				String entry = BundleLauncherHelper.writeAdditionalPluginsEntry(pluginLaunchModel.getPluginModelBase(), pluginLaunchModel.getPluginResolution(), true);
				pluginsEntry.add(entry);
				checkPluginLaunchModels.add(pluginLaunchModel);
			}
		}

		for (Iterator iterator = fAdditionalPlugins.iterator(); iterator.hasNext();) {
			PluginLaunchModel uncheckedPluginLaunchModel = (PluginLaunchModel) iterator.next();
			if (checkPluginLaunchModels.contains(uncheckedPluginLaunchModel))
				continue;
			String entry = BundleLauncherHelper.writeAdditionalPluginsEntry(uncheckedPluginLaunchModel.getPluginModelBase(), uncheckedPluginLaunchModel.getPluginResolution(), false);
			pluginsEntry.add(entry);
		}
		config.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, featuresEntry);
		config.setAttribute(IPDELauncherConstants.ADDITIONAL_PLUGINS, pluginsEntry);

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
			int checked = fTree.getCheckedLeafCount();
			int total = fFeatureModels.values().size() + fAdditionalPlugins.size();
			fCounter.setText(NLS.bind(PDEUIMessages.AbstractPluginBlock_counter, new Integer(checked), new Integer(total)));
		}
	}

	public void setVisible(boolean visible) {
		if (!visible) {
			if (fDialog != null) {
				fDialog.close();
				fDialog = null;
			}
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
		FeatureModelManager fmm = PDECore.getDefault().getFeatureModelManager();
		IFeatureModel[] workspaceModels = fmm.getWorkspaceModels();
		for (int i = 0; i < workspaceModels.length; i++) {
			String id = workspaceModels[i].getFeature().getId();
			if (id != null) {
				//don't consider broken features: https://bugs.eclipse.org/bugs/show_bug.cgi?id=377563
				featureModels.put(id, new FeatureLaunchModel(workspaceModels[i], null));
			}
		}

		// If there is both a workspace and a target model with the same id, combine them into the same launch model
		IFeatureModel[] externalModels = fmm.getExternalModels();
		for (int i = 0; i < externalModels.length; i++) {
			String id = externalModels[i].getFeature().getId();
			if (id != null) {
				//don't consider broken features: https://bugs.eclipse.org/bugs/show_bug.cgi?id=377563
				if (featureModels.containsKey(id)) {
					FeatureLaunchModel launchModel = (FeatureLaunchModel) featureModels.get(id);
					launchModel.setTargetModel(externalModels[i]);
				} else {
					featureModels.put(id, new FeatureLaunchModel(null, externalModels[i]));
				}
			}
		}

		fFeatureModels = featureModels;
		try {
			fAdditionalPlugins = new ArrayList();
			List checkedAdditionalPlugins = new ArrayList();
			HashMap allAdditionalMap = BundleLauncherHelper.getAdditionalPlugins(config, false);
			HashMap checkedAdditionalMap = BundleLauncherHelper.getAdditionalPlugins(config, true);
			for (Iterator iterator = allAdditionalMap.keySet().iterator(); iterator.hasNext();) {
				IPluginModelBase model = (IPluginModelBase) iterator.next();
				PluginLaunchModel launchModel = new PluginLaunchModel(model, (String) allAdditionalMap.get(model));
				fAdditionalPlugins.add(launchModel);
				if (checkedAdditionalMap.containsKey(model)) {
					checkedAdditionalPlugins.add(launchModel);
				}
			}

			List models = new ArrayList(fFeatureModels.values());
			if (fAdditionalPlugins.size() > 0) {
				models.add(fAdditionalPluginsParentElement);
			}
			tree.setInput(models);

			// Loop through the saved config to determine location settings and selection
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
				List checkedElements = new ArrayList();
				checkedElements.addAll(selectedFeatureList);
				checkedElements.addAll(checkedAdditionalPlugins);
				tree.setCheckedElements(checkedElements.toArray());
			}
		} catch (CoreException e) {
			PDELaunchingPlugin.log(e);
		}

		updateCounter();
	}

}
