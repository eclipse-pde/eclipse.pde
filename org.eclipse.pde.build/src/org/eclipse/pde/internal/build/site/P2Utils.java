/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.site;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.internal.build.ShapeAdvisor;
import org.eclipse.pde.internal.build.Utils;

/**
 * Temporary utilities until P2 and FrameworkAdmin are graduated into the SDK.
 * 
 * @since 3.4
 */
public class P2Utils {

	private static final String SRC_BUNDLE_TXT_FOLDER = "org.eclipse.equinox.source"; //$NON-NLS-1$
	private static final String BUNDLE_TXT_FOLDER = "org.eclipse.equinox.simpleconfigurator"; //$NON-NLS-1$
	private static final String SRC_BUNDLE_TXT_PATH = SRC_BUNDLE_TXT_FOLDER + "/source.info"; //$NON-NLS-1$
	public static final String BUNDLE_TXT_PATH = BUNDLE_TXT_FOLDER + "/bundles.info"; //$NON-NLS-1$

	/**
	 * Returns bundles defined by the 'bundles.info' file in the
	 * specified location, or <code>null</code> if none. The "bundles.info" file
	 * is assumed to be at a fixed relative location to the specified file.  This 
	 * method will also look for a "source.info".  If available, any source
	 * bundles found will also be added to the returned list.
	 * 
	 * @param platformHome absolute path in the local file system to an installation
	 * @return URLs of all bundles in the installation or <code>null</code> if not able
	 * 	to locate a bundles.info
	 */
	public static URL[] readBundlesTxt(String platformHome) {

		Path basePath = new Path(platformHome);

		File configArea = new File(platformHome, "configuration"); //$NON-NLS-1$
		File bundlesTxt = new File(configArea, BUNDLE_TXT_PATH);
		List bundles = getBundlesFromFile(bundlesTxt, basePath);

		if (bundles == null) {
			return null;
		}

		File srcBundlesTxt = new File(configArea, SRC_BUNDLE_TXT_PATH);
		List srcBundles = getBundlesFromFile(srcBundlesTxt, basePath);

		if (srcBundles == null) {
			return (URL[]) bundles.toArray(new URL[bundles.size()]);
		}

		bundles.addAll(srcBundles);
		return (URL[]) bundles.toArray(new URL[bundles.size()]);

	}

	/**
	 * Reads a file containing bundle entries of the following format:
	 * <pre>symbolicName,version,location,start level,expectedState</pre>
	 * Returns a list of URL locations, one for each bundle entry or <code>
	 * null</code> if there is a problem reading the file.
	 * 
	 * @param file the file to read
	 * @param basePath the path describing the base location of the platform install
	 * @return list containing URL locations or <code>null</code>
	 */
	private static List getBundlesFromFile(File file, Path basePath) {
		if (!file.exists()) {
			return null;
		}
		URL url = null;
		try {
			url = file.toURL();
		} catch (MalformedURLException e) {
			return null;
		}
		List bundles = new ArrayList();
		try {
			BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream()));

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
			return null;
		} catch (IOException e) {
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
		ShapeAdvisor advisor = new ShapeAdvisor();
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
				BundleDescription currentModel = (BundleDescription) iterator.next();
				String name = currentModel.getSymbolicName();
				String version = currentModel.getVersion().toString();

				boolean sourceBundle = Utils.isSourceBundle(currentModel);
				BufferedWriter writer = sourceBundle ? srcOut : out;
				
				writer.write(name);
				writer.write(',');
				writer.write(version);
				writer.write(",file:plugins/"); //$NON-NLS-1$
				writer.write((String)advisor.getFinalShape(currentModel)[0]);
				
				if (Utils.isSourceBundle(currentModel)) {
					writer.write(",-1,false"); //$NON-NLS-1$
				} else {
					writer.write(',');
					String currentLevel = (String) bundles.get(currentModel);
					int index = currentLevel.indexOf(':');
					String level = index > 0 ? currentLevel.substring(0, index) : "default"; //$NON-NLS-1$
					String auto = index > 0 && index < currentLevel.length() - 1 ? currentLevel.substring(index + 1) : "default"; //$NON-NLS-1$
					if ("default".equals(auto)) //$NON-NLS-1$
						auto = Boolean.toString(defaultAutoStart);
					if ("default".equals(level)) //$NON-NLS-1$
						level = Integer.toString(defaultStartLevel);
					writer.write(level);
					writer.write(',');
					writer.write(auto);
				}
				writer.newLine();
			}
			out.flush();
			out.close();
			srcOut.flush();
			srcOut.close();
		} catch (IOException e) {
			return null;
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					//ignore
				}
			}
			if (srcOut != null) {
				try {
					srcOut.close();
				} catch (IOException e) {
					//ignore
				}
			}
		}
		if (!bundlesTxt.exists()) {
			return null;
		}
		try {
			return bundlesTxt.toURL();
		} catch (MalformedURLException e) {
			return null;
		}
	}

	/**
	 * Creates a bundles.info file in the given directory containing the name,
	 * version, location, start level and expected state of every bundle in the
	 * given collection.  Will also create a source.info file containing
	 * a lsit of all source bundles found in the given collection. Uses special 
	 * defaults for the start level and auto start settings. Returns the URL 
	 * location of the bundle.txt or <code>null</code> if there was a problem 
	 * creating it.
	 * 
	 * @param bundles collection of IPluginModelBase objects to write into the bundles.info/source.info
	 * @param directory directory to create the bundles.info and source.info files in
	 * @return URL location of the bundles.info or <code>null</code>
	 */
	public static URL writeBundlesTxt(Collection bundles, File directory, boolean refactoredRuntime) {
		String defaultAppend = "default:default"; //$NON-NLS-1$
		Map bundleMap = new HashMap(bundles.size());

		for (Iterator iterator = bundles.iterator(); iterator.hasNext();) {
			BundleDescription desc = (BundleDescription) iterator.next();
			if (desc != null) {
				String modelName = desc.getSymbolicName();

				if ("org.eclipse.equinox.simpleconfigurator".equals(modelName)) { //$NON-NLS-1$
					bundleMap.put(desc, "1:true"); //$NON-NLS-1$
				} else if ("org.eclipse.equinox.common".equals(modelName)) { //$NON-NLS-1$
					bundleMap.put(desc, "2:true"); //$NON-NLS-1$
				} else if ("org.eclipse.osgi".equals(modelName)) { //$NON-NLS-1$
					bundleMap.put(desc, "-1:true"); //$NON-NLS-1$
				} else if ("org.eclipse.update.configurator".equals(modelName)) { //$NON-NLS-1$
					bundleMap.put(desc, "3:true"); //$NON-NLS-1$
				} else if ("org.eclipse.core.runtime".equals(modelName)) { //$NON-NLS-1$
					if (refactoredRuntime) {
						bundleMap.put(desc, "default:true"); //$NON-NLS-1$
					} else {
						bundleMap.put(desc, "2:true"); //$NON-NLS-1$
					}
				} else {
					bundleMap.put(desc, defaultAppend);
				}
			}
		}

		return writeBundlesTxt(bundleMap, 4, false, directory);
	}

}
