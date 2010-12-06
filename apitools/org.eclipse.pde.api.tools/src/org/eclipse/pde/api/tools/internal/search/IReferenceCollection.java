/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.search;



/**
 * Describes a reference with element descriptors. Similar to an IReference but does not need
 * to be connected to an actual baseline/API components.
 */
public interface IReferenceCollection {

	/**
	 * Adds the reference descriptor for a type to the collection
	 * @param type type
	 * @param referenceDescriptor reference descriptor to be added
	 */
	public void add(String type, IReferenceDescriptor referenceDescriptor);
	
	/**
	 * Checks if a reference for a given type already exists in the collection
	 * @param type
	 * @return Returns <code>true</code> if the the collection has references for the given type,
	 * <code>false</code> otherwise.
	 */
	public boolean hasReferencesTo(String type);
		
	/**
	 * Returns the list of all the references made to this component in the API Use Scan report
	 * @return the list of reference descriptors
	 */
	public IReferenceDescriptor[] getAllExternalDependencies();
	
	/**
	 * Returns the list of all the references made to a particular member of this component 
	 * in the API Use Scan report
	 * @param types to which references have been made
	 * @return the list of reference descriptors
	 */
	public IReferenceDescriptor[] getExternalDependenciesTo(String[] types);
	
	/**
	 * Clears the collection
	 */
	public void clear();
}
