/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.Path;

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
		File bundlesTxt = new File(home, "configuration" + File.separator +  //$NON-NLS-1$
				"org.eclipse.equinox.simpleconfigurator" + File.separator +  //$NON-NLS-1$
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
}
