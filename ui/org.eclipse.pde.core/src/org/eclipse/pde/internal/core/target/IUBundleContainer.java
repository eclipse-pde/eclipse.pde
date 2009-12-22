/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;

/**
 * A bundle container that references IU's in one or more repositories.
 * 
 * @since 3.5
 */
public class IUBundleContainer implements IBundleContainer {

	private InstallableUnitDescription[] fDescriptions;

	/**
	 * Constructs a installable unit bundle container for the specified units.
	 * 
	 * @param ids IU identifiers
	 * @param versions IU versions
	 * @param repositories metadata repositories used to search for IU's or <code>null</code> if
	 *   default set
	 */
	public IUBundleContainer(InstallableUnitDescription[] descriptions) {
		fDescriptions = descriptions;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#generateRepositories(org.eclipse.equinox.p2.core.IProvisioningAgent, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IMetadataRepository[] generateRepositories(IProvisioningAgent agent, IProgressMonitor monitor) throws CoreException {
		return new IMetadataRepository[0];
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#getRootIUs()
	 */
	public InstallableUnitDescription[] getRootIUs() throws CoreException {
		return fDescriptions;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#isContentEqual(org.eclipse.pde.internal.core.target.provisional.IBundleContainer)
	 */
	public boolean isContentEqual(IBundleContainer container) {
		if (container instanceof IUBundleContainer) {
			IUBundleContainer iuContainer = (IUBundleContainer) container;
			if (fDescriptions.length == iuContainer.fDescriptions.length) {
				for (int i = 0; i < fDescriptions.length; i++) {
					if (!fDescriptions[i].getId().equals(iuContainer.fDescriptions[i].getId())) {
						return false;
					}
					if (!fDescriptions[i].getVersion().equals(iuContainer.fDescriptions[i].getVersion())) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns whether the arrays have equal contents or are both <code>null</code>.
	 * 
	 * @param objects1
	 * @param objects2
	 * @return whether the arrays have equal contents or are both <code>null</code>
	 */
	protected boolean isEqualOrNull(Object[] objects1, Object[] objects2) {
		if (objects1 == null) {
			return objects2 == null;
		}
		if (objects2 == null) {
			return false;
		}
		if (objects1.length == objects2.length) {
			for (int i = 0; i < objects1.length; i++) {
				if (!objects1[i].equals(objects2[i])) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#getVMArguments()
	 */
	public String[] getVMArguments() {
		return null;
	}

}
