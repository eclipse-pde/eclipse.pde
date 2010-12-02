/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
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
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.IRequiredComponentDescription;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.search.IReferenceCollection;

/**
 * Describes the API of a software component. An API component
 * is composed of a set of {@link IApiTypeRoot}s owned by the component and
 * a description (manifest) of the component's API. 
 * 
 * @since 1.0.0
 */
public interface IApiComponent extends IApiTypeContainer {
	
	/**
	 * Returns this component's symbolic name. This is a handle-only 
	 * method - the component may not exist or may be disposed.
	 * 
	 * @return component's symbolic name
	 */
	public String getSymbolicName();
	
	/**
	 * Returns this component's API description.
	 * 
	 * @return API description for this component
	 * @throws CoreException if there was a problem creating the API description for this component
	 * @throws CoreException if its baseline is disposed
	 */
	public IApiDescription getApiDescription() throws CoreException;
	/**
	 * Returns whether this component has an underlying API description. Even if a component
	 * has no underlying description it will return one from {@link #getApiDescription()},
	 * but it will be empty. This method allows clients to know if there was any thing used
	 * to populate the description.
	 * 
	 * @return whether this component has an underlying API description
	 */
	public boolean hasApiDescription();
	
	/**
	 * Returns this component's version identifier.
	 * 
	 * @return component version
	 */
	public String getVersion();
	
	/**
	 * Returns the execution environments required by this component for building
	 * and running. An execution environment is represented by a unique identifier
	 * as defined by OSGi - for example "J2SE-1.4" or "CDC-1.0/Foundation-1.0".
	 * Any of the environments will allow this component to be resolved.
	 * 
	 * @return execution environment identifier
	 * @throws CoreException if its baseline is disposed
	 */
	public String[] getExecutionEnvironments() throws CoreException;
	
	/**
	 * Returns {@link IApiTypeContainer}s containing the {@link IApiTypeRoot}s associated with
	 * this component in the order they would appear on the build path
	 * or class path.
	 * 
	 * @return {@link IApiTypeContainer}s
	 */
	public IApiTypeContainer[] getApiTypeContainers() throws CoreException;
	
	/**
	 * Returns {@link IApiTypeContainer}s containing the {@link IApiTypeRoot}s associated with
	 * this component that comes from the given component id in the order they 
	 * would appear on the build path or class path.
	 * 
	 * This is used to filter out the {@link IApiTypeContainer}s coming from a fragment
	 * 
	 * @param id the given component id
	 * @return {@link IApiTypeContainer}s
	 * @throws CoreException if its baseline is disposed
	 */
	public IApiTypeContainer[] getApiTypeContainers(String id) throws CoreException;

	/**
	 * Returns a collection of descriptions of components required by this
	 * component or an empty collection if none.
	 * 
	 * @return required component descriptions, possibly empty
	 * @throws CoreException if its baseline is disposed
	 */
	public IRequiredComponentDescription[] getRequiredComponents() throws CoreException;
	
	/**
	 * Returns the location of this API component.
	 * 
	 * @return location
	 */
	public String getLocation();
	
	/**
	 * Returns if the component is a system component (a JRE definition for example) or not.
	 * System components are not persisted with baselines, as they are recreated dynamically
	 * by the framework 
	 * @return true if the component is a system component, false otherwise
	 */
	public boolean isSystemComponent();
	
	/**
	 * Returns if the component is a source component or not.
	 * A source component is not persisted with profiles, as they don't contain any binaries.
	 * 
	 * <p>A component is a source component if and only if one of the following conditions is true:</p>
	 * <ul>
	 * <li>its manifest contains an entry called <code>Eclipse-SourceBundle</code></li>
	 * <li>its <code>plugin.xml</code> file contains an extension for the extension point if the
	 * component is not a fragment.
	 * <li>its <code>fragment.xml</code> file contains an extension for the extension point if the
	 * component is a fragment.
	 * <code>org.eclipse.pde.core.source</code></li>
	 * </ul>
	 *
	 * @return true if the component is a source component, false otherwise
	 * @throws CoreException if its baseline is disposed
	 */
	public boolean isSourceComponent() throws CoreException;
	
	/**
	 * Disposes this API component. Clients must call this method when done
	 * with an API component. Note that disposing an {@link IApiBaseline} will dispose all of
	 * its components. 
	 */
	public void dispose();
	
	/**
	 * Returns the {@link IApiBaseline} this component is contained in.
	 * 
	 * @return the parent {@link IApiBaseline}
	 * @throws CoreException if its baseline is disposed
	 */
	public IApiBaseline getBaseline() throws CoreException;
		
	/**
	 * Returns a store of problem filters defined for this component or <code>null</code>
	 * if none.
	 * 
	 * @return filter store or <code>null</code>
	 * @throws CoreException if its baseline is disposed
	 */
	public IApiFilterStore getFilterStore() throws CoreException;
	
	/**
	 * Returns whether this API component is a fragment.
	 * 
	 * @return whether this API component is a fragment
	 * @throws CoreException if its baseline is disposed
	 */
	public boolean isFragment() throws CoreException;
	
	/**
	 * Returns the host {@link IApiComponent} for this component iff this component is a fragment. Otherwise
	 * <code>null</code> is returned.
	 * 
	 * @return the host {@link IApiComponent} for this component or <code>null</code>.
	 * @throws CoreException if the baseline is disposed
	 */
	public IApiComponent getHost() throws CoreException;
	
	/**
	 * Returns whether this API component is the host of one or more fragments.
	 * 
	 * @return whether this API component is the host of one or more fragments
	 * @throws CoreException if its baseline is disposed
	 */
	public boolean hasFragments() throws CoreException;
	
	/**
	 * Returns the lowest execution environments required by this component for building
	 * and running. An execution environment is represented by a unique identifier
	 * as defined by OSGi - for example "J2SE-1.4" or "CDC-1.0/Foundation-1.0".
	 * 
	 * <p>The result will be more than one execution environment when the corresponding api component
	 * has a mixed of execution environments in the JRE family (JRE-1.1, J2SE-1.2,...) and in OSGi minimums
	 * or cdc/foundation families.</p>
	 * <p>Since the latter ones are not exact subsets of the ones from the JREs family, we need to return all
	 * the incompatible ones.</p>
	 * 
	 * @return execution environment identifiers
	 * @throws CoreException if its baseline is disposed
	 */
	public String[] getLowestEEs() throws CoreException;
	/**
	 * Returns the list of errors found during the component resolution.
	 * 
	 * @return the list of errors or <code>null</code>  if none
	 * @throws CoreException if its baseline is disposed
	 */
	public ResolverError[] getErrors() throws CoreException;
	
	/**
	 * Returns the associated element descriptor for this member.
	 * 
	 * @return element descriptor
	 */
	public IElementDescriptor getHandle();
	
	/**
	 * Returns all references to this component as registered by API use scans with the Use Scan Manager.
	 * @return the collection of reference descriptors
	 */
	public IReferenceCollection getExternalDependencies();
}
