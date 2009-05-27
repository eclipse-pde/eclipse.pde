/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.nls;

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.refactoring.PDERefactor;

public class ExternalizeStringsWizard extends RefactoringWizard {
	private ExternalizeStringsWizardPage page1;
	private ModelChangeTable fModelChangeTable;

	public ExternalizeStringsWizard(ModelChangeTable changeTable, PDERefactor refactoring) {
		super(refactoring, WIZARD_BASED_USER_INTERFACE);
		setWindowTitle(PDEUIMessages.ExternalizeStringsWizard_title);
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_EXTSTR_WIZ);
		setNeedsProgressMonitor(true);
		fModelChangeTable = changeTable;
	}

	protected void addUserInputPages() {
		setDefaultPageTitle(getRefactoring().getName());
		page1 = new ExternalizeStringsWizardPage(fModelChangeTable);
		addPage(page1);
	}
}
