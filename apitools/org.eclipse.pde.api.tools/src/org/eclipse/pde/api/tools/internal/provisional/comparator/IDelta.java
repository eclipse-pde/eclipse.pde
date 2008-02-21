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
package org.eclipse.pde.api.tools.internal.provisional.comparator;

/**
 * Interface that represents a delta.
 * This interface is not intended to be implemented or extended by the client.
 */
public interface IDelta {
	/**
	 * Status constant indicating that the element has been added.
	 */
	public int ADDED = 0x0001;

	/**
	 * Status constant indicating that the element has been added with extend restriction.
	 * extend = false
	 */
	public int ADDED_EXTEND_RESTRICTION = 0x0002;

	/**
	 * Status constant indicating that the element has been added without implement restriction.
	 */
	public int ADDED_IMPLEMENT_RESTRICTION = 0x0004;

	/**
	 * Status constant indicating that the element has been added, but it is not visible.
	 */
	public int ADDED_NON_VISIBLE = 0x0008;

	/**
	 * Status constant indicating that the element has been added without extend restriction.
	 */
	public int ADDED_NOT_EXTEND_RESTRICTION = 0x0010;

	/**
	 * Status constant indicating that the element has been added with extend restriction for a static member.
	 */
	public int ADDED_NOT_EXTEND_RESTRICTION_STATIC = 0x0020;

	/**
	 * Status constant indicating that the element has been added with implement restriction.
	 */
	public int ADDED_NOT_IMPLEMENT_RESTRICTION = 0x0040;

	/**
	 * Status constant indicating that the element has been changed, as
	 * described by the change flags.
	 * 
	 * @see #getFlags()
	 */
	public int CHANGED = 0x0080;
	

	/**
	 * Status constant indicating that the element has been changed, as
	 * described by the change flags without extend restriction.
	 * 
	 * @see #getFlags()
	 */
	public int CHANGED_NOT_EXTEND_RESTRICTION = 0x0100;

	/**
	 * Status constant indicating that the element has been changed but
	 * it is not visible (package default or private), as
	 * described by the change flags.
	 * 
	 * @see #getFlags()
	 */
	public int CHANGED_NON_VISIBLE = 0x0200;

	/**
	 * Status constant indicating that the element has changed`visibility, but
	 * not in a breaking way.
	 * 
	 * @see #getFlags()
	 */
	public int CHANGED_VISIBILITY = 0x0400;

	/**
	 * Status constant indicating that the element has been removed.
	 */
	public int REMOVED = 0x0800;

	/**
	 * Status constant indicating that the element has been removed from a member with
	 * extend restriction.
	 */
	public int REMOVED_EXTEND_RESTRICTION = 0x1000;

	/**
	 * Status constant indicating that the element has been removed, but it is
	 * not visible.
	 */
	public int REMOVED_NON_VISIBLE = 0x2000;

	/**
	 * Element type constant indicating that the delta is reported against an annotation type declaration.
	 * 
	 * @see #getElementType()
	 */
	public static final int ANNOTATION_ELEMENT_TYPE = 0x001;

	/**
	 * Element type constant indicating that the delta is reported against an API component.
	 * 
	 * @see #getElementType()
	 */
	public static final int API_COMPONENT_ELEMENT_TYPE = 0x002;

	/**
	 * Element type constant indicating that the delta is reported against an API profile.
	 * 
	 * @see #getElementType()
	 */
	public static final int API_PROFILE_ELEMENT_TYPE = 0x004;

	/**
	 * Element type constant indicating that the delta is reported against a class type declaration.
	 * 
	 * @see #getElementType()
	 */
	public static final int CLASS_ELEMENT_TYPE = 0x008;

	/**
	 * Element type constant indicating that the delta is reported against a constructor declaration.
	 * 
	 * @see #getElementType()
	 */
	public static final int CONSTRUCTOR_ELEMENT_TYPE = 0x010;

	/**
	 * Element type constant indicating that the delta is reported against an enum type declaration.
	 * 
	 * @see #getElementType()
	 */
	public static final int ENUM_ELEMENT_TYPE = 0x020;


	/**
	 * Element type constant indicating that the delta is reported against a field declaration.
	 * 
	 * @see #getElementType()
	 */
	public static final int FIELD_ELEMENT_TYPE = 0x040;

	/**
	 * Element type constant indicating that the delta is reported against an interface type declaration.
	 * 
	 * @see #getElementType()
	 */
	public static final int INTERFACE_ELEMENT_TYPE = 0x080;

	/**
	 * Element type constant indicating that the delta is reported against a member type.
	 * 
	 * @see #getElementType()
	 */
	public static final int MEMBER_ELEMENT_TYPE = 0x100;

	/**
	 * Element type constant indicating that the delta is reported against a method declaration.
	 * 
	 * @see #getElementType()
	 */
	public static final int METHOD_ELEMENT_TYPE = 0x200;


