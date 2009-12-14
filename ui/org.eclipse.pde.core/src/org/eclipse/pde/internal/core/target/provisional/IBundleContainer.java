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
import org.eclipse.equinox.p2.repository.IRepository;

/**
 * A collection of bundles. A bundle container abstracts the storage and location of the
 * underlying bundles and may contain a combination of executable and source bundles.
 * 
 * @since 3.5
 */
public interface IBundleContainer {

	/**
	 * Implementors may cache the repositories for the life of the object
	 * The generated repositories should be added to the repository manager associated with the agent
	 * 
	 * @param agent 
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility
	    to call done() on the given monitor. Accepts null, indicating that no progress should be
	    reported and that the operation cannot be cancelled.
	 * @return
	 * @throws CoreException
	 */
	public IRepository[] generateRepositories(IProvisioningAgent agent, IProgressMonitor monitor) throws CoreException;

	/**
	 * If repositories have not been generated, this method may call it
	 * 
	 * @param agent 
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility
	    to call done() on the given monitor. Accepts null, indicating that no progress should be
	    reported and that the operation cannot be cancelled.
	 * @return
	 * @throws CoreException
	 */
	public InstallableUnitDescription[] getRootIUs(IProvisioningAgent agent, IProgressMonitor monitor) throws CoreException;

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
