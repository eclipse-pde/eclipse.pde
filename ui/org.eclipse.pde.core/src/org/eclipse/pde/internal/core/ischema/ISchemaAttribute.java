/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
	public static final String[] USE_TABLE = {"optional", "required", "default"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 

	public static final int BOOL_IND = 0;
	public static final int STR_IND = 1;
	public static final int JAVA_IND = 2;
	public static final int RES_IND = 3;
	public static final int ID_IND = 4;
	public static final String[] TYPES = {"boolean", //$NON-NLS-1$
			"string", //$NON-NLS-1$
			"java", //$NON-NLS-1$
			"resource", //$NON-NLS-1$
			"identifier" //$NON-NLS-1$
	};

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

}