	public static final int ABSTRACT_TO_NON_ABSTRACT = 1;
	public static final int ANNOTATION_DEFAULT_VALUE = 2;
	public static final int API_COMPONENT = 3;
	public static final int ARRAY_TO_VARARGS = 4;
	public static final int CHECKED_EXCEPTION = 5;
	public static final int CLASS_BOUND = 6;
	public static final int CLINIT = 7;
	public static final int CONSTRUCTOR = 8;
	public static final int CONTRACTED_SUPERCLASS_SET = 9;
	public static final int CONTRACTED_SUPERINTERFACES_SET = 10;
	public static final int DECREASE_ACCESS = 11;
	public static final int DUPLICATED_TYPE = 12;
	public static final int ENUM_CONSTANT = 13;
	public static final int EXECUTION_ENVIRONMENT = 14;
	public static final int EXPANDED_SUPERCLASS_SET = 15;
	public static final int EXPANDED_SUPERINTERFACES_SET = 16;
	public static final int FIELD = 17;
	public static final int FIELD_MOVED_UP = 18;
	public static final int FINAL_TO_NON_FINAL = 19;
	public static final int FINAL_TO_NON_FINAL_NON_STATIC = 20;
	public static final int FINAL_TO_NON_FINAL_STATIC_CONSTANT = 21;
	public static final int FINAL_TO_NON_FINAL_STATIC_NON_CONSTANT = 22;
	public static final int INCREASE_ACCESS = 23;
	public static final int INTERFACE_BOUND = 24;
	public static final int INTERFACE_BOUNDS = 25;
	public static final int METHOD = 26;
	public static final int METHOD_MOVED_UP = 27;
	public static final int METHOD_WITH_DEFAULT_VALUE = 28;
	public static final int METHOD_WITHOUT_DEFAULT_VALUE = 29;
	public static final int NATIVE_TO_NON_NATIVE = 30;
	public static final int NON_ABSTRACT_TO_ABSTRACT = 31;
	public static final int NON_FINAL_TO_FINAL = 32;
	public static final int NON_NATIVE_TO_NATIVE = 33;
	public static final int NON_STATIC_TO_STATIC = 34;
	public static final int NON_SYNCHRONIZED_TO_SYNCHRONIZED = 35;
	public static final int NON_TRANSIENT_TO_TRANSIENT = 36;
	public static final int RESTRICTIONS = 37;
	public static final int STATIC_TO_NON_STATIC = 38;
	public static final int SUPERCLASS = 39;
	public static final int SYNCHRONIZED_TO_NON_SYNCHRONIZED = 40;
	public static final int TO_ANNOTATION = 41;
	public static final int TO_CLASS = 42;
	public static final int TO_ENUM = 43;
	public static final int TO_INTERFACE = 44;
	public static final int TRANSIENT_TO_NON_TRANSIENT = 45;
	public static final int TYPE = 46;
	public static final int TYPE_ARGUMENTS = 47;
	public static final int TYPE_MEMBER = 48;
	public static final int TYPE_PARAMETER = 49;
	public static final int TYPE_PARAMETER_NAME = 50;
	public static final int TYPE_PARAMETERS = 51;
	public static final int UNCHECKED_EXCEPTION = 52;
	public static final int VALUE = 53;
	public static final int VARARGS_TO_ARRAY = 54;

	/**
	 * Return true if the receiver has no children deltas, false otherwise.
	 * 
	 * @return true if the receiver has no children deltas, false otherwise.
	 */
	public boolean isEmpty();

	/**
	 * Returns the key of this delta.
	 * 
	 * @return the key of this delta
	 */
	public String getKey();
	
	/**
	 * Returns the kind of this delta that describe how an element has changed.
	 * 
	 * @return the kind of this delta that describe how an element has changed
	 */
	public int getKind();

	/**
	 * Returns flags that describe how an element has changed.
	 * 
	 * @return flags that describe how an element has changed
	 */
	public int getFlags();

	/**
	 * Returns the type of the element on which a delta occurred. Any of
	 * {@link IDelta#ANNOTATION_ELEMENT_TYPE}, {@link IDelta#ENUM_ELEMENT_TYPE},
	 * {@link IDelta#CONSTRUCTOR_ELEMENT_TYPE}, {@link IDelta#METHOD_ELEMENT_TYPE},
	 * {@link IDelta#INTERFACE_ELEMENT_TYPE}, {@link IDelta#CLASS_ELEMENT_TYPE},
	 * {@link IDelta#FIELD_ELEMENT_TYPE}, {@link IDelta#API_COMPONENT_ELEMENT_TYPE}
	 * and {@link IDelta#API_PROFILE_ELEMENT_TYPE}. 
	 * 
	 * @return flags that describe how an element has changed
	 */
	public int getElementType();

	/**
	 * Returns the children of the receiver. Return an empty list if none
	 * 
	 * @return children of the receiver
	 */
	public IDelta[] getChildren();
	
	/**
	 * Traverse the given delta and apply the visitor
	 * @param visitor the given delta visitor
	 */
	public void accept(DeltaVisitor visitor);
	
	/**
	 * Returns the type name against which the delta is returned.
	 * 
	 * @return the type name against which the delta is returned.
	 */
	public String getTypeName();

	/**
	 * Returns the delta description. This can be used as an error message. The message is returned
	 * in the current locale.
	 * 
	 * @return the delta description
	 */
	public String getMessage();
}
