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
 * Describes a method call site. The {@link #getTypeName()} indicates the receiving type
 * (<b>not</b> necessarily the declaring type).
 * 
 * @since 1.1
 */
public interface IMethodReference extends ITypeReference {

	/**
	 * Name of the method referenced.
	 * 
	 * @return method name
	 */
	public String getMethodName();
	
	/**
	 * Returns the signature of the referenced method.
	 * 
	 * @return method signature
	 */
	public String getMethodSignature();
}
