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

import org.eclipse.core.boot.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.bundle.*;
import org.eclipse.pde.internal.core.feature.*;
import org.eclipse.pde.internal.core.ibundle.*;
import org.eclipse.pde.internal.core.ifeature.*;

/**
 * @version 	1.0
 * @author
 */
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
				IPath location = getPluginLocation(model);
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

	public static String[] createPluginPath() throws CoreException {
		return createPluginPath(getVisibleModels());
	}

	public static String[] createPluginPath(IPluginModelBase[] models)
		throws CoreException {
		String paths[] = new String[models.length];
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase model = models[i];
			paths[i] = createURL(model);
		}
		return paths;
	}

	private static IPluginModelBase[] getVisibleModels() {
		Vector result = new Vector();
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		addFromList(result, manager.getPlugins());
		IPluginModelBase[] array =
			(IPluginModelBase[]) result.toArray(
				new IPluginModelBase[result.size()]);
		return array;
	}

	private static void addFromList(Vector result, IPluginModelBase[] list) {
		for (int i = 0; i < list.length; i++) {
			IPluginModelBase model = list[i];
			if (model.isEnabled())
				result.add(list[i]);
		}
	}


	public static void createPlatformConfigurationArea(
		TreeMap pluginMap,
		File configDir,
		String primaryFeatureId,
		HashMap autoStartPlugins)
		throws CoreException {
		try {
			if (PDECore.getDefault().getModelManager().isOSGiRuntime()) {
				createConfigIniFile(configDir, pluginMap, primaryFeatureId, autoStartPlugins);
				if (pluginMap.containsKey("org.eclipse.update.configurator")) {  //$NON-NLS-1$
					savePlatformConfiguration(BootLoader.getPlatformConfiguration(null),configDir, pluginMap, primaryFeatureId);
				}
			} else {
				savePlatformConfiguration(new PlatformConfiguration(null), new File(configDir, "platform.cfg"), pluginMap, primaryFeatureId); //$NON-NLS-1$
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
	
	private static void createConfigIniFile(File configDir, TreeMap pluginMap, String primaryFeatureId, HashMap autoStartPlugins) {
		if (!configDir.exists())
			configDir.mkdirs();
		File file = new File(configDir, "config.ini"); //$NON-NLS-1$
		try {
			FileOutputStream stream = new FileOutputStream(file);
			OutputStreamWriter writer = new OutputStreamWriter(stream, "8859_1"); //$NON-NLS-1$
			BufferedWriter bWriter = new BufferedWriter(writer);
			
			bWriter.write("#Eclipse Runtime Configuration File"); //$NON-NLS-1$
			bWriter.newLine();
			bWriter.write("osgi.install.area=file:" + ExternalModelManager.getEclipseHome().toString()); //$NON-NLS-1$
			bWriter.newLine();
			
			if (primaryFeatureId != null) {
				String splashPath = getBundleURL(primaryFeatureId, pluginMap);
				if (splashPath == null) {
					int index = primaryFeatureId.lastIndexOf('.');
					if (index != -1) {
						String id = primaryFeatureId.substring(0, index);
						splashPath = getBundleURL(id, pluginMap);
					}
				}
				if (splashPath != null) {
					bWriter.write("osgi.splashPath=" + splashPath); //$NON-NLS-1$
					bWriter.newLine();
				}
			}
			
			bWriter.write("osgi.configuration.cascaded=false"); //$NON-NLS-1$
			bWriter.newLine();
			
			bWriter.write("osgi.framework=" + getBundleURL("org.eclipse.osgi", pluginMap)); //$NON-NLS-1$ //$NON-NLS-2$
			bWriter.newLine();
			
			Iterator iter = autoStartPlugins.keySet().iterator();
			StringBuffer buffer = new StringBuffer();
			
			while (iter.hasNext()) {
				String id = iter.next().toString();
				String url = getBundleURL(id, pluginMap);
				if (url == null)
					continue;
				buffer.append("reference:" + url); //$NON-NLS-1$
				Integer integer = (Integer)autoStartPlugins.get(id);
				if (integer.intValue() > 0)
					buffer.append("@" + integer.intValue() + ":start"); //$NON-NLS-1$ //$NON-NLS-2$
				if (iter.hasNext())
					buffer.append(","); //$NON-NLS-1$
			}
			
			if (!autoStartPlugins.containsKey("org.eclipse.update.configurator") || //$NON-NLS-1$
					!pluginMap.containsKey("org.eclipse.update.configurator")) { //$NON-NLS-1$
				iter = pluginMap.keySet().iterator();
				while (iter.hasNext()) {
					String id = iter.next().toString();
					if ("org.eclipse.osgi".equals(id) || autoStartPlugins.containsKey(id)) //$NON-NLS-1$
						continue;
					String url = getBundleURL(id, pluginMap);
					if (url == null)
						continue;
					if (buffer.length() > 0)
						buffer.append(","); //$NON-NLS-1$
					buffer.append("reference:" + url); //$NON-NLS-1$					
				}
			}
			
			if (buffer.length() > 0) {
				bWriter.write("osgi.bundles=" + buffer.toString()); //$NON-NLS-1$
				bWriter.newLine();
			}	
			
			bWriter.write("osgi.bundles.defaultStartLevel=4"); //$NON-NLS-1$
			bWriter.newLine();
			
			bWriter.write("eof=eof"); //$NON-NLS-1$
			bWriter.flush();
			bWriter.close();
		} catch (IOException e) {
			PDECore.logException(e);
		}
	}
	
	private static String getBundleURL(String id, TreeMap pluginMap) {
		IPluginModelBase model = (IPluginModelBase)pluginMap.get(id);
		if (model == null)
			return null;
		
		String location = model.getInstallLocation();
		IResource resource = model.getUnderlyingResource();
		if (resource != null) {
			if (model instanceof IBundlePluginModelBase) {
				location = resource.getLocation().removeLastSegments(2).toString();
			} else {
				location = resource.getLocation().removeLastSegments(1).toString();
			}
		}
		return "file:" + new Path(location).addTrailingSeparator().toString(); //$NON-NLS-1$
	}
	
	private static void savePlatformConfiguration(
		IPlatformConfiguration platformConfiguration,
		File configFile,
		TreeMap pluginMap,
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
		boolean bundle=false;
		if (model instanceof BundlePluginModelBase) {
			bundle=true;
		}
		IResource resource = model.getUnderlyingResource();
		if (resource != null) {
			IPath realPath = resource.getLocation();
			return realPath.removeLastSegments(bundle?4:3);
		} 
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
			IPath bootPath = getPluginLocation(bootModel);
			URL bootURL = new URL("file:" + bootPath.toOSString()); //$NON-NLS-1$
			config.setBootstrapPluginLocation(BOOT_ID, bootURL);
		}
		config.isTransient(true);
	}

	private static IPath getPluginLocation(IPluginModelBase model) {
		String location = model.getInstallLocation();
		IResource resource = model.getUnderlyingResource();
		if (resource != null) {
			if (model instanceof IBundlePluginModelBase) {
				location = resource.getLocation().removeLastSegments(2).toOSString();
			} else {
				location = resource.getLocation().removeLastSegments(1).toOSString();
			}
		}
		return new Path(location).addTrailingSeparator();
	}

	private static void createFeatureEntries(
		IPlatformConfiguration config,
		TreeMap pluginMap,
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
		IPath pluginPath = getPluginLocation(primaryPlugin);
		URL pluginURL = new URL("file:" + pluginPath.toString()); //$NON-NLS-1$
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

	private static String getKey(IPluginModelBase model) {
		if (model.isLoaded()) {
			return model.getPluginBase().getId();
		}
		IResource resource = model.getUnderlyingResource();
		if (resource != null)
			return resource.getProject().getName();
		return model.getInstallLocation();
	}

	private static String createDataSuffix(IPath data) {
		if (data == null)
			return ""; //$NON-NLS-1$
		String suffix = data.toOSString();
		// replace file and device separators with underscores
		suffix = suffix.replace(File.separatorChar, '_');
		return suffix.replace(':', '_');
	}

	public static String createURL(IPluginModelBase model) {
		return getPluginLocation(model).addTrailingSeparator().toString();
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

	private static Choice[] getKnownChoices(String[] values) {
		Choice[] choices = new Choice[values.length];
		for (int i = 0; i < choices.length; i++) {
			choices[i] = new Choice(values[i], values[i]);
		}
		return choices;
	}

	public static Choice[] getOSChoices() {
		return getKnownChoices(BootLoader.knownOSValues());
	}

	public static Choice[] getWSChoices() {
		return getKnownChoices(BootLoader.knownWSValues());
	}

	public static Choice[] getNLChoices() {
		Locale[] locales = Locale.getAvailableLocales();
		Choice[] choices = new Choice[locales.length];
		for (int i = 0; i < locales.length; i++) {
			Locale locale = locales[i];
			choices[i] =
				new Choice(
					locale.toString(),
					locale.toString() + " - " + locale.getDisplayName()); //$NON-NLS-1$
		}
		return choices;
	}

	public static Choice[] getArchChoices() {
		return getKnownChoices(BootLoader.knownOSArchValues());
	}
}
