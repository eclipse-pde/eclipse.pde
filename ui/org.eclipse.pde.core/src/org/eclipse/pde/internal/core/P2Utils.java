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

/**
 * Temporary utilities until P2 and FrameworkAdmin are graduated into the SDK.
 * 
 * @since 3.4
 */
public class P2Utils {

	/**
	 * Returns bundles defined by the 'bundles.txt' file in the
	 * specified location, or <code>null</code> if none. The "bundles.txt" file
	 * is assumed to be at a fixed relative location to the specified file.
	 * 
	 * @param platformHome absolute path in the local file system to an installation
	 * @return URLs of all bundles in the installation or <code>null</code> if not able
	 * 	to locate a bundles.txt
	 */
	public static URL[] readBundlesTxt(String platformHome) {

		File home = new File(platformHome);
		File bundlesTxt = new File(home, "configuration" + File.separator + //$NON-NLS-1$
				"org.eclipse.equinox.simpleconfigurator" + File.separator + //$NON-NLS-1$
				"bundles.txt"); //$NON-NLS-1$
		if (!bundlesTxt.exists()) {
			return null;
		}
		URL url = null;
		try {
			url = bundlesTxt.toURL();
		} catch (MalformedURLException e) {
			return null;
		}

		Path basePath = new Path(platformHome);
		List bundles = new ArrayList();
		try {
			BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream()));

			String line;
			try {
				//URL baseUrl = new URL(url, "./"); //$NON-NLS-1$
				while ((line = r.readLine()) != null) {
					if (line.startsWith("#")) //$NON-NLS-1$
						continue;
					line = line.trim();// symbolicName,version,location,start level,expectedState
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
			return null;
		} catch (IOException e) {
			return null;
		}
		return (URL[]) bundles.toArray(new URL[bundles.size()]);
	}

	/**
	 * Creates a bundles.txt file in the given directory containing the name,
	 * version, location, start level and expected state of the bundles in the
	 * launch.  The map of bundles must be of the form IModelPluginBase to a String
	 * ("StartLevel:AutoStart").  Returns the URL location of the bundle.txt or <code>null</code>
	 * if there was a problem creating it.
	 * 
	 * @param bundles map containing all bundles to write to the bundles.txt, maps IPluginModelBase to String ("StartLevel:AutoStart")
	 * @param defaultStartLevel start level to use when "default" is the start level
	 * @param defaultAutoStart auto start setting to use when "default" is the auto start setting
	 * @param directory directory directory to create the bundles.txt file in
	 * @return URL location of the bundles.txt or <code>null</code>
	 */
	public static URL writeBundlesTxt(Map bundles, int defaultStartLevel, boolean defaultAutoStart, File directory) {
		if (bundles.size() == 0) {
			return null;
		}

		File bundlesTxt = new File(directory, "bundles.txt"); //$NON-NLS-1$
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(bundlesTxt));
			for (Iterator iterator = bundles.keySet().iterator(); iterator.hasNext();) {
				IPluginModelBase currentModel = (IPluginModelBase) iterator.next();
				IPluginBase base = currentModel.getPluginBase();
				if (base != null) {
					String modelName = base.getId();
					out.write(modelName);
					out.write(',');
					out.write(base.getVersion());
					out.write(',');
					File location = new File(currentModel.getInstallLocation());
					if (location != null) {
						try {
							out.write(location.toURL().toString());
						} catch (MalformedURLException e) {
							PDECore.log(e);
						}
					} else {
						PDECore.log(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, "While creating bundles.txt, could not find the bundle location for bundle " + currentModel)); //$NON-NLS-1$
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
	 * Creates a bundles.txt file in the given directory containing the name,
	 * version, location, start level and expected state of every bundle in the
	 * given collection.  Uses special defaults for the start level and auto start
	 * settings. Returns the URL location of the bundle.txt or <code>null</code>
	 * if there was a problem creating it.
	 * 
	 * @param bundles collection of IPluginModelBase objects to write into the bundles.txt
	 * @param directory directory to create the bundles.txt file in
	 * @return URL location of the bundles.txt or <code>null</code>
	 */
	public static URL writeBundlesTxt(Collection bundles, File directory) {
		String defaultAppend = "default:default"; //$NON-NLS-1$
		Map bundleMap = new HashMap(bundles.size());

		for (Iterator iterator = bundles.iterator(); iterator.hasNext();) {
			IPluginModelBase currentModel = (IPluginModelBase) iterator.next();
			BundleDescription desc = currentModel.getBundleDescription();
			if (desc != null) {
				String modelName = desc.getSymbolicName();

				if ("org.eclipse.equinox.simpleconfigurator".equals(modelName)) { //$NON-NLS-1$
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
