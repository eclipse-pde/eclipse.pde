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

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.plugin.PluginBase;

/**
 * Temporary utilities until P2 and FrameworkAdmin are graduated into the SDK.
 * 
 * @since 3.4
 */
public class P2Utils {

	private static final String SRC_BUNDLE_TXT_FOLDER = "org.eclipse.equinox.source"; //$NON-NLS-1$
	private static final String BUNDLE_TXT_FOLDER = "org.eclipse.equinox.simpleconfigurator"; //$NON-NLS-1$
	private static final String SRC_BUNDLE_TXT_PATH = SRC_BUNDLE_TXT_FOLDER + File.separator + "source.info"; //$NON-NLS-1$
	private static final String BUNDLE_TXT_PATH = BUNDLE_TXT_FOLDER + File.separator + "bundles.info"; //$NON-NLS-1$

	public static final String P2_FLAVOR_DEFAULT = "tooling"; //$NON-NLS-1$

	/**
	 * Returns bundles defined by the 'bundles.info' file in the
	 * specified location, or <code>null</code> if none. The "bundles.info" file
	 * is assumed to be at a fixed location relative to the configuration area URL.
	 * This method will also look for a "source.info".  If available, any source
	 * bundles found will also be added to the returned list.  If bundle URLs found
	 * in the bundles.txt are relative, they will be appended to platformHome to
	 * make them absolute.
	 * 
	 * @param platformHome absolute path in the local file system to an installation
	 * @param configurationArea url location of the configuration directory to search for bundles.info and source.info
	 * @return URLs of all bundles in the installation or <code>null</code> if not able
	 * 	to locate a bundles.info
	 */
	public static URL[] readBundlesTxt(String platformHome, URL configurationArea) {
		Path basePath = new Path(platformHome);
		if (configurationArea == null) {
			return null;
		}
		try {
			URL bundlesTxt = new URL(configurationArea.getProtocol(), configurationArea.getHost(), configurationArea.getFile().concat(BUNDLE_TXT_PATH));
			List bundles = getBundlesFromFile(bundlesTxt, basePath);

			if (bundles == null) {
				return null;
			}

			URL srcBundlesTxt = new URL(configurationArea.getProtocol(), configurationArea.getHost(), configurationArea.getFile().concat(SRC_BUNDLE_TXT_PATH));
			List srcBundles = getBundlesFromFile(srcBundlesTxt, basePath);

			if (srcBundles != null) {
				bundles.addAll(srcBundles);
			}
			return (URL[]) bundles.toArray(new URL[bundles.size()]);

		} catch (MalformedURLException e) {
			PDECore.log(e);
			return null;
		}
	}

	/**
	 * Reads a file containing bundle entries of the following format:
	 * <pre>symbolicName,version,location,start level,expectedState</pre>
	 * Returns a list of URL locations, one for each bundle entry or <code>
	 * null</code> if there is a problem reading the file.
	 * 
	 * @param file the URL of the file to read
	 * @param basePath the path describing the base location of the platform install
	 * @return list containing URL locations or <code>null</code>
	 */
	private static List getBundlesFromFile(URL fileURL, Path basePath) {
		List bundles = new ArrayList();
		try {
			BufferedReader r = new BufferedReader(new InputStreamReader(fileURL.openStream()));

			String line;
			try {
				//URL baseUrl = new URL(url, "./"); //$NON-NLS-1$
				while ((line = r.readLine()) != null) {
					if (line.startsWith("#")) //$NON-NLS-1$
						continue;
					line = line.trim();
					if (line.length() == 0)
						continue;

					// (expectedState is an integer).
					if (line.startsWith("org.eclipse.equinox.simpleconfigurator.baseUrl" + "=")) { //$NON-NLS-1$ //$NON-NLS-2$
						continue;
					}
					StringTokenizer tok = new StringTokenizer(line, ",", true); //$NON-NLS-1$
					String symbolicName = tok.nextToken();
					if (symbolicName.equals(",")) //$NON-NLS-1$
						symbolicName = null;
					else
						tok.nextToken(); // ,

					String version = tok.nextToken();
					if (version.equals(",")) //$NON-NLS-1$
						version = null;
					else
						tok.nextToken(); // ,

					String urlSt = tok.nextToken();
					if (urlSt.equals(",")) { //$NON-NLS-1$
						if (symbolicName != null && version != null)
							urlSt = symbolicName + "_" + version + ".jar"; //$NON-NLS-1$ //$NON-NLS-2$
						else
							urlSt = null;
					} else
						tok.nextToken(); // ,
					try {
						URL bundleUrl = new URL(urlSt);
						Path path = new Path(bundleUrl.getFile());
						if (!path.isAbsolute()) {
							urlSt = basePath.append(path).toOSString();
							bundleUrl = new URL("file:" + urlSt); //$NON-NLS-1$
						}
						bundles.add(bundleUrl);
					} catch (MalformedURLException e) {
						//bundles.add(getUrlInFull(urlSt, baseUrl));
						return null;
					}

					tok.nextToken(); // start level
					tok.nextToken(); // ,
					tok.nextToken(); // expected state

				}
			} catch (IOException e) {
				return null;
			} finally {
				try {
					r.close();
				} catch (IOException ex) {
					return null;
				}
			}
		} catch (MalformedURLException e) {
			PDECore.log(e);
			return null;
		} catch (IOException e) {
			PDECore.log(e);
			return null;
		}
		return bundles;
	}

