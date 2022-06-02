/*******************************************************************************
 * Copyright (c) 2007, 2022 IBM Corporation and others.
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
 *     Hannes Wellmann - Bug 576885 - Unify methods to parse bundle-sets from launch-configs
 *     Hannes Wellmann - Bug 577118 - Handle multiple Plug-in versions in launching facility
 *     Hannes Wellmann - Bug 576886 - Clean up and improve BundleLaunchHelper and extract String literal constants
 *     Hannes Wellmann - Bug 576887 - Handle multiple versions of features and plug-ins for feature-launches
 *     Hannes Wellmann - Bug 576888, Bug 576889 - Consider included child-features and required dependency-features for feature-launches
 *     Hannes Wellmann - Bug 576890 - Ignore included features/plug-ins not matching target-environment
 *     Hannes Wellmann - Bug 544838 - Option to automatically add requirements at launch
 *******************************************************************************/
package org.eclipse.pde.internal.launching.launcher;

import static java.util.Collections.emptySet;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;

import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.util.VersionUtil;
import org.eclipse.pde.internal.launching.*;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.osgi.framework.Version;

public class BundleLauncherHelper {

	private BundleLauncherHelper() { // static use only
	}

	public static final char VERSION_SEPARATOR = '*';
	private static final char START_LEVEL_SEPARATOR = '@';
	private static final String AUTO_START_SEPARATOR = ":"; //$NON-NLS-1$  
	private static final String DEFAULT = "default"; //$NON-NLS-1$
	private static final String DEFAULT_START_LEVELS = DEFAULT + AUTO_START_SEPARATOR + DEFAULT;
	private static final String FEATURE_PLUGIN_RESOLUTION_SEPARATOR = ":"; //$NON-NLS-1$  
	private static final String FEATURES_ADDITIONAL_PLUGINS_DATA_SEPARATOR = ":"; //$NON-NLS-1$  

	/**
	 * When creating a mapping of bundles to their start levels, update configurator is set
	 * to auto start at level three.  However, if at launch time we are launching with both
	 * simple configurator and update configurator, we change the start level as they
	 * shouldn't be started together.
	 */
	public static final String DEFAULT_UPDATE_CONFIGURATOR_START_LEVEL_TEXT = "3"; //$NON-NLS-1$
	public static final String DEFAULT_UPDATE_CONFIGURATOR_AUTO_START_TEXT = "true"; //$NON-NLS-1$
	public static final String DEFAULT_UPDATE_CONFIGURATOR_START_LEVEL = DEFAULT_UPDATE_CONFIGURATOR_START_LEVEL_TEXT + AUTO_START_SEPARATOR + DEFAULT_UPDATE_CONFIGURATOR_AUTO_START_TEXT;

	public static Map<IPluginModelBase, String> getWorkspaceBundleMap(ILaunchConfiguration configuration) throws CoreException {
		return getWorkspaceBundleMap(configuration, new HashMap<>());
	}

	public static Map<IPluginModelBase, String> getMergedBundleMap(ILaunchConfiguration configuration, boolean osgi) throws CoreException {

		ILaunchConfigurationWorkingCopy wc = getWorkingCopy(configuration);
		if (!osgi) {

			migrateLaunchConfiguration(wc);

			if (wc.getAttribute(IPDELauncherConstants.USE_DEFAULT, true)) {
				Map<IPluginModelBase, String> map = new LinkedHashMap<>();
				for (IPluginModelBase model : PluginRegistry.getActiveModels()) {
					if (!isFragmentForOtherPlatform(model)) { // Filter out platform-specific fragments that cannot resolve
						addBundleToMap(map, model, DEFAULT_START_LEVELS);
					}
				}
				return map;
			}

		} else {
			migrateOsgiLaunchConfiguration(wc);
		}

		if (wc.getAttribute(IPDELauncherConstants.USE_CUSTOM_FEATURES, false)) {
			return getMergedBundleMapFeatureBased(wc);
		}

		Map<IPluginModelBase, String> selectedBundles = getAllSelectedPluginBundles(wc);
		boolean autoAddRequirements = configuration.getAttribute(IPDELauncherConstants.AUTOMATIC_INCLUDE_REQUIREMENTS, false);
		if (autoAddRequirements) {
			addRequiredBundles(selectedBundles, configuration);
		}
		return selectedBundles;
	}

	private static boolean isFragmentForOtherPlatform(IPluginModelBase model) {
		return model.isFragmentModel() && !model.getBundleDescription().isResolved() && !TargetPlatformHelper.matchesCurrentEnvironment(model);
	}

