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

import java.io.File;
import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.spi.p2.artifact.repository.SimpleArtifactRepositoryFactory;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.query.IQueryResult;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;

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
	public IMetadataRepository[] generateRepositories(IProvisioningAgent agent, IProgressMonitor monitor) throws CoreException {
		// The repository is cached in the object instance, to rescan the object must be recreated
		if (fRepo != null) {
			return new IMetadataRepository[] {fRepo};
		}

		SubMonitor subMon = SubMonitor.convert(monitor, "Create repository for " + getLocation(false), 100);

		File dir = getDirectory();
		if (!dir.isDirectory()) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.DirectoryBundleContainer_1, dir.toString())));
		}

		// Search the directory for bundles
		subMon.subTask(Messages.DirectoryBundleContainer_0);
		File site = getSite(dir);
		File[] files = site.listFiles();

		IInstallableUnit[] ius = generateMetadataForFiles(files, subMon.newChild(80));
		if (subMon.isCanceled()) {
			return new IMetadataRepository[0];
		}

		// Create the repository and add the units to it
		IMetadataRepositoryManager repoManager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		if (repoManager == null) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.P2Utils_UnableToAcquireP2Service));
		}

		// Create the repository, if it already exists, update its contents
		// TODO We shouldn't create the repository in the directory as we may not have write access
		URI repoLocation = getSite(getDirectory()).toURI();
		IStatus repoStatus = repoManager.validateRepositoryLocation(repoLocation, subMon.newChild(5));
		IMetadataRepository repo;
		if (repoStatus.isOK()) {
			repo = repoManager.loadRepository(repoLocation, subMon.newChild(5));
			repo.removeAll();
			repo.addInstallableUnits(ius);
		} else {
			repo = repoManager.createRepository(repoLocation, "Directory Repository", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, new Properties());
			subMon.worked(5);
			repo.addInstallableUnits(ius);
		}
		subMon.worked(10);
		subMon.done();
		fRepo = repo;
		return new IMetadataRepository[] {repo};
	}

	public IFileArtifactRepository generateArtifactRepository() throws CoreException {
		// Ensure that the metadata has been generated
		if (fRepo == null) {
			return null;
		}

//		URI repoLocation = getSite(getDirectory()).toURI();
//		IStatus repoStatus = manager.validateRepositoryLocation(repoLocation, subMon.newChild(5));
//		IMetadataRepository repo;
//		if (repoStatus.isOK()) {
//			repo = repoManager.loadRepository(repoLocation, subMon.newChild(5));
//			repo.removeAll();
//			repo.addInstallableUnits(ius);
//		} else {
//			repo = repoManager.createRepository(repoLocation, "Directory Repository", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, new Properties());
//			subMon.worked(5);
//			repo.addInstallableUnits(ius);
//		}

		// Create the artifact repository.  This will fail if a repository already exists here
		SimpleArtifactRepositoryFactory factory = new SimpleArtifactRepositoryFactory();
		URI repoLocation = getSite(getDirectory()).toURI();
		IFileArtifactRepository artifactRepository = (IFileArtifactRepository) factory.create(repoLocation, "Sample Artifact Repository", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, Collections.EMPTY_MAP);
		return artifactRepository;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#getRootIUs()
	 */
	public InstallableUnitDescription[] getRootIUs() throws CoreException {
		// Ensure that the metadata has been generated
		if (fRepo == null) {
			return null;
		}

		// Collect all installable units in the repository
		IQueryResult result = fRepo.query(InstallableUnitQuery.ANY, null);

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

}
