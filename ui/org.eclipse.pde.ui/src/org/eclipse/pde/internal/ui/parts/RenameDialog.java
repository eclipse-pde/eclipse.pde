/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.parts;

import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

public class RenameDialog extends Dialog {
	private String oldName;
	private String newName;
	private Text text;
	
	public RenameDialog(Shell shell, String oldName) {
		super(shell);
		setOldName(oldName);
	}
	
	public void setOldName(String oldName) {
		this.oldName = oldName;
		if (text!=null) 
			text.setText(oldName);
		this.newName = oldName;
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		
		GridData gd = new GridData(GridData.FILL_BOTH);
		container.setLayoutData(gd);
		
		Label label = new Label(container, SWT.NULL);
		label.setText(PDEPlugin.getResourceString("RenameDialog.label")); //$NON-NLS-1$
		
		text = new Text(container, SWT.SINGLE|SWT.BORDER);
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				textChanged(text.getText());
			}
		});
		gd = new GridData(GridData.FILL_HORIZONTAL);
		text.setLayoutData(gd);
		return container;
	}
	
	public int open() {
		text.setText(oldName);
		text.selectAll();
		textChanged(oldName);
		return super.open();
	}
	
	private void textChanged(String text) {
		Button okButton = getButton(IDialogConstants.OK_ID);
		okButton.setEnabled(text.equals(oldName)==false);
	}
	
	public String getNewName() {
		return newName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	protected void okPressed() {
		newName = text.getText();
		super.okPressed();
	}

}
