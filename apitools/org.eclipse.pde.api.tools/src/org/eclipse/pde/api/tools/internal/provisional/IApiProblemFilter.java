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
package org.eclipse.pde.api.tools.internal.provisional;

import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;

/**
 * Describes API problems that should be filtered. Allows filters to be set at any level
 * in the element hierarchy - for example, filter all problems from a component or filter
 * specific problems from a member.
 * <p>
 * Problem filters are created from an {@link IApiComponent}.
 * </p>
 * @since 1.0.0
 */
public interface IApiProblemFilter {

	/**
	 * Constant representing the filter kind for adding a class bound.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#ANNOTATION_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#CLASS_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#CONSTRUCTOR_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#INTERFACE_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#METHOD_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>ADDED_CLASS_BOUND</code> 
	 * 
	 * @see #getKinds()
	 */
	public static final String ADDED_CLASS_BOUND = "ADDED_CLASS_BOUND"; //$NON-NLS-1$
	
	
	/**
	 * Constant representing the filter kind for changing the bound class.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#ANNOTATION_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#CLASS_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#CONSTRUCTOR_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#INTERFACE_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#METHOD_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>CHANGED_CLASS_BOUND></code>
	 * 
	 * @see #getKinds()
	 */
	public static final String CHANGED_CLASS_BOUND = "CHANGED_CLASS_BOUND"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for removing a class bound.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#ANNOTATION_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#CLASS_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#CONSTRUCTOR_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#INTERFACE_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#METHOD_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>REMOVED_CLASS_BOUND</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String REMOVED_CLASS_BOUND = "REMOVED_CLASS_BOUND"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for adding an interface bound.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#ANNOTATION_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#CLASS_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#CONSTRUCTOR_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#INTERFACE_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#METHOD_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>ADDED_INTERFACE_BOUND</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String ADDED_INTERFACE_BOUND = "ADDED_INTERFACE_BOUND"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for changing an interface bound.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#ANNOTATION_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#CLASS_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#CONSTRUCTOR_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#INTERFACE_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#METHOD_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>CHANGED_INTERFACE_BOUND</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String CHANGED_INTERFACE_BOUND = "CHANGED_INTERFACE_BOUND"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for removing an interface bound.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#ANNOTATION_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#CLASS_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#CONSTRUCTOR_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#INTERFACE_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#METHOD_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>REMOVED_INTERFACE_BOUND</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String REMOVED_INTERFACE_BOUND = "REMOVED_INTERFACE_BOUND"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for adding interface bounds.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#ANNOTATION_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#CLASS_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#CONSTRUCTOR_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#INTERFACE_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#METHOD_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>ADDED_INTERFACE_BOUNDS</code>
	 *  
	 *  @see #getKinds()
	 */
	public static final String ADDED_INTERFACE_BOUNDS = "ADDED_INTERFACE_BOUNDS"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for removing interface bounds.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#ANNOTATION_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#CLASS_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#CONSTRUCTOR_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#INTERFACE_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#METHOD_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>REMOVED_INTERFACE_BOUNDS</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String REMOVED_INTERFACE_BOUNDS = "REMOVED_INTERFACE_BOUNDS"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for changing the super-interfaces set.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#ANNOTATION_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#CLASS_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#ENUM_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#INTERFACE_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>CHANGED_CONTRACTED_SUPERINTERFACES_SET</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String CHANGED_CONTRACTED_SUPERINTERFACES_SET = "CHANGED_CONTRACTED_SUPERINTERFACES_SET"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for changing an execution environment into an API component.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#API_COMPONENT_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>CHANGED_EXECUTION_ENVIRONMENT</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String CHANGED_EXECUTION_ENVIRONMENT = "CHANGED_EXECUTION_ENVIRONMENT"; //$NON-NLS-1$
	/**
	 * Constant representing the filter kind for changing into a class.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#ANNOTATION_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#ENUM_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#INTERFACE_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>CHANGED_TO_CLASS</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String CHANGED_TO_CLASS = "CHANGED_TO_CLASS"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for changing into an enum.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#ANNOTATION_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#CLASS_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#ENUM_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#INTERFACE_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>CHANGED_TO_ENUM</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String CHANGED_TO_ENUM = "CHANGED_TO_ENUM"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for changing into an interface.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#ANNOTATION_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#CLASS_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#ENUM_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>CHANGED_TO_INTERFACE</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String CHANGED_TO_INTERFACE = "CHANGED_TO_INTERFACE"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for changing into an annotation.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#CLASS_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#ENUM_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#INTERFACE_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>CHANGED_TO_ANNOTATION</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String CHANGED_TO_ANNOTATION = "CHANGED_TO_ANNOTATION"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for decreasing access to an element.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#CLASS_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#CONSTRUCTOR_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#ENUM_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#FIELD_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#INTERFACE_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#METHOD_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>CHANGED_DECREASE_ACCESS</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String CHANGED_DECREASE_ACCESS = "CHANGED_DECREASE_ACCESS"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for making a concrete element abstract.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#CLASS_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#CONSTRUCTOR_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#ENUM_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#METHOD_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>CHANGED_NON_ABSTRACT_TO_ABSTRACT</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String CHANGED_NON_ABSTRACT_TO_ABSTRACT = "CHANGED_NON_ABSTRACT_TO_ABSTRACT"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for making an element final.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#CLASS_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#FIELD_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#METHOD_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>CHANGED_NON_FINAL_TO_FINAL</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String CHANGED_NON_FINAL_TO_FINAL = "CHANGED_NON_FINAL_TO_FINAL"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for changing an element to be static.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#CONSTRUCTOR_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#FIELD_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#METHOD_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>CHANGED_NON_STATIC_TO_STATIC</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String CHANGED_NON_STATIC_TO_STATIC = "CHANGED_NON_STATIC_TO_STATIC"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for changing a constructor to be non-static.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#CONSTRUCTOR_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#FIELD_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#METHOD_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>CHANGED_STATIC_TO_NON_STATIC</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String CHANGED_STATIC_TO_NON_STATIC = "CHANGED_STATIC_TO_NON_STATIC"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for changing a final field to be static
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#FIELD_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>CHANGED_FINAL_TO_NON_FINAL_STATIC_CONSTANT</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String CHANGED_FINAL_TO_NON_FINAL_STATIC_CONSTANT = "CHANGED_FINAL_TO_NON_FINAL_STATIC_CONSTANT"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for changing the superclass of a class.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#CLASS_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>CHANGED_SUPERCLASS</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String CHANGED_SUPERCLASS = "CHANGED_SUPERCLASS"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for changing the superclass set of a class.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#CLASS_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>CHANGED_CONTRACTED_SUPERCLASS_SET</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String CHANGED_CONTRACTED_SUPERCLASS_SET = "CHANGED_CONTRACTED_SUPERCLASS_SET"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for changing varargs to an array for a constructor.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#CONSTRUCTOR_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#METHOD_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>CHANGED_VARARGS_TO_ARRAY</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String CHANGED_VARARGS_TO_ARRAY = "CHANGED_VARARGS_TO_ARRAY"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for changing the type of a field.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#FIELD_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>CHANGED_TYPE</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String CHANGED_TYPE = "CHANGED_TYPE"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for changing the value of a field.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#FIELD_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>CHANGED_VALUE</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String CHANGED_VALUE = "CHANGED_VALUE"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for adding a type parameter.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#ANNOTATION_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#CLASS_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#CONSTRUCTOR_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#INTERFACE_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#METHOD_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>ADDED_TYPE_PARAMETER</code> 
	 * 
	 * @see #getKinds()
	 */
	public static final String ADDED_TYPE_PARAMETER = "ADDED_TYPE_PARAMETER"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for adding an execution environment into an API component.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#API_COMPONENT_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>ADDED_EXECUTION_ENVIRONMENT</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String ADDED_EXECUTION_ENVIRONMENT = "ADDED_EXECUTION_ENVIRONMENT"; //$NON-NLS-1$
	/**
	 * Constant representing the filter kind for adding a value to a field.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#FIELD_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>ADDED_VALUE</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String ADDED_VALUE = "ADDED_VALUE"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for removing a type parameter.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#ANNOTATION_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#CLASS_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#CONSTRUCTOR_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#INTERFACE_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#METHOD_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>REMOVED_TYPE_PARAMETER</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String REMOVED_TYPE_PARAMETER = "REMOVED_TYPE_PARAMETER"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for removing type parameters from an annotation.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#ANNOTATION_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#CLASS_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#INTERFACE_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#METHOD_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>REMOVED_TYPE_PARAMETERS</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String REMOVED_TYPE_PARAMETERS = "REMOVED_TYPE_PARAMETERS"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for adding a method with no default value to an annotation.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#ANNOTATION_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>ADDED_METHOD_WITHOUT_DEFAULT_VALUE</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String ADDED_METHOD_WITHOUT_DEFAULT_VALUE = "ADDED_METHOD_WITHOUT_DEFAULT_VALUE"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for adding a '@noimplement' tag.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#ANNOTATION_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#INTERFACE_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>ADDED_NOT_IMPLEMENT_RESTRICTION</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String ADDED_NOT_IMPLEMENT_RESTRICTION = "ADDED_NOT_IMPLEMENT_RESTRICTION"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for adding an '@noextend' tag.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#CLASS_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#CONSTRUCTOR_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#ENUM_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#METHOD_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>ADDED_NO_EXTEND</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String ADDED_NO_EXTEND = "ADDED_NO_EXTEND"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for removing a field.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#ANNOTATION_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#CLASS_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#ENUM_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#INTERFACE_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>REMOVED_FIELD</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String REMOVED_FIELD = "REMOVED_FIELD"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for removing a method.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#CLASS_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#ENUM_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#INTERFACE_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>REMOVED_METHOD</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String REMOVED_METHOD = "REMOVED_METHOD"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for removing a constructor from a class.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#CLASS_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#ENUM_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>REMOVED_CONSTRUCTOR</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String REMOVED_CONSTRUCTOR = "REMOVED_CONSTRUCTOR"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for removing a type member.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#ANNOTATION_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#CLASS_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#ENUM_ELEMENT_TYPE}</li>
	 * <li>{@link IDelta#INTERFACE_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>REMOVED_TYPE_MEMBER</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String REMOVED_TYPE_MEMBER = "REMOVED_TYPE_MEMBER"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for removing a constant from an enum.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#ENUM_ELEMENT_TYPE}</li>
	 * </ul>
	 * Values is: <code>REMOVED_ENUM_CONSTANT</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String REMOVED_ENUM_CONSTANT = "REMOVED_ENUM_CONSTANT"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for removing the default annotation of a method.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#METHOD_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>REMOVED_ANNOTATION_DEFAULT_VALUE</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String REMOVED_ANNOTATION_DEFAULT_VALUE = "REMOVED_ANNOTATION_DEFAULT_VALUE"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for removing a method with a default value from an annotation.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#ANNOTATION_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>REMOVED_METHOD_WITH_DEFAULT_VALUE</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String REMOVED_METHOD_WITH_DEFAULT_VALUE = "REMOVED_METHOD_WITH_DEFAULT_VALUE"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for removing a method without a default value from an annotation.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#ANNOTATION_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>REMOVED_METHOD_WITHOUT_DEFAULT_VALUE</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String REMOVED_METHOD_WITHOUT_DEFAULT_VALUE = "REMOVED_METHOD_WITHOUT_DEFAULT_VALUE"; //$NON-NLS-1$	
	
	/**
	 * Constant representing the filter kind for a type being removed from an API component.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#API_COMPONENT_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>REMOVED_TYPE</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String REMOVED_TYPE = "REMOVED_TYPE"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for a duplicated type being removed from an API component.
	 * A duplicated type occurs when a type is defined in a host component and one of its fragment. 
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#API_COMPONENT_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>REMOVED_DUPLICATED_TYPE</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String REMOVED_DUPLICATED_TYPE = "REMOVED_DUPLICATED_TYPE"; //$NON-NLS-1$

	/**
	 * Constant representing the filter kind for an API component being removed from an API profile.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#API_PROFILE_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>REMOVED_API_COMPONENT</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String REMOVED_API_COMPONENT = "REMOVED_API_COMPONENT"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for removing the value of a field.
	 * This kind applies to element types:
	 * <ul>
	 * <li>Field</li>
	 * </ul>
	 * Value is: <code>REMOVED_VALUE</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String REMOVED_VALUE = "REMOVED_VALUE"; //$NON-NLS-1$
	
	/**
	 * Constant representing the filter kind for removing an execution environment into an API component.
	 * This kind applies to element types:
	 * <ul>
	 * <li>{@link IDelta#API_COMPONENT_ELEMENT_TYPE}</li>
	 * </ul>
	 * Value is: <code>REMOVED_EXECUTION_ENVIRONMENT</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String REMOVED_EXECUTION_ENVIRONMENT = "REMOVED_EXECUTION_ENVIRONMENT"; //$NON-NLS-1$
	/**
	 * Constant representing the filter kind for removing type arguments from a field.
	 * This kind applies to element types:
	 * <ul>
	 * <li>Field</li>
	 * </ul>
	 * Value is: <code>REMOVED_TYPE_ARGUMENTS</code>
	 * 
	 * @see #getKinds()
	 */
	public static final String REMOVED_TYPE_ARGUMENTS = "REMOVED_TYPE_ARGUMENTS"; //$NON-NLS-1$

	/**
	 * Returns the identifier of the API component this filter applies to. Problems
	 * contained within this component are potentially filtered.
	 * 
	 * @return identifier of the API component this filter applies to
	 */
	public String getComponentId();
	
	/**
	 * Returns the element this filter applies to. Problems contained within this element
	 * are potentially filtered.
	 * 
	 * @return element this filter applies to or <code>null</code>
	 */
	public IElementDescriptor getElement();
	
	/**
	 * Returns the kinds of problems to filter or <code>null</code>. 
	 * Corresponds to problem severity types (each kind of user configurable problem).
	 * 
	 * @return the specific kinds of problems to filter or <code>null</code>
	 */
	public String[] getKinds();
	
	/**
	 * Removes the specified filter kind from this filter and returns the success of the removal
	 * 
	 * @param kind the kind to remove
	 * @return true if the kind was removed, false otherwise
	 */
	public boolean removeKind(String kind);
	
	/**
	 * Adds the specified kind to this filter. Has no effect if the kind already exists in this filter.
	 * 
	 * @param kind the new kind to add to this filter
	 */
	public void addKind(String kind);
}
