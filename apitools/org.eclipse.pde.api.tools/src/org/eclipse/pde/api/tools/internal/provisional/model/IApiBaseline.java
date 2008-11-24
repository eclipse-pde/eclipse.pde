/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.provisional.model;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

/**
 * A collection of related API components that together make up
 * an {@link IApiBaseline} that can be compared with another {@link IApiBaseline}.
 * 
 * @since 1.0.0
 */
public interface IApiBaseline extends IApiElement {
	
	/**
	 * Returns all API components in this profile. The components
	 * are returned in the order that components are searched when
	 * performing name lookup for a type (simulates the order
	 * components would be searched when performing class loading
	 * at runtime or name resolution at compile time).
	 *  
	 *  This is a convenience method for retrieving all children 
	 *  of the baseline that are {@link IApiComponent}s.
	 *  
	 * @return all API components in this profile
	 */
	public IApiComponent[] getApiComponents();
	
	/**
	 * Allows the name of the profile to be changed to the new name.
	 * If the new name is <code>null</code>, no changes are made.
	 * @param name the new name for the profile
	 */
	public void setName(String name);
	
	/**
	 * Adds the given API components to this profile, excluding all the source components.
	 * 
	 * @param components components to add
	 */
	public void addApiComponents(IApiComponent[] components);
	
	/**
	 * Returns the API components that provides the specified package when referenced from
	 * the specified source component or an empty array if none, never <code>null</code>.
	 * 
	 * @param sourceComponent component referencing the package
	 * @param packageName name of referenced package
	 * @return API components providing the package or an empty array
	 * @exception CoreException if an exception occurs
	 */
	public IApiComponent[] resolvePackage(IApiComponent sourceComponent, String packageName) throws CoreException;

	/**
	 * Returns the API component in this profile with the given symbolic name
	 * or <code>null</code> if none.
	 * 
	 * @param id component symbolic name
	 * @return API component or <code>null</code>
	 */
	public IApiComponent getApiComponent(String id);
	
	/**
	 * Returns the execution environment this profile is resolved with, or
	 * <code>null</code> if none (not yet determined or unable to bind to an
	 * execution environment). A profile can be created with a specific execution
	 * environment, or be created to automatically resolve an execution environment
	 * as components are added to it.
	 * <p> 
	 * An execution environment is represented by a unique identifier
	 * as defined by OSGi - for example "J2SE-1.4" or "CDC-1.0/Foundation-1.0".
	 * </p>
	 * @return execution environment identifier or <code>null</code>
	 */
	public String getExecutionEnvironment();
	
	/**
	 * Returns a status describing how the execution environment bound to this API
	 * profile satisfies the requirements of the components in this profile.
	 * 
	 * @return status describing execution environment bound to this profile
	 */
	public IStatus getExecutionEnvironmentStatus();
	
	/**
	 * Disposes this API profile. Clients must call this method when done
	 * with a profile in order to free system resources.
	 * <p>
	 * All API components in this profile are disposed.
	 * </p>
	 */
	public void dispose();
	
	/**
	 * Closes all components in this profile. The profile may still be used after closing,
	 * but clients should close the profile when they are done with it to free
	 * system resources.
	 * 
	 * @throws CoreException if closing fails
	 */
	public void close() throws CoreException;	
	
	/**
	 * Returns all components in this profile depending on the given components.
	 * The returned collection includes the given components and all dependents.
	 * 
	 * @param components the initial set of components
	 * @return an array of components for the given roots and all
	 * 	components in the state that depend on them
	 */
	public IApiComponent[] getDependentComponents(IApiComponent[] components);

	/**
	 * Returns all the prerequisite components in this profile for the given components. 
	 * The returned collection includes the given components and all prerequisites.
	 * 
	 * @param components the initial set of components
	 * @return an array of components for the given leaves and their
	 * 	prerequisite components in this profile
	 */
	public IApiComponent[] getPrerequisiteComponents(IApiComponent[] components);
	
}
