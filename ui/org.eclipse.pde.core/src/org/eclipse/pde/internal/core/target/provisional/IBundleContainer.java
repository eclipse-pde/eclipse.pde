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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;

/**
 * A source of bundles and features for a target. A bundle container abstracts the storage and location of the
 * underlying bundles and may contain a combination of executable and source bundles.
 * 
 * @since 3.5
 */
public interface IBundleContainer {

	/**
	 * Returns repositories containing metadata for the bundles in this bundle container.  The metadata repositories must be loaded
	 * using the provided provisioning agent.  A bundle container will use this method to provide repositories if it needs to generate
	 * metadata or if existing metadata may change locations.  Containers with metadata in a static location should set the repository 
	 * location on the target itself, see {@link ITargetDefinition#setRepositories(java.net.URI[])}.  A progress monitor is provided
	 * for long running operations.
	 * <p>
	 * The generated repositories may be cached for the life of the bundle container.
	 * </p>
	 * @param agent provisioning agent that should be used to create or load the repositories
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility
	    to call done() on the given monitor. Accepts null, indicating that no progress should be
	    reported and that the operation cannot be cancelled.
	 * @return a list of repositories, possibly empty
	 * @throws CoreException if there is a problem generating a repository
	 */
	public IMetadataRepository[] generateRepositories(IProvisioningAgent agent, IProgressMonitor monitor) throws CoreException;

	/**
	 * Return descriptions of the root installable units that this bundle container will provide to the target.  This method may 
	 * return <code>null</code> if {@link #generateRepositories(IProvisioningAgent, IProgressMonitor)} has not been called previously.
	 * The descriptions will contain both an ID and a version.  The root installable units along with their dependencies will be added
	 * to the target during the {@link ITargetDefinition#resolve(IProgressMonitor)} operation.
	 *  
	 * @return list of installable unit descriptions, possibly empty, or <code>null</code> if {@link #generateRepositories(IProvisioningAgent, IProgressMonitor)} has not been called
	 * @throws CoreException if there is a problem creating the descriptions
	 */
	public InstallableUnitDescription[] getRootIUs() throws CoreException;

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
