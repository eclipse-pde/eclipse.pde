/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;


/**
 * Features common to type members.
 * 
 * @since 1.1
 */
public interface IApiMember extends IApiElement {
	
	/**
	 * Returns a the type this member is declared in or <code>null</code>
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
	 * Returns the API component this type originated from or <code>null</code>
	 * if unknown.
	 * 
	 * @return API component this type originated from or <code>null</code>
	 */
	public IApiComponent getApiComponent();
	
	/**
	 * Returns the associated element descriptor for this member.
	 * 
	 * @return element descriptor
	 */
	public IMemberDescriptor getHandle();

}
