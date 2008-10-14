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
 * A reference to a field. The declaring type of the field is described
 * by this reference's {@link #getTypeName()}.
 * 
 * @since 1.1
 */
public interface IFieldReference extends ITypeReference {

	/**
	 * Returns the name of the field that was referenced.
	 * 
	 * @return field name
	 */
	public String getFieldName();
}
