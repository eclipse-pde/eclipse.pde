/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
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
	public int ADDED = 1;
	/**
	 * Status constant indicating that the element has been changed, as
	 * described by the change flags.
	 * 
	 * @see #getFlags()
	 */
	public int CHANGED = 2;

	/**
	 * Status constant indicating that the element has been removed.
	 */
	public int REMOVED = 3;

	/**
	 * Element type constant indicating that the delta is reported against an annotation type declaration.
	 * 
	 * @see #getElementType()
	 */
	public static final int ANNOTATION_ELEMENT_TYPE = 1;

	/**
	 * Element type constant indicating that the delta is reported against an API component.
	 * 
	 * @see #getElementType()
	 */
	public static final int API_COMPONENT_ELEMENT_TYPE = 2;

	/**
	 * Element type constant indicating that the delta is reported against an API baseline.
	 * 
	 * @see #getElementType()
	 */
	public static final int API_BASELINE_ELEMENT_TYPE = 3;

	/**
	 * Element type constant indicating that the delta is reported against a class type declaration.
	 * 
	 * @see #getElementType()
	 */
	public static final int CLASS_ELEMENT_TYPE = 4;

	/**
	 * Element type constant indicating that the delta is reported against a constructor declaration.
	 * 
	 * @see #getElementType()
	 */
	public static final int CONSTRUCTOR_ELEMENT_TYPE = 5;

	/**
	 * Element type constant indicating that the delta is reported against an enum type declaration.
	 * 
	 * @see #getElementType()
	 */
	public static final int ENUM_ELEMENT_TYPE = 6;


	/**
	 * Element type constant indicating that the delta is reported against a field declaration.
	 * 
	 * @see #getElementType()
	 */
	public static final int FIELD_ELEMENT_TYPE = 7;

	/**
	 * Element type constant indicating that the delta is reported against an interface type declaration.
	 * 
	 * @see #getElementType()
	 */
	public static final int INTERFACE_ELEMENT_TYPE = 8;

	/**
	 * Element type constant indicating that the delta is reported against a method declaration.
	 * 
	 * @see #getElementType()
	 */
	public static final int METHOD_ELEMENT_TYPE = 9;
	/**
	 * Element type constant indicating that the delta is reported against a type parameter.
	 * 
	 * @see #getElementType()
	 */
	public static final int TYPE_PARAMETER_ELEMENT_TYPE = 10;
	
	/**
	 * Delta kind flag that denotes removing the abstract keyword from a member.
	 *  <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #CHANGED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int ABSTRACT_TO_NON_ABSTRACT = 1;
	/**
	 * Delta kind flag that denotes the default value of an annotation.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #ADDED}</li>
	 * <li>{@link #CHANGED}</li>
	 * <li>{@link #REMOVED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int ANNOTATION_DEFAULT_VALUE = 2;
	/**
	 * Delta kind flag that denotes an {@link IApiComponent}.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #ADDED}</li>
	 * <li>{@link #REMOVED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int API_COMPONENT = 3;
	/**
	 * Delta kind flag that denotes changing an array of objects to a Java 1.5 varargs.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #CHANGED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int ARRAY_TO_VARARGS = 4;
	/**
	 * Delta kind flag that denotes a checked exception.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #ADDED}</li>
	 * <li>{@link #REMOVED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int CHECKED_EXCEPTION = 5;
	/**
	 * Delta kind flag that denotes a Java 1.5 generics class bound.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #ADDED}</li>
	 * <li>{@link #CHANGED}</li>
	 * <li>{@link #REMOVED}</li>
	 * </ul> 
	 * 
	 * @see #getFlags()
	 */
	public static final int CLASS_BOUND = 6;
	/**
	 * Delta kind flag that denotes a static initializer.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #ADDED}</li>
	 * <li>{@link #CHANGED}</li>
	 * <li>{@link #REMOVED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int CLINIT = 7;
	/**
	 * Delta kind flag that denotes a constructor.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #ADDED}</li>
	 * <li>{@link #REMOVED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int CONSTRUCTOR = 8;
	/**
	 * Delta kind flag that denotes an interface in the super-interface set has been removed.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #CHANGED}</li>
	 * </ul> 
	 * 
	 * @see #getFlags()
	 */
	public static final int CONTRACTED_SUPERINTERFACES_SET = 10;
	/**
	 * Delta kind flag that denotes decreasing the access of a member.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #CHANGED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int DECREASE_ACCESS = 11;
	/**
	 * Delta kind flag that denotes a constant value enum.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #ADDED}</li>
	 * <li>{@link #REMOVED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int ENUM_CONSTANT = 12;
	/**
	 * Delta kind flag that denotes and execution environment.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #ADDED}</li>
	 * <li>{@link #REMOVED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int EXECUTION_ENVIRONMENT = 13;
	/**
	 * Delta kind flag that denotes an interface has been added to the current set of super-interfaces.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #CHANGED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int EXPANDED_SUPERINTERFACES_SET = 15;
	/**
	 * Delta kind flag that denotes a field has been added or removed.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #ADDED}</li>
	 * <li>{@link #REMOVED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int FIELD = 16;
	/**
	 * Delta kind flag that denotes a field has been moved up the current super-class hierarchy.
	 *  <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #REMOVED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int FIELD_MOVED_UP = 17;
	/**
	 * Delta kind flag that denotes the final keyword has been removed from a member.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #CHANGED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int FINAL_TO_NON_FINAL = 18;
	/**
	 * Delta kind flag that denotes that the final keyword has been removed from a static member.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #CHANGED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int FINAL_TO_NON_FINAL_NON_STATIC = 19;
	/**
	 * Delta kind flag that denotes the final keyword has been removed from a constant field.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #CHANGED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int FINAL_TO_NON_FINAL_STATIC_CONSTANT = 20;
	/**
	 * Delta kind flag that denotes the final keyword has been removed from a non-constant field.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #CHANGED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int FINAL_TO_NON_FINAL_STATIC_NON_CONSTANT = 21;
	/**
	 * Delta kind flag that denotes the access to a member has been increased.
	 *  <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #CHANGED}</li>
	 * </ul>
	 * @see #getFlags()
	 */
	public static final int INCREASE_ACCESS = 22;
	/**
	 * Delta kind flag that denotes a Java 1.5 interface bound has been changed.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #ADDED}</li>
	 * <li>{@link #CHANGED}</li>
	 * <li>{@link #REMOVED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int INTERFACE_BOUND = 23;
	/**
	 * Delta kind flag that denotes a method has been added or removed.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #ADDED}</li>
	 * <li>{@link #REMOVED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int METHOD = 25;
	/**
	 * Delta kind flag that denotes a method has moved up the super-class hierarchy.
	 *  <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #REMOVED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int METHOD_MOVED_UP = 26;
	/**
	 * Delta kind flag that denotes a method with a default value has been added or removed.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #ADDED}</li>
	 * <li>{@link #REMOVED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int METHOD_WITH_DEFAULT_VALUE = 27;
	/**
	 * Delta kind flag that denotes a method without a default value has been added or removed.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #ADDED}</li>
	 * <li>{@link #REMOVED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int METHOD_WITHOUT_DEFAULT_VALUE = 28;
	/**
	 * Delta kind flag that denotes the native keyword has been removed.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #CHANGED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int NATIVE_TO_NON_NATIVE = 29;
	/**
	 * Delta kind flag that denotes the abstract keyword has been added.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #CHANGED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int NON_ABSTRACT_TO_ABSTRACT = 30;
	/**
	 * Delta kind flag that denotes the final keyword has been added.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #CHANGED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int NON_FINAL_TO_FINAL = 31;
	/**
	 * Delta kind flag that denotes the native keyword has been added.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #CHANGED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int NON_NATIVE_TO_NATIVE = 32;
	/**
	 * Delta kind flag that denotes the static keyword has been added.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #CHANGED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int NON_STATIC_TO_STATIC = 33;
	/**
	 * Delta kind flag that denotes the synchronized keyword has been added.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #CHANGED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int NON_SYNCHRONIZED_TO_SYNCHRONIZED = 34;
	/**
	 * Delta kind flag that denotes the transient keyword has been added.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #CHANGED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int NON_TRANSIENT_TO_TRANSIENT = 35;
	/**
	 * Delta kind flag that denotes a method addition that is overriding a method from a superclass
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #ADDED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int OVERRIDEN_METHOD = 36;
	/**
	 * Delta kind flag that denotes API restrictions on a member have been added.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #ADDED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int RESTRICTIONS = 37;
	/**
	 * Delta kind flag that denotes the static keyword has been removed.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #CHANGED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int STATIC_TO_NON_STATIC = 38;
	/**
	 * Delta kind flag that denotes a super-class has been added, or removed.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #ADDED}</li>
	 * <li>{@link #REMOVED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int SUPERCLASS = 39;
	/**
	 * Delta kind flag that denotes the synchronized keyword has been removed.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #CHANGED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int SYNCHRONIZED_TO_NON_SYNCHRONIZED = 40;
	/**
	 * Delta kind flag that denotes a type has been converted to a different kind. For example, from a class
	 * to an annotation.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #CHANGED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int TYPE_CONVERSION = 41;
	/**
	 * Delta kind flag that denotes the transient keyword has been removed.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #CHANGED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int TRANSIENT_TO_NON_TRANSIENT = 45;
	/**
	 * Delta kind flag that denotes a type has changed in some way.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #ADDED}</li>
	 * <li>{@link #CHANGED}</li>
	 * <li>{@link #REMOVED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int TYPE = 46;
	/**
	 * Delta kind flag that denotes type arguments have been added or removed.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #ADDED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int TYPE_ARGUMENTS = 47;
	/**
	 * Delta kind flag that denotes a type member has been added or removed from a type.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #ADDED}</li>
	 * <li>{@link #REMOVED}</li>
	 * </ul>
	 * @see #getFlags()
	 */
	public static final int TYPE_MEMBER = 48;
	/**
	 * Delta kind flag that denotes a type parameter has been added or removed.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #ADDED}</li>
	 * <li>{@link #REMOVED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int TYPE_PARAMETER = 49;
	/**
	 * Delta kind flag that a type parameter name has changed.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #CHANGED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int TYPE_PARAMETER_NAME = 50;
	/**
	 * Delta kind flag that denotes parameters have been added or removed from a type.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #ADDED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int TYPE_PARAMETERS = 51;
	/**
	 * Delta kind flag that denotes the visibility of a type has changed.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #CHANGED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int TYPE_VISIBILITY = 52;
	/**
	 * Delta kind flag that denotes an unchecked exception has been added or removed.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #ADDED}</li>
	 * <li>{@link #REMOVED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int UNCHECKED_EXCEPTION = 53;
	/**
	 * Delta kind flag that denotes the value of a member has changed in some way.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #ADDED}</li>
	 * <li>{@link #CHANGED}</li>
	 * <li>{@link #REMOVED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int VALUE = 54;
	/**
	 * Delta kind flag that denotes changing a Java 1.5 varargs to an array of {@link Object}s.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #CHANGED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int VARARGS_TO_ARRAY = 55;
	/**
	 * Delta kind flag that denotes changing the visibility of a type from VisibilityModifiers.API to another visibility.
	 * As a consequence, the corresponding type is no longer an API type.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #REMOVED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int API_TYPE = 56;
	/**
	 * Delta kind flag that denotes the volatile keyword has been added.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #CHANGED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int NON_VOLATILE_TO_VOLATILE = 57;
	/**
	 * Delta kind flag that denotes the volatile keyword has been removed.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #CHANGED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int VOLATILE_TO_NON_VOLATILE = 58;
	/**
	 * Delta kind flag that denotes changing the major version of a bundle.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #CHANGED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int MAJOR_VERSION = 59;
	/**
	 * Delta kind flag that denotes changing the minor version of a bundle.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #CHANGED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int MINOR_VERSION = 60;
	/**
	 * Delta kind flag that denotes adding @noreference restrictions to an API field.
	 * As a consequence, the corresponding field is no longer an API field.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #REMOVED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int API_FIELD = 61;
	/**
	 * Delta kind flag that denotes adding @noreference restrictions to an API method.
	 * As a consequence, the corresponding method is no longer an API method.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #REMOVED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int API_METHOD = 62;
	/**
	 * Delta kind flag that denotes adding @noreference restrictions to an API constructor.
	 * As a consequence, the corresponding constructor is no longer an API constructor.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #REMOVED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int API_CONSTRUCTOR = 63;
	/**
	 * Delta kind flag that denotes adding @noreference restrictions to an API enum constant.
	 * As a consequence, the corresponding enum constant is no longer an API enum constant.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #REMOVED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int API_ENUM_CONSTANT = 64;
	/**
	 * Delta kind flag that denotes adding @noreference restrictions to an API enum constant.
	 * As a consequence, the corresponding enum constant is no longer an API enum constant.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #REMOVED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int API_METHOD_WITH_DEFAULT_VALUE = 65;
	/**
	 * Delta kind flag that denotes adding @noreference restrictions to an API enum constant.
	 * As a consequence, the corresponding enum constant is no longer an API enum constant.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #REMOVED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int API_METHOD_WITHOUT_DEFAULT_VALUE = 66;
	/**
	 * Delta kind flag that denotes a method has moved down in the type hierarchy.
	 *  <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #ADDED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int METHOD_MOVED_DOWN = 67;
	/**
	 * Delta kind flag that denotes that a type argument has been added or removed.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #ADDED}</li>
	 * <li>{@link #CHANGED}</li>
	 * <li>{@link #REMOVED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int TYPE_ARGUMENT = 68;
	/**
	 * Delta kind flag that denotes that an interface got a super interface with methods.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #ADDED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int SUPER_INTERFACE_WITH_METHODS = 69;
	/**
	 * Delta kind flag that denotes a re-exported type has been added or removed.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #ADDED}</li>
	 * <li>{@link #REMOVED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int REEXPORTED_TYPE = 70;
	/**
	 * Delta kind flag that denotes changing the visibility of a re-exported type from VisibilityModifiers.API to another visibility.
	 * As a consequence, the corresponding re-exported type is no longer an API type.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #REMOVED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int REEXPORTED_API_TYPE = 71;
	/**
	 * Delta kind flag that denotes adding or removing the deprecated modifiers on the corresponding element.
	 * <br>
	 * Applies to kinds:
	 * <ul>
	 * <li>{@link #ADDED}</li>
	 * <li>{@link #REMOVED}</li>
	 * </ul>
	 * 
	 * @see #getFlags()
	 */
	public static final int DEPRECATION = 72;
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
	 * Returns the delta's description. This can be used as an error message. The message is returned
	 * in the current locale.
	 * 
	 * @return the delta's description
	 */
	public String getMessage();
	
	/**
	 * Returns the set of arguments that can be used to compose NLS'd messages. These arguments will typically
	 * be type, method and field names.
	 * 
	 * @return the set of arguments to compose NLS'd messages
	 */
	public String[] getArguments();
	
	/**
	 * Returns the delta's current restrictions.
	 * 
	 * @return the delta's current restrictions
	 */
	public int getCurrentRestrictions();

	/**
	 * Returns the delta's previous restrictions.
	 * 
	 * @return the delta's previous restrictions
	 */
	public int getPreviousRestrictions();

	/**
	 * Returns the delta's new modifiers. This corresponds to the new modifiers of the affected element.
	 * by the delta.
	 * 
	 * @return the delta's new modifiers
	 */
	public int getNewModifiers();

	/**
	 * Returns the delta's old modifiers. This corresponds to the old modifiers of the affected element.
	 * by the delta.
	 * 
	 * @return the delta's old modifiers
	 */
	public int getOldModifiers();

	/**
	 * Returns the component identifier including its version identifier in which the given delta is
	 * reported, or <code>null</code>. Can be <code>null</code> if the delta is reported against an
	 * API profile.
	 * 
	 * @return the component id in which the given delta is reported, or <code>null</code> if none
	 */
	public String getComponentVersionId();
	

	/**
	 * Returns the component identifier without its version identifier in which the given delta is
	 * reported, or <code>null</code>. Can be <code>null</code> if the delta is reported against an
	 * API profile.
	 * 
	 * @return the component id in which the given delta is reported, or <code>null</code> if none
	 */
	public String getComponentId();
}
