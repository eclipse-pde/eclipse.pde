/*******************************************************************************
 *  Copyright (c) 2003, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.parts.IFormEntryListener;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.forms.events.HyperlinkEvent;

public class FormEntryAdapter implements IFormEntryListener {
	private IContextPart contextPart;
	protected IActionBars actionBars;

	public FormEntryAdapter(IContextPart contextPart) {
		this(contextPart, null);
	}

	public FormEntryAdapter(IContextPart contextPart, IActionBars actionBars) {
		this.contextPart = contextPart;
		this.actionBars = actionBars;
	}

	@Override
	public void focusGained(FormEntry entry) {
		ITextSelection selection = new TextSelection(1, 1);
		contextPart.getPage().getPDEEditor().getContributor().updateSelectableActions(selection);
	}

	@Override
	public void textDirty(FormEntry entry) {
		contextPart.fireSaveNeeded();
	}

	@Override
	public void textValueChanged(FormEntry entry) {
	}

	@Override
	public void browseButtonSelected(FormEntry entry) {
	}

	@Override
	public void linkEntered(HyperlinkEvent e) {
		if (actionBars == null)
			return;
		IStatusLineManager mng = actionBars.getStatusLineManager();
		mng.setMessage(e.getLabel());
	}

	@Override
	public void linkExited(HyperlinkEvent e) {
		if (actionBars == null)
			return;
		IStatusLineManager mng = actionBars.getStatusLineManager();
		mng.setMessage(null);
	}

	@Override
	public void linkActivated(HyperlinkEvent e) {
	}

	@Override
	public void selectionChanged(FormEntry entry) {
		ITextSelection selection = new TextSelection(1, 1);
		contextPart.getPage().getPDEEditor().getContributor().updateSelectableActions(selection);
	}
}
