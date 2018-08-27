/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.wizards;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

/**
 * Wizard for updating the Javadoc tags of a java project using the
 * component.xml file for the project
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

	@Override
	public boolean performFinish() {
		super.performFinish();
		return ((ApiToolingSetupWizardPage) getStartingPage()).finish();
	}

	@Override
	protected void addUserInputPages() {
		addPage(new ApiToolingSetupWizardPage());
	}
}
