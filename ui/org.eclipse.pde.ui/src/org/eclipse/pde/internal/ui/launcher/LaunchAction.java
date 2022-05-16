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

	private IProduct fProduct;
	private String fMode;
	private String fPath;
	private Map<String, IPluginConfiguration> fPluginConfigurations;

	public LaunchAction(IProduct product, String path, String mode) {
		fProduct = product;
		fMode = mode;
		fPath = path;
		// initialize configurations... we should do this lazily
		// TODO
		fPluginConfigurations = new HashMap<>();
		IPluginConfiguration[] configurations = fProduct.getPluginConfigurations();
		for (IPluginConfiguration config : configurations) {
			fPluginConfigurations.put(config.getId(), config);
		}
	}

	@Override
	public void run() {
		try {
			ILaunchConfiguration config = findLaunchConfiguration();
			if (config != null)
				DebugUITools.launch(config, fMode);
		} catch (CoreException e) {
		}
	}

	private ILaunchConfiguration findLaunchConfiguration() throws CoreException {
		ILaunchConfiguration[] configs = getLaunchConfigurations();

		if (configs.length == 0)
			return createConfiguration();

		ILaunchConfiguration config = null;
		if (configs.length == 1) {
			config = configs[0];
		} else {
			// Prompt the user to choose a config.
			config = chooseConfiguration(configs);
		}

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
		IPluginModelBase[] models = getModels();
		for (IPluginModelBase model : models) {
			if (model.getUnderlyingResource() == null) {
				appendBundle(explugins, model);
			} else {
				appendBundle(wsplugins, model);
			}
		}
		wc.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES, wsplugins);
		wc.setAttribute(IPDELauncherConstants.SELECTED_TARGET_BUNDLES, explugins);
		if (fProduct.useFeatures()) {
			refreshFeatureLaunchAttributes(wc, models);
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

	private void refreshFeatureLaunchAttributes(ILaunchConfigurationWorkingCopy wc, IPluginModelBase[] models) {
		FeatureModelManager fmm = PDECore.getDefault().getFeatureModelManager();
		Set<String> selectedFeatures = Arrays.stream(fProduct.getFeatures()) //
				.map(f -> fmm.findFeatureModel(f.getId(), f.getVersion())).filter(Objects::nonNull)
				.map(m -> formatFeatureEntry(m.getFeature().getId(), IPDELauncherConstants.LOCATION_DEFAULT))
				.collect(Collectors.toCollection(LinkedHashSet::new));

		Set<String> additionalPlugins = Arrays.stream(models)
				.map(model -> getPluginConfiguration(model)
						.map(c -> formatAdditionalPluginEntry(model, c.fResolution, true, c.fStartLevel, c.fAutoStart))
						.orElse(null))
				.filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));

		wc.setAttribute(IPDELauncherConstants.SELECTED_FEATURES, selectedFeatures);
		wc.setAttribute(IPDELauncherConstants.ADDITIONAL_PLUGINS, additionalPlugins);
		wc.setAttribute(IPDELauncherConstants.USE_CUSTOM_FEATURES, true);
	}

	private void appendBundle(Set<String> plugins, IPluginModelBase model) {
		AdditionalPluginData config = getPluginConfiguration(model).orElse(FeatureBlock.DEFAULT_PLUGIN_DATA);
		String entry = BundleLauncherHelper.formatBundleEntry(model, config.fStartLevel, config.fAutoStart);
		plugins.add(entry);
	}

	private Optional<AdditionalPluginData> getPluginConfiguration(IPluginModelBase model) {
		IPluginConfiguration configuration = fPluginConfigurations.get(model.getPluginBase().getId());
		if (configuration == null) {
			return Optional.empty();
		}

		String startLevel = (configuration.getStartLevel() > 0) ? String.valueOf(configuration.getStartLevel())
				: "default"; //$NON-NLS-1$
		String autoStart = String.valueOf(configuration.isAutoStart());
		AdditionalPluginData data = new AdditionalPluginData(IPDELauncherConstants.LOCATION_DEFAULT, true, startLevel,
				autoStart);
		return Optional.of(data);
	}

	private String getProgramArguments(String os, String arch) {
		StringBuilder buffer = new StringBuilder(LaunchArgumentsHelper.getInitialProgramArguments());
		IArgumentsInfo info = fProduct.getLauncherArguments();
		String userArgs = (info != null) ? CoreUtility.normalize(info.getCompleteProgramArguments(os, arch)) : ""; //$NON-NLS-1$
		return concatArgs(buffer, userArgs);
	}

	private String getVMArguments(String os, String arch) {
		StringBuilder buffer = new StringBuilder(LaunchArgumentsHelper.getInitialVMArguments());
		IArgumentsInfo info = fProduct.getLauncherArguments();
		String userArgs = (info != null) ? CoreUtility.normalize(info.getCompleteVMArguments(os, arch)) : ""; //$NON-NLS-1$
		return concatArgs(buffer, userArgs);
	}

	private String concatArgs(StringBuilder initialArgs, String userArgs) {
		List<String> initialArgsList = Arrays.asList(DebugPlugin.splitArguments(initialArgs.toString()));
		if (userArgs != null && userArgs.length() > 0) {
			List<String> userArgsList = Arrays.asList(DebugPlugin.splitArguments(userArgs));
			boolean previousHasSubArgument = false;
			for (String userArg : userArgsList) {
				boolean hasSubArgument = userArg.toString().equals('-' + IEnvironment.P_OS) || userArg.toString().equals('-' + IEnvironment.P_WS);
				hasSubArgument = hasSubArgument || userArg.toString().equals('-' + IEnvironment.P_ARCH) || userArg.toString().equals('-' + IEnvironment.P_NL);
				if (!initialArgsList.contains(userArg) || hasSubArgument || previousHasSubArgument) {
					initialArgs.append(' ');
					initialArgs.append(userArg);
				}
				previousHasSubArgument = hasSubArgument;
			}
		}
		String arguments = null;
		try {
			arguments = removeDuplicateArguments(initialArgs);
		} catch (Exception e) {
			PDEPlugin.log(e);
			return initialArgs.toString();
		}
		return arguments;
	}

	private String removeDuplicateArguments(StringBuilder initialArgs) {
		String[] progArguments = { '-' + IEnvironment.P_OS, '-' + IEnvironment.P_WS, '-' + IEnvironment.P_ARCH,
				'-' + IEnvironment.P_NL };
		String defaultStart = "${target."; //$NON-NLS-1$ // see
											// LaunchArgumentHelper
		ArrayList<String> userArgsList = new ArrayList<>(
				Arrays.asList(DebugPlugin.splitArguments(initialArgs.toString())));
		for (String progArgument : progArguments) {
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
		StringBuilder arguments = new StringBuilder();
		for (Iterator<String> iterator = userArgsList.iterator(); iterator.hasNext();) {
			Object userArg = iterator.next();
			arguments.append(userArg);
			if(iterator.hasNext())
				arguments.append(' ');
		}
		return arguments.toString();
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

	private IPluginModelBase[] getModels() {
		Set<IPluginModelBase> launchPlugins = new HashSet<>();
		if (fProduct.useFeatures()) {
			IFeatureModel[] features = getUniqueFeatures();
			for (IFeatureModel feature : features) {
				addFeaturePlugins(feature.getFeature(), launchPlugins);
			}
		} else {
			IProductPlugin[] plugins = fProduct.getPlugins();
			for (IProductPlugin plugin : plugins) {
				String id = plugin.getId();
				if (id == null)
					continue;
				IPluginModelBase model = PluginRegistry.findModel(id);
				if (model != null && TargetPlatformHelper.matchesCurrentEnvironment(model))
					launchPlugins.add(model);
			}
		}
		return launchPlugins.toArray(new IPluginModelBase[launchPlugins.size()]);
	}

	private IFeatureModel[] getUniqueFeatures() {
		ArrayList<IFeatureModel> list = new ArrayList<>();
		IProductFeature[] features = fProduct.getFeatures();
		for (IProductFeature feature : features) {
			String id = feature.getId();
			String version = feature.getVersion();
			addFeatureAndChildren(id, version, list);
		}
		return list.toArray(new IFeatureModel[list.size()]);
	}

	private void addFeatureAndChildren(String id, String version, List<IFeatureModel> list) {
		FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
		IFeatureModel model = manager.findFeatureModel(id, version);
		if (model == null || list.contains(model))
			return;

		list.add(model);

		IFeatureChild[] children = model.getFeature().getIncludedFeatures();
		for (IFeatureChild element : children) {
			addFeatureAndChildren(element.getId(), element.getVersion(), list);
		}
	}

	private void addFeaturePlugins(IFeature feature, Set<IPluginModelBase> launchPlugins) {
		IFeaturePlugin[] plugins = feature.getPlugins();
		for (IFeaturePlugin plugin : plugins) {
			String id = plugin.getId();
			String version = plugin.getVersion();
			if (id == null || version == null)
				continue;
			IPluginModelBase model = PluginRegistry.findModel(id, version, IMatchRules.EQUIVALENT, null);
			if (model == null)
				model = PluginRegistry.findModel(id);
			if (model != null && !launchPlugins.contains(model) && TargetPlatformHelper.matchesCurrentEnvironment(model))
				launchPlugins.add(model);
		}
	}

	private String getTemplateConfigIni(String os) {
		IConfigurationFileInfo info = fProduct.getConfigurationFileInfo();
		if (info != null) {
			String path = info.getPath(os);
			if (path == null) // if we can't find an os path, let's try the normal one
				path = info.getPath(null);
			if (path != null) {
				String expandedPath = getExpandedPath(path);
				if (expandedPath != null) {
					File file = new File(expandedPath);
					if (file.exists() && file.isFile())
						return file.getAbsolutePath();
				}
			}
		}
		return null;
	}

	private String getExpandedPath(String path) {
		if (path == null || path.length() == 0)
			return null;
		IResource resource = PDEPlugin.getWorkspace().getRoot().findMember(new Path(path));
		if (resource != null) {
			IPath fullPath = resource.getLocation();
			return fullPath == null ? null : fullPath.toOSString();
		}
		return null;
	}

	private ILaunchConfiguration chooseConfiguration(ILaunchConfiguration[] configs) {
		IDebugModelPresentation labelProvider = DebugUITools.newDebugModelPresentation();
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), labelProvider);
		dialog.setElements(configs);
		dialog.setTitle(PDEUIMessages.RuntimeWorkbenchShortcut_title);
		if (fMode.equals(ILaunchManager.DEBUG_MODE)) {
			dialog.setMessage(PDEUIMessages.RuntimeWorkbenchShortcut_select_debug);
		} else {
			dialog.setMessage(PDEUIMessages.RuntimeWorkbenchShortcut_select_run);
		}
		dialog.setMultipleSelection(false);
		int result = dialog.open();
		labelProvider.dispose();
		if (result == Window.OK) {
			return (ILaunchConfiguration) dialog.getFirstResult();
		}
		return null;
	}

	private ILaunchConfiguration createConfiguration() throws CoreException {
		ILaunchConfigurationType configType = getWorkbenchLaunchConfigType();
		String computedName = getComputedName(new Path(fPath).lastSegment());
		ILaunchConfigurationWorkingCopy wc = configType.newInstance(null, computedName);
		wc.setAttribute(IPDELauncherConstants.LOCATION, LaunchArgumentsHelper.getDefaultWorkspaceLocation(computedName));
		wc.setAttribute(IPDELauncherConstants.USE_DEFAULT, false);
		wc.setAttribute(IPDELauncherConstants.DOCLEAR, false);
		wc.setAttribute(IPDEConstants.DOCLEARLOG, false);
		wc.setAttribute(IPDEConstants.APPEND_ARGS_EXPLICITLY, true);
		wc.setAttribute(IPDELauncherConstants.ASKCLEAR, true);
		wc.setAttribute(IPDELauncherConstants.USE_PRODUCT, true);
		wc.setAttribute(IPDELauncherConstants.AUTOMATIC_VALIDATE, true);
		wc.setAttribute(IPDELauncherConstants.AUTOMATIC_ADD, false);
		wc.setAttribute(IPDELauncherConstants.PRODUCT_FILE, fPath);
		wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, PDESourcePathProvider.ID);
		wc.setAttribute(IPDELauncherConstants.INCLUDE_OPTIONAL, false);
		return refreshConfiguration(wc);
	}

	private String getComputedName(String prefix) {
		ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
		return lm.generateLaunchConfigurationName(prefix);
	}

	private ILaunchConfiguration[] getLaunchConfigurations() throws CoreException {
		ArrayList<ILaunchConfiguration> result = new ArrayList<>();
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = manager.getLaunchConfigurationType(EclipseLaunchShortcut.CONFIGURATION_TYPE);
		ILaunchConfiguration[] configs = manager.getLaunchConfigurations(type);
		for (int i = 0; i < configs.length; i++) {
			if (!DebugUITools.isPrivate(configs[i])) {
				String path = configs[i].getAttribute(IPDELauncherConstants.PRODUCT_FILE, ""); //$NON-NLS-1$
				if (new Path(fPath).equals(new Path(path))) {
					result.add(configs[i]);
				}
			}
		}
		return result.toArray(new ILaunchConfiguration[result.size()]);
	}

	protected ILaunchConfigurationType getWorkbenchLaunchConfigType() {
		ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
		return lm.getLaunchConfigurationType(EclipseLaunchShortcut.CONFIGURATION_TYPE);
	}

}
