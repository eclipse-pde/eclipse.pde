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
import java.util.Iterator;
import java.util.Properties;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.NameVersionDescriptor;

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
	private IMetadataRepository fMetaRepo;

	/**
	 * Cached, loaded artifact repository hodling artifacts for this container
	 */
	private IArtifactRepository fArtifactRepo;

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
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#generateRepositories(org.eclipse.equinox.p2.core.IProvisioningAgent, org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IRepository[] generateRepositories(IProvisioningAgent agent, IPath targetRepositories, IProgressMonitor monitor) throws CoreException {
		// The repository is cached in the object instance, to rescan the object must be recreated
		if (fMetaRepo != null && fArtifactRepo != null) {
			return new IRepository[] {fMetaRepo, fArtifactRepo};
		}

		// Get the repository services from the agent
		IMetadataRepositoryManager repoManager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		if (repoManager == null) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.P2Utils_UnableToAcquireP2Service));
		}

		IArtifactRepositoryManager artifactManager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
		if (artifactManager == null) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.P2Utils_UnableToAcquireP2Service));
		}

		SubMonitor subMon = SubMonitor.convert(monitor, NLS.bind(Messages.DirectoryBundleContainer_createRepoTask, getLocation(false)), 100);

		File dir = getDirectory();
		if (!dir.isDirectory()) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.DirectoryBundleContainer_1, dir.toString())));
		}

		// Search the directory for bundles
		subMon.subTask(Messages.DirectoryBundleContainer_0);
		File site = getSite(dir);
		File[] files = site.listFiles();

		// Create metadata		
		IInstallableUnit[] ius = generateMetadata(files, subMon.newChild(40));
		if (subMon.isCanceled()) {
			return new IRepository[0];
		}

		URI repoLocation = new File(targetRepositories.toOSString()).toURI();

		// Create the metadata repository, if it already exists, update its contents
		IStatus repoStatus = repoManager.validateRepositoryLocation(repoLocation, subMon.newChild(5));
		IMetadataRepository metaRepo;
		if (repoStatus.isOK()) {
			metaRepo = repoManager.loadRepository(repoLocation, subMon.newChild(5));
			metaRepo.removeAll();
			metaRepo.addInstallableUnits(ius);
		} else {
			metaRepo = repoManager.createRepository(repoLocation, "Generated Directory Repository", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, new Properties()); //$NON-NLS-1$
			subMon.worked(5);
			metaRepo.addInstallableUnits(ius);
		}
		// Remove the location from the manager so it doesn't show up elsewhere in the UI
		repoManager.removeRepository(repoLocation);
		fMetaRepo = metaRepo;

		// Create the artifact descriptors
		IArtifactDescriptor[] artifacts = generateArtifactDescriptors(files, subMon.newChild(40));
		if (subMon.isCanceled()) {
			return new IRepository[0];
		}

		// Create the artifact repository, update it if it already exists
		IArtifactRepository artifactRepo = null;
		try {
			artifactRepo = artifactManager.loadRepository(repoLocation, subMon.newChild(5));
			artifactRepo.removeAll();
		} catch (ProvisionException e) {
			artifactRepo = artifactManager.createRepository(repoLocation, "Generated Directory Repository", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, new Properties()); //$NON-NLS-1$
		}
		artifactRepo.addDescriptors(artifacts);
		subMon.worked(5);
		// Remove the location from the manager so it doesn't show up elsewhere in the UI
		artifactManager.removeRepository(repoLocation);
		fArtifactRepo = artifactRepo;

		subMon.done();
		return new IRepository[] {metaRepo, artifactRepo};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#getRootIUs()
	 */
	public NameVersionDescriptor[] getRootIUs() throws CoreException {
		// Ensure that the metadata has been generated
		if (fMetaRepo == null) {
			return null;
		}

		// Collect all installable units in the repository
		IQueryResult result = fMetaRepo.query(InstallableUnitQuery.ANY, null);

		NameVersionDescriptor[] descriptions = new NameVersionDescriptor[result.unmodifiableSet().size()];
		int i = 0;
		for (Iterator iterator = result.iterator(); iterator.hasNext();) {
			IInstallableUnit unit = (IInstallableUnit) iterator.next();
			descriptions[i] = new NameVersionDescriptor(unit.getId(), unit.getVersion().toString());
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
