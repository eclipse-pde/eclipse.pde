/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.*;
import java.net.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.feature.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.update.configurator.*;

public class TargetPlatform implements IEnvironmentVariables {

	private static final String BOOT_ID = "org.eclipse.core.boot"; //$NON-NLS-1$

	static class LocalSite {
		private ArrayList plugins;
		private IPath path;

		public LocalSite(IPath path) {
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
				if (!PDECore.getDefault().getModelManager().isOSGiRuntime()) {
					location = location.append(model.isFragmentModel()
							? "fragment.xml" //$NON-NLS-1$
							: "plugin.xml"); //$NON-NLS-1$
			    }
				//31489 - entry must be relative
				list[i] = location.setDevice(null).makeRelative().toString();
			}
			return list;
		}
	}
	
	public static Properties getConfigIniProperties(String filename) {
		File iniFile = new File(ExternalModelManager.getEclipseHome().toOSString(), filename);
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

	public static String[] createPluginPath() throws CoreException {
		return createPluginPath(PDECore.getDefault().getModelManager().getPlugins());
	}

	public static String[] createPluginPath(IPluginModelBase[] models)
		throws CoreException {
		String paths[] = new String[models.length];
		for (int i = 0; i < models.length; i++) {
			paths[i] = models[i].getInstallLocation();
		}
		return paths;
	}


	public static void createPlatformConfigurationArea(
		Map pluginMap,
		File configDir,
		String brandingPluginID)
		throws CoreException {
		try {
			if (PDECore.getDefault().getModelManager().isOSGiRuntime()) {
				if (pluginMap.containsKey("org.eclipse.update.configurator")) {  //$NON-NLS-1$
					savePlatformConfiguration(ConfiguratorUtils.getPlatformConfiguration(null),configDir, pluginMap, brandingPluginID);
				}
			} else {
				savePlatformConfiguration(new PlatformConfiguration(null), new File(configDir, "platform.cfg"), pluginMap, brandingPluginID); //$NON-NLS-1$
			} 			
		} catch (CoreException e) {
			// Rethrow
			throw e;
		} catch (Exception e) {
			// Wrap everything else in a core exception.
			String message = e.getMessage();
			if (message==null || message.length() == 0)
				message = PDECore.getResourceString("TargetPlatform.exceptionThrown"); //$NON-NLS-1$
			throw new CoreException(
				new Status(
					IStatus.ERROR,
					PDECore.getPluginId(),
					IStatus.ERROR,
					message,
					e));
		}
	}
	
	public static String getBundleURL(String id, Map pluginMap) {
		IPluginModelBase model = (IPluginModelBase)pluginMap.get(id);
		if (model == null)
			return null;
		
		return "file:" + new Path(model.getInstallLocation()).addTrailingSeparator().toString(); //$NON-NLS-1$
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

		IPluginModelBase bootModel = (IPluginModelBase)pluginMap.get(BOOT_ID);	
		URL configURL = new URL("file:" + configFile.getPath()); //$NON-NLS-1$
		createConfigurationEntries(platformConfiguration, bootModel, sites);
		createFeatureEntries(platformConfiguration, pluginMap, primaryFeatureId);
		platformConfiguration.refresh();
		platformConfiguration.save(configURL);

		if (bootModel!=null) {
			String version = bootModel.getPluginBase().getVersion();
			if (version!=null) {
				PluginVersionIdentifier bootVid = new PluginVersionIdentifier(version);
				PluginVersionIdentifier breakVid = new PluginVersionIdentifier("2.0.3"); //$NON-NLS-1$
				if (breakVid.isGreaterThan(bootVid))
				// Platform configuration version changed in 2.1
				// but the same fix is in 2.0.3.
				// Must switch back to configuration 1.0 for 
				// older configurations.
				repairConfigurationVersion(configURL);
			}
		}
	}

	private static IPath getTransientSitePath(IPluginModelBase model) {
		return new Path(model.getInstallLocation()).removeLastSegments(2);		
	}
	
	private static void repairConfigurationVersion(URL url) throws IOException {
		File file = new File(url.getFile());
		if (file.exists()) {
			Properties p = new Properties();
			FileInputStream fis = new FileInputStream(file);
			p.load(fis);
			p.setProperty("version", "1.0"); //$NON-NLS-1$ //$NON-NLS-2$
			fis.close();
			FileOutputStream fos = new FileOutputStream(file);
			p.store(fos, (new Date()).toString());
			fos.close();
		}
	}

	private static void addToSite(
		IPath path,
		IPluginModelBase model,
		ArrayList sites) {
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
		IPluginModelBase bootModel,
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

		if (!PDECore.getDefault().getModelManager().isOSGiRuntime()) {
			// Set boot location
			URL bootURL = new URL("file:" + bootModel.getInstallLocation()); //$NON-NLS-1$
			config.setBootstrapPluginLocation(BOOT_ID, bootURL);
		}
		config.isTransient(true);
	}


	private static void createFeatureEntries(
		IPlatformConfiguration config,
		Map pluginMap,
		String primaryFeatureId)
		throws MalformedURLException {
		IPath targetPath = ExternalModelManager.getEclipseHome();

		if (primaryFeatureId == null)
			return;
		// We have primary feature Id.
		IFeatureModel featureModel =
			loadPrimaryFeatureModel(targetPath, primaryFeatureId);
		if (featureModel == null)
			return;
		IFeature feature = featureModel.getFeature();
		String featureVersion = feature.getVersion();
		String pluginId = primaryFeatureId;
		IPluginModelBase primaryPlugin = (IPluginModelBase)pluginMap.get(pluginId);
		if (primaryPlugin == null)
			return;
		URL pluginURL = new URL("file:" + primaryPlugin.getInstallLocation()); //$NON-NLS-1$
		URL[] root = new URL[] { pluginURL };
		IPlatformConfiguration.IFeatureEntry featureEntry =
			config.createFeatureEntry(
				primaryFeatureId,
				featureVersion,
				pluginId,
				primaryPlugin.getPluginBase().getVersion(),
				true,
				null,
				root);
		config.configureFeatureEntry(featureEntry);
		featureModel.dispose();
	}

	private static IFeatureModel loadPrimaryFeatureModel(
		IPath targetPath,
		String featureId) {
		File mainFeatureDir = targetPath.append("features").toFile(); //$NON-NLS-1$
		if (mainFeatureDir.exists() == false || !mainFeatureDir.isDirectory())
			return null;
		File[] featureDirs = mainFeatureDir.listFiles();

		PluginVersionIdentifier bestVid = null;
		File bestDir = null;

		for (int i = 0; i < featureDirs.length; i++) {
			File featureDir = featureDirs[i];
			String name = featureDir.getName();
			if (featureDir.isDirectory() && name.startsWith(featureId)) {
				int loc = name.lastIndexOf("_"); //$NON-NLS-1$
				if (loc == -1)
					continue;
				String version = name.substring(loc + 1);
				PluginVersionIdentifier vid =
					new PluginVersionIdentifier(version);
				if (bestVid == null || vid.isGreaterThan(bestVid)) {
					bestVid = vid;
					bestDir = featureDir;
				}
			}
		}
		if (bestVid == null)
			return null;
		// We have a feature and know the version
		File manifest = new File(bestDir, "feature.xml"); //$NON-NLS-1$
		ExternalFeatureModel model = new ExternalFeatureModel();
		model.setInstallLocation(bestDir.getAbsolutePath());

		InputStream stream = null;
		boolean error = false;
		try {
			stream = new FileInputStream(manifest);
			model.load(stream, false);
		} catch (Exception e) {
			error = true;
		}
		if (stream != null) {
			try {
				stream.close();
			} catch (IOException e) {
			}
		}
		if (error || !model.isLoaded())
			return null;
		return model;
	}


	public static String getOS() {
		return getProperty(OS);
	}

	public static String getWS() {
		return getProperty(WS);
	}

	public static String getNL() {
		return getProperty(NL);
	}

	public static String getOSArch() {
		return getProperty(ARCH);
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

}
