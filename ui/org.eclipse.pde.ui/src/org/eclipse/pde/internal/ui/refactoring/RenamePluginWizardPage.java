package org.eclipse.pde.internal.ui.refactoring;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.pde.internal.core.util.IdUtil;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

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
public class RenamePluginWizardPage extends UserInputWizardPage {
	
	private Text fNewId;
	private Button fUpdateReferences;
	private Button fRenameProject;
	private RenamePluginInfo fInfo;
	
	private static final String RENAME_PROJECT = "renameProject"; //$NON-NLS-1$

	protected RenamePluginWizardPage(RenamePluginInfo info) {
		super("RenamePluginWizardPage"); //$NON-NLS-1$
		fInfo = info;
	}

	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		Dialog.applyDialogFont(composite);
		
		createNewID(composite);
		createRenameProject(composite);
		createUpdateReferences(composite);
		
		setPageComplete(false);
		setControl(composite);
	}
	
	private void createNewID(Composite composite) {
		Label label = new Label(composite, SWT.NONE);
		label.setText(PDEUIMessages.RenamePluginWizardPage_newId);
		
		fNewId = new Text(composite, SWT.BORDER);
		fNewId.setText(fInfo.getCurrentID());
		fNewId.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fNewId.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				fInfo.setNewID(fNewId.getText());
				validatePage();
			}
		});
	}
	
	private void createRenameProject(Composite composite) {
		fRenameProject = new Button(composite, SWT.CHECK);
		fRenameProject.setText(PDEUIMessages.RenamePluginWizardPage_renameProject);
		fRenameProject.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, false, 2, 1));
		fRenameProject.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fInfo.setRenameProject(fRenameProject.getSelection());
			}
		});
		boolean checked = getRefactoringSettings().getBoolean(RENAME_PROJECT);
		fRenameProject.setSelection(checked);
		fInfo.setRenameProject(checked);
	}
	
	private void createUpdateReferences(Composite composite) {
		fUpdateReferences = new Button(composite, SWT.CHECK);
		fUpdateReferences.setText(PDEUIMessages.RenamePluginWizardPage_updateReferences);
		fUpdateReferences.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, false, 2, 1));
		fUpdateReferences.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fInfo.setUpdateReferences(fUpdateReferences.getSelection());
			}
		});
		fUpdateReferences.setSelection(true);
	}
	
	public void dispose() {
		getRefactoringSettings().put(RENAME_PROJECT, fRenameProject.getSelection());
	}
	
	protected void validatePage() {
		String text = fNewId.getText();
		String errorMessage = null;
		if (text.length() == 0)
			errorMessage = PDEUIMessages.RenamePluginWizardPage_idNotSet;
		else if (!IdUtil.isValidCompositeID(text)) 
			errorMessage = PDEUIMessages.RenamePluginWizardPage_invalidId;
		if (errorMessage == null && text.equals(fInfo.getCurrentID()))
			setPageComplete(false);
		else
			setPageComplete(errorMessage == null ? new RefactoringStatus() : RefactoringStatus.createFatalErrorStatus(errorMessage));
	}

}
