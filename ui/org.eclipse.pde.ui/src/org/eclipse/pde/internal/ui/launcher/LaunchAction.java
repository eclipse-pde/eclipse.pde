/*******************************************************************************
 * Copyright (c) 2005, 2022 IBM Corporation and others.
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
 *     EclipseSource Corporation - ongoing enhancements
 *     Hannes Wellmann - Bug 570760 - Option to automatically add requirements to product-launch
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import static org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper.formatAdditionalPluginEntry;
import static org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper.formatFeatureEntry;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.launching.IPDEConstants;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper.AdditionalPluginData;
import org.eclipse.pde.internal.launching.launcher.LaunchArgumentsHelper;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.launching.PDESourcePathProvider;
import org.eclipse.pde.ui.launcher.EclipseLaunchShortcut;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

public class LaunchAction extends Action {

	private static final String DEFAULT = "default"; //$NON-NLS-1$

	private IProduct fProduct;
	private String fMode;
	private IPath fPath;
	private Map<String, IPluginConfiguration> fPluginConfigurations;

	public LaunchAction(IProduct product, IPath path, String mode) {
		fProduct = product;
		fMode = mode;
		fPath = path;
		fPluginConfigurations = Arrays.stream(fProduct.getPluginConfigurations())
				.collect(Collectors.toUnmodifiableMap(IPluginConfiguration::getId, c -> c));
	}

	@Override
	public void run() {
		try {
			ILaunchConfiguration config = findLaunchConfiguration();
			if (config != null) {
				DebugUITools.launch(config, fMode);
			}
		} catch (CoreException e) {
			PDEPlugin.log(Status.error(PDEUIMessages.ProductEditor_launchFailed, e));
		}
	}

	public ILaunchConfiguration findLaunchConfiguration() throws CoreException {
		List<ILaunchConfiguration> configs = getLaunchConfigurations();

		if (configs.isEmpty()) {
			return createConfiguration();
		}
		ILaunchConfiguration config = configs.size() == 1 //
				? configs.get(0)
				: chooseConfiguration(configs); // Prompt the user to choose one

		if (config != null) {
			config = refreshConfiguration(config.getWorkingCopy());
		}
		return config;
	}

	private ILaunchConfiguration refreshConfiguration(ILaunchConfigurationWorkingCopy wc) throws CoreException {
		wc.setAttribute(IPDELauncherConstants.PRODUCT, fProduct.getProductId());
		wc.setAttribute(IPDELauncherConstants.APPLICATION, fProduct.getApplication());

		if (TargetPlatformHelper.usesNewApplicationModel()) {
			wc.setAttribute(IPDEConstants.LAUNCHER_PDE_VERSION, "3.3"); //$NON-NLS-1$
		} else if (TargetPlatformHelper.getTargetVersion() >= 3.2) {
			wc.setAttribute(IPDEConstants.LAUNCHER_PDE_VERSION, "3.2a"); //$NON-NLS-1$
		}
		String os = Platform.getOS();
		String arch = Platform.getOSArch();
		wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, getVMArguments(os, arch));
		wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, getProgramArguments(os, arch));
		wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH, getJREContainer(os));

		Set<String> wsplugins = new HashSet<>();
		Set<String> explugins = new HashSet<>();
		Set<IPluginModelBase> models = getModels(fProduct);
		for (IPluginModelBase model : models) {
			appendBundle(model.getUnderlyingResource() == null ? explugins : wsplugins, model);
		}
		wc.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES, wsplugins);
		wc.setAttribute(IPDELauncherConstants.SELECTED_TARGET_BUNDLES, explugins);

		if (fProduct.useFeatures()) {
			wc.setAttribute(IPDELauncherConstants.USE_CUSTOM_FEATURES, true);
			refreshFeatureLaunchPlugins(wc, models);
		} else {
			wc.removeAttribute(IPDELauncherConstants.USE_CUSTOM_FEATURES);
			wc.removeAttribute(IPDELauncherConstants.SELECTED_FEATURES);
			wc.removeAttribute(IPDELauncherConstants.ADDITIONAL_PLUGINS);
		}
		wc.setAttribute(IPDELauncherConstants.AUTOMATIC_INCLUDE_REQUIREMENTS,
				fProduct.includeRequirementsAutomatically());

		String configIni = getTemplateConfigIni(os);
		wc.setAttribute(IPDELauncherConstants.CONFIG_GENERATE_DEFAULT, configIni == null);
		if (configIni != null) {
			wc.setAttribute(IPDELauncherConstants.CONFIG_TEMPLATE_LOCATION, configIni);
		}
		return wc.doSave();
	}

	private void refreshFeatureLaunchPlugins(ILaunchConfigurationWorkingCopy wc, Set<IPluginModelBase> allModels) {
		FeatureModelManager featureManager = PDECore.getDefault().getFeatureModelManager();
		Set<String> selectedFeatures = Arrays.stream(fProduct.getFeatures()) //
				.map(f -> featureManager.findFeatureModel(f.getId(), f.getVersion())).filter(Objects::nonNull)
				.map(m -> formatFeatureEntry(m.getFeature().getId(), IPDELauncherConstants.LOCATION_DEFAULT))
				.collect(Collectors.toCollection(LinkedHashSet::new));

		Set<String> additionalPlugins = allModels.stream()
				.map(model -> getPluginConfiguration(model)
						.map(c -> formatAdditionalPluginEntry(model, c.fResolution, true, c.fStartLevel, c.fAutoStart)))
				.flatMap(Optional::stream).collect(Collectors.toCollection(LinkedHashSet::new));

		wc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, selectedFeatures);
		wc.setAttribute(IPDELauncherConstants.ADDITIONAL_PLUGINS, additionalPlugins);
	}

	private void appendBundle(Set<String> plugins, IPluginModelBase model) {
		AdditionalPluginData config = getPluginConfiguration(model).orElse(FeatureBlock.DEFAULT_PLUGIN_DATA);
		String entry = BundleLauncherHelper.formatBundleEntry(model, config.fStartLevel, config.fAutoStart);
		plugins.add(entry);
	}

	private Optional<AdditionalPluginData> getPluginConfiguration(IPluginModelBase model) {
		IPluginConfiguration config = fPluginConfigurations.get(model.getPluginBase().getId());
		if (config == null) {
			return Optional.empty();
		}
		String startLevel = config.getStartLevel() > 0 ? Integer.toString(config.getStartLevel()) : DEFAULT;
		String autoStart = Boolean.toString(config.isAutoStart());
		return Optional
				.of(new AdditionalPluginData(IPDELauncherConstants.LOCATION_DEFAULT, true, startLevel, autoStart));
	}

	private String getProgramArguments(String os, String arch) {
		IArgumentsInfo info = fProduct.getLauncherArguments();
		String userArgs = info != null ? CoreUtility.normalize(info.getCompleteProgramArguments(os, arch)) : ""; //$NON-NLS-1$
		return concatArgs(LaunchArgumentsHelper.getInitialProgramArguments(), userArgs);
	}

	private String getVMArguments(String os, String arch) {
		IArgumentsInfo info = fProduct.getLauncherArguments();
		String userArgs = info != null ? CoreUtility.normalize(info.getCompleteVMArguments(os, arch)) : ""; //$NON-NLS-1$
		return concatArgs(LaunchArgumentsHelper.getInitialVMArguments(), userArgs);
	}

	private static final Set<String> PROGRAM_ARGUMENTS = Set.of(//
			'-' + IEnvironment.P_OS, '-' + IEnvironment.P_WS, '-' + IEnvironment.P_ARCH, '-' + IEnvironment.P_NL);

	private String concatArgs(String initialArgs, String userArgs) {
		List<String> arguments = new ArrayList<>(Arrays.asList(DebugPlugin.splitArguments(initialArgs)));
		if (userArgs != null && userArgs.length() > 0) {
			List<String> userArgsList = Arrays.asList(DebugPlugin.splitArguments(userArgs));
			boolean previousHasSubArgument = false;
			for (String userArg : userArgsList) {
				boolean hasSubArgument = PROGRAM_ARGUMENTS.contains(userArg);
				if (!arguments.contains(userArg) || hasSubArgument || previousHasSubArgument) {
					arguments.add(userArg);
				}
				previousHasSubArgument = hasSubArgument;
			}
		}
		try {
			return removeDuplicateArguments(arguments);
		} catch (Exception e) {
			PDEPlugin.log(e);
			return String.join(" ", arguments); //$NON-NLS-1$
		}
	}

	private String removeDuplicateArguments(List<String> userArgsList) {
		String defaultStart = "${target."; //$NON-NLS-1$ // see
											// LaunchArgumentHelper
		for (String progArgument : PROGRAM_ARGUMENTS) {
			int index1 = userArgsList.indexOf(progArgument);
			int index2 = userArgsList.lastIndexOf(progArgument);
			if (index1 != index2) {
				String s1 = userArgsList.get(index1 + 1);
				String s2 = userArgsList.get(index2 + 1);
				// in case of duplicate remove initial program arguments
				if (s1.startsWith(defaultStart) && !s2.startsWith(defaultStart)) {
					userArgsList.remove(index1);
					userArgsList.remove(index1);
				} else if (s2.startsWith(defaultStart) && !s1.startsWith(defaultStart)) {
					userArgsList.remove(index2);
					userArgsList.remove(index2);
				}
			}
		}
		return String.join(" ", userArgsList); //$NON-NLS-1$
	}

	private String getJREContainer(String os) {
		IJREInfo info = fProduct.getJREInfo();
		if (info != null) {
			IPath jrePath = info.getJREContainerPath(os);
			if (jrePath != null) {
				return jrePath.toPortableString();
			}
		}
		return null;
	}

	public static Set<IPluginModelBase> getModels(IProduct product) {
		return product.useFeatures() ? getModelsFromListedFeatures(product) : getModelsFromListedPlugins(product);
	}

	private static Set<IPluginModelBase> getModelsFromListedPlugins(IProduct product) {
		return Arrays.stream(product.getPlugins()) //
				.map(IProductPlugin::getId).filter(Objects::nonNull)//
				.map(PluginRegistry::findModel).filter(Objects::nonNull)
				.filter(TargetPlatformHelper::matchesCurrentEnvironment)//
				.collect(Collectors.toSet());
	}

	private static Set<IPluginModelBase> getModelsFromListedFeatures(IProduct product) {
		Set<IPluginModelBase> launchPlugins = new HashSet<>();
		for (IFeatureModel feature : getUniqueFeatures(product)) {
			addFeaturePlugins(feature.getFeature(), launchPlugins);
		}
		return launchPlugins;
	}

	private static Collection<IFeatureModel> getUniqueFeatures(IProduct product) {
		Queue<IFeatureModel> pending = new ArrayDeque<>();
		Set<IFeatureModel> features = new LinkedHashSet<>();
		for (IProductFeature feature : product.getFeatures()) {
			addFeature(feature.getId(), feature.getVersion(), pending, features);
		}
		while (!pending.isEmpty()) { // breadth-first search for all features
			IFeatureModel feature = pending.remove();
			for (IFeatureChild child : feature.getFeature().getIncludedFeatures()) {
				addFeature(child.getId(), child.getVersion(), pending, features);
			}
		}
		return features;
	}

	private static void addFeature(String id, String version, Queue<IFeatureModel> pending,
			Set<IFeatureModel> features) {
		FeatureModelManager featureManager = PDECore.getDefault().getFeatureModelManager();
		IFeatureModel feature = featureManager.findFeatureModel(id, version);
		if (feature != null && features.add(feature)) {
			pending.add(feature);
		}
	}

	private static void addFeaturePlugins(IFeature feature, Set<IPluginModelBase> launchPlugins) {
		for (IFeaturePlugin plugin : feature.getPlugins()) {
			String id = plugin.getId();
			String version = plugin.getVersion();
			if (id == null || version == null) {
				continue;
			}
			IPluginModelBase model = PluginRegistry.findModel(id, version, IMatchRules.EQUIVALENT, null);
			if (model == null) {
				model = PluginRegistry.findModel(id);
			}
			if (model != null && !launchPlugins.contains(model)
					&& TargetPlatformHelper.matchesCurrentEnvironment(model)) {
				launchPlugins.add(model);
			}
		}
	}

	private String getTemplateConfigIni(String os) {
		IConfigurationFileInfo info = fProduct.getConfigurationFileInfo();
		if (info != null) {
			String path = info.getPath(os);
			if (path == null) {
				// if we can't find an os path, let's try the normal one
				path = info.getPath(null);
			}
			if (path != null) {
				String expandedPath = getExpandedPath(path);
				if (expandedPath != null) {
					File file = new File(expandedPath);
					if (file.isFile()) {
						return file.getAbsolutePath();
					}
				}
			}
		}
		return null;
	}

	private String getExpandedPath(String path) {
		if (path == null || path.length() == 0) {
			return null;
		}
		IResource resource = PDEPlugin.getWorkspace().getRoot().findMember(new Path(path));
		if (resource != null) {
			IPath fullPath = resource.getLocation();
			return fullPath == null ? null : fullPath.toOSString();
		}
		return null;
	}

	private ILaunchConfiguration chooseConfiguration(List<ILaunchConfiguration> configs) {
		IDebugModelPresentation labelProvider = DebugUITools.newDebugModelPresentation();
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(PDEPlugin.getActiveWorkbenchShell(),
				labelProvider);
		dialog.setElements(configs.toArray());
		dialog.setTitle(PDEUIMessages.RuntimeWorkbenchShortcut_title);
		dialog.setMessage(fMode.equals(ILaunchManager.DEBUG_MODE) //
				? PDEUIMessages.RuntimeWorkbenchShortcut_select_debug
				: PDEUIMessages.RuntimeWorkbenchShortcut_select_run);
		dialog.setMultipleSelection(false);
		int result = dialog.open();
		labelProvider.dispose();
		return result == Window.OK ? (ILaunchConfiguration) dialog.getFirstResult() : null;
	}

	private ILaunchConfiguration createConfiguration() throws CoreException {
		ILaunchConfigurationType configType = getWorkbenchLaunchConfigType();
		String computedName = getComputedName(fPath.lastSegment());
		ILaunchConfigurationWorkingCopy wc = configType.newInstance(null, computedName);
		wc.setAttribute(IPDELauncherConstants.LOCATION,
				LaunchArgumentsHelper.getDefaultWorkspaceLocation(computedName));
		wc.setAttribute(IPDELauncherConstants.USE_DEFAULT, false);
		wc.setAttribute(IPDELauncherConstants.DOCLEAR, false);
		wc.setAttribute(IPDEConstants.DOCLEARLOG, false);
		wc.setAttribute(IPDEConstants.APPEND_ARGS_EXPLICITLY, true);
		wc.setAttribute(IPDELauncherConstants.ASKCLEAR, true);
		wc.setAttribute(IPDELauncherConstants.USE_PRODUCT, true);
		wc.setAttribute(IPDELauncherConstants.AUTOMATIC_VALIDATE, true);
		wc.setAttribute(IPDELauncherConstants.AUTOMATIC_ADD, false);
		wc.setAttribute(IPDELauncherConstants.PRODUCT_FILE, fPath.toOSString());
		wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, PDESourcePathProvider.ID);
		wc.setAttribute(IPDELauncherConstants.INCLUDE_OPTIONAL, false);
		return refreshConfiguration(wc);
	}

	private String getComputedName(String prefix) {
		ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
		return lm.generateLaunchConfigurationName(prefix);
	}

	private List<ILaunchConfiguration> getLaunchConfigurations() throws CoreException {
		List<ILaunchConfiguration> result = new ArrayList<>();
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = manager.getLaunchConfigurationType(EclipseLaunchShortcut.CONFIGURATION_TYPE);
		for (ILaunchConfiguration config : manager.getLaunchConfigurations(type)) {
			if (!DebugUITools.isPrivate(config)) {
				String path = config.getAttribute(IPDELauncherConstants.PRODUCT_FILE, ""); //$NON-NLS-1$
				if (fPath.equals(new Path(path))) {
					result.add(config);
				}
			}
		}
		return result;
	}

	protected ILaunchConfigurationType getWorkbenchLaunchConfigType() {
		ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
		return lm.getLaunchConfigurationType(EclipseLaunchShortcut.CONFIGURATION_TYPE);
	}

}
