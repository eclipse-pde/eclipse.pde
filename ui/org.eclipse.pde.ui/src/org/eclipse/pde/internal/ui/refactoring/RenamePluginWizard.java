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

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

public class RenamePluginWizard extends RefactoringWizard {

	RefactoringInfo fInfo;

	public RenamePluginWizard(Refactoring refactoring, RefactoringInfo info) {
		super(refactoring, RefactoringWizard.DIALOG_BASED_USER_INTERFACE);
		fInfo = info;
	}

	protected void addUserInputPages() {
		setDefaultPageTitle(getRefactoring().getName());
		addPage(new RenamePluginWizardPage(fInfo));
	}

}
