/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.launching.launcher;

import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.launching.IPDEConstants;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.osgi.framework.Version;

public class BundleLauncherHelper {

	/**
	 * When creating a mapping of bundles to their start levels, update configurator is set
	 * to auto start at level three.  However, if at launch time we are launching with both
	 * simple configurator and update configurator, we change the start level as they 
	 * shouldn't be started together.
	 */
	public static final String DEFAULT_UPDATE_CONFIGURATOR_START_LEVEL_TEXT = "3"; //$NON-NLS-1$
	public static final String DEFAULT_UPDATE_CONFIGURATOR_AUTO_START_TEXT = "true"; //$NON-NLS-1$
	public static final String DEFAULT_UPDATE_CONFIGURATOR_START_LEVEL = DEFAULT_UPDATE_CONFIGURATOR_START_LEVEL_TEXT + ":" + DEFAULT_UPDATE_CONFIGURATOR_AUTO_START_TEXT; //$NON-NLS-1$

	public static final char VERSION_SEPARATOR = '*';

	public static Map getWorkspaceBundleMap(ILaunchConfiguration configuration) throws CoreException {
		return getWorkspaceBundleMap(configuration, null, IPDELauncherConstants.WORKSPACE_BUNDLES);
	}

	public static Map getTargetBundleMap(ILaunchConfiguration configuration) throws CoreException {
		return getTargetBundleMap(configuration, null, IPDELauncherConstants.TARGET_BUNDLES);
	}

