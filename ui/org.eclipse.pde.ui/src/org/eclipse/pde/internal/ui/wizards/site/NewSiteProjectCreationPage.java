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
package org.eclipse.pde.internal.ui.wizards.site;

import java.io.*;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.*;

/**
 * @author cgwong
 */
public class NewSiteProjectCreationPage extends WizardNewProjectCreationPage {
	private boolean createSite = false;
	
	public static final String HTML_CHECK_LABEL = "SiteHTML.checkLabel"; //$NON-NLS-1$
	public static final String HTML_WEB_LABEL = "SiteHTML.webLabel"; //$NON-NLS-1$
	public static final String WEB_ERR = "SiteHTML.webError"; //$NON-NLS-1$
	private static final int SIZING_TEXT_FIELD_WIDTH = 350;

	protected Text webText;
	private Button htmlButton;
	private Label webLabel;
		
	private Listener textModifyListener = new Listener(){
		public void handleEvent(Event e){
			setPageComplete(validatePage());
			setErrorMessage(getStatusString());
		}
	};
	
	/**
	 * Creates a new project creation wizard page.
	 *
	 * @param pageName the name of this page
	 */
	public NewSiteProjectCreationPage(String pageName) {
		super(pageName);
	}
	
	/** (non-Javadoc)
	 * Method declared on IDialogPage.
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		Composite control = (Composite)getControl();
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 15;
		control.setLayout(layout);
		
		Group webGroup = new Group(control, SWT.NULL);
		webGroup.setText(PDEPlugin.getResourceString("NewSiteProjectCreationPage.webTitle")); //$NON-NLS-1$
		webGroup.setFont(parent.getFont());
		
		initializeDialogUnits(parent);
		layout = new GridLayout();
		layout.numColumns = 2;
		webGroup.setLayout(layout);
		webGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		htmlButton = new Button(webGroup, SWT.CHECK | SWT.RIGHT);
		htmlButton.setText(PDEPlugin.getResourceString(HTML_CHECK_LABEL));
		GridData gd = new GridData();
		gd.horizontalSpan=2;
		htmlButton.setLayoutData(gd);
		
		webLabel = new Label(webGroup, SWT.NULL);
		webLabel.setText(PDEPlugin.getResourceString(HTML_WEB_LABEL));
		gd = new GridData(GridData.FILL_HORIZONTAL);
		webLabel.setLayoutData(gd);
		webText = new Text(webGroup, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = SIZING_TEXT_FIELD_WIDTH;
		webText.setLayoutData(gd);
		webText.setText("web"); //$NON-NLS-1$
		webText.setEnabled(createSite);
		webLabel.setEnabled(createSite);
		webText.addListener(SWT.Modify, textModifyListener);

		htmlButton.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e){
				createSite = htmlButton.getSelection();
				webLabel.setEnabled(createSite);
				webText.setEnabled(createSite);
				setPageComplete(validatePage());
				setErrorMessage(getStatusString());
			}
		});
		
		setPageComplete(validatePage());
		setControl(webGroup);
		Dialog.applyDialogFont(webGroup);
	}
	
	public String getStatusString(){
		if (createSite && getWebLocation().equals("")) //$NON-NLS-1$
			return PDEPlugin.getResourceString(WEB_ERR);
		return null;
	}
	public boolean isCreateUpdateSiteHTML(){
		return createSite;
	}
		
	public String getWebLocation(){
		String text = webText.getText();
		if (text.startsWith(File.separator) || text.startsWith("/")) //$NON-NLS-1$
			text = text.substring(1);
		if (text.endsWith(File.separator) || text.endsWith("/")) //$NON-NLS-1$
			text= text.substring(0,text.length()-1);
		return text;
	}

	protected boolean validatePage() {
		return !(createSite && getWebLocation().equals("")); //$NON-NLS-1$
	}
}
