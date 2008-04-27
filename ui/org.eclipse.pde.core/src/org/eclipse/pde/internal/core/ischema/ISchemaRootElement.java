/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.ischema;

public interface ISchemaRootElement extends ISchemaElement {

	/**
	 * Property that indicates if an element has a replacement
	 */
	public static final String P_DEP_REPLACEMENT = "replacement"; //$NON-NLS-1$

	/**
	 * Property that indicates if an element is internal
	 */
	public static final String P_INTERNAL = "internal"; //$NON-NLS-1$

	/**
	 * Property that indicates if an element has friends
	 */
	public static final String P_FRIENDS = "friends"; //$NON-NLS-1$

	public void setDeprecatedSuggestion(String value);

	/**
	 * Returns a value if the element has a replacement suggestion for its deprecated self.
	 */
	public String getDeprecatedSuggestion();

	/**
	 * Returns <samp>true</samp> if the element is internal; <samp>false</samp> otherwise.
	 */
	public boolean isInternal();

	/**
	 * 
	 * @param value
	 */
	public void setInternal(boolean value);

}
