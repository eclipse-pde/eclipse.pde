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
package org.eclipse.pde.api.tools.internal.descriptors;

import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;

/**
 * Package description.
 * 
 * @since 1.0.0
 */
public class PackageDescriptorImpl extends NamedElementDescriptorImpl implements IPackageDescriptor {

	/**
	 * Constructs a package description
	 * 
	 * @param name dot qualified package name, empty string for default package
	 */
	public PackageDescriptorImpl(String name) {
		super(name);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		String name = getName();
		return name.equals("") ? "<default package>" : name; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof IPackageDescriptor) {
			IPackageDescriptor pkg = (IPackageDescriptor) obj;
			return getName().equals(pkg.getName());
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getName().hashCode();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IElementDescriptor#getElementType()
	 */
	public int getElementType() {
		return IElementDescriptor.PACKAGE;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IPackageDescriptor#getType(java.lang.String, java.lang.String)
	 */
	public IReferenceTypeDescriptor getType(String typeQualifiedName, String signature) {
		String[] names = typeQualifiedName.split("\\$"); //$NON-NLS-1$
		IReferenceTypeDescriptor typeDescriptor = new ReferenceTypeDescriptorImpl(names[0], this, signature);
		for (int i = 1; i < names.length; i++) {
			typeDescriptor = typeDescriptor.getType(names[i]);
		}
		return typeDescriptor;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor#getType(java.lang.String)
	 */
	public IReferenceTypeDescriptor getType(String typeQualifiedName) {
		String[] names = typeQualifiedName.split("\\$"); //$NON-NLS-1$
		IReferenceTypeDescriptor typeDescriptor = new ReferenceTypeDescriptorImpl(names[0], this);
		for (int i = 1; i < names.length; i++) {
			typeDescriptor = typeDescriptor.getType(names[i]);
		}
		return typeDescriptor;
	}	
}
