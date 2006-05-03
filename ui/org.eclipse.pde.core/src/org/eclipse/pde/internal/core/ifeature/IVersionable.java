/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.ifeature;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IIdentifiable;

public interface IVersionable extends IIdentifiable {
	/**
	 * A property that will be carried by the change event if 'version' field of
	 * this object is changed.
	 */
	public static final String P_VERSION = "version"; //$NON-NLS-1$

	/**
	 * Returns a version of this object.
	 * 
	 * @return the version of this object
	 */
	public String getVersion();

	/**
	 * Sets the version of this IVersonable to the provided value. This method
	 * will throw CoreException if object is not editable.
	 * 
	 * @param version
	 *            a new version of this object
	 */
	void setVersion(String version) throws CoreException;
}
