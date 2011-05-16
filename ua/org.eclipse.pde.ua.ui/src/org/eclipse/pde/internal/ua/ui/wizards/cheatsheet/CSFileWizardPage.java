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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.wizards.PDEWizardNewFileCreationPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;

/**
 * CheatSheetFileWizardPage
 *
 */
public class CSFileWizardPage extends PDEWizardNewFileCreationPage {

	private Button fSimpleCheatSheetButton;

	private Button fCompositeCheatSheetButton;

	private Group fGroup;

	protected static final String F_FILE_EXTENSION = "xml"; //$NON-NLS-1$

	public static final int F_SIMPLE_CHEAT_SHEET = 0;

	public static final int F_COMPOSITE_CHEAT_SHEET = 1;

	/**
	 * @param pageName
	 * @param selection
	 */
	public CSFileWizardPage(String pageName, IStructuredSelection selection) {
		super(pageName, selection);

		initialize();
	}

	/**
	 * 
	 */
	protected void initialize() {
		setTitle(CSWizardMessages.CSFileWizardPage_title);
		setDescription(CSWizardMessages.CSFileWizardPage_description);
		// Force the file extension to be 'xml'
		setFileExtension(F_FILE_EXTENSION);
	}

	/**
	 * @return
	 */
	public int getCheatSheetType() {
		if (fSimpleCheatSheetButton.getSelection()) {
			return F_SIMPLE_CHEAT_SHEET;
		} else if (fCompositeCheatSheetButton.getSelection()) {
			return F_COMPOSITE_CHEAT_SHEET;
		}
		// Neither selected. Unknown type
		return -1;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#createAdvancedControls(org.eclipse.swt.widgets.Composite)
	 */
	protected void createAdvancedControls(Composite parent) {

		GridData data = null;

		// Cheat Sheet Group
		fGroup = new Group(parent, SWT.NONE);
		fGroup.setText(CSWizardMessages.CSFileWizardPage_group);
		fGroup.setLayout(new GridLayout(1, false));
		fGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// Simple Cheat Sheet Button
		fSimpleCheatSheetButton = new Button(fGroup, SWT.RADIO);
		fSimpleCheatSheetButton.setText(CSWizardMessages.CSFileWizardPage_simpleCheatSheet);
		fSimpleCheatSheetButton.setSelection(true);
		fSimpleCheatSheetButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getWizard().getContainer().updateButtons();
			}
		});

		// Simple Cheat Sheet Description Label
		final Label simpleCSLabel = new Label(fGroup, SWT.WRAP);
		simpleCSLabel.setText(CSWizardMessages.CSFileWizardPage_simpleCheatSheetDesc);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 300;
		data.horizontalIndent = 20;
		simpleCSLabel.setLayoutData(data);

		// Composite Cheat Sheet Button
		fCompositeCheatSheetButton = new Button(fGroup, SWT.RADIO);
		fCompositeCheatSheetButton.setSelection(false);
		fCompositeCheatSheetButton.setText(CSWizardMessages.CSFileWizardPage_compositeCheatSheet);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.verticalIndent = 10;
		fCompositeCheatSheetButton.setLayoutData(data);
		fCompositeCheatSheetButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getWizard().getContainer().updateButtons();
			}
		});

		// Composite Cheat Sheet Description Label
		final Label compositeCSLabel = new Label(fGroup, SWT.WRAP);
		compositeCSLabel.setText(CSWizardMessages.CSFileWizardPage_compositeCheatSheetDesc);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 300;
		data.horizontalIndent = 20;
		compositeCSLabel.setLayoutData(data);

	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		Dialog.applyDialogFont(fGroup);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.CHEAT_SHEET_PAGE);
	}

}
