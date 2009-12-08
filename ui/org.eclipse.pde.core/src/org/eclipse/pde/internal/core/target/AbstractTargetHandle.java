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

import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;

import java.io.File;
import java.io.InputStream;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
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
	 * Path to the local directory where the local bundle pool is stored for p2 profile
	 * based targets.
	 */
	public static final IPath BUNDLE_POOL = PDECore.getDefault().getStateLocation().append(".bundle_pool"); //$NON-NLS-1$

	/**
	 * Path to the local directory where install folders are created for p2 profile
	 * based targets.
	 */
	static final IPath INSTALL_FOLDERS = PDECore.getDefault().getStateLocation().append(".install_folders"); //$NON-NLS-1$	

	/**
	 * Prefix for all profiles ID's associated with target definitions
	 */
	static final String PROFILE_ID_PREFIX = "TARGET_DEFINITION:"; //$NON-NLS-1$

	/**
	 * Installable unit property to mark IU's that have been installed in a profile by
	 * a bundle container (rather than as a secondary/required IU).
	 */
	static final String PROP_INSTALLED_IU = PDECore.PLUGIN_ID + ".installed_iu"; //$NON-NLS-1$

	/**
	 * Profile property that keeps track of provisioning mode for the target
	 * (slice versus plan).
	 */
	static final String PROP_PROVISION_MODE = PDECore.PLUGIN_ID + ".provision_mode"; //$NON-NLS-1$

	/**
	 * Profile property that keeps track of provisioning mode for the target
	 * (all environments/true versus false).
	 */
	static final String PROP_ALL_ENVIRONMENTS = PDECore.PLUGIN_ID + ".all_environments"; //$NON-NLS-1$	

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
		if (registry != null) {
			IProfile profile = registry.getProfile(getProfileId());
			if (profile != null) {
				String location = profile.getProperty(IProfile.PROP_INSTALL_FOLDER);
				registry.removeProfile(getProfileId());
				if (location != null && location.length() > 0) {
					File folder = new File(location);
					if (folder.exists()) {
						delete(folder);
					}
				}
			}
		}
	}

	/**
	 * Recursively deletes folder and files.
	 * 
	 * @param folder
	 */
	private void delete(File folder) {
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

	/**
	 * Returns the profile registry or <code>null</code>
	 * 
	 * @return profile registry or <code>null</code>
	 */
	static IProfileRegistry getProfileRegistry() {
		return (IProfileRegistry) PDECore.getDefault().acquireService(IProfileRegistry.class.getName());
	}

}
