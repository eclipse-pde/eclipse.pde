/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Created on Feb 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.internal.ui.parts.*;
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
	public void focusGained(FormEntry entry) {
		contextPart.getPage().getPDEEditor().getContributor().updateSelectableActions(new StructuredSelection());
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.newparts.IFormEntryListener#textDirty(org.eclipse.pde.internal.ui.newparts.FormEntry)
	 */
	public void textDirty(FormEntry entry) {
		contextPart.fireSaveNeeded();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.newparts.IFormEntryListener#textValueChanged(org.eclipse.pde.internal.ui.newparts.FormEntry)
	 */
	public void textValueChanged(FormEntry entry) {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.newparts.IFormEntryListener#browseButtonSelected(org.eclipse.pde.internal.ui.newparts.FormEntry)
	 */
	public void browseButtonSelected(FormEntry entry) {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.events.HyperlinkListener#linkEntered(org.eclipse.ui.forms.events.HyperlinkEvent)
	 */
	public void linkEntered(HyperlinkEvent e) {
		if (actionBars==null) return;
		IStatusLineManager mng = actionBars.getStatusLineManager();
		mng.setMessage(e.getLabel());
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.events.HyperlinkListener#linkExited(org.eclipse.ui.forms.events.HyperlinkEvent)
	 */
	public void linkExited(HyperlinkEvent e) {
		if (actionBars==null) return;
		IStatusLineManager mng = actionBars.getStatusLineManager();
		mng.setMessage(null);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.events.HyperlinkListener#linkActivated(org.eclipse.ui.forms.events.HyperlinkEvent)
	 */
	public void linkActivated(HyperlinkEvent e) {
	}
}