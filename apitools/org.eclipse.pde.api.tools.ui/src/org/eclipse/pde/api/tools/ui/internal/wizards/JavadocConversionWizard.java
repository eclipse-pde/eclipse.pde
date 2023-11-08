/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
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
 * Wizard to convert Javadoc tags to the new annotations
 *
 * @since 1.0.500
 */
public class JavadocConversionWizard extends RefactoringWizard {

	/**
	 * Constructor
	 *
	 * @param refactoring
	 */
	public JavadocConversionWizard(Refactoring refactoring) {
		super(refactoring, RefactoringWizard.WIZARD_BASED_USER_INTERFACE);
		setWindowTitle(WizardMessages.JavadocConversionWizard_0);
		setNeedsProgressMonitor(true);
		setChangeCreationCancelable(true);
		setDefaultPageTitle(WizardMessages.JavadocConversionWizard_0);
	}

	@Override
	public boolean performFinish() {
		super.performFinish();
		return ((JavadocConversionPage) getStartingPage()).finish();
	}

	@Override
	protected void addUserInputPages() {
		addPage(new JavadocConversionPage());
	}
}
