/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Les Jones <lesojones@gmail.com> - Bug 214457
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.core.util.VersionUtil;
import org.osgi.framework.*;

public class TargetPlatformHelper {

	private static String REFERENCE_PREFIX = "reference:"; //$NON-NLS-1$
	private static String FILE_URL_PREFIX = "file:"; //$NON-NLS-1$

	private static Map fCachedLocations;

	public static Properties getConfigIniProperties() {
		File iniFile = new File(TargetPlatform.getLocation(), "configuration/config.ini"); //$NON-NLS-1$
		if (!iniFile.exists())
			return null;
		Properties pini = new Properties();
		try {
			FileInputStream fis = new FileInputStream(iniFile);
			pini.load(fis);
			fis.close();
			return pini;
		} catch (IOException e) {
		}
		return null;
	}

	public static String getBundleList() {
		Properties properties = getConfigIniProperties();
		String osgiBundles = properties == null ? null : properties.getProperty("osgi.bundles"); //$NON-NLS-1$
		if (osgiBundles == null) {
			StringBuffer buffer = new StringBuffer();
			if (getTargetVersion() > 3.1) {
				buffer.append("org.eclipse.equinox.common@2:start,"); //$NON-NLS-1$
				buffer.append("org.eclipse.update.configurator@3:start,"); //$NON-NLS-1$
				buffer.append("org.eclipse.core.runtime@start"); //$NON-NLS-1$
			} else {
				buffer.append("org.eclipse.core.runtime@2:start,"); //$NON-NLS-1$
				buffer.append("org.eclipse.update.configurator@3:start"); //$NON-NLS-1$
			}
			osgiBundles = buffer.toString();
		} else {
			osgiBundles = stripPathInformation(osgiBundles);
		}
		return osgiBundles;
	}

	public static String stripPathInformation(String osgiBundles) {
		StringBuffer result = new StringBuffer();
		StringTokenizer tokenizer = new StringTokenizer(osgiBundles, ","); //$NON-NLS-1$
		while (tokenizer.hasMoreElements()) {
			String token = tokenizer.nextToken();
			int index = token.indexOf('@');

			// read up until the first @, if there
			String bundle = index > 0 ? token.substring(0, index) : token;
			bundle = bundle.trim();

			// strip [reference:][file:/] prefixes if any
			if (bundle.startsWith(REFERENCE_PREFIX) && bundle.length() > REFERENCE_PREFIX.length())
				bundle = bundle.substring(REFERENCE_PREFIX.length());
			if (bundle.startsWith(FILE_URL_PREFIX) && bundle.length() > FILE_URL_PREFIX.length())
				bundle = bundle.substring(FILE_URL_PREFIX.length());

			// if the path is relative, the last segment is the bundle symbolic name
			// Otherwise, we need to retrieve the bundle symbolic name ourselves
			IPath path = new Path(bundle);
			String id = path.isAbsolute() ? getSymbolicName(bundle) : path.lastSegment();
			if (result.length() > 0)
				result.append(","); //$NON-NLS-1$
			result.append(id != null ? id : bundle);
			if (index > -1)
				result.append(token.substring(index).trim());
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
		File runtimeDir = new File(configDir, "org.eclipse.core.runtime"); //$NON-NLS-1$
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
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory())
					continue;
				String name = files[i].getName();
				if (name.startsWith("plugin") && name.endsWith(".properties") //$NON-NLS-1$ //$NON-NLS-2$
						&& files[i].lastModified() > timestamp) {
					return true;
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
			if (id != null && !id.startsWith("org.eclipse.pde.junit.runtime")) { //$NON-NLS-1$
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
			Properties profileProps = getJavaProfileProperties(profiles[i], state);
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
		if (result.size() > 0)
			return (Dictionary[]) result.toArray(new Dictionary[result.size()]);
		return new Dictionary[] {TargetPlatformHelper.getTargetEnvironment(state)};
	}

	// We need system bundle passed here, because we are building the properties to be passed to
	// the State - it doesn't know which is the system bundle yet.
	private static Properties getJavaProfileProperties(String ee, MinimalState state) {
		BundleDescription osgiBundle = state.getState().getBundle(state.getSystemBundle(), null);
		if (osgiBundle == null)
			return null;

		File location = new File(osgiBundle.getLocation());
		String filename = ee.replace('/', '_') + ".profile"; //$NON-NLS-1$
		InputStream is = null;
		ZipFile zipFile = null;
		try {
			// find the input stream to the profile properties file
			if (location.isDirectory()) {
				File file = new File(location, filename);
				if (file.exists())
					is = new FileInputStream(file);
			} else {
				zipFile = null;
				try {
					zipFile = new ZipFile(location, ZipFile.OPEN_READ);
					ZipEntry entry = zipFile.getEntry(filename);
					if (entry != null)
						is = zipFile.getInputStream(entry);
				} catch (IOException e) {
					// nothing to do
				}
			}
			if (is != null) {
				Properties profile = new Properties();
				profile.load(is);
				return profile;
			}
		} catch (IOException e) {
			// nothing to do
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
					// nothing to do
				}
			if (zipFile != null)
				try {
					zipFile.close();
				} catch (IOException e) {
					// nothing to do
				}
		}
		return null;
	}

	public static String[] getKnownExecutionEnvironments() {
		String jreProfile = System.getProperty("pde.jreProfile"); //$NON-NLS-1$
		if (jreProfile != null && jreProfile.length() > 0) {
			if ("none".equals(jreProfile)) //$NON-NLS-1$
				return new String[0];
			return new String[] {jreProfile};
		}
		return ExecutionEnvironmentAnalyzer.getKnownExecutionEnvironments();
	}

	public static String getTargetVersionString() {
		IPluginModelBase model = PluginRegistry.findModel("org.eclipse.osgi"); //$NON-NLS-1$
		if (model == null)
			return ICoreConstants.TARGET34;

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
		}
		return ICoreConstants.TARGET34;
	}

	public static double getTargetVersion() {
		return Double.parseDouble(getTargetVersionString());
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
				list.add(location + IPath.SEPARATOR + "feature.xml"); //$NON-NLS-1$
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

}
