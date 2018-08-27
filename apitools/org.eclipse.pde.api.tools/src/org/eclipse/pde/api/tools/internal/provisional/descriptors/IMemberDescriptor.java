/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal.provisional.descriptors;

/**
 * Describes a parented
 * {@link org.eclipse.pde.api.tools.internal.provisional.model.IApiElement}. For
 * example a class, method or field
 * <p>
 * A member has either an enclosing type for a parent. Top level types have a
 * package for a parent.
 * </p>
 *
 * @since 1.0.0
 */
public interface IMemberDescriptor extends IElementDescriptor {

	/**
	 * Returns the name of this member.
	 *
	 * @return member name
	 */
	public String getName();

	/**
	 * Returns a descriptor for the type this member is declared in or
	 * <code>null</code> if none.
	 *
	 * @return enclosing type or <code>null</code>
	 */
	public IReferenceTypeDescriptor getEnclosingType();

	/**
	 * Returns a descriptor for the package this member is contained in.
	 *
	 * @return package descriptor
	 */
	public IPackageDescriptor getPackage();
}
