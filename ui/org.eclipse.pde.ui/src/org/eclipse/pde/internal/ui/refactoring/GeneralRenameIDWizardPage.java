/*******************************************************************************
 *  Copyright (c) 2007, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.refactoring;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public abstract class GeneralRenameIDWizardPage extends UserInputWizardPage {

	protected RefactoringInfo fInfo;
	private Text fNewId;
	private Button fUpdateReferences;

	public GeneralRenameIDWizardPage(String title, RefactoringInfo info) {
		super(title);
		fInfo = info;
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		Dialog.applyDialogFont(composite);

		createMainControl(composite);

		setPageComplete(false);
		setControl(composite);
	}

	protected void createMainControl(Composite parent) {
		createNewID(parent);
		createUpdateReferences(parent);
	}

	protected void createNewID(Composite composite) {
		Label label = new Label(composite, SWT.NONE);
		label.setText(PDEUIMessages.RenamePluginWizardPage_newId);

		fNewId = new Text(composite, SWT.BORDER);
		fNewId.setText(fInfo.getCurrentValue());
		fNewId.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fNewId.addModifyListener(e -> {
			fInfo.setNewValue(fNewId.getText());
			validatePage();
		});
	}

	protected void createUpdateReferences(Composite composite) {
		fUpdateReferences = new Button(composite, SWT.CHECK);
		fUpdateReferences.setText(PDEUIMessages.RenamePluginWizardPage_updateReferences);
		fUpdateReferences.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, false, 2, 1));
		fUpdateReferences.addSelectionListener(widgetSelectedAdapter(e -> fInfo.setUpdateReferences(fUpdateReferences.getSelection())));
		fUpdateReferences.setSelection(true);
	}

	protected void validatePage() {
		String text = fNewId.getText();
		String errorMessage = null;
		if (text.length() == 0)
			errorMessage = PDEUIMessages.RenamePluginWizardPage_idNotSet;
		else
			errorMessage = validateId(text);
		if (errorMessage == null && text.equals(fInfo.getCurrentValue()))
			setPageComplete(false);
		else
			setPageComplete(errorMessage == null ? new RefactoringStatus() : RefactoringStatus.createFatalErrorStatus(errorMessage));
	}

	/**
	 * Intended to allow subclassing wizard pages validate the id based on custom criteria.
	 *
	 * @param id
	 * @return null if id is valid, otherwise return corresponding error message
	 */
	protected abstract String validateId(String id);

}
