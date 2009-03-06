/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.simpleconfigurator.manipulator.SimpleConfiguratorManipulator;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.build.BundleHelper;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.core.plugin.PluginBase;

/**
 * Utilities to read and write bundle and source information files.
 * 
 * @since 3.4
 */
public class P2Utils {

	private static final String SRC_INFO_FOLDER = "org.eclipse.equinox.source"; //$NON-NLS-1$
	private static final String BUNDLE_INFO_FOLDER = IPDEBuildConstants.BUNDLE_SIMPLE_CONFIGURATOR;
	private static final String SRC_INFO_PATH = SRC_INFO_FOLDER + File.separator + "source.info"; //$NON-NLS-1$
	private static final String BUNDLE_INFO_PATH = BUNDLE_INFO_FOLDER + File.separator + "bundles.info"; //$NON-NLS-1$

	public static final String P2_FLAVOR_DEFAULT = "tooling"; //$NON-NLS-1$

	/**
	 * Returns bundles defined by the 'bundles.info' file in the
	 * specified location, or <code>null</code> if none. The "bundles.info" file
	 * is assumed to be at a fixed location relative to the configuration area URL.
	 * This method will also look for a "source.info".  If available, any source
	 * bundles found will also be added to the returned list.  If bundle URLs found
	 * in the bundles.info are relative, they will be appended to platformHome to
	 * make them absolute.
	 * 
	 * @param platformHome absolute path in the local file system to an installation
	 * @param configurationArea url location of the configuration directory to search for bundles.info and source.info
	 * @return URLs of all bundles in the installation or <code>null</code> if not able
	 * 	to locate a bundles.info
	 */
	public static URL[] readBundlesTxt(String platformHome, URL configurationArea) {
		if (configurationArea == null) {
			return null;
		}
		try {
			BundleInfo[] bundles = readBundles(platformHome, configurationArea);
			if (bundles == null) {
				return null;
			}
			int length = bundles.length;
			BundleInfo[] srcBundles = readSourceBundles(platformHome, configurationArea);
			if (srcBundles != null) {
				length += srcBundles.length;
			}
			URL[] urls = new URL[length];
			copyURLs(urls, 0, bundles);
			if (srcBundles != null && srcBundles.length > 0) {
				copyURLs(urls, bundles.length, srcBundles);
			}
			return urls;
		} catch (MalformedURLException e) {
			PDECore.log(e);
			return null;
		}
	}

	/**
	 * Returns bundles defined by the 'bundles.info' relative to the given
	 * home and configuration area, or <code>null</code> if none.
	 * The "bundles.info" file is assumed to be at a fixed location relative to the
	 * configuration area URL.
	 * 
	 * @param platformHome absolute path in the local file system to an installation
	 * @param configurationArea url location of the configuration directory to search
	 *  for bundles.info
	 * @return all bundles in the installation or <code>null</code> if not able
	 * 	to locate a bundles.info
	 */
	public static BundleInfo[] readBundles(String platformHome, URL configurationArea) {
		IPath basePath = new Path(platformHome);
		if (configurationArea == null) {
			return null;
		}
		try {
			URL bundlesTxt = new URL(configurationArea.getProtocol(), configurationArea.getHost(), new File(configurationArea.getFile(), BUNDLE_INFO_PATH).getAbsolutePath());
			File home = basePath.toFile();
			BundleInfo bundles[] = getBundlesFromFile(bundlesTxt, home);
			if (bundles == null || bundles.length == 0) {
				return null;
			}
			return bundles;
		} catch (MalformedURLException e) {
			PDECore.log(e);
			return null;
		} catch (IOException e) {
			PDECore.log(e);
			return null;
		}
	}

	/**
	 * Returns source bundles defined by the 'source.info' file in the
	 * specified location, or <code>null</code> if none. The "source.info" file
	 * is assumed to be at a fixed location relative to the configuration area URL.
	 * 
	 * @param platformHome absolute path in the local file system to an installation
	 * @param configurationArea url location of the configuration directory to search for bundles.info and source.info
	 * @return all source bundles in the installation or <code>null</code> if not able
	 * 	to locate a source.info
	 */
	public static BundleInfo[] readSourceBundles(String platformHome, URL configurationArea) {
		IPath basePath = new Path(platformHome);
		if (configurationArea == null) {
			return null;
		}
		try {
			File home = basePath.toFile();
			URL srcBundlesTxt = new URL(configurationArea.getProtocol(), configurationArea.getHost(), configurationArea.getFile().concat(SRC_INFO_PATH));
			BundleInfo srcBundles[] = getBundlesFromFile(srcBundlesTxt, home);
			if (srcBundles == null || srcBundles.length == 0) {
				return null;
			}
			return srcBundles;
		} catch (MalformedURLException e) {
			PDECore.log(e);
			return null;
		} catch (IOException e) {
			PDECore.log(e);
			return null;
		}
	}

