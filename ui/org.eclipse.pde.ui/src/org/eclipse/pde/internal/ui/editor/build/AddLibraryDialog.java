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
package org.eclipse.pde.internal.ui.editor.build;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class AddLibraryDialog extends Dialog {
	private String newName;
	private String[] libraries;
	private static String init = "library.jar";
	private Text text;

	public AddLibraryDialog(Shell shell, String[] libraries) {
		super(shell);
		setLibraryNames(libraries);
	}
	
	public void setLibraryNames(String[] libraries) {
		this.libraries = libraries;
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		
		GridData gd = new GridData(GridData.FILL_BOTH);
		container.setLayoutData(gd);
		
		Label label = new Label(container, SWT.NULL);
		label.setText(PDEPlugin.getResourceString("BuildPropertiesEditor.AddLibraryDialog.label")); //$NON-NLS-1$
		
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
		text.setText(init);
		text.selectAll();
		return super.open();
	}
	
	private void textChanged(String text) {
		Button okButton = getButton(IDialogConstants.OK_ID);
		okButton.setEnabled(!isDuplicate(text));
	}
	
	public String getText(){
		return text.getText();
	}
	public String getNewName() {
		return newName;
	}

	public Text getTextControl(){
		return text;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	protected void okPressed() {
		newName = text.getText();
		super.okPressed();
	}
	
	private boolean isDuplicate(String text){
		if(libraries==null || libraries.length==0)
			return false;
		for (int i =0;i<libraries.length; i++){
			if (libraries[i].equals(text))
				return true;
		}
		return false;
	}


}
