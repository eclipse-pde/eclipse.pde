/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core;

import org.eclipse.core.runtime.*;
/**
 * Classes implement this interface if
 * their instances need to be uniquely identified
 * using an id.
 * 
 * @since 2.0
 */
public interface IIdentifiable {
	/**
	 * A property that will be carried by the change event
	 * if 'id' field of this object is changed.
	 */
	public static final String P_ID = "id"; //$NON-NLS-1$
	/**
	 * Returns a unique id of this object.
	 * @return the id of this object
	 */
	public String getId();
	/**
	 * Sets the id of this IIdentifiable to the provided value.
	 * This method will throw CoreException if
	 * object is not editable.
	 *
	 *@param id a new id of this object
	 */
	void setId(String id) throws CoreException;
}
