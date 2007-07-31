/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.toc;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.PDEWizardNewFileCreationPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class TocWizardPage extends PDEWizardNewFileCreationPage {
	
	private static String EXTENSION = "xml"; //$NON-NLS-1$
	private Label fTocNameLabel;
	private Text fTocNameText;
	
	private ModifyListener tocNameListener = new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			setPageComplete(validatePage());
		}
	};
	
	public TocWizardPage(String pageName, IStructuredSelection selection) {
		super(pageName, selection);
		setTitle(PDEUIMessages.TocWizardPage_title);
		setDescription(PDEUIMessages.TocWizardPage_desc);
		// Force the file extension to be 'xml'
		setFileExtension(EXTENSION);		
	}
	
	public void createControl(Composite parent) {
		super.createControl(parent);
		
//TODO: Implement a Help context for this wizard
//		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.TARGET_DEFINITION_PAGE );
	}
	
	protected void createAdvancedControls(Composite parent) 
	{	Composite tocNameGroup = createTocNameGroup(parent);
		
		fTocNameLabel = new Label(tocNameGroup, SWT.NONE);
		fTocNameLabel.setText(PDEUIMessages.TocWizardPage_tocName);
		
		fTocNameText = new Text(tocNameGroup, SWT.BORDER | SWT.SINGLE);
		fTocNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fTocNameText.addModifyListener(tocNameListener);
	}

	private Composite createTocNameGroup(Composite parent) {
		Composite tocNameGroup = new Composite(parent, SWT.NONE);
		
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.numColumns = 2;	
		tocNameGroup.setLayout(layout);
		tocNameGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		return tocNameGroup;
	}
    
	protected boolean validatePage() {
		String tocName = getTocName();
		if(tocName == null)
		{	return false;
		}

		tocName = tocName.trim();
		// Verify the TOC name is non-empty
		if (tocName.length() == 0) {
			// Set the appropriate error message
			setErrorMessage(PDEUIMessages.TocWizardPage_emptyTocName);
			return false;
		}
		// Perform default validation
		return super.validatePage();
	}
	
	public String getTocName()
	{	if(fTocNameText != null)
		{	return fTocNameText.getText();
		}

		return null;
	}
}
