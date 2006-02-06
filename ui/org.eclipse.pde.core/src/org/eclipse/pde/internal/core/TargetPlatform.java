/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.update.configurator.ConfiguratorUtils;
import org.eclipse.update.configurator.IPlatformConfiguration;

public class TargetPlatform implements IEnvironmentVariables {

	static class LocalSite {
		private ArrayList plugins;
		private IPath path;

		public LocalSite(IPath path) {
			if (path.getDevice() != null)
				this.path = path.setDevice(path.getDevice().toUpperCase(Locale.ENGLISH));
			else
				this.path = path;
			plugins = new ArrayList();
		}

		public IPath getPath() {
			return path;
		}

		public URL getURL() throws MalformedURLException {
			return new URL("file:" + path.addTrailingSeparator().toString()); //$NON-NLS-1$
		}

		public void add(IPluginModelBase model) {
			plugins.add(model);
		}

		public String[] getRelativePluginList() {
			String[] list = new String[plugins.size()];
			for (int i = 0; i < plugins.size(); i++) {
				IPluginModelBase model = (IPluginModelBase) plugins.get(i);
				IPath location = new Path(model.getInstallLocation());
				// defect 37319
				if (location.segmentCount() > 2)
					location = location.removeFirstSegments(location.segmentCount() - 2);
				//31489 - entry must be relative
				list[i] = location.setDevice(null).makeRelative().toString();
			}
			return list;
		}
	}
	