	/**
	 * Copies URLs from the given bundle info objects into the specified array starting at the given index.
	 * 
	 * @param dest array to copy URLs into
	 * @param start index to start copying into
	 * @param infos associated bundle infos
	 * @throws MalformedURLException
	 */
	private static void copyURLs(URL[] dest, int start, BundleInfo[] infos) throws MalformedURLException {
		for (int i = 0; i < infos.length; i++) {
			dest[start++] = new File(infos[i].getLocation()).toURL();
		}
	}

	/**
	 * Returns a list of {@link BundleInfo} for each bundle entry or <code>null</code> if there
	 * is a problem reading the file.
	 * 
	 * @param file the URL of the file to read
	 * @param home the path describing the base location of the platform install
	 * @return list containing URL locations or <code>null</code>
	 * @throws IOException 
	 */
	private static BundleInfo[] getBundlesFromFile(URL fileURL, File home) throws IOException {
		SimpleConfiguratorManipulator manipulator = (SimpleConfiguratorManipulator) PDECore.getDefault().acquireService(SimpleConfiguratorManipulator.class.getName());
		if (manipulator == null) {
			return null;
		}
		return manipulator.loadConfiguration(fileURL, home);
	}

	/**
	 * Creates a bundles.info file in the given directory containing the name,
	 * version, location, start level and expected state of the bundles in the
	 * launch.  Will also create a source.info containing all of the 
	 * source bundles in the launch. The map of bundles must be of the form 
	 * IModelPluginBase to a String ("StartLevel:AutoStart").  Returns the 
	 * URL location of the bundle.info or <code>null</code> if there was a 
	 * problem creating it.
	 * 
	 * @param bundles map containing all bundles to write to the bundles.info, maps IPluginModelBase to String ("StartLevel:AutoStart")
	 * @param defaultStartLevel start level to use when "default" is the start level
	 * @param defaultAutoStart auto start setting to use when "default" is the auto start setting
	 * @param directory configuration directory to create the files in
	 * @return URL location of the bundles.info or <code>null</code>
	 */
	public static URL writeBundlesTxt(Map bundles, int defaultStartLevel, boolean defaultAutoStart, File directory) {
		if (bundles.size() == 0) {
			return null;
		}
		List bundleInfo = new ArrayList(bundles.size());
		List sourceInfo = new ArrayList(bundles.size());
		for (Iterator iterator = bundles.keySet().iterator(); iterator.hasNext();) {
			IPluginModelBase currentModel = (IPluginModelBase) iterator.next();
			IPluginBase base = currentModel.getPluginBase();

			BundleInfo info = new BundleInfo();
			String installLocation = currentModel.getInstallLocation();
			if (installLocation != null) {
				info.setLocation(new File(installLocation).toURI());
				if (base instanceof PluginBase && ((PluginBase) base).getBundleSourceEntry() != null) {
					info.setSymbolicName(base.getId());
					info.setVersion(base.getVersion());
					info.setStartLevel(-1);
					info.setMarkedAsStarted(false);
					sourceInfo.add(info);
				} else if (base != null) {
					info.setSymbolicName(base.getId());
					info.setVersion(base.getVersion());
					String currentLevel = (String) bundles.get(currentModel);
					int index = currentLevel.indexOf(':');
					String levelString = index > 0 ? currentLevel.substring(0, index) : "default"; //$NON-NLS-1$
					String auto = index > 0 && index < currentLevel.length() - 1 ? currentLevel.substring(index + 1) : "default"; //$NON-NLS-1$
					boolean isAuto = true;
					int level = -1;
					if ("default".equals(auto)) {//$NON-NLS-1$
						isAuto = defaultAutoStart;
					} else {
						isAuto = Boolean.valueOf(auto).booleanValue();
					}
					if ("default".equals(levelString)) {//$NON-NLS-1$
						level = defaultStartLevel;
					} else {
						try {
							level = Integer.parseInt(levelString);
						} catch (NumberFormatException e) {
							PDECore.log(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, "Error writing bundles, could not parse start level for bundle " + currentModel)); //$NON-NLS-1$
						}
					}
					info.setMarkedAsStarted(isAuto);
					info.setStartLevel(level);
					bundleInfo.add(info);
				}
			} else {
				PDECore.log(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, "Error writing bundles, could not find the bundle location for bundle " + currentModel)); //$NON-NLS-1$
			}
		}

		File bundlesTxt = new File(directory, BUNDLE_INFO_PATH);
		File srcBundlesTxt = new File(directory, SRC_INFO_PATH);

		BundleInfo[] infos = (BundleInfo[]) bundleInfo.toArray(new BundleInfo[bundleInfo.size()]);
		BundleInfo[] sources = (BundleInfo[]) sourceInfo.toArray(new BundleInfo[sourceInfo.size()]);

		SimpleConfiguratorManipulator manipulator = (SimpleConfiguratorManipulator) BundleHelper.getDefault().acquireService(SimpleConfiguratorManipulator.class.getName());
		try {
			manipulator.saveConfiguration(infos, bundlesTxt, null);
			manipulator.saveConfiguration(sources, srcBundlesTxt, null);
		} catch (IOException e) {
			PDECore.logException(e);
			return null;
		}

		if (!bundlesTxt.exists()) {
			return null;
		}
		try {
			return bundlesTxt.toURL();
		} catch (MalformedURLException e) {
			PDECore.logException(e);
			return null;
		}
	}

	/**
	 * Creates a bundles.info file in the given directory containing the name,
	 * version, location, start level and expected state of every bundle in the
	 * given collection.  Will also create a source.info file containing
	 * a list of all source bundles found in the given collection. If a bundle
	 * has a specified start level in the osgi bundle list, that value is used
	 * instead of the default.  Returns the URL location of the bundle.txt or 
	 * <code>null</code> if there was a problem creating it.
	 * 
	 * @param bundles collection of IPluginModelBase objects to write into the bundles.info/source.info
	 * @param osgiBundleList comma separated list of bundles specified in a template config.ini, used to override start levels
	 * @param directory directory to create the bundles.info and source.info files in
	 * @return URL location of the bundles.info or <code>null</code>
	 */
	public static URL writeBundlesTxt(Collection bundles, String osgiBundleList, File directory) {
		// Parse the osgi bundle list for start levels
		Map osgiStartLevels = new HashMap();
		StringTokenizer tokenizer = new StringTokenizer(osgiBundleList, ","); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			int index = token.indexOf('@');
			if (index != -1) {
				String modelName = token.substring(0, index);
				String startData = token.substring(index + 1);
				index = startData.indexOf(':');
				String level = index > 0 ? startData.substring(0, index) : "default"; //$NON-NLS-1$
				String auto = index > 0 && index < startData.length() - 1 ? startData.substring(index + 1) : "default"; //$NON-NLS-1$
				if ("start".equals(auto)) { //$NON-NLS-1$
					auto = "true"; //$NON-NLS-1$
				}
				osgiStartLevels.put(modelName, level + ':' + auto);
			}
		}

		// Create a map of bundles to start levels
		String defaultAppend = "default:default"; //$NON-NLS-1$
		Map bundleMap = new HashMap(bundles.size());
		for (Iterator iterator = bundles.iterator(); iterator.hasNext();) {
			IPluginModelBase currentModel = (IPluginModelBase) iterator.next();
			BundleDescription desc = currentModel.getBundleDescription();
			if (desc != null) {
				String modelName = desc.getSymbolicName();
				if (modelName != null && osgiStartLevels.containsKey(modelName)) {
					bundleMap.put(currentModel, osgiStartLevels.get(modelName));
				} else if ("org.eclipse.equinox.ds".equals(modelName)) { //$NON-NLS-1$
					bundleMap.put(currentModel, "1:true"); //$NON-NLS-1$ 
				} else if (IPDEBuildConstants.BUNDLE_SIMPLE_CONFIGURATOR.equals(modelName)) {
					bundleMap.put(currentModel, "1:true"); //$NON-NLS-1$
				} else if (IPDEBuildConstants.BUNDLE_EQUINOX_COMMON.equals(modelName)) {
					bundleMap.put(currentModel, "2:true"); //$NON-NLS-1$
				} else if (IPDEBuildConstants.BUNDLE_OSGI.equals(modelName)) {
					bundleMap.put(currentModel, "-1:true"); //$NON-NLS-1$
				} else if (IPDEBuildConstants.BUNDLE_UPDATE_CONFIGURATOR.equals(modelName)) {
					bundleMap.put(currentModel, "3:true"); //$NON-NLS-1$
				} else if (IPDEBuildConstants.BUNDLE_CORE_RUNTIME.equals(modelName)) {
					if (TargetPlatformHelper.getTargetVersion() > 3.1) {
						bundleMap.put(currentModel, "default:true"); //$NON-NLS-1$
					} else {
						bundleMap.put(currentModel, "2:true"); //$NON-NLS-1$
					}
				} else {
					bundleMap.put(currentModel, defaultAppend);
				}
			}
		}

		return writeBundlesTxt(bundleMap, 4, false, directory);
	}

}