	public static Map<IPluginModelBase, String> getAllSelectedPluginBundles(ILaunchConfiguration config) throws CoreException {
		Map<String, List<Version>> idVersions = new HashMap<>();
		Map<IPluginModelBase, String> map = getWorkspaceBundleMap(config, idVersions);
		map.putAll(getTargetBundleMap(config, idVersions));
		return map;
	}

	private static void addRequiredBundles(Map<IPluginModelBase, String> bundle2startLevel, ILaunchConfiguration configuration) throws CoreException {

		List<String> appRequirements = RequirementHelper.getApplicationLaunchRequirements(configuration);
		RequirementHelper.addApplicationLaunchRequirements(appRequirements, configuration, bundle2startLevel);

		boolean includeOptional = configuration.getAttribute(IPDELauncherConstants.INCLUDE_OPTIONAL, true);
		Set<BundleDescription> requiredDependencies = includeOptional //
				? DependencyManager.getDependencies(bundle2startLevel.keySet(), DependencyManager.Options.INCLUDE_OPTIONAL_DEPENDENCIES)
				: DependencyManager.getDependencies(bundle2startLevel.keySet());

		requiredDependencies.stream() //
				.map(PluginRegistry::findModel).filter(Objects::nonNull) //
				.forEach(p -> addDefaultStartingBundle(bundle2startLevel, p));
	}

	// --- feature based launches ---

	private static Map<IPluginModelBase, String> getMergedBundleMapFeatureBased(ILaunchConfiguration configuration) throws CoreException {

		String defaultPluginResolution = configuration.getAttribute(IPDELauncherConstants.FEATURE_PLUGIN_RESOLUTION, IPDELauncherConstants.LOCATION_WORKSPACE);
		ITargetDefinition target = PDECore.getDefault().acquireService(ITargetPlatformService.class).getWorkspaceTargetDefinition();
		boolean addRequirements = configuration.getAttribute(IPDELauncherConstants.AUTOMATIC_INCLUDE_REQUIREMENTS, true);

		Map<IFeature, String> feature2resolution = getSelectedFeatures(configuration, target, addRequirements);

		// Get the feature model for each selected feature id and resolve its plugins
		Set<IPluginModelBase> launchPlugins = new HashSet<>();

		feature2resolution.forEach((feature, pluginResolution) -> {
			if (IPDELauncherConstants.LOCATION_DEFAULT.equalsIgnoreCase(pluginResolution)) {
				pluginResolution = defaultPluginResolution;
			}
			IFeaturePlugin[] featurePlugins = feature.getPlugins();
			for (IFeaturePlugin featurePlugin : featurePlugins) {
				if (featurePlugin.matchesEnvironment(target)) {
					IPluginModelBase plugin = getIncludedPlugin(featurePlugin.getId(), featurePlugin.getVersion(), pluginResolution);
					if (plugin != null) {
						launchPlugins.add(plugin);
					}
				}
			}
			if (addRequirements) {
				IFeatureImport[] featureImports = feature.getImports();
				for (IFeatureImport featureImport : featureImports) {
					if (featureImport.getType() == IFeatureImport.PLUGIN) {
						IPluginModelBase plugin = getRequiredPlugin(featureImport.getId(), featureImport.getVersion(), featureImport.getMatch(), pluginResolution);
						if (plugin != null) {
							launchPlugins.add(plugin);
						}
					}
				}
			}
		});

		Map<IPluginModelBase, AdditionalPluginData> additionalPlugins = getAdditionalPlugins(configuration, true);
		launchPlugins.addAll(additionalPlugins.keySet());

		if (addRequirements) {
			// Add all missing  plug-ins required by the application/product set in the config
			List<String> appRequirements = RequirementHelper.getApplicationLaunchRequirements(configuration);
			RequirementHelper.addApplicationLaunchRequirements(appRequirements, configuration, launchPlugins, launchPlugins::add);

			// Get all required plugins
			Set<BundleDescription> additionalBundles = DependencyManager.getDependencies(launchPlugins);
			for (BundleDescription bundle : additionalBundles) {
				IPluginModelBase plugin = getRequiredPlugin(bundle.getSymbolicName(), bundle.getVersion().toString(), IMatchRules.PERFECT, defaultPluginResolution);
				launchPlugins.add(Objects.requireNonNull(plugin));// should never be null
			}
		}

		// Create the start levels for the selected plugins and add them to the map
		Map<IPluginModelBase, String> map = new LinkedHashMap<>();
		for (IPluginModelBase model : launchPlugins) {
			AdditionalPluginData additionalPluginData = additionalPlugins.get(model);
			String startLevels = additionalPluginData != null ? additionalPluginData.startLevels() : DEFAULT_START_LEVELS;
			addBundleToMap(map, model, startLevels); // might override data of plug-ins included by feature
		}
		return map;
	}