	/**
	 * Creates a bundles.info file in the given directory containing the name,
	 * version, location, start level and expected state of the bundles in the
	 * launch.  Will also create a source.info containing all of the 
	 * source bundles in the launch. The map of bundles must be of the form 
	 * IModelPluginBase to a String ("StartLevel:AutoStart").  Returns the 
	 * URL location of the bundle.txt or <code>null</code> if there was a 
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
		File bundlesTxt = new File(directory, BUNDLE_TXT_PATH);
		File srcBundlesTxt = new File(directory, SRC_BUNDLE_TXT_PATH);
		BufferedWriter out = null;
		BufferedWriter srcOut = null;
		try {
			new File(directory, SRC_BUNDLE_TXT_FOLDER).mkdirs();
			new File(directory, BUNDLE_TXT_FOLDER).mkdirs();
			bundlesTxt.createNewFile();
			srcBundlesTxt.createNewFile();
			out = new BufferedWriter(new FileWriter(bundlesTxt));
			srcOut = new BufferedWriter(new FileWriter(srcBundlesTxt));
			for (Iterator iterator = bundles.keySet().iterator(); iterator.hasNext();) {
				IPluginModelBase currentModel = (IPluginModelBase) iterator.next();
				IPluginBase base = currentModel.getPluginBase();

				if (base instanceof PluginBase && ((PluginBase) base).getBundleSourceEntry() != null) {
					String modelName = base.getId();
					srcOut.write(modelName);
					srcOut.write(',');
					srcOut.write(base.getVersion());
					srcOut.write(',');
					String installLocation = currentModel.getInstallLocation();
					if (installLocation != null) {
						File location = new File(installLocation);
						try {
							srcOut.write(location.toURL().toString());
						} catch (MalformedURLException e) {
							PDECore.log(e);
						}
					} else {
						PDECore.log(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, "While creating source.info, could not find the bundle location for bundle " + currentModel)); //$NON-NLS-1$
					}
					srcOut.write(",-1,false"); //$NON-NLS-1$
					srcOut.newLine();
				} else if (base != null) {
					String modelName = base.getId();
					out.write(modelName);
					out.write(',');
					out.write(base.getVersion());
					out.write(',');
					String installLocation = currentModel.getInstallLocation();
					if (installLocation != null) {
						File location = new File(installLocation);
						try {
							out.write(location.toURL().toString());
						} catch (MalformedURLException e) {
							PDECore.log(e);
						}
					} else {
						PDECore.log(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, "While creating bundles.info, could not find the bundle location for bundle " + currentModel)); //$NON-NLS-1$
					}
					out.write(',');
					String currentLevel = (String) bundles.get(currentModel);
					int index = currentLevel.indexOf(':');
					String level = index > 0 ? currentLevel.substring(0, index) : "default"; //$NON-NLS-1$
					String auto = index > 0 && index < currentLevel.length() - 1 ? currentLevel.substring(index + 1) : "default"; //$NON-NLS-1$
					if ("default".equals(auto)) //$NON-NLS-1$
						auto = Boolean.toString(defaultAutoStart);
					if ("default".equals(level)) //$NON-NLS-1$
						level = Integer.toString(defaultStartLevel);
					out.write(level);
					out.write(',');
					out.write(auto);
					out.newLine();
				}
			}
			out.flush();
			out.close();
			srcOut.flush();
			srcOut.close();
		} catch (IOException e) {
			PDECore.logException(e);
			return null;
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
				}
			}
			if (srcOut != null) {
				try {
					srcOut.close();
				} catch (IOException e) {
				}
			}
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
				} else if ("org.eclipse.equinox.simpleconfigurator".equals(modelName)) { //$NON-NLS-1$
					bundleMap.put(currentModel, "1:true"); //$NON-NLS-1$
				} else if ("org.eclipse.equinox.common".equals(modelName)) { //$NON-NLS-1$
					bundleMap.put(currentModel, "2:true"); //$NON-NLS-1$
				} else if ("org.eclipse.osgi".equals(modelName)) { //$NON-NLS-1$
					bundleMap.put(currentModel, "-1:true"); //$NON-NLS-1$
				} else if ("org.eclipse.update.configurator".equals(modelName)) { //$NON-NLS-1$
					bundleMap.put(currentModel, "3:true"); //$NON-NLS-1$
				} else if ("org.eclipse.core.runtime".equals(modelName)) { //$NON-NLS-1$
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
