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
package org.eclipse.pde.internal.core.target.impl;

import java.io.InputStream;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfileRegistry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;
import org.eclipse.pde.internal.core.target.provisional.ITargetHandle;

/**
 * Common implementation of target handles.
 * 
 * @since 3.5
 */
abstract class AbstractTargetHandle implements ITargetHandle {

	/**
	 * Path to the local directory where the local bundle pool is stored for p2 profile
	 * based targets.
	 */
	static final IPath LOCAL_BUNDLE_POOL = PDECore.getDefault().getStateLocation().append(".local_pool"); //$NON-NLS-1$

	/**
	 * Prefix for all profiles ID's associated with target definitions
	 */
	static final String PROFILE_ID_PREFIX = "TARGET_DEFINITION:"; //$NON-NLS-1$

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.ITargetHandle#getTargetDefinition()
	 */
	public ITargetDefinition getTargetDefinition() throws CoreException {
		TargetDefinition definition = new TargetDefinition(this);
		if (exists()) {
			definition.setContents(getInputStream());
		}
		return definition;
	}

	/**
	 * Returns an input stream of the target definition's contents.
	 * 
	 * @return stream of content
	 * @throws CoreException if an error occurs
	 */
	protected abstract InputStream getInputStream() throws CoreException;

	/**
	 * Deletes the underlying target definition.
	 * 
	 * @throws CoreException if unable to delete
	 */
	abstract void delete() throws CoreException;

	/**
	 * Saves the definition to underlying storage.
	 * 
	 * @param definition target to save
	 * @throws CoreException on failure
	 */
	abstract void save(ITargetDefinition definition) throws CoreException;

	/**
	 * Returns the profile identifier for this target handle. There is one profile
	 * per target definition.
	 * 
	 * @return profile identifier
	 * @throws CoreException in unable to generate identifier
	 */
	String getProfileId() throws CoreException {
		StringBuffer buffer = new StringBuffer();
		buffer.append(PROFILE_ID_PREFIX);
		buffer.append(getMemento());
		return buffer.toString();
	}

	/**
	 * Deletes the profile associated with this target handle, if any. Returns
	 * <code>true</code> if a profile existed and was deleted, otherwise <code>false</code>.
	 * 
	 * @throws CoreException if unable to delete the profile
	 */
	void deleteProfile() throws CoreException {
		IProfileRegistry registry = getProfileRegistry();
		registry.removeProfile(getProfileId());
	}

	/**
	 * Returns the profile registry.
	 * 
	 * @return profile registry
	 * @throws CoreException if the registry does not exist
	 */
	static IProfileRegistry getProfileRegistry() throws CoreException {
		IProfileRegistry registry = (IProfileRegistry) PDECore.getDefault().acquireService(IProfileRegistry.class.getName());
		if (registry == null) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.AbstractTargetHandle_0));
		}
		return registry;
	}

}
