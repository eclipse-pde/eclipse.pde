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
 * Objects that implement this interface are carrying metadata about
 * XML schema attributes. This data is stored as schema attribute
 * annotations.
 */
public interface IMetaAttribute {
	/**
	 * Indicates that the value of the associated attribute is a regular string.
	 */
	public static final int STRING = 0;
	/**
	 * Indicates that the value of the associated attribute is a name of a fully qualified Java class.
	 */
	public static final int JAVA = 1;
	/**
	 * Indicates that the value of the associated attribute is a workspace resource.
	 */
	public static final int RESOURCE = 2;
	/*
	 * non-Javadoc
	 */
	public static final String [] kindTable = { "string", "java", "resource" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

/**
 * Returns optional name of the Java type this type must be based on (only for JAVA kind).
 */
public String getBasedOn();
/**
 * Returns the kind of this attribute's value (STRING, JAVA or RESOURCE).
 */
public int getKind();
}
