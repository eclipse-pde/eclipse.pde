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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
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

	@Override
	public IFieldDescriptor getField(String name) {
		return new FieldDescriptorImpl(name, this);
	}

	@Override
	public IMethodDescriptor getMethod(String name, String signature) {
		return new MethodDescriptorImpl(name, this, signature);
	}

	@Override
	public IReferenceTypeDescriptor getType(String simpleName) {
		return new ReferenceTypeDescriptorImpl(simpleName, this);
	}

	@Override
	public String toString() {
		return getQualifiedName();
	}

	@Override
	public synchronized String getQualifiedName() {
		if (fFullName == null) {
			StringBuilder buffer = new StringBuilder();
			buffer.append(getPackage().getName());
			if (buffer.length() > 0) {
				buffer.append('.');
			}
			List<IReferenceTypeDescriptor> all = null;
			IReferenceTypeDescriptor enclosingType = getEnclosingType();
			while (enclosingType != null) {
				if (all == null) {
					all = new ArrayList<>();
				}
				all.add(0, enclosingType);
				enclosingType = enclosingType.getEnclosingType();
			}
			if (all != null) {
				for (IReferenceTypeDescriptor desc : all) {
					buffer.append(desc.getName());
					buffer.append('$');
				}
			}
			buffer.append(getName());
			fFullName = buffer.toString();
		}
		return fFullName;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IReferenceTypeDescriptor) {
			IReferenceTypeDescriptor refType = (IReferenceTypeDescriptor) obj;
			return getQualifiedName().equals(refType.getQualifiedName());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return getQualifiedName().hashCode();
	}

	@Override
	public int compareTo(IElementDescriptor o) {
		if (o instanceof ReferenceTypeDescriptorImpl) {
			return getQualifiedName().compareTo(((ReferenceTypeDescriptorImpl) o).getQualifiedName());
		}
		if (ApiPlugin.DEBUG_ELEMENT_DESCRIPTOR_FRAMEWORK) {
			System.err.println(o.getClass());
		}
		return super.compareTo(o);
	}

	@Override
	public int getElementType() {
		return IElementDescriptor.TYPE;
	}

	@Override
	public String getSignature() {
		if (fSignature == null) {
			StringBuilder buf = new StringBuilder();
			buf.append('L');
			buf.append(getQualifiedName());
			buf.append(';');
			fSignature = buf.toString();
		}
		return fSignature;
	}

	@Override
	public String getGenericSignature() {
		return fGenericSignature;
	}

	@Override
	public boolean isAnonymous() {
		if (getEnclosingType() != null) {
			try {
				Integer.parseInt(getName());
				return true;
			} catch (NumberFormatException e) {
				// ignore
			}
		}
		return false;
	}

}
