/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.target;

import java.io.*;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.Messages;
import org.eclipse.pde.internal.core.util.ManifestUtils;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * Describes a single bundle in a target definition. Also used to represent
 * content in the target that is missing or invalid.
 * 
 * @since 3.8
 */
public class TargetBundle {

	/**
	 * Status code indicating that this target bundle represents a required plug-in that is missing from a target definition
	 */
	public static final int STATUS_PLUGIN_DOES_NOT_EXIST = 100;

	/**
	 * Status code indicating that this target bundle represents a required feature that is missing from a target definition
	 */
	public static final int STATUS_FEATURE_DOES_NOT_EXIST = 110;

	/**
	 * Status code indicating that a required bundle version does not exist (a bundle
	 * with the correct symbolic name is present, but the specified version was not
	 * found).
	 */
	public static final int STATUS_VERSION_DOES_NOT_EXIST = 101;

	/**
	 * Status code indicating that a bundle's manifest could not be read, or did not exist. 
	 */
	public static final int STATUS_INVALID_MANIFEST = 102;

	protected BundleInfo fInfo;
	protected boolean fIsFragment = false;
	protected BundleInfo fSourceTarget;
	protected String fSourcePath = null;

	/**
	 * Constructs a target bundle for a local bundle.  The bundle may be a directory or
	 * an archive file. The manifest of the bundle will be read to collect the additional
	 * information.
	 * 
	 * @param bundleLocation the location of the bundle (directory or archive) to open
	 * @throws CoreException if there is a problem opening the bundle or its manifest
	 */
	public TargetBundle(File bundleLocation) throws CoreException {
		initialize(bundleLocation);
	}

	/**
	 * Constructs an empty target bundle with no information.
	 */
	protected TargetBundle() {
		fInfo = new BundleInfo();
	}

	/**
	 * Returns a {@link BundleInfo} object containing additional information about the bundle
	 * this target bundle represents. It is not guaranteed that the bundle info will have any
	 * fields set.  The base implementation of {@link TargetBundle} will fill in the location,
	 * symbolic name and version if that information was available in the bundle's manifest. 
	 * 
	 * @return a bundle info object with information on the bundle this target bundle represents
	 */
	public BundleInfo getBundleInfo() {
		return fInfo;
	}

	/**
	 * Returns a status object describing any problems with this target bundle. The base 
	 * implementation of {@link TargetBundle} will always return an OK status.
	 * 
	 * @return status of this bundle
	 */
	public IStatus getStatus() {
		// The status will always be ok as the constructor would throw an exception for any issues. 
		return Status.OK_STATUS;
	}

	/**
	 * Returns <code>true</code> if this bundle is a source bundle and 
	 * <code>false</code> if this bundle is an executable bundle.
	 * 
	 * @return whether the resolved bundle is a source bundle
	 */
	public boolean isSourceBundle() {
		return fSourceTarget != null;
	}

	/**
	 * If this bundle is a source bundle this method returns a bundle info
	 * representing the executable bundle that this bundle provides source for.
	 * The returned bundle info may not have a symbolic name and version set if
	 * this source bundle is an old style source plug-in.
	 * 
	 * @return bundle info representing bundle this bundle provides source for or <code>null</code>
	 */
	public BundleInfo getSourceTarget() {
		return fSourceTarget;
	}

	/**
	 * Returns whether this bundle is a fragment.
	 * 
	 * @return whether this bundle is a fragment
	 */
	public boolean isFragment() {
		return fIsFragment;
	}

	/**
	 * Returns bundle relative path to old-style source folders, or <code>null</code>
	 * if not applicable.
	 * 
	 * @return bundle relative path to old-style source folders, or <code>null</code>
	 */
	public String getSourcePath() {
		return fSourcePath;
	}

