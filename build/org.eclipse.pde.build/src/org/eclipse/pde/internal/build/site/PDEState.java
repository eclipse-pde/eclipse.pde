/*******************************************************************************
 *  Copyright (c) 2004, 2021 IBM Corporation and others.
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
package org.eclipse.pde.internal.build.site;

import java.io.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.*;
import org.osgi.framework.*;

// This class provides a higher level API on the state
public class PDEState implements IPDEBuildConstants, IBuildPropertiesConstants {
	private static final String[] MANIFEST_ENTRIES = {Constants.BUNDLE_LOCALIZATION, Constants.BUNDLE_NAME, Constants.BUNDLE_VENDOR, ECLIPSE_BUNDLE_SHAPE, ECLIPSE_SOURCE_BUNDLE, ECLIPSE_SOURCE_REF};
	private static final int LAST_SUPPORTED_JDK = Integer.parseInt(JavaCore.latestSupportedJavaVersion());
	private StateObjectFactory factory;
	protected State state;
	private long id;
	private Properties repositoryVersions;
	private Properties sourceReferences;
	private HashMap<Long, String[]> bundleClasspaths;
	private ProfileManager profileManager;
	private Map<Long, String> patchBundles;
	private List<BundleDescription> addedBundle;
	private List<BundleDescription> unqualifiedBundles; //All the bundle description objects that have .qualifier in them 
	private Properties platformProperties;
	private List<BundleDescription> sortedBundles = null;
	private final Set<Dictionary<String, String>> convertedManifests;
	private long lastSortingDate = 0L;
	private String[] eeSources;

	protected long getNextId() {
		return ++id;
	}

	public PDEState(PDEUIStateWrapper initialState) {
		this();
		state = initialState.getState();
		factory = state.getFactory();
		id = initialState.getNextId();
		bundleClasspaths = initialState.getClasspaths();
		patchBundles = initialState.getPatchData();
		addedBundle = new ArrayList<>();
		unqualifiedBundles = new ArrayList<>();
		//forceQualifiers();
	}

	public PDEState() {
		factory = Platform.getPlatformAdmin().getFactory();
		state = factory.createState(false);
		state.setResolver(Platform.getPlatformAdmin().createResolver());
		id = 0;
		bundleClasspaths = new HashMap<>();
		patchBundles = new HashMap<>();
		convertedManifests = new HashSet<>(2);
		loadPluginTagFile();
		loadSourceReferences();
	}

	public StateObjectFactory getFactory() {
		return factory;
	}

	public boolean addBundleDescription(BundleDescription toAdd) {
		return state.addBundle(toAdd);
	}

	//Add a bundle to the state, updating the version number 
	public boolean addBundle(Dictionary<String, String> enhancedManifest, File bundleLocation) {
		String oldVersion = updateVersionNumber(enhancedManifest);
		try {
			BundleDescription descriptor;
			descriptor = factory.createBundleDescription(state, enhancedManifest, bundleLocation.getAbsolutePath(), getNextId());
			bundleClasspaths.put(Long.valueOf(descriptor.getBundleId()), BundleHelper.getClasspath(enhancedManifest));
			String patchValue = fillPatchData(enhancedManifest);
			if (patchValue != null)
				patchBundles.put(Long.valueOf(descriptor.getBundleId()), patchValue);
			rememberQualifierTagPresence(descriptor);
			rememberManifestConversion(descriptor, enhancedManifest);
			rememberManifestEntries(descriptor, enhancedManifest, MANIFEST_ENTRIES);
			rememberSourceReference(descriptor, oldVersion);
			if (addBundleDescription(descriptor) == true && addedBundle != null)
				addedBundle.add(descriptor);
		} catch (BundleException e) {
			IStatus status = new Status(IStatus.WARNING, IPDEBuildConstants.PI_PDEBUILD, EXCEPTION_STATE_PROBLEM, NLS.bind(Messages.exception_stateAddition, BundleHelper.getManifestHeader(enhancedManifest, Constants.BUNDLE_NAME)), e);
			BundleHelper.getDefault().getLog().log(status);
			return false;
		}
		return true;
	}

	private void rememberQualifierTagPresence(BundleDescription descriptor) {
		Properties bundleProperties = null;
		bundleProperties = (Properties) descriptor.getUserObject();
		if (bundleProperties == null) {
			bundleProperties = new Properties();
			descriptor.setUserObject(bundleProperties);
		}
		bundleProperties.setProperty(PROPERTY_QUALIFIER, "marker"); //$NON-NLS-1$
	}

	private void rememberSourceReference(BundleDescription descriptor, String oldVersion) {
		if (sourceReferences == null)
			return;

		String key = QualifierReplacer.getQualifierKey(descriptor.getSymbolicName(), oldVersion);
		if (key == null || !sourceReferences.containsKey(key))
			key = descriptor.getSymbolicName() + ',' + Version.emptyVersion.toString();
		if (sourceReferences.containsKey(key)) {
			Properties bundleProperties = (Properties) descriptor.getUserObject();
			if (bundleProperties == null) {
				bundleProperties = new Properties();
				descriptor.setUserObject(bundleProperties);
			}
			bundleProperties.setProperty(PROPERTY_SOURCE_REFERENCE, sourceReferences.getProperty(key));
		}
	}

	private void rememberManifestEntries(BundleDescription descriptor, Dictionary<String, String> manifest, String[] entries) {
		if (entries == null || entries.length == 0)
			return;

		Properties properties = (Properties) descriptor.getUserObject();
		if (properties == null) {
			properties = new Properties();
			descriptor.setUserObject(properties);
		}

		for (String entry2 : entries) {
			String entry = BundleHelper.getManifestHeader(manifest, entry2);
			if (entry != null) {
				properties.put(entry2, entry);
			}
		}
	}

	private void rememberManifestConversion(BundleDescription descriptor, Dictionary<String, String> manifest) {
		if (convertedManifests == null || !convertedManifests.contains(manifest))
			return;

		convertedManifests.remove(manifest);
		Properties bundleProperties = (Properties) descriptor.getUserObject();
		if (bundleProperties == null) {
			bundleProperties = new Properties();
			descriptor.setUserObject(bundleProperties);
		}
		bundleProperties.setProperty(PROPERTY_CONVERTED_MANIFEST, "marker"); //$NON-NLS-1$
	}

	private void mapVersionReplacedBundle(BundleDescription oldBundle, BundleDescription newBundle) {
		Properties bundleProperties = null;
		bundleProperties = (Properties) oldBundle.getUserObject();
		if (bundleProperties == null) {
			bundleProperties = new Properties();
			oldBundle.setUserObject(bundleProperties);
		}
		bundleProperties.setProperty(PROPERTY_VERSION_REPLACEMENT, String.valueOf(newBundle.getBundleId()));
	}

	private String fillPatchData(Dictionary<String, String> manifest) {
		if (BundleHelper.getManifestHeader(manifest, EXTENSIBLE_API) != null) {
			return EXTENSIBLE_API + ": true"; //$NON-NLS-1$
		}

		if (BundleHelper.getManifestHeader(manifest, PATCH_FRAGMENT) != null) {
			return PATCH_FRAGMENT + ": true"; //$NON-NLS-1$
		}
		return null;
	}

	private void loadPluginTagFile() {
		repositoryVersions = new Properties();
		try (InputStream input = new BufferedInputStream(new FileInputStream(AbstractScriptGenerator.getWorkingDirectory() + '/' + DEFAULT_PLUGIN_REPOTAG_FILENAME_DESCRIPTOR))) {
			repositoryVersions.load(input);
		} catch (IOException e) {
			//Ignore
		}
	}

	private void loadSourceReferences() {
		sourceReferences = new Properties();
		try (InputStream input = new BufferedInputStream(new FileInputStream(AbstractScriptGenerator.getWorkingDirectory() + '/' + DEFAULT_SOURCE_REFERENCES_FILENAME_DESCRIPTOR))) {
			sourceReferences.load(input);
		} catch (IOException e) {
			//Ignore
		}
	}

	public boolean addBundle(File bundleLocation) {
		Dictionary<String, String> manifest;
		manifest = loadManifest(bundleLocation);
		if (manifest == null) {
			return addFlexibleRoot(bundleLocation);
		}
		try {
			hasQualifier(bundleLocation, manifest);
		} catch (BundleException e) {
			//should not happen since we know the header
		}
		return addBundle(manifest, bundleLocation);
	}

	private boolean addFlexibleRoot(File bundleLocation) {
		if (!new File(bundleLocation, PDE_CORE_PREFS).exists())
			return false;

		try {
			Properties properties = AbstractScriptGenerator.readProperties(bundleLocation.getAbsolutePath(), PDE_CORE_PREFS, IStatus.OK);
			String root = properties.getProperty(BUNDLE_ROOT_PATH);
			if (root != null)
				return addBundle(new File(bundleLocation, root));
		} catch (CoreException e) {
			//ignore
		}
		return false;
	}

	private String updateVersionNumber(Dictionary<String, String> manifest) {
		String newVersion = null;
		String oldVersion = null;
		try {
			String symbolicName = BundleHelper.getManifestHeader(manifest, Constants.BUNDLE_SYMBOLICNAME);
			if (symbolicName == null)
				return null;

			symbolicName = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, symbolicName)[0].getValue();
			oldVersion = BundleHelper.getManifestHeader(manifest, Constants.BUNDLE_VERSION);
			newVersion = QualifierReplacer.replaceQualifierInVersion(oldVersion, symbolicName, manifest.get(PROPERTY_QUALIFIER), repositoryVersions);
		} catch (BundleException e) {
			//ignore
		}
		if (newVersion != null)
			manifest.put(Constants.BUNDLE_VERSION, newVersion);
		return oldVersion;
	}

	/**
	 * @param bundleLocation
	 * @param manifest
	 * @throws BundleException
	 */
	private void hasQualifier(File bundleLocation, Dictionary<String, String> manifest) throws BundleException {
		ManifestElement[] versionInfo = ManifestElement.parseHeader(Constants.BUNDLE_VERSION, BundleHelper.getManifestHeader(manifest, Constants.BUNDLE_VERSION));
		if (versionInfo != null) {
			if (versionInfo[0].getValue().endsWith(PROPERTY_QUALIFIER)) {
				manifest.put(PROPERTY_QUALIFIER, getQualifierPropery(bundleLocation.getAbsolutePath()));
			}
		}
	}

	private String getQualifierPropery(String bundleLocation) {
		String qualifierInfo = null;
		try {
			qualifierInfo = AbstractScriptGenerator.readProperties(bundleLocation, IPDEBuildConstants.PROPERTIES_FILE, IStatus.INFO).getProperty(PROPERTY_QUALIFIER);
		} catch (CoreException e) {
			//ignore
		}
		if (qualifierInfo == null)
			qualifierInfo = PROPERTY_CONTEXT;
		return qualifierInfo;
	}

	//Return a dictionary representing a manifest. The data may result from plugin.xml conversion  
	private Dictionary<String, String> basicLoadManifest(File bundleLocation) {
		InputStream manifestStream = null;
		ZipFile jarFile = null;
		try {
			if ("jar".equalsIgnoreCase(new Path(bundleLocation.getName()).getFileExtension()) && bundleLocation.isFile()) { //$NON-NLS-1$
				jarFile = new ZipFile(bundleLocation, ZipFile.OPEN_READ);
				ZipEntry manifestEntry = jarFile.getEntry(JarFile.MANIFEST_NAME);
				if (manifestEntry != null) {
					manifestStream = jarFile.getInputStream(manifestEntry);
				}
			} else {
				manifestStream = new BufferedInputStream(new FileInputStream(new File(bundleLocation, JarFile.MANIFEST_NAME)));
			}
		} catch (IOException e) {
			//ignore
		}

		//It is not a manifest, but a plugin or a fragment
		if (manifestStream == null)
			return null;

		try {
			Hashtable<String, String> result = new Hashtable<>();
			result.putAll(ManifestElement.parseBundleManifest(manifestStream, null));
			return result;
		} catch (IOException | BundleException e) {
			return null;
		} finally {
			try {
				manifestStream.close();
			} catch (IOException e1) {
				//Ignore
			}
			try {
				if (jarFile != null)
					jarFile.close();
			} catch (IOException e2) {
				//Ignore
			}
		}
	}

	private boolean enforceSymbolicName(File bundleLocation, Dictionary<String, String> initialManifest) {
		if (BundleHelper.getManifestHeader(initialManifest, Constants.BUNDLE_SYMBOLICNAME) != null)
			return true;

		return false;
	}

	private void enforceClasspath(Dictionary<String, String> manifest) {
		String classpath = BundleHelper.getManifestHeader(manifest, Constants.BUNDLE_CLASSPATH);
		if (classpath == null)
			manifest.put(Constants.BUNDLE_CLASSPATH, "."); //$NON-NLS-1$
	}

	private void enforceVersion(Dictionary<String, String> manifest) {
		String version = BundleHelper.getManifestHeader(manifest, Constants.BUNDLE_VERSION);
		if (version == null)
			manifest.put(Constants.BUNDLE_VERSION, "0.0.0"); //$NON-NLS-1$
	}

	private Dictionary<String, String> loadManifest(File bundleLocation) {
		Dictionary<String, String> manifest = basicLoadManifest(bundleLocation);
		if (manifest == null)
			return null;

		// require a Bundle-SymbolicName
		if (!enforceSymbolicName(bundleLocation, manifest))
			return null;
		enforceVersion(manifest);
		enforceClasspath(manifest);
		return manifest;
	}

	public void addBundles(Collection<File> bundles) {
		for (File bundle : bundles) {
			addBundle(bundle);
		}
	}

	public void resolveState() {
		List<Config> configs = AbstractScriptGenerator.getConfigInfos();
		ArrayList<Dictionary<String, Object>> properties = new ArrayList<>(); //Collection of dictionaries
		Dictionary<String, Object> prop;

		// initialize profileManager and get the JRE profiles
		String[] javaProfiles = getJavaProfiles();
		String systemPackages = null;
		String ee = null;

		for (Config aConfig : configs) {
			prop = new Hashtable<>();
			if (AbstractScriptGenerator.getPropertyAsBoolean(RESOLVER_DEV_MODE))
				prop.put(PROPERTY_RESOLVER_MODE, VALUE_DEVELOPMENT);
			String os = aConfig.getOs();
			String ws = aConfig.getWs();
			String arch = aConfig.getArch();
			if (Config.ANY.equalsIgnoreCase(os))
				prop.put(OSGI_OS, CatchAllValue.singleton);
			else
				prop.put(OSGI_OS, os);

			if (Config.ANY.equalsIgnoreCase(ws))
				prop.put(OSGI_WS, CatchAllValue.singleton);
			else
				prop.put(OSGI_WS, ws);

			if (Config.ANY.equalsIgnoreCase(arch))
				prop.put(OSGI_ARCH, CatchAllValue.singleton);
			else
				prop.put(OSGI_ARCH, arch);

			// check the user-specified platform properties
			if (platformProperties != null) {
				for (Enumeration<Object> e = platformProperties.keys(); e.hasMoreElements();) {
					String key = (String) e.nextElement();
					prop.put(key, platformProperties.get(key));
				}
			}

			properties.add(prop);
		}

		Properties profileProps = null;
		boolean added = false;
		String eeJava9 = null;
		//javaProfiles are sorted, go in reverse order, and if when we hit 0 we haven't added any yet, 
		//then add that last profile so we have something.
		for (int j = javaProfiles.length - 1; j >= 0; j--) {
			// add a property set for each EE that is defined in the build.
			profileProps = profileManager.getProfileProperties(javaProfiles[j]);
			if (profileProps != null) {
				String profileName = profileProps.getProperty(ProfileManager.PROFILE_NAME);
				if (AbstractScriptGenerator.getImmutableAntProperty(profileName) != null || (j == 0 && !added)) {
					systemPackages = profileProps.getProperty(ProfileManager.SYSTEM_PACKAGES);
					ee = profileProps.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);

					prop = new Hashtable<>();
					prop.put(ProfileManager.SYSTEM_PACKAGES, systemPackages);
					if (profileName.equals("JavaSE-9")) { //$NON-NLS-1$
						eeJava9 = ee;
					}
					prop.put(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, ee);
					properties.add(prop);
					added = true;
				}
			}
		}
		// from java 10 and beyond 
		ArrayList<String> eeJava10AndBeyond = new ArrayList<>();
		for (int i = 10; i <= LAST_SUPPORTED_JDK; i++) {
			eeJava10AndBeyond.add("JavaSE-" + i);//$NON-NLS-1$		
		}
		prop = new Hashtable<>();
		String previousEE = eeJava9;
		for (String execEnvID : eeJava10AndBeyond) {
			prop = new Hashtable<>();
			IExecutionEnvironment env = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment(execEnvID);
			systemPackages = querySystemPackages(env);
			String currentEE = previousEE + "," + execEnvID; //$NON-NLS-1$
			if (systemPackages == null) {
				previousEE = currentEE;
				continue;
			}
			prop.put(ProfileManager.SYSTEM_PACKAGES, systemPackages);
			prop.put(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, currentEE);
			previousEE = currentEE;
			properties.add(prop);
		}

		Dictionary<String, Object>[] stateProperties = properties.toArray(new Dictionary[properties.size()]);
		state.setPlatformProperties(stateProperties);
		state.resolve(false);

		if (unqualifiedBundles != null) {
			forceQualifiers();
		}
	}

	public static String querySystemPackages(IExecutionEnvironment environment) {
		// Copy of org.eclipse.pde.internal.core.TargetPlatformHelper.querySystemPackages()
		IVMInstall vm = bestVmInstallFor(environment);
		if (vm == null || !JavaRuntime.isModularJava(vm)) {
			return null;
		}
		String release = environment.getProfileProperties().getProperty(JavaCore.COMPILER_COMPLIANCE);
		try {
			Collection<String> packages = new TreeSet<>();
			String jrtPath = "lib/" + org.eclipse.jdt.internal.compiler.util.JRTUtil.JRT_FS_JAR; //$NON-NLS-1$
			String path = new File(vm.getInstallLocation(), jrtPath).toString(); // $NON-NLS-1$
			var jrt = org.eclipse.jdt.internal.core.builder.ClasspathLocation.forJrtSystem(path, null, null, release);
			for (String moduleName : jrt.getModuleNames(null)) {
				var module = jrt.getModule(moduleName);
				if (module == null) {
					continue;
				}
				for (var packageExport : module.exports()) {
					if (!packageExport.isQualified()) {
						packages.add(new String(packageExport.name()));
					}
				}
			}
			return String.join(",", packages); //$NON-NLS-1$
		} catch (CoreException e) {
			Platform.getLog(PDEState.class).log(Status.error("failed to read system packages for " + environment, e)); //$NON-NLS-1$
		}
		return null;
	}

	private static IVMInstall bestVmInstallFor(IExecutionEnvironment environment) {
		IVMInstall defaultVM = environment.getDefaultVM();
		if (defaultVM != null) {
			return defaultVM;
		}
		IVMInstall[] compatible = environment.getCompatibleVMs();
		if (compatible.length == 0) {
			return null;
		}
		for (IVMInstall vm : compatible) {
			if (environment.isStrictlyCompatible(vm)) {
				return vm;
			}
		}
		return compatible[0];
	}

	public State getState() {
		return state;
	}

	public BundleDescription[] getDependentBundles(String bundleId, Version version) {
		BundleDescription root = state.getBundle(bundleId, version);
		return getDependentBundles(root);
	}

	/**
	 * This methods return the bundleDescriptions to which imports have been
	 * bound to.
	 * 
	 * @param root
	 */
	public static BundleDescription[] getImportedBundles(BundleDescription root) {
		if (root == null)
			return new BundleDescription[0];
		ExportPackageDescription[] packages = root.getResolvedImports();
		ArrayList<BundleDescription> resolvedImports = new ArrayList<>(packages.length);
		for (ExportPackageDescription package1 : packages)
			if (!root.getLocation().equals(package1.getExporter().getLocation()) && !resolvedImports.contains(package1.getExporter()))
				resolvedImports.add(package1.getExporter());
		return resolvedImports.toArray(new BundleDescription[resolvedImports.size()]);
	}

	/**
	 * This methods return the bundleDescriptions to which required bundles
	 * have been bound to.
	 * 
	 * @param root
	 */
	public static BundleDescription[] getRequiredBundles(BundleDescription root) {
		if (root == null)
			return new BundleDescription[0];
		return root.getResolvedRequires();
	}

	public BundleDescription getResolvedBundle(String bundleId, String version) {
		return getBundle(bundleId, version, true);
	}

	public BundleDescription getBundle(String bundleId, String version, boolean resolved) {
		if (IPDEBuildConstants.GENERIC_VERSION_NUMBER.equals(version) || version == null) {
			BundleDescription bundle = getResolvedBundle(bundleId);
			if (bundle == null && !resolved)
				bundle = getState().getBundle(bundleId, null);
			return bundle;
		}
		Version parsedVersion = Version.parseVersion(version);
		BundleDescription description = getState().getBundle(bundleId, parsedVersion);
		if (description != null && (!resolved || description.isResolved()))
			return description;

		if (parsedVersion.getQualifier().indexOf(IBuildPropertiesConstants.PROPERTY_QUALIFIER) > -1) {
			BundleDescription[] bundles = sortByVersion(getState().getBundles(bundleId));
			VersionRange qualifierRange = Utils.createVersionRange(version);
			//bundles are sorted, start at the high end
			for (int i = bundles.length - 1; i >= 0; i--) {
				if (qualifierRange.isIncluded(bundles[i].getVersion()) && (!resolved || bundles[i].isResolved()))
					return bundles[i];
			}
		}
		return null;
	}

	/**
	 * Sort the BundleDescription[] by Version, lowest to highest.
	 * (It is likely they are already close to this order)
	 * @param bundles
	 * @return sorted BundleDescription []
	 */
	private BundleDescription[] sortByVersion(BundleDescription[] bundles) {
		if (bundles.length > 1) {
			Arrays.sort(bundles, (o1, o2) -> o1.getVersion().compareTo(o2.getVersion()));
		}
		return bundles;
	}

	public BundleDescription getResolvedBundle(String bundleId) {
		BundleDescription[] description = sortByVersion(getState().getBundles(bundleId));
		if (description == null)
			return null;
		//bundles are sorted, start at the high end
		for (int i = description.length - 1; i >= 0; i--) {
			if (description[i].isResolved())
				return description[i];
		}
		return null;
	}

	public static BundleDescription[] getDependentBundles(BundleDescription root) {
		BundleDescription[] imported = getImportedBundles(root);
		BundleDescription[] required = getRequiredBundles(root);
		BundleDescription[] dependents = new BundleDescription[imported.length + required.length];
		System.arraycopy(imported, 0, dependents, 0, imported.length);
		System.arraycopy(required, 0, dependents, imported.length, required.length);
		return dependents;
	}

	public static BundleDescription[] getDependentBundlesWithFragments(BundleDescription root) {
		BundleDescription[] imported = getImportedBundles(root);
		BundleDescription[] importedByFragments = getImportedByFragments(root);
		BundleDescription[] required = getRequiredBundles(root);
		BundleDescription[] requiredByFragments = getRequiredByFragments(root);
		BundleDescription[] dependents = new BundleDescription[imported.length + importedByFragments.length + required.length + requiredByFragments.length];
		System.arraycopy(imported, 0, dependents, 0, imported.length);
		System.arraycopy(importedByFragments, 0, dependents, imported.length, importedByFragments.length);
		System.arraycopy(required, 0, dependents, imported.length + importedByFragments.length, required.length);
		System.arraycopy(requiredByFragments, 0, dependents, imported.length + importedByFragments.length + required.length, requiredByFragments.length);
		return dependents;
	}

	public static BundleDescription[] getImportedByFragments(BundleDescription root) {
		BundleDescription[] fragments = root.getFragments();
		List<BundleDescription> importedByFragments = new ArrayList<>();
		for (BundleDescription fragment2 : fragments) {
			if (!fragment2.isResolved())
				continue;
			merge(importedByFragments, getImportedBundles(fragment2));
		}
		BundleDescription[] result = new BundleDescription[importedByFragments.size()];
		return importedByFragments.toArray(result);
	}

	public static BundleDescription[] getRequiredByFragments(BundleDescription root) {
		BundleDescription[] fragments = root.getFragments();
		List<BundleDescription> importedByFragments = new ArrayList<>();
		for (BundleDescription fragment2 : fragments) {
			if (!fragment2.isResolved())
				continue;
			merge(importedByFragments, getRequiredBundles(fragment2));
		}
		BundleDescription[] result = new BundleDescription[importedByFragments.size()];
		return importedByFragments.toArray(result);
	}

	public static void merge(List<BundleDescription> source, BundleDescription[] toAdd) {
		for (BundleDescription element : toAdd) {
			if (!source.contains(element))
				source.add(element);
		}
	}

	public Properties loadPropertyFileIn(Map<String, String> toMerge, File location) {
		Properties result = new Properties();
		result.putAll(toMerge);
		try (InputStream propertyStream = new BufferedInputStream(new FileInputStream(new File(location, PROPERTIES_FILE)))) {
			result.load(propertyStream);
		} catch (IOException e) {
			//ignore because compiled plug-ins do not have such files
		}
		return result;
	}

	public HashMap<Long, String[]> getExtraData() {
		return bundleClasspaths;
	}

	public Map<Long, String> getPatchData() {
		return patchBundles;
	}

	public List<BundleDescription> getSortedBundles() {
		if (lastSortingDate != getState().getTimeStamp()) {
			lastSortingDate = getState().getTimeStamp();
			BundleDescription[] toSort = getState().getResolvedBundles();
			Platform.getPlatformAdmin().getStateHelper().sortBundles(toSort);
			sortedBundles = Arrays.asList(toSort);
		}
		return sortedBundles;
	}

	public void cleanupOriginalState() {
		if (addedBundle == null && unqualifiedBundles == null)
			return;

		for (BundleDescription added : addedBundle) {
			state.removeBundle(added);
		}
		addedBundle.clear();

		for (BundleDescription toAddBack : unqualifiedBundles) {
			state.removeBundle(toAddBack.getBundleId());
			addBundleDescription(toAddBack);
		}
		unqualifiedBundles.clear();

		BundleDescription[] allBundles = state.getBundles();
		for (BundleDescription bundle : allBundles) {
			bundle.setUserObject(null);
		}
		state.resolve();
	}

	private File getOSGiLocation() {
		BundleDescription osgiBundle = state.getBundle(BUNDLE_OSGI, null);
		if (osgiBundle == null)
			return null;
		return new File(osgiBundle.getLocation());
	}

	private String[] getJavaProfiles() {
		return getProfileManager().getJavaProfiles();
	}

	//Replace the version numbers that ends with .qualifier
	private void forceQualifiers() {
		BundleDescription[] resolvedBundles = state.getResolvedBundles(); //We only get the resolved bundles since, changing the qualifier should not change the resolution state 
		for (BundleDescription b : resolvedBundles) {
			if (b.getVersion().getQualifier().endsWith(PROPERTY_QUALIFIER)) {
				unqualifiedBundles.add(state.removeBundle(b.getBundleId())); //We keep the removed bundle so we can reinsert it in the state when we are done
				String newVersion = QualifierReplacer.replaceQualifierInVersion(b.getVersion().toString(), b.getSymbolicName(), getQualifierPropery(b.getLocation()), null);

				//Here it is important to reuse the same bundle id than the bundle we are removing so that we don't loose the information about the classpath
				BundleDescription newBundle = state.getFactory().createBundleDescription(b.getBundleId(), b.getSymbolicName(), new Version(newVersion), b.getLocation(), b.getRequiredBundles(), b.getHost(), b.getImportPackages(), b.getExportPackages(), b.isSingleton(), b.attachFragments(), b.dynamicFragments(), b.getPlatformFilter(), b.getExecutionEnvironments(), b.getGenericRequires(), b.getGenericCapabilities(), b.getNativeCodeSpecification());
				addBundleDescription(newBundle);
				rememberQualifierTagPresence(newBundle);
				mapVersionReplacedBundle(b, newBundle);
			}
		}
		state.resolve();
	}

	/*
	 * If this bundle had its qualifier version replaced, return the replacement bundle description
	 * return the original bundle if no replacement occurred
	 */
	public BundleDescription getVersionReplacement(BundleDescription bundle) {
		Properties props = (Properties) bundle.getUserObject();
		if (props == null)
			return bundle;
		String idString = props.getProperty(PROPERTY_VERSION_REPLACEMENT);
		if (idString == null)
			return bundle;
		try {
			long newId = Long.parseLong(idString);
			BundleDescription newBundle = state.getBundle(newId);
			if (newBundle != null)
				return newBundle;
		} catch (NumberFormatException e) {
			// fall through
		}
		return bundle;
	}

	public void setPlatformProperties(Properties platformProperties) {
		this.platformProperties = platformProperties;
	}

	public void setEESources(String[] eeSources) {
		this.eeSources = eeSources;
	}

	public ProfileManager getProfileManager() {
		if (profileManager == null) {
			File osgi = getOSGiLocation();
			String[] sources = null;
			if (osgi != null) {
				if (eeSources != null) {
					sources = new String[eeSources.length + 1];
					sources[0] = osgi.getAbsolutePath();
					System.arraycopy(eeSources, 0, sources, 1, eeSources.length);
				} else {
					sources = new String[] {osgi.getAbsolutePath()};
				}
				profileManager = new ProfileManager(sources, false);
			} else {
				profileManager = new ProfileManager(eeSources, true);
			}
		}
		return profileManager;
	}
}
