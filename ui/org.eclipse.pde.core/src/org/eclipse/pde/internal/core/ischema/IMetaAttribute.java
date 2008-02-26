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
 * Objects that implement this interface are carrying metadata about XML schema
 * attributes. This data is stored as schema attribute annotations.
 */
public interface IMetaAttribute {
	/**
	 * Indicates that the value of the associated attribute is a regular string.
	 */
	public static final int STRING = 0;

	/**
	 * Indicates that the value of the associated attribute is a name of a fully
	 * qualified Java class.
	 */
	public static final int JAVA = 1;

	/**
	 * Indicates that the value of the associated attribute is a workspace
	 * resource.
	 */
	public static final int RESOURCE = 2;

	/**
	 * Indicates that the value of the associated attribute is defined
	 * in another extension element attribute.
	 * 
	 * @since 3.4
	 */
	public static final int IDENTIFIER = 3;

	/**
	 * Property that indicates if an attribute is translatable
	 */
	public static final String P_TRANSLATABLE = "translatable"; //$NON-NLS-1$

	/**
	 * Property that indicates if an attribute is deprecated
	 */
	public static final String P_DEPRECATED = "deprecated"; //$NON-NLS-1$

	/**
	 * Returns optional name of the Java type this type must be based on (only
	 * for JAVA kind), or the path expression for IDENTIFIER kind.
	 */
	public String getBasedOn();

	/**
	 * Returns <samp>true</samp> if the attribute is translatable; <samp>false</samp> otherwise.
	 */
	public boolean isTranslatable();

	/**
	 * Returns <samp>true</samp> if the attribute is deprecated; <samp>false</samp> otherwise.
	 */
	public boolean isDeprecated();

	/**
	 * Returns the kind of this attribute's value (STRING, JAVA, RESOURCE or IDENTIFIER).
	 */
	public int getKind();
}
