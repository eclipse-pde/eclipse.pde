/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet;

import org.eclipse.ui.forms.widgets.Section;

/**
 * ICSMaster
 */
public interface ICSMaster {

	public void updateButtons();

	public boolean isEditable();

	/**
	 * Special case:  Need to set the selection after the full UI is created
	 * in order to properly fire an event to summon up the right details
	 * section
	 */
	public void fireSelection();

	public Section getSection();

	public boolean setFormInput(Object object);

}
