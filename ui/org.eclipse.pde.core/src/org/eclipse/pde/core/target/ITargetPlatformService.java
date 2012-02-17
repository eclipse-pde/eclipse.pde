/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.target;

import java.net.URI;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.pde.internal.core.target.IUBundleContainer;

/**
 * A service to manage target platform definitions available to the workspace.
 * 
 * @since 3.8
 * @noimplement This interface is not intended to be implemented by clients. 
 */
public interface ITargetPlatformService {

	/**
	 * Status code indicating that a bundle in a target definition is not
	 * contained in the current target platform state (a bundle has been
	 * added to the file system that the target platform does not know
	 * about).
	 */
	public static final int STATUS_MISSING_FROM_TARGET_PLATFORM = 1;

	/**
	 * Status code indicating that a bundle in the current target platform
	 * state is not contained in a target definition (a bundle has been
	 * deleted from the file system that the target platform does not
	 * know about).
	 */
	public static final int STATUS_MISSING_FROM_TARGET_DEFINITION = 2;

	/**
	 * Returns handles to all target definitions known in the workspace.
	 * 
	 * @return handles to all target definitions known in the workspace
	 */
	public ITargetHandle[] getTargets(IProgressMonitor monitor);

	/**
	 * Returns a handle to a target definition backed by the underlying file.
	 * The target definition may or may not exist. If the file does not exist
	 * then this is a new target definition which becomes one of the known
	 * workspace target definitions when it is saved.
	 * 
	 * @param file target definition file that may or may not exist 
	 * @return target handle
	 */
	public ITargetHandle getTarget(IFile file);

	/**
	 * Returns a handle to a target definition backed by the underlying {@link URI}.
	 * The target definition may or may not exist. If the {@link URI} is valid
	 * then this is a new target definition which becomes one of the known
	 * external target definitions.
	 * 
	 * @param uri target definition {@link URI} that may or may not exist 
	 * @return target handle
	 */
	public ITargetHandle getTarget(URI uri);

	/**
	 * Returns a new target definition to be stored with local metadata. The target
	 * becomes one of the known workspace target definitions when it is saved.
	 * 
	 * @return new empty target definition
	 */
	public ITargetDefinition newTarget();

	/**
	 * Persists the given target definition. The target becomes one of known
	 * workspace target definitions when it is saved.
	 * <p>
	 * The target is persisted in a location determined by its handle. A handle
	 * may refer to an {@link IFile} or a workspace metadata location. Any existing
	 * target definition at the same location is overwritten.
	 * </p>
	 * @param definition definition to persist
	 * @throws CoreException if unable to persist the definition
	 */
	public void saveTargetDefinition(ITargetDefinition definition) throws CoreException;

	/**
	 * Deletes the target definition associated with the given handle.
	 * 
	 * @param handle target handle
	 * @throws CoreException if the associated target does not exist or deletion fails
	 */
	public void deleteTarget(ITargetHandle handle) throws CoreException;

	/**
	 * Creates and returns a target handle from the given memento. The memento must
	 * have been generated from {@link ITargetHandle#getMemento()}.
	 * 
	 * @param memento a target handle memento
	 * @return target handle
	 * @throws CoreException if the target handle format is invalid
	 */
	public ITargetHandle getTarget(String memento) throws CoreException;

	/**
	 * Creates and returns a target location that contains all bundles in the
	 * specified directory which may contain string substitution variables.
	 * 
	 * @param path absolute path in the local file system, may contain string variables
	 * @return target location
	 */
	public ITargetLocation newDirectoryLocation(String path);

	/**
	 * Creates and returns a target location that contains all bundles installed in
	 * a profile at the specified location with the specified configuration area. If
	 * a configuration area is not specified the default location is used. The specified 
	 * home location and configuration location may contain string substitution variables.
	 * 
	 * @param home absolute path in the local file system to the root of an installed profile
	 * 	which may contain string substitution variables
	 * @param configurationLocation absolute path in the local file system to the
	 *  configuration area for the specified installation which may contain string substitution
	 *  variables or <code>null</code> to use the default location
	 * @return target location
	 */
	public ITargetLocation newProfileLocation(String home, String configurationLocation);

