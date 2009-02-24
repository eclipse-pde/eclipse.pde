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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.P2Utils;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.provisional.IResolvedBundle;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

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
		URL configUrl = getConfigurationArea();
		String home = resolveHomeLocation().toOSString();
		if (!new File(home).isDirectory()) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.ProfileBundleContainer_0, home)));
		}
		BundleInfo[] infos = P2Utils.readBundles(home, configUrl);
		if (infos == null) {
			infos = new BundleInfo[0];
		}
		BundleInfo[] source = P2Utils.readSourceBundles(home, configUrl);
		if (source == null) {
			source = new BundleInfo[0];
		}
		IResolvedBundle[] all = new IResolvedBundle[infos.length + source.length];
		for (int i = 0; i < infos.length; i++) {
			BundleInfo info = infos[i];
			all[i] = resolveBundle(info, false);
		}
		int index = 0;
		for (int i = infos.length; i < all.length; i++) {
			BundleInfo info = source[index];
			all[i] = resolveBundle(info, true);
			index++;
		}
		return all;
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
	 * Returns a URL to the configuration area associated with this profile.
	 * 
	 * @return configuration area URL
	 * @throws CoreException if unable to generate a URL
	 */
	private URL getConfigurationArea() throws CoreException {
		IPath home = resolveHomeLocation();
		IPath configuration = null;
		if (fConfiguration == null) {
			configuration = home.append("configuration"); //$NON-NLS-1$
		} else {
			configuration = new Path(resolveVariables(fConfiguration));
		}
		try {
			return configuration.toFile().toURL();
		} catch (MalformedURLException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.ProfileBundleContainer_1, home.toOSString()), e));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer#isContentEqual(org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer)
	 */
	public boolean isContentEqual(AbstractBundleContainer container) {
		if (container instanceof ProfileBundleContainer) {
			ProfileBundleContainer pbc = (ProfileBundleContainer) container;
			return fHome.equals(pbc.fHome) && isNullOrEqual(fConfiguration, fConfiguration) && super.isContentEqual(container);
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
		return new StringBuffer().append("Installation ").append(fHome).append(' ').append(fConfiguration == null ? "Default Configuration" : fConfiguration).append(' ').append(getIncludedBundles() == null ? "All" : Integer.toString(getIncludedBundles().length)).append(" included").toString(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#getVMArguments()
	 */
	public String[] getVMArguments() {
		String FWK_ADMIN_EQ = "org.eclipse.equinox.frameworkadmin.equinox"; //$NON-NLS-1$

		String[] jvmArgs = null;
		try {
			FrameworkAdmin fwAdmin = (FrameworkAdmin) PDECore.getDefault().acquireService(FrameworkAdmin.class.getName());
			if (fwAdmin == null) {
				Bundle fwAdminBundle = Platform.getBundle(FWK_ADMIN_EQ);
				fwAdminBundle.start();
				fwAdmin = (FrameworkAdmin) PDECore.getDefault().acquireService(FrameworkAdmin.class.getName());
			}
			Manipulator manipulator = fwAdmin.getManipulator();
			ConfigData configData = new ConfigData(null, null, null, null);

			String home = getLocation(true);
			manipulator.getLauncherData().setLauncher(new File(home, "eclipse")); //$NON-NLS-1$
			File installDirectory = new File(home);
			if (Platform.getOS().equals(Platform.OS_MACOSX))
				installDirectory = new File(installDirectory, "Eclipse.app/Contents/MacOS"); //$NON-NLS-1$
			manipulator.getLauncherData().setLauncherConfigLocation(new File(installDirectory, "eclipse.ini")); //$NON-NLS-1$
			manipulator.getLauncherData().setHome(new File(home));

			manipulator.setConfigData(configData);
			manipulator.load();
			jvmArgs = manipulator.getLauncherData().getJvmArgs();
		} catch (BundleException e) {
			PDECore.log(e);
		} catch (CoreException e) {
			PDECore.log(e);
		} catch (IOException e) {
			PDECore.log(e);
		}
		if (jvmArgs.length == 0) {
			return null;
		}
		return jvmArgs;
	}

}
