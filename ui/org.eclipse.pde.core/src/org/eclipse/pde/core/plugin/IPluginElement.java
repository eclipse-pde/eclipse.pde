/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.plugin;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.core.ischema.*;
/**
 * Classes that implement this interface model the
 * XML elements found in the plug-in model.
 */
public interface IPluginElement extends IPluginParent {
	/**
	 * A property name that will be used to notify
	 * about element body text change.
	 */
	public static final String P_TEXT = "text";
	/**
	 * A property name that will be used to notify
	 * about global replacement of the element's attributes.
	 */
	public static final String P_ATTRIBUTES = "attributes";

	/**
	 * A property name that will be used to notify individual
	 * change in an element's attribute.
	 */
	public static final String P_ATTRIBUTE = "attribute";
	/**
	 * Creates an identical copy of this XML element.
	 * The new element will share the same model and
	 * the parent.
	 *
	 * @return a copy of this element
	 */
	public IPluginElement createCopy();
	/**
	 * Returns an attribute object whose name
	 * matches the provided name.
	 * @param name the name of the attribute
	 * @return the attribute object, or <samp>null</samp> if not found
	 */
	IPluginAttribute getAttribute(String name);
	/**
	 * Returns all attributes currently defined in this element
	 * @return an array of attribute objects that belong to this element
	 */
	IPluginAttribute[] getAttributes();
	/**
	 * Returns the number of attributes in this element.
	 * @return number of attributes defined in this element
	 */
	int getAttributeCount();
	/**
	 * Returns the body text of this element.
	 *
	 * @return body text of this element or <samp>null</samp> if not set.
	 */
	String getText();
	/**
	 * Returns the schema for this element.
	 *
	 * @return the schema for this element or <samp>null</samp> if not found.
	 */
	ISchemaElement getElementInfo();
	/**
	 * Sets the attribute with the provided name
	 * to the provided value. If attribute object
	 * is not found, a new one will be created and
	 * its value set to the provided value.
	 * This method will throw a CoreException if
	 * the model is not editable.
	 *
	 * @param name the name of the attribute
	 * @param value the value to be set 
	 */
	void setAttribute(String name, String value) throws CoreException;
	/**
	 * Sets the body text of this element
	 * to the provided value. This method
	 * will throw a CoreException if the
	 * model is not editable.
	 *
	 * @param text the new body text of this element
	 */
	void setText(String text) throws CoreException;
}
