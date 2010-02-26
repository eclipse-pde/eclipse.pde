/*******************************************************************************
 * Copyright (c) 2008, 2010 Code 9 Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.parts.PluginVersionPart;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.*;

public class VersionDialog extends StatusDialog {

	private String fVersion;
	private boolean fEditable;
	private PluginVersionPart fVersionPart;

	public VersionDialog(Shell parent, boolean editable, String version) {
		super(parent);
		fEditable = editable;
		fVersionPart = new PluginVersionPart(false) {
			protected String getGroupText() {
				return PDEUIMessages.VersionDialog_text;
			}
		};
		fVersionPart.setVersion(version);
		setTitle(PDEUIMessages.VersionDialog_title);
	}

	protected Control createDialogArea(Composite parent) {
		Composite comp = (Composite) super.createDialogArea(parent);

		fVersionPart.createVersionFields(comp, true, fEditable);
		ModifyListener ml = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateStatus(fVersionPart.validateFullVersionRangeText(true));
			}
		};
		fVersionPart.addListeners(ml, ml);

		return comp;
	}

	public String getVersion() {
		return fVersion;
	}

	protected void okPressed() {
		fVersion = fVersionPart.getVersion();
		super.okPressed();
	}

}