	/**
	 * Initializes the contents of this target bundle from the provided local bundle
	 * 
	 * @param file the bundle to initialize from
	 */
	private void initialize(File file) throws CoreException {
		if (file == null || !file.exists()) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.TargetFeature_FileDoesNotExist, file)));
		}
		Map manifest = ManifestUtils.loadManifest(file);
		try {
			fInfo = new BundleInfo(file.toURI());
			// Attempt to retrieve additional bundle information from the manifest
			String header = (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME);
			if (header != null) {
				ManifestElement[] elements = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, header);
				if (elements != null) {
					String name = elements[0].getValue();
					if (name != null) {
						fInfo.setSymbolicName(name);
						header = (String) manifest.get(Constants.BUNDLE_VERSION);
						if (header != null) {
							elements = ManifestElement.parseHeader(Constants.BUNDLE_VERSION, header);
							if (elements != null) {
								fInfo.setVersion(elements[0].getValue());
							}
						}
					}
					fSourceTarget = getProvidedSource(file, name, manifest);
				}
			}
			fIsFragment = manifest.containsKey(Constants.FRAGMENT_HOST);
		} catch (BundleException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, STATUS_INVALID_MANIFEST, NLS.bind(Messages.DirectoryBundleContainer_3, file.getAbsolutePath()), e));
		}
	}

	/**
	 * If the given bundle is a source bundle, the bundle that this bundle provides source for will be returned.
	 * If the given bundle is not a source bundle or there was a problem getting the source target, <code>null</code>
	 * will be returned.
	 * 
	 * @param bundle location of the bundle in the file system, can be <code>null</code> to skip searching plugin.xml
	 * @param symbolicName symbolic name of the bundle, can be <code>null</code> to skip searching of plugin.xml
	 * @param manifest the bundle's manifest, can be <code>null</code> to skip searching of manifest entries
	 * @return bundle for provided source or <code>null</code> if not a source bundle
	 */
	private BundleInfo getProvidedSource(File bundle, String symbolicName, Map manifest) {
		fSourcePath = null;
		if (manifest != null) {
			if (manifest.containsKey(ICoreConstants.ECLIPSE_SOURCE_BUNDLE)) {
				try {
					ManifestElement[] manifestElements = ManifestElement.parseHeader(ICoreConstants.ECLIPSE_SOURCE_BUNDLE, (String) manifest.get(ICoreConstants.ECLIPSE_SOURCE_BUNDLE));
					if (manifestElements != null) {
						for (int j = 0; j < manifestElements.length; j++) {
							ManifestElement currentElement = manifestElements[j];
							String binaryPluginName = currentElement.getValue();
							String versionEntry = currentElement.getAttribute(Constants.VERSION_ATTRIBUTE);
							// Currently the version attribute is required
							if (binaryPluginName != null && binaryPluginName.length() > 0 && versionEntry != null && versionEntry.length() > 0) {
								return new BundleInfo(binaryPluginName, versionEntry, null, BundleInfo.NO_LEVEL, false);
							}
						}
					}
				} catch (BundleException e) {
					PDECore.log(e);
					return null;
				}
			}
			// source bundles never have a class path
			if (manifest.containsKey(Constants.BUNDLE_CLASSPATH)) {
				return null;
			}
		}

		if (bundle != null && symbolicName != null) {
			// old source bundles were never jar'd
			if (bundle.isFile()) {
				return null;
			}

			// check for an "org.eclipse.pde.core.source" extension 
			File pxml = new File(bundle, ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR);
			if (!pxml.exists()) {
				pxml = new File(bundle, ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR);
			}
			if (pxml.exists()) {
				IExtensionRegistry registry = RegistryFactory.createRegistry(null, this, this);
				// Contribute PDE source extension point
				String bogusDef = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<?eclipse version=\"3.2\"?>\n<plugin><extension-point id=\"source\" name=\"source\"/>\n</plugin>"; //$NON-NLS-1$
				RegistryContributor pointContributor = new RegistryContributor(PDECore.PLUGIN_ID, PDECore.PLUGIN_ID, null, null);
				registry.addContribution(new ByteArrayInputStream(bogusDef.getBytes()), pointContributor, false, null, null, this);
				// Search for extensions to the extension point
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
							return new BundleInfo(null, null, bundle.toURI(), BundleInfo.NO_LEVEL, false);
						}
					}
				} catch (FileNotFoundException e) {
				}
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer result = new StringBuffer().append(getBundleInfo().toString());
		IStatus status = getStatus();
		if (status != null && !status.isOK()) {
			result.append(' ').append(status.toString());
		}
		return result.toString();
	}
}
