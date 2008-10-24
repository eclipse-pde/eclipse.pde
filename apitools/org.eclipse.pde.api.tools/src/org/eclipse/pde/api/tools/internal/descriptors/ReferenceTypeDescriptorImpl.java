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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;

/**
 * Reference type descriptor.
 * 
 * @since 1.0.0
 */
public class ReferenceTypeDescriptorImpl extends MemberDescriptorImpl implements IReferenceTypeDescriptor {
	
	/**
	 * Fully qualified name
	 */
	private String fFullName = null;
	
	/**
	 * Type signature
	 */
	private String fSignature = null;
	
	/**
	 * Generic information or <code>null</code>
	 */
	private String fGenericSignature = null;

	/**
	 * Constructs a type descriptor with the given name and parent.
	 * 
	 * @param name simple type name
	 * @param parent package or enclosing type
	 */
	ReferenceTypeDescriptorImpl(String name, IElementDescriptor parent) {
		super(name, parent);
		
	}
	
	/**
	 * Constructs a type descriptor with the given name and parent.
	 * 
	 * @param name simple type name
	 * @param parent package or enclosing type
	 * @param genericSignature generic signature info or <code>null</code>
	 */
	ReferenceTypeDescriptorImpl(String name, IElementDescriptor parent, String genericSignature) {
		this(name, parent);
		fGenericSignature = genericSignature;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IReferenceTypeDescriptor#getField(java.lang.String)
	 */
	public IFieldDescriptor getField(String name) {
		return new FieldDescriptorImpl(name, this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IReferenceTypeDescriptor#getMethod(java.lang.String, java.lang.String)
	 */
	public IMethodDescriptor getMethod(String name, String signature) {
		return new MethodDescriptorImpl(name, this, signature);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IReferenceTypeDescriptor#getType(java.lang.String)
	 */
	public IReferenceTypeDescriptor getType(String simpleName) {
		return new ReferenceTypeDescriptorImpl(simpleName, this);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getQualifiedName();
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IReferenceTypeDescriptor#getQualifiedName()
	 */
	public synchronized String getQualifiedName() {
		if (fFullName == null) {
			StringBuffer buffer = new StringBuffer();
			buffer.append(getPackage().getName());
			if (buffer.length() > 0) {
				buffer.append('.');
			}
			List all = null;
			IReferenceTypeDescriptor enclosingType = getEnclosingType();
			while (enclosingType != null) {
				if (all == null) {
					all = new ArrayList();
				}
				all.add(0, enclosingType);
				enclosingType = enclosingType.getEnclosingType();
			}
			if (all != null) {
				Iterator iterator = all.iterator();
				while (iterator.hasNext()) {
					buffer.append(((IReferenceTypeDescriptor)iterator.next()).getName());
					buffer.append('$');
				}
			}
			buffer.append(getName());
			fFullName = buffer.toString();
		}
		return fFullName;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof IReferenceTypeDescriptor) {
			IReferenceTypeDescriptor refType = (IReferenceTypeDescriptor) obj;
			return getQualifiedName().equals(refType.getQualifiedName());
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getQualifiedName().hashCode();
	}		

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.descriptors.ElementDescriptorImpl#getComparable()
	 */
	protected Comparable getComparable() {
		return getQualifiedName();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IElementDescriptor#getElementType()
	 */
	public int getElementType() {
		return IElementDescriptor.TYPE;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IReferenceTypeDescriptor#getSignature()
	 */
	public String getSignature() {
		if (fSignature == null) {
			StringBuffer buf = new StringBuffer();
			buf.append('L');
			buf.append(getQualifiedName());
			buf.append(';');
			fSignature = buf.toString();
		}
		return fSignature;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IReferenceTypeDescriptor#getGenericSignature()
	 */
	public String getGenericSignature() {
		return fGenericSignature;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.descriptors.IReferenceTypeDescriptor#isAnonymous()
	 */
	public boolean isAnonymous() {
		if (getEnclosingType() != null) {
			try {
				Integer.parseInt(getName());
				return true;
			} catch (NumberFormatException e) {
			}
		}
		return false;
	}

}
