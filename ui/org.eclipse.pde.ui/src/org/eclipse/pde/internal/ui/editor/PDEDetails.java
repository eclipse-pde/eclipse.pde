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
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.widgets.FormToolkit;

public abstract class PDEDetails extends AbstractFormPart implements IDetailsPage, IContextPart {

	public PDEDetails() {
	}
	
	public boolean canPaste(Clipboard clipboard) {
		return true;
	}
	
	public boolean doGlobalAction(String actionId) {
		return false;
	}
	
	protected void markDetailsPart(Control control) {
		control.setData("part", this); //$NON-NLS-1$
	}
	
	protected void createSpacer(FormToolkit toolkit, Composite parent, int span) {
		Label spacer = toolkit.createLabel(parent, ""); //$NON-NLS-1$
		GridData gd = new GridData();
		gd.horizontalSpan = span;
		spacer.setLayoutData(gd);
	}
	public void cancelEdit() {
		super.refresh();
	}
}