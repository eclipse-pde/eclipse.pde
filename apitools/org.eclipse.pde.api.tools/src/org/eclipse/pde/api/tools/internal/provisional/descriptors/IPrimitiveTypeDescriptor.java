/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
 * Description of a primitive type.
 *  <p>
 * A primitive type has no parent.
 * </p>
 * @since 1.0.0
 */
public interface IPrimitiveTypeDescriptor extends ITypeDescriptor {
	
	/**
	 * Returns an array type descriptor of this type with the specified number
	 * of dimensions.
	 * 
	 * @param dimensions number of dimensions in the array
	 * @return array type descriptor
	 */
	public IArrayTypeDescriptor getArray(int dimensions);	
}
