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

import java.io.*;
/**
 * Models that implement this interface indicate that
 * they can be changed. When a model is changed,
 * it becomes 'dirty'. This state can either be reset
 * (in case of a 'false alarm' or naturally set to
 * false as a result of saving the changes.
 * Models that implement this interface are expected
 * to be able to save in ASCII file format
 * (e.g. XML).
 * <p>
 * <b>Note:</b> This interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 */
public interface IEditable {
	/**
	 * Tests whether the model marked as editable can be
	 * edited. Even though a model is generally editable,
	 * it can me marked as read-only because some condition
	 * prevents it from changing state (for example,
	 * the underlying resource is locked). While 
	 * read-only models can never be changed, editable
	 * models can go in and out editable state during
	 * their life cycle.
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public boolean isEditable();
	/**
	 * Tests whether the model has been changed from the last clean
	 * state.
	 * @return true if the model has been changed and need saving
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public boolean isDirty();
	/**
	 * Saves the model into the provided writer.
	 * The assumption is that the model can be
	 * persisted in an ASCII output stream (for example, an XML file).
	 * This method should clear the 'dirty' flag when
	 * done.
	 *
	 * @param writer an object that should be used to
	 * write ASCII representation of the model
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public void save(PrintWriter writer);
	/**
	 * Sets the dirty flag of the model. This method is
	 * normally not intended to be used outside the model. 
	 * Most often, a dirty model should be saved to clear the flag.
	 *
	 * @param dirty a new value for the 'dirty' flag
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	void setDirty(boolean dirty);
}
