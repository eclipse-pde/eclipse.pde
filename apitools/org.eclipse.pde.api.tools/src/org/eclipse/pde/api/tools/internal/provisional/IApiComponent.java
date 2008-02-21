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
package org.eclipse.pde.api.tools.internal.provisional;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;



/**
 * Describes the API of a software component. An API component
 * is composed of a set of class files owned by the component and
 * a description (manifest) of the component's API. 
 * 
 * @since 1.0.0
 */
public interface IApiComponent extends IClassFileContainer {
	
	/**
	 * Export option key {@link String} value specifying an absolute file
	 * system path to a directory where the component should be exported.
	 */
	public static final String EXPORT_DIRECTORY = "EXPORT_DIRECTORY"; //$NON-NLS-1$
	
	/**
	 * Optional export option key for a {@link Boolean} value indicating if class
	 * files should be exported as stubs rather than original class files.
	 * Has no effect if the component was generated from stubs - since original
	 * class files cannot be regenerated from stubs. When unspecified class files
	 * are exported in their original form.
	 */
	public static final String EXPORT_CLASS_FILE_STUBS = "EXPORT_CLASS_FILE_STUBS"; //$NON-NLS-1$
	
	/**
	 * Optional export option key for a {@link Boolean} value indicating if generated
	 * archive should be compressed. When unspecified, archive is not compressed.
	 */
	public static final String EXPORT_COMPRESS = "EXPORT_COMPRESS"; //$NON-NLS-1$	

	/**
	 * Returns a human readable name for this component.
	 * 
	 * @return component name
	 * @exception CoreException if unable to retrieve the name
	 */
	public String getName() throws CoreException;
	
	/**
	 * Returns this component's symbolic name.
	 * 
	 * @return component's symbolic name
	 */
	public String getId();
	
	/**
	 * Returns this component's API description.
	 * 
	 * @return API manifest
	 * @throws CoreException if there was a problem creating the API description for this component
	 */
	public IApiDescription getApiDescription() throws CoreException;
	
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
	 */
	public String[] getExecutionEnvironments();
	
	/**
	 * Returns class file containers containing the class files associated with
	 * this component in the order they would appear on the build path
	 * or class path.
	 * 
	 * @return class file containers
	 */
	public IClassFileContainer[] getClassFileContainers();
	
	/**
	 * Returns class file containers containing the class files associated with
	 * this component that comes from the given component id in the order they 
	 * would appear on the build path or class path.
	 * 
	 * This is used to filter out the class file containers coming from a fragment
	 * 
	 * @param id the given component id
	 * @return class file containers
	 */
	public IClassFileContainer[] getClassFileContainers(String id);

	/**
	 * Returns a collection of descriptions of components required by this
	 * component, or an empty collection if none.
	 * 
	 * @return required component descriptions, possibly empty
	 */
	public IRequiredComponentDescription[] getRequiredComponents();	
	
	/**
	 * Returns the location of this API component.
	 * 
	 * @return location
	 */
	public String getLocation();
	
	/**
	 * Returns if the component is a system component (a JRE definition for example) or not.
	 * System components are not persisted with profiles, as they are recreated dynamically
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
	 */
	public boolean isSourceComponent();
	/**
	 * Disposes this API component. Clients must call this method when done
	 * with an API component. Note that disposing an {@link IApiProfile} will dispose all of
	 * its components. 
	 */
	public void dispose();
	
	/**
	 * Returns the profile this component is contained in.
	 * 
	 * @return API profile
	 */
	public IApiProfile getProfile();
	
	/**
	 * Exports this API component based on the supplied export options.
	 * 
	 * @param options export options
	 * @param monitor progress monitor or <code>null</code> if none
	 * @throws CoreException
	 */
	public void export(Map options, IProgressMonitor monitor) throws CoreException;
	
	/**
	 * Returns a store of problem filters defined for this component or <code>null</code>
	 * if none.
	 * 
	 * @return filter store or <code>null</code>
	 * @throws CoreException
	 */
	public IApiFilterStore getFilterStore() throws CoreException;
	
	/**
	 * Creates and returns a new problem filter that filters are problems contained
	 * in the specified element applicable to the given kinds.
	 * 
	 * @param element the element problems are filtered from.
	 * @param kinds specific problem kinds to filter for this element.
	 * @return new problem filter
	 */
	public IApiProblemFilter newProblemFilter(IElementDescriptor element, String[] kinds);
	
	/**
	 * Returns whether this API component is a fragment.
	 * 
	 * @return whether this API component is a fragment
	 */
	public boolean isFragment();
	
	/**
	 * Returns whether this API component is the host of one or more fragments.
	 * 
	 * @return whether this API component is the host of one or more fragments
	 */
	public boolean hasFragments();
}
