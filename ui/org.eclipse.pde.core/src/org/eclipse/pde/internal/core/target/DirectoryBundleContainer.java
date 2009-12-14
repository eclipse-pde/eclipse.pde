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
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.Publisher;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.service.pluginconversion.PluginConverter;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * A directory of bundles.
 * 
 * @since 3.5
 */
public class DirectoryBundleContainer extends AbstractLocalBundleContainer {

	/**
	 * Path to this container's directory in the local file system.
	 * The path may contain string substitution variables.
	 */
	private String fPath;

	/**
	 * Cached, loaded metadata repository holding metadata for this container
	 */
	private IMetadataRepository fRepo;

	/**
	 * Constructs a directory bundle container at the given location.
	 * 
	 * @param path directory location in the local file system, may contain string substitution variables
	 */
	public DirectoryBundleContainer(String path) {
		fPath = path;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.AbstractLocalBundleContainer#getLocation(boolean)
	 */
	public String getLocation(boolean resolve) throws CoreException {
		if (resolve) {
			return getDirectory().toString();
		}
		return fPath;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#generateRepositories(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IRepository[] generateRepositories(IProvisioningAgent agent, IProgressMonitor monitor) throws CoreException {
		// The repository is cached in the object instance, to rescan the object must be recreated
		if (fRepo != null) {
			return new IRepository[] {fRepo};
		}

		SubMonitor subMon = SubMonitor.convert(monitor, "Create repository for " + getLocation(false), 100);

		File dir = getDirectory();
		if (!dir.isDirectory()) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.DirectoryBundleContainer_1, dir.toString())));
		}

		// Search the directory for bundles
		File site = getSite(dir);
		File[] files = site.listFiles();
		subMon.subTask(Messages.DirectoryBundleContainer_0);

		StateObjectFactory stateFactory = Platform.getPlatformAdmin().getFactory();
		State state = stateFactory.createState(false);

		SubMonitor loopProgress = subMon.newChild(50).setWorkRemaining(files.length);
		List bundleDescriptions = new ArrayList(files.length);
		for (int i = 0; i < files.length; i++) {
			if (subMon.isCanceled()) {
				return new IRepository[0];
			}
			try {
				Map manifest = loadManifest(files[i]);
				String header = (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME);
				if (header != null) {
					ManifestElement[] elements = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, header);
					if (elements != null) {
						String name = elements[0].getValue();
						if (name != null) {
							Hashtable dictionary = new Hashtable(manifest.size());
							dictionary.putAll(manifest);
							BundleDescription bd = stateFactory.createBundleDescription(state, (Dictionary) manifest, (files[i].toURL()).toExternalForm(), (long) i);
							bundleDescriptions.add(bd);
						}
					}
				}
			} catch (MalformedURLException e) {
				// ignore invalid bundles
			} catch (BundleException e) {
				// ignore invalid bundles
			} catch (CoreException e) {
				// ignore invalid bundles
			}
			loopProgress.worked(1);
		}

		if (subMon.isCanceled()) {
			return new IRepository[0];
		}

		// Create metadata for the bundles
		IInstallableUnit[] ius = P2Utils.createInstallableUnits(bundleDescriptions);
		subMon.worked(40);

		if (subMon.isCanceled()) {
			return new IRepository[0];
		}

		// Create the repository and add the units to it
		// TODO Could the publisher create a state/bundle description/ius for us?
		// TODO Need to have better naming, managing of repositories
		Publisher publisher = new Publisher(new PublisherInfo());
		IMetadataRepository metaRepo = publisher.createMetadataRepository(agent, getSite(getDirectory()).toURI(), "Directory Repository", false, false);
		metaRepo.addInstallableUnits(ius);

		// Register the repository in the given agent
		IMetadataRepositoryManager repoManager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		if (repoManager != null) {
			// If the manager is inaccessible don't register the repository
			repoManager.addRepository(metaRepo.getLocation());
		}

		subMon.worked(10);
		fRepo = metaRepo;
		return new IRepository[] {metaRepo};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#getRootIUs(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public InstallableUnitDescription[] getRootIUs(IProvisioningAgent agent, IProgressMonitor monitor) throws CoreException {
		SubMonitor subMon = SubMonitor.convert(monitor, "Collecting contents of " + getLocation(true), 100);

		// Ensure that the metadata has been generated
		if (fRepo == null) {
			generateRepositories(agent, subMon.newChild(50));
		}
		subMon.setWorkRemaining(50);

		if (fRepo == null) {
			return new InstallableUnitDescription[0];
		}

		// Collect all installable units in the repository
		Collector result = fRepo.query(InstallableUnitQuery.ANY, subMon.newChild(50));

		InstallableUnitDescription[] descriptions = new InstallableUnitDescription[result.size()];
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

	/**
	 * Returns the directory to search for bundles in.
	 * 
	 * @return directory if unable to resolve variables in the path
	 */
	private File getDirectory() throws CoreException {
		String path = resolveVariables(fPath);
		return new File(path);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer#isContentEqual(org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer)
	 */
	public boolean isContentEqual(IBundleContainer container) {
		if (container instanceof DirectoryBundleContainer) {
			DirectoryBundleContainer dbc = (DirectoryBundleContainer) container;
			return fPath.equals(dbc.fPath);
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return new StringBuffer().append("Directory ").append(fPath).toString(); //$NON-NLS-1$
	}

	/**
	 * Returns the directory to scan for bundles - a "plug-ins" sub directory if present.
	 * 
	 * @param root the location the container specifies as a root directory
	 * @return the given directory or its plug-ins sub directory if present
	 */
	protected File getSite(File root) {
		File file = new File(root, IPDEBuildConstants.DEFAULT_PLUGIN_LOCATION);
		if (file.exists()) {
			return file;
		}
		return root;
	}

	/**
	 * Parses a bundle's manifest into a dictionary. The bundle may be in a jar
	 * or in a directory at the specified location.
	 * 
	 * @param bundleLocation root location of the bundle
	 * @return bundle manifest dictionary
	 * @throws CoreException if manifest has invalid syntax or is missing
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
					Map map = loadPluginXML(bundleLocation);
					if (map != null) {
						return map; // else fall through to invalid manifest
					}
				}
			}
			if (manifestStream == null) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, 0, NLS.bind(Messages.DirectoryBundleContainer_3, bundleLocation.getAbsolutePath()), null));
			}
			Map map = ManifestElement.parseBundleManifest(manifestStream, new Hashtable(10));
			// Validate manifest - BSN must be present.
			// Else look for plugin.xml in case it's an old style plug-in
			String bsn = (String) map.get(Constants.BUNDLE_SYMBOLICNAME);
			if (bsn == null && bundleLocation.isDirectory()) {
				map = loadPluginXML(bundleLocation); // not a bundle manifest, try plugin.xml
			}
			if (map == null) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, 0, NLS.bind(Messages.DirectoryBundleContainer_3, bundleLocation.getAbsolutePath()), null));
			}
			return map;
		} catch (BundleException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, 0, NLS.bind(Messages.DirectoryBundleContainer_3, bundleLocation.getAbsolutePath()), e));
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, 0, NLS.bind(Messages.DirectoryBundleContainer_3, bundleLocation.getAbsolutePath()), e));
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

	/**
	 * Parses an old style plug-in's (or fragment's) XML definition file into a dictionary.
	 * The plug-in must be in a directory at the specified location.
	 * 
	 * @param pluginDir root location of the plug-in
	 * @return bundle manifest dictionary or <code>null</code> if none
	 * @throws CoreException if manifest has invalid syntax
	 */
	private Map loadPluginXML(File pluginDir) throws CoreException {
		File pxml = new File(pluginDir, ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR);
		File fxml = new File(pluginDir, ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR);
		if (pxml.exists() || fxml.exists()) {
			// support classic non-OSGi plug-in
			PluginConverter converter = (PluginConverter) PDECore.getDefault().acquireService(PluginConverter.class.getName());
			if (converter != null) {
				try {
					Dictionary convert = converter.convertManifest(pluginDir, false, null, false, null);
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
					throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.DirectoryBundleContainer_2, pluginDir.getAbsolutePath()), e));
				}
			}
		}
		return null;
	}

}
