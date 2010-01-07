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
package org.eclipse.pde.internal.core.target;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.EngineActivator;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.query.IQueryResult;
import org.eclipse.equinox.p2.publisher.Publisher;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.P2Utils;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;

/**
 * A bundle container representing an installed profile.
 * 
 * @since 3.5 
 */
public class ProfileBundleContainer extends AbstractLocalBundleContainer {

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
	 * Path to home/root install location. May contain string variables.
	 */
	private String fHome;

	/**
	 * Alternate configuration location or <code>null</code> if default.
	 * May contain string variables.
	 */
	private String fConfiguration;

	/**
	 * Cached, loaded metadata repository holding metadata for this container
	 */
	private IMetadataRepository fRepo;

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
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#generateRepositories(org.eclipse.equinox.p2.core.IProvisioningAgent, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IMetadataRepository[] generateRepositories(IProvisioningAgent agent, IProgressMonitor monitor) throws CoreException {
		// TODO Use the progress monitor

		// Get the installation location
		String home = resolveHomeLocation().toOSString();
		if (!new File(home).isDirectory()) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.ProfileBundleContainer_0, home)));
		}
		URL configURL = getConfigurationArea();
		if (configURL == null) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.ProfileBundleContainer_2, home)));
		}
		File configArea = new File(configURL.getFile());
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
			p2DataArea = new File(configArea, "p2");
		}

		if (profileName == null || profileName.length() == 0) {
			profileName = "SDKProfile";
		}

		IPath profilePath = new Path(p2DataArea.getAbsolutePath());
		profilePath = profilePath.append(EngineActivator.ID).append("profileRegistry").append(profileName + ".profile");
		File profile = profilePath.toFile();
		if (!profile.exists()) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind("Could not find profile at: {0}", profile.toString())));
		}

		// TODO Have to handle cases where p2 is not present (platform.xml and directory)
//		BundleInfo[] infos = P2Utils.readBundles(home, configUrl);
//		if (infos == null) {
//			IResolvedBundle[] platformXML = resolvePlatformXML(definition, home, monitor);
//			if (platformXML != null) {
//				return platformXML;
//			}
//			infos = new BundleInfo[0];
//		}
//
//		if (monitor.isCanceled()) {
//			return new IResolvedBundle[0];
//		}

		fRepo = Publisher.loadMetadataRepository(agent, profile.toURI(), false, false);
		return new IMetadataRepository[] {fRepo};
	}

	public InstallableUnitDescription[] getRootIUs() throws CoreException {
		if (fRepo == null) {
			return null;
		}

		// Collect all installable units in the repository
		IQueryResult result = fRepo.query(P2Utils.BUNDLE_QUERY, null);

		InstallableUnitDescription[] descriptions = new InstallableUnitDescription[result.unmodifiableSet().size()];
		int i = 0;
		for (Iterator iterator = result.iterator(); iterator.hasNext();) {
			IInstallableUnit unit = (IInstallableUnit) iterator.next();
			descriptions[i] = new InstallableUnitDescription();
			descriptions[i].setId(unit.getId());
			descriptions[i].setVersion(unit.getVersion());
			i++;
		}

		return descriptions;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#isContentEqual(org.eclipse.pde.internal.core.target.provisional.IBundleContainer)
	 */
	public boolean isContentEqual(IBundleContainer container) {
		if (container instanceof ProfileBundleContainer) {
			ProfileBundleContainer pbc = (ProfileBundleContainer) container;
			return fHome.equals(pbc.fHome) && isNullOrEqual(fConfiguration, fConfiguration);
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.AbstractLocalBundleContainer#getLocation(boolean)
	 */
	public String getLocation(boolean resolve) throws CoreException {
		if (resolve) {
			return resolveHomeLocation().toOSString();
		}
		return fHome;
	}

	/**
	 * Returns the configuration area for this container if one was specified during creation.
	 * 
	 * @return string path to configuration location or <code>null</code>
	 */
	public String getConfigurationLocation() {
		return fConfiguration;
	}

//	/**
//	 * Resolves installed bundles based on update manager's platform XML.
//	 * 
//	 * @param definition
//	 * @param home
//	 * @param monitor
//	 * @return resolved bundles or <code>null</code> if none
//	 * @throws CoreException
//	 */
//	protected IResolvedBundle[] resolvePlatformXML(ITargetDefinition definition, String home, IProgressMonitor monitor) throws CoreException {
//		File[] files = PluginPathFinder.getPaths(home, false, false);
//		if (files.length > 0) {
//			List all = new ArrayList(files.length);
//			SubMonitor localMonitor = SubMonitor.convert(monitor, Messages.DirectoryBundleContainer_0, files.length);
//			for (int i = 0; i < files.length; i++) {
//				if (localMonitor.isCanceled()) {
//					throw new OperationCanceledException();
//				}
//				try {
//					IResolvedBundle rb = generateBundle(files[i]);
//					if (rb != null) {
//						all.add(rb);
//					}
//				} catch (CoreException e) {
//					// ignore invalid bundles
//				}
//				localMonitor.worked(1);
//			}
//			localMonitor.done();
//			if (!all.isEmpty()) {
//				return (IResolvedBundle[]) all.toArray(new IResolvedBundle[all.size()]);
//			}
//		}
//		return null;
//	}

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
