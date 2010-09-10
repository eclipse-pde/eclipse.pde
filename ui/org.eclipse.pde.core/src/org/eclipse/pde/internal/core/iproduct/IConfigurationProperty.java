/*******************************************************************************
 * Copyright (c) 2009, 2010 EclipseSource Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     EclipseSource Corporation - initial API and implementation
 *     IBM Corporation - continuing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.core.iproduct;

/**
 * Properties that are added to the config.ini.  Properties consist of a string name and
 * string value.
 * 
 * @see IProduct
 * 
 * @since 3.7
 */
public interface IConfigurationProperty extends IProductObject {
	public static final String P_NAME = "name"; //$NON-NLS-1$
	public static final String P_VALUE = "value"; //$NON-NLS-1$

	/**
	 * @return The name of this property
	 */
	String getName();

	/**
	 * Sets the name of this property
	 * 
	 * @param name new name for the property
	 */
	void setName(String name);

	/**
	 * @return The current value for this property
	 */
	String getValue();

	/**
	 * Sets the value of this property
	 * 
	 * @param value new value for the property
	 */
	void setValue(String value);

}
