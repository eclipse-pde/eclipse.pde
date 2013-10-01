/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
		setChangeCreationCancelable(false);
		setDefaultPageTitle(WizardMessages.JavadocConversionWizard_0);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ltk.ui.refactoring.RefactoringWizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		super.performFinish();
		return ((JavadocConversionPage) getStartingPage()).finish();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ltk.ui.refactoring.RefactoringWizard#addUserInputPages()
	 */
	@Override
	protected void addUserInputPages() {
		addPage(new JavadocConversionPage());
	}
}
