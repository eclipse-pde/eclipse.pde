/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Les Jones <lesojones@gmail.com> - Bug 214457
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.core.util.VersionUtil;
import org.osgi.framework.*;

public class TargetPlatformHelper {

	public static final String REFERENCE_PREFIX = "reference:"; //$NON-NLS-1$
	public static final String PLATFORM_PREFIX = "platform:"; //$NON-NLS-1$
	public static final String FILE_URL_PREFIX = "file:"; //$NON-NLS-1$
	public static final String JAR_EXTENSION = ".jar"; //$NON-NLS-1$

	private static Map fCachedLocations;

	public static Properties getConfigIniProperties() {
		File iniFile = new File(TargetPlatform.getLocation(), "configuration/config.ini"); //$NON-NLS-1$
		if (!iniFile.exists())
			return null;
		Properties pini = new Properties();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(iniFile);
			pini.load(fis);
			fis.close();
			return pini;
		} catch (IOException e) {
			PDECore.logException(e);
		} finally {
			try {
				if (fis != null)
					fis.close();
			} catch (IOException e) {
				PDECore.logException(e);
			}
		}
		return null;
	}

	/**
	 * Returns the list of bundles in the osgi.bundles property of the 
	 * platform config ini, or a set of default bundles if the property 
	 * could not be found.
	 * @return string list of bundles
	 */
	public static String getBundleList() {
		Properties properties = getConfigIniProperties();
		String osgiBundles = properties == null ? null : properties.getProperty("osgi.bundles"); //$NON-NLS-1$
		if (osgiBundles == null) {
			osgiBundles = getDefaultBundleList();
		} else {
			osgiBundles = stripPathInformation(osgiBundles);
		}
		return osgiBundles;
	}

	/**
	 * @return the default list of bundles to use in the osgi.bundles property 
	 */
	public static String getDefaultBundleList() {
		StringBuffer buffer = new StringBuffer();
		if (getTargetVersion() > 3.1) {
			buffer.append("org.eclipse.equinox.common@2:start,"); //$NON-NLS-1$
			buffer.append("org.eclipse.update.configurator@3:start,"); //$NON-NLS-1$
			buffer.append("org.eclipse.core.runtime@start"); //$NON-NLS-1$
		} else {
			buffer.append("org.eclipse.core.runtime@2:start,"); //$NON-NLS-1$
			buffer.append("org.eclipse.update.configurator@3:start"); //$NON-NLS-1$
		}
		return buffer.toString();
	}

	/**
	 * Removes path information from the given string containing one or more comma separated
	 * osgi bundles.  Replaces escaped '\:' with ':'.  Removes, reference, platform and file
	 * prefixes.  Removes any other path information converting the location or the last
	 * segment to a bundle id.
	 * @param osgiBundles list of bundles to strip path information from (commma separated)
	 * @return list of bundles with path information stripped
	 */
	public static String stripPathInformation(String osgiBundles) {
		StringBuffer result = new StringBuffer();
		StringTokenizer tokenizer = new StringTokenizer(osgiBundles, ","); //$NON-NLS-1$
		while (tokenizer.hasMoreElements()) {
			String token = tokenizer.nextToken();
			token = token.replaceAll("\\\\:|/:", ":"); //$NON-NLS-1$ //$NON-NLS-2$

			// read up until the first @, if there
			int atIndex = token.indexOf('@');
			String bundle = atIndex > 0 ? token.substring(0, atIndex) : token;
			bundle = bundle.trim();

			// strip [reference:][platform:][file:] prefixes if any
			if (bundle.startsWith(REFERENCE_PREFIX) && bundle.length() > REFERENCE_PREFIX.length())
				bundle = bundle.substring(REFERENCE_PREFIX.length());
			if (bundle.startsWith(PLATFORM_PREFIX) && bundle.length() > PLATFORM_PREFIX.length())
				bundle = bundle.substring(PLATFORM_PREFIX.length());
			if (bundle.startsWith(FILE_URL_PREFIX) && bundle.length() > FILE_URL_PREFIX.length())
				bundle = bundle.substring(FILE_URL_PREFIX.length());

			// if the path is relative, the last segment is the bundle symbolic name
			// Otherwise, we need to retrieve the bundle symbolic name ourselves
			IPath path = new Path(bundle);
			String id = null;
			if (path.isAbsolute()) {
				id = getSymbolicName(bundle);
			}
			if (id == null) {
				id = path.lastSegment();
			}
			if (id != null) {
				int underscoreIndex = id.indexOf('_');
				if (underscoreIndex >= 0) {
					id = id.substring(0, underscoreIndex);
				}
				// TODO If a relative path is used with a non .jar extension and does not have a version, we have no way of recognizing what the symbolic name is (bug 355890) 
				if (id.endsWith(JAR_EXTENSION)) {
					id = id.substring(0, id.length() - 4);
				}
			}
			if (result.length() > 0)
				result.append(","); //$NON-NLS-1$
			result.append(id != null ? id : bundle);
			if (atIndex > -1)
				result.append(token.substring(atIndex).trim());
		}
		return result.toString();
	}

	private static synchronized String getSymbolicName(String path) {
		if (fCachedLocations == null)
			fCachedLocations = new HashMap();

		File file = new File(path);
		if (file.exists() && !fCachedLocations.containsKey(path)) {
			try {
				Dictionary dictionary = MinimalState.loadManifest(file);
				String value = (String) dictionary.get(Constants.BUNDLE_SYMBOLICNAME);
				if (value != null) {
					ManifestElement[] elements = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, value);
					String id = elements.length > 0 ? elements[0].getValue() : null;
					if (id != null)
						fCachedLocations.put(path, elements[0].getValue());
				}
			} catch (IOException e) {
			} catch (BundleException e) {
			}
		}
		return (String) fCachedLocations.get(path);
	}

	public static void checkPluginPropertiesConsistency(Map map, File configDir) {
		File runtimeDir = new File(configDir, IPDEBuildConstants.BUNDLE_CORE_RUNTIME);
		if (runtimeDir.exists() && runtimeDir.isDirectory()) {
			long timestamp = runtimeDir.lastModified();
			Iterator iter = map.values().iterator();
			while (iter.hasNext()) {
				if (hasChanged((IPluginModelBase) iter.next(), timestamp)) {
					CoreUtility.deleteContent(runtimeDir);
					break;
				}
			}
		}
	}

	private static boolean hasChanged(IPluginModelBase model, long timestamp) {
		if (model.getUnderlyingResource() != null) {
			File[] files = new File(model.getInstallLocation()).listFiles();
			if (files != null) {
				for (int i = 0; i < files.length; i++) {
					if (files[i].isDirectory())
						continue;
					String name = files[i].getName();
					if (name.startsWith(Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME) && name.endsWith(".properties") //$NON-NLS-1$
							&& files[i].lastModified() > timestamp) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public static Set getApplicationNameSet() {
		TreeSet result = new TreeSet();
		IExtension[] extensions = PDECore.getDefault().getExtensionsRegistry().findExtensions("org.eclipse.core.runtime.applications", true); //$NON-NLS-1$
		for (int i = 0; i < extensions.length; i++) {
			String id = extensions[i].getUniqueIdentifier();
			IConfigurationElement[] elements = extensions[i].getConfigurationElements();
			if (elements.length != 1)
				continue;
			String visiblity = elements[0].getAttribute("visible"); //$NON-NLS-1$
			boolean visible = visiblity == null ? true : Boolean.valueOf(visiblity).booleanValue();
			if (id != null && visible) {
				result.add(id);
			}
		}
		result.add("org.eclipse.ui.ide.workbench"); //$NON-NLS-1$
		return result;
	}

	public static String[] getApplicationNames() {
		Set result = getApplicationNameSet();
		return (String[]) result.toArray(new String[result.size()]);
	}

	public static TreeSet getProductNameSet() {
		TreeSet result = new TreeSet();
		IExtension[] extensions = PDECore.getDefault().getExtensionsRegistry().findExtensions("org.eclipse.core.runtime.products", true); //$NON-NLS-1$
		for (int i = 0; i < extensions.length; i++) {
			IConfigurationElement[] elements = extensions[i].getConfigurationElements();
			if (elements.length != 1)
				continue;
			if (!"product".equals(elements[0].getName())) //$NON-NLS-1$
				continue;
			String id = extensions[i].getUniqueIdentifier();
			if (id != null && id.trim().length() > 0)
				result.add(id);
		}
		return result;
	}

	public static String[] getProductNames() {
		TreeSet result = getProductNameSet();
		return (String[]) result.toArray(new String[result.size()]);
	}

	public static Dictionary getTargetEnvironment() {
		Dictionary result = new Hashtable();
		result.put(ICoreConstants.OSGI_OS, TargetPlatform.getOS());
		result.put(ICoreConstants.OSGI_WS, TargetPlatform.getWS());
		result.put(ICoreConstants.OSGI_NL, TargetPlatform.getNL());
		result.put(ICoreConstants.OSGI_ARCH, TargetPlatform.getOSArch());
		result.put(ICoreConstants.OSGI_RESOLVE_OPTIONAL, "true"); //$NON-NLS-1$
		result.put(ICoreConstants.OSGI_RESOLVER_MODE, "development"); //$NON-NLS-1$
		return result;
	}

	public static Dictionary getTargetEnvironment(MinimalState state) {
		Dictionary result = getTargetEnvironment();
		result.put(ICoreConstants.OSGI_SYSTEM_BUNDLE, state.getSystemBundle());
		return result;
	}

	public static Dictionary[] getPlatformProperties(String[] profiles, MinimalState state) {
		if (profiles == null || profiles.length == 0)
			return new Dictionary[] {getTargetEnvironment(state)};

		// add java profiles for those EE's that have a .profile file in the current system bundle
		ArrayList result = new ArrayList(profiles.length);
		for (int i = 0; i < profiles.length; i++) {
			IExecutionEnvironment environment = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment(profiles[i]);
			if (environment != null) {
				Properties profileProps = environment.getProfileProperties();
				if (profileProps != null) {
					Dictionary props = TargetPlatformHelper.getTargetEnvironment(state);
					String systemPackages = profileProps.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES);
					if (systemPackages != null)
						props.put(Constants.FRAMEWORK_SYSTEMPACKAGES, systemPackages);
					String ee = profileProps.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);
					if (ee != null)
						props.put(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, ee);
					result.add(props);
				}
			}
		}
		if (result.size() > 0)
			return (Dictionary[]) result.toArray(new Dictionary[result.size()]);
		return new Dictionary[] {TargetPlatformHelper.getTargetEnvironment(state)};
	}

	public static String[] getKnownExecutionEnvironments() {
		String jreProfile = System.getProperty("pde.jreProfile"); //$NON-NLS-1$
		if (jreProfile != null && jreProfile.length() > 0) {
			if ("none".equals(jreProfile)) //$NON-NLS-1$
				return new String[0];
			return new String[] {jreProfile};
		}
		IExecutionEnvironment[] environments = JavaRuntime.getExecutionEnvironmentsManager().getExecutionEnvironments();
		String[] ids = new String[environments.length];
		for (int i = 0; i < environments.length; i++) {
			ids[i] = environments[i].getId();
		}
		return ids;
	}

	/**
	 * Returns the version of Eclipse the target platform is pointing to or {@link ICoreConstants#TARGET_VERSION_LATEST}
	 * if the target platform does not contain <code>org.eclipse.osgi</code>.
	 * 
	 * @return the target version of Eclipse or the latest version PDE knows about.
	 */
	public static String getTargetVersionString() {
		IPluginModelBase model = PluginRegistry.findModel(IPDEBuildConstants.BUNDLE_OSGI);
		if (model == null)
			return ICoreConstants.TARGET_VERSION_LATEST;

		String version = model.getPluginBase().getVersion();
		if (VersionUtil.validateVersion(version).getSeverity() == IStatus.OK) {
			Version vid = new Version(version);
			int major = vid.getMajor();
			int minor = vid.getMinor();
			if (major == 3 && minor == 0)
				return ICoreConstants.TARGET30;
			if (major == 3 && minor == 1)
				return ICoreConstants.TARGET31;
			if (major == 3 && minor == 2)
				return ICoreConstants.TARGET32;
			if (major == 3 && minor == 3)
				return ICoreConstants.TARGET33;
			if (major == 3 && minor == 4)
				return ICoreConstants.TARGET34;
			if (major == 3 && minor == 5)
				return ICoreConstants.TARGET35;
			if (major == 3 && minor == 6)
				return ICoreConstants.TARGET36;
			if (major == 3 && minor == 7)
				return ICoreConstants.TARGET37;
			if (major == 3 && minor == 8)
				return ICoreConstants.TARGET38;
		}
		return ICoreConstants.TARGET_VERSION_LATEST;
	}

	/**
	 * Returns the version of Eclipse the target platform is pointing to or {@link ICoreConstants#TARGET_VERSION_LATEST}
	 * if the target platform does not contain <code>org.eclipse.osgi</code>.
	 * 
	 * @return the target version of Eclipse or the latest version PDE knows about.
	 */
	public static double getTargetVersion() {
		return Double.parseDouble(getTargetVersionString());
	}

	/**
	 * Returns the schema version to use when targetting a specific version.
	 * If <code>null</code> is* passed as the version, the current target platform's
	 * version is used (result of getTargetVersion()).
	 * @param targetVersion the plugin version being targeted or <code>null</code>
	 * @return a string version
	 */
	public static String getSchemaVersionForTargetVersion(String targetVersion) {
		double target;
		if (targetVersion == null) {
			target = getTargetVersion();
		} else {
			target = Double.parseDouble(targetVersion);
		}
		// In 3.4 the schemas changed the spelling of appInfo to appinfo to be w3c compliant, see bug 213255.
		String schemaVersion = ICoreConstants.TARGET34;
		if (target < 3.2) {
			// Default schema version is 3.0
			schemaVersion = ICoreConstants.TARGET30;
		} else if (target < 3.4) {
			// In 3.2 the way periods in ids was changed
			schemaVersion = ICoreConstants.TARGET32;
		}
		return schemaVersion;
	}

	/**
	 * Reverse engineer the target version based on a schema version.
	 * If <code>null</code> is* passed as the version, the current target platform's
	 * version is used (result of getTargetVersion()).
	 * @param schemaVersion the schema version being targeted or <code>null</code>
	 * @return a compatible target version
	 */
	public static String getTargetVersionForSchemaVersion(String schemaVersion) {
		if (schemaVersion == null) {
			return getTargetVersionString();
		}
		// In 3.4 the schemas changed the spelling of appInfo to appinfo to be w3c compliant, see bug 213255.
		if (schemaVersion.equals(ICoreConstants.TARGET30)) {
			// 3.0 schema version was good up to 3.1
			return ICoreConstants.TARGET31;
		}
		if (schemaVersion.equals(ICoreConstants.TARGET32)) {
			// 3.2 schema version was good for 3.2 and 3.3
			return ICoreConstants.TARGET33;
		}
		// otherwise, compatible with latest version
		return getTargetVersionString();
	}

	/**
	 * Gets the schema version to use for the current target platform
	 * @return String schema version for the current target platform
	 */
	public static String getSchemaVersion() {
		return getSchemaVersionForTargetVersion(null);
	}

	public static PDEState getPDEState() {
		return PDECore.getDefault().getModelManager().getState();
	}

	public static State getState() {
		return getPDEState().getState();
	}

	public static Map getPatchMap(PDEState state) {
		HashMap properties = new HashMap();
		IPluginModelBase[] models = PluginRegistry.getActiveModels();
		for (int i = 0; i < models.length; i++) {
			BundleDescription desc = models[i].getBundleDescription();
			if (desc == null)
				continue;
			Long id = new Long(desc.getBundleId());
			if (ClasspathUtilCore.hasExtensibleAPI(models[i])) {
				properties.put(id, ICoreConstants.EXTENSIBLE_API + ": true"); //$NON-NLS-1$
			} else if (ClasspathUtilCore.isPatchFragment(models[i])) {
				properties.put(id, ICoreConstants.PATCH_FRAGMENT + ": true"); //$NON-NLS-1$
			}
		}
		return properties;
	}

	public static HashMap getBundleClasspaths(PDEState state) {
		HashMap properties = new HashMap();
		BundleDescription[] bundles = state.getState().getBundles();
		for (int i = 0; i < bundles.length; i++) {
			properties.put(new Long(bundles[i].getBundleId()), getValue(bundles[i], state));
		}
		return properties;
	}

	private static String[] getValue(BundleDescription bundle, PDEState state) {
		IPluginModelBase model = PluginRegistry.findModel(bundle);
		String[] result = null;
		if (model != null) {
			IPluginLibrary[] libs = model.getPluginBase().getLibraries();
			result = new String[libs.length];
			for (int i = 0; i < libs.length; i++) {
				result[i] = libs[i].getName();
			}
		} else {
			String[] libs = state.getLibraryNames(bundle.getBundleId());
			result = new String[libs.length];
			for (int i = 0; i < libs.length; i++) {
				result[i] = libs[i];
			}
		}
		if (result.length == 0)
			return new String[] {"."}; //$NON-NLS-1$
		return result;
	}

	public static String[] getFeaturePaths() {
		IFeatureModel[] models = PDECore.getDefault().getFeatureModelManager().getModels();
		ArrayList list = new ArrayList();
		for (int i = 0; i < models.length; i++) {
			String location = models[i].getInstallLocation();
			if (location != null)
				list.add(location + IPath.SEPARATOR + ICoreConstants.FEATURE_FILENAME_DESCRIPTOR);
		}
		return (String[]) list.toArray(new String[list.size()]);
	}

	public static boolean matchesCurrentEnvironment(IPluginModelBase model) {
		BundleContext context = PDECore.getDefault().getBundleContext();
		Dictionary environment = getTargetEnvironment();
		BundleDescription bundle = model.getBundleDescription();
		String filterSpec = bundle != null ? bundle.getPlatformFilter() : null;
		try {
			return filterSpec == null || context.createFilter(filterSpec).match(environment);
		} catch (InvalidSyntaxException e) {
			return false;
		}
	}

	public static boolean usesNewApplicationModel() {
		return PluginRegistry.findModel("org.eclipse.equinox.app") != null; //$NON-NLS-1$
	}

	/**
	 * Reads and returns the VM arguments specified in the running platform's .ini file,
	 * or am empty string if none.
	 *  
	 * @return VM arguments specified in the running platform's .ini file
	 */
	public static String getIniVMArgs() {
		File installDirectory = new File(Platform.getInstallLocation().getURL().getFile());
		if (Platform.getOS().equals(Platform.OS_MACOSX))
			installDirectory = new File(installDirectory, "Eclipse.app/Contents/MacOS"); //$NON-NLS-1$
		File eclipseIniFile = new File(installDirectory, "eclipse.ini"); //$NON-NLS-1$
		BufferedReader in = null;
		StringBuffer result = new StringBuffer();
		if (eclipseIniFile.exists()) {
			try {
				in = new BufferedReader(new FileReader(eclipseIniFile));
				String str;
				boolean vmargs = false;
				while ((str = in.readLine()) != null) {
					if (vmargs) {
						if (result.length() > 0)
							result.append(" "); //$NON-NLS-1$
						result.append(str);
					}
					// start concat'ng if we have vmargs
					if (vmargs == false && str.equals("-vmargs")) //$NON-NLS-1$
						vmargs = true;
				}
			} catch (IOException e) {
				PDECore.log(e);
			} finally {
				if (in != null)
					try {
						in.close();
					} catch (IOException e) {
						PDECore.log(e);
					}
			}
		}
		return result.toString();
	}
}
