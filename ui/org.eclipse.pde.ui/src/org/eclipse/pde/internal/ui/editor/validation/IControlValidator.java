/*******************************************************************************
 *  Copyright (c) 2006, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.validation;

import org.eclipse.swt.widgets.Control;

public interface IControlValidator {

	/**
	 * Enable / disable the validator.
	 * @param enabled
	 */
	public void setEnabled(boolean enabled);

	/**
	 * Determine whether the validator is enabled / disabled 
	 */
	public boolean getEnabled();

	/**
	 * Validate the control (manual validation).
	 */
	public boolean validate();

	/**
	 * Get the control that this validator validates.
	 */
	public Control getControl();

	/**
	 * Determine whether the control contents are valid.  No validation is
	 * done.  Validity is determined by the last time the control was validated
	 */
	public boolean isValid();

	/**
	 * Reset the validator.  Clear error messages and reset state.
	 */
	public void reset();

	/**
	 * Controls whether the message handler automatically updates messages in 
	 * the form. Setting the refresh to true, triggers an immediate update 
	 * @param refresh
	 */
	public void setRefresh(boolean refresh);

}
