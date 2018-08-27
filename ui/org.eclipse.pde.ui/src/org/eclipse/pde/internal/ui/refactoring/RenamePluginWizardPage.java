/*******************************************************************************
 *  Copyright (c) 2007, 2015 IBM Corporation and others.
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

import org.eclipse.pde.internal.core.util.IdUtil;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public class RenamePluginWizardPage extends GeneralRenameIDWizardPage {

	private Button fRenameProject;

	private static final String RENAME_PROJECT = "renameProject"; //$NON-NLS-1$

	protected RenamePluginWizardPage(RefactoringInfo info) {
		super("RenamePluginWizardPage", info); //$NON-NLS-1$
	}

	@Override
	protected void createMainControl(Composite composite) {
		createNewID(composite);
		createRenameProject(composite);
		createUpdateReferences(composite);
	}

	private void createRenameProject(Composite composite) {
		fRenameProject = new Button(composite, SWT.CHECK);
		fRenameProject.setText(PDEUIMessages.RenamePluginWizardPage_renameProject);
		fRenameProject.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, false, 2, 1));
		fRenameProject.addSelectionListener(widgetSelectedAdapter(e -> ((RefactoringPluginInfo) fInfo).setRenameProject(fRenameProject.getSelection())));
		boolean checked = getRefactoringSettings().getBoolean(RENAME_PROJECT);
		fRenameProject.setSelection(checked);
		((RefactoringPluginInfo) fInfo).setRenameProject(checked);
	}

	@Override
	public void dispose() {
		getRefactoringSettings().put(RENAME_PROJECT, fRenameProject.getSelection());
		super.dispose();
	}

	@Override
	protected String validateId(String id) {
		return IdUtil.isValidCompositeID3_0(id) ? null : PDEUIMessages.RenamePluginWizardPage_invalidId;
	}

}
