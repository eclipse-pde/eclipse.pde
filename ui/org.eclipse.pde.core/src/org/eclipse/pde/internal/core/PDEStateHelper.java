/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;


public class PDEStateHelper {
	private static SAXParser parser;

	public static BundleDescription[] getDependentBundles(BundleDescription root) {
		BundleDescription[] imported = getImportedBundles(root);  // Import-Package
		BundleDescription[] required = getRequiredBundles(root);  // require-bundle <=> <import> from plugin.xml
		BundleDescription[] dependents = new BundleDescription[imported.length + required.length];
		System.arraycopy(imported, 0, dependents, 0, imported.length);
		System.arraycopy(required, 0, dependents, imported.length, required.length);
		return dependents;
	}
	public static BundleDescription[] getDependentBundlesWithFragments(BundleDescription root) {
		BundleDescription[] imported = getImportedBundles(root);
		BundleDescription[] importedByFragments = getImportedByFragments(root);
		BundleDescription[] required = getRequiredBundles(root);
		BundleDescription[] requiredByFragments = getRequiredByFragments(root);
		BundleDescription[] dependents = new BundleDescription[imported.length + importedByFragments.length + required.length + requiredByFragments.length];
		System.arraycopy(imported, 0, dependents, 0, imported.length);
		System.arraycopy(importedByFragments, 0, dependents, imported.length, importedByFragments.length);
		System.arraycopy(required, 0, dependents, imported.length + importedByFragments.length, required.length);
		System.arraycopy(requiredByFragments, 0, dependents, imported.length + importedByFragments.length + required.length, requiredByFragments.length);
		return dependents;
	}
	public static BundleDescription[] getImportedByFragments(BundleDescription root) {
		BundleDescription[] fragments = root.getFragments();
		List importedByFragments = new ArrayList();
		for (int i = 0; i < fragments.length; i++) {
			if (!fragments[i].isResolved())
				continue;
			merge(importedByFragments, getImportedBundles(fragments[i]));
		}
		BundleDescription[] result = new BundleDescription[importedByFragments.size()];
		return (BundleDescription[]) importedByFragments.toArray(result);
	}
	public static BundleDescription[] getRequiredByFragments(BundleDescription root) {
		BundleDescription[] fragments = root.getFragments();
		List importedByFragments = new ArrayList();
		for (int i = 0; i < fragments.length; i++) {
			if (!fragments[i].isResolved())
				continue;
			merge(importedByFragments, getRequiredBundles(fragments[i]));
		}
		BundleDescription[] result = new BundleDescription[importedByFragments.size()];
		return (BundleDescription[]) importedByFragments.toArray(result);
	}
	public static void merge(List source, BundleDescription[] toAdd) {
		for (int i = 0; i < toAdd.length; i++) {
			if (!source.contains(toAdd[i]))
				source.add(toAdd[i]);
		}
	}
	public static String[] getClasspath(Dictionary manifest) {
		String fullClasspath = (String) manifest.get(Constants.BUNDLE_CLASSPATH);
		String[] result = new String[0];
		try {
			if (fullClasspath != null) {
				ManifestElement[] classpathEntries = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, fullClasspath);
				result = new String[classpathEntries.length];
				for (int i = 0; i < classpathEntries.length; i++) {
					result[i] = classpathEntries[i].getValue();
				}
			}
		} catch (BundleException e) {
		}
		return result;
	}
	/**
	 * This methods return the bundleDescriptions to which imports have been
	 * bound to.
	 * 
	 * @param bundleId
	 * @param version
	 * @return
	 */
	public static BundleDescription[] getImportedBundles(BundleDescription root) {
		if (root == null)
			return new BundleDescription[0];
		ExportPackageDescription[] packages = root.getResolvedImports();
		ArrayList resolvedImports = new ArrayList(packages.length);
		for (int i = 0; i < packages.length; i++)
			if (!root.getLocation().equals(packages[i].getExporter().getLocation()) && !resolvedImports.contains(packages[i].getExporter()))
				resolvedImports.add(packages[i].getExporter());
		return (BundleDescription[]) resolvedImports.toArray(new BundleDescription[resolvedImports.size()]);
		}
	/**
	 * This methods return the bundleDescriptions to which required bundles
	 * have been bound to.
	 * 
	 * @param bundleId
	 * @param version
	 * @return
	 */
	public static BundleDescription[] getRequiredBundles(BundleDescription root) {
		if (root == null)
			return new BundleDescription[0];
		return root.getResolvedRequires();
		}
	
	public static synchronized void parseExtensions(BundleDescription desc, Element parent) {
		ZipFile jarFile = null;
		InputStream stream = null;
		try {
			String filename = desc.getHost() == null ? "plugin.xml" : "fragment.xml"; //$NON-NLS-1$ //$NON-NLS-2$
			String path = desc.getLocation();

			File file = new File(path);
			if (file.isFile()) {
				jarFile = new ZipFile(file, ZipFile.OPEN_READ);
				ZipEntry manifestEntry = jarFile.getEntry(filename); 
				if (manifestEntry != null) 
					stream = jarFile.getInputStream(manifestEntry);				
			} else if (file.isDirectory()) {
				File manifest = new File(file, filename);
				if (manifest.exists() && manifest.isFile()) {
					stream = new FileInputStream(manifest);
				}
			}
			if (stream != null)
				getParser().parse(stream, new ExtensionsHandler(parent));
			
		} catch (IOException e) {
		} catch (ParserConfigurationException e) {
		} catch (SAXException e) {
		} catch (FactoryConfigurationError e) {
		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (IOException e1) {
			}
			try {
				if (jarFile != null)
					jarFile.close();
			} catch (IOException e2) {
			}
		}
	}
	
	private static SAXParser getParser() throws ParserConfigurationException, SAXException{
		if (parser == null)
			parser = SAXParserFactory.newInstance().newSAXParser();
		return parser;
	}

}
