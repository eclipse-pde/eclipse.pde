/*******************************************************************************
 * Copyright (c) 2007, 2021 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.internal.p2.publisher.eclipse.IProductDescriptor;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductContentType;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.engine.IEngine;
import org.eclipse.equinox.p2.engine.IPhaseSet;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.engine.PhaseSetFactory;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.ILicense;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.ITouchpointType;
import org.eclipse.equinox.p2.metadata.IUpdateDescriptor;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.planner.ProfileInclusionRules;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.p2.publisher.eclipse.ProductAction;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.simpleconfigurator.manipulator.SimpleConfiguratorManipulator;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.pde.core.plugin.IMatchRules;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.build.BundleHelper;
import org.eclipse.pde.internal.core.ifeature.IEnvironment;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureChild;
import org.eclipse.pde.internal.core.ifeature.IFeatureImport;
import org.eclipse.pde.internal.core.ifeature.IFeatureInfo;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.core.plugin.PluginBase;
import org.osgi.framework.Constants;

/**
 * Utilities to read and write p2 files
 *
 * @since 3.4
 */
@SuppressWarnings("restriction")
public class P2Utils {

	public static final String P2_FLAVOR_DEFAULT = "tooling"; //$NON-NLS-1$

	public static final ITouchpointType TOUCHPOINT_OSGI = MetadataFactory.createTouchpointType("org.eclipse.equinox.p2.osgi", Version.createOSGi(1, 0, 0)); //$NON-NLS-1$
	private static final String CAPABILITY_NS_OSGI_BUNDLE = "osgi.bundle"; //$NON-NLS-1$
	private static final String CAPABILITY_NS_OSGI_FRAGMENT = "osgi.fragment"; //$NON-NLS-1$
	public static final String TYPE_ECLIPSE_BUNDLE = "bundle"; //$NON-NLS-1$
	public static final String NAMESPACE_ECLIPSE_TYPE = "org.eclipse.equinox.p2.eclipse.type"; //$NON-NLS-1$
	public static final IProvidedCapability BUNDLE_CAPABILITY = MetadataFactory.createProvidedCapability(NAMESPACE_ECLIPSE_TYPE, TYPE_ECLIPSE_BUNDLE, Version.createOSGi(1, 0, 0));
	public static final String CAPABILITY_NS_JAVA_PACKAGE = "java.package"; //$NON-NLS-1$

	/**
	 * Returns bundles defined by the 'bundles.info' relative to the given
	 * home and configuration area, or <code>null</code> if none.
	 * The "bundles.info" file is assumed to be at a fixed location relative to the
	 * configuration area URL.
	 *
	 * @param platformHome absolute path in the local file system to an installation
	 * @param configurationArea url location of the configuration directory to search
	 *  for bundles.info
	 * @return all bundles in the installation or <code>null</code> if not able
	 * 	to locate a bundles.info
	 */
	public static BundleInfo[] readBundles(String platformHome, File configurationArea) {
		IPath basePath = IPath.fromOSString(platformHome);
		if (configurationArea == null) {
			return null;
		}
		try {
			File bundlesTxt = new File(configurationArea, SimpleConfiguratorManipulator.BUNDLES_INFO_PATH);
			File home = basePath.toFile();
			BundleInfo[] bundles = getBundlesFromFile(bundlesTxt, home);
			if (bundles == null || bundles.length == 0) {
				return null;
			}
			return bundles;
		} catch (IOException e) {
			PDECore.log(e);
			return null;
		}
	}

	/**
	 * Returns source bundles defined by the 'source.info' file in the
	 * specified location, or <code>null</code> if none. The "source.info" file
	 * is assumed to be at a fixed location relative to the configuration area URL.
	 *
	 * @param platformHome absolute path in the local file system to an installation
	 * @param configurationArea url location of the configuration directory to search for bundles.info and source.info
	 * @return all source bundles in the installation or <code>null</code> if not able
	 * 	to locate a source.info
	 */
	public static BundleInfo[] readSourceBundles(String platformHome, File configurationArea) {
		IPath basePath = IPath.fromOSString(platformHome);
		if (configurationArea == null) {
			return null;
		}
		try {
			File home = basePath.toFile();
			File srcBundlesTxt = new File(configurationArea, SimpleConfiguratorManipulator.SOURCE_INFO_PATH);
			BundleInfo[] srcBundles = getBundlesFromFile(srcBundlesTxt, home);
			if (srcBundles == null || srcBundles.length == 0) {
				return null;
			}
			return srcBundles;
		} catch (IOException e) {
			PDECore.log(e);
			return null;
		}
	}

