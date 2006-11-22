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

package org.eclipse.pde.internal.ui.editor.cheatsheet;

import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * ICSDetailsSurrogate
 *
 */
public interface ICSDetailsSurrogate {

	/**
	 * @return
	 */
	public boolean isEditableElement();
	
	/**
	 * @return
	 */
	public FormToolkit getToolkit();
	
	/**
	 * @return
	 */
	public ICSMaster getMasterSection();

	/**
	 * @return
	 */
	public PDEFormPage getPage();
	
}