	public static Map getMergedBundleMap(ILaunchConfiguration configuration, boolean osgi) throws CoreException {
		Set set = new HashSet();
		Map map = new HashMap();

		// if we are using the eclipse-based launcher, we need special checks
		if (!osgi) {

			checkBackwardCompatibility(configuration, true);

			if (configuration.getAttribute(IPDELauncherConstants.USE_DEFAULT, true)) {
				IPluginModelBase[] models = PluginRegistry.getActiveModels();
				for (int i = 0; i < models.length; i++) {
					addBundleToMap(map, models[i], "default:default"); //$NON-NLS-1$
				}
				return map;
			}

			if (configuration.getAttribute(IPDELauncherConstants.USEFEATURES, false)) {
				IPluginModelBase[] models = PluginRegistry.getWorkspaceModels();
				for (int i = 0; i < models.length; i++) {
					addBundleToMap(map, models[i], "default:default"); //$NON-NLS-1$
				}
				return map;
			}
		}

		if (configuration.getAttribute(IPDELauncherConstants.USE_CUSTOM_FEATURES, false)) {
			// Get the default location settings
			String defaultLocation = configuration.getAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, IPDELauncherConstants.LOCATION_WORKSPACE);
			String defaultPluginResolution = configuration.getAttribute(IPDELauncherConstants.FEATURE_PLUGIN_RESOLUTION, IPDELauncherConstants.LOCATION_WORKSPACE);

			// Get all available features
			HashMap workspaceFeatureMap = new HashMap();
			HashMap externalFeatureMap = new HashMap();

			FeatureModelManager fmm = PDECore.getDefault().getFeatureModelManager();
			IFeatureModel[] workspaceFeatureModels = fmm.getWorkspaceModels();
			for (int i = 0; i < workspaceFeatureModels.length; i++) {
				String id = workspaceFeatureModels[i].getFeature().getId();
				workspaceFeatureMap.put(id, workspaceFeatureModels[i]);
			}

			IFeatureModel[] externalFeatureModels = fmm.getExternalModels();
			for (int i = 0; i < externalFeatureModels.length; i++) {
				String id = externalFeatureModels[i].getFeature().getId();
				externalFeatureMap.put(id, externalFeatureModels[i]);
			}

			// Get the selected features and their plugin resolution
			Map featureResolutionMap = new HashMap();
			Set selectedFeatures = configuration.getAttribute(IPDELauncherConstants.SELECTED_FEATURES, (Set) null);
			if (selectedFeatures != null) {
				for (Iterator iterator = selectedFeatures.iterator(); iterator.hasNext();) {
					String currentSelected = (String) iterator.next();
					String[] attributes = currentSelected.split(":"); //$NON-NLS-1$
					if (attributes.length > 1) {
						featureResolutionMap.put(attributes[0], attributes[1]);
					}
				}
			}

			// Get the feature model for each selected feature id and resolve its plugins
			Set launchPlugins = new HashSet();
			for (Iterator iterator = featureResolutionMap.keySet().iterator(); iterator.hasNext();) {
				String id = (String) iterator.next();

				IFeatureModel featureModel = null;
				if (IPDELauncherConstants.LOCATION_WORKSPACE.equalsIgnoreCase(defaultLocation)) {
					featureModel = (IFeatureModel) workspaceFeatureMap.get(id);
				}
				if (featureModel == null || IPDELauncherConstants.LOCATION_EXTERNAL.equalsIgnoreCase(defaultLocation)) {
					if (externalFeatureMap.containsKey(id)) {
						featureModel = (IFeatureModel) externalFeatureMap.get(id);
					}
				}
				if (featureModel == null) {
					continue;
				}

				IFeaturePlugin[] featurePlugins = featureModel.getFeature().getPlugins();
				String pluginResolution = (String) featureResolutionMap.get(id);
				if (IPDELauncherConstants.LOCATION_DEFAULT.equalsIgnoreCase(pluginResolution)) {
					pluginResolution = defaultPluginResolution;
				}

				for (int i = 0; i < featurePlugins.length; i++) {
					ModelEntry modelEntry = PluginRegistry.findEntry(featurePlugins[i].getId());
					if (modelEntry != null) {
						IPluginModelBase model = findModel(modelEntry, featurePlugins[i].getVersion(), pluginResolution);
						if (model != null)
							launchPlugins.add(model);
					}
				}

				IFeatureImport[] featureImports = featureModel.getFeature().getImports();
				for (int i = 0; i < featureImports.length; i++) {
					if (featureImports[i].getType() == IFeatureImport.PLUGIN) {
						ModelEntry modelEntry = PluginRegistry.findEntry(featureImports[i].getId());
						if (modelEntry != null) {
							IPluginModelBase model = findModel(modelEntry, featureImports[i].getVersion(), pluginResolution);
							if (model != null)
								launchPlugins.add(model);
						}
					}
				}
			}

			HashMap additionalPlugins = getAdditionalPlugins(configuration, true);
			launchPlugins.addAll(additionalPlugins.keySet());

			// Get any plug-ins required by the application/product set on the config
			if (!osgi) {
				String[] applicationIds = RequirementHelper.getApplicationRequirements(configuration);
				for (int i = 0; i < applicationIds.length; i++) {
					ModelEntry modelEntry = PluginRegistry.findEntry(applicationIds[i]);
					if (modelEntry != null) {
						IPluginModelBase model = findModel(modelEntry, null, defaultPluginResolution);
						if (model != null)
							launchPlugins.add(model);
					}
				}
			}

			// Get all required plugins
			// exclude "org.eclipse.ui.workbench.compatibility" - it is only needed for pre-3.0 bundles
			Set additionalIds = DependencyManager.getDependencies(launchPlugins.toArray(), false, new String[] {"org.eclipse.ui.workbench.compatibility"}); //$NON-NLS-1$
			Iterator it = additionalIds.iterator();
			while (it.hasNext()) {
				String id = (String) it.next();
				ModelEntry modelEntry = PluginRegistry.findEntry(id);
				if (modelEntry != null) {
					IPluginModelBase model = findModel(modelEntry, null, defaultPluginResolution);
					if (model != null)
						launchPlugins.add(model);
				}
			}

			//remove conflicting duplicates - if they have same version or both are singleton
			HashMap pluginMap = new HashMap();
			List workspaceModels = null;
			for (Iterator iterator = launchPlugins.iterator(); iterator.hasNext();) {
				IPluginModelBase model = (IPluginModelBase) iterator.next();
				String id = model.getPluginBase().getId();
				if (pluginMap.containsKey(id)) {
					IPluginModelBase existing = (IPluginModelBase) pluginMap.get(id);
					if (model.getPluginBase().getVersion().equalsIgnoreCase(existing.getPluginBase().getVersion()) || (isSingleton(model) && isSingleton(existing))) {
						if (workspaceModels == null)
							workspaceModels = Arrays.asList(PluginRegistry.getWorkspaceModels());
						if (!workspaceModels.contains(existing)) { //if existing model is external 							
							pluginMap.put(id, model); // launch the workspace model 
							continue;
						}
					}
				}
				pluginMap.put(id, model);
			}

			// Create the start levels for the selected plugins and add them to the map
			for (Iterator iterator = pluginMap.values().iterator(); iterator.hasNext();) {
				IPluginModelBase model = (IPluginModelBase) iterator.next();
				addBundleToMap(map, model, "default:default"); //$NON-NLS-1$
			}
			return map;
		}

