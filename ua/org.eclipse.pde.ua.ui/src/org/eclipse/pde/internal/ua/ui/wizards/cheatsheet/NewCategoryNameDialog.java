/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.wizards.cheatsheet;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

/**
 * NewCategoryNameDialog
 */
public class NewCategoryNameDialog extends TrayDialog {

	private Text fNameText;

	private String fNameTextValue;

	/**
	 * @param shell
	 */
	public NewCategoryNameDialog(Shell shell) {
		super(shell);

		fNameText = null;
		fNameTextValue = null;
	}

	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IHelpContextIds.NEW_CS_CATEGORY_NAME_DIALOG);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {

		Composite composite = createUI(parent);
		createListeners();
		updateUI();

		return composite;
	}

	/**
	 * @param parent
	 */
	private Composite createUI(Composite parent) {
		// Create the container
		Composite container = createUIContainer(parent);
		// Create the instructional label
		createUIInstructionLabel(container);
		// Create the name field
		createUINameField(container);
		// Apply the default font to the dialog
		applyDialogFont(container);

		return container;
	}

	/**
	 * 
	 */
	private void createListeners() {
		// NO-OP	
	}

	/**
	 * 
	 */
	private void updateUI() {
		// NO-OP
	}

	/**
	 * @param parent
	 * @return
	 */
	private Composite createUIContainer(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		return composite;
	}

	/**
	 * @param container
	 */
	private void createUIInstructionLabel(Composite container) {
		Label label = new Label(container, SWT.WRAP);
		label.setText(CSWizardMessages.NewCategoryNameDialog_labelDesc);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		data.widthHint = 200;
		label.setLayoutData(data);
	}

	/**
	 * @param parent
	 */
	private void createUINameField(Composite parent) {
		// Create the label
		createUINameLabel(parent);
		// Create the text widget
		createUINameText(parent);
	}

	/**
	 * @param parent
	 */
	private void createUINameLabel(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(CSWizardMessages.NewCategoryNameDialog_labelText);
	}

	/**
	 * @param parent
	 */
	private void createUINameText(Composite parent) {
		int style = SWT.BORDER;
		fNameText = new Text(parent, style);
		fNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	protected void okPressed() {
		// This is needed because the widget is disposed before after okay is
		// pressed before the value can be retrieved
		fNameTextValue = fNameText.getText();
		super.okPressed();
	}

	/**
	 * @return
	 */
	public String getNameText() {
		return fNameTextValue;
	}

}
