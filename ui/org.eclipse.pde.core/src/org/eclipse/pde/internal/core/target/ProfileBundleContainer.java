/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.io.*;
import java.net.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.p2.engine.EngineActivator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.build.site.PluginPathFinder;
import org.eclipse.pde.internal.core.P2Utils;
import org.eclipse.pde.internal.core.PDECore;

/**
 * A bundle container representing an installed profile.
 * 
 * @since 3.5 
 */
public class ProfileBundleContainer extends AbstractBundleContainer {

	// The following constants are duplicated from org.eclipse.equinox.internal.p2.core.Activator
	private static final String CONFIG_INI = "config.ini"; //$NON-NLS-1$
	private static final String PROP_AGENT_DATA_AREA = "eclipse.p2.data.area"; //$NON-NLS-1$
	private static final String PROP_PROFILE = "eclipse.p2.profile"; //$NON-NLS-1$
	private static final String PROP_CONFIG_DIR = "osgi.configuration.area"; //$NON-NLS-1$
	private static final String PROP_USER_DIR = "user.dir"; //$NON-NLS-1$
	private static final String PROP_USER_HOME = "user.home"; //$NON-NLS-1$
	private static final String VAR_CONFIG_DIR = "@config.dir"; //$NON-NLS-1$
	private static final String VAR_USER_DIR = "@user.dir"; //$NON-NLS-1$
	private static final String VAR_USER_HOME = "@user.home"; //$NON-NLS-1$

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
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer#resolveBundles(org.eclipse.pde.core.target.ITargetDefinition, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected TargetBundle[] resolveBundles(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException {
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
			TargetBundle[] platformXML = resolvePlatformXML(definition, home, monitor);
			if (platformXML != null) {
				return platformXML;
			}
			infos = new BundleInfo[0];
		}

		if (monitor.isCanceled()) {
			return new TargetBundle[0];
		}

		BundleInfo[] source = P2Utils.readSourceBundles(home, configUrl);
		if (source == null) {
			source = new BundleInfo[0];
		}
		List/*<TargetBundle>*/all = new ArrayList/*<TargetBundle>*/();
		SubMonitor localMonitor = SubMonitor.convert(monitor, Messages.DirectoryBundleContainer_0, infos.length + source.length);
		// Add executable bundles
		for (int i = 0; i < infos.length; i++) {
			if (monitor.isCanceled()) {
				return new TargetBundle[0];
			}
			URI location = infos[i].getLocation();
			try {
				all.add(new TargetBundle(URIUtil.toFile(location)));
			} catch (CoreException e) {
				all.add(new InvalidTargetBundle(new BundleInfo(location), e.getStatus()));
			}
			localMonitor.worked(1);
		}
		// Add source bundles
		for (int i = 0; i < source.length; i++) {
			if (monitor.isCanceled()) {
				return new TargetBundle[0];
			}
			URI location = source[i].getLocation();
			try {
				all.add(new TargetBundle(URIUtil.toFile(location)));
			} catch (CoreException e) {
				all.add(new InvalidTargetBundle(new BundleInfo(location), e.getStatus()));
			}
			localMonitor.worked(1);
		}
		localMonitor.done();
		return (TargetBundle[]) all.toArray(new TargetBundle[all.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.AbstractBundleContainer#resolveFeatures(org.eclipse.pde.core.target.ITargetDefinition, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected TargetFeature[] resolveFeatures(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException {
		if (definition instanceof TargetDefinition) {
			return ((TargetDefinition) definition).resolveFeatures(getLocation(false), monitor);
		}
		return new TargetFeature[0];
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
	protected TargetBundle[] resolvePlatformXML(ITargetDefinition definition, String home, IProgressMonitor monitor) throws CoreException {
		File[] files = PluginPathFinder.getPaths(home, false, false);
		if (files.length > 0) {
			List all = new ArrayList(files.length);
			SubMonitor localMonitor = SubMonitor.convert(monitor, Messages.DirectoryBundleContainer_0, files.length);
			for (int i = 0; i < files.length; i++) {
				if (localMonitor.isCanceled()) {
					throw new OperationCanceledException();
				}
				try {
					all.add(new TargetBundle(files[i]));
				} catch (CoreException e) {
					// ignore files that are not valid bundles (users may have non-bundle files in their target directory)
				}
				localMonitor.worked(1);
			}
			localMonitor.done();
			if (!all.isEmpty()) {
				return (TargetBundle[]) all.toArray(new TargetBundle[all.size()]);
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
			return fHome.equals(pbc.fHome) && isNullOrEqual(pbc.fConfiguration, fConfiguration);
		}
		return false;
	}

	/**
	 * Returns the location of the profile file that describes the installation this container represents or <code>null</code>
	 * if no profile file could be determined.  This method checks the configuration file for a p2 data area entry and profile name
	 * to determine where the profile is located.
	 * <p>
	 * Note that when self hosting, the returned profile location will not have all running plug-ins installed unless the launch has generated
	 * a complete profile.
	 * </p>
	 * 
	 * @return the profile file or <code>null</code>
	 */
	public File getProfileFileLocation() throws CoreException {
		// Get the configuration location
		String home = resolveHomeLocation().toOSString();
		if (!new File(home).isDirectory()) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.ProfileBundleContainer_0, home)));
		}
		File configArea = null;
		URL configURL = getConfigurationArea();
		if (configURL != null) {
			configArea = new File(configURL.getFile());
		} else {
			configArea = new File(home);
		}
		if (!configArea.isDirectory()) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.ProfileBundleContainer_2, configArea)));
		}

		// Location of the profile
		File p2DataArea = null;
		String profileName = null;

		// Load the config.ini to try and find the backing profile
		File configIni = new File(configArea, CONFIG_INI);
		if (configIni.isFile()) {
			// Read config.ini
			Properties configProps = new Properties();
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(configIni);
				configProps.load(fis);
				fis.close();
			} catch (IOException e) {
				PDECore.log(e);
			} finally {
				try {
					if (fis != null)
						fis.close();
				} catch (IOException e) {
				}
			}

			String p2Area = configProps.getProperty(PROP_AGENT_DATA_AREA);
			if (p2Area != null) {
				if (p2Area.startsWith(VAR_USER_HOME)) {
					String base = substituteVar(configProps, p2Area, VAR_USER_HOME, PROP_USER_HOME, configArea);
					p2Area = new Path(base).toFile().getAbsolutePath();
				} else if (p2Area.startsWith(VAR_USER_DIR)) {
					String base = substituteVar(configProps, p2Area, VAR_USER_DIR, PROP_USER_DIR, configArea);
					p2Area = new Path(base).toFile().getAbsolutePath();
				} else if (p2Area.startsWith(VAR_CONFIG_DIR)) {
					String base = substituteVar(configProps, p2Area, VAR_CONFIG_DIR, PROP_CONFIG_DIR, configArea);
					p2Area = new Path(base).toFile().getAbsolutePath();
				}
				p2DataArea = new File(p2Area);
			}

			profileName = configProps.getProperty(PROP_PROFILE);
		}

		if (p2DataArea == null || !p2DataArea.isDirectory()) {
			p2DataArea = new File(configArea, "p2"); //$NON-NLS-1$
		}

		if (profileName == null || profileName.length() == 0) {
			profileName = "SDKProfile"; //$NON-NLS-1$
		}

		IPath profilePath = new Path(p2DataArea.getAbsolutePath());
		profilePath = profilePath.append(EngineActivator.ID).append("profileRegistry").append(profileName + ".profile"); //$NON-NLS-1$ //$NON-NLS-2$
		File profile = profilePath.toFile();

		if (profile.exists()) {
			return profile;
		}

		return null;
	}

	/**
	 * Replaces a variable in config.ini
	 * @param props properties containing entries from the 
	 * @param source the string to replace the var in
	 * @param var the variable to replace
	 * @param prop the property to lookup for a replacement value
	 * @param defaultValue value to use if the property can't be found
	 * @return source string with the variable replaced with the proper value
	 */
	private String substituteVar(Properties props, String source, String var, String prop, File defaultValue) {
		String value = props.getProperty(prop);
		if (value == null)
			value = defaultValue.getAbsolutePath();
		return value + source.substring(var.length());
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
