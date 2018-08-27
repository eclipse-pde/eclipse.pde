/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.editor.targetdefinition;

import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.parts.IFormEntryListener;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.events.HyperlinkEvent;

/**
 * Simple listener implementation for FormEntry parts that simply marks
 * the form as being dirty when the entry text is modified.
 * @see FormEntry
 */
public class SimpleFormEntryAdapter implements IFormEntryListener {

	private AbstractFormPart fFormPart;

	public SimpleFormEntryAdapter(AbstractFormPart formPart) {
		fFormPart = formPart;
	}

	@Override
	public void browseButtonSelected(FormEntry entry) {
	}

	@Override
	public void focusGained(FormEntry entry) {
	}

	@Override
	public void selectionChanged(FormEntry entry) {
	}

	@Override
	public void textDirty(FormEntry entry) {
		fFormPart.markDirty();
	}

	@Override
	public void textValueChanged(FormEntry entry) {
	}

	@Override
	public void linkActivated(HyperlinkEvent e) {
	}

	@Override
	public void linkEntered(HyperlinkEvent e) {
	}

	@Override
	public void linkExited(HyperlinkEvent e) {
	}

}