	/**
	 * Returns a list of {@link BundleInfo} for each bundle entry or
	 * <code>null</code> if there is a problem reading the file.
	 *
	 * @param filePath
	 *            the file to read
	 * @param home
	 *            the path describing the base location of the platform install
	 * @return list containing URL locations or <code>null</code>
	 */
	private static BundleInfo[] getBundlesFromFile(File filePath, File home) throws IOException {
		SimpleConfiguratorManipulator manipulator = PDECore.getDefault()
				.acquireService(SimpleConfiguratorManipulator.class);
		if (manipulator == null) {
			return null;
		}
		// the input stream will be buffered for us
		try (InputStream in = Files.newInputStream(filePath.toPath())) {
			return manipulator.loadConfiguration(in, home.toURI());
		} catch (NoSuchFileException e) {
			return null;
		}
	}

	/**
	 * Creates a bundles.info file in the given directory containing the name,
	 * version, location, start level and expected state of the bundles in the
	 * launch.  Will also create a source.info containing all of the
	 * source bundles in the launch. The map of bundles must be of the form
	 * IModelPluginBase to a String ("StartLevel:AutoStart").  Returns the
	 * URL location of the bundle.info or <code>null</code> if there was a
	 * problem creating it.
	 *
	 * @param bundles map containing all bundles to write to the bundles.info, maps IPluginModelBase to String ("StartLevel:AutoStart")
	 * @param defaultStartLevel start level to use when "default" is the start level
	 * @param defaultAutoStart auto start setting to use when "default" is the auto start setting
	 * @param directory configuration directory to create the files in
	 * @param osgiBundleList a list of bundles coming from a template config.ini
	 * @return URL location of the bundles.info or <code>null</code>
	 */
	public static URL writeBundlesTxt(Map<IPluginModelBase, String> bundles, int defaultStartLevel,
			boolean defaultAutoStart, File directory, String osgiBundleList) {
		if (bundles.isEmpty()) {
			return null;
		}

		// Parse the osgi bundle list for start levels
		Map<String, String> osgiStartLevels = new HashMap<>();
		if (osgiBundleList != null) {
			StringTokenizer tokenizer = new StringTokenizer(osgiBundleList, ","); //$NON-NLS-1$
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken();
				int index = token.indexOf('@');
				if (index != -1) {
					String modelName = token.substring(0, index);
					String startData = token.substring(index + 1);
					index = startData.indexOf(':');
					String level = index > 0 ? startData.substring(0, index) : "default"; //$NON-NLS-1$
					String auto = index > 0 && index < startData.length() - 1 ? startData.substring(index + 1) : "default"; //$NON-NLS-1$
					if ("start".equals(auto)) { //$NON-NLS-1$
						auto = "true"; //$NON-NLS-1$
					}
					osgiStartLevels.put(modelName, level + ':' + auto);
				}
			}
		}

