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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
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
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.ITouchpointType;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.planner.ProfileInclusionRules;
import org.eclipse.equinox.simpleconfigurator.manipulator.SimpleConfiguratorManipulator;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.build.BundleHelper;
import org.eclipse.pde.internal.core.plugin.PluginBase;
import org.osgi.framework.Constants;

/**
 * Utilities to read and write p2 files
 *
 * @since 3.4
 */
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
	 * Returns bundles defined by the 'bundles.info' file in the
	 * specified location, or <code>null</code> if none. The "bundles.info" file
	 * is assumed to be at a fixed location relative to the configuration area URL.
	 * This method will also look for a "source.info".  If available, any source
	 * bundles found will also be added to the returned list.  If bundle URLs found
	 * in the bundles.info are relative, they will be appended to platformHome to
	 * make them absolute.
	 *
	 * @param platformHome absolute path in the local file system to an installation
	 * @param configurationArea url location of the configuration directory to search for bundles.info and source.info
	 * @return URLs of all bundles in the installation or <code>null</code> if not able
	 * 	to locate a bundles.info
	 */
	public static URL[] readBundlesTxt(String platformHome, File configurationArea) {
		if (configurationArea == null) {
			return null;
		}
		try {
			BundleInfo[] bundles = readBundles(platformHome, configurationArea);
			if (bundles == null) {
				return null;
			}
			int length = bundles.length;
			BundleInfo[] srcBundles = readSourceBundles(platformHome, configurationArea);
			if (srcBundles != null) {
				length += srcBundles.length;
			}
			URL[] urls = new URL[length];
			copyURLs(urls, 0, bundles);
			if (srcBundles != null && srcBundles.length > 0) {
				copyURLs(urls, bundles.length, srcBundles);
			}
			return urls;
		} catch (MalformedURLException e) {
			PDECore.log(e);
			return null;
		}
	}

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
		IPath basePath = new Path(platformHome);
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
		IPath basePath = new Path(platformHome);
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
	 * Copies URLs from the given bundle info objects into the specified array starting at the given index.
	 *
	 * @param dest array to copy URLs into
	 * @param start index to start copying into
	 * @param infos associated bundle infos
	 * @throws MalformedURLException
	 */
	private static void copyURLs(URL[] dest, int start, BundleInfo[] infos) throws MalformedURLException {
		for (BundleInfo info : infos) {
			dest[start++] = new File(info.getLocation()).toURL();
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
	 * @throws IOException
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
	 * Generates a profile containing metadata for all of the bundles in the provided collection.
	 * The profile will have the given profile ID and will be persisted in the profile registry
	 * directory inside the given p2 data area.
	 *
	 * @param profileID the ID to be used when creating the profile, if a profile with the same name exists, it will be overwritten
	 * @param p2DataArea the directory which contains p2 data including the profile registry, if the directory path doesn't exist it will be created
	 * @param bundles the collection of IPluginModelBase objects representing bundles to create metadata for and add to the profile
	 *
	 * @throws CoreException if the profile cannot be generated
	 */
	public static void createProfile(String profileID, File p2DataArea, Collection<List<IPluginModelBase>> bundles) throws CoreException {
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
}
