/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core;

import org.eclipse.core.runtime.IAdaptable;

/**
 * A base generic model. Classes that implement this
 * interface are expected to be able to:
 * <ul>
 * <li>Dispose (clear all the data and reset)</li>
 * <li>Tell if they are editable</li>
 * <li>Tell if they contain valid data</li>
 * </ul>
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 */
public interface IBaseModel extends IAdaptable {
	/**
	 * Releases all the data in this model and
	 * clears the state. A disposed model
	 * can be returned to the normal state
	 * by reloading.
	 */
	void dispose();

	/**
	 * Tests if this model has been disposed.
	 * Disposed model cannot be used until
	 * it is loaded/reloaded.
	 * @return <code>true</code> if the model has been disposed
	 */
	boolean isDisposed();

	/**
	 * Tests if this model can be modified. Modification
	 * of a model that is not editable will result
	 * in CoreException being thrown.
	 * @return <code>true</code> if this model can be modified
	 */
	boolean isEditable();

	/**
	 * Tests if this model valid. When models
	 * are loaded from the file, they may pass the
	 * syntax error checking and load all the model objects.
	 * However, some of the objects may contain invalid
	 * values that make the model unusable.
	 * @return <code>true</code> only if the model can be safely used in all
	 * computations.
	 */
	boolean isValid();
}
