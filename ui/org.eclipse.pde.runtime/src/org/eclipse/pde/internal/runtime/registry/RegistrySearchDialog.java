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
package org.eclipse.pde.internal.runtime.registry;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.pde.internal.runtime.*;
import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;


public class RegistrySearchDialog extends Dialog{
	public static String ENTER_ID = "RegistrySearchDialog.enterId";
	public static String ENTER_NAME = "RegistrySearchDialog.enterName";
	private boolean isId;
	private Text text;
	private String oldText = null;
	
	public RegistrySearchDialog(Shell parentShell, boolean isId){
		super(parentShell);
		this.isId = isId;
	}
	protected Control createDialogArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginWidth = 10;
		layout.marginHeight = 10;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Label label = new Label(container, SWT.NULL);
		label.setText(isId ? PDERuntimePlugin.getResourceString(ENTER_ID) : PDERuntimePlugin.getResourceString(ENTER_NAME)); //$NON-NLS-1$
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		text = new Text(container, SWT.SINGLE|SWT.BORDER);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		return container;
	}
	
	public int open() {
		text.setText(oldText!=null? oldText : "");
		return super.open();
	}

	protected void okPressed() {
		oldText = text.getText();
		super.okPressed();
	}
	
	public String getSearchText(){
		return oldText;
	}
	
}
