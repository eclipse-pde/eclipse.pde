/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.service.pluginconversion.PluginConverter;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.provisional.*;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * Common function for bundle containers.
 * 
 * @since 3.5
 */
public abstract class AbstractBundleContainer implements IBundleContainer {

	/**
	 * Resolved bundles or <code>null</code> if unresolved
	 */
	private IResolvedBundle[] fBundles;

	/**
	 * Status generated when this container was resolved, possibly <code>null</code>
	 */
	private IStatus fResolutionStatus;

	/**
	 * Bundle restrictions (subset) this container is restricted to or <code>null</code> if
	 * no restrictions.
	 */
	private BundleInfo[] fRestrictions;

	/**
	 * Optional bundles or <code>null</code> if none
	 */
	private BundleInfo[] fOptional;

	/**
	 * A registry can be built to identify old school source bundles.
	 */
	private IExtensionRegistry fRegistry;

	/**
	 * Most recent source patch detected from an old-style source bundle extension.
	 */
	private String fSourcePath;

	/**
	 * Resolves any string substitution variables in the given text returning
	 * the result.
	 * 
	 * @param text text to resolve
	 * @return result of the resolution
	 * @throws CoreException if unable to resolve 
	 */
	protected String resolveVariables(String text) throws CoreException {
		IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
		return manager.performStringSubstitution(text);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#isResolved()
	 */
	public final boolean isResolved() {
		return fResolutionStatus != null && fResolutionStatus.getSeverity() != IStatus.CANCEL;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#resolve(org.eclipse.pde.internal.core.target.provisional.ITargetDefinition, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public final IStatus resolve(ITargetDefinition definition, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 10);
		try {
			fBundles = resolveBundles(definition, subMonitor.newChild(10));
			fResolutionStatus = Status.OK_STATUS;
			if (subMonitor.isCanceled()) {
				fBundles = null;
				fResolutionStatus = Status.CANCEL_STATUS;
			}
		} catch (CoreException e) {
			fBundles = new IResolvedBundle[0];
			fResolutionStatus = e.getStatus();
		} finally {
			if (fRegistry != null) {
				fRegistry.stop(this);
				fRegistry = null;
			}
			subMonitor.done();
			if (monitor != null) {
				monitor.done();
			}
		}
		return fResolutionStatus;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#getBundleStatus()
	 */
	public IStatus getBundleStatus() {
		if (!isResolved()) {
			return null;
		}
		if (!fResolutionStatus.isOK()) {
			return fResolutionStatus;
		}
		// build status from bundle list
		IResolvedBundle[] bundles = getBundles();
		MultiStatus status = new MultiStatus(PDECore.PLUGIN_ID, 0, Messages.AbstractBundleContainer_0, null);
		for (int i = 0; i < bundles.length; i++) {
			if (!bundles[i].getStatus().isOK()) {
				status.add(bundles[i].getStatus());
			}
		}
		if (status.isOK()) {
			// return the generic ok status vs a problem multi-status with no children
			return Status.OK_STATUS;
		}
		return status;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#getBundles()
	 */
	public final IResolvedBundle[] getBundles() {
		if (isResolved()) {
			return getMatchingBundles(fBundles, getIncludedBundles(), getOptionalBundles());
		}
		return null;
	}

	/**
	 * Returns the list of resolved bundles in this container.  Does not filter based on any
	 * includedBundles or optionalBundles set on this container.  Returns <code>null</code> if
	 * this container has not been resolved.  Use {@link #getBundles()} to get the restricted
	 * list of bundles.
	 *  
	 * @return list of resolved bundles or <code>null</code>
	 */
	public IResolvedBundle[] getAllBundles() {
		if (isResolved()) {
			return fBundles;
		}
		return null;
	}

	/**
	 * Resolves all source and executable bundles in this container regardless of any bundle restrictions.
	 * <p>
	 * Subclasses must implement this method.
	 * </p><p>
	 * <code>beginTask()</code> and <code>done()</code> will be called on the given monitor by the caller. 
	 * </p>
	 * @param definition target context
	 * @param monitor progress monitor
	 * @return all source and executable bundles in this container regardless of any bundle restrictions
	 * @throws CoreException if an error occurs
	 */
	protected abstract IResolvedBundle[] resolveBundles(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException;

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#getRestrictions()
	 */
	public final BundleInfo[] getIncludedBundles() {
		return fRestrictions;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#setRestrictions(org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo[])
	 */
	public final void setIncludedBundles(BundleInfo[] bundles) {
		fRestrictions = bundles;
	}

	/**
	 * Returns bundles from the specified collection that match the symbolic names
	 * and/or version in the specified criteria. When no version is specified
	 * the newest version (if any) is selected.
	 * 
	 * @param collection bundles to resolve against match criteria
	 * @param included bundles to include or <code>null</code> if no restrictions
	 * @param optional optional bundles or <code>null</code> of no optional bundles
	 * @return bundles that match this container's restrictions
	 */
	static IResolvedBundle[] getMatchingBundles(IResolvedBundle[] collection, BundleInfo[] included, BundleInfo[] optional) {
		if (included == null && optional == null) {
			return collection;
		}
		// map bundles names to available versions
		Map bundleMap = new HashMap(collection.length);
		for (int i = 0; i < collection.length; i++) {
			IResolvedBundle resolved = collection[i];
			List list = (List) bundleMap.get(resolved.getBundleInfo().getSymbolicName());
			if (list == null) {
				list = new ArrayList(3);
				bundleMap.put(resolved.getBundleInfo().getSymbolicName(), list);
			}
			list.add(resolved);
		}
		List resolved = new ArrayList();
		if (included == null) {
			for (int i = 0; i < collection.length; i++) {
				resolved.add(collection[i]);
			}
		} else {
			for (int i = 0; i < included.length; i++) {
				BundleInfo info = included[i];
				resolved.add(resolveBundle(bundleMap, info, false));
			}
		}
		if (optional != null) {
			for (int i = 0; i < optional.length; i++) {
				BundleInfo option = optional[i];
				IResolvedBundle resolveBundle = resolveBundle(bundleMap, option, true);
				IStatus status = resolveBundle.getStatus();
				if (status.isOK()) {
					// add to list if not there already
					if (!resolved.contains(resolveBundle)) {
						resolved.add(resolveBundle);
					}
				} else {
					// missing optional bundle - add it to the list
					resolved.add(resolveBundle);
				}
			}
		}
		return (IResolvedBundle[]) resolved.toArray(new IResolvedBundle[resolved.size()]);
	}

	/**
	 * Resolves a bundle for the given info from the given map. The map contains
	 * keys of symbolic names and values are lists of {@link IResolvedBundle}'s available
	 * that match the names.
	 * 
	 * @param bundleMap available bundles to resolve against
	 * @param info name and version to match against
	 * @param optional whether the bundle is optional
	 * @return resolved bundle
	 */
	private static IResolvedBundle resolveBundle(Map bundleMap, BundleInfo info, boolean optional) {
		List list = (List) bundleMap.get(info.getSymbolicName());
		if (list != null) {
			String version = info.getVersion();
			if (version == null) {
				// select newest
				if (list.size() > 1) {
					// sort the list
					Collections.sort(list, new Comparator() {
						public int compare(Object o1, Object o2) {
							BundleInfo b1 = ((IResolvedBundle) o1).getBundleInfo();
							BundleInfo b2 = ((IResolvedBundle) o2).getBundleInfo();
							return b1.getVersion().compareTo(b2.getVersion());
						}
					});
				}
				// select the last one
				ResolvedBundle rb = (ResolvedBundle) list.get(list.size() - 1);
				rb.setOptional(optional);
				return rb;
			}
			Iterator iterator = list.iterator();
			while (iterator.hasNext()) {
				IResolvedBundle bundle = (IResolvedBundle) iterator.next();
				if (bundle.getBundleInfo().getVersion().equals(version)) {
					((ResolvedBundle) bundle).setOptional(optional);
					return bundle;
				}
			}
			// VERSION DOES NOT EXIST
			int sev = IStatus.ERROR;
			String message = NLS.bind(Messages.AbstractBundleContainer_1, new Object[] {info.getVersion(), info.getSymbolicName()});
			if (optional) {
				sev = IStatus.INFO;
				message = NLS.bind(Messages.AbstractBundleContainer_2, new Object[] {info.getVersion(), info.getSymbolicName()});
			}
			return new ResolvedBundle(info, new Status(sev, PDECore.PLUGIN_ID, IResolvedBundle.STATUS_VERSION_DOES_NOT_EXIST, message, null), false, optional, false);
		}
		// DOES NOT EXIST
		int sev = IStatus.ERROR;
		String message = NLS.bind(Messages.AbstractBundleContainer_3, info.getSymbolicName());
		if (optional) {
			sev = IStatus.INFO;
			message = NLS.bind(Messages.AbstractBundleContainer_4, info.getSymbolicName());
		}
		return new ResolvedBundle(info, new Status(sev, PDECore.PLUGIN_ID, IResolvedBundle.STATUS_DOES_NOT_EXIST, message, null), false, optional, false);
	}

	/**
	 * Returns a string that identifies the type of bundle container.  This type is persisted to xml
	 * so that the correct bundle container is created when deserializing the xml.  This type is also
	 * used to alter how the containers are presented to the user in the UI.
	 * 
	 * @return string identifier for the type of bundle container.
	 */
	public abstract String getType();

	/**
	 * Returns a path in the local file system to the root of the bundle container.
	 * <p>
	 * TODO: Ideally we won't need this method. Currently the PDE target platform preferences are
	 * based on a home location and additional locations, so we need the information.
	 * </p>
	 * @param resolve whether to resolve variables in the path
	 * @return home location
	 * @exception CoreException if unable to resolve the location
	 */
	public abstract String getLocation(boolean resolve) throws CoreException;

	/**
	 * Returns whether restrictions are equivalent. Subclasses should override for other data.
	 * 
	 * @param container bundle container
	 * @return whether content is equivalent
	 */
	public boolean isContentEqual(AbstractBundleContainer container) {
		return isInfosEqual(fRestrictions, container.fRestrictions) && isInfosEqual(fOptional, container.fOptional);
	}

	private boolean isInfosEqual(BundleInfo[] infos1, BundleInfo[] infos2) {
		if (infos1 == null) {
			return infos2 == null;
		}
		if (infos2 == null) {
			return false;
		}
		if (infos1.length == infos2.length) {
			for (int i = 0; i < infos1.length; i++) {
				if (!infos1[i].equals(infos2[i])) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#getOptionalBundles()
	 */
	public BundleInfo[] getOptionalBundles() {
		return fOptional;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#setOptionalBundles(org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo[])
	 */
	public void setOptionalBundles(BundleInfo[] bundles) {
		fOptional = bundles;
	}

	/**
	 * Parses a bunlde's manifest into a dictionary. The bundle may be in a jar
	 * or in a directory at the specified location.
	 * 
	 * @param bundle the bundle
	 * @return bundle manifest dictionary or <code>null</code> if none
	 * @throws CoreException if unable to read
	 */
	protected Map loadManifest(BundleInfo bundle) throws CoreException {
		ZipFile jarFile = null;
		InputStream manifestStream = null;
		try {
			String path = bundle.getLocation().toURL().getFile();
			File dirOrJar = new File(path);
			String extension = new Path(path).getFileExtension();
			if (extension != null && extension.equals("jar") && dirOrJar.isFile()) { //$NON-NLS-1$
				jarFile = new ZipFile(dirOrJar, ZipFile.OPEN_READ);
				ZipEntry manifestEntry = jarFile.getEntry(JarFile.MANIFEST_NAME);
				if (manifestEntry != null) {
					manifestStream = jarFile.getInputStream(manifestEntry);
				}
			} else {
				File file = new File(dirOrJar, JarFile.MANIFEST_NAME);
				if (file.exists())
					manifestStream = new FileInputStream(file);
			}
			if (manifestStream == null) {
				return null;
			}
			return ManifestElement.parseBundleManifest(manifestStream, new Hashtable(10));
		} catch (BundleException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IResolvedBundle.STATUS_INVALID_MANIFEST, Messages.AbstractBundleContainer_5, e));
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IResolvedBundle.STATUS_INVALID_MANIFEST, Messages.AbstractBundleContainer_5, e));
		} finally {
			closeZipFileAndStream(manifestStream, jarFile);
		}
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
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IResolvedBundle.STATUS_INVALID_MANIFEST, NLS.bind(Messages.DirectoryBundleContainer_3, bundleLocation.getAbsolutePath()), null));
			}
			return ManifestElement.parseBundleManifest(manifestStream, new Hashtable(10));
		} catch (BundleException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IResolvedBundle.STATUS_INVALID_MANIFEST, NLS.bind(Messages.DirectoryBundleContainer_3, bundleLocation.getAbsolutePath()), e));
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IResolvedBundle.STATUS_INVALID_MANIFEST, NLS.bind(Messages.DirectoryBundleContainer_3, bundleLocation.getAbsolutePath()), e));
		} finally {
			closeZipFileAndStream(manifestStream, jarFile);
		}
	}

	void closeZipFileAndStream(InputStream stream, ZipFile jarFile) {
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

	protected IResolvedBundle resolveBundle(BundleInfo info, boolean source) {
		boolean fragment = false;
		IStatus status = null;
		try {
			File file = new File(info.getLocation());
			Map manifest = loadManifest(file);
			fragment = manifest.containsKey(Constants.FRAGMENT_HOST);
		} catch (CoreException e) {
			status = e.getStatus();
		}
		return new ResolvedBundle(info, status, source, false, fragment);
	}

	/**
	 * Disposes any registry created when resolving bundles.
	 */
	protected void disposeRegistry() {

	}

	/**
	 * Returns a resolved bundle for the given file or <code>null</code> if none.
	 * <p>
	 * Clients of this method must call 
	 * </p>
	 * @param file root jar or folder that contains a bundle
	 * @return resolved bundle or <code>null</code>
	 * @exception CoreException if not a valid bundle
	 */
	protected IResolvedBundle generateBundle(File file) throws CoreException {
		Map manifest = loadManifest(file);
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
							info.setLocation(file.toURI());
							header = (String) manifest.get(Constants.BUNDLE_VERSION);
							if (header != null) {
								elements = ManifestElement.parseHeader(Constants.BUNDLE_VERSION, header);
								if (elements != null) {
									info.setVersion(elements[0].getValue());
								}
							}
							boolean source = isSourceBundle(file, name, manifest);
							boolean fragment = manifest.containsKey(Constants.FRAGMENT_HOST);
							ResolvedBundle rb = new ResolvedBundle(info, null, source, false, fragment);
							rb.setSourcePath(fSourcePath);
							return rb;
						}
					}
				}
			} catch (BundleException e) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IResolvedBundle.STATUS_INVALID_MANIFEST, NLS.bind(Messages.DirectoryBundleContainer_3, file.getAbsolutePath()), e));
			}
		}
		return null;
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
		if (!pxml.exists()) {
			pxml = new File(bundle, ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR);
		}
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
}
