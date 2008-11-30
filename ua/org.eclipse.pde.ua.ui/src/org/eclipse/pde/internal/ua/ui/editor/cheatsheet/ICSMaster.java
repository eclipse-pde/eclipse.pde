/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet;

import org.eclipse.ui.forms.widgets.Section;

/**
 * ICSMaster
 */
public interface ICSMaster {

	/**
	 * 
	 */
	public void updateButtons();

	/**
	 * @return
	 */
	public boolean isEditable();

	/**
	 * Special case:  Need to set the selection after the full UI is created
	 * in order to properly fire an event to summon up the right details 
	 * section
	 */
	public void fireSelection();

	/**
	 * @return
	 */
	public Section getSection();

	/**
	 * @param object
	 * @return
	 */
	public boolean setFormInput(Object object);

}
