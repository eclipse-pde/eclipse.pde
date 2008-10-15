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
 * Describes a method.
 * 
 * @since 1.0.0
 */
public interface IMethodDescriptor extends IMemberDescriptor {
	
	/**
	 * Returns the signature of this method. This includes the signatures for the
	 * parameter types and return type, but does not include the method name,
	 * exception types, or type parameters.
	 * <p>
	 * For example, a method declared as <code>public void foo(String text, int length)</code>
	 * would return <code>"(Ljava.lang.String;I)V"</code>.
	 * </p>
	 * @return the signature of this method
	 */
	public String getSignature();
	
	/**
	 * Returns whether this method is a constructor
	 * 
	 * @return whether this method is a constructor
	 */
	public boolean isConstructor();

}