		String workspace = osgi == false ? IPDELauncherConstants.SELECTED_WORKSPACE_PLUGINS : IPDELauncherConstants.WORKSPACE_BUNDLES;
		String target = osgi == false ? IPDELauncherConstants.SELECTED_TARGET_PLUGINS : IPDELauncherConstants.TARGET_BUNDLES;
		map = getWorkspaceBundleMap(configuration, set, workspace);
		map.putAll(getTargetBundleMap(configuration, set, target));
		return map;
	}

	/**
	 * Finds the best candidate model from the <code>resolution</code> location. If the model is not found there, 
	 * alternate location is explored before returning <code>null</code>.
	 * @param modelEntry
	 * @param version
	 * @param location
	 * @return model
	 */
	private static IPluginModelBase findModel(ModelEntry modelEntry, String version, String location) {
		IPluginModelBase model = null;
		if (IPDELauncherConstants.LOCATION_WORKSPACE.equalsIgnoreCase(location)) {
			model = getBestCandidateModel(modelEntry.getWorkspaceModels(), version);
		}
		if (model == null) {
			model = getBestCandidateModel(modelEntry.getExternalModels(), version);
		}
		if (model == null && IPDELauncherConstants.LOCATION_EXTERNAL.equalsIgnoreCase(location)) {
			model = getBestCandidateModel(modelEntry.getWorkspaceModels(), version);
		}
		return model;
	}

	private static boolean isSingleton(IPluginModelBase model) {
		if (model.getBundleDescription() == null || model.getBundleDescription().isSingleton()) {
			return true;
		}
		return false;
	}

	/**
	 * Returns model from the given list that is a 'best match' to the given bundle version or
	 * <code>null</code> if no enabled models were in the provided list.  The best match will
	 * be an exact version match if one is found.  Otherwise a model that is resolved in the
	 * OSGi state with the highest version is returned.
	 * 
	 * @param models list of candidate models to choose from
	 * @param version the bundle version to find a match for
	 * @return best candidate model from the list of models or <code>null</code> if no there were no acceptable models in the list
	 */
	private static IPluginModelBase getBestCandidateModel(IPluginModelBase[] models, String version) {
		Version requiredVersion = version != null ? Version.parseVersion(version) : Version.emptyVersion;
		IPluginModelBase model = null;
		for (int i = 0; i < models.length; i++) {
			if (models[i].getBundleDescription() == null || !models[i].isEnabled())
				continue;

			if (model == null) {
				model = models[i];
				if (requiredVersion.compareTo(model.getBundleDescription().getVersion()) == 0) {
					break;
				}
				continue;
			}

			if (!model.isEnabled() && models[i].isEnabled()) {
				model = models[i];
				continue;
			}

			BundleDescription current = model.getBundleDescription();
			BundleDescription candidate = models[i].getBundleDescription();
			if (current == null || candidate == null) {
				continue;
			}

			if (!current.isResolved() && candidate.isResolved()) {
				model = models[i];
				continue;
			}

			if (requiredVersion.compareTo(candidate.getVersion()) == 0) {
				model = models[i];
				break;
			}

			if (current.getVersion().compareTo(candidate.getVersion()) < 0) {
				model = models[i];
			}
		}
		return model;
	}

	public static IPluginModelBase[] getMergedBundles(ILaunchConfiguration configuration, boolean osgi) throws CoreException {
		Map map = getMergedBundleMap(configuration, osgi);
		return (IPluginModelBase[]) map.keySet().toArray(new IPluginModelBase[map.size()]);
	}

	public static Map getWorkspaceBundleMap(ILaunchConfiguration configuration, Set set, String attribute) throws CoreException {
		String selected = configuration.getAttribute(attribute, ""); //$NON-NLS-1$
		Map map = new HashMap();
		StringTokenizer tok = new StringTokenizer(selected, ","); //$NON-NLS-1$
		while (tok.hasMoreTokens()) {
			String token = tok.nextToken();
			int index = token.indexOf('@');
			if (index < 0) { // if no start levels, assume default
				token = token.concat("@default:default"); //$NON-NLS-1$
				index = token.indexOf('@');
			}
			String idVersion = token.substring(0, index);
			int versionIndex = idVersion.indexOf(VERSION_SEPARATOR);
			String id = (versionIndex > 0) ? idVersion.substring(0, versionIndex) : idVersion;
			String version = (versionIndex > 0) ? idVersion.substring(versionIndex + 1) : null;
			if (set != null)
				set.add(id);
			ModelEntry entry = PluginRegistry.findEntry(id);
			if (entry != null) {
				IPluginModelBase[] models = entry.getWorkspaceModels();
				Set versions = new HashSet();
				for (int i = 0; i < models.length; i++) {
					IPluginBase base = models[i].getPluginBase();
					String v = base.getVersion();
					if (versions.add(v)) { // don't add exact same version more than once

						// match only if...
						// a) if we have the same version
						// b) no version
						// c) all else fails, if there's just one bundle available, use it
						if (base.getVersion().equals(version) || version == null || models.length == 1)
							addBundleToMap(map, models[i], token.substring(index + 1));
					}
				}
			}
		}

		if (configuration.getAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true)) {
			Set deselectedPlugins = LaunchPluginValidator.parsePlugins(configuration, IPDELauncherConstants.DESELECTED_WORKSPACE_PLUGINS);
			IPluginModelBase[] models = PluginRegistry.getWorkspaceModels();
			for (int i = 0; i < models.length; i++) {
				String id = models[i].getPluginBase().getId();
				if (id == null)
					continue;
				if (!deselectedPlugins.contains(models[i])) {
					if (set != null)
						set.add(id);
					if (!map.containsKey(models[i]))
						addBundleToMap(map, models[i], "default:default"); //$NON-NLS-1$
				}
			}
		}
		return map;
	}

	public static String resolveSystemRunLevelText(IPluginModelBase model) {
		BundleDescription description = model.getBundleDescription();
		String modelName = description.getSymbolicName();

		if (IPDEBuildConstants.BUNDLE_DS.equals(modelName)) {
			return "1"; //$NON-NLS-1$ 
		} else if (IPDEBuildConstants.BUNDLE_SIMPLE_CONFIGURATOR.equals(modelName)) {
			return "1"; //$NON-NLS-1$
		} else if (IPDEBuildConstants.BUNDLE_EQUINOX_COMMON.equals(modelName)) {
			return "2"; //$NON-NLS-1$
		} else if (IPDEBuildConstants.BUNDLE_OSGI.equals(modelName)) {
			return "-1"; //$NON-NLS-1$
		} else if (IPDEBuildConstants.BUNDLE_UPDATE_CONFIGURATOR.equals(modelName)) {
			return DEFAULT_UPDATE_CONFIGURATOR_START_LEVEL_TEXT;
		} else if (IPDEBuildConstants.BUNDLE_CORE_RUNTIME.equals(modelName)) {
			if (TargetPlatformHelper.getTargetVersion() > 3.1) {
				return "default"; //$NON-NLS-1$
			}
			return "2"; //$NON-NLS-1$
		} else {
			return null;
		}
	}

	public static String resolveSystemAutoText(IPluginModelBase model) {
		BundleDescription description = model.getBundleDescription();
		String modelName = description.getSymbolicName();

		if (IPDEBuildConstants.BUNDLE_DS.equals(modelName)) {
			return "true"; //$NON-NLS-1$ 
		} else if (IPDEBuildConstants.BUNDLE_SIMPLE_CONFIGURATOR.equals(modelName)) {
			return "true"; //$NON-NLS-1$
		} else if (IPDEBuildConstants.BUNDLE_EQUINOX_COMMON.equals(modelName)) {
			return "true"; //$NON-NLS-1$
		} else if (IPDEBuildConstants.BUNDLE_OSGI.equals(modelName)) {
			return "true"; //$NON-NLS-1$
		} else if (IPDEBuildConstants.BUNDLE_UPDATE_CONFIGURATOR.equals(modelName)) {
			return DEFAULT_UPDATE_CONFIGURATOR_AUTO_START_TEXT;
		} else if (IPDEBuildConstants.BUNDLE_CORE_RUNTIME.equals(modelName)) {
			if (TargetPlatformHelper.getTargetVersion() > 3.1) {
				return "true"; //$NON-NLS-1$
			}
			return "true"; //$NON-NLS-1$
		} else {
			return null;
		}
	}

	/**
	 * Adds the given bundle and start information to the map.  This will override anything set
	 * for system bundles, and set their start level to the appropriate level
	 * @param map The map to add the bundles too
	 * @param bundle The bundle to add
	 * @param substring the start information in the form level:autostart
	 */
	private static void addBundleToMap(Map map, IPluginModelBase bundle, String sl) {
		BundleDescription desc = bundle.getBundleDescription();
		boolean defaultsl = (sl == null || sl.equals("default:default")); //$NON-NLS-1$
		if (desc != null && defaultsl) {
			String runLevelText = resolveSystemRunLevelText(bundle);
			String autoText = resolveSystemAutoText(bundle);
			if (runLevelText != null && autoText != null) {
				map.put(bundle, runLevelText + ":" + autoText); //$NON-NLS-1$
			} else {
				map.put(bundle, sl);
			}
		} else {
			map.put(bundle, sl);
		}

	}

	public static Map getTargetBundleMap(ILaunchConfiguration configuration, Set set, String attribute) throws CoreException {
		String selected = configuration.getAttribute(attribute, ""); //$NON-NLS-1$
		Map map = new HashMap();
		StringTokenizer tok = new StringTokenizer(selected, ","); //$NON-NLS-1$
		while (tok.hasMoreTokens()) {
			String token = tok.nextToken();
			int index = token.indexOf('@');
			if (index < 0) { // if no start levels, assume default
				token = token.concat("@default:default"); //$NON-NLS-1$
				index = token.indexOf('@');
			}
			String idVersion = token.substring(0, index);
			int versionIndex = idVersion.indexOf(VERSION_SEPARATOR);
			String id = (versionIndex > 0) ? idVersion.substring(0, versionIndex) : idVersion;
			String version = (versionIndex > 0) ? idVersion.substring(versionIndex + 1) : null;
			if (set != null && set.contains(id))
				continue;
			ModelEntry entry = PluginRegistry.findEntry(id);
			if (entry != null) {
				IPluginModelBase[] models = entry.getExternalModels();
				for (int i = 0; i < models.length; i++) {
					if (models[i].isEnabled()) {
						IPluginBase base = models[i].getPluginBase();
						// match only if...
						// a) if we have the same version
						// b) no version
						// c) all else fails, if there's just one bundle available, use it
						if (base.getVersion().equals(version) || version == null || models.length == 1)
							addBundleToMap(map, models[i], token.substring(index + 1));
					}
				}
			}
		}
		return map;
	}

	public static String writeBundleEntry(IPluginModelBase model, String startLevel, String autoStart) {
		IPluginBase base = model.getPluginBase();
		String id = base.getId();
		StringBuffer buffer = new StringBuffer(id);

		ModelEntry entry = PluginRegistry.findEntry(id);
		if (entry != null && entry.getActiveModels().length > 1) {
			buffer.append(VERSION_SEPARATOR);
			buffer.append(model.getPluginBase().getVersion());
		}

		boolean hasStartLevel = (startLevel != null && startLevel.length() > 0);
		boolean hasAutoStart = (autoStart != null && autoStart.length() > 0);

		if (hasStartLevel || hasAutoStart)
			buffer.append('@');
		if (hasStartLevel)
			buffer.append(startLevel);
		if (hasStartLevel || hasAutoStart)
			buffer.append(':');
		if (hasAutoStart)
			buffer.append(autoStart);
		return buffer.toString();
	}

	public static void checkBackwardCompatibility(ILaunchConfiguration configuration, boolean save) throws CoreException {
		ILaunchConfigurationWorkingCopy wc = null;
		if (configuration.isWorkingCopy()) {
			wc = (ILaunchConfigurationWorkingCopy) configuration;
		} else {
			wc = configuration.getWorkingCopy();
		}

		String value = configuration.getAttribute("wsproject", (String) null); //$NON-NLS-1$
		if (value != null) {
			wc.setAttribute("wsproject", (String) null); //$NON-NLS-1$
			if (value.indexOf(';') != -1) {
				value = value.replace(';', ',');
			} else if (value.indexOf(':') != -1) {
				value = value.replace(':', ',');
			}
			value = (value.length() == 0 || value.equals(",")) //$NON-NLS-1$
			? null
					: value.substring(0, value.length() - 1);

			boolean automatic = configuration.getAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true);
			String attr = automatic ? IPDELauncherConstants.DESELECTED_WORKSPACE_PLUGINS : IPDELauncherConstants.SELECTED_WORKSPACE_PLUGINS;
			wc.setAttribute(attr, value);
		}

		String value2 = configuration.getAttribute("extplugins", (String) null); //$NON-NLS-1$
		if (value2 != null) {
			wc.setAttribute("extplugins", (String) null); //$NON-NLS-1$
			if (value2.indexOf(';') != -1) {
				value2 = value2.replace(';', ',');
			} else if (value2.indexOf(':') != -1) {
				value2 = value2.replace(':', ',');
			}
			value2 = (value2.length() == 0 || value2.equals(",")) ? null : value2.substring(0, value2.length() - 1); //$NON-NLS-1$
			wc.setAttribute(IPDELauncherConstants.SELECTED_TARGET_PLUGINS, value2);
		}

		String version = configuration.getAttribute(IPDEConstants.LAUNCHER_PDE_VERSION, (String) null);
		boolean newApp = TargetPlatformHelper.usesNewApplicationModel();
		boolean upgrade = !"3.3".equals(version) && newApp; //$NON-NLS-1$
		if (!upgrade)
			upgrade = TargetPlatformHelper.getTargetVersion() >= 3.2 && version == null;
		if (upgrade) {
			wc.setAttribute(IPDEConstants.LAUNCHER_PDE_VERSION, newApp ? "3.3" : "3.2a"); //$NON-NLS-1$ //$NON-NLS-2$
			boolean usedefault = configuration.getAttribute(IPDELauncherConstants.USE_DEFAULT, true);
			boolean useFeatures = configuration.getAttribute(IPDELauncherConstants.USEFEATURES, false);
			boolean automaticAdd = configuration.getAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true);
			if (!usedefault && !useFeatures) {
				ArrayList list = new ArrayList();
				if (version == null) {
					list.add("org.eclipse.core.contenttype"); //$NON-NLS-1$
					list.add("org.eclipse.core.jobs"); //$NON-NLS-1$
					list.add(IPDEBuildConstants.BUNDLE_EQUINOX_COMMON);
					list.add("org.eclipse.equinox.preferences"); //$NON-NLS-1$
					list.add("org.eclipse.equinox.registry"); //$NON-NLS-1$
					list.add("org.eclipse.core.runtime.compatibility.registry"); //$NON-NLS-1$
				}
				if (!"3.3".equals(version) && newApp) //$NON-NLS-1$
					list.add("org.eclipse.equinox.app"); //$NON-NLS-1$
				StringBuffer extensions = new StringBuffer(configuration.getAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_PLUGINS, "")); //$NON-NLS-1$
				StringBuffer target = new StringBuffer(configuration.getAttribute(IPDELauncherConstants.SELECTED_TARGET_PLUGINS, "")); //$NON-NLS-1$
				for (int i = 0; i < list.size(); i++) {
					String plugin = list.get(i).toString();
					IPluginModelBase model = PluginRegistry.findModel(plugin);
					if (model == null)
						continue;
					if (model.getUnderlyingResource() != null) {
						if (automaticAdd)
							continue;
						if (extensions.length() > 0)
							extensions.append(","); //$NON-NLS-1$
						extensions.append(plugin);
					} else {
						if (target.length() > 0)
							target.append(","); //$NON-NLS-1$
						target.append(plugin);
					}
				}
				if (extensions.length() > 0)
					wc.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_PLUGINS, extensions.toString());
				if (target.length() > 0)
					wc.setAttribute(IPDELauncherConstants.SELECTED_TARGET_PLUGINS, target.toString());
			}
		}

		if (save && (value != null || value2 != null || upgrade))
			wc.doSave();
	}

	public static String writeAdditionalPluginsEntry(IPluginModelBase model, String pluginResolution, boolean checked) {
		IPluginBase base = model.getPluginBase();
		String id = base.getId();
		StringBuffer buffer = new StringBuffer(id);
		buffer.append(':');
		buffer.append(base.getVersion());
		buffer.append(':');
		buffer.append(pluginResolution);
		buffer.append(':');
		buffer.append(checked);
		return buffer.toString();
	}

	/**
	 * Returns a map of IPluginModelBase to their associated String resolution setting. Reads the 
	 * additional plug-ins attribute of the given launch config and returns a map of plug-in models
	 * to their resolution.  The attribute stores the id, version, enablement and resolution of each plug-in.
	 * The models to be returned are determined by trying to find a model with a matching name, matching version
	 * (or highest) in the resolution location (falling back on other locations if the chosen option is unavailable).
	 * The includeDisabled option allows the returned list to contain only plug-ins that are enabled (checked) in
	 * the config.
	 * 
	 * @param config launch config to read attribute from
	 * @param onlyEnabled whether all plug-ins in the attribute should be returned or just the ones marked as enabled/checked
	 * @return map of IPluginModelBase to String resolution setting
	 * @throws CoreException if there is a problem reading the launch config
	 */
	public static HashMap getAdditionalPlugins(ILaunchConfiguration config, boolean onlyEnabled) throws CoreException {
		HashMap resolvedAdditionalPlugins = new HashMap();
		Set userAddedPlugins = config.getAttribute(IPDELauncherConstants.ADDITIONAL_PLUGINS, (Set) null);
		String defaultPluginResolution = config.getAttribute(IPDELauncherConstants.FEATURE_PLUGIN_RESOLUTION, IPDELauncherConstants.LOCATION_WORKSPACE);
		if (userAddedPlugins != null) {
			for (Iterator iterator = userAddedPlugins.iterator(); iterator.hasNext();) {
				String addedPlugin = (String) iterator.next();
				String[] pluginData = addedPlugin.split(":"); //$NON-NLS-1$
				boolean checked = Boolean.valueOf(pluginData[3]).booleanValue();
				if (!onlyEnabled || checked) {
					String id = pluginData[0];
					String version = pluginData[1];
					String pluginResolution = pluginData[2];
					ModelEntry pluginModelEntry = PluginRegistry.findEntry(id);
					if (pluginModelEntry != null) {
						if (IPDELauncherConstants.LOCATION_DEFAULT.equalsIgnoreCase(pluginResolution)) {
							pluginResolution = defaultPluginResolution;
						}
						IPluginModelBase model = findModel(pluginModelEntry, version, pluginResolution);
						if (model != null) {
							resolvedAdditionalPlugins.put(model, pluginData[2]);
						}
					}
				}
			}
		}
		return resolvedAdditionalPlugins;
	}
}
