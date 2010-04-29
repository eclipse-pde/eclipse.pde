/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
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
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;


/**
 * Describes an {@link IApiElement} that can be parented: i.e. an element that can appear as
 * a member of another {@link IApiElement}.
 * <br><br>
 * For example a type, field or method.
 * 
 * @see IApiType
 * @see IApiMethod
 * @see IApiField
 * 
 * @since 1.1
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IApiMember extends IApiElement {
	
	/**
	 * Returns the type this member is declared in or <code>null</code>
	 * if none.
	 * 
	 * @return enclosing type or <code>null</code>
	 * @exception CoreException if unable to retrieve enclosing type
	 */
	public IApiType getEnclosingType() throws CoreException;
	
	/**
	 * Returns the modifier bit mask associated with this member or -1 if unknown.
	 * Modifiers are as defined by {@link org.eclipse.jdt.core.Flags}
	 * 
	 * @return modifiers bit mask
	 * @exception CoreException if this element does not exist or there in an 
	 *  error reading its underlying storage
	 */
	public int getModifiers();
	
	/**
	 * Returns the signature of member
	 * 
	 * @return member signature
	 */
	public String getSignature();
	
	/**
	 * Returns type generic signature information about this member or <code>null</code>
	 * if none. May contain extra information for parameterized types.
	 * 
	 * @return generic type signature information for this type or <code>null</code>
	 */
	public String getGenericSignature();
	
	/**
	 * Returns the name of the package that this member is located in
	 * 
	 * @return the name of the enclosing package for this member
	 */
	public String getPackageName();
	
	/**
	 * Returns the associated element descriptor for this member.
	 * 
	 * @return element descriptor
	 */
	public IMemberDescriptor getHandle();

}