	/**
	 * Creates and returns a target location that contains all bundles contained in
	 * the specified installable units (IU's) in the given repositories. If repositories are
	 * not specified default repositories are searched (based on user preferences).
	 * 
	 * @param units installable units
	 * @param repositories URI's describing repository locations or <code>null</code> to use
	 * 	default repositories
	 * @param resolutionFlags bitmask of flags to control IU resolution, possible flags are {@link IUBundleContainer#INCLUDE_ALL_ENVIRONMENTS}, {@link IUBundleContainer#INCLUDE_REQUIRED}, {@link IUBundleContainer#INCLUDE_SOURCE}, {@link IUBundleContainer#INCLUDE_CONFIGURE_PHASE} 
	 * @return target location
	 */
	public ITargetLocation newIULocation(IInstallableUnit[] units, URI[] repositories, int resolutionFlags);

	/**
	 * Creates and returns a target location that contains all bundles contained in
	 * the specified installable units (IU's) in the given repositories. If repositories are
	 * not specified default repositories are searched (based on user preferences).
	 * 
	 * @param unitIds installable unit identifiers
	 * @param versions version identifiers
	 * @param repositories URI's describing repository locations or <code>null</code> to use
	 * 	default repositories
	 * @param resolutionFlags bitmask of flags to control IU resolution, possible flags are {@link IUBundleContainer#INCLUDE_ALL_ENVIRONMENTS}, {@link IUBundleContainer#INCLUDE_REQUIRED}, {@link IUBundleContainer#INCLUDE_SOURCE}, {@link IUBundleContainer#INCLUDE_CONFIGURE_PHASE}
	 * @return target location
	 */
	public ITargetLocation newIULocation(String[] unitIds, String[] versions, URI[] repositories, int resolutionFlags);

	/**
	 * Creates and returns a target location that contains all bundles referenced by
	 * the feature at the specified location. The location is the directory that defines
	 * the feature.
	 * 
	 * @param home installation location containing a features directory which may contain
	 *  string substitution variables
	 * @param featureId feature symbolic name
	 * @param version feature version identifier or <code>null</code> to use most recent available
	 * @return target location
	 */
	public ITargetLocation newFeatureLocation(String home, String featureId, String version);

	/**
	 * Returns a handle to the target definition that corresponds to the active target platform
	 * or <code>null</code> if none.  If the plug-in registry has not been initialized, this method
	 * will initialize it so the workspace target can be set.
	 * 
	 * @return handle to workspace target platform or <code>null</code> if none
	 * @exception CoreException if an error occurs generating the handle
	 */
	public ITargetHandle getWorkspaceTargetHandle() throws CoreException;

	/**
	 * Returns a status describing whether the given target definition is synchronized with
	 * workspace's target platform state. It is possible that bundles could have been added/removed
	 * from the underlying target location storage making the current target platform state out of
	 * synch with the contents of the a definition. The given target definition must already be 
	 * resolved or this method will return <code>null</code>.
	 * <p>
	 * An <code>OK</code> status is returned when in synch. A multi-status is returned
	 * when there are synchronization issues. <code>Null</code> is returned if the target
	 * has not been resolved. Each status contains one of the following codes
	 * and the name of the associated bundle as a message:
	 * <ul>
	 * <li>STATUS_MISSING_FROM_STATE</li>
	 * <li>STATUS_MISSING_FROM_TARGET_DEFINITION</li>
	 * </ul>
	 * </p>
	 * @param target resolved target definition to compare with target platform state
	 * @return status describing whether the target is in synch with target platform state
	 * @throws CoreException if comparison fails
	 */
	public IStatus compareWithTargetPlatform(ITargetDefinition target) throws CoreException;

	/**
	 * Copies all attributes from one target definition to another.
	 * 
	 * @param from attributes are copied from this definition 
	 * @param to attributes are copied to this definition
	 * @throws CoreException in copy fails
	 */
	public void copyTargetDefinition(ITargetDefinition from, ITargetDefinition to) throws CoreException;

	/**
	 * Sets the content of the given target definition based on the target file supplied 
	 * by an <code>org.eclipse.pde.core.targets</code> extension with the specified identifier.
	 *  
	 * @param definition target definition to load
	 * @param targetExtensionId identifier of a targets extension
	 * @throws CoreException if the extension is not found or an error occurs reading the target
	 * 	file or loading the target definition
	 */
	public void loadTargetDefinition(ITargetDefinition definition, String targetExtensionId) throws CoreException;

	/**
	 * Returns a new target definition with default settings. The default target contains all plug-ins
	 * and features from the running host.  It uses an explicit configuration area if not equal to the
	 * default location.
	 * 
	 * @return a new target definition with default settings
	 */
	public ITargetDefinition newDefaultTarget();

}
