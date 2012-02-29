/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.launching.launcher;

import java.io.*;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.launching.IPDEConstants;
import org.eclipse.pde.internal.launching.PDELaunchingPlugin;
import org.eclipse.pde.launching.IPDELauncherConstants;

/**
 * Contains helper methods for launching an Eclipse Runtime Workbench
 */
public class LaunchConfigurationHelper {

	private static final String PROP_OSGI_FRAMEWORK = "osgi.framework"; //$NON-NLS-1$
	private static final String PROP_OSGI_BUNDLES = "osgi.bundles"; //$NON-NLS-1$
	private static final String PROP_P2_DATA_AREA = "eclipse.p2.data.area"; //$NON-NLS-1$
	private static final String PROP_PRODUCT = "eclipse.product"; //$NON-NLS-1$
	private static final String PROP_APPLICATION = "eclipse.application"; //$NON-NLS-1$

	private static final String DEFAULT_PROFILE_NAME = "SelfHostingProfile"; //$NON-NLS-1$

	/**
	 * The p2 data area will be set to a directory with this name inside the configuration folder
	 */
	private static final String DEFAULT_P2_DIRECTORY = ".p2"; //$NON-NLS-1$

	public static void synchronizeManifests(ILaunchConfiguration config, File configDir) {
		try {
			String programArgs = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, ""); //$NON-NLS-1$
			if (programArgs.indexOf("-clean") != -1) //$NON-NLS-1$
				return;
		} catch (CoreException e) {
		}
		File dir = new File(configDir, "org.eclipse.osgi/manifests"); //$NON-NLS-1$
		if (dir.exists() && dir.isDirectory()) {
			PDECore.getDefault().getJavaElementChangeListener().synchronizeManifests(dir);
		}
	}

	public static File getConfigurationArea(ILaunchConfiguration config) {
		File dir = getConfigurationLocation(config);
		if (!dir.exists())
			dir.mkdirs();
		return dir;
	}

	public static File getConfigurationLocation(ILaunchConfiguration config) {
		//bug 170213 change config location if config name contains #
		String configName = config.getName();
		configName = configName.replace('#', 'h');
		File dir = new File(PDECore.getDefault().getStateLocation().toOSString(), configName);
		try {
			if (!config.getAttribute(IPDELauncherConstants.CONFIG_USE_DEFAULT_AREA, true)) {
				String userPath = config.getAttribute(IPDELauncherConstants.CONFIG_LOCATION, (String) null);
				if (userPath != null) {
					userPath = getSubstitutedString(userPath);
					dir = new File(userPath).getAbsoluteFile();
				}
			}
		} catch (CoreException e) {
		}
		return dir;
	}

	private static String getSubstitutedString(String text) throws CoreException {
		if (text == null)
			return ""; //$NON-NLS-1$
		IStringVariableManager mgr = VariablesPlugin.getDefault().getStringVariableManager();
		return mgr.performStringSubstitution(text);
	}

	/**
	 * Writes out the config.ini and other configuration files based on the bundles being launched.  This includes
	 * writing out bundles.info if the simple configurator is being used or platform.xml if update configurator
	 * is being used. 
	 * 
	 * @param configuration launch configuration
	 * @param productID id of the product being launched, may be <code>null</code>
	 * @param bundles map of bundle id to plug-in model, these are the bundles being launched
	 * @param bundlesWithStartLevels map of plug-in model to a string containing start level information
	 * @param configurationDirectory config directory where the created files will be placed
	 * @return a properties object containing the properties written out to config.ini 
	 * @throws CoreException
	 */
	public static Properties createConfigIniFile(ILaunchConfiguration configuration, String productID, Map bundles, Map bundlesWithStartLevels, File configurationDirectory) throws CoreException {
		Properties properties = null;
		// if we are to generate a config.ini, start with the values in the target platform's config.ini - bug 141918
		if (configuration.getAttribute(IPDELauncherConstants.CONFIG_GENERATE_DEFAULT, true)) {
			String appID = configuration.getAttribute(IPDELauncherConstants.APPLICATION, TargetPlatform.getDefaultApplication());
			properties = TargetPlatformHelper.getConfigIniProperties();
			// if target's config.ini does not exist, lets try to fill in default values
			if (properties == null)
				properties = new Properties();
			// clear properties only if we are NOT launching the default product or app (bug 175437, bug 315039)
			else if ((productID != null && !productID.equals(properties.get(PROP_PRODUCT)) || (appID != null && !appID.equals(properties.get(PROP_APPLICATION))))) {
				properties.clear();
			}
			// if target's config.ini has the osgi.bundles header, then parse and compute the proper osgi.bundles value
			String bundleList = properties.getProperty(PROP_OSGI_BUNDLES);
			if (bundleList != null)
				properties.setProperty(PROP_OSGI_BUNDLES, computeOSGiBundles(TargetPlatformHelper.stripPathInformation(bundleList), bundles, bundlesWithStartLevels));
		} else {
			String templateLoc = configuration.getAttribute(IPDELauncherConstants.CONFIG_TEMPLATE_LOCATION, (String) null);
			if (templateLoc != null) {
				properties = loadFromTemplate(getSubstitutedString(templateLoc));
				// if template contains osgi.bundles, then only strip the path, do not compute the value
				String osgiBundles = properties.getProperty(PROP_OSGI_BUNDLES);
				if (osgiBundles != null)
					properties.setProperty(PROP_OSGI_BUNDLES, TargetPlatformHelper.stripPathInformation(osgiBundles));
			}
		}
		// whether we create a new config.ini or read from one as a template, we should add the required properties - bug 161265
		if (properties != null) {
			addRequiredProperties(properties, productID, bundles, bundlesWithStartLevels);
		} else {
			properties = new Properties();
		}
		if (!configurationDirectory.exists()) {
			configurationDirectory.mkdirs();
		}
		String osgiBundles = properties.getProperty(PROP_OSGI_BUNDLES);
		int start = configuration.getAttribute(IPDELauncherConstants.DEFAULT_START_LEVEL, 4);
		properties.put("osgi.bundles.defaultStartLevel", Integer.toString(start)); //$NON-NLS-1$
		boolean autostart = configuration.getAttribute(IPDELauncherConstants.DEFAULT_AUTO_START, false);

		// Special processing for launching with p2 (simple configurator)
		if (osgiBundles != null && osgiBundles.indexOf(IPDEBuildConstants.BUNDLE_SIMPLE_CONFIGURATOR) != -1 && bundles.containsKey(IPDEBuildConstants.BUNDLE_SIMPLE_CONFIGURATOR)) {

			// If update configurator is set to its default start level, override it as simple/update configurators should not be autostarted together
			Object updateConfiguratorBundle = bundles.get(IPDEBuildConstants.BUNDLE_UPDATE_CONFIGURATOR);
			if (updateConfiguratorBundle != null) {
				String startLevel = (String) bundlesWithStartLevels.get(updateConfiguratorBundle);
				if (startLevel != null && startLevel.equals(BundleLauncherHelper.DEFAULT_UPDATE_CONFIGURATOR_START_LEVEL)) {
					bundlesWithStartLevels.put(updateConfiguratorBundle, "4:false"); //$NON-NLS-1$
				}
			}

			// Write out P2 files (bundles.txt)
			URL bundlesTxt = null;
			boolean usedefault = configuration.getAttribute(IPDELauncherConstants.USE_DEFAULT, true);
			boolean useFeatures = configuration.getAttribute(IPDELauncherConstants.USEFEATURES, false);
			if (usedefault || useFeatures) {
				bundlesTxt = P2Utils.writeBundlesTxt(bundlesWithStartLevels, 4, false, configurationDirectory, osgiBundles);
			} else {
				bundlesTxt = P2Utils.writeBundlesTxt(bundlesWithStartLevels, start, autostart, configurationDirectory, null);
			}

			// Add bundles.txt as p2 config data
			if (bundlesTxt != null) {
				properties.setProperty("org.eclipse.equinox.simpleconfigurator.configUrl", bundlesTxt.toString()); //$NON-NLS-1$
				// if we have simple configurator and update configurator together, ensure update doesn't reconcile
				if (bundles.get(IPDEBuildConstants.BUNDLE_UPDATE_CONFIGURATOR) != null) {
					properties.setProperty("org.eclipse.update.reconcile", "false"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}

			// Make the p2 data area in the configuration area itself, rather than a sibling of the configuration
			// area (which is a the root pde.core shared metadata area) @see bug 272810
			properties.setProperty(PROP_P2_DATA_AREA, "@config.dir/".concat(DEFAULT_P2_DIRECTORY)); //$NON-NLS-1$

			// Generate a profile to launch with, set the profile id as the default
			if (configuration.getAttribute(IPDELauncherConstants.GENERATE_PROFILE, false)) {
				String profileID = DEFAULT_PROFILE_NAME;
				File p2DataArea = new File(configurationDirectory, DEFAULT_P2_DIRECTORY);

				// Unless we are restarting an existing profile, generate/overwrite the profile
				if (!configuration.getAttribute(IPDEConstants.RESTART, false) || !P2Utils.profileExists(profileID, p2DataArea)) {
					P2Utils.createProfile(profileID, p2DataArea, bundles.values());
				}
				properties.setProperty("eclipse.p2.profile", profileID); //$NON-NLS-1$
			}
		} else {
			// Special processing for update manager (update configurator)
			String brandingId = LaunchConfigurationHelper.getContributingPlugin(productID);
			// Create a platform.xml
			TargetPlatform.createPlatformConfiguration(configurationDirectory, (IPluginModelBase[]) bundles.values().toArray(new IPluginModelBase[bundles.size()]), brandingId != null ? (IPluginModelBase) bundles.get(brandingId) : null);
		}

		setBundleLocations(bundles, properties, autostart);

		save(new File(configurationDirectory, "config.ini"), properties); //$NON-NLS-1$
		return properties;
	}

	private static void addRequiredProperties(Properties properties, String productID, Map bundles, Map bundlesWithStartLevels) {
		if (!properties.containsKey("osgi.install.area")) //$NON-NLS-1$
			properties.setProperty("osgi.install.area", "file:" + TargetPlatform.getLocation()); //$NON-NLS-1$ //$NON-NLS-2$
		if (!properties.containsKey("osgi.configuration.cascaded")) //$NON-NLS-1$
			properties.setProperty("osgi.configuration.cascaded", "false"); //$NON-NLS-1$ //$NON-NLS-2$
		if (!properties.containsKey(PROP_OSGI_FRAMEWORK))
			properties.setProperty(PROP_OSGI_FRAMEWORK, IPDEBuildConstants.BUNDLE_OSGI);
		if (!properties.containsKey("osgi.splashPath") && productID != null) //$NON-NLS-1$
			addSplashLocation(properties, productID, bundles);
		// if osgi.splashPath is set, try to resolve relative paths to absolute paths
		if (properties.containsKey("osgi.splashPath")) //$NON-NLS-1$
			resolveLocationPath(properties.getProperty("osgi.splashPath"), properties, bundles); //$NON-NLS-1$
		if (!properties.containsKey(PROP_OSGI_BUNDLES))
			properties.setProperty(PROP_OSGI_BUNDLES, computeOSGiBundles(TargetPlatform.getBundleList(), bundles, bundlesWithStartLevels));
		if (!properties.containsKey("osgi.bundles.defaultStartLevel")) //$NON-NLS-1$
			properties.setProperty("osgi.bundles.defaultStartLevel", "4"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Computes a list of osgi bundles to be put into the osgi.bundles property based
	 * on the bundles from the target platform config.ini and a map of bundles we are
	 * launching with.  The list of bundles must have already had it's path information
	 * removed.
	 * @param bundleList list of bundles without path information
	 * @param bundles map of bundle id to bundle model, contains all bundles being launched with
	 * @param bundlesWithStartLevels map of bundles of start level
	 * @return string list of osgi bundles
	 */
	private static String computeOSGiBundles(String bundleList, Map bundles, Map bundlesWithStartLevels) {

		// if p2 and only simple configurator and
		// if simple configurator isn't selected & isn't in bundle list... hack it

		// if using p2's simple configurator, a bundles.txt will be written, so we only need simple configurator in the config.ini
		if (bundles.get(IPDEBuildConstants.BUNDLE_SIMPLE_CONFIGURATOR) != null)
			return "org.eclipse.equinox.simpleconfigurator@1:start"; //$NON-NLS-1$

		StringBuffer buffer = new StringBuffer();
		Set initialBundleSet = new HashSet();
		StringTokenizer tokenizer = new StringTokenizer(bundleList, ","); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			int index = token.indexOf('@');
			String id = index != -1 ? token.substring(0, index) : token;
			if (bundles.containsKey(id)) {
				if (buffer.length() > 0)
					buffer.append(',');
				buffer.append(id);
				if (index != -1 && index < token.length() - 1)
					buffer.append(token.substring(index));
				initialBundleSet.add(id);
			}
		}

		// if org.eclipse.update.configurator is not included (LIKE IN BASIC RCP APPLICATION), then write out all bundles in osgi.bundles - bug 170772
		if (!initialBundleSet.contains(IPDEBuildConstants.BUNDLE_UPDATE_CONFIGURATOR)) {
			initialBundleSet.add(IPDEBuildConstants.BUNDLE_OSGI);
			Iterator iter = bundlesWithStartLevels.keySet().iterator();
			while (iter.hasNext()) {
				IPluginModelBase model = (IPluginModelBase) iter.next();
				String id = model.getPluginBase().getId();
				if (!initialBundleSet.contains(id)) {
					if (buffer.length() > 0)
						buffer.append(',');

					String slinfo = (String) bundlesWithStartLevels.get(model);
					buffer.append(id);
					buffer.append('@');
					buffer.append(slinfo);
				}
			}
		}
		return buffer.toString();
	}

	private static Properties loadFromTemplate(String templateLoc) throws CoreException {
		Properties properties = new Properties();
		File templateFile = new File(templateLoc);
		if (templateFile.exists() && templateFile.isFile()) {
			FileInputStream stream = null;
			try {
				stream = new FileInputStream(templateFile);
				properties.load(stream);
			} catch (Exception e) {
				String message = e.getMessage();
				if (message != null)
					throw new CoreException(new Status(IStatus.ERROR, PDELaunchingPlugin.getPluginId(), IStatus.ERROR, message, e));
			} finally {
				if (stream != null) {
					try {
						stream.close();
					} catch (IOException e) {
					}
				}
			}
		}
		return properties;
	}

	private static void addSplashLocation(Properties properties, String productID, Map map) {
		Properties targetConfig = TargetPlatformHelper.getConfigIniProperties();
		String targetProduct = targetConfig == null ? null : targetConfig.getProperty("eclipse.product"); //$NON-NLS-1$
		String targetSplash = targetConfig == null ? null : targetConfig.getProperty("osgi.splashPath"); //$NON-NLS-1$
		if (!productID.equals(targetProduct) || targetSplash == null) {
			ArrayList locations = new ArrayList();
			String plugin = getContributingPlugin(productID);
			locations.add(plugin);
			IPluginModelBase model = (IPluginModelBase) map.get(plugin);
			if (model != null) {
				BundleDescription desc = model.getBundleDescription();
				if (desc != null) {
					BundleDescription[] fragments = desc.getFragments();
					for (int i = 0; i < fragments.length; i++)
						locations.add(fragments[i].getSymbolicName());
				}
			}
			resolveLocationPath(locations, properties, map);
		} else
			resolveLocationPath(targetSplash, properties, map);
	}

	private static void resolveLocationPath(String splashPath, Properties properties, Map map) {
		ArrayList locations = new ArrayList();
		StringTokenizer tok = new StringTokenizer(splashPath, ","); //$NON-NLS-1$
		while (tok.hasMoreTokens())
			locations.add(tok.nextToken());
		resolveLocationPath(locations, properties, map);
	}

	private static void resolveLocationPath(ArrayList locations, Properties properties, Map map) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < locations.size(); i++) {
			String location = (String) locations.get(i);
			if (location.startsWith("platform:/base/plugins/")) { //$NON-NLS-1$
				location = location.replaceFirst("platform:/base/plugins/", ""); //$NON-NLS-1$ //$NON-NLS-2$
			}
			String url = getBundleURL(location, map, false);
			if (url == null)
				continue;
			if (buffer.length() > 0)
				buffer.append(","); //$NON-NLS-1$
			buffer.append(url);
		}
		if (buffer.length() > 0)
			properties.setProperty("osgi.splashPath", buffer.toString()); //$NON-NLS-1$
	}

	/**
	 * Returns a string url representing the install location of the bundle model with the
	 * specified id.  The model is obtained using the provided map.
	 * @param id the id of the bundle
	 * @param pluginMap mapping of bundle ids to bundle models
	 * @param includeReference whether to prefix the url with 'reference:'
	 * @return string url for the bundle location
	 */
	public static String getBundleURL(String id, Map pluginMap, boolean includeReference) {
		IPluginModelBase model = (IPluginModelBase) pluginMap.get(id.trim());
		return getBundleURL(model, includeReference);
	}

	/**
	 * Returns a string url representing the install location of the given bundle model
	 * @param model the model to create the url for
	 * @param includeReference whether to prefix the url with 'reference:'
	 * @return string url for bundle location
	 */
	public static String getBundleURL(IPluginModelBase model, boolean includeReference) {
		if (model == null || model.getInstallLocation() == null)
			return null;
		StringBuffer buf = new StringBuffer();
		if (includeReference) {
			buf.append(TargetPlatformHelper.REFERENCE_PREFIX);
		}
		buf.append(TargetPlatformHelper.FILE_URL_PREFIX);
		buf.append(new Path(model.getInstallLocation()).removeTrailingSeparator().toString());
		return buf.toString();
	}

	/**
	 * Use the map of bundles we are launching with to update the osgi.framework
	 * and osgi.bundles properties with the correct info.
	 * @param map map of bundles being launched (id mapped to model)
	 * @param properties properties for config.ini
	 */
	private static void setBundleLocations(Map map, Properties properties, boolean defaultAuto) {
		String framework = properties.getProperty(PROP_OSGI_FRAMEWORK);
		if (framework != null) {
			framework = TargetPlatformHelper.stripPathInformation(framework);
			String url = getBundleURL(framework, map, false);
			if (url != null)
				properties.setProperty(PROP_OSGI_FRAMEWORK, url);
		}

		String bundles = properties.getProperty(PROP_OSGI_BUNDLES);
		if (bundles != null) {
			StringBuffer buffer = new StringBuffer();
			StringTokenizer tokenizer = new StringTokenizer(bundles, ","); //$NON-NLS-1$
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken().trim();
				String url = getBundleURL(token, map, false);
				int i = -1;
				if (url == null) {
					i = token.indexOf('@');
					if (i != -1) {
						url = getBundleURL(token.substring(0, i), map, false);
					}
					if (url == null) {
						i = token.indexOf(':');
						if (i != -1)
							url = getBundleURL(token.substring(0, i), map, false);
					}
				}
				if (url != null) {
					if (buffer.length() > 0) {
						buffer.append(","); //$NON-NLS-1$
					}
					buffer.append("reference:" + url); //$NON-NLS-1$
					if (i != -1) {
						String slinfo = token.substring(i + 1);
						buffer.append(getStartData(slinfo, defaultAuto));
					}
				}
			}
			properties.setProperty(PROP_OSGI_BUNDLES, buffer.toString());
		}
	}

	/**
	 * Convenience method to parses the startData ("startLevel:autoStart"), convert it to the
	 * format expected by the OSGi bundles property, and append to a StringBuffer.
	 * @param startData data to parse ("startLevel:autoStart")
	 * @param defaultAuto default auto start setting
	 */
	public static String getStartData(String startData, boolean defaultAuto) {
		StringBuffer buffer = new StringBuffer();
		int index = startData.indexOf(':');
		String level = index > 0 ? startData.substring(0, index) : "default"; //$NON-NLS-1$
		String auto = startData;
		if (!startData.equals("start")) //$NON-NLS-1$
			auto = index >= 0 && index < startData.length() - 1 ? startData.substring(index + 1) : "default"; //$NON-NLS-1$
		if ("default".equals(auto)) //$NON-NLS-1$
			auto = Boolean.toString(defaultAuto);
		if (!level.equals("default") || "true".equals(auto) || "start".equals(auto)) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			buffer.append("@"); //$NON-NLS-1$

		if (!level.equals("default")) { //$NON-NLS-1$
			buffer.append(level);
			if ("start".equals(auto) || "true".equals(auto)) //$NON-NLS-1$ //$NON-NLS-2$
				buffer.append(":"); //$NON-NLS-1$
		}
		if ("start".equals(auto) || "true".equals(auto)) { //$NON-NLS-1$ //$NON-NLS-2$
			buffer.append("start"); //$NON-NLS-1$
		}
		return buffer.toString();
	}

	public static void save(File file, Properties properties) {
		try {
			FileOutputStream stream = new FileOutputStream(file);
			properties.store(stream, "Configuration File"); //$NON-NLS-1$
			stream.flush();
			stream.close();
		} catch (IOException e) {
			PDECore.logException(e);
		}
	}

	public static String getContributingPlugin(String productID) {
		if (productID == null)
			return null;
		int index = productID.lastIndexOf('.');
		return index == -1 ? productID : productID.substring(0, index);
	}

	public static String getProductID(ILaunchConfiguration configuration) throws CoreException {
		if (configuration.getAttribute(IPDELauncherConstants.USE_PRODUCT, false)) {
			return configuration.getAttribute(IPDELauncherConstants.PRODUCT, (String) null);
		}

		// find the product associated with the application, and return its
		// contributing plug-in
		String appID = configuration.getAttribute(IPDELauncherConstants.APPLICATION, TargetPlatform.getDefaultApplication());
		IExtension[] extensions = PDECore.getDefault().getExtensionsRegistry().findExtensions("org.eclipse.core.runtime.products", true); //$NON-NLS-1$
		for (int i = 0; i < extensions.length; i++) {
			String id = extensions[i].getUniqueIdentifier();
			if (id == null)
				continue;
			IConfigurationElement[] children = extensions[i].getConfigurationElements();
			if (children.length != 1)
				continue;
			if (!"product".equals(children[0].getName())) //$NON-NLS-1$
				continue;
			if (appID.equals(children[0].getAttribute("application"))) //$NON-NLS-1$
				return id;
		}
		return null;

	}

}
