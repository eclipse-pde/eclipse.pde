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
package org.eclipse.pde.api.tools.internal.provisional.descriptors;



/**
 * Description of an array type.
 * <p>
 * An array type has no parent.
 * </p>
 * @since 1.0.0
 */
public interface IArrayTypeDescriptor extends ITypeDescriptor {
	
	/**
	 * Returns a descriptor for the type of elements in this array.
	 * 
	 * @return component type
	 */
	public ITypeDescriptor getComponentType();
	
	/**
	 * Returns the number of dimensions in this array.
	 * 
	 * @return the number of dimensions in this array
	 */
	public int getDimensions();
	
}
