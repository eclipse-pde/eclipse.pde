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
 * Describes a reference to a type, field, or method.
 * 
 * @since 1.1
 */
public interface IReference {
	
	/**
	 * An {@link IReference} of this type can be safely cast to an {@link ITypeReference}.
	 */
	public static final int T_TYPE_REFERENCE = 1;
	/**
	 * An {@link IReference} of this type can be safely cast to an {@link IFieldReference}.
	 */	
	public static final int T_FIELD_REFERENCE = 2;
	/**
	 * An {@link IReference} of this type can be safely cast to an {@link IMethodReference}.
	 */
	public static final int T_METHOD_REFERENCE = 3;
	
	/**
	 * The line number from which the reference was made or -1 if unknown.
	 * 
	 * @return source line number or -1
	 */
	public int getLineNumber();
	
	/**
	 * Returns the member where the reference exists.
	 * 
	 * @return member where the reference exists
	 */
	public IApiMember getMember();
	
	/**
	 * Returns the specific kind of reference that was made.
	 * See {@link org.eclipse.pde.api.tools.internal.provisional.search.ReferenceModifiers}
	 *  
	 * @return reference kind - one of the reference modifiers
	 */
	public int getReferenceKind();
	
	/**
	 * Returns the type of reference that has been made - one of type, field, or method.
	 * 
	 * @return one of the reference type constants defined in this interface
	 */
	public int getReferenceType();
	
	/**
	 * Returns the fully qualified name of the type that has been referenced.
	 * 
	 * @return fully qualified name of the type that has been referenced
	 */
	public String getReferencedTypeName();
	
	/**
	 * Returns the name of the field or method that has been referenced, or 
	 * <code>null</code> if this is a {@link #T_TYPE_REFERENCE}.
	 * 
	 * @return the name of the field or method that has been referenced, or 
	 * <code>null</code>
	 */
	public String getReferencedMemberName();
	
	/**
	 * Returns the type signature of the method that has been referenced, or 
	 * <code>null</code> if this is not a {@link #T_METHOD_REFERENCE}.
	 * 
	 * @return the type signature of the method that has been referenced, or 
	 * <code>null</code>
	 */
	public String getReferencedSignature();	

}
