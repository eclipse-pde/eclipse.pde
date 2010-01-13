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
import java.net.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.EngineActivator;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.query.IQueryResult;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;

/**
 * A bundle container representing an installed profile.
 * 
 * @since 3.5 
 */
public class ProfileBundleContainer extends AbstractLocalBundleContainer {

	private static final String TEMP_REPO_LOCATION = ".temp_repository";

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
		SubMonitor subMon = SubMonitor.convert(monitor, "Loading repository for " + getLocation(false), 100);

		// Get the installation location
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

		subMon.worked(5);

		IMetadataRepositoryManager repoManager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		if (repoManager == null) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.P2Utils_UnableToAcquireP2Service));
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

			subMon.worked(10);

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
		subMon.setWorkRemaining(85);
		if (monitor.isCanceled()) {
			return new IMetadataRepository[0];
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

		if (profile.exists()) {
			fRepo = repoManager.loadRepository(profile.toURI(), subMon.newChild(20));
			// When self hosting, you can end up with a repository, but only features in it, fall back on bundles.info instead
			if (!fRepo.query(P2Utils.BUNDLE_QUERY, subMon.newChild(5)).isEmpty()) {
				return new IMetadataRepository[] {fRepo};
			}
		}
		subMon.setWorkRemaining(75);
		if (monitor.isCanceled()) {
			return new IMetadataRepository[0];
		}

		// If the profile doesn't exist or is empty, fall back on other options to get installation information
		File[] files = null;

		// Read bundles.txt
		// This case will be hit when self hosting, as we don't require the user to generate a p2 profile on launch, but we do write bundles.txt
		List fileList = new ArrayList();
		BundleInfo[] infos = P2Utils.readBundles(home, configURL);
		if (infos != null) {
			for (int i = 0; i < infos.length; i++) {
				URI location = infos[i].getLocation();
				if (new File(location).exists()) {
					fileList.add(new File(location));
				}
			}
		}
		subMon.worked(10);
		infos = P2Utils.readSourceBundles(home, configURL);
		if (infos != null) {
			for (int i = 0; i < infos.length; i++) {
				URI location = infos[i].getLocation();
				if (new File(location).exists()) {
					fileList.add(new File(location));
				}
			}
		}
		if (fileList.size() > 0) {
			files = (File[]) fileList.toArray(new File[fileList.size()]);
		}
		subMon.worked(10);
		if (monitor.isCanceled()) {
			return new IMetadataRepository[0];
		}

		// Read platform.xml
		if (files == null) {
			files = org.eclipse.pde.internal.build.site.PluginPathFinder.getPaths(home, false, false);
		}
		subMon.worked(10);
		if (monitor.isCanceled()) {
			return new IMetadataRepository[0];
		}

		// Scan directory
		if (files == null) {
			files = readDirectory(new File(home));
		}
		subMon.worked(10);
		if (monitor.isCanceled()) {
			return new IMetadataRepository[0];
		}

		if (files == null || files.length == 0) {
			return new IMetadataRepository[0];
		}

		IInstallableUnit[] ius = generateMetadataForFiles(files, subMon.newChild(20));
		if (monitor.isCanceled()) {
			return new IMetadataRepository[0];
		}

		// Save the repo in a temp location so it gets replaced each time we scan
		File repoDir = new File(configArea, TEMP_REPO_LOCATION);
		repoDir.mkdir();

		IStatus repoStatus = repoManager.validateRepositoryLocation(repoDir.toURI(), subMon.newChild(5));
		IMetadataRepository repo;
		if (repoStatus.isOK()) {
			repo = repoManager.loadRepository(repoDir.toURI(), subMon.newChild(10));
			repo.removeAll();
			repo.addInstallableUnits(ius);
		} else {
			repo = repoManager.createRepository(repoDir.toURI(), "Temporary Target Repository", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, new Properties());
			repo.addInstallableUnits(ius);
			subMon.worked(10);
		}
		subMon.done();
		fRepo = repo;
		return new IMetadataRepository[] {repo};
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

	private File[] readDirectory(File root) {
		File file = new File(root, IPDEBuildConstants.DEFAULT_PLUGIN_LOCATION);
		if (file.exists()) {
			return file.listFiles();
		}
		return root.listFiles();
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
