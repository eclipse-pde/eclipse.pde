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

/**
 * A field type member.
 * 
 * @since 1.1
 *
 */
public interface IApiField extends IApiMember {

	/**
	 * Returns whether this field represents a constant in an enum type.
	 * 
	 * @return whether this field represents a constant in an enum type
	 */
	public boolean isEnumConstant();
	
	/**
	 * Returns the constant value for this field or <code>null</code> if none.
	 * 
	 * @return constant value or <code>null</code>
	 */
	public Object getConstantValue();
}
