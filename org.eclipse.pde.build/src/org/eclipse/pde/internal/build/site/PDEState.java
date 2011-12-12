/*******************************************************************************
 *  Copyright (c) 2004, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.site;

import java.io.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.service.pluginconversion.PluginConverter;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.*;
import org.osgi.framework.*;

// This class provides a higher level API on the state
public class PDEState implements IPDEBuildConstants, IBuildPropertiesConstants {
	private static final String[] MANIFEST_ENTRIES = {Constants.BUNDLE_LOCALIZATION, Constants.BUNDLE_NAME, Constants.BUNDLE_VENDOR, ECLIPSE_BUNDLE_SHAPE, ECLIPSE_SOURCE_BUNDLE, ECLIPSE_SOURCE_REF};

	private StateObjectFactory factory;
	protected State state;
	private long id;
	private Properties repositoryVersions;
	private Properties sourceReferences;
	private HashMap bundleClasspaths;
	private ProfileManager profileManager;
	private Map patchBundles;
	private List addedBundle;
	private List unqualifiedBundles; //All the bundle description objects that have .qualifier in them 
	private Dictionary platformProperties;
	private List sortedBundles = null;
	private final Set convertedManifests;
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
		addedBundle = new ArrayList();
		unqualifiedBundles = new ArrayList();
		//forceQualifiers();
	}

	public PDEState() {
		factory = Platform.getPlatformAdmin().getFactory();
		state = factory.createState(false);
		state.setResolver(Platform.getPlatformAdmin().createResolver());
		id = 0;
		bundleClasspaths = new HashMap();
		patchBundles = new HashMap();
		convertedManifests = new HashSet(2);
		loadPluginTagFile();
		loadSourceReferences();
	}

	public StateObjectFactory getFactory() {
		return factory;
	}

	public boolean addBundleDescription(BundleDescription toAdd) {
		return state.addBundle(toAdd);
	}

	private PluginConverter acquirePluginConverter() throws Exception {
		return (PluginConverter) BundleHelper.getDefault().acquireService(PluginConverter.class.getName());
	}

	//Add a bundle to the state, updating the version number 
	public boolean addBundle(Dictionary enhancedManifest, File bundleLocation) {
		String oldVersion = updateVersionNumber(enhancedManifest);
		try {
			BundleDescription descriptor;
			descriptor = factory.createBundleDescription(state, enhancedManifest, bundleLocation.getAbsolutePath(), getNextId());
			bundleClasspaths.put(new Long(descriptor.getBundleId()), BundleHelper.getClasspath(enhancedManifest));
			String patchValue = fillPatchData(enhancedManifest);
			if (patchValue != null)
				patchBundles.put(new Long(descriptor.getBundleId()), patchValue);
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

	private void rememberManifestEntries(BundleDescription descriptor, Dictionary manifest, String[] entries) {
		if (entries == null || entries.length == 0)
			return;

		Properties properties = (Properties) descriptor.getUserObject();
		if (properties == null) {
			properties = new Properties();
			descriptor.setUserObject(properties);
		}

		for (int i = 0; i < entries.length; i++) {
			String entry = BundleHelper.getManifestHeader(manifest, entries[i]);
			if (entry != null) {
				properties.put(entries[i], entry);
			}
		}
	}

	private void rememberManifestConversion(BundleDescription descriptor, Dictionary manifest) {
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

	private String fillPatchData(Dictionary manifest) {
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
		try {
			InputStream input = new BufferedInputStream(new FileInputStream(AbstractScriptGenerator.getWorkingDirectory() + '/' + DEFAULT_PLUGIN_REPOTAG_FILENAME_DESCRIPTOR));
			try {
				repositoryVersions.load(input);
			} finally {
				input.close();
			}
		} catch (IOException e) {
			//Ignore
		}
	}

	private void loadSourceReferences() {
		sourceReferences = new Properties();
		try {
			InputStream input = new BufferedInputStream(new FileInputStream(AbstractScriptGenerator.getWorkingDirectory() + '/' + DEFAULT_SOURCE_REFERENCES_FILENAME_DESCRIPTOR));
			try {
				sourceReferences.load(input);
			} finally {
				input.close();
			}
		} catch (IOException e) {
			//Ignore
		}
	}

	public boolean addBundle(File bundleLocation) {
		Dictionary manifest;
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

	private String updateVersionNumber(Dictionary manifest) {
		String newVersion = null;
		String oldVersion = null;
		try {
			String symbolicName = BundleHelper.getManifestHeader(manifest, Constants.BUNDLE_SYMBOLICNAME);
			if (symbolicName == null)
				return null;

			symbolicName = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, symbolicName)[0].getValue();
			oldVersion = BundleHelper.getManifestHeader(manifest, Constants.BUNDLE_VERSION);
			newVersion = QualifierReplacer.replaceQualifierInVersion(oldVersion, symbolicName, (String) manifest.get(PROPERTY_QUALIFIER), repositoryVersions);
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
	private void hasQualifier(File bundleLocation, Dictionary manifest) throws BundleException {
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
	private Dictionary basicLoadManifest(File bundleLocation) {
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
			return convertPluginManifest(bundleLocation, true);

		try {
			Hashtable result = new Hashtable();
			result.putAll(ManifestElement.parseBundleManifest(manifestStream, null));
			return result;
		} catch (IOException ioe) {
			return null;
		} catch (BundleException e) {
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

	private boolean enforceSymbolicName(File bundleLocation, Dictionary initialManifest) {
		if (BundleHelper.getManifestHeader(initialManifest, Constants.BUNDLE_SYMBOLICNAME) != null)
			return true;

		Dictionary generatedManifest = convertPluginManifest(bundleLocation, false);
		if (generatedManifest == null)
			return false;

		//merge manifests. The values from the generated manifest are added to the initial one. Values from the initial one are not deleted 
		Enumeration enumeration = generatedManifest.keys();
		while (enumeration.hasMoreElements()) {
			Object key = enumeration.nextElement();
			if (BundleHelper.getManifestHeader(initialManifest, (String) key) == null)
				initialManifest.put(key, generatedManifest.get(key));
		}
		return true;
	}

	private void enforceClasspath(Dictionary manifest) {
		String classpath = BundleHelper.getManifestHeader(manifest, Constants.BUNDLE_CLASSPATH);
		if (classpath == null)
			manifest.put(Constants.BUNDLE_CLASSPATH, "."); //$NON-NLS-1$
	}

	private void enforceVersion(Dictionary manifest) {
		String version = BundleHelper.getManifestHeader(manifest, Constants.BUNDLE_VERSION);
		if (version == null)
			manifest.put(Constants.BUNDLE_VERSION, "0.0.0"); //$NON-NLS-1$
	}

	private Dictionary loadManifest(File bundleLocation) {
		Dictionary manifest = basicLoadManifest(bundleLocation);
		if (manifest == null)
			return null;

		// require a Bundle-SymbolicName
		if (!enforceSymbolicName(bundleLocation, manifest))
			return null;
		enforceVersion(manifest);
		enforceClasspath(manifest);
		return manifest;
	}

	private Dictionary convertPluginManifest(File bundleLocation, boolean logConversionException) {
		PluginConverter converter;
		try {
			converter = acquirePluginConverter();
			Dictionary manifest = converter.convertManifest(bundleLocation, false, AbstractScriptGenerator.isBuildingOSGi() ? null : "2.1", false, null); //$NON-NLS-1$
			if (convertedManifests != null)
				convertedManifests.add(manifest);
			return manifest;
		} catch (PluginConversionException convertException) {
			if (bundleLocation.getName().equals(org.eclipse.pde.build.Constants.FEATURE_FILENAME_DESCRIPTOR))
				return null;
			if (!new File(bundleLocation, org.eclipse.pde.build.Constants.PLUGIN_FILENAME_DESCRIPTOR).exists() && !new File(bundleLocation, org.eclipse.pde.build.Constants.FRAGMENT_FILENAME_DESCRIPTOR).exists())
				return null;
			if (logConversionException) {
				IStatus status = new Status(IStatus.WARNING, PI_PDEBUILD, 0, NLS.bind(Messages.exception_errorConverting, bundleLocation.getAbsolutePath()), convertException);
				BundleHelper.getDefault().getLog().log(status);
			}
			return null;
		} catch (Exception serviceException) {
			IStatus status = new Status(IStatus.WARNING, PI_PDEBUILD, 0, NLS.bind(Messages.exception_cannotAcquireService, "Plugin converter"), serviceException); //$NON-NLS-1$
			BundleHelper.getDefault().getLog().log(status);
			return null;
		}
	}

	public void addBundles(Collection bundles) {
		for (Iterator iter = bundles.iterator(); iter.hasNext();) {
			File bundle = (File) iter.next();
			addBundle(bundle);
		}
	}

	public void resolveState() {
		List configs = AbstractScriptGenerator.getConfigInfos();
		ArrayList properties = new ArrayList(); //Collection of dictionaries
		Dictionary prop;

		// initialize profileManager and get the JRE profiles
		String[] javaProfiles = getJavaProfiles();
		String systemPackages = null;
		String ee = null;

		for (Iterator iter = configs.iterator(); iter.hasNext();) {
			Config aConfig = (Config) iter.next();
			prop = new Hashtable();
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
				for (Enumeration e = platformProperties.keys(); e.hasMoreElements();) {
					String key = (String) e.nextElement();
					prop.put(key, platformProperties.get(key));
				}
			}

			properties.add(prop);
		}

		Properties profileProps = null;
		boolean added = false;
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

					prop = new Hashtable();
					prop.put(ProfileManager.SYSTEM_PACKAGES, systemPackages);
					prop.put(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, ee);
					properties.add(prop);
					added = true;
				}
			}
		}

		Dictionary[] stateProperties = (Dictionary[]) properties.toArray(new Dictionary[properties.size()]);
		state.setPlatformProperties(stateProperties);
		state.resolve(false);

		if (unqualifiedBundles != null) {
			forceQualifiers();
		}
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
		ArrayList resolvedImports = new ArrayList(packages.length);
		for (int i = 0; i < packages.length; i++)
			if (!root.getLocation().equals(packages[i].getExporter().getLocation()) && !resolvedImports.contains(packages[i].getExporter()))
				resolvedImports.add(packages[i].getExporter());
		return (BundleDescription[]) resolvedImports.toArray(new BundleDescription[resolvedImports.size()]);
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
			Arrays.sort(bundles, new Comparator() {
				public int compare(Object o1, Object o2) {
					return ((BundleDescription) o1).getVersion().compareTo(((BundleDescription) o2).getVersion());
				}
			});
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
		List importedByFragments = new ArrayList();
		for (int i = 0; i < fragments.length; i++) {
			if (!fragments[i].isResolved())
				continue;
			merge(importedByFragments, getImportedBundles(fragments[i]));
		}
		BundleDescription[] result = new BundleDescription[importedByFragments.size()];
		return (BundleDescription[]) importedByFragments.toArray(result);
	}

	public static BundleDescription[] getRequiredByFragments(BundleDescription root) {
		BundleDescription[] fragments = root.getFragments();
		List importedByFragments = new ArrayList();
		for (int i = 0; i < fragments.length; i++) {
			if (!fragments[i].isResolved())
				continue;
			merge(importedByFragments, getRequiredBundles(fragments[i]));
		}
		BundleDescription[] result = new BundleDescription[importedByFragments.size()];
		return (BundleDescription[]) importedByFragments.toArray(result);
	}

	public static void merge(List source, BundleDescription[] toAdd) {
		for (int i = 0; i < toAdd.length; i++) {
			if (!source.contains(toAdd[i]))
				source.add(toAdd[i]);
		}
	}

	public Properties loadPropertyFileIn(Map toMerge, File location) {
		Properties result = new Properties();
		result.putAll(toMerge);
		try {
			InputStream propertyStream = new BufferedInputStream(new FileInputStream(new File(location, PROPERTIES_FILE)));
			try {
				result.load(propertyStream);
			} finally {
				propertyStream.close();
			}
		} catch (IOException e) {
			//ignore because compiled plug-ins do not have such files
		}
		return result;
	}

	public HashMap getExtraData() {
		return bundleClasspaths;
	}

	public Map getPatchData() {
		return patchBundles;
	}

	public List getSortedBundles() {
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

		for (Iterator iter = addedBundle.iterator(); iter.hasNext();) {
			BundleDescription added = (BundleDescription) iter.next();
			state.removeBundle(added);
		}
		addedBundle.clear();

		for (Iterator iter = unqualifiedBundles.iterator(); iter.hasNext();) {
			BundleDescription toAddBack = (BundleDescription) iter.next();
			state.removeBundle(toAddBack.getBundleId());
			addBundleDescription(toAddBack);
		}
		unqualifiedBundles.clear();

		BundleDescription[] allBundles = state.getBundles();
		for (int i = 0; i < allBundles.length; i++) {
			allBundles[i].setUserObject(null);
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
		for (int i = 0; i < resolvedBundles.length; i++) {
			if (resolvedBundles[i].getVersion().getQualifier().endsWith(PROPERTY_QUALIFIER)) {
				BundleDescription b = resolvedBundles[i];
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

	public void setPlatformProperties(Dictionary platformProperties) {
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
