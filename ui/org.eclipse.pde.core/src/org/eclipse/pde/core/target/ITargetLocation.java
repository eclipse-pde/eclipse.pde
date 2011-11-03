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
package org.eclipse.pde.core.target;

import org.eclipse.core.runtime.*;

/**
 * Describes a location in a target that provides bundles and features. Abstracts
 * the storage and provisioning of bundles. May contain a combination of 
 * executable and source bundles.
 * <p>
 * Clients are allowed to provide their own implementations. For the target definition
 * to be persisted correctly, clients must provide a factory through the 
 * <code>org.eclipse.pde.core.targetLocations</code> extension point.
 * </p><p>
 * To display an implementation in the PDE UI, clients may do the following:
 * </p><p>
 * - Implement <code>ITargetLocationWizard</code> and contribute to the <code>org.eclipse.pde.ui.targetLocationProvisioners</code>
 * extension point.
 * </p><p>
 * - Have their target location adapt to <code>org.eclipse.jface.viewers.ILabelProvider</code>
 * to provide text and icon labels in the target definition wizard and editor.
 * </p><p>
 * - Have their target location adapt to <code>org.eclipse.jface.viewers.ITreeContentProvider</code>
 * to provide children items in the target definition wizard and editor. The children must adapt to
 * <code>ILabelProvider</code> to get text and icon labels in the tree.
 * </p><p>
 * - Have their target location adapt to <code>org.eclipse.pde.ui.target.ITargetLocationEditor</code>
 * to open an edit wizard when the edit button is pressed on the target definition wizard and editor.
 * </p><p>
 * - Have their target location adapt to <code>org.eclipse.pde.ui.target.ITargetLocationUpdater</code>
 * to run an update job on the location when the update button is pressed on the target definition
 * wizard and editor.
 * </p>
 * 
 * @since 3.8
 */
public interface ITargetLocation extends IAdaptable {

	/**
	 * Resolves all contents of this location in the context of the specified
	 * target. Returns a status describing the resolution.
	 * <p>
	 * If resolution is successful an OK status is returned. If a problem
	 * occurs while resolving a non-OK status will be returned. If the 
	 * progress monitor is cancelled a CANCEL status will be returned. The
	 * returned status can be accessed later using {@link #getStatus()}.
	 * </p><p>
	 * This location will be considered resolved even if a problem occurs
	 * while resolving. See {@link #isResolved()}
	 * 
	 * @param definition target being resolved for
	 * @param monitor progress monitor or <code>null</code>
	 * @return resolution status
	 */
	public IStatus resolve(ITargetDefinition definition, IProgressMonitor monitor);

	/**
	 * Returns whether this location has resolved all of its contents. If there
	 * was a problem during the resolution the location will still be considered
	 * resolved, see {@link #getStatus()}.
	 * 
	 * @see #resolve(ITargetDefinition, IProgressMonitor)
	 * @return whether this location has resolved all of its contents
	 */
	public boolean isResolved();

	/**
	 * Returns the status of the last bundle resolution or <code>null</code> if 
	 * this location has not been resolved. If there was a problem during the 
	 * resolution, the status returned by {@link #resolve(ITargetDefinition, IProgressMonitor)}
	 * will be returned.
	 * 	 
	 * @return resolution status or <code>null</code>
	 */
	public IStatus getStatus();

	/**
	 * Returns a string that identifies the implementation of this target location.
	 * For target definitions to be persisted correctly, this must match the type
	 * in a contributed <code>org.eclipse.pde.core.targetLocations</code> extension.
	 * 
	 * @return string identifier for the type of target location.
	 */
	public String getType();

	/**
	 * Returns a path in the local file system to the root of the target location.
	 * <p>
	 * The current target platform framework requires a local file location but this
	 * requirement may be removed in the future.  This method should not be referenced.
	 * </p>
	 * 
	 * @param resolve whether to resolve variables in the path
	 * @return home location
	 * @exception CoreException if unable to resolve the location
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public String getLocation(boolean resolve) throws CoreException;

	/**
	 * Returns the bundles in this location or <code>null</code> if this location is not resolved
	 * <p>
	 * Some of the returned bundles may have non-OK statuses.  These bundles may be missing some
	 * information (location, version, source target).  To get a bundle's status call
	 * {@link TargetBundle#getStatus()}.  You can also use {@link #getStatus()} to
	 * get the complete set of problems.
	 * </p>
	 * @return resolved bundles or <code>null</code>
	 */
	public TargetBundle[] getBundles();

	/**
	 * Returns all features available in this location or <code>null</code> if this location is
	 * not resolved.
	 * <p>
	 * This method may return no features, even if the location has multiple bundles.  For all
	 * returned features, the bundles that the features reference should be returned in the list
	 * returned by {@link #getBundles()}
	 * </p>
	 * @return features or <code>null</code>
	 */
	public TargetFeature[] getFeatures();

	/**
	 * Returns VM Arguments that are specified in the bundle location or <code>null</code> if none.
	 * 
	 * @return list of VM Arguments or <code>null</code> if none available
	 */
	public String[] getVMArguments();

	/**
	 * Returns a serialized XML string that stores information about this location so it can 
	 * be restored later using a {@link ITargetLocationFactory}.  May return <code>null</code>
	 * to have this location ignored when saving a target definition.
	 * <p>
	 * The returned xml must contain a single root element named <code>location</code>. The root
	 * element may have attributes set and children.  The xml should not be prefixed by a XML 
	 * declaration such as <code>&lt?xml version="1.0" encoding="UTF-8"?&gt</code>.
	 * </p>
	 * @return an XML string storing all location information or <code>null</code>
	 */
	public String serialize();
}
