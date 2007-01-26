/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.validation;

import org.eclipse.swt.widgets.Control;

/**
 * IControlValdiator
 *
 */
public interface IControlValidator {
	
	/**
	 * @param enabled
	 */
	public void setEnabled(boolean enabled);
	
	/**
	 * @return
	 */
	public boolean getEnabled();
	
	/**
	 * @return
	 */
	public boolean validate();
	
	/**
	 * @return
	 */
	public Control getControl();
	
	/**
	 * @return
	 */
	public boolean isValid();
	
	/**
	 * 
	 */
	public void reset();
	
}
