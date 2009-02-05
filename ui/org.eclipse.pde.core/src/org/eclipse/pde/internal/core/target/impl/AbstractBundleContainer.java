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
import java.net.MalformedURLException;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.runtime.*;
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
	 * Bundle restrictions (subset) this container is restricted to or <code>null</code> if
	 * no restrictions.
	 */
	private BundleInfo[] fRestrictions;

	/**
	 * Optional bundles or <code>null</code> if none
	 */
	private BundleInfo[] fOptional;

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
		return fBundles != null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#resolve(org.eclipse.pde.internal.core.target.provisional.ITargetDefinition, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public final IStatus resolve(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException {
		fBundles = resolveBundles(definition, monitor);
		// build status
		MultiStatus status = new MultiStatus(PDECore.PLUGIN_ID, 0, "Target Resolution", null);
		for (int i = 0; i < fBundles.length; i++) {
			IResolvedBundle bundle = fBundles[i];
			if (!bundle.getStatus().isOK()) {
				status.add(bundle.getStatus());
			}
		}
		if (status.isOK()) {
			return Status.OK_STATUS;
		}
		if (status.getChildren().length == 1) {
			return status.getChildren()[0];
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
	 * Resolves all source and executable bundles in this container regardless of any bundle restrictions.
	 * <p>
	 * Subclasses must implement this method.
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
		List resolved = new ArrayList(included.length);
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
			String message = NLS.bind("Missing required version {0} of plug-in: {1}", new Object[] {info.getVersion(), info.getSymbolicName()});
			if (optional) {
				sev = IStatus.INFO;
				message = NLS.bind("Missing version {0} of optional plug-in: {1}", new Object[] {info.getVersion(), info.getSymbolicName()});
			}
			return new ResolvedBundle(info, new Status(sev, PDECore.PLUGIN_ID, IResolvedBundle.STATUS_VERSION_DOES_NOT_EXIST, message, null), false, optional, false);
		}
		// DOES NOT EXIST
		int sev = IStatus.ERROR;
		String message = NLS.bind("Missing required plug-in: {0}", info.getSymbolicName());
		if (optional) {
			sev = IStatus.INFO;
			message = NLS.bind("Missing optional plug-in: {0}", info.getSymbolicName());
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
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IResolvedBundle.STATUS_INVALID_MANIFEST, "Error reading bundle manifest", e));
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IResolvedBundle.STATUS_INVALID_MANIFEST, "Error reading bundle manifest", e));
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
				return null;
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
			String path = info.getLocation().toURL().getFile();
			File file = new File(path);
			Map manifest = loadManifest(file);
			fragment = manifest.containsKey(Constants.FRAGMENT_HOST);
		} catch (CoreException e) {
			status = e.getStatus();
		} catch (MalformedURLException e) {
			status = new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IResolvedBundle.STATUS_DOES_NOT_EXIST, NLS.bind(Messages.DirectoryBundleContainer_3, info.getLocation().toString()), e);
		}
		return new ResolvedBundle(info, status, source, false, fragment);
	}
}
