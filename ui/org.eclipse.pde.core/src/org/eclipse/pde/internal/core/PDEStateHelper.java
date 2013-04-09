/*******************************************************************************
 *  Copyright (c) 2003, 2013 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.service.pluginconversion.PluginConverter;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;
import org.osgi.util.tracker.ServiceTracker;

/**
 * A collection of static helper methods used when creating
 * the {@link PDEState}
 *
 */
public class PDEStateHelper {

	private static PluginConverter fConverter = null;

	/**
	 * Parses a bundle's manifest into a dictionary. The bundle may be in a jar
	 * or in a directory at the specified location.
	 * 
	 * Note: Used by org.eclipse.pde.api.tools.internal.model.BundleComponent.getManifest()
	 * and may be called without OSGi running.
	 * 
	 * @param bundleLocation root location of the bundle
	 * @return bundle manifest dictionary or <code>null</code> if none
	 */
	public static Map<String, String> loadManifest(File bundleLocation) {
		ZipFile jarFile = null;
		InputStream manifestStream = null;
		try {
			String extension = new Path(bundleLocation.getName()).getFileExtension();
			if (extension != null && bundleLocation.isFile()) {
				jarFile = new ZipFile(bundleLocation, ZipFile.OPEN_READ);
				ZipEntry manifestEntry = jarFile.getEntry(JarFile.MANIFEST_NAME);
				if (manifestEntry != null) {
					manifestStream = jarFile.getInputStream(manifestEntry);
				}
			} else {
				File file = new File(bundleLocation, JarFile.MANIFEST_NAME);
				if (file.exists())
					manifestStream = new FileInputStream(file);
			}
		} catch (IOException e) {
		}
		if (manifestStream == null)
			return null;
		try {
			return ManifestElement.parseBundleManifest(manifestStream, null);
		} catch (BundleException e) {
		} catch (IOException e) {
		} finally {
			try {
				if (jarFile != null)
					jarFile.close();
			} catch (IOException e2) {
			}
		}
		return null;
	}

	/**
	 * Uses the Equinox {@link PluginConverter} to load manifest contents of a pre-OSGi style
	 * plug-in (plug-in information stored in a plugin.xml or fragment.xml).  Will return
	 * <code>null</code> if the plugin contents could not be parsed.  Will return <code>null</code>
	 * if called from a JRE without OSGi running.
	 * 
	 * Note: Used by org.eclipse.pde.api.tools.internal.model.BundleComponent.getManifest()
	 * and may be called without OSGi running (will return <code>null</code>
	 * 
	 * @param bundleLocation root location of the bundle to load
	 * @return a map of manifest entries or <code>null</code>
	 * @throws PluginConversionException if there is a probe parsing the plugin.xml/fragment.xml contents
	 */
	public static Map<String, String> loadOldStyleManifest(File bundleLocation) throws PluginConversionException {
		PluginConverter converter = acquirePluginConverter();
		if (converter == null) {
			return null;
		}

		Dictionary<String, String> converted = converter.convertManifest(bundleLocation, false, null, false, null);
		if (converted == null) {
			return null;
		}

		Map<String, String> manifest = new HashMap<String, String>(converted.size());
		Enumeration<String> keys = converted.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			manifest.put(key, converted.get(key));
		}
		return manifest;
	}

	private static PluginConverter acquirePluginConverter() {
		if (fConverter == null) {
			PDECore activator = PDECore.getDefault();
			if (activator != null) {
				// OSGi is running so acquire the PluginConverter service 
				ServiceTracker<?, ?> tracker = new ServiceTracker<Object, Object>(activator.getBundleContext(), PluginConverter.class.getName(), null);
				tracker.open();
				fConverter = (PluginConverter) tracker.getService();
				tracker.close();
				tracker = null;
			}
		}
		return fConverter;
	}

	/**
	 * Returns the bundles that export packages imported by the given bundle
	 * via the Import-Package header
	 * 
	 * @param root the given bundle
	 * 
	 * @return an array of bundles that export packages being imported by the given bundle
	 */
	public static BundleDescription[] getImportedBundles(BundleDescription root) {
		if (root == null)
			return new BundleDescription[0];
		ExportPackageDescription[] packages = root.getResolvedImports();
		ArrayList<BundleDescription> resolvedImports = new ArrayList<BundleDescription>(packages.length);
		for (int i = 0; i < packages.length; i++)
			if (!root.getLocation().equals(packages[i].getExporter().getLocation()) && !resolvedImports.contains(packages[i].getExporter()))
				resolvedImports.add(packages[i].getExporter());
		return resolvedImports.toArray(new BundleDescription[resolvedImports.size()]);
	}

}
