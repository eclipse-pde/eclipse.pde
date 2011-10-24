/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.target;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;

/**
 * Contributed target locations that want to support updating in the target wizard and editor must adapt
 * their {@link ITargetLocation} to this interface. 
 * 
 * @noextend This interface is not intended to be extended by clients.
 * @since 3.7
 */
public interface ITargetLocationUpdater {

	/**
	 * Status code that can be set on an OK status returned by {@link #update(ITargetDefinition, ITargetLocation, IProgressMonitor)}
	 * <p>
	 * If this status code is set, the target will not resolve the target after the update completes. 
	 * </p>
	 */
	public static final int STATUS_CODE_NO_CHANGE = 101;

	/**
	 * Returns whether this updater can update the given target location. This method will be called
	 * when a selection is made to determine if the update button should be enabled.
	 * 
	 * @param target the target definition being edited
	 * @param targetLocation the target location to update
	 * @return whether this update can update the given target location
	 */
	public boolean canUpdate(ITargetDefinition target, ITargetLocation targetLocation);

	/**
	 * Updates the given target location. If an OK status is returned, the target will be resolved
	 * unless the status has the code {@link #STATUS_CODE_NO_CHANGE}. If a non-OK status is returned
	 * the message will be presented to the user.
	 * <p>
	 * This method may be called from a non-UI thread.  A progress monitor is provided.
	 * </p>
	 * 
	 * @param target the target definition being edited
	 * @param targetLocation the target location to update
	 * @param monitor progress monitor
	 * @return result of the update, use an OK status with {@link #STATUS_CODE_NO_CHANGE} to indicate everything is up to date
	 */
	public IStatus update(ITargetDefinition target, ITargetLocation targetLocation, IProgressMonitor monitor);

}
