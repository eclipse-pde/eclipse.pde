/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.wizards;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

/**
 * Wizard for updating the Javadoc tags of a java project using the component.xml file for the project
 * 
 * @since 1.0.0
 */
public class ApiToolingSetupWizard extends RefactoringWizard {

	/**
	 * Constructor
	 */
	public ApiToolingSetupWizard(Refactoring refactoring) {
		super(refactoring, RefactoringWizard.WIZARD_BASED_USER_INTERFACE);
		setWindowTitle(WizardMessages.UpdateJavadocTagsWizard_0);
		setNeedsProgressMonitor(true);
		setChangeCreationCancelable(false);
		setDefaultPageTitle(WizardMessages.ApiToolingSetupWizard_0);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ltk.ui.refactoring.RefactoringWizard#performFinish()
	 */
	public boolean performFinish() {
		super.performFinish();
		return ((ApiToolingSetupWizardPage) getStartingPage()).finish();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.ui.refactoring.RefactoringWizard#addUserInputPages()
	 */
	protected void addUserInputPages() {
		addPage(new ApiToolingSetupWizardPage());
	}
}
