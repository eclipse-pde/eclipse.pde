/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.provisional.builder;

import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;

/**
 * Describes a reference to a type, field, or method.
 * 
 * @since 1.1
 */
public interface IReference {
	
	/**
	 * An {@link IReference} of this type represents a type reference.
	 * 
	 * @see #getReferenceType()
	 */
	public static final int T_TYPE_REFERENCE = 1;
	/**
	 * An {@link IReference} of this type represents a field reference.
	 * 
	 * @see #getReferenceType()
	 */	
	public static final int T_FIELD_REFERENCE = 2;
	/**
	 * An {@link IReference} of this type represents a method reference.
	 * 
	 * @see #getReferenceType()
	 */
	public static final int T_METHOD_REFERENCE = 3;
	
	/**
	 * Flag to indicate the reference is illegal, i.e. a reference
	 * made to an API restricted {@link org.eclipse.pde.api.tools.internal.provisional.model.IApiElement}
	 * @since 1.1
	 */
	public static final int F_ILLEGAL = 1;
	
	/**
	 * Constant representing the superclass of the class is a class in the target space.
	 * 
	 * Value is <code>2^0</code>
	 * 
	 * @see #getReferenceKind()
	 */
	public static final int REF_EXTENDS = 0x1;
	/**
	 * Constant representing the class implements an interface in the target space.
	 * 
	 * Value is <code>2^1</code>
	 * 
	 * @see #getReferenceKind()
	 */
	public static final int REF_IMPLEMENTS = 0x1 << 1;
	/**
	 * Constant representing a field is declared of a type in the target space.
	 *  
	 * Value is <code>2^2</code>
	 * 
	 * @see #getReferenceKind()
	 */
	public static final int REF_FIELDDECL = 0x1 << 2;
	/**
	 * Constant representing the return type of a declared method is in the target space.
	 * 
	 * Value is <code>2^3</code>
	 * 
	 * @see #getReferenceKind()
	 */
	public static final int REF_RETURNTYPE = 0x1 << 3;
	/**
	 * Constant representing a parameter of a method is in the target space.
	 * 
	 * Value is <code>2^4</code>
	 * 
	 * @see #getReferenceKind()
	 */
	public static final int REF_PARAMETER = 0x1 << 4;
	/**
	 * Constant representing an exception in the throws clause of a method is in the target space.
	 * 
	 * Value is <code>2^5</code>
	 * 
	 * @see #getReferenceKind()
	 */
	public static final int REF_THROWS = 0x1 << 5;
	/**
	 * Constant representing a constructor method was invoked on a class in the target space.
	 * 
	 * Value is <code>2^6</code>
	 * 
	 * @see #getReferenceKind()
	 */
	public static final int REF_CONSTRUCTORMETHOD = 0x1 << 6;
	/**
	 * Constant representing a static method is invoked on a class in the target space.
	 * 
	 * Value is <code>2^7</code>
	 * 
	 * @see #getReferenceKind()
	 */
	public static final int REF_STATICMETHOD = 0x1 << 7;
	/**
	 * Constant representing a virtual (instance) method is invoked on a class in the target space.
	 * 
	 * Value is <code>2^8</code>
	 * 
	 * @see #getReferenceKind()
	 */
	public static final int REF_VIRTUALMETHOD = 0x1 << 8;
	/**
	 * Constant representing a method on an interface in the target space is invoked.
	 * 
	 * Value is <code>2^9</code>
	 * 
	 * @see #getReferenceKind()
	 */
	public static final int REF_INTERFACEMETHOD = 0x1 << 9;
	/**
	 * Constant representing a parameter passed to a method is in the target space.
	 * 
	 * Value is <code>2^10</code>
	 * 
	 * @see #getReferenceKind()
	 */
	public static final int REF_PASSEDPARAMETER = 0x1 << 10;
	/**
	 * Constant representing reading a static field from a class in the target space.
	 * 
	 * Value is <code>2^11</code>
	 * 
	 * @see #getReferenceKind()
	 */
	public static final int REF_GETSTATIC = 0x1 << 11;
	/**
	 * Constant representing setting a static field from a class in the target space.
	 * 
	 * Value is <code>2^12</code>
	 * 
	 * @see #getReferenceKind()
	 */
	public static final int REF_PUTSTATIC = 0x1 << 12;
	/**
	 * Constant representing reading an instance field from a class in the target space.
	 * 
	 * Value is <code>2^13</code>
	 * 
	 * @see #getReferenceKind()
	 */
	public static final int REF_GETFIELD = 0x1 << 13;
	/**
	 * Constant representing setting an instance field from a class in the target space.
	 * 
	 * Value is <code>2^14</code>
	 * 
	 * @see #getReferenceKind()
	 */
	public static final int REF_PUTFIELD = 0x1 << 14;
	/**
	 * Constant representing an array is created of a type in the target space.
	 * 
	 * Value is <code>2^15</code>
	 * 
	 * @see #getReferenceKind()
	 */
	public static final int REF_ARRAYALLOC = 0x1 << 15;
	/**
	 * Constant representing a method contains a local variable that is in the target space.
	 * 
	 * Value is <code>2^16</code>
	 * 
	 * @see #getReferenceKind()
	 */
	public static final int REF_LOCALVARIABLE = 0x1 << 16;
	/**
	 * Constant representing code in a method catches an exception of a class in the target space.
	 * 
	 * Value is <code>2^17</code>
	 * 
	 * @see #getReferenceKind()
	 */
	public static final int REF_CATCHEXCEPTION = 0x1 << 17;
	/**
	 * Constant representing method code contains a cast using a type in the target space.
	 * 
	 * Value is <code>2^18</code>
	 * 
	 * @see #getReferenceKind()
	 */
	public static final int REF_CHECKCAST = 0x1 << 18;
	/**
	 * Constant representing method code contains a type in the target space as the operand of the
	 * instanceof operator.
	 * 
	 * Value is <code>2^19</code>
	 * 
	 * @see #getReferenceKind()
	 */
	public static final int REF_INSTANCEOF = 0x1 << 19;
	/**
	 * Constant representing that the INVOKE_SPECIAL instruction has been used in the 
	 * target space.
	 * 
	 * Value is <code>2^20</code>
	 * 
	 * @see #getReferenceKind()
	 */
	public static final int REF_SPECIALMETHOD = 0x1 << 20;
	/**
	 * Constant representing a parameterized type has been declared in the target space. Used or Java 5+ support
	 * of signatures.
	 * 
	 * Value is <code>2^21</code>
	 * 
	 * @see #getReferenceKind()
	 */
	public static final int REF_PARAMETERIZED_TYPEDECL = 0x1 << 21;
	/**
	 * Constant representing a parameterized field has been declared in the target space. Used for Java 5+ support
	 * of signatures.
	 * 
	 * Value is <code>2^22</code>
	 * 
	 * @see #getReferenceKind()
	 */
	public static final int REF_PARAMETERIZED_FIELDDECL = 0x1 << 22;
	/**
	 * Constant representing a parameterized method has been declared in the target space. Used for Java 5+ support
	 * of signatures. 
	 * 
	 * Value is <code>2^23</code>
	 * 
	 * @see #getReferenceKind()
	 */
	public static final int REF_PARAMETERIZED_METHODDECL = 0x1 << 23;
	/**
	 * Constant representing a parameterized type for a local variable is in the target space. Used
	 * for Java 5+ support of signatures.
	 * 
	 * Value is <code>2^24</code>
	 * 
	 * @see #getReferenceKind()
	 */
	public static final int REF_PARAMETERIZED_VARIABLE = 0x1 << 24;
	/**
	 * Constant representing a local variable is declared in the target space.
	 * 
	 * Value is <code>2^25</code>
	 * 
	 * @see #getReferenceKind()
	 */
	public static final int REF_LOCALVARIABLEDECL = 0x1 << 25;
	/**
	 * Constant representing a type is read from the constant pool and placed on the stack
	 * 
	 * Value is <code>2^26</code>
	 * 
	 * @see #getReferenceKind()
	 */
	public static final int REF_CONSTANTPOOL = 0x1 << 26;
	/**
	 * Constant representing a type has been instantiated via a constructor. This reference
	 * is to the type, rather than the actual constructor method.
	 * 
	 * Value is <code>2^27</code>
	 * 
	 * @see #getReferenceKind()
	 */
	public static final int REF_INSTANTIATE = 0x1 << 27;
	/**
	 * Constant representing a method overriding a method in a super type.
	 * 
	 * Value is <code>2^28</code>
	 * 
	 * @see #getReferenceKind()
	 */
	public static final int REF_OVERRIDE = 0x1 << 28;
	/**
	 * Constant representing a constructor method was invoked on a direct superclass from 
	 * a constructor.
	 * 
	 * Value is <code>2^29</code>
	 * 
	 * @see #getReferenceKind()
	 */
	public static final int REF_SUPER_CONSTRUCTORMETHOD = 0x1 << 29;
	/**
	 * Bit mask used to indicate all kinds of references.
	 * 
	 * @see #getReferenceKind()
	 */
	public static final int MASK_REF_ALL = 0x7FFFFFFF;
	
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
	 * 
	 * @return reference kind - one of the reference modifiers
	 */
	public int getReferenceKind();
	
	/**
	 * Returns any flags set on the reference
	 * 
	 * @return any flags set on the reference
	 */
	public int getReferenceFlags();
	
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
	 * Returns the type signature of the method or type that has been referenced, or 
	 * <code>null</code> if this is not a {@link #T_METHOD_REFERENCE} or if not
	 * available for an {@link #T_TYPE_REFERENCE}.
	 * 
	 * @return the type signature of the method or type that has been referenced, or 
	 * <code>null</code>
	 */
	public String getReferencedSignature();	
	
	/**
	 * Returns the resolved member that is referenced or <code>null</code> if
	 * this reference is unresolved.
	 * 
	 * @return resolved reference or <code>null</code>
	 */
	public IApiMember getResolvedReference();
}
