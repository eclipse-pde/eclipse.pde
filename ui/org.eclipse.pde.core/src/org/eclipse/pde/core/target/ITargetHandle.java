/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
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
package org.eclipse.pde.core.target;

import org.eclipse.core.runtime.CoreException;

/**
 * A handle to a target definition.
 *
 * @since 3.8
 */
public interface ITargetHandle {

	/**
	 * Returns the target definition this handle references.
	 *
	 * @return target definition
	 * @throws CoreException if the underlying target definition does not exist
	 */
	public ITargetDefinition getTargetDefinition() throws CoreException;

	/**
	 * Returns a memento for this handle.
	 *
	 * @return a memento for this handle
	 * @exception CoreException if unable to generate a memento
	 */
	public String getMemento() throws CoreException;

	/**
	 * Returns whether or not the underlying target definition exists.
	 *
	 * @return whether or not the underlying target definition exists
	 */
	public boolean exists();

}
