/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
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
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.objectweb.asm.Opcodes;

/**
 * Base implementation of {@link IApiMethod}
 * 
 * @since 1.0.0
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class ApiMethod extends ApiMember implements IApiMethod {
	/**
	 * Extra flags for polymorphic methods. Value doesn't collide with any of the values in org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants
	 */
	public static final int Polymorphic = 0x200000;
	
	private static final String INIT = "<init>"; //$NON-NLS-1$
	private static final String CLINIT = "<clinit>"; //$NON-NLS-1$	
	
	private String[] fExceptions;
	private String fDefaultValue;
	
	private IMethodDescriptor fHandle;
	
	/**
	 * Constructor
	 * @param enclosing enclosing type
	 * @param name method name
	 * @param signature method signature
	 * @param genericSig 
	 * @param flags
	 */
	protected ApiMethod(IApiType enclosing, String name, String signature, String genericSig, int flags, String[] exceptions) {
		super(enclosing, name, signature, genericSig, IApiElement.METHOD, flags);
		fExceptions = exceptions;
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod#isConstructor()
	 */
	public boolean isConstructor() {
		return getName().equals(INIT);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.model.ApiMember#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof IApiMethod) {
			return super.equals(obj) && ((IApiMethod)obj).getSignature().equals(getSignature());
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.model.ApiMember#hashCode()
	 */
	public int hashCode() {
		return super.hashCode() + getSignature().hashCode();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod#getExceptionNames()
	 */
	public String[] getExceptionNames() {
		return fExceptions;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod#isClassInitializer()
	 */
	public boolean isClassInitializer() {
		return getName().equals(CLINIT);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod#getDefaultValue()
	 */
	public String getDefaultValue() {
		return fDefaultValue;
	}

	/**
	 * Used when building a type structure.
	 * 
	 * @param value default value
	 */
	public void setDefaultValue(String value) {
		fDefaultValue = value;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod#isSynthetic()
	 */
	public boolean isSynthetic() {
		return (getModifiers() & Opcodes.ACC_SYNTHETIC) != 0;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod#isSynthetic()
	 */
	public boolean isPolymorphic() {
		return (getModifiers() & Polymorphic) != 0;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer
			.append("Method : access(") //$NON-NLS-1$
			.append(getModifiers())
			.append(") ") //$NON-NLS-1$
			.append(getSignature())
			.append(' ')
			.append(getName());
		if (getExceptionNames() != null) {
			buffer.append(" throws "); //$NON-NLS-1$
			for (int i = 0; i < getExceptionNames().length; i++) {
				if (i > 0) buffer.append(',');
				buffer.append(getExceptionNames()[i]);
			}
		}
		buffer.append(';').append(Util.LINE_DELIMITER);
		if (getGenericSignature() != null) {
			buffer
				.append(" Generic signature : ") //$NON-NLS-1$
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
				fHandle = ((IReferenceTypeDescriptor)type.getHandle()).getMethod(getName(), getSignature());
			} catch (CoreException e) {
				// should not happen for field or method - enclosing type is cached
			}
		}
		return fHandle;
	}

}
