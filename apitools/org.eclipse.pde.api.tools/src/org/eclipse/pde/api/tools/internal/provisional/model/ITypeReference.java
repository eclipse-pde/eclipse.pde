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
 * @since 1.1
 */
public interface ITypeReference  extends IReference {

	/**
	 * Returns the fully qualified name of the type that was referenced.
	 * 
	 * @return fully qualified name of the type that was referenced
	 */
	public String getTypeName();
}
