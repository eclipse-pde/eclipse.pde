/*******************************************************************************
 * Copyright (c) 2008 Code 9 Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 242028
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.parts;

import org.eclipse.ui.forms.events.IHyperlinkListener;

public interface IFormEntryListener extends IHyperlinkListener {
	/**
	 * The user clicked on the text control and focus was transfered to it.
	 * 
	 * @param entry
	 */
	void focusGained(FormEntry entry);

	/**
	 * The user changed the text in the text control of the entry.
	 * 
	 * @param entry
	 */
	void textDirty(FormEntry entry);

	/**
	 * The value of the entry has been changed to be the text in the text
	 * control (as a result of 'commit' action).
	 * 
	 * @param entry
	 */
	void textValueChanged(FormEntry entry);

	/**
	 * The user pressed the 'Browse' button for the entry.
	 * 
	 * @param entry
	 */
	void browseButtonSelected(FormEntry entry);

	void selectionChanged(FormEntry entry);
}
