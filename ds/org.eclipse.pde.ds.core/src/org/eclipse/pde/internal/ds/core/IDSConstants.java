/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 223738
 *******************************************************************************/
package org.eclipse.pde.internal.ds.core;

/**
 * Interface containing constants used for the declarative services editor.
 * 
 * @since 3.4
 */
public interface IDSConstants {

	// Elements

	public static final String ELEMENT_COMPONENT = "component"; //$NON-NLS-1$

	public static final String ELEMENT_IMPLEMENTATION = "implementation"; //$NON-NLS-1$

	public static final String ELEMENT_PROPERTIES = "properties"; //$NON-NLS-1$
	
	public static final String ELEMENT_PROPERTY = "property"; //$NON-NLS-1$

	public static final String ELEMENT_SERVICE = "service"; //$NON-NLS-1$
	
	public static final String ELEMENT_PROVIDE = "provide"; //$NON-NLS-1$

	public static final String ELEMENT_REFERENCE = "reference"; //$NON-NLS-1$
	
	//Component Attributes
	public static final String ATTRIBUTE_COMPONENT_NAME = "name"; //$NON-NLS-1$
	
	public static final String ATTRIBUTE_COMPONENT_ENABLED = "enabled"; //$NON-NLS-1$
	
	public static final String ATTRIBUTE_COMPONENT_FACTORY = "factory"; //$NON-NLS-1$
	
	public static final String ATTRIBUTE_COMPONENT_IMMEDIATE = "immediate"; //$NON-NLS-1$
	
	//Implementation Attributes
	public static final String ATTRIBUTE_IMPLEMENTATION_CLASS = "class"; //$NON-NLS-1$
	
	//Property Attributes
	public static final String ATTRIBUTE_PROPERTY_NAME = "name"; //$NON-NLS-1$
	
	public static final String ATTRIBUTE_PROPERTY_VALUE = "value"; //$NON-NLS-1$
	
	public static final String ATTRIBUTE_PROPERTY_TYPE = "type"; //$NON-NLS-1$
	
	
	//Properties Attributes
	public static final String ATTRIBUTE_PROPERTIES_ENTRY = "entry"; //$NON-NLS-1$
	
	//Service Attributes
	public static final String ATTRIBUTE_SERVICE_FACTORY = "servicefactory"; //$NON-NLS-1$
	
	//Provide Attributes
	public static final String ATTRIBUTE_PROVIDE_INTERFACE = "interface"; //$NON-NLS-1$
	
	//Reference Attributes
	public static final String ATTRIBUTE_REFERENCE_NAME = "name"; //$NON-NLS-1$
	
	public static final String ATTRIBUTE_REFERENCE_INTERFACE = "interface"; //$NON-NLS-1$
	
	public static final String ATTRIBUTE_REFERENCE_CARDINALITY = "cardinality"; //$NON-NLS-1$
	
	public static final String ATTRIBUTE_REFERENCE_POLICY= "policy"; //$NON-NLS-1$
	
	public static final String ATTRIBUTE_REFERENCE_TARGET= "target"; //$NON-NLS-1$
	
	public static final String ATTRIBUTE_REFERENCE_BIND= "bind"; //$NON-NLS-1$
	
	public static final String ATTRIBUTE_REFERENCE_UNBIND= "unbind"; //$NON-NLS-1$
		
	
	//Types
	public static final int TYPE_COMPONENT = 0;

	public static final int TYPE_IMPLEMENTATION = 1;

	public static final int TYPE_PROPERTIES = 2;
	
	public static final int TYPE_PROPERTY = 3;

	public static final int TYPE_SERVICE = 4;
	
	public static final int TYPE_PROVIDE = 5;

	public static final int TYPE_REFERENCE = 6;

	public static final String VALUE_PROPERTY_TYPE_STRING = "String"; //$NON-NLS-1$
	public static final String VALUE_PROPERTY_TYPE_LONG = "Long"; //$NON-NLS-1$
	public static final String VALUE_PROPERTY_TYPE_FLOAT = "Float"; //$NON-NLS-1$
	public static final String VALUE_PROPERTY_TYPE_DOUBLE = "Double"; //$NON-NLS-1$
	public static final String VALUE_PROPERTY_TYPE_INTEGER = "Integer"; //$NON-NLS-1$
	public static final String VALUE_PROPERTY_TYPE_BYTE = "Byte"; //$NON-NLS-1$
	public static final String VALUE_PROPERTY_TYPE_CHAR = "Char"; //$NON-NLS-1$
	public static final String VALUE_PROPERTY_TYPE_BOOLEAN = "Boolean"; //$NON-NLS-1$
	public static final String VALUE_PROPERTY_TYPE_SHORT = "Short"; //$NON-NLS-1$

	public static final String VALUE_REFERENCE_POLICY_STATIC = "static"; //$NON-NLS-1$
	public static final String VALUE_REFERENCE_POLICY_DYNAMIC = "dynamic"; //$NON-NLS-1$

	public static final String VALUE_REFERENCE_CARDINALITY_ZERO_ONE = "0..1"; //$NON-NLS-1$
	public static final String VALUE_REFERENCE_CARDINALITY_ZERO_N = "0..n"; //$NON-NLS-1$
	public static final String VALUE_REFERENCE_CARDINALITY_ONE_ONE = "1..1"; //$NON-NLS-1$
	public static final String VALUE_REFERENCE_CARDINALITY_ONE_N = "1..n"; //$NON-NLS-1$
}
