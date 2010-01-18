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
import java.io.InputStream;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;
import org.eclipse.pde.internal.core.target.provisional.ITargetHandle;

/**
 * Common implementation of target handles.
 * 
 * @since 3.5
 */
public abstract class AbstractTargetHandle implements ITargetHandle {

	/**
	 * Prefix for all profiles ID's associated with target definitions
	 */
	static final String PROFILE_ID_PREFIX = "TARGET_DEFINITION:"; //$NON-NLS-1$

	static final IPath GENERATED_REPOSITORIES = PDECore.getDefault().getStateLocation().append(".generated_repositories"); //$NON-NLS-1$

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
	 * Deletes the underlying target definition and associated profile if available.
	 * 
	 * Clients should call {@link #deleteProfile()} within this method to remove the 
	 * associated profile if available.
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
	 * @throws CoreException if unable to generate identifier
	 */
	String getProfileId() throws CoreException {
		StringBuffer buffer = new StringBuffer();
		buffer.append(PROFILE_ID_PREFIX);
		buffer.append(getMemento());
		return buffer.toString();
	}

	/**
	 * Returns the temp directory where this target stores its generated repositories.
	 * 
	 * @return path to a temporary directory which may not exist
	 * @throws CoreException if unable to generate folder name
	 */
	IPath getRepositoryLocation() throws CoreException {
		return GENERATED_REPOSITORIES.append(getProfileId());
	}

	/**
	 * Deletes the profile associated with this target handle, if any.
	 * Also deletes the temp directory storing generated repositories
	 * for the target if it exists
	 * 
	 * @throws CoreException If there is a problem deleting the target
	 */
	void deleteProfile() throws CoreException {
		// Find the profile and remove it from the registry
		IProvisioningAgent agent = TargetPlatformService.getProvisioningAgent();
		IProfileRegistry registry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
		if (registry != null) {
			IProfile profile = registry.getProfile(getProfileId());
			if (profile != null) {
				registry.removeProfile(getProfileId());
			}
		}
		agent.stop();

		// Look for a temp repo directory and delete it if it exists
		File repoLocation = new File(getRepositoryLocation().toOSString());
		delete(repoLocation);

	}

	/**
	 * Recursively deletes folder and files.
	 * 
	 * @param folder
	 */
	private void delete(File folder) {
		if (folder.isFile()) {
			folder.delete();
		} else if (folder.isDirectory()) {
			File[] files = folder.listFiles();
			for (int i = 0; i < files.length; i++) {
				File file = files[i];
				if (file.isDirectory()) {
					delete(file);
				}
				file.delete();
			}
			folder.delete();
		}
	}
}
