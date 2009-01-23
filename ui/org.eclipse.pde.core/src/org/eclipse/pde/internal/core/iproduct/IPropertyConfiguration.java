/*******************************************************************************
 * Copyright (c) 2009 EclipseSource Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     EclipseSource Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.iproduct;

/**
 * A simple name and value property  
 */
public interface IPropertyConfiguration extends IProductObject {
	public static final String P_NAME = "name"; //$NON-NLS-1$
	public static final String P_VALUE = "value"; //$NON-NLS-1$

	String getName();

	void setName(String name);

	String getValue();

	void setValue(String value);

}
