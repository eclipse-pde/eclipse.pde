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
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.provisional.IResolvedBundle;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;
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
	 * Most recent source patch detected from an old-style source bundle extension.
	 */
	private String fSourcePath;

	/**
	 * Constructs a directory bundle container at the given location.
	 * 
	 * @param path directory location in the local file system, may contain string substitution variables
	 */
	public DirectoryBundleContainer(String path) {
		fPath = path;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer#getLocation(boolean)
	 */
	public String getLocation(boolean resolve) throws CoreException {
		if (resolve) {
			return getDirectory().toString();
		}
		return fPath;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer#getType()
	 */
	public String getType() {
		return TYPE;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer#resolveBundles(org.eclipse.pde.internal.core.target.provisional.ITargetDefinition, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IResolvedBundle[] resolveBundles(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException {
		File dir = getDirectory();
		if (dir.isDirectory()) {
			try {
				File[] files = dir.listFiles();
				SubMonitor localMonitor = SubMonitor.convert(monitor, Messages.DirectoryBundleContainer_0, files.length);
				List bundles = new ArrayList(files.length);
				for (int i = 0; i < files.length; i++) {
					if (localMonitor.isCanceled()) {
						throw new OperationCanceledException();
					}
					try {
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
											boolean source = isSourceBundle(files[i], name, manifest);
											boolean fragment = manifest.containsKey(Constants.FRAGMENT_HOST);
											ResolvedBundle rb = new ResolvedBundle(info, null, source, false, fragment);
											rb.setSourcePath(fSourcePath);
											bundles.add(rb);
										}
									}
								}
							} catch (BundleException e) {
								// ignore invalid bundles
							}
						}
					} catch (CoreException e) {
						// ignore invalid bundles
					}
					localMonitor.worked(1);
				}
				localMonitor.done();
				return (IResolvedBundle[]) bundles.toArray(new IResolvedBundle[bundles.size()]);
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
	 * Sets the last source path detected in an old-style source bundle.
	 * 
	 * @param bundle location of the bundle in the file system
	 * @param symbolicName symbolic name of the bundle
	 * @param manifest the bundle's manifest
	 * @return whether the given bundle is a source bundle
	 */
	private boolean isSourceBundle(File bundle, String symbolicName, Map manifest) {
		fSourcePath = null;
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
						IConfigurationElement[] elements = extension.getConfigurationElements();
						if (elements.length == 1) {
							fSourcePath = elements[0].getAttribute("path"); //$NON-NLS-1$
						}
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer#isContentEqual(org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer)
	 */
	public boolean isContentEqual(AbstractBundleContainer container) {
		if (container instanceof DirectoryBundleContainer) {
			DirectoryBundleContainer dbc = (DirectoryBundleContainer) container;
			return fPath.equals(dbc.fPath) && super.isContentEqual(container);
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return new StringBuffer().append("Directory ").append(fPath).append(' ').append(getIncludedBundles() == null ? "All" : Integer.toString(getIncludedBundles().length)).append(" included").toString(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#getVMArguments()
	 */
	public String[] getVMArguments() {
		return null;
	}
}
