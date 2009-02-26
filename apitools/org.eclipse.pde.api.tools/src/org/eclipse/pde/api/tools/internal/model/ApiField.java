/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.model;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiField;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Base implementation of {@link IApiField}
 * 
 * @since 1.0.0
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class ApiField extends ApiMember implements IApiField {
	
	/**
	 * Constant value
	 */
	private Object fValue;
	
	private IFieldDescriptor fHandle;
	
	/**
	 * Constructor
	 * @param parent the enclosing type of the field
	 * @param name the name of the field
	 * @param signature the signature for the field
	 * @param genericSig the generic signature of the field
	 * @param flags the flags for the field
	 * @param value the value assigned to the field
	 * @param value constant value or <code>null</code> if none
	 */
	protected ApiField(IApiType enclosing, String name, String signature, String genericSig, int flags, Object value) {
		super(enclosing, name, signature, genericSig, IApiElement.FIELD, flags);
		fValue = value;
	}
	
	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiField#isEnumConstant()
	 */
	public boolean isEnumConstant() {
		return (Flags.isEnum(getModifiers()));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.model.ApiMember#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof IApiField) {
			return super.equals(obj);
		}
		return false;
	}

	public int hashCode() {
		return super.hashCode() + (this.fValue != null ? this.fValue.hashCode() : 0);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiField#getConstantValue()
	 */
	public Object getConstantValue() {
		return fValue;
	}
	
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer
			.append("Field : access(") //$NON-NLS-1$
			.append(getModifiers())
			.append(") ") //$NON-NLS-1$
			.append(getSignature())
			.append(' ')
			.append(getName())
			.append(" isEnum constant ") //$NON-NLS-1$
			.append(isEnumConstant());
		if (getConstantValue() != null) {
			buffer.append(" = ").append(getConstantValue()); //$NON-NLS-1$
		}
		buffer.append(';').append(Util.LINE_DELIMITER);
		if (getGenericSignature() != null) {
			buffer
				.append(" Signature : ") //$NON-NLS-1$
				.append(getGenericSignature()).append(Util.LINE_DELIMITER);
		}
		return String.valueOf(buffer);
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiMember#getHandle()
	 */
	public IMemberDescriptor getHandle() {
		if (fHandle == null) {
			try {
				IApiType type = getEnclosingType();
				fHandle = ((IReferenceTypeDescriptor)type.getHandle()).getField(getName());
			} catch (CoreException e) {
				// should not happen for field or method - enclosing type is cached
			}
		}
		return fHandle;
	}
}
