/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core;

import org.eclipse.core.runtime.CoreException;

/**
 * Classes implement this interface if
 * their instances need to be uniquely identified
 * using an id.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 */
public interface IIdentifiable {
	/**
	 * A property that will be carried by the change event
	 * if 'id' field of this object is changed.
	 */
	String P_ID = "id"; //$NON-NLS-1$

	/**
	 * Returns a unique id of this object.
	 * @return the id of this object
	 */
	String getId();

	/**
	 * Sets the id of this IIdentifiable to the provided value.
	 *
	 * @param id
	 *            a new id of this object
	 * @throws CoreException
	 *             If object is not editable.
	 */
	void setId(String id) throws CoreException;
}
