/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;

public class LaunchConfigurationHelper {
	
	public static void synchronizeManifests(ILaunchConfiguration config, File configDir) {
		try {
			String programArgs = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, 
														""); //$NON-NLS-1$
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
		File dir = new File(PDECore.getDefault().getStateLocation().toOSString(), config.getName());
		try {
			if (!config.getAttribute(IPDELauncherConstants.CONFIG_USE_DEFAULT_AREA, true)) {
				String userPath = config.getAttribute(IPDELauncherConstants.CONFIG_LOCATION, (String)null);
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
	
	public static Properties createConfigIniFile(ILaunchConfiguration configuration, String productID, Map map, File directory) throws CoreException {
		Properties properties = null;
		// if we are to generate a config.ini, start with the values in the target platform's config.ini - bug 141918
		if (configuration.getAttribute(IPDELauncherConstants.CONFIG_GENERATE_DEFAULT, true)) {
			properties = TargetPlatformHelper.getConfigIniProperties();
			// if target's config.ini does not exist, lets try to fill in default values
			if (properties == null)
				properties = new Properties();
			// if target's config.ini has the osgi.bundles header, then parse and compute the proper osgi.bundles value
			String bundleList = properties.getProperty("osgi.bundles"); //$NON-NLS-1$
			if (bundleList != null)
				properties.setProperty("osgi.bundles", computeOSGiBundles(TargetPlatformHelper.stripPathInformation(bundleList), map)); //$NON-NLS-1$
		} else {
			String templateLoc = configuration.getAttribute(IPDELauncherConstants.CONFIG_TEMPLATE_LOCATION, (String)null);
			if (templateLoc != null) {
				properties = loadFromTemplate(getSubstitutedString(templateLoc));
				// if template contains osgi.bundles, then only strip the path, do not compute the value
				String osgiBundles = properties.getProperty("osgi.bundles"); //$NON-NLS-1$
				if (osgiBundles != null)
					properties.setProperty("osgi.bundles", TargetPlatformHelper.stripPathInformation(osgiBundles)); //$NON-NLS-1$
			}
		}
		// whether we create a new config.ini or read from one as a template, we should add the required properties - bug 161265
		if (properties != null)
			addRequiredProperties(properties, productID, map);
		else
			properties = new Properties();
		setBundleLocations(map, properties);
		if (!directory.exists())
			directory.mkdirs();
		save(new File(directory, "config.ini"), properties); //$NON-NLS-1$
		return properties;
	}
	
	private static void addRequiredProperties(Properties properties, String productID, Map map) {
		if (!properties.containsKey("osgi.install.area")) //$NON-NLS-1$
			properties.setProperty("osgi.install.area", "file:" + TargetPlatform.getLocation()); //$NON-NLS-1$ //$NON-NLS-2$
		if (!properties.containsKey("osgi.configuration.cascaded")) //$NON-NLS-1$
			properties.setProperty("osgi.configuration.cascaded", "false"); //$NON-NLS-1$ //$NON-NLS-2$
		if (!properties.containsKey("osgi.framework")) //$NON-NLS-1$
			properties.setProperty("osgi.framework", "org.eclipse.osgi"); //$NON-NLS-1$ //$NON-NLS-2$
		if (productID != null && !properties.containsKey("osgi.splashPath")) //$NON-NLS-1$
			addSplashLocation(properties, productID, map);
		// if osgi.splashPath is set, try to resolve relative paths to absolute paths
		else if (properties.containsKey("osgi.splashPath")) //$NON-NLS-1$
			resolveLocationPath(properties.getProperty("osgi.splashPath"), properties, map); //$NON-NLS-1$
		if (!properties.containsKey("osgi.bundles")) //$NON-NLS-1$
			properties.setProperty("osgi.bundles", computeOSGiBundles(TargetPlatformHelper.getBundleList(), map)); //$NON-NLS-1$
		if (!properties.containsKey("osgi.bundles.defaultStartLevel")) //$NON-NLS-1$
			properties.setProperty("osgi.bundles.defaultStartLevel", "4"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	private static String computeOSGiBundles(String bundleList, Map map) {
		StringBuffer buffer = new StringBuffer();
		Set initialBundleSet = new HashSet();
		StringTokenizer tokenizer = new StringTokenizer(bundleList, ","); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			int index = token.indexOf('@');
			String id = index != -1 ? token.substring(0, index) : token;
			if (map.containsKey(id)) {
				if (buffer.length() > 0)
					buffer.append(',');
				buffer.append(id);
				if (index != -1 && index < token.length() -1)
					buffer.append(token.substring(index));				
				initialBundleSet.add(id);
			}
		}
		// if org.eclipse.update.configurator is not included (LIKE IN BASIC RCP APPLICATION), then write out all bundles in osgi.bundles - bug 170772
		if (!initialBundleSet.contains("org.eclipse.update.configurator")) { //$NON-NLS-1$
			initialBundleSet.add("org.eclipse.osgi"); //$NON-NLS-1$
			Iterator iter = map.keySet().iterator();
			while (iter.hasNext()) {
				String id = iter.next().toString();
				if (!initialBundleSet.contains(id)) {
					if (buffer.length() > 0)
						buffer.append(',');
					buffer.append(id);
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
					throw new CoreException(
						new Status(
							IStatus.ERROR,
							PDEPlugin.getPluginId(),
							IStatus.ERROR,
							message,
							e));
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

	private static void addSplashLocation(Properties properties, String productID, Map map)  {
		Properties targetConfig = TargetPlatformHelper.getConfigIniProperties(); 
		String targetProduct = targetConfig == null ? null : targetConfig.getProperty("eclipse.product"); //$NON-NLS-1$
		String targetSplash = targetConfig == null ? null : targetConfig.getProperty("osgi.splashPath"); //$NON-NLS-1$
		if (!productID.equals(targetProduct) || targetSplash == null) {
			ArrayList locations = new ArrayList();
			String plugin = getContributingPlugin(productID);
			locations.add(plugin);
			IPluginModelBase model = (IPluginModelBase)map.get(plugin);
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
			String location = (String)locations.get(i);
			if (location.startsWith("platform:/base/plugins/")) { //$NON-NLS-1$
				location = location.replaceFirst("platform:/base/plugins/", ""); //$NON-NLS-1$ //$NON-NLS-2$
			}
			String url = getBundleURL(location, map);
			if (url == null)
				continue;
			if (buffer.length() > 0)
				buffer.append(","); //$NON-NLS-1$
			buffer.append(url);
		}
		if (buffer.length() > 0)
			properties.setProperty("osgi.splashPath", buffer.toString()); //$NON-NLS-1$
	}
	
	public static String getBundleURL(String id, Map pluginMap) {
		IPluginModelBase model = (IPluginModelBase)pluginMap.get(id.trim());
		if (model == null)
			return null;
		
		return "file:" + new Path(model.getInstallLocation()).removeTrailingSeparator().toString(); //$NON-NLS-1$
	}
		
	private static void setBundleLocations(Map map, Properties properties) {
		String framework = properties.getProperty("osgi.framework"); //$NON-NLS-1$
		if (framework != null) {
			if (framework.startsWith("platform:/base/plugins/")) { //$NON-NLS-1$
				framework.replaceFirst("platform:/base/plugins/", ""); //$NON-NLS-1$ //$NON-NLS-2$
			}
			String url = getBundleURL(framework, map);
			if (url != null)
				properties.setProperty("osgi.framework", url); //$NON-NLS-1$
		}
		
		String bundles = properties.getProperty("osgi.bundles"); //$NON-NLS-1$
		if (bundles != null) {
			StringBuffer buffer = new StringBuffer();
			StringTokenizer tokenizer = new StringTokenizer(bundles, ","); //$NON-NLS-1$
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken().trim();
				String url = getBundleURL(token, map);
				int index = -1;
				if (url == null) {
					index = token.indexOf('@');
					if (index != -1)
						url = getBundleURL(token.substring(0,index), map);
					if (url == null) {
						index = token.indexOf(':');
						if (index != -1)
							url = getBundleURL(token.substring(0,index), map);
					}
				}
				if (url != null) {
					if (buffer.length() > 0) {
						buffer.append(","); //$NON-NLS-1$
					}
					buffer.append("reference:" + url); //$NON-NLS-1$
					if (index != -1)
						buffer.append(token.substring(index));
				}
			}
			properties.setProperty("osgi.bundles", buffer.toString()); //$NON-NLS-1$
		}
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
		String result = null;
		if (configuration.getAttribute(IPDELauncherConstants.USE_PRODUCT, false)) {
			result = configuration.getAttribute(IPDELauncherConstants.PRODUCT, (String)null);
		} else {
			// find the product associated with the application, and return its contributing plug-in
			String appID = configuration.getAttribute(IPDELauncherConstants.APPLICATION, TargetPlatform.getDefaultApplication());
			IPluginModelBase[] plugins = PluginRegistry.getActiveModels();
			for (int i = 0; i < plugins.length; i++) {
				String id = plugins[i].getPluginBase().getId();
				IPluginExtension[] extensions = plugins[i].getPluginBase().getExtensions();
				for (int j = 0; j < extensions.length; j++) {
					String point = extensions[j].getPoint();
					String extId = extensions[j].getId();
					if ("org.eclipse.core.runtime.products".equals(point) && extId != null) {//$NON-NLS-1$
						IPluginObject[] children = extensions[j].getChildren();
						if (children.length != 1)
							continue;
						if (!"product".equals(children[0].getName())) //$NON-NLS-1$
							continue;
						if (appID.equals(((IPluginElement)children[0]).getAttribute("application").getValue())) { //$NON-NLS-1$
							result = id + "." + extId; //$NON-NLS-1$
							break;
						}
					}
				}
			}
		}
		if (result != null)
			return result;
		
		Properties properties = TargetPlatformHelper.getConfigIniProperties();
		return properties == null ? null : properties.getProperty("eclipse.product"); //$NON-NLS-1$
	}

}
