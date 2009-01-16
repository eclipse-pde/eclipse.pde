/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target.impl;

import java.io.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.service.pluginconversion.PluginConverter;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * A directory of bundles.
 * 
 * @since 3.5
 */
public class DirectoryBundleContainer extends AbstractBundleContainer {

	/**
	 * Constant describing the type of bundle container 
	 */
	public static final String TYPE = "Directory"; //$NON-NLS-1$

	/**
	 * Path to this container's directory in the local file system.
	 * The path may contain string substitution variables.
	 */
	private String fPath;

	/**
	 * A registry can be built to identify old school source bundles.
	 */
	private IExtensionRegistry fRegistry;

	/**
	 * Constructs a directory bundle container at the given location.
	 * 
	 * @param path directory location in the local file system, may contain string substitution variables
	 */
	public DirectoryBundleContainer(String path) {
		fPath = path;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#getHomeLocation()
	 */
	public String getHomeLocation() throws CoreException {
		return getDirectory().toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer#getType()
	 */
	public String getType() {
		return TYPE;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer#resolveAllBundles(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected BundleInfo[] resolveAllBundles(IProgressMonitor monitor) throws CoreException {
		return resolveBundles(monitor, false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer#resolveAllSourceBundles(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected BundleInfo[] resolveAllSourceBundles(IProgressMonitor monitor) throws CoreException {
		return resolveBundles(monitor, true);
	}

	/**
	 * Resolves and returns source or code bundles based on the given flag.
	 * 
	 * @param monitor progress monitor or <code>null</code>
	 * @param source whether to retrieve source bundles
	 * @return bundles
	 * @throws CoreException
	 */
	private BundleInfo[] resolveBundles(IProgressMonitor monitor, boolean source) throws CoreException {
		File dir = getDirectory();
		if (dir.exists() && dir.isDirectory()) {
			try {
				File[] files = dir.listFiles();
				SubMonitor localMonitor = SubMonitor.convert(monitor, Messages.DirectoryBundleContainer_0, files.length);
				List bundles = new ArrayList(files.length);
				for (int i = 0; i < files.length; i++) {
					if (localMonitor.isCanceled()) {
						throw new OperationCanceledException();
					}
					Map manifest = loadManifest(files[i]);
					if (manifest != null) {
						try {
							String header = (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME);
							if (header != null) {
								ManifestElement[] elements = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, header);
								if (elements != null) {
									String name = elements[0].getValue();
									if (name != null) {
										BundleInfo info = new BundleInfo();
										info.setSymbolicName(name);
										info.setLocation(files[i].toURI());
										header = (String) manifest.get(Constants.BUNDLE_VERSION);
										if (header != null) {
											elements = ManifestElement.parseHeader(Constants.BUNDLE_VERSION, header);
											if (elements != null) {
												info.setVersion(elements[0].getValue());
											}
										}
										if (source == isSourceBundle(files[i], name, manifest)) {
											bundles.add(info);
										}
									}
								}
							}
						} catch (BundleException e) {
							// ignore invalid bundles
						}
					}
					localMonitor.worked(1);
				}
				localMonitor.done();
				return (BundleInfo[]) bundles.toArray(new BundleInfo[bundles.size()]);
			} finally {
				if (fRegistry != null) {
					fRegistry.stop(this);
					fRegistry = null;
				}
			}
		}
		throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.DirectoryBundleContainer_1, dir.toString())));
	}

	/**
	 * Returns whether the given bundle is a source bundle.
	 * 
	 * @param bundle location of the bundle in the file system
	 * @param symbolicName symbolic name of the bundle
	 * @param manifest the bundle's manifest
	 * @return whether the given bundle is a source bundle
	 */
	private boolean isSourceBundle(File bundle, String symbolicName, Map manifest) {
		if (manifest.containsKey(ICoreConstants.ECLIPSE_SOURCE_BUNDLE)) {
			// this is the new source bundle identifier
			return true;
		}
		// old source bundles were never jar'd
		if (bundle.isFile()) {
			return false;
		}
		// source bundles never have a class path
		if (manifest.containsKey(Constants.BUNDLE_CLASSPATH)) {
			return false;
		}
		// check for an "org.eclipse.pde.core.source" extension 
		File pxml = new File(bundle, ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR);
		if (pxml.exists()) {
			IExtensionRegistry registry = getRegistry();
			RegistryContributor contributor = new RegistryContributor(symbolicName, symbolicName, null, null);
			try {
				registry.addContribution(new BufferedInputStream(new FileInputStream(pxml)), contributor, false, null, null, this);
				IExtension[] extensions = registry.getExtensions(contributor);
				for (int i = 0; i < extensions.length; i++) {
					IExtension extension = extensions[i];
					if (ICoreConstants.EXTENSION_POINT_SOURCE.equals(extension.getExtensionPointUniqueIdentifier())) {
						return true;
					}
				}
			} catch (FileNotFoundException e) {
			}
		}
		return false;
	}

	/**
	 * Returns an extension registry used to identify source bundles.
	 * 
	 * @return extension registry
	 */
	private IExtensionRegistry getRegistry() {
		if (fRegistry == null) {
			fRegistry = RegistryFactory.createRegistry(null, this, this);
			// contribute PDE source extension point
			String bogusDef = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<?eclipse version=\"3.2\"?>\n<plugin><extension-point id=\"source\" name=\"source\"/>\n</plugin>"; //$NON-NLS-1$
			RegistryContributor contributor = new RegistryContributor(PDECore.PLUGIN_ID, PDECore.PLUGIN_ID, null, null);
			fRegistry.addContribution(new ByteArrayInputStream(bogusDef.getBytes()), contributor, false, null, null, this);
		}
		return fRegistry;
	}

	/**
	 * Returns the directory to search for bundles in.
	 * 
	 * @return directory if unable to resolve variables in the path
	 */
	protected File getDirectory() throws CoreException {
		String path = resolveVariables(fPath);
		return new File(path);
	}

	/**
	 * Parses a bunlde's manifest into a dictionary. The bundle may be in a jar
	 * or in a directory at the specified location.
	 * 
	 * @param bundleLocation root location of the bundle
	 * @return bundle manifest dictionary or <code>null</code> if none
	 * @throws CoreException if manifest has invalid syntax
	 */
	protected Map loadManifest(File bundleLocation) throws CoreException {
		ZipFile jarFile = null;
		InputStream manifestStream = null;
		String extension = new Path(bundleLocation.getName()).getFileExtension();
		try {
			if (extension != null && extension.equals("jar") && bundleLocation.isFile()) { //$NON-NLS-1$
				jarFile = new ZipFile(bundleLocation, ZipFile.OPEN_READ);
				ZipEntry manifestEntry = jarFile.getEntry(JarFile.MANIFEST_NAME);
				if (manifestEntry != null) {
					manifestStream = jarFile.getInputStream(manifestEntry);
				}
			} else {
				File file = new File(bundleLocation, JarFile.MANIFEST_NAME);
				if (file.exists()) {
					manifestStream = new FileInputStream(file);
				} else {
					File pxml = new File(bundleLocation, ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR);
					File fxml = new File(bundleLocation, ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR);
					if (pxml.exists() || fxml.exists()) {
						// support classic non-OSGi plug-in
						PluginConverter converter = (PluginConverter) PDECore.getDefault().acquireService(PluginConverter.class.getName());
						if (converter != null) {
							try {
								Dictionary convert = converter.convertManifest(bundleLocation, false, null, false, null);
								if (convert != null) {
									Map map = new HashMap(convert.size(), 1.0f);
									Enumeration keys = convert.keys();
									while (keys.hasMoreElements()) {
										Object key = keys.nextElement();
										map.put(key, convert.get(key));
									}
									return map;
								}
							} catch (PluginConversionException e) {
								throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.DirectoryBundleContainer_2, bundleLocation.getAbsolutePath()), e));
							}
						}
					}
				}
			}
			if (manifestStream == null) {
				return null;
			}
			return ManifestElement.parseBundleManifest(manifestStream, new Hashtable(10));
		} catch (BundleException e) {
			PDECore.log(e);
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.DirectoryBundleContainer_3, bundleLocation.getAbsolutePath()), e));
		} finally {
			closeZipFileAndStream(manifestStream, jarFile);
		}
		return null;
	}

	/**
	 * Closes the stream and jar file if not <code>null</code>.
	 * 
	 * @param stream stream to close or <code>null</code>
	 * @param jarFile jar to close or <code>null</code>
	 */
	private void closeZipFileAndStream(InputStream stream, ZipFile jarFile) {
		try {
			if (stream != null) {
				stream.close();
			}
		} catch (IOException e) {
			PDECore.log(e);
		}
		try {
			if (jarFile != null) {
				jarFile.close();
			}
		} catch (IOException e) {
			PDECore.log(e);
		}
	}

}