	private static Map<IFeature, String> getSelectedFeatures(ILaunchConfiguration configuration, ITargetDefinition target, boolean addRequirements) throws CoreException {
		String featureLocation = configuration.getAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, IPDELauncherConstants.LOCATION_WORKSPACE);

		Predicate<IFeature> targetEnvironmentFilter = f -> f.matchesEnvironment(target);

		// Get all available features
		Map<String, List<List<IFeature>>> featureMaps = getPrioritizedAvailableFeatures(featureLocation);

		Set<String> selectedFeatures = configuration.getAttribute(IPDELauncherConstants.SELECTED_FEATURES, emptySet());

		Map<IFeature, String> feature2pluginResolution = new HashMap<>();
		Queue<IFeature> pendingFeatures = new ArrayDeque<>();
		for (String currentSelected : selectedFeatures) {
			String[] attributes = currentSelected.split(FEATURE_PLUGIN_RESOLUTION_SEPARATOR);
			if (attributes.length > 1) {
				String id = attributes[0];
				String pluginResolution = attributes[1];
				IFeature feature = getRequiredFeature(id, null, IMatchRules.GREATER_OR_EQUAL, targetEnvironmentFilter, featureMaps);
				addFeatureIfAbsent(feature, pluginResolution, feature2pluginResolution, pendingFeatures); // feature should be absent
			}
		}

