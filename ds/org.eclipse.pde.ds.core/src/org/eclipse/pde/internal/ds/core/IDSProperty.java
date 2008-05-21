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
 * Represents a single property file
 * 
 * @since 3.4
 * @see IDSObject
 */
public interface IDSProperty extends IDSObject {

	
	/**
	 * Returns the name of the property
	 * 
	 * @return String containing the attribute name
	 */
	public String getPropertyName();

	/**
	 * Sets the name of the property
	 * 
	 * @param name
	 *            New name
	 */
	public void setPropertyName(String name);

	/**
	 * Returns the value of the property
	 * 
	 * @return String containing the value of the property.
	 */
	public String getPropertyValue();

	/**
	 * Sets the value of the property.
	 * 
	 * @param value
	 *            New value
	 */
	public void setPropertyValue(String value);

	/**
	 * Sets the type of the property.
	 * 
	 * The type defines how to interpret the value. The type must be one of the
	 * following Java types: String (default), Long, Double, Float, Integer,
	 * Byte, Character, Boolean, Short.
	 * 
	 * @param type
	 *            New type
	 */
	public void setPropertyType(String type);

	/**
	 * Returns the type of the property.
	 * 
	 * @return String representing one of the following Java types: String
	 *         (default), Long, Double, Float, Integer, Byte, Character,
	 *         Boolean, Short.
	 */
	
	public String getPropertyType();

	/**
	 * Sets the attribute body.
	 * 
	 * If the value attribute is not specified, the body of the property element
	 * must contain one or more values.
	 * 
	 * The value of the property is then an array of the specified type. Except
	 * for String objects, the result will be translated to an array of
	 * primitive types. For example, if the type attribute specifies Integer,
	 * then the resulting array must be int[].
	 * 
	 * For example, a component that needs an array of hosts can use the
	 * following property definition:
	 * <code> <property name="hosts"> www.acme.com
	 * backup.acme.com </property> <\code>
	 * 
	 * @param body
	 *            New body
	 */
	public void setPropertyElemBody(String body);


	/**
	 * Return all elements in body attribute
	 * 
	 * @return String containing the text of all elements in body attribute
	 */
	public String getPropertyElemBody();
}
