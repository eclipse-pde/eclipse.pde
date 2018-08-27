/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.provisional.model;

/**
 * A field type member.
 *
 * @see IApiType
 * @see IApiMethod
 *
 * @since 1.1
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
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
