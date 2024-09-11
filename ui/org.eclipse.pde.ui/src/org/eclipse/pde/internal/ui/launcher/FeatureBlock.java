/*******************************************************************************
 * Copyright (c) 2010, 2022 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDEPreferencesManager;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureChild;
import org.eclipse.pde.internal.core.ifeature.IFeatureImport;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.launching.PDELaunchingPlugin;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper.AdditionalPluginData;
import org.eclipse.pde.internal.launching.launcher.LaunchValidationOperation;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.pde.internal.ui.dialogs.FeatureSelectionDialog;
import org.eclipse.pde.internal.ui.dialogs.PluginSelectionDialog;
import org.eclipse.pde.internal.ui.elements.NamedElement;
import org.eclipse.pde.internal.ui.shared.CachedCheckboxTreeViewer;
import org.eclipse.pde.internal.ui.shared.FilteredCheckboxTree;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.ui.launcher.AbstractLauncherTab;
import org.eclipse.pde.ui.launcher.BundlesTab;
import org.eclipse.pde.ui.launcher.PluginsTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
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

		public Image getColumnImage(Object obj, int index) {
			// If there is a workspace feature available, display the workspace feature icon, even if the user has selected external
			if (index == COLUMN_FEATURE_NAME) {
				if (obj instanceof FeatureLaunchModel model) {
					return pdeLabelProvider.getImage(model.getModel(true));
				} else if (obj instanceof NamedElement) {
					return ((NamedElement) obj).getImage();
				} else if (obj instanceof PluginLaunchModel) {
					IPluginModelBase pluginModelBase = ((PluginLaunchModel) obj).getPluginModelBase();
					return pdeLabelProvider.getColumnImage(pluginModelBase, index);
				}
			} else if (index == COLUMN_PLUGIN_RESOLUTION) {
				if (obj instanceof PluginLaunchModel pluginLaunchModel) {
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
			if (obj instanceof PluginLaunchModel pluginLaunchModel) {
				switch (index) {
					case COLUMN_FEATURE_NAME :
						return pluginLaunchModel.getPluginModelId();
					case COLUMN_PLUGIN_RESOLUTION :
						return getResolutionLabel(pluginLaunchModel.getPluginResolution());
				}
			}
			if (obj instanceof FeatureLaunchModel model) {
				return switch (index) {
					case COLUMN_FEATURE_NAME -> model.getId();
					case COLUMN_PLUGIN_RESOLUTION -> getResolutionLabel(model.getResolutionValue());
					default -> ""; //$NON-NLS-1$
				};
			}
			return ""; //$NON-NLS-1$
		}

		@Override
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
			if (element instanceof FeatureLaunchModel featureModel) {
				styledString.append(" (", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
				String version = featureModel.getVersion();
				int index = version.indexOf('-');
				if (index > -1)
					version = version.substring(0, index);
				styledString.append(version, StyledString.QUALIFIER_STYLER);
				styledString.append(")", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
			} else if (element instanceof PluginLaunchModel pluginLaunchModel) {
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

		@Override
		public Image getImage(Object element) {
			// Only the name column gets an image, see getColumnImage()
			return null;
		}

		@Override
		public String getText(Object element) {
			// If the label provider implement ILabelProvider the ViewerComparator calls getText() with whatever was passed to it, in our case we are already passing the label text based on sort order
			if (element instanceof String) {
				return (String) element;
			}
			return getColumnText(element, 0);
		}

		@Override
		public void dispose() {
			pdeLabelProvider.dispose();
			super.dispose();
		}
	}

	class ButtonSelectionListener extends SelectionAdapter {

		@Override
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
			ArrayList<IFeatureModel> featureModels = new ArrayList<>();
			for (FeatureLaunchModel model : fFeatureModels.values()) {
				if (!fTree.getChecked(model)) {
					featureModels.add(model.getModel(true));
				}
			}
			if (featureModels.isEmpty()) {
				MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.FeatureSelectionDialog_title, PDEUIMessages.FeatureBlock_AllFeatureSelected);
				return;
			}
			FeatureSelectionDialog dialog = new FeatureSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), featureModels.toArray(new IFeatureModel[featureModels.size()]), true);
			dialog.create();
			if (dialog.open() == Window.OK) {
				fTree.getControl().setRedraw(false);
				fTree.removeFilter(fSelectedOnlyFilter);
				Object[] selectedModels = dialog.getResult();
				for (Object model : selectedModels) {
					String id = ((IFeatureModel) model).getFeature().getId();
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
			IStructuredSelection selection = fTree.getStructuredSelection();
			int index = fAdditionalPlugins.indexOf(selection.getFirstElement());
			fAdditionalPlugins.removeAll(selection.toList());
			fTree.remove(selection.toArray());
			List<?> input = (List<?>) fTree.getInput();
			input.removeAll(selection.toList());
			if (fAdditionalPlugins.isEmpty()) {
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
				ArrayList<PluginLaunchModel> modelList = new ArrayList<>(models.length);
				for (Object model : models) {
					PluginLaunchModel pluginLaunchModel = new PluginLaunchModel((IPluginModelBase) model,
							DEFAULT_PLUGIN_DATA, fTab::updateLaunchConfigurationDialog);
					modelList.add(pluginLaunchModel);
				}

				@SuppressWarnings("unchecked")
				List<Object> input = (List<Object>) fTree.getInput();
				if (!input.contains(fAdditionalPluginsParentElement)) {
					input.add(fAdditionalPluginsParentElement);
				}
				fAdditionalPlugins.addAll(modelList);

				fTree.getControl().setRedraw(false);
				fTree.removeFilter(fSelectedOnlyFilter);
				fTree.refresh();
				for (PluginLaunchModel model : modelList) {
					fTree.setChecked(model, true);
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
			Set<IPluginModelBase> additionalPlugins = new HashSet<>();
			for (PluginLaunchModel model : fAdditionalPlugins) {
				additionalPlugins.add(model.getPluginModelBase());
			}
			List<IPluginModelBase> result = new ArrayList<>();
			for (int i = 0; i < plugins.length; i++) {
				if (!additionalPlugins.contains(plugins[i])) {
					result.add(plugins[i]);
				}
			}
			return result.toArray(new IPluginModelBase[result.size()]);
		}

		private void handleValidate() {
			try {
				// The feature block is used in both the OSGi config and Eclipse configs, use the tab id to determine which we are using
				boolean isOSGi = fTab.getId().equals(IPDELauncherConstants.TAB_BUNDLES_ID);
				Set<IPluginModelBase> models = BundleLauncherHelper.getMergedBundleMap(fLaunchConfig, isOSGi).keySet();
				// Unlike PluginBlock, we don't want to validate the application/product requirements because we will grab them automatically at launch time
				LaunchValidationOperation fOperation = new LaunchValidationOperation(fLaunchConfig, models);
				fOperation.run(new NullProgressMonitor());
				if (fDialog == null) {
					if (fOperation.hasErrors()) {
						fDialog = new PluginStatusDialog(getShell(), SWT.MODELESS | SWT.CLOSE | SWT.BORDER | SWT.TITLE | SWT.RESIZE);
						fDialog.setInput(fOperation);
						fDialog.open();
						fDialog = null;
					} else if (fOperation.isEmpty()) {
						if (fTab instanceof PluginsTab) {
							MessageDialog.openInformation(getShell(), PDEUIMessages.PluginStatusDialog_pluginValidation,PDEUIMessages.AbstractLauncherToolbar_noSelection_plugins);
						}else if (fTab instanceof BundlesTab) {
							MessageDialog.openInformation(getShell(), PDEUIMessages.PluginStatusDialog_pluginValidation,PDEUIMessages.AbstractLauncherToolbar_noSelection_bundles);
						}else{
							MessageDialog.openInformation(getShell(), PDEUIMessages.PluginStatusDialog_pluginValidation, NLS.bind(PDEUIMessages.AbstractLauncherToolbar_noSelection, fTab.getName().toLowerCase(Locale.ENGLISH)));
						}
					} else {
						MessageDialog.openInformation(getShell(), PDEUIMessages.PluginStatusDialog_pluginValidation, PDEUIMessages.AbstractLauncherToolbar_noProblems);
					}
				} else {
					if (fOperation.getInput().size() > 0)
						fDialog.refresh(fOperation.getInput());
					else {
						Map<String, IStatus> input = new HashMap<>(1);
						input.put(PDEUIMessages.AbstractLauncherToolbar_noProblems, Status.OK_STATUS);
						fDialog.refresh(input);
					}
				}
			} catch (CoreException e) {
				PDEPlugin.log(e);
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
				Set<String> requiredFeatureIDs = new HashSet<>();
				for (Object feature : features) {
					if (feature instanceof FeatureLaunchModel) {
						requiredFeatureIDs.add(((FeatureLaunchModel) feature).getId());
						getFeatureDependencies(((FeatureLaunchModel) feature).getModel(fFeatureWorkspaceButton.getSelection()), requiredFeatureIDs);
					}
				}

				fTree.getControl().setRedraw(false);
				fTree.removeFilter(fSelectedOnlyFilter);
				for (String featureId : requiredFeatureIDs) {
					Object featureModel = fFeatureModels.get(featureId);
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

		private void getFeatureDependencies(IFeatureModel model, Set<String> requiredFeatureIDs) {
			IFeature feature = model.getFeature();
			IFeatureImport[] featureImports = feature.getImports();
			for (IFeatureImport featureImport : featureImports) {
				if (featureImport.getType() == IFeatureImport.FEATURE) {
					addFeature(requiredFeatureIDs, featureImport.getId());
				}
			}

			IFeatureChild[] featureIncludes = feature.getIncludedFeatures();
			for (IFeatureChild featureInclude : featureIncludes) {
				addFeature(requiredFeatureIDs, featureInclude.getId());
			}
		}

		private void addFeature(Set<String> requiredFeatureIDs, String id) {
			if (!requiredFeatureIDs.contains(id)) {
				FeatureLaunchModel model = fFeatureModels.get(id);
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
				sortDirn = switch (tree.getSortDirection()) {
					case SWT.DOWN -> SWT.UP;
					case SWT.UP -> SWT.DOWN;
					default -> SWT.DOWN;
				};
			}
			tree.setSortDirection(sortDirn);
			int sortOrder = sortDirn == SWT.UP ? -1 : 1;
			int sortColumn = ((Integer) tc.getData(COLUMN_ID)).intValue();
			fTree.setComparator(new TreeSorter(sortColumn, sortOrder));
			saveSortOrder();
		}

		private void handleRestoreDefaults() {
			fWorkspacePluginButton.setSelection(true);
			fExternalPluginButton.setSelection(false);

			List<?> input = (List<?>) fTree.getInput();
			input.removeAll(fAdditionalPlugins);
			input.remove(fAdditionalPluginsParentElement);
			fAdditionalPlugins.clear();

			fRemovePluginButton.setEnabled(false);
			for (FeatureLaunchModel model : fFeatureModels.values()) {
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
		@Override
		public boolean canModify(Object element, String property) {
			if (element instanceof FeatureLaunchModel || element instanceof PluginLaunchModel) {
				return fTree.getChecked(element);
			}
			return false;
		}

		@Override
		public Object getValue(Object element, String property) {
			if (PROPERTY_RESOLUTION.equals(property)) {
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
				return Integer.valueOf(0);
			} else if (location.equalsIgnoreCase(IPDELauncherConstants.LOCATION_WORKSPACE)) {
				return Integer.valueOf(1);
			} else if (location.equalsIgnoreCase(IPDELauncherConstants.LOCATION_EXTERNAL)) {
				return Integer.valueOf(2);
			}
			return null;
		}

		@Override
		public void modify(Object item, String property, Object value) {
			if (property == PROPERTY_RESOLUTION && value != null) {
				Object data = ((TreeItem) item).getData();
				int comboIndex = ((Integer) value).intValue();
				String location = getLocation(comboIndex);
				if (data instanceof FeatureLaunchModel model) {
					if (!location.equalsIgnoreCase(model.getResolutionValue())) {
						model.setPluginResolution(location);
						fTree.refresh(model, true);
						fTab.updateLaunchConfigurationDialog();
					}
				} else if (data instanceof PluginLaunchModel pluginLaunchModel) {
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
			location = switch (comboIndex) {
				case 0 -> IPDELauncherConstants.LOCATION_DEFAULT;
				case 1 -> IPDELauncherConstants.LOCATION_WORKSPACE;
				case 2 -> IPDELauncherConstants.LOCATION_EXTERNAL;
				default -> location;
			};
			return location;
		}
	}

	class PluginContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getElements(Object input) {
			if (input instanceof Collection) {
				return ((Collection<?>) input).toArray();
			}
			if (input instanceof Object[]) {
				return (Object[]) input;
			}
			return new Object[0];
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement == fAdditionalPluginsParentElement) {
				return fAdditionalPlugins.toArray();
			}
			return new Object[0];
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof PluginLaunchModel)
				return fAdditionalPluginsParentElement;
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof NamedElement)
				return true;
			return false;
		}
	}

	class TreeSorter extends ViewerComparator {
		int sortColumn;
		int sortOrder;

		public TreeSorter(int sortColumn, int sortOrder) {
			this.sortColumn = sortColumn;
			this.sortOrder = sortOrder;
		}

		@Override
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

	static class PluginLaunchModel
			implements StartLevelEditingSupport.IHasStartLevel, AutoStartEditingSupport.IHasAutoStart {
		private final Runnable fPropertyChangedListener;
		private final IPluginModelBase fPluginModelBase;
		private String fPluginResolution;
		private String fStartLevel;
		private String fAutoStart;

		public PluginLaunchModel(IPluginModelBase pluginModelBase, AdditionalPluginData data,
				Runnable propertyChangedListener) {
			fPluginModelBase = pluginModelBase;
			fPropertyChangedListener = propertyChangedListener;
			fPluginResolution = data.fResolution;
			fStartLevel = data.fStartLevel;
			fAutoStart = data.fAutoStart;
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

		@Override
		public String getStartLevel() {
			return fStartLevel;
		}

		@Override
		public void setStartLevel(String startLevel) {
			requireNonNull(startLevel, "startLevel"); //$NON-NLS-1$
			if (!Objects.equals(fStartLevel, startLevel)) {
				fStartLevel = startLevel;
				if (fPropertyChangedListener != null) {
					fPropertyChangedListener.run();
				}
			}
		}

		@Override
		public String getAutoStart() {
			return fAutoStart;
		}

		@Override
		public void setAutoStart(String autoStart) {
			requireNonNull(autoStart, "autoStart"); //$NON-NLS-1$
			if (!Objects.equals(fAutoStart, autoStart)) {
				fAutoStart = autoStart;
				if (fPropertyChangedListener != null) {
					fPropertyChangedListener.run();
				}
			}
		}

		public String getPluginModelId() {
			return fPluginModelBase.getPluginBase().getId();
		}

		public String getPluginModelVersion() {
			return fPluginModelBase.getPluginBase().getVersion();
		}
	}

	static class FeatureLaunchModel {
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

		@Override
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
	private static final int COLUMN_START_LEVEL = 2;
	private static final int COLUMN_AUTO_START = 3;

	private static final String COLUMN_ID = "columnID"; //$NON-NLS-1$
	private static final String PROPERTY_RESOLUTION = "resolution"; //$NON-NLS-1$

	static final AdditionalPluginData DEFAULT_PLUGIN_DATA = new AdditionalPluginData(
			IPDELauncherConstants.LOCATION_DEFAULT, true, null, null);

	private Button fAddRequiredFeaturesButton;
	private Button fDefaultsButton;
	private Button fSelectAllButton;
	private Button fDeselectAllButton;

	private Button fAutoIncludeRequirementsButton;
	private boolean fAutoIncludeRequirementsButtonChanged;
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
	private final AbstractLauncherTab fTab;
	private CachedCheckboxTreeViewer fTree;

	private ViewerFilter fSelectedOnlyFilter;
	private boolean fIsDisposed = false;
	private PluginStatusDialog fDialog;

	/**
	 * Maps feature ID to the FeatureLaunchModel that represents the feature in the tree
	 */
	private Map<String, FeatureLaunchModel> fFeatureModels;
	private List<PluginLaunchModel> fAdditionalPlugins;
	private NamedElement fAdditionalPluginsParentElement;

	public FeatureBlock(AbstractLauncherTab pluginsTab) {
		Assert.isNotNull(pluginsTab);
		fTab = pluginsTab;
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	public void createControl(Composite parent, int span, int indent) {
		fListener = new ButtonSelectionListener();
		fSelectedOnlyFilter = new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				return fTree.getChecked(element);
			}
		};

		Composite composite = SWTFactory.createComposite(parent, 1, span, GridData.FILL_BOTH, 0, 0);

		Composite treeGroup = SWTFactory.createComposite(composite, 2, 1, GridData.FILL_BOTH, 0, 0);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalIndent = indent;
		treeGroup.setLayoutData(gd);
		createCheckBoxTree(treeGroup);
		createButtonContainer(treeGroup, 10);

		if (fTab instanceof PluginsTab) {
			fAutoIncludeRequirementsButton = SWTFactory.createCheckButton(treeGroup, PDEUIMessages.AdvancedLauncherTab_autoIncludeRequirements_features_withPlugins, null, false, 1);
		} else if (fTab instanceof BundlesTab) {
			fAutoIncludeRequirementsButton = SWTFactory.createCheckButton(treeGroup, PDEUIMessages.AdvancedLauncherTab_autoIncludeRequirements_features_withBundles, null, false, 1);
		}
		fAutoIncludeRequirementsButton.addSelectionListener(fListener);
		fAutoIncludeRequirementsButton.addSelectionListener(
				SelectionListener.widgetSelectedAdapter(e -> this.fAutoIncludeRequirementsButtonChanged = true));

		fFeatureWorkspaceButton = SWTFactory.createCheckButton(treeGroup, PDEUIMessages.FeatureBlock_UseWorkspaceFeatures, null, true, 2);
		fFeatureWorkspaceButton.addSelectionListener(fListener);

		Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite validatecomp = SWTFactory.createComposite(composite, 2, 1, GridData.FILL_HORIZONTAL, 0, 0);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent = indent;
		validatecomp.setLayoutData(gd);

		if (fTab instanceof PluginsTab) {
			fAutoValidate = SWTFactory.createCheckButton(validatecomp, PDEUIMessages.PluginsTabToolBar_auto_validate_plugins, null, false, 1);
		} else if (fTab instanceof BundlesTab) {
			fAutoValidate = SWTFactory.createCheckButton(validatecomp, PDEUIMessages.PluginsTabToolBar_auto_validate_bundles, null, false, 1);
		} else {
			fAutoValidate = SWTFactory.createCheckButton(validatecomp,
					NLS.bind(PDEUIMessages.PluginsTabToolBar_auto_validate,
							fTab.getName().replace("&", "").toLowerCase(Locale.ENGLISH)), //$NON-NLS-1$ //$NON-NLS-2$
					null, false, 1);
		}

		fAutoValidate.addSelectionListener(fListener);
		Composite rightAlignComp = SWTFactory.createComposite(validatecomp, 1, 1, SWT.NONE, 0, 0);
		rightAlignComp.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, true, true));

		if (fTab instanceof PluginsTab) {
			fValidateButton = SWTFactory.createPushButton(rightAlignComp, PDEUIMessages.PluginsTabToolBar_validate_plugins, null);
		} else if (fTab instanceof BundlesTab) {
			fValidateButton = SWTFactory.createPushButton(rightAlignComp,PDEUIMessages.PluginsTabToolBar_validate_bundles, null);
		} else {
			fValidateButton = SWTFactory.createPushButton(rightAlignComp,
					NLS.bind(PDEUIMessages.PluginsTabToolBar_validate, fTab.getName().replace("&", "")), null); //$NON-NLS-1$//$NON-NLS-2$
		}

		fValidateButton.addSelectionListener(fListener);
	}

	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		fIsDisposed = true;
	}

	private void createCheckBoxTree(Composite parent) {
		PatternFilter filter = new PatternFilter() {
			@Override
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
			@Override
			protected WorkbenchJob doCreateRefreshJob() {
				// If we are only showing selected items, we need to redo the filter after text filtering is applied.  The only selected filter uses the tree's check state, which hasn't been restored correctly at filter time.
				WorkbenchJob job = super.doCreateRefreshJob();
				job.addJobChangeListener(new JobChangeAdapter() {
					@Override
					public void done(IJobChangeEvent event) {
						if (event.getResult().isOK()) {
							getDisplay().asyncExec(() -> {
								fTree.getControl().setRedraw(false);
								fTree.removeFilter(fSelectedOnlyFilter);
								fTree.restoreLeafCheckState();
								try {
									if (fLaunchConfig.getAttribute(IPDELauncherConstants.SHOW_SELECTED_ONLY, false)) {
										fTree.addFilter(fSelectedOnlyFilter);
									}
								} catch (CoreException e) {

								}
								fTree.getControl().setRedraw(true);
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
		column1.setData(COLUMN_ID, Integer.valueOf(COLUMN_FEATURE_NAME));

		TreeColumn column2 = new TreeColumn(fTree.getTree(), SWT.CENTER);
		column2.setText(PDEUIMessages.FeatureBlock_pluginResolution);
		column2.setWidth(100);
		column2.addSelectionListener(fListener);
		column2.setData(COLUMN_ID, Integer.valueOf(COLUMN_PLUGIN_RESOLUTION));

		fTree.setLabelProvider(new FeatureTreeLabelProvider());

		TreeViewerColumn startLevelColumn = new TreeViewerColumn(fTree, SWT.CENTER, COLUMN_START_LEVEL);
		startLevelColumn.getColumn().setText(PDEUIMessages.EquinoxPluginBlock_levelColumn);
		startLevelColumn.getColumn().setWidth(100);
		startLevelColumn.setEditingSupport(new StartLevelEditingSupport(fTree));
		startLevelColumn.setLabelProvider(additionPluginLabelProvider(PluginLaunchModel::getStartLevel));

		TreeViewerColumn autoStartColumn = new TreeViewerColumn(fTree, SWT.CENTER, COLUMN_AUTO_START);
		autoStartColumn.getColumn().setText(PDEUIMessages.EquinoxPluginBlock_autoColumn);
		autoStartColumn.getColumn().setWidth(100);
		autoStartColumn.setEditingSupport(new AutoStartEditingSupport(fTree));
		autoStartColumn.setLabelProvider(additionPluginLabelProvider(PluginLaunchModel::getAutoStart));

		fTree.getTree().setHeaderVisible(true);
		fTree.setContentProvider(new PluginContentProvider());
		fTree.addCheckStateListener(event -> {
			updateCounter();
			fTab.updateLaunchConfigurationDialog();
		});
		String[] items = new String[] {PDEUIMessages.FeatureBlock_default, PDEUIMessages.FeatureBlock_WorkspaceResolutionLabel, PDEUIMessages.FeatureBlock_ExternalResolutionLabel};
		ComboBoxCellEditor cellEditor = new ComboBoxCellEditor(fTree.getTree(), items);
		cellEditor.getControl().pack();
		fTree.setCellEditors(new CellEditor[] {null, cellEditor});
		fTree.setColumnProperties(new String[] {null, PROPERTY_RESOLUTION});
		fTree.setCellModifier(new LocationCellModifier());
		fTree.addSelectionChangedListener(event -> {
			IStructuredSelection selection = (IStructuredSelection) fTree.getSelection();
			boolean allPlugins = true;
			for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
				Object element = iterator.next();
				if (!(element instanceof PluginLaunchModel)) {
					allPlugins = false;
				}
			}
			fRemovePluginButton.setEnabled(allPlugins);
		});
		fTree.addCheckStateListener(e -> fTree.update(e.getElement(), null));

		fTree.getTree().addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.keyCode == SWT.DEL) {
					// Any changes here need to be reflected in the remove button handling
					IStructuredSelection selection = (IStructuredSelection) fTree.getSelection();
					int index = fAdditionalPlugins.indexOf(selection.getFirstElement());
					List<?> input = (List<?>) fTree.getInput();
					for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
						Object element = iterator.next();
						if (element instanceof PluginLaunchModel) {
							fAdditionalPlugins.remove(element);
							fTree.remove(element);
							input.remove(element);
						}
					}
					if (fAdditionalPlugins.isEmpty()) {
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

	private CellLabelProvider additionPluginLabelProvider(Function<PluginLaunchModel, String> labelProvider) {
		return new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				Object element = cell.getElement();
				if ((element instanceof PluginLaunchModel) && fTree.isCheckedLeafElement(element)) {
					cell.setText(labelProvider.apply((PluginLaunchModel) element));
				} else {
					cell.setText(null);
				}
			}
		};
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
		if (fTab instanceof PluginsTab) {
			fAddPluginButton = SWTFactory.createPushButton(buttonComp, PDEUIMessages.FeatureBlock_AddPluginsLabel_plugins, null);
		}else if (fTab instanceof BundlesTab) {
			fAddPluginButton = SWTFactory.createPushButton(buttonComp,PDEUIMessages.FeatureBlock_AddPluginsLabel_bundles, null);
		}else{
			fAddPluginButton = SWTFactory.createPushButton(buttonComp,
					NLS.bind(PDEUIMessages.FeatureBlock_AddPluginsLabel, fTab.getName().replace("&", "")), null); //$NON-NLS-1$//$NON-NLS-2$
		}
		fAddPluginButton.addSelectionListener(fListener);
		if (fTab instanceof PluginsTab) {
			fRemovePluginButton = SWTFactory.createPushButton(buttonComp, PDEUIMessages.FeatureBlock_RemovePluginsLabel_plugins, null);
		}else if (fTab instanceof BundlesTab) {
			fRemovePluginButton = SWTFactory.createPushButton(buttonComp, PDEUIMessages.FeatureBlock_RemovePluginsLabel_bundles, null);
		}else{
			fRemovePluginButton = SWTFactory.createPushButton(buttonComp,
					NLS.bind(PDEUIMessages.FeatureBlock_RemovePluginsLabel, fTab.getName().replace("&", "")), null); //$NON-NLS-1$//$NON-NLS-2$
		}

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
		if (fTab instanceof PluginsTab) {
			fAdditionalPluginsParentElement = new NamedElement(PDEUIMessages.FeatureBlock_AdditionalPluginsEntry_plugins, siteImage);
		}else if (fTab instanceof BundlesTab) {
			fAdditionalPluginsParentElement = new NamedElement(PDEUIMessages.FeatureBlock_AdditionalPluginsEntry_bundles, siteImage);
		}else{
			fAdditionalPluginsParentElement = new NamedElement(
					NLS.bind(PDEUIMessages.FeatureBlock_AdditionalPluginsEntry, fTab.getName().replace("&", "")), //$NON-NLS-1$ //$NON-NLS-2$
					siteImage);
		}
	}

	public void initialize() throws CoreException {
		initializeFrom(fLaunchConfig);
	}

	public void initializeFrom(ILaunchConfiguration config) throws CoreException {
		fLaunchConfig = config;
		fTree.removeFilter(fSelectedOnlyFilter);
		setInput(config, fTree);

		// Setup other buttons
		boolean autoIncludeRequired = config.getAttribute(IPDELauncherConstants.AUTOMATIC_INCLUDE_REQUIREMENTS, true);
		fAutoIncludeRequirementsButton.setSelection(autoIncludeRequired);
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
		fAutoValidate.setSelection(config.getAttribute(IPDELauncherConstants.AUTOMATIC_VALIDATE, true));

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
		if (fAutoIncludeRequirementsButtonChanged) {
			boolean includeRequirements = fAutoIncludeRequirementsButton.getSelection();
			config.setAttribute(IPDELauncherConstants.AUTOMATIC_INCLUDE_REQUIREMENTS, includeRequirements);
			fAutoIncludeRequirementsButtonChanged = false;
		}
		boolean showSelectedOnly = fFilterButton.getSelection();
		if (showSelectedOnly) {
			config.setAttribute(IPDELauncherConstants.SHOW_SELECTED_ONLY, true);
		} else {
			config.removeAttribute(IPDELauncherConstants.SHOW_SELECTED_ONLY);
		}
		config.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, fFeatureWorkspaceButton.getSelection() ? IPDELauncherConstants.LOCATION_WORKSPACE : IPDELauncherConstants.LOCATION_EXTERNAL);
		config.setAttribute(IPDELauncherConstants.FEATURE_PLUGIN_RESOLUTION, fWorkspacePluginButton.getSelection() ? IPDELauncherConstants.LOCATION_WORKSPACE : IPDELauncherConstants.LOCATION_EXTERNAL);
		config.setAttribute(IPDELauncherConstants.AUTOMATIC_VALIDATE, fAutoValidate.getSelection());
		savePluginState(config);
		saveSortOrder();
		updateCounter();
	}

	private void savePluginState(ILaunchConfigurationWorkingCopy config) {
		Set<String> featuresEntry = new HashSet<>(); // By using a set, debug will sort the attribute for us
		Set<String> pluginsEntry = new HashSet<>();
		ArrayList<PluginLaunchModel> checkPluginLaunchModels = new ArrayList<>();

		Object[] models = fTree.getCheckedLeafElements();

		for (Object model : models) {
			if (model instanceof FeatureLaunchModel feature) {
				String entry = BundleLauncherHelper.formatFeatureEntry(feature.getId(), feature.getResolutionValue());
				featuresEntry.add(entry);
			} else if (model instanceof PluginLaunchModel pluginLaunchModel) {
				pluginsEntry.add(buildAdditionalPluginEntry(pluginLaunchModel, true));
				checkPluginLaunchModels.add(pluginLaunchModel);
			}
		}

		for (PluginLaunchModel uncheckedPluginLaunchModel : fAdditionalPlugins) {
			if (!checkPluginLaunchModels.contains(uncheckedPluginLaunchModel)) {
				pluginsEntry.add(buildAdditionalPluginEntry(uncheckedPluginLaunchModel, false));
			}
		}
		config.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, featuresEntry);
		config.setAttribute(IPDELauncherConstants.ADDITIONAL_PLUGINS, pluginsEntry);

	}

	private static String buildAdditionalPluginEntry(PluginLaunchModel pl, boolean isChecked) {
		return BundleLauncherHelper.formatAdditionalPluginEntry(pl.getPluginModelBase(), pl.getPluginResolution(),
				isChecked, pl.getStartLevel(), pl.getAutoStart());
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
		config.removeAttribute(IPDELauncherConstants.SHOW_SELECTED_ONLY);
		config.setAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, IPDELauncherConstants.LOCATION_WORKSPACE);
		config.setAttribute(IPDELauncherConstants.FEATURE_PLUGIN_RESOLUTION, IPDELauncherConstants.LOCATION_WORKSPACE);
		config.setAttribute(IPDELauncherConstants.AUTOMATIC_VALIDATE, true);
	}

	private void updateCounter() {
		if (fCounter != null) {
			int checked = fTree.getCheckedLeafCount();
			int total = fFeatureModels.size() + fAdditionalPlugins.size();
			fCounter.setText(NLS.bind(PDEUIMessages.AbstractPluginBlock_counter, Integer.valueOf(checked), Integer.valueOf(total)));
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
		Map<String, FeatureLaunchModel> featureModels = new LinkedHashMap<>();
		FeatureModelManager fmm = PDECore.getDefault().getFeatureModelManager();
		IFeatureModel[] workspaceModels = fmm.getWorkspaceModels();
		for (IFeatureModel workspaceModel : workspaceModels) {
			String id = workspaceModel.getFeature().getId();
			if (id != null) {
				//don't consider broken features: https://bugs.eclipse.org/bugs/show_bug.cgi?id=377563
				featureModels.put(id, new FeatureLaunchModel(workspaceModel, null));
			}
		}

		// If there is both a workspace and a target model with the same id, combine them into the same launch model
		IFeatureModel[] externalModels = fmm.getExternalModels();
		for (IFeatureModel externalModel : externalModels) {
			String id = externalModel.getFeature().getId();
			if (id != null) {
				//don't consider broken features: https://bugs.eclipse.org/bugs/show_bug.cgi?id=377563
				if (featureModels.containsKey(id)) {
					FeatureLaunchModel launchModel = featureModels.get(id);
					launchModel.setTargetModel(externalModel);
				} else {
					featureModels.put(id, new FeatureLaunchModel(null, externalModel));
				}
			}
		}

		fFeatureModels = featureModels;
		try {
			fAdditionalPlugins = new ArrayList<>();
			List<PluginLaunchModel> checkedAdditionalPlugins = new ArrayList<>();
			Map<IPluginModelBase, AdditionalPluginData> additionalMap = BundleLauncherHelper
					.getAdditionalPlugins(config, false);
			for (Entry<IPluginModelBase, AdditionalPluginData> additionalEntry : additionalMap.entrySet()) {
				AdditionalPluginData data = additionalEntry.getValue();
				PluginLaunchModel launchModel = new PluginLaunchModel(additionalEntry.getKey(), data,
						fTab::updateLaunchConfigurationDialog);
				fAdditionalPlugins.add(launchModel);

				if (data.fEnabled) {
					checkedAdditionalPlugins.add(launchModel);
				}
			}

			List<Object> models = new ArrayList<>(fFeatureModels.values());
			if (!fAdditionalPlugins.isEmpty()) {
				models.add(fAdditionalPluginsParentElement);
			}
			tree.setInput(models);

			// Loop through the saved config to determine location settings and selection
			Set<String> selected = config.getAttribute(IPDELauncherConstants.SELECTED_FEATURES, (Set<String>) null);
			if (selected == null) {
				tree.setCheckedElements(fFeatureModels.values().toArray());
			} else {
				ArrayList<FeatureLaunchModel> selectedFeatureList = new ArrayList<>();
				for (String currentSelected : selected) {
					String[] attributes = currentSelected.split(":"); //$NON-NLS-1$
					if (attributes.length > 0) {
						String id = attributes[0];
						FeatureLaunchModel model = fFeatureModels.get(id);
						if (model != null) {
							selectedFeatureList.add(model);
							if (attributes.length > 1) {
								model.setPluginResolution(attributes[1]);
							}
						}
					}
				}
				List<Object> checkedElements = new ArrayList<>();
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
