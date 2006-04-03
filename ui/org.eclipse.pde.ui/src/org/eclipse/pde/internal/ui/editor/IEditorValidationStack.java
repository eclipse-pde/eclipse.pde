/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;


/**
 * A stack used to keep track of validating fields in a form.
 * Validations are pushed to the top so that the most recently discovered
 * has highest priority.
 * @author janeklb
 *
 */
public interface IEditorValidationStack {
	
	/**
	 * Push a validation provider on top of the stack.
	 * @param provider
	 */
	public void push(IEditorValidator provider);
	
	/**
	 * Display the top most error on the stack, or null if the stack is empty.
	 */
	public void top();
	
	/**
	 * Returns if the validation stack is empty;
	 * @return if the stack is empty
	 */
	public boolean isEmpty();
}
