/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
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
 * <p>
 * <b>Note:</b> This interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 */
public interface IBaseModel extends IAdaptable {
	/**
	 * Releases all the data in this model and
	 * clears the state. A disposed model
	 * can be returned to the normal state
	 * by reloading.
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	void dispose();
	/**
	 * Tests if this model has been disposed.
	 * Disposed model cannot be used until
	 * it is loaded/reloaded.
	 * @return true if the model has been disposed
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	boolean isDisposed();
	/**
	 * Tests if this model can be modified. Modification
	 * of a model that is not editable will result
	 * in CoreException being thrown.
	 * @return true if this model can be modified
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	boolean isEditable();
	/**
	 * Tests if this model valid. When models
	 * are loaded from the file, they may pass the
	 * syntax error checking and load all the model objects.
	 * However, some of the objects may contain invalid
	 * values that make the model unusable.
	 * This method should return <samp>true</samp> only
	 * if the model can be safely used in all
	 * computations.
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	boolean isValid();	
}
