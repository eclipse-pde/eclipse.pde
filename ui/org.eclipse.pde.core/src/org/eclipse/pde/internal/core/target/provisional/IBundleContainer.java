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
package org.eclipse.pde.internal.core.target.provisional;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;

/**
 * A source of bundles and features for a target. A bundle container abstracts the storage and location of the
 * underlying bundles and may contain a combination of executable and source bundles.
 * 
 * @since 3.5
 */
public interface IBundleContainer {

	/**
	 * Generates and returns the {@link IMetadataRepository} and {@link IArtifactRepository} for the bundles in this
	 * container.  The repositories should be created in the provided location which is unique to this container but
	 * may not be identical each time {@link #generateRepositories(IProvisioningAgent, IPath, IProgressMonitor)} is 
	 * called.  The location will be deleted if the target definition is deleted.  The repositories should be 
	 * created or loaded using the {@link IRepositoryManager} and {@link IArtifactRepositoryManager} services from 
	 * the given {@link IProvisioningAgent}, however they should also be removed using 
	 * {@link IRepositoryManager#removeRepository(java.net.URI)}.  If this container uses static repositories that do 
	 * not need to be generated, this method should return an empty array and the repository locations added directly 
	 * to the target using {@link ITargetDefinition#setRepositories(java.net.URI[])}.
	 * <p>
	 * The generated repositories may be cached for the life of the bundle container.
	 * </p>
	 * @param agent provisioning agent that should be used to create or load the repositories
	 * @param targetRepositories path describing a folder where a target's generated repositories can be saved
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility
	    to call done() on the given monitor. Accepts null, indicating that no progress should be
	    reported and that the operation cannot be cancelled.
	 * @return a list of repositories, possibly empty
	 * @throws CoreException if there is a problem generating a repository
	 */
	public IRepository[] generateRepositories(IProvisioningAgent agent, IPath targetRepositories, IProgressMonitor monitor) throws CoreException;

	/**
	 * Return descriptors for the root installable units that this bundle container will provide to the target.  This method may 
	 * return <code>null</code> if {@link #generateRepositories(IProvisioningAgent, IProgressMonitor)} has not been called previously.
	 * The descriptors will contain both an ID and a version.  The root installable units along with their dependencies will be added
	 * to the target during the {@link ITargetDefinition#resolve(IProgressMonitor)} operation.
	 *  
	 * @return list of name version descriptors, possibly empty, or <code>null</code> if {@link #generateRepositories(IProvisioningAgent, IProgressMonitor)} has not been called
	 * @throws CoreException if there is a problem creating the descriptions
	 */
	public NameVersionDescriptor[] getRootIUs() throws CoreException;

	/**
	 * Returns VM Arguments that are specified in the bundle container or <code>null</code> if none.
	 * 
	 * @return list of VM Arguments or <code>null</code> if none available
	 */
	public String[] getVMArguments();

	/**
	 * Returns whether this container is equivalent to another.
	 * 
	 * @param container bundle container
	 * @return whether content is equivalent
	 */
	public abstract boolean isContentEqual(IBundleContainer container);
}
