/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.provisional.builder;

/**
 * A context of types reported from the API analysis builder
 * 
 * @since 1.0.1
 */
public interface IBuildContext {
	
	/**
	 * Returns the collection of structurally changed types that have been reported by the 
	 * {@link ApiAnalysisBuilder}.
	 * If no types have been reported as changed an empty array is returned, never <code>null</code>.
	 * 
	 * @return the collection of API visible types that have been changed or an empty array
	 */
	public String[] getStructurallyChangedTypes();
	
	/**
	 * Returns a collection of types that have had an API description change.
	 * If no types have had an API description change, an empty array is returned, never <code>null</code>.
	 *  
	 * @return the collection of API visible types that have had a description change or an empty array
	 */
	public String[] getDescriptionChangedTypes();

	/**
	 * Returns the collection of types that have been removed (where an {@link IResourceDelta#REMOVED} delta was found)
	 * If no types have been reported as removed an empty array is returned, never <code>null</code>.
	 * 
	 * @return the collection of removed types or an empty array, never <code>null</code>
	 */
	public String[] getRemovedTypes();
	
	/**
	 * Returns the complete collection of types dependent the description changed types reported from the
	 * {@link org.eclipse.pde.api.tools.internal.builder.ApiAnalysisBuilder}.
	 * If no types have been reported as dependent an empty array is returned, never <code>null</code>.
	 * 
	 * @return the complete collection of types dependent on description changes or an empty array
	 */
	public String[] getDescriptionDependentTypes();
	
	/**
	 * Cleans up the build context and frees any held memory
	 */
	public void dispose();
	
	/**
	 * Returns if the build context has any structurally changed types. 
	 * 
	 * Has better performance impact than getting the collection of changed
	 * type names to ask for the size.
	 * 
	 * @return true if there are changed type names recorded, false otherwise
	 */
	public boolean hasStructuralChanges();
	
	/**
	 * Returns if the build context has any recorded API description changes.
	 * 
	 * @return whether any types have API description changes
	 */
	public boolean hasDescriptionChanges();
	
	/**
	 * Returns whether this build context has any type dependent on API description changes.
	 * 
	 * @return <code>true</code> if there are type dependent on API description changes, otherwise <code>false</code>
	 */
	public boolean hasDescriptionDependents();
	
	/**
	 * Returns if the build context has any recorded removed types. 
	 * 
	 * Has better performance impact than getting the collection of removed
	 * type names to ask for the size.
	 * 
	 * @return true if there are removed type names recorded, false otherwise
	 */
	public boolean hasRemovedTypes();
	
	/**
	 * Returns if this build context has any recorded
	 * types that require incremental building.
	 * 
	 * @return true if there are types to build incrementally, false otherwise
	 */
	public boolean hasTypes();
	
	/**
	 * Returns if this build context contains the given type name in its collection of
	 * types with API description changes.
	 *  
	 * @param typename
	 * @return true if this context contains the given type name, false otherwise
	 */
	public boolean containsDescriptionChange(String typename);
	
	/**
	 * Returns if this build context contains the given type name in its collection of
	 * types dependent on API description changes.
	 *  
	 * @param typename
	 * @return true if this context contains the given type name, false otherwise
	 */
	public boolean containsDescriptionDependent(String typename);	
	
	/**
	 * Returns if this build context contains the given type name in its changed types
	 * collection.
	 *  
	 * @param typename
	 * @return true if this context contains the given type name, false otherwise
	 */
	public boolean containsStructuralChange(String typename);
	
	/**
	 * Returns if this build context contains the given type name 
	 * in its removed types collection
	 * 
	 * @param typename
	 * @return true if this context contains the given type, false otherwise
	 */
	public boolean containsRemovedType(String typename);
}