		List<BundleInfo> bundleInfo = new ArrayList<>(bundles.size());
		List<BundleInfo> sourceInfo = new ArrayList<>(bundles.size());
		for (final IPluginModelBase currentModel : bundles.keySet()) {
			IPluginBase base = currentModel.getPluginBase();

			BundleInfo info = new BundleInfo();
			String installLocation = currentModel.getInstallLocation();
			if (installLocation != null) {
				info.setLocation(new File(installLocation).toURI());
				if (base instanceof PluginBase && ((PluginBase) base).getBundleSourceEntry() != null) {
					info.setSymbolicName(base.getId());
					info.setVersion(base.getVersion());
					info.setStartLevel(-1);
					info.setMarkedAsStarted(false);
					sourceInfo.add(info);
				} else if (base != null) {
					info.setSymbolicName(base.getId());
					info.setVersion(base.getVersion());
					String currentLevel = bundles.get(currentModel);
					// override the start level setting if something comes from the config.ini
					if (osgiStartLevels.containsKey(base.getId())) {
						currentLevel = osgiStartLevels.get(base.getId());
					}
					int index = currentLevel.indexOf(':');
					String levelString = index > 0 ? currentLevel.substring(0, index) : "default"; //$NON-NLS-1$
					String auto = index > 0 && index < currentLevel.length() - 1 ? currentLevel.substring(index + 1) : "default"; //$NON-NLS-1$
					boolean isAuto = true;
					int level = -1;
					if ("default".equals(auto)) {//$NON-NLS-1$
						isAuto = defaultAutoStart;
					} else {
						isAuto = Boolean.parseBoolean(auto);
					}
					if ("default".equals(levelString)) {//$NON-NLS-1$
						level = defaultStartLevel;
					} else {
						try {
							level = Integer.parseInt(levelString);
						} catch (NumberFormatException e) {
							PDECore.log(Status.error(
									"Error writing bundles, could not parse start level for bundle " + currentModel)); //$NON-NLS-1$
						}
					}
					info.setMarkedAsStarted(isAuto);
					info.setStartLevel(level);
					bundleInfo.add(info);
				}
			} else {
				PDECore.log(Status
						.error("Error writing bundles, could not find the bundle location for bundle " + currentModel)); //$NON-NLS-1$
			}
		}

		File bundlesTxt = new File(directory, SimpleConfiguratorManipulator.BUNDLES_INFO_PATH);
		File srcBundlesTxt = new File(directory, SimpleConfiguratorManipulator.SOURCE_INFO_PATH);

		BundleInfo[] infos = bundleInfo.toArray(new BundleInfo[bundleInfo.size()]);
		BundleInfo[] sources = sourceInfo.toArray(new BundleInfo[sourceInfo.size()]);

		SimpleConfiguratorManipulator manipulator = BundleHelper.getDefault()
				.acquireService(SimpleConfiguratorManipulator.class);
		try {
			manipulator.saveConfiguration(infos, bundlesTxt, null);
			manipulator.saveConfiguration(sources, srcBundlesTxt, null);
		} catch (IOException e) {
			PDECore.logException(e);
			return null;
		}

