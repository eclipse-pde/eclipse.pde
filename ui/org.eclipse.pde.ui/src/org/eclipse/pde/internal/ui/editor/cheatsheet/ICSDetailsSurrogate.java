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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

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
	 * @param parent
	 * @param columns
	 * @return
	 */
	public Composite createUISectionContainer(Composite parent, int columns);
	
	/**
	 * @param parent
	 * @param text
	 * @param description
	 * @param style
	 * @return
	 */
	public Section createUISection(Composite parent, String text,
			String description, int style);
	
	/**
	 * @return
	 */
	public PDEFormPage getPage();
	
}
