/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.osgi.framework.Constants;

/**
 * Supports target weaving (combining the target platform with workspace
 * projects to generate a woven target platform).
 * 
 * @since 3.4
 */
public class TargetWeaver {

	/**
	 * Whether the running platform is in development mode. 
	 */
	private static boolean fgIsDev = false;

	/**
	 * Location of dev.properties
	 */
	private static String fgDevPropertiesURL = null;

	/**
	 * Property file corresponding to dev.properties
	 */
	private static Properties fgDevProperties = null;

	/**
	 * Initializes system properties
	 */
	static {
		fgIsDev = Platform.inDevelopmentMode();
		if (fgIsDev) {
			fgDevPropertiesURL = System.getProperty("osgi.dev"); //$NON-NLS-1$
		}
	}

	/**
	 * Returns the dev.properties as a property store.
	 * 
	 * @return properties
	 */
	protected static Properties getDevProperties() {
		if (fgIsDev) {
			if (fgDevProperties == null) {
				fgDevProperties = new Properties();
				if (fgDevPropertiesURL != null) {
					try {
						URL url = new URL(fgDevPropertiesURL);
						String path = url.getFile();
						if (path != null && path.length() > 0) {
							File file = new File(path);
							if (file.exists()) {
								BufferedInputStream stream = null;
								try {
									stream = new BufferedInputStream(new FileInputStream(file));
									fgDevProperties.load(stream);
								} catch (FileNotFoundException e) {
									PDECore.log(e);
								} catch (IOException e) {
									PDECore.log(e);
								} finally {
									if (stream != null)
										stream.close();
								}
							}
						}
					} catch (MalformedURLException e) {
						PDECore.log(e);
					} catch (IOException e) {
						PDECore.log(e);
					}
				}
			}
			return fgDevProperties;
		}
		return null;
	}

	/**
	 * Updates the bundle class path if this manifest refers to a project in development
	 * mode from the launching workspace. 
	 * 
	 * @param manifest manifest to update
	 */
	public static void weaveManifest(Dictionary manifest) {
		if (manifest != null && fgIsDev) {
			Properties properties = getDevProperties();
			String id = (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME);
			if (id != null) {
				int index = id.indexOf(';');
				if (index != -1) {
					id = id.substring(0, index);
				}
				String property = properties.getProperty(id, null);
				if (property != null) {
					manifest.put(Constants.BUNDLE_CLASSPATH, property);
				}
			}
		}
	}

	/**
	 * When launching a secondary runtime workbench, all projects already in dev mode
	 * must continue in dev mode such that their class files are found.
	 * 
	 * @param properties dev.properties
	 */
	public static void weaveDevProperties(Properties properties) {
		if (fgIsDev) {
			Properties devProperties = getDevProperties();
			if (devProperties != null) {
				Set entries = devProperties.entrySet();
				Iterator iterator = entries.iterator();
				while (iterator.hasNext()) {
					Entry entry = (Entry) iterator.next();
					properties.setProperty((String) entry.getKey(), (String) entry.getValue());
				}
			}
		}
	}

	/**
	 * If a source annotation is pointing to a host project that is being wove, returns
	 * an empty string so that the source annotation is the root of the project. 
	 * Otherwise returns the given library name.
	 *   
	 * @param model plug-in we are attaching source for
	 * @param libraryName the standard library name
	 * @return empty string or the standard library name
	 */
	public static String getWeavedSourceLibraryName(IPluginModelBase model, String libraryName) {
		// Note that if the host project has binary-linked libraries, these libraries appear in the dev.properties file with full path names,
		// and the library name must be returned as-is.
		if (fgIsDev && !new File(libraryName).isAbsolute()) {
			Properties properties = getDevProperties();
			String id = null;
			if (model.getBundleDescription() != null) {
				id = model.getBundleDescription().getSymbolicName();
			}
			if (id != null) {
				String property = properties.getProperty(id, null);
				if (property != null) {
					return ""; //$NON-NLS-1$
				}
			}
		}
		return libraryName;
	}
}
