/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.refactoring;

import org.eclipse.pde.internal.core.util.IdUtil;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public class RenamePluginWizardPage extends GeneralRenameIDWizardPage {

	private Button fRenameProject;

	private static final String RENAME_PROJECT = "renameProject"; //$NON-NLS-1$

	protected RenamePluginWizardPage(RefactoringInfo info) {
		super("RenamePluginWizardPage", info); //$NON-NLS-1$
	}

	protected void createMainControl(Composite composite) {
		createNewID(composite);
		createRenameProject(composite);
		createUpdateReferences(composite);
	}

	private void createRenameProject(Composite composite) {
		fRenameProject = new Button(composite, SWT.CHECK);
		fRenameProject.setText(PDEUIMessages.RenamePluginWizardPage_renameProject);
		fRenameProject.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, false, 2, 1));
		fRenameProject.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				((RefactoringPluginInfo) fInfo).setRenameProject(fRenameProject.getSelection());
			}
		});
		boolean checked = getRefactoringSettings().getBoolean(RENAME_PROJECT);
		fRenameProject.setSelection(checked);
		((RefactoringPluginInfo) fInfo).setRenameProject(checked);
	}

	public void dispose() {
		getRefactoringSettings().put(RENAME_PROJECT, fRenameProject.getSelection());
		super.dispose();
	}

	protected String validateId(String id) {
		return IdUtil.isValidCompositeID3_0(id) ? null : PDEUIMessages.RenamePluginWizardPage_invalidId;
	}

}
