/*******************************************************************************
 * Copyright (c) 2009, 2014 EclipseSource Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
	public static final String P_OS = "os"; //$NON-NLS-1$
	public static final String P_ARCH = "arch"; //$NON-NLS-1$

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
	 * @return The value of this property
	 */
	String getValue();

	/**
	 * Sets the value of this property
	 *
	 * @param value new value for the property
	 */
	void setValue(String value);

	/**
	 * @return The platform os for which for this property applies, or null if it
	 * applies to all platforms
	 */
	String getOs();

	/**
	 * Sets the os to which this property applies
	 *
	 * @param os the platform string describing the os to which this property
	 * applies, or null if it applies to all platforms
	 */

	void setOs(String os);

	/**
	 * @return The platform architecture for which this property applies, or null
	 * if it applies to all architectures
	 */

	String getArch();

	/**
	 * Sets the platform to which this property applies
	 *
	 * @param arch the platform string describing the arch to which this property
	 * applies, or null if it applies to all architectures
	 */
	void setArch(String arch);

}
