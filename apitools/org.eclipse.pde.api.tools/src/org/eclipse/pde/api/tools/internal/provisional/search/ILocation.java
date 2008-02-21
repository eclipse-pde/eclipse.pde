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
package org.eclipse.pde.api.tools.internal.provisional.search;

import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;


/**
 * Describes a location in terms of an {@link IReference}.
 * 
 * @noimplement This interface is not to be implemented by clients.
 * @since 1.0.0
 */
public interface ILocation {
	
	/**
	 * Returns the component this location is contained in or <code>null</code>
	 * if unknown.
	 * 
	 * @return the containing component or <code>null</code> if unknown
	 */
	public IApiComponent getApiComponent();
	
	/**
	 * Returns the type containing this location.
	 * 
	 * @return type containing this location
	 */
	public IReferenceTypeDescriptor getType();
	
	/**
	 * Returns the member this location is associated with. May be the
	 * same as {@link #getType()} if this location is associated with
	 * a type rather than a type member.
	 * 
	 * @return associated member
	 */
	public IMemberDescriptor getMember();
	
	/**
	 * Returns the line number of the location. If there is no line number
	 * -1 is returned
	 * @return the line number of the location or -1 if there isn't one
	 */
	public int getLineNumber();

	/**
	 * Set the line number for the current location
	 * @param value new value for the line number
	 */
	public void setLineNumber(int value);
}
