/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.ischema;

/**
 * Objects that implement this interface hold data about attributes of schema
 * elements.
 */
public interface ISchemaAttribute extends ISchemaObject, IMetaAttribute {
	/**
	 * This attribute can be omitted by the extension element.
	 */
	public static final int OPTIONAL = 0;

	/**
	 * This attribute must be defined in the extension element.
	 */
	public static final int REQUIRED = 1;

	/**
	 * This attribute can be omitted by the extension element, and if it is, its
	 * value will be set to the value defined in the "value" field.
	 */
	public static final int DEFAULT = 2;

	/**
	 * Table of the 'use' clause choices.
	 */
	public static final String[] useTable = {
			"optional", "required", "default" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 

	/**
	 * Returns the type of this attribute. Attributes can only have simple
	 * types.
	 */
	public ISchemaSimpleType getType();

	/**
	 * Returns the 'use' mode of this attribute (OPTIONAL, REQUIRED or DEFAULT).
	 */
	public int getUse();

	/**
	 * Returns the default value of this attribute when 'use' clause is DEFAULT.
	 */
	public Object getValue();
	
	/**
	 * Returns <samp>true</samp> if the attribute is translatable; <samp>false</samp> otherwise.
	 */
	public boolean isTranslatable();
}
