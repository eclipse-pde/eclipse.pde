/*******************************************************************************
 *  Copyright (c) 2006, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.commands;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.ViewPart;

public class CommandView extends ViewPart implements ISelectionChangedListener {

	private CommandComposerPart fCSP;

	public void createPartControl(Composite parent) {
		fCSP = new CommandComposerPart();
	}

	public FormToolkit getToolkit() {
		return fCSP.getToolkit();
	}

	public ScrolledForm getForm() {
		return null;
//		return fCSP.getForm();
	}

	public TagManager getTagManager() {
		return fCSP.getTagManager();
	}

	public void setFocus() {
		fCSP.setFocus();
	}

	public void dispose() {
		fCSP.dispose();
		super.dispose();
	}

	public void selectionChanged(SelectionChangedEvent event) {
		fCSP.selectionChanged(event);
	}

}
