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
package org.eclipse.pde.api.tools.internal.search;

import org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;

/**
 * Describes a reference with element descriptors. Similar to an IReference but does not need
 * to be connected to an actual baseline/API components.
 */
public interface IReferenceDescriptor {
	/**
	 * The line number from which the reference was made or -1 if unknown.
	 * 
	 * @return source line number or -1
	 */
	public int getLineNumber();
	
	/**
	 * Returns the member descriptor where the reference exists.
	 * 
	 * @return member descriptor where the reference exists
	 */
	public IMemberDescriptor getMember();
	
	/**
	 * Returns the component descriptor where the reference exists.
	 * 
	 * @return component descriptor where the reference exists
	 */
	public IComponentDescriptor getComponent();

	/**
	 * Returns the specific kind of reference that was made.
	 * 
	 * @return reference kind - one of the reference modifiers
	 */
	public int getReferenceKind();
	
	/**
	 * Returns any flags set on the reference
	 * 
	 * @return any flags set on the reference
	 */
	public int getReferenceFlags();
	
	/**
	 * Returns the type of reference that has been made - one of type, field, or method.
	 * 
	 * @return one of the reference type constants defined in this interface
	 */
	public int getReferenceType();
	
	/**
	 * Returns a descriptor for the referenced member.
	 * 
	 * @return referenced member descriptor
	 */
	public IMemberDescriptor getReferencedMember();
	
	/**
	 * A descriptor for the referenced component.
	 * 
	 * @return referenced component descriptor
	 */
	public IComponentDescriptor getReferencedComponent();
	
	/**
	 * Describes the visibility of the reference.
	 * 
	 * @return visibility 
	 * @see org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers
	 */
	public int getVisibility();
	
	/**
	 * Returns the collection of reported problem messages for the original {@link IReference} or 
	 * <code>null</code> if there are no messages.
	 * 
	 * @return the list of problem messages or <code>null</code>
	 * @since 1.1
	 */
	public String[] getProblemMessages();
}
