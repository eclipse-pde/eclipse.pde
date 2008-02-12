/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.provisional;


/**
 * Interface describing the {@link ApiProfileManager}
 * 
 * @noimplement this interface is not to be implemented by clients
 * @since 1.0.0
 */
public interface IApiProfileManager {
	
	/**
	 * Allows a new profile to be added to the managers' cache of profiles.
	 * New profiles are added to the cache on a replace policy, meaning if there
	 * already exists an {@link IApiProfile} entry it will be replaced with the 
	 * new profile provided. If <code>null</code> is passed in as a new profile 
	 * no work is done.
	 * 
	 * @param newprofile the new profile to add to the manager
	 */
	public void addApiProfile(IApiProfile newprofile);
	
	/**
	 * Returns the complete listing of {@link IApiProfile}s contained in the 
	 * manager or an empty array, never <code>null</code>.
	 * 
	 * @return the complete listing of {@link IApiProfile}s or an empty array
	 */
	public IApiProfile[] getApiProfiles();
	
	/**
	 * Returns the {@link IApiProfile} object with the given id, or
	 * <code>null</code> if there is no profile with the given id.
	 * 
	 * @param profileid the id of the profile to fetch
	 * @return the {@link IApiProfile} with the given id or <code>null</code>
	 */
	public IApiProfile getApiProfile(String profileid);
	
	/**
	 * Removes the {@link IApiProfile} with the given id from 
	 * the manager, which propagates to the file-system to remove the 
	 * underlying stored profile (if it exists).
	 * 
	 * @param id the unique id of the profile to remove from the manager
	 * @return true if the removal was successful false otherwise. A successful removal 
	 * constitutes the associated profile being removed from the manager and/or the 
	 * persisted state file being removed from disk.
	 */
	public boolean removeApiProfile(String id);
	
	/**
	 * Allows the {@link IApiProfile} with the specified id to be set as 
	 * the default profile. This method will accept <code>null</code>, which will remove
	 * a default {@link IApiProfile} setting.
	 * @param id the id of the {@link IApiProfile} to be the default
	 */
	public void setDefaultApiProfile(String id);
	
	/**
	 * Returns the {@link IApiProfile} that is the current default, or <code>null</code>
	 * if one has not been set.
	 * @return the default {@link IApiProfile} or <code>null</code>
	 */
	public IApiProfile getDefaultApiProfile();
	
	/**
	 * Returns the workspace profile. Creates a new one if one does not exist.
	 * If this method is called without the framework running it returns <code>null</code>
	 * <p>
	 * The workspace profile should be re-retrieved each time it is required
	 * as some workspace modifications cause the underlying profile object to
	 * change (for example, modification of MANIFEST.MF, build.properties,
	 * or project build paths).
	 * </p>
	 * @return the workspace profile or <code>null</code>
	 */
	public IApiProfile getWorkspaceProfile();
}
