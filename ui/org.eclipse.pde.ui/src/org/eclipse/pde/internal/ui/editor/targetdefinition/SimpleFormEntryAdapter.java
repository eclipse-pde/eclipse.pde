/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.parts.IFormEntryListener#browseButtonSelected(org.eclipse.pde.internal.ui.parts.FormEntry)
	 */
	public void browseButtonSelected(FormEntry entry) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.parts.IFormEntryListener#focusGained(org.eclipse.pde.internal.ui.parts.FormEntry)
	 */
	public void focusGained(FormEntry entry) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.parts.IFormEntryListener#selectionChanged(org.eclipse.pde.internal.ui.parts.FormEntry)
	 */
	public void selectionChanged(FormEntry entry) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.parts.IFormEntryListener#textDirty(org.eclipse.pde.internal.ui.parts.FormEntry)
	 */
	public void textDirty(FormEntry entry) {
		fFormPart.markDirty();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.parts.IFormEntryListener#textValueChanged(org.eclipse.pde.internal.ui.parts.FormEntry)
	 */
	public void textValueChanged(FormEntry entry) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.events.IHyperlinkListener#linkActivated(org.eclipse.ui.forms.events.HyperlinkEvent)
	 */
	public void linkActivated(HyperlinkEvent e) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.events.IHyperlinkListener#linkEntered(org.eclipse.ui.forms.events.HyperlinkEvent)
	 */
	public void linkEntered(HyperlinkEvent e) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.events.IHyperlinkListener#linkExited(org.eclipse.ui.forms.events.HyperlinkEvent)
	 */
	public void linkExited(HyperlinkEvent e) {
	}

}
