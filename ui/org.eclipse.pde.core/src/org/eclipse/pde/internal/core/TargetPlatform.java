/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.feature.*;
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
				location =
					location.append(
						model.isFragmentModel()
							? "fragment.xml" //$NON-NLS-1$
							: "plugin.xml"); //$NON-NLS-1$
				// defect 37319
				if (location.segmentCount() > 3)
					location = location.removeFirstSegments(location.segmentCount() - 3);
				//31489 - entry must be relative
				list[i] = location.setDevice(null).makeRelative().toString();
			}
			return list;
		}
	}

	public static File createPropertiesFile() throws CoreException {
		return createPropertiesFile(getVisibleModels(), null);
	}

	public static URL[] createPluginPath() throws CoreException {
		return createPluginPath(getVisibleModels());
	}

	public static URL[] createPluginPath(IPluginModelBase[] models)
		throws CoreException {
		URL urls[] = new URL[models.length];
		for (int i = 0; i < urls.length; i++) {
			IPluginModelBase model = models[i];
			String urlName = createURL(model);
			try {
				urls[i] = new URL(urlName);
			} catch (MalformedURLException e) {
				PDECore.logException(e);
				return new URL[0];
			}
		}
		return urls;
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

	public static File createPropertiesFile(
		IPluginModelBase[] plugins,
		IPath data)
		throws CoreException {
		try {
			String dataSuffix = createDataSuffix(data);
			IPath statePath = PDECore.getDefault().getStateLocation();
			String fileName = "plugin_path.properties"; //$NON-NLS-1$
			File dir = new File(statePath.toOSString());

			if (dataSuffix.length() > 0) {
				dir = new File(dir, dataSuffix);
				if (!dir.exists()) {
					dir.mkdir();
				}
			}
			File pluginFile = new File(dir, fileName);
			Properties properties = new Properties();

			for (int i = 0; i < plugins.length; i++) {
				IPluginModelBase curr = plugins[i];
				String key = getKey(curr);
				if (key != null)
					properties.setProperty(key, createURL(curr));
			}

			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(pluginFile);
				properties.store(fos, null);
			} finally {
				if (fos != null) {
					fos.close();
				}
			}
			return pluginFile;
		} catch (IOException e) {
			throw new CoreException(
				new Status(
					IStatus.ERROR,
					PDECore.getPluginId(),
					IStatus.ERROR,
					e.getMessage(),
					e));
		}
	}

	public static File createPlatformConfigurationArea(
		TreeMap pluginMap,
		IPath data,
		String primaryFeatureId)
		throws CoreException {
		try {
			File configDir = createWorkingDirectory(data);
			if (PDECore.getDefault().getModelManager().isOSGiRuntime()) {
				createConfigIniFile(configDir, pluginMap, primaryFeatureId);
			}
			File configFile = new File(configDir, "platform.cfg");
			savePlatformConfiguration(configFile, pluginMap, primaryFeatureId);
			return configFile;
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
	
	private static void createConfigIniFile(File configDir, TreeMap pluginMap, String primaryFeatureId) {
		File file = new File(configDir, "config.ini");
		try {
			FileOutputStream stream = new FileOutputStream(file);
			OutputStreamWriter writer = new OutputStreamWriter(stream, "8859_1");
			BufferedWriter bWriter = new BufferedWriter(writer);
			
			bWriter.write("#Eclipse Runtime Configuration File");
			bWriter.newLine();
			bWriter.write("osgi.installLocation=file:" + ExternalModelManager.getEclipseHome(null).toString());
			bWriter.newLine();
			
			String splashPath = getLocation(primaryFeatureId, pluginMap);
			if (splashPath != null) {
				bWriter.write("osgi.splashPath=" + splashPath);
				bWriter.newLine();
			}
				
			bWriter.write("osgi.framework=" + getLocation("org.eclipse.osgi", pluginMap));
			bWriter.newLine();
			
			StringBuffer buffer = new StringBuffer();
			buffer.append(getOSGiLocation("org.eclipse.osgi.services", pluginMap) + ",");
			buffer.append(getOSGiLocation("org.eclipse.osgi.util", pluginMap) + ",");
			buffer.append(getOSGiLocation("org.eclipse.core.runtime", pluginMap) + "@2,");
			buffer.append(getOSGiLocation("org.eclipse.update.configurator", pluginMap) + "@3");
			bWriter.write("osgi.bundles=" + buffer.toString());
			bWriter.newLine();
			
			bWriter.write("eof=eof");
			bWriter.flush();
			bWriter.close();
		} catch (IOException e) {
			PDECore.logException(e);
		}
	}
	
	private static String getLocation(String id, TreeMap pluginMap) {
		IPluginModelBase model = (IPluginModelBase)pluginMap.get(id);
		if (model == null)
			return null;
		
		IPath path = null;
		IResource resource = model.getUnderlyingResource();
		if (resource != null && resource.isLinked()) {
			path = resource.getLocation().removeLastSegments(1).addTrailingSeparator();
		} else {
			path = new Path(model.getInstallLocation()).addTrailingSeparator();
		}	
		return "file:" + path.toString();
	}
	
	private static String getOSGiLocation(String id, TreeMap pluginMap) {
		return "reference:" + getLocation(id, pluginMap);
	}
	
	public static File createWorkingDirectory(IPath data) {
		String dataSuffix = createDataSuffix(data);
		IPath statePath = PDECore.getDefault().getStateLocation();
		File dir = new File(statePath.toOSString());

		if (dataSuffix.length() > 0) {
			dir = new File(dir, dataSuffix);
			if (!dir.exists()) {
				dir.mkdir();
			}
		}
		return dir;		
	}

	private static void savePlatformConfiguration(
		File configFile,
		TreeMap pluginMap,
		String primaryFeatureId)
		throws IOException, CoreException, MalformedURLException {
		ArrayList sites = new ArrayList();

		// Compute local sites
		IAlternativeRuntimeSupport altRuntime = PDECore.getDefault().getRuntimeSupport();
		Iterator iter = pluginMap.values().iterator();
		while(iter.hasNext()) {
			IPluginModelBase model = (IPluginModelBase)iter.next();
			IPath sitePath = altRuntime.getTransientSitePath(model);
			addToSite(sitePath, model, sites);
		}

		IPluginModelBase bootModel = (IPluginModelBase)pluginMap.get(BOOT_ID);	
		URL configURL = new URL("file:" + configFile.getPath()); //$NON-NLS-1$
		IPlatformConfiguration platformConfiguration =
			BootLoader.getPlatformConfiguration(null);
		createConfigurationEntries(platformConfiguration, bootModel, sites);
		createFeatureEntries(platformConfiguration, pluginMap, primaryFeatureId);
		platformConfiguration.refresh();
		platformConfiguration.save(configURL);

		if (bootModel!=null) {
			String version = bootModel.getPluginBase().getVersion();
			if (version!=null) {
				PluginVersionIdentifier bootVid = new PluginVersionIdentifier(version);
				PluginVersionIdentifier breakVid = new PluginVersionIdentifier("2.0.3");
				if (breakVid.isGreaterThan(bootVid))
				// Platform configuration version changed in 2.1
				// but the same fix is in 2.0.3.
				// Must switch back to configuration 1.0 for 
				// older configurations.
				repairConfigurationVersion(configURL);
			}
		}
	}

	private static void repairConfigurationVersion(URL url) throws IOException {
		File file = new File(url.getFile());
		if (file.exists()) {
			Properties p = new Properties();
			FileInputStream fis = new FileInputStream(file);
			p.load(fis);
			p.setProperty("version", "1.0");
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
		// Moved this into alternative runtime support
		// because bundle location reported here must
		// be different
		return PDECore.getDefault().getRuntimeSupport().getPluginLocation(model);
	}

	private static void createFeatureEntries(
		IPlatformConfiguration config,
		TreeMap pluginMap,
		String primaryFeatureId)
		throws MalformedURLException {
		IPath targetPath = ExternalModelManager.getEclipseHome(null);

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

	private static String createURL(IPluginModelBase model) {
		String linkedURL = createLinkedURL(model);
		if (linkedURL != null)
			return linkedURL;
		String prefix = "file:" + model.getInstallLocation() + File.separator; //$NON-NLS-1$

		if (model instanceof IPluginModel) {
			return prefix + "plugin.xml"; //$NON-NLS-1$
		} else if (model instanceof IFragmentModel) {
			return prefix + "fragment.xml"; //$NON-NLS-1$
		} else
			return ""; //$NON-NLS-1$
	}

	private static String createLinkedURL(IPluginModelBase model) {
		IResource resource = model.getUnderlyingResource();
		if (resource == null || !resource.isLinked())
			return null;
		// linked resource - redirect
		return "file:" + resource.getLocation().toOSString(); //$NON-NLS-1$
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
