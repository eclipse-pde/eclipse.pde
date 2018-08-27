/*******************************************************************************
 * Copyright (c) 2008, 2014 IBM Corporation and others.
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
 * A method type member.
 *
 * @since 1.1
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IApiMethod extends IApiMember {

	/**
	 * Returns the signature of this method. This includes the signatures for
	 * the parameter types and return type, but does not include the method
	 * name, exception types, or type parameters.
	 * <p>
	 * For example, a method declared as
	 * <code>public void foo(String text, int length)</code> would return
	 * <code>"(Ljava.lang.String;I)V"</code>.
	 * </p>
	 *
	 * @return the signature of this method
	 */
	@Override
	public String getSignature();

	/**
	 * Returns whether this method is a constructor.
	 *
	 * @return whether this method is a constructor
	 */
	public boolean isConstructor();

	/**
	 * Returns whether this method is a class initializer.
	 *
	 * @return whether this method is a class initializer
	 */
	public boolean isClassInitializer();

	/**
	 * Returns whether this method is a default method or not.
	 *
	 * @return whether this method is a default method
	 */
	public boolean isDefaultMethod();

	/**
	 * Returns names of exceptions thrown by this method, or <code>null</code>
	 * if none
	 *
	 * @return exception names thrown by this method or <code>null</code>
	 */
	public String[] getExceptionNames();

	/**
	 * Returns the default value for this method or <code>null</code> if none.
	 * Only applies to methods contained in annotations.
	 *
	 * @return default value or <code>null</code>
	 */
	public String getDefaultValue();

	/**
	 * Returns whether this method is synthetic.
	 *
	 * @return whether this method is synthetic
	 */
	public boolean isSynthetic();

	/**
	 * Returns whether this method is polymorphic.
	 *
	 * @return whether this method is polymorphic
	 */
	public boolean isPolymorphic();
}