		while (!pendingFeatures.isEmpty()) { // perform exhaustive breath-first-search for included and required features
			IFeature feature = pendingFeatures.remove();
			String pluginResolution = feature2pluginResolution.get(feature); // inherit resolution from including feature

			IFeatureChild[] includedFeatures = feature.getIncludedFeatures();
			for (IFeatureChild featureChild : includedFeatures) {
				if (featureChild.matchesEnvironment(target)) {
					IFeature child = getIncludedFeature(featureChild.getId(), featureChild.getVersion(), targetEnvironmentFilter, featureMaps);
					addFeatureIfAbsent(child, pluginResolution, feature2pluginResolution, pendingFeatures);
				}
			}

			if (addRequirements) {
				IFeatureImport[] featureImports = feature.getImports();
				for (IFeatureImport featureImport : featureImports) {
					if (featureImport.getType() == IFeatureImport.FEATURE) {
						IFeature dependency = getRequiredFeature(featureImport.getId(), featureImport.getVersion(), featureImport.getMatch(), targetEnvironmentFilter, featureMaps);
						addFeatureIfAbsent(dependency, pluginResolution, feature2pluginResolution, pendingFeatures);
					}
				}
			}
		}
		return feature2pluginResolution;
	}

	private static Map<String, List<List<IFeature>>> getPrioritizedAvailableFeatures(String featureLocation) {
		FeatureModelManager fmm = PDECore.getDefault().getFeatureModelManager();
		List<IFeatureModel[]> featureModelsPerLocation = isWorkspace(featureLocation) //
				? List.of(fmm.getWorkspaceModels(), fmm.getExternalModels()) //
				: Collections.singletonList(fmm.getExternalModels());

		Map<String, List<List<IFeature>>> featureMaps = new HashMap<>();
		for (IFeatureModel[] featureModels : featureModelsPerLocation) {
			Map<String, List<IFeature>> id2feature = Arrays.stream(featureModels).map(IFeatureModel::getFeature).filter(f -> {
				if (f.getId() == null) {
					IResource resource = f.getModel().getUnderlyingResource();
					PDELaunchingPlugin.log(Status.warning(resource != null //
							? NLS.bind(PDEMessages.BundleLauncherHelper_workspaceFeatureWithIdNull, resource.getProject().getName(), resource.getProjectRelativePath())
							: NLS.bind(PDEMessages.BundleLauncherHelper_targetFeatureWithIdNull, f.getModel().getInstallLocation())));
					return false;
				}
				return true;
			}).collect(groupingBy(IFeature::getId));
			id2feature.forEach((id, features) -> featureMaps.computeIfAbsent(id, i -> new ArrayList<>()).add(features));
		}
		return featureMaps;
	}

	private static void addFeatureIfAbsent(IFeature feature, String resolution, Map<IFeature, String> featurePluginResolution, Queue<IFeature> pendingFeatures) {
		if (feature != null && featurePluginResolution.putIfAbsent(feature, resolution) == null) {
			// Don't add feature more than once to not override the resolution if already present (e.g. a child was specified explicitly)
			pendingFeatures.add(feature); // ... and to not process it more than once 
		}
	}

	private static boolean isWorkspace(String location) {
		if (IPDELauncherConstants.LOCATION_WORKSPACE.equalsIgnoreCase(location)) {
			return true;
		} else if (IPDELauncherConstants.LOCATION_EXTERNAL.equalsIgnoreCase(location)) {
			return false;
		}
		throw new IllegalArgumentException("Unsupported location: " + location); //$NON-NLS-1$
	}

	private static final Comparator<IFeature> NEUTRAL_COMPARATOR = comparing(f -> 0);

	private static IFeature getIncludedFeature(String id, String version, Predicate<IFeature> environmentFilter, Map<String, List<List<IFeature>>> prioritizedFeatures) {
		List<List<IFeature>> features = prioritizedFeatures.get(id);
		return getIncluded(features, environmentFilter, IFeature::getVersion, NEUTRAL_COMPARATOR, version);
	}

	private static IFeature getRequiredFeature(String id, String version, int versionMatchRule, Predicate<IFeature> environmentFilter, Map<String, List<List<IFeature>>> prioritizedFeatures) {
		List<List<IFeature>> features = prioritizedFeatures.get(id);
		return getRequired(features, environmentFilter, IFeature::getVersion, NEUTRAL_COMPARATOR, version, versionMatchRule);
	}

	private static final Predicate<IPluginModelBase> ENABLED_VALID_PLUGIN_FILTER = p -> p.getBundleDescription() != null && p.isEnabled();
	private static final Function<IPluginModelBase, String> GET_PLUGIN_VERSION = m -> m.getPluginBase().getVersion();
	private static final Comparator<IPluginModelBase> COMPARE_PLUGIN_RESOLVED = comparing(p -> p.getBundleDescription().isResolved());

	private static IPluginModelBase getIncludedPlugin(String id, String version, String pluginLocation) {
		List<List<IPluginModelBase>> plugins = getPlugins(id, pluginLocation);
		return getIncluded(plugins, ENABLED_VALID_PLUGIN_FILTER, GET_PLUGIN_VERSION, COMPARE_PLUGIN_RESOLVED, version);
	}

	private static IPluginModelBase getRequiredPlugin(String id, String version, int versionMatchRule, String pluginLocation) {
		List<List<IPluginModelBase>> plugins = getPlugins(id, pluginLocation);
		return getRequired(plugins, ENABLED_VALID_PLUGIN_FILTER, GET_PLUGIN_VERSION, COMPARE_PLUGIN_RESOLVED, version, versionMatchRule);
	}

	static IPluginModelBase getLatestPlugin(String id, String pluginLocation) {
		return getRequiredPlugin(id, null, IMatchRules.NONE, pluginLocation);
	}

	private static List<List<IPluginModelBase>> getPlugins(String id, String pluginLocation) {
		ModelEntry entry = PluginRegistry.findEntry(id);
		if (entry == null) {
			return Collections.emptyList();
		}
		List<IPluginModelBase> wsPlugins = List.of(entry.getWorkspaceModels()); // contains no or one element in most cases
		List<IPluginModelBase> tpPlugins = List.of(entry.getExternalModels()); // contains no or one element in most cases
		return isWorkspace(pluginLocation) ? List.of(wsPlugins, tpPlugins) : List.of(tpPlugins, wsPlugins);
	}

	/**
	 * Selects and returns an {@code included} element for the specified version from the given containers using the following logic:
	 * <p>
	 * <ol>
	 * <li>take first container</li>
	 * <li>if an exactly qualified matching version exists select that</li>
	 * <li>if an unqualified matching version exists select that</li>
	 * <li>if any version exists, select the latest one</li>
	 * <li>if no version was yet selected, go to next container and continue at step 2.</li>
	 * </ol>
	 * </p>
	 * @return the selected included element or null if none was found
	 */
	private static <E> E getIncluded(List<List<E>> containers, Predicate<E> filter, Function<E, String> getVersion, Comparator<E> primaryComparator, String version) {
		if (containers == null || containers.isEmpty()) {
			return null;
		}
		Version includedVersion = Version.parseVersion(version);

		Comparator<E> compareVersion = primaryComparator.thenComparing(e -> Version.parseVersion(getVersion.apply(e)), Comparator//
				.<Version, Boolean> comparing(includedVersion::equals) // false < true
				.thenComparing(v -> VersionUtil.compareMacroMinorMicro(v, includedVersion) == 0) // false < true
				.thenComparing(Comparator.naturalOrder()));

		return getMaxElement(containers, filter, compareVersion);
	}

	/**	
	 * Selects and returns an {@code required} element for the specified version (may be null) and match-rule from the given containers using the following logic:
	 * <p>
	 * <ol>
	 * <li>take first container</li>
	 * <li>filter-out versions that do not obey the match rule with respect to the required version</li>
	 * <li>selected and return latest version available</li>
	 * <li>if no version was yet selected, go to next container and continue at step 2.</li>
	 * </ol>
	 * </p>
	 * @return the selected required element or null if none was found
	 */
	private static <E> E getRequired(List<List<E>> containers, Predicate<E> filter, Function<E, String> getVersion, Comparator<E> primaryComparator, String version, int versionMatchRule) {
		if (containers == null || containers.isEmpty()) {
			return null;
		}
		if (version != null && !version.equals(Version.emptyVersion.toString())) {
			Predicate<E> matchingVersion = e -> VersionUtil.compare(getVersion.apply(e), version, versionMatchRule);
			filter = filter.and(matchingVersion);
		} // if no/empty version is specified take the most recent version from the first/preferred location

		Comparator<E> compareVersion = primaryComparator.thenComparing(e -> Version.parseVersion(getVersion.apply(e)));

		return getMaxElement(containers, filter, compareVersion);
	}

	private static <E> E getMaxElement(List<List<E>> containers, Predicate<E> filter, Comparator<E> comparator) {
		for (List<E> container : containers) {
			Optional<E> selection = container.stream().filter(filter).max(comparator);
			if (selection.isPresent()) { // take most recent element
				return selection.get();
			}
		}
		return null;
	}

	// --- plug-in based launches ---

	private static final BiPredicate<List<Version>, Version> CONTAINS_SAME_VERSION = List::contains;
	private static final BiPredicate<List<Version>, Version> CONTAINS_SAME_MMM_VERSION = (versions, toAdd) -> versions.stream().anyMatch(v -> VersionUtil.compareMacroMinorMicro(toAdd, v) == 0);

	private static Map<IPluginModelBase, String> getWorkspaceBundleMap(ILaunchConfiguration configuration, Map<String, List<Version>> idVersions) throws CoreException {
		Set<String> workspaceBundles = configuration.getAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES, emptySet());

		Map<IPluginModelBase, String> map = getBundleMap(workspaceBundles, ModelEntry::getWorkspaceModels, CONTAINS_SAME_VERSION, idVersions);

		if (configuration.getAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true)) {
			Set<String> deselectedWorkspaceBundles = configuration.getAttribute(IPDELauncherConstants.DESELECTED_WORKSPACE_BUNDLES, emptySet());
			Set<IPluginModelBase> deselectedPlugins = getBundleMap(deselectedWorkspaceBundles, ModelEntry::getWorkspaceModels, null, null).keySet();
			IPluginModelBase[] models = PluginRegistry.getWorkspaceModels();
			for (IPluginModelBase model : models) {
				if (model.getPluginBase().getId() != null && !deselectedPlugins.contains(model) && !map.containsKey(model)) {
					addPlugin(map, model, DEFAULT_START_LEVELS, idVersions, CONTAINS_SAME_VERSION);
				}
			}
		}
		return map;
	}

	private static Map<IPluginModelBase, String> getTargetBundleMap(ILaunchConfiguration configuration, Map<String, List<Version>> idVersions) throws CoreException {
		Set<String> targetBundles = configuration.getAttribute(IPDELauncherConstants.SELECTED_TARGET_BUNDLES, emptySet());
		return getBundleMap(targetBundles, ModelEntry::getExternalModels, CONTAINS_SAME_MMM_VERSION, idVersions); // don't add same major-minor-micro-version more than once
	}

	private static Map<IPluginModelBase, String> getBundleMap(Set<String> entries, Function<ModelEntry, IPluginModelBase[]> getModels, BiPredicate<List<Version>, Version> versionFilter, Map<String, List<Version>> idVersions) {
		Map<IPluginModelBase, String> map = new LinkedHashMap<>();
		for (String bundleEntry : entries) {
			int index = bundleEntry.indexOf(START_LEVEL_SEPARATOR);
			if (index < 0) { // if no start levels, assume default
				index = bundleEntry.length();
				bundleEntry += START_LEVEL_SEPARATOR + DEFAULT_START_LEVELS;
			}
			String idVersion = bundleEntry.substring(0, index);
			int versionIndex = idVersion.indexOf(VERSION_SEPARATOR);
			String id = (versionIndex > 0) ? idVersion.substring(0, versionIndex) : idVersion;
			String version = (versionIndex > 0) ? idVersion.substring(versionIndex + 1) : null;

			ModelEntry entry = PluginRegistry.findEntry(id);
			if (entry != null) {
				IPluginModelBase[] models = getModels.apply(entry);
				String startData = bundleEntry.substring(index + 1);
				for (IPluginModelBase model : getSelectedModels(models, version, versionFilter == null)) {
					addPlugin(map, model, startData, idVersions, versionFilter);
				}
			}
		}
		return map;
	}

	static final Comparator<IPluginModelBase> VERSION = comparing(BundleLauncherHelper::getVersion);

	private static Iterable<IPluginModelBase> getSelectedModels(IPluginModelBase[] models, String version, boolean greedy) {
		// match only if...
		// a) if we have the same version
		// b) no version (if greedy take latest, else take all)
		// c) all else fails, if there's just one bundle available, use it
		Stream<IPluginModelBase> selectedModels = Arrays.stream(models).filter(IPluginModelBase::isEnabled); // workspace models are always enabled, external might be disabled
		if (version == null) {
			if (!greedy) {
				selectedModels = selectedModels.max(VERSION).stream(); // take only latest
			} // Otherwise be greedy and take all if versionFilter is null
		} else {
			selectedModels = selectedModels.filter(m -> m.getPluginBase().getVersion().equals(version) || models.length == 1);
		}
		return selectedModels::iterator;
	}

	private static void addPlugin(Map<IPluginModelBase, String> map, IPluginModelBase model, String startData, Map<String, List<Version>> idVersions, BiPredicate<List<Version>, Version> containsVersion) {
		if (containsVersion == null) { // be greedy and just take all (idVersions is null as well)
			addBundleToMap(map, model, startData);
		} else {
			List<Version> pluginVersions = idVersions.computeIfAbsent(model.getPluginBase().getId(), n -> new ArrayList<>());
			Version version = getVersion(model);
			if (!containsVersion.test(pluginVersions, version)) { // apply version filter    
				pluginVersions.add(version);
				addBundleToMap(map, model, startData);
			}
		}
	}

	private static Version getVersion(IPluginModelBase model) {
		BundleDescription bundleDescription = model.getBundleDescription();
		if (bundleDescription == null) {
			try {
				return Version.parseVersion(model.getPluginBase().getVersion());
			} catch (IllegalArgumentException e) {
				return Version.emptyVersion;
			}
		}
		return bundleDescription.getVersion();
	}

	/**
	 * Adds the given bundle and start information to the map.  This will override anything set
	 * for system bundles, and set their start level to the appropriate level
	 * @param map The map to add the bundles too
	 * @param bundle The bundle to add
	 * @param substring the start information in the form level:autostart
	 */
	private static void addBundleToMap(Map<IPluginModelBase, String> map, IPluginModelBase bundle, String startData) {
		BundleDescription desc = bundle.getBundleDescription();
		boolean defaultsl = startData == null || startData.equals(DEFAULT_START_LEVELS);
		if (desc != null && defaultsl) {
			startData = getStartData(desc, startData);
		}
		map.put(bundle, startData);
	}

	public static String getStartData(BundleDescription desc, String defaultStartData) {
		String runLevel = resolveSystemRunLevelText(desc);
		String auto = resolveSystemAutoText(desc);
		return runLevel != null && auto != null ? (runLevel + AUTO_START_SEPARATOR + auto) : defaultStartData;
	}

	public static void addDefaultStartingBundle(Map<IPluginModelBase, String> map, IPluginModelBase bundle) {
		addBundleToMap(map, bundle, DEFAULT_START_LEVELS);
	}

	private static final Map<String, String> AUTO_STARTED_BUNDLE_LEVELS = Map.ofEntries( //
			Map.entry(IPDEBuildConstants.BUNDLE_DS, "1"), //$NON-NLS-1$
			Map.entry(IPDEBuildConstants.BUNDLE_SIMPLE_CONFIGURATOR, "1"), //$NON-NLS-1$
			Map.entry(IPDEBuildConstants.BUNDLE_EQUINOX_COMMON, "2"), //$NON-NLS-1$
			Map.entry(IPDEBuildConstants.BUNDLE_OSGI, "1"), //$NON-NLS-1$
			Map.entry(IPDEBuildConstants.BUNDLE_CORE_RUNTIME, DEFAULT), //
			Map.entry(IPDEBuildConstants.BUNDLE_FELIX_SCR, "1")); //$NON-NLS-1$

	public static String resolveSystemRunLevelText(BundleDescription description) {
		return AUTO_STARTED_BUNDLE_LEVELS.get(description.getSymbolicName());
	}

	public static String resolveSystemAutoText(BundleDescription description) {
		return AUTO_STARTED_BUNDLE_LEVELS.containsKey(description.getSymbolicName()) ? "true" : null; //$NON-NLS-1$
	}

	public static String formatBundleEntry(IPluginModelBase model, String startLevel, String autoStart) {
		IPluginBase base = model.getPluginBase();
		String id = base.getId();
		StringBuilder buffer = new StringBuilder(id);

		ModelEntry entry = PluginRegistry.findEntry(id);
		if (entry != null) {
			boolean isWorkspacePlugin = model.getUnderlyingResource() != null;
			IPluginModelBase[] entryModels = isWorkspacePlugin ? entry.getWorkspaceModels() : entry.getExternalModels();
			if (entryModels.length > 1) {
				buffer.append(VERSION_SEPARATOR);
				buffer.append(model.getPluginBase().getVersion());
			}
		}

		boolean hasStartLevel = startLevel != null && !startLevel.isEmpty();
		boolean hasAutoStart = autoStart != null && !autoStart.isEmpty();

		if (hasStartLevel || hasAutoStart) {
			buffer.append(START_LEVEL_SEPARATOR);
			if (hasStartLevel) {
				buffer.append(startLevel);
			}
			buffer.append(AUTO_START_SEPARATOR);
			if (hasAutoStart) {
				buffer.append(autoStart);
			}
		}
		return buffer.toString();
	}

	public static String formatFeatureEntry(String featureId, String pluginResolution) {
		return featureId + FEATURE_PLUGIN_RESOLUTION_SEPARATOR + pluginResolution;
	}

	@SuppressWarnings("deprecation")
	public static void migrateLaunchConfiguration(ILaunchConfigurationWorkingCopy configuration) throws CoreException {

		String value = configuration.getAttribute("wsproject", (String) null); //$NON-NLS-1$
		if (value != null) {
			configuration.setAttribute("wsproject", (String) null); //$NON-NLS-1$
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
			configuration.setAttribute(attr, value);
		}

		String value2 = configuration.getAttribute("extplugins", (String) null); //$NON-NLS-1$
		if (value2 != null) {
			configuration.setAttribute("extplugins", (String) null); //$NON-NLS-1$
			if (value2.indexOf(';') != -1) {
				value2 = value2.replace(';', ',');
			} else if (value2.indexOf(':') != -1) {
				value2 = value2.replace(':', ',');
			}
			value2 = (value2.length() == 0 || value2.equals(",")) ? null : value2.substring(0, value2.length() - 1); //$NON-NLS-1$
			configuration.setAttribute(IPDELauncherConstants.SELECTED_TARGET_PLUGINS, value2);
		}

		convertToSet(configuration, IPDELauncherConstants.SELECTED_TARGET_PLUGINS, IPDELauncherConstants.SELECTED_TARGET_BUNDLES);
		convertToSet(configuration, IPDELauncherConstants.SELECTED_WORKSPACE_PLUGINS, IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES);
		convertToSet(configuration, IPDELauncherConstants.DESELECTED_WORKSPACE_PLUGINS, IPDELauncherConstants.DESELECTED_WORKSPACE_BUNDLES);

		String version = configuration.getAttribute(IPDEConstants.LAUNCHER_PDE_VERSION, (String) null);
		boolean newApp = TargetPlatformHelper.usesNewApplicationModel();
		boolean upgrade = !"3.3".equals(version) && newApp; //$NON-NLS-1$
		if (!upgrade) {
			upgrade = TargetPlatformHelper.getTargetVersion() >= 3.2 && version == null;
		}
		if (upgrade) {
			configuration.setAttribute(IPDEConstants.LAUNCHER_PDE_VERSION, newApp ? "3.3" : "3.2a"); //$NON-NLS-1$ //$NON-NLS-2$
			boolean usedefault = configuration.getAttribute(IPDELauncherConstants.USE_DEFAULT, true);
			boolean automaticAdd = configuration.getAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true);
			if (!usedefault) {
				ArrayList<String> list = new ArrayList<>();
				if (version == null) {
					list.add("org.eclipse.core.contenttype"); //$NON-NLS-1$
					list.add("org.eclipse.core.jobs"); //$NON-NLS-1$
					list.add(IPDEBuildConstants.BUNDLE_EQUINOX_COMMON);
					list.add("org.eclipse.equinox.preferences"); //$NON-NLS-1$
					list.add("org.eclipse.equinox.registry"); //$NON-NLS-1$
				}
				if (!"3.3".equals(version) && newApp) { //$NON-NLS-1$
					list.add("org.eclipse.equinox.app"); //$NON-NLS-1$
				}
				Set<String> extensions = new LinkedHashSet<>(configuration.getAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES, emptySet()));
				Set<String> target = new LinkedHashSet<>(configuration.getAttribute(IPDELauncherConstants.SELECTED_TARGET_BUNDLES, emptySet()));
				for (String plugin : list) {
					IPluginModelBase model = PluginRegistry.findModel(plugin);
					if (model == null) {
						continue;
					}
					if (model.getUnderlyingResource() != null) {
						if (!automaticAdd) {
							extensions.add(plugin);
						}
					} else {
						target.add(plugin);
					}
				}
				if (!extensions.isEmpty()) {
					configuration.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES, extensions);
				}
				if (!target.isEmpty()) {
					configuration.setAttribute(IPDELauncherConstants.SELECTED_TARGET_BUNDLES, target);
				}
			}
		}
	}

	private static ILaunchConfigurationWorkingCopy getWorkingCopy(ILaunchConfiguration configuration) throws CoreException {
		if (configuration.isWorkingCopy()) {
			return (ILaunchConfigurationWorkingCopy) configuration;
		}
		return configuration.getWorkingCopy();
	}

	@SuppressWarnings("deprecation")
	public static void migrateOsgiLaunchConfiguration(ILaunchConfigurationWorkingCopy configuration) throws CoreException {
		convertToSet(configuration, IPDELauncherConstants.WORKSPACE_BUNDLES, IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES);
		convertToSet(configuration, IPDELauncherConstants.TARGET_BUNDLES, IPDELauncherConstants.SELECTED_TARGET_BUNDLES);
		convertToSet(configuration, IPDELauncherConstants.DESELECTED_WORKSPACE_PLUGINS, IPDELauncherConstants.DESELECTED_WORKSPACE_BUNDLES);
	}

	private static void convertToSet(ILaunchConfigurationWorkingCopy wc, String stringAttribute, String listAttribute) throws CoreException {
		String value = wc.getAttribute(stringAttribute, (String) null);
		if (value != null) {
			wc.removeAttribute(stringAttribute);
			String[] itemArray = value.split(","); //$NON-NLS-1$
			Set<String> itemSet = new HashSet<>(Arrays.asList(itemArray));
			wc.setAttribute(listAttribute, itemSet);
		}
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
	public static Map<IPluginModelBase, AdditionalPluginData> getAdditionalPlugins(ILaunchConfiguration config, boolean onlyEnabled) throws CoreException {
		Map<IPluginModelBase, AdditionalPluginData> resolvedAdditionalPlugins = new HashMap<>();
		Set<String> userAddedPlugins = config.getAttribute(IPDELauncherConstants.ADDITIONAL_PLUGINS, emptySet());
		String defaultPluginResolution = config.getAttribute(IPDELauncherConstants.FEATURE_PLUGIN_RESOLUTION, IPDELauncherConstants.LOCATION_WORKSPACE);

		for (String addedPlugin : userAddedPlugins) {
			String[] pluginData = addedPlugin.split(FEATURES_ADDITIONAL_PLUGINS_DATA_SEPARATOR);
			boolean checked = Boolean.parseBoolean(pluginData[3]);
			if (!onlyEnabled || checked) {
				String id = pluginData[0];
				String version = pluginData[1];
				String pluginResolution = pluginData[2];
				if (IPDELauncherConstants.LOCATION_DEFAULT.equalsIgnoreCase(pluginResolution)) {
					pluginResolution = defaultPluginResolution;
				}

				IPluginModelBase model = getIncludedPlugin(id, version, pluginResolution);
				if (model != null) {
					String startLevel = (pluginData.length >= 6) ? pluginData[4] : null;
					String autoStart = (pluginData.length >= 6) ? pluginData[5] : null;
					AdditionalPluginData additionalPluginData = new AdditionalPluginData(pluginData[2], checked, startLevel, autoStart);
					resolvedAdditionalPlugins.put(model, additionalPluginData);
				}
			}
		}
		return resolvedAdditionalPlugins;
	}

	public static String formatAdditionalPluginEntry(IPluginModelBase pluginModel, String pluginResolution, boolean isChecked, String fStartLevel, String fAutoStart) {
		IPluginBase plugin = pluginModel.getPluginBase();
		return String.join(FEATURES_ADDITIONAL_PLUGINS_DATA_SEPARATOR, plugin.getId(), plugin.getVersion(), pluginResolution, String.valueOf(isChecked), fStartLevel, fAutoStart);
	}

	public static class AdditionalPluginData {
		public final String fResolution;
		public final boolean fEnabled;
		public final String fStartLevel;
		public final String fAutoStart;

		public AdditionalPluginData(String resolution, boolean enabled, String startLevel, String autoStart) {
			fResolution = resolution;
			fEnabled = enabled;
			fStartLevel = (startLevel == null || startLevel.isEmpty()) ? DEFAULT : startLevel;
			fAutoStart = (autoStart == null || autoStart.isEmpty()) ? DEFAULT : autoStart;
		}

		String startLevels() {
			return fStartLevel + AUTO_START_SEPARATOR + fAutoStart;
		}
	}
}
