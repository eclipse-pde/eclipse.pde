/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.site.PluginPathFinder;
import org.eclipse.pde.internal.core.P2Utils;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.target.provisional.IResolvedBundle;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;

/**
 * A bundle container representing an installed profile.
 * 
 * @since 3.5 
 */
public class ProfileBundleContainer extends AbstractBundleContainer {

	/**
	 * Constant describing the type of bundle container 
	 */
	public static final String TYPE = "Profile"; //$NON-NLS-1$

	/**
	 * Path to home/root install location. May contain string variables.
	 */
	private String fHome;

	/**
	 * Alternate configuration location or <code>null</code> if default.
	 * May contain string variables.
	 */
	private String fConfiguration;

	/**
	 * Creates a new bundle container for the profile at the specified location.
	 * 
	 * @param home path in local file system, may contain string variables
	 * @param configurationLocation alternate configuration location or <code>null</code> for default,
	 *  may contain string variables
	 */
	public ProfileBundleContainer(String home, String configurationLocation) {
		fHome = home;
		fConfiguration = configurationLocation;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer#getLocation(boolean)
	 */
	public String getLocation(boolean resolve) throws CoreException {
		if (resolve) {
			return resolveHomeLocation().toOSString();
		}
		return fHome;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer#getType()
	 */
	public String getType() {
		return TYPE;
	}

	/**
	 * Returns the configuration area for this container if one was specified during creation.
	 * 
	 * @return string path to configuration location or <code>null</code>
	 */
	public String getConfigurationLocation() {
		return fConfiguration;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer#resolveBundles(org.eclipse.pde.internal.core.target.provisional.ITargetDefinition, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IResolvedBundle[] resolveBundles(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException {
		String home = resolveHomeLocation().toOSString();
		if (!new File(home).isDirectory()) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.ProfileBundleContainer_0, home)));
		}

		URL configUrl = getConfigurationArea();
		if (configUrl != null) {
			if (!new File(configUrl.getFile()).isDirectory()) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.ProfileBundleContainer_2, home)));
			}
		}

		BundleInfo[] infos = P2Utils.readBundles(home, configUrl);
		if (infos == null) {
			IResolvedBundle[] platformXML = resolvePlatformXML(definition, home, monitor);
			if (platformXML != null) {
				return platformXML;
			}
			infos = new BundleInfo[0];
		}

		if (monitor.isCanceled()) {
			return new IResolvedBundle[0];
		}

		BundleInfo[] source = P2Utils.readSourceBundles(home, configUrl);
		if (source == null) {
			source = new BundleInfo[0];
		}
		IResolvedBundle[] all = new IResolvedBundle[infos.length + source.length];
		SubMonitor localMonitor = SubMonitor.convert(monitor, Messages.DirectoryBundleContainer_0, all.length);
		for (int i = 0; i < infos.length; i++) {
			if (monitor.isCanceled()) {
				return new IResolvedBundle[0];
			}
			BundleInfo info = infos[i];
			all[i] = resolveBundle(info, false);
			localMonitor.worked(1);
		}
		int index = 0;
		for (int i = infos.length; i < all.length; i++) {
			if (monitor.isCanceled()) {
				return new IResolvedBundle[0];
			}
			BundleInfo info = source[index];
			all[i] = resolveBundle(info, true);
			index++;
			localMonitor.worked(1);
		}
		localMonitor.done();
		return all;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.AbstractBundleContainer#resolveFeatures(org.eclipse.pde.internal.core.target.provisional.ITargetDefinition, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IFeatureModel[] resolveFeatures(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException {
		if (definition instanceof TargetDefinition) {
			return ((TargetDefinition) definition).getFeatureModels(getLocation(false), monitor);
		}
		return new IFeatureModel[0];
	}

	/**
	 * Resolves installed bundles based on update manager's platform XML.
	 * 
	 * @param definition
	 * @param home
	 * @param monitor
	 * @return resolved bundles or <code>null</code> if none
	 * @throws CoreException
	 */
	protected IResolvedBundle[] resolvePlatformXML(ITargetDefinition definition, String home, IProgressMonitor monitor) throws CoreException {
		File[] files = PluginPathFinder.getPaths(home, false, false);
		if (files.length > 0) {
			List all = new ArrayList(files.length);
			SubMonitor localMonitor = SubMonitor.convert(monitor, Messages.DirectoryBundleContainer_0, files.length);
			for (int i = 0; i < files.length; i++) {
				if (localMonitor.isCanceled()) {
					throw new OperationCanceledException();
				}
				try {
					IResolvedBundle rb = generateBundle(files[i]);
					if (rb != null) {
						all.add(rb);
					}
				} catch (CoreException e) {
					// ignore invalid bundles
				}
				localMonitor.worked(1);
			}
			localMonitor.done();
			if (!all.isEmpty()) {
				return (IResolvedBundle[]) all.toArray(new IResolvedBundle[all.size()]);
			}
		}
		return null;
	}

	/**
	 * Returns the home location with all variables resolved as a path.
	 * 
	 * @return resolved home location
	 * @throws CoreException
	 */
	private IPath resolveHomeLocation() throws CoreException {
		return new Path(resolveVariables(fHome));
	}

	/**
	 * Returns a URL to the configuration area associated with this profile or <code>null</code>
	 * if none.
	 * 
	 * @return configuration area URL or <code>null</code>
	 * @throws CoreException if unable to generate a URL or the user specified location does not exist
	 */
	private URL getConfigurationArea() throws CoreException {
		IPath home = resolveHomeLocation();
		IPath configuration = null;
		if (fConfiguration == null) {
			configuration = home.append("configuration"); //$NON-NLS-1$
		} else {
			configuration = new Path(resolveVariables(fConfiguration));
		}
		File file = configuration.toFile();
		if (file.exists()) {
			try {
				return file.toURL();
			} catch (MalformedURLException e) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.ProfileBundleContainer_1, home.toOSString()), e));
			}
		} else if (fConfiguration != null) {
			// If the user specified config area does not exist throw an error
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.ProfileBundleContainer_2, configuration.toOSString())));
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer#isContentEqual(org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer)
	 */
	public boolean isContentEqual(AbstractBundleContainer container) {
		if (container instanceof ProfileBundleContainer) {
			ProfileBundleContainer pbc = (ProfileBundleContainer) container;
			return fHome.equals(pbc.fHome) && isNullOrEqual(fConfiguration, fConfiguration);
		}
		return false;
	}

	private boolean isNullOrEqual(Object o1, Object o2) {
		if (o1 == null) {
			return o2 == null;
		}
		if (o2 == null) {
			return false;
		}
		return o1.equals(o2);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return new StringBuffer().append("Installation ").append(fHome).append(' ').append(fConfiguration == null ? "Default Configuration" : fConfiguration).toString(); //$NON-NLS-1$ //$NON-NLS-2$
	}

}