		if (!bundlesTxt.exists()) {
			return null;
		}
		try {
			return bundlesTxt.toURL();
		} catch (MalformedURLException e) {
			PDECore.logException(e);
			return null;
		}
	}

	/**
	 * Returns whether a profile with the given ID exists in a profile registry
	 * stored in the give p2 data area.
	 *
	 * @param profileID id of the profile to check
	 * @param p2DataArea data area where the profile registry is
	 * @return whether the profile exists
	 */
	public static boolean profileExists(String profileID, File p2DataArea) throws CoreException {
		IProvisioningAgentProvider provider = PDECore.getDefault().acquireService(IProvisioningAgentProvider.class);
		if (provider == null) {
			throw new CoreException(Status.error(PDECoreMessages.P2Utils_UnableToAcquireP2Service));
		}

		IProvisioningAgent agent = provider.createAgent(p2DataArea.toURI());
		if (agent == null) {
			throw new CoreException(Status.error(PDECoreMessages.P2Utils_UnableToAcquireP2Service));
		}

		IProfileRegistry registry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
		if (registry == null) {
			throw new CoreException(Status.error(PDECoreMessages.P2Utils_UnableToAcquireP2Service));
		}

		return registry.containsProfile(profileID);
	}

	/**
	 * Generates a profile containing metadata for all of the bundles in the
	 * provided collection. The profile will have the given profile ID and will
	 * be persisted in the profile registry directory inside the given p2 data
	 * area.
	 *
	 * @param profileID
	 *            the ID to be used when creating the profile, if a profile with
	 *            the same name exists, it will be overwritten
	 * @param p2DataArea
	 *            the directory which contains p2 data including the profile
	 *            registry, if the directory path doesn't exist it will be
	 *            created
	 * @param bundles
	 *            the collection of IPluginModelBase objects representing
	 *            bundles to create metadata for and add to the profile
	 *
	 * @throws CoreException
	 *             if the profile cannot be generated
	 */
	public static void createProfile(String profileID, File p2DataArea, Collection<List<IPluginModelBase>> bundles)
			throws CoreException {
		createProfile(profileID, p2DataArea, bundles, null, null);
	}

	/**
	 * Generates a profile containing metadata for all of the bundles in the
	 * provided collection. The profile will have the given profile ID and will
	 * be persisted in the profile registry directory inside the given p2 data
	 * area.
	 *
	 * @param profileID
	 *            the ID to be used when creating the profile, if a profile with
	 *            the same name exists, it will be overwritten
	 * @param p2DataArea
	 *            the directory which contains p2 data including the profile
	 *            registry, if the directory path doesn't exist it will be
	 *            created
	 * @param bundles
	 *            the collection of IPluginModelBase objects representing
	 *            bundles to create metadata for and add to the profile
	 * @param featureMap
	 *            the map of IFeature objects representing features to create
	 *            metadata for and add to the profile, if the mapping is true it
	 *            is assumed a root feature
	 *
	 * @throws CoreException
	 *             if the profile cannot be generated
	 */
	public static void createProfile(String profileID, File p2DataArea, Collection<List<IPluginModelBase>> bundles,
			Map<IFeature, Boolean> featureMap, ProductInfo productInfo) throws CoreException {
		// Acquire the required p2 services, creating an agent in the target p2 metadata area
		IProvisioningAgentProvider provider = PDECore.getDefault().acquireService(IProvisioningAgentProvider.class);
		if (provider == null) {
			throw new CoreException(Status.error(PDECoreMessages.P2Utils_UnableToAcquireP2Service));
		}

		IProvisioningAgent agent = provider.createAgent(p2DataArea.toURI());
		if (agent == null) {
			throw new CoreException(Status.error(PDECoreMessages.P2Utils_UnableToAcquireP2Service));
		}

		IProfileRegistry registry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
		if (registry == null) {
			throw new CoreException(Status.error(PDECoreMessages.P2Utils_UnableToAcquireP2Service));
		}

		IEngine engine = (IEngine) agent.getService(IEngine.SERVICE_NAME);
		if (engine == null) {
			throw new CoreException(Status.error(PDECoreMessages.P2Utils_UnableToAcquireP2Service));
		}

		// Delete any previous profiles with the same ID
		registry.removeProfile(profileID);

		Map<String, String> props = new HashMap<>();
//		props.setProperty(IProfile.PROP_INSTALL_FOLDER, registryArea.getAbsolutePath());
		props.put(IProfile.PROP_INSTALL_FEATURES, Boolean.TRUE.toString());
		// Set up environment and nationalization properties so OS specific fragments are installed
		props.put(IProfile.PROP_ENVIRONMENTS, generateEnvironmentProperties());
		props.put(IProfile.PROP_NL, TargetPlatform.getNL());

		// Create the profile
		IProfile profile = registry.addProfile(profileID, props);

		// Create metadata for the bundles
		Collection<IInstallableUnit> ius = bundles.stream().flatMap(Collection::stream)
				.map(IPluginModelBase::getBundleDescription).map(P2Utils::createBundleIU) //
				.collect(Collectors.toList());

		// Add the metadata to the profile
		ProvisioningContext context = new ProvisioningContext(agent);
		IProvisioningPlan plan = engine.createPlan(profile, context);
		for (final IInstallableUnit iu : ius) {
			plan.addInstallableUnit(iu);
			plan.setInstallableUnitProfileProperty(iu, "org.eclipse.equinox.p2.internal.inclusion.rules", ProfileInclusionRules.createOptionalInclusionRule(iu)); //$NON-NLS-1$
		}
		if (featureMap != null && !featureMap.isEmpty()) {
			Map<String, List<IPluginBase>> plugins = bundles.stream().flatMap(Collection::stream)
					.map(IPluginModelBase::getPluginBase)
					.collect(Collectors.groupingBy(m -> m.getPluginBase().getId()));
			Map<String, List<IFeature>> features = featureMap.keySet().stream()
					.collect(Collectors.groupingBy(IFeature::getId));
			for (Entry<IFeature, Boolean> featureEntry : featureMap.entrySet()) {
				createFeatureIUs(featureEntry.getKey(), featureEntry.getValue(), plan, plugins, features);
			}
		}
		if (productInfo != null) {
			ProductAction productAction = new ProductAction(null, new IProductDescriptor() {

				@Override
				public boolean useFeatures() {
					return false;
				}

				@Override
				public boolean includeLaunchers() {
					return false;
				}

				@Override
				public boolean hasFeatures() {
					return false;
				}

				@Override
				public boolean hasBundles() {
					return false;
				}

				@Override
				public String getVersion() {
					return productInfo.version();
				}

				@Override
				public String getVMArguments(String os, String arch) {
					return null;
				}

				@Override
				public String getVMArguments(String os) {
					return null;
				}

				@Override
				public String getVM(String os) {
					return null;
				}

				@Override
				public String getSplashLocation() {
					return null;
				}

				@Override
				public List<IRepositoryReference> getRepositoryEntries() {
					return List.of();
				}

				@Override
				public String getProgramArguments(String os, String arch) {
					return null;
				}

				@Override
				public String getProgramArguments(String os) {
					return null;
				}

				@Override
				public String getProductName() {
					return productInfo.name();
				}

				@Override
				public String getProductId() {
					return null;
				}

				@Override
				public ProductContentType getProductContentType() {
					return ProductContentType.MIXED;
				}

				@Override
				public File getLocation() {
					return null;
				}

				@Override
				public String getLicenseURL() {
					return null;
				}

				@Override
				public String getLicenseText() {
					return null;
				}

				@Override
				public String getLauncherName() {
					return null;
				}

				@Override
				public String getId() {
					return productInfo.id();
				}

				@Override
				public String[] getIcons(String os) {
					return null;
				}

				@Override
				public List<IVersionedId> getFeatures(int options) {
					return List.of();
				}

				@Override
				public List<IVersionedId> getFeatures() {
					return List.of();
				}

				@Override
				public Map<String, String> getConfigurationProperties(String os, String arch) {
					return null;
				}

				@Override
				public Map<String, String> getConfigurationProperties() {
					return null;
				}

				@Override
				public String getConfigIniPath(String os) {
					return null;
				}

				@Override
				public List<IVersionedId> getBundles() {
					return List.of();
				}

				@Override
				public List<BundleInfo> getBundleInfos() {
					return List.of();
				}

				@Override
				public String getApplication() {
					return null;
				}
			}, P2_FLAVOR_DEFAULT, null);
			PublisherResult results = new PublisherResult();
			productAction.perform(new PublisherInfo(), results, null);
			results.query(QueryUtil.ALL_UNITS, null).forEach(plan::addInstallableUnit);
		}
		IPhaseSet phaseSet = PhaseSetFactory.createDefaultPhaseSetExcluding(new String[] {PhaseSetFactory.PHASE_CHECK_TRUST, PhaseSetFactory.PHASE_COLLECT, PhaseSetFactory.PHASE_CONFIGURE, PhaseSetFactory.PHASE_UNCONFIGURE, PhaseSetFactory.PHASE_UNINSTALL});
		IStatus status = engine.perform(plan, phaseSet, new NullProgressMonitor());

		if (!status.isOK() && status.getSeverity() != IStatus.CANCEL) {
			throw new CoreException(status);
		}

	}

	/**
	 * Generates the environment properties string for the self hosting p2 profile by looking up the current target platform properties.
	 *
	 * @return environment properties string
	 */
	private static String generateEnvironmentProperties() {
		StringBuilder env = new StringBuilder();
		env.append("osgi.ws="); //$NON-NLS-1$
		env.append(TargetPlatform.getWS());
		env.append(","); //$NON-NLS-1$
		env.append("osgi.os="); //$NON-NLS-1$
		env.append(TargetPlatform.getOS());
		env.append(","); //$NON-NLS-1$
		env.append("osgi.arch="); //$NON-NLS-1$
		env.append(TargetPlatform.getOSArch());
		return env.toString();
	}

	private static void createFeatureIUs(IFeature feature, boolean root, IProvisioningPlan plan,
			Map<String, List<IPluginBase>> plugins, Map<String, List<IFeature>> features) {
		// see
		// org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction.createGroupIU(Feature,
		// List<IInstallableUnit>, IPublisherInfo)
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setId(getGroupId(feature.getId()));
		Version version = Version.parseVersion(feature.getVersion());
		iu.setVersion(version);
		iu.setProperty(IInstallableUnit.PROP_NAME, feature.getLabel());
		IFeatureInfo description = feature.getFeatureInfo(IFeature.INFO_DESCRIPTION);
		if (description != null) {
			setProperty(iu, IInstallableUnit.PROP_DESCRIPTION, description.getDescription());
			setProperty(iu, IInstallableUnit.PROP_DESCRIPTION_URL, description.getURL());
		}
		setProperty(iu, IInstallableUnit.PROP_PROVIDER, feature.getProviderName());
		IFeatureInfo license = feature.getFeatureInfo(IFeature.INFO_LICENSE);
		if (license != null) {
			iu.setLicenses(new ILicense[] {
					MetadataFactory.createLicense(toURIOrNull(license.getURL()), license.getDescription()) });
		}
		IFeatureInfo copyright = feature.getFeatureInfo(IFeature.INFO_COPYRIGHT);
		if (copyright != null) {
			iu.setCopyright(
					MetadataFactory.createCopyright(toURIOrNull(copyright.getURL()), copyright.getDescription()));
		}
		iu.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor(iu.getId(),
				new VersionRange(Version.emptyVersion, true, iu.getVersion(), false), IUpdateDescriptor.NORMAL, null));
		List<IRequirement> required = new ArrayList<>();
		for (IFeatureChild includedFeature : feature.getIncludedFeatures()) {
			Version v = Version.parseVersion(includedFeature.getVersion());
			VersionRange range;
			if (Version.emptyVersion.equals(v)) {
				range = features.getOrDefault(includedFeature.getId(), List.of()).stream()
						.max(Comparator.comparing(IFeature::getVersion))
						.map(IFeature::getVersion).map(Version::parseVersion)
						.map(P2Utils::strictVersionRange).orElse(VersionRange.emptyRange);
			} else {
				range = strictVersionRange(version);
			}
			required.add(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID,
					getGroupId(includedFeature.getId()),
					range, getFilter(includedFeature.getFilter(), false, includedFeature), includedFeature.isOptional(),
					false));
		}
		for (IFeaturePlugin plugin : feature.getPlugins()) {
			Version v = Version.parseVersion(plugin.getVersion());
			VersionRange range;
			if (Version.emptyVersion.equals(v)) {
				range = plugins.getOrDefault(plugin.getId(), List.of()).stream()
						.max(Comparator.comparing(IPluginBase::getVersion))
						.map(IPluginBase::getVersion).map(Version::parseVersion).map(P2Utils::strictVersionRange)
						.orElse(VersionRange.emptyRange);
			} else {
				range = strictVersionRange(version);
			}
			required.add(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, plugin.getId(), range,
					getFilter(plugin.getFilter(), false, plugin), false,
					false));
		}
		for (IFeatureImport fimport : feature.getImports()) {
			VersionRange range = getRangeForImport(fimport);
			if (fimport.getType() == IFeatureImport.FEATURE) {
				required.add(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID,
						getGroupId(fimport.getId()), range, getFilter(fimport.getFilter(), false, null), false, false));
			} else if (fimport.getType() == IFeatureImport.PLUGIN) {
				required.add(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, fimport.getId(), range,
						getFilter(fimport.getFilter(), false, null), false, false));
			}
		}

		iu.setRequirements(required.toArray(IRequirement[]::new));
		iu.setTouchpointType(ITouchpointType.NONE);
		iu.setProperty(InstallableUnitDescription.PROP_TYPE_GROUP, Boolean.TRUE.toString());
		iu.setFilter(getFilter(null, false, feature));
		List<IProvidedCapability> providedCapabilities = new ArrayList<>();
		providedCapabilities.add(
				MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, iu.getId(),
						iu.getVersion()));
		iu.setCapabilities(providedCapabilities.toArray(IProvidedCapability[]::new));
		IInstallableUnit unit = MetadataFactory.createInstallableUnit(iu);
		plan.addInstallableUnit(unit);
		if (root) {
			plan.setInstallableUnitProfileProperty(unit, IProfile.PROP_PROFILE_ROOT_IU, Boolean.TRUE.toString());
		}
	}

	public static VersionRange getRangeForImport(IFeatureImport featureImport) {
		String version = featureImport.getVersion();
		if (version != null && !version.isEmpty()) {
			try {
				org.osgi.framework.Version osgi = org.osgi.framework.Version.parseVersion(version);
				Version minVersion = Version.parseVersion(version);
				switch (featureImport.getMatch()) {
				case IMatchRules.NONE:
				case IMatchRules.COMPATIBLE:
					// Compatible - The dependency version must be at least at
					// the
					// version specified, or at a higher service level or minor
					// level (major version level must equal the specified
					// version).
					return new VersionRange(minVersion, true, Version.createOSGi(osgi.getMajor() + 1, 0, 0), false);
				case IMatchRules.EQUIVALENT:
					// Equivalent - The dependency version must be at least at
					// the version specified, or at a higher service level
					// (major and minor version levels must equal the specified
					// version).
					return new VersionRange(minVersion, true,
							Version.createOSGi(osgi.getMajor(), osgi.getMinor() + 1, 0), false);
				case IMatchRules.PERFECT:
					// Perfect - The dependency version must match exactly the
					// specified version.
					return strictVersionRange(minVersion);
				case IMatchRules.GREATER_OR_EQUAL:
					// Greater or Equal - The dependency version must be at
					// least at the version specified, or at a higher service,
					// minor or major level.
					return new VersionRange(minVersion, true, Version.MAX_VERSION, true);
				}
			} catch (RuntimeException e) {
				// ignore
			}
		}
		return VersionRange.emptyRange;
	}

	private static VersionRange strictVersionRange(Version version) {
		return new VersionRange(version, true, version, true);
	}

	private static IMatchExpression<IInstallableUnit> getFilter(String baseFilter, boolean isImport,
			IEnvironment environment) {
		StringBuilder result = new StringBuilder();
		result.append("(&"); //$NON-NLS-1$
		if (baseFilter != null) {
			result.append(baseFilter);
		}
		if (isImport) {
			result.append("(!(org.eclipse.equinox.p2.exclude.import=true))"); //$NON-NLS-1$
		}
		if (environment != null) {
			expandFilter(environment.getOS(), "osgi.os", result); //$NON-NLS-1$
			expandFilter(environment.getWS(), "osgi.ws", result); //$NON-NLS-1$
			expandFilter(environment.getArch(), "osgi.arch", result);//$NON-NLS-1$
			expandFilter(environment.getNL(), "osgi.nl", result); //$NON-NLS-1$
		}
		if (result.length() == 2) {
			return null;
		}
		result.append(')');
		return InstallableUnit.parseFilter(result.toString());
	}

	private static void expandFilter(String filter, String osgiFilterValue, StringBuilder result) {
		if (filter != null && filter.length() != 0) {
			StringTokenizer token = new StringTokenizer(filter, ","); //$NON-NLS-1$
			if (token.countTokens() == 1) {
				result.append('(' + osgiFilterValue + '=' + filter + ')');
			} else {
				result.append("(|"); //$NON-NLS-1$
				while (token.hasMoreElements()) {
					result.append('(' + osgiFilterValue + '=' + token.nextToken() + ')');
				}
				result.append(')');
			}
		}
	}

	private static String getGroupId(String id) {
		if (id == null) {
			return null;
		}
		return id + ".feature.group"; //$NON-NLS-1$
	}

	/**
	 * Returns a URI corresponding to the given URL in string form, or null if a
	 * well formed URI could not be created.
	 */
	private static URI toURIOrNull(String url) {
		if (url == null) {
			return null;
		}
		try {
			return URIUtil.fromString(url);
		} catch (URISyntaxException e) {
			return null;
		}
	}

	private static void setProperty(InstallableUnitDescription iu, String key, String value) {
		if (value != null && !value.isEmpty()) {
			iu.setProperty(key, value);
		}
	}

	/**
	 * Creates an installable unit from a bundle description
	 *
	 * @param bd bundle description to create metadata for
	 * @return an installable unit
	 */
	private static IInstallableUnit createBundleIU(BundleDescription bd) {
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setSingleton(bd.isSingleton());
		iu.setId(bd.getSymbolicName());
		iu.setVersion(fromOSGiVersion(bd.getVersion()));
		iu.setFilter(bd.getPlatformFilter());
		iu.setTouchpointType(TOUCHPOINT_OSGI);

		boolean isFragment = bd.getHost() != null;

		//Process the required bundles
		BundleSpecification requiredBundles[] = bd.getRequiredBundles();
		ArrayList<IRequirement> reqsDeps = new ArrayList<>();
		if (isFragment) {
			reqsDeps.add(MetadataFactory.createRequirement(CAPABILITY_NS_OSGI_BUNDLE, bd.getHost().getName(), fromOSGiVersionRange(bd.getHost().getVersionRange()), null, false, false));
		}
		for (final BundleSpecification requiredBundle : requiredBundles) {
			reqsDeps.add(MetadataFactory.createRequirement(CAPABILITY_NS_OSGI_BUNDLE, requiredBundle.getName(), fromOSGiVersionRange(requiredBundle.getVersionRange()), null, requiredBundle.isOptional(), false));
		}

		// Process the import packages
		ImportPackageSpecification osgiImports[] = bd.getImportPackages();
		for (final ImportPackageSpecification importSpec : osgiImports) {
			String importPackageName = importSpec.getName();
			if (importPackageName.indexOf('*') != -1) {
				continue;
			}
			VersionRange versionRange = fromOSGiVersionRange(importSpec.getVersionRange());
			//TODO this needs to be refined to take into account all the attribute handled by imports
			boolean isOptional = importSpec.getDirective(Constants.RESOLUTION_DIRECTIVE).equals(ImportPackageSpecification.RESOLUTION_DYNAMIC) || importSpec.getDirective(Constants.RESOLUTION_DIRECTIVE).equals(ImportPackageSpecification.RESOLUTION_OPTIONAL);
			reqsDeps.add(MetadataFactory.createRequirement(CAPABILITY_NS_JAVA_PACKAGE, importPackageName, versionRange, null, isOptional, false));
		}
		iu.setRequirements((reqsDeps.toArray(new IRequirement[reqsDeps.size()])));

		// Create set of provided capabilities
		ArrayList<IProvidedCapability> providedCapabilities = new ArrayList<>();
		providedCapabilities.add(MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, bd.getSymbolicName(), fromOSGiVersion(bd.getVersion())));
		providedCapabilities.add(MetadataFactory.createProvidedCapability(CAPABILITY_NS_OSGI_BUNDLE, bd.getSymbolicName(), fromOSGiVersion(bd.getVersion())));

		// Process the export package
		ExportPackageDescription exports[] = bd.getExportPackages();
		for (final ExportPackageDescription export : exports) {
			//TODO make sure that we support all the refinement on the exports
			providedCapabilities.add(MetadataFactory.createProvidedCapability(CAPABILITY_NS_JAVA_PACKAGE, export.getName(), fromOSGiVersion(export.getVersion())));
		}
		// Here we add a bundle capability to identify bundles
		providedCapabilities.add(BUNDLE_CAPABILITY);
		if (isFragment) {
			providedCapabilities.add(MetadataFactory.createProvidedCapability(CAPABILITY_NS_OSGI_FRAGMENT, bd.getHost().getName(), fromOSGiVersion(bd.getVersion())));
		}

		iu.setCapabilities(providedCapabilities.toArray(new IProvidedCapability[providedCapabilities.size()]));
		return MetadataFactory.createInstallableUnit(iu);
	}

	private static Version fromOSGiVersion(org.osgi.framework.Version version) {
		if (version == null) {
			return Version.MAX_VERSION;
		}
		if (version.getMajor() == Integer.MAX_VALUE && version.getMinor() == Integer.MAX_VALUE
				&& version.getMicro() == Integer.MAX_VALUE) {
			return Version.MAX_VERSION;
		}
		return Version.createOSGi(version.getMajor(), version.getMinor(), version.getMicro(), version.getQualifier());
	}

	private static VersionRange fromOSGiVersionRange(org.eclipse.osgi.service.resolver.VersionRange range) {
		if (range.equals(org.eclipse.osgi.service.resolver.VersionRange.emptyRange)) {
			return VersionRange.emptyRange;
		}
		return new VersionRange(fromOSGiVersion(range.getMinimum()), range.getIncludeMinimum(), fromOSGiVersion(range.getRight()), range.getIncludeMaximum());
	}

	public static record ProductInfo(String id, String version, String name) {

	}
}
