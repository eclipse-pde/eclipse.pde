/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal.descriptors;

import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;

/**
 * Description of a field.
 *
 * @since 1.0.0
 */
public abstract class MemberDescriptorImpl extends NamedElementDescriptorImpl implements IMemberDescriptor {

	/**
	 * parent element or <code>null</code>
	 */
	private IElementDescriptor fParent;

	/**
	 * Constructs a member with the given name and parent
	 *
	 * @param name field name
	 * @param parent type containing the field declaration or package containing
	 *            the type
	 * @param modifiers
	 */
	MemberDescriptorImpl(String name, IElementDescriptor parent) {
		super(name);
		fParent = parent;
	}

	@Override
	public IReferenceTypeDescriptor getEnclosingType() {
		IElementDescriptor parent = getParent();
		if (parent instanceof IReferenceTypeDescriptor) {
			return (IReferenceTypeDescriptor) parent;
		}
		return null;
	}

	@Override
	public IPackageDescriptor getPackage() {
		IElementDescriptor parent = getParent();
		while (!(parent instanceof IPackageDescriptor)) {
			parent = ((MemberDescriptorImpl) parent).getParent();
		}
		return (IPackageDescriptor) parent;
	}

	@Override
	public IElementDescriptor getParent() {
		return fParent;
	}

}