	public static Properties getConfigIniProperties() {
		File iniFile = new File(ExternalModelManager.getEclipseHome().toOSString(), "configuration/config.ini"); //$NON-NLS-1$
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
				buffer.append("org.eclipse.core.jobs@2:start,"); //$NON-NLS-1$
				buffer.append("org.eclipse.core.runtime.compatibility.registry,"); //$NON-NLS-1$
				buffer.append("org.eclipse.equinox.registry@2:start,"); //$NON-NLS-1$
				buffer.append("org.eclipse.equinox.preferences,"); //$NON-NLS-1$
				buffer.append("org.eclipse.core.contenttype,"); //$NON-NLS-1$
			}
			buffer.append("org.eclipse.core.runtime@2:start,"); //$NON-NLS-1$
			buffer.append("org.eclipse.update.configurator@3:start"); //$NON-NLS-1$
			osgiBundles = buffer.toString();
		} else {
			osgiBundles = osgiBundles.replaceAll("\\s", ""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return osgiBundles;
	}
	
	public static void createPlatformConfigurationArea(
		Map pluginMap,
		File configDir,
		String brandingPluginID)
		throws CoreException {
		try {
			if (pluginMap.containsKey("org.eclipse.update.configurator"))   //$NON-NLS-1$
				savePlatformConfiguration(ConfiguratorUtils.getPlatformConfiguration(null),configDir, pluginMap, brandingPluginID);			
			checkPluginPropertiesConsistency(pluginMap, configDir);
		} catch (CoreException e) {
			// Rethrow
			throw e;
		} catch (Exception e) {
			// Wrap everything else in a core exception.
			String message = e.getMessage();
			if (message==null || message.length() == 0)
				message = PDECoreMessages.TargetPlatform_exceptionThrown; 
			throw new CoreException(
				new Status(
					IStatus.ERROR,
					PDECore.getPluginId(),
					IStatus.ERROR,
					message,
					e));
		}
	}
	
	private static void checkPluginPropertiesConsistency(Map map, File configDir) {
		File runtimeDir = new File(configDir, "org.eclipse.core.runtime"); //$NON-NLS-1$
		if (runtimeDir.exists() && runtimeDir.isDirectory()) {
			long timestamp = runtimeDir.lastModified();
			Iterator iter = map.values().iterator();
			while (iter.hasNext()) {
				if (hasChanged((IPluginModelBase)iter.next(), timestamp)) {
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

	private static void savePlatformConfiguration(
		IPlatformConfiguration platformConfiguration,
		File configFile,
		Map pluginMap,
		String primaryFeatureId)
		throws IOException, CoreException, MalformedURLException {
		ArrayList sites = new ArrayList();

		// Compute local sites
		Iterator iter = pluginMap.values().iterator();
		while(iter.hasNext()) {
			IPluginModelBase model = (IPluginModelBase)iter.next();
			IPath sitePath = getTransientSitePath(model);
			addToSite(sitePath, model, sites);
		}

		createConfigurationEntries(platformConfiguration,sites);
		if (primaryFeatureId != null)
			createFeatureEntries(platformConfiguration, pluginMap, primaryFeatureId);
		platformConfiguration.refresh();
		platformConfiguration.save(new URL("file:" + configFile.getPath())); //$NON-NLS-1$
	}

	private static IPath getTransientSitePath(IPluginModelBase model) {
		return new Path(model.getInstallLocation()).removeLastSegments(2);		
	}
	
	private static void addToSite(
		IPath path,
		IPluginModelBase model,
		ArrayList sites) {
		if (path.getDevice() != null)
			path = path.setDevice(path.getDevice().toUpperCase(Locale.ENGLISH));
		for (int i = 0; i < sites.size(); i++) {
			LocalSite localSite = (LocalSite) sites.get(i);
			if (localSite.getPath().equals(path)) {
				localSite.add(model);
				return;
			}
		}
		// First time - add site
		LocalSite localSite = new LocalSite(path);
		localSite.add(model);
		sites.add(localSite);
	}

	private static void createConfigurationEntries(
		IPlatformConfiguration config,
		ArrayList sites)
		throws CoreException, MalformedURLException {

		for (int i = 0; i < sites.size(); i++) {
			LocalSite localSite = (LocalSite) sites.get(i);
			String[] plugins = localSite.getRelativePluginList();

			int policy = IPlatformConfiguration.ISitePolicy.USER_INCLUDE;
			IPlatformConfiguration.ISitePolicy sitePolicy =
				config.createSitePolicy(policy, plugins);
			IPlatformConfiguration.ISiteEntry siteEntry =
				config.createSiteEntry(localSite.getURL(), sitePolicy);
			config.configureSite(siteEntry);
		}
		config.isTransient(true);
	}


	private static void createFeatureEntries(
		IPlatformConfiguration config,
		Map pluginMap,
		String brandingPluginID)
		throws MalformedURLException {

		// We have primary feature Id.
		IFeatureModel featureModel = PDECore.getDefault().getFeatureModelManager().findFeatureModel(brandingPluginID);
		if (featureModel == null)
			return;
		
		IFeature feature = featureModel.getFeature();
		String featureVersion = feature.getVersion();
		IPluginModelBase primaryPlugin = (IPluginModelBase)pluginMap.get(brandingPluginID);
		if (primaryPlugin == null)
			return;
		
		URL pluginURL = new URL("file:" + primaryPlugin.getInstallLocation()); //$NON-NLS-1$
		URL[] root = new URL[] { pluginURL };
		IPlatformConfiguration.IFeatureEntry featureEntry =
			config.createFeatureEntry(
				brandingPluginID,
				featureVersion,
				brandingPluginID,
				primaryPlugin.getPluginBase().getVersion(),
				true,
				null,
				root);
		config.configureFeatureEntry(featureEntry);
	}

	public static String getOS() {
		String value = getProperty(OS);
		return value.equals("") ? Platform.getOS() : value; //$NON-NLS-1$
	}

	public static String getWS() {
		String value = getProperty(WS);
		return value.equals("") ? Platform.getWS() : value; //$NON-NLS-1$
	}

	public static String getNL() {
		String value = getProperty(NL);
		return value.equals("") ? Platform.getNL() : value; //$NON-NLS-1$
	}

	public static String getOSArch() {
		String value = getProperty(ARCH);
		return value.equals("") ? Platform.getOSArch() : value; //$NON-NLS-1$
	}

	private static String getProperty(String key) {
		return PDECore.getDefault().getPluginPreferences().getString(key);
	}
	
	public static String[] getApplicationNames() {
		TreeSet result = new TreeSet();
		IPluginModelBase[] plugins = PDECore.getDefault().getModelManager().getPlugins();
		for (int i = 0; i < plugins.length; i++) {
			IPluginExtension[] extensions = plugins[i].getPluginBase().getExtensions();
			for (int j = 0; j < extensions.length; j++) {
				String point = extensions[j].getPoint();
				if (point != null && point.equals("org.eclipse.core.runtime.applications")) { //$NON-NLS-1$
					String id = extensions[j].getPluginBase().getId();
					if (id == null || id.trim().length() == 0 || id.startsWith("org.eclipse.pde.junit.runtime")) //$NON-NLS-1$
						continue;
					if (extensions[j].getId() != null)
						result.add(id+ "." + extensions[j].getId());					 //$NON-NLS-1$
				}
			}
		}
		return (String[])result.toArray(new String[result.size()]);
	}
	
	public static TreeSet getProductNameSet() {
		TreeSet result = new TreeSet();
		IPluginModelBase[] plugins = PDECore.getDefault().getModelManager().getPlugins();
		for (int i = 0; i < plugins.length; i++) {
			IPluginExtension[] extensions = plugins[i].getPluginBase().getExtensions();
			for (int j = 0; j < extensions.length; j++) {
				String point = extensions[j].getPoint();
				if (point != null && point.equals("org.eclipse.core.runtime.products")) {//$NON-NLS-1$
					IPluginObject[] children = extensions[j].getChildren();
					if (children.length != 1)
						continue;
					if (!"product".equals(children[0].getName())) //$NON-NLS-1$
						continue;
					String id = extensions[j].getPluginBase().getId();
					if (id == null || id.trim().length() == 0)
						continue;
					if (extensions[j].getId() != null)
						result.add(id+ "." + extensions[j].getId());					 //$NON-NLS-1$
				}
			}
		}
		return result;
	}
	
	public static String[] getProductNames() {
		TreeSet result = getProductNameSet();
		return (String[])result.toArray(new String[result.size()]);
	}
	
	public static Dictionary getTargetEnvironment() {
		Dictionary result = new Hashtable();
		result.put ("osgi.os", TargetPlatform.getOS()); //$NON-NLS-1$
		result.put ("osgi.ws", TargetPlatform.getWS()); //$NON-NLS-1$
		result.put ("osgi.nl", TargetPlatform.getNL()); //$NON-NLS-1$
		result.put ("osgi.arch", TargetPlatform.getOSArch()); //$NON-NLS-1$
		result.put("osgi.resolveOptional", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		return result;
	}

	public static String getTargetVersionString() {
		return PDECore.getDefault().getModelManager().getTargetVersion();
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
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		IPluginModelBase[] models = manager.getAllPlugins();
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
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(bundle);
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
		String[] paths = new String[models.length];
		for (int i = 0; i < models.length; i++) {
			paths[i] = models[i].getInstallLocation() + IPath.SEPARATOR + "feature.xml"; //$NON-NLS-1$
		}
		return paths;
	}

	/**
	 * Obtains product ID
	 * 
	 * @return String or null
	 */
	public static String getDefaultProduct() {
		Properties config = getConfigIniProperties();
		if (config != null) {
			String product = (String) config.get("eclipse.product"); //$NON-NLS-1$
			if (product != null && getProductNameSet().contains(product))
				return product;
		}
		Set set = getProductNameSet();
		if (set.contains("org.eclipse.sdk.ide")) //$NON-NLS-1$
			return "org.eclipse.sdk.ide"; //$NON-NLS-1$
		
		return set.contains("org.eclipse.platform.ide") ? "org.eclipse.platform.ide" : null; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public static boolean isRuntimeRefactored1() {
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		return manager.findEntry("org.eclipse.equinox.common") != null; //$NON-NLS-1$
	}
	
	public static boolean isRuntimeRefactored2() {
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		return manager.findEntry("org.eclipse.core.runtime.compatibility.registry") != null;		 //$NON-NLS-1$
	}
	
}
