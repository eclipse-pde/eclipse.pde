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

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

public class RenamePluginWizard extends RefactoringWizard {

	RefactoringInfo fInfo;

	public RenamePluginWizard(Refactoring refactoring, RefactoringInfo info) {
		super(refactoring, RefactoringWizard.DIALOG_BASED_USER_INTERFACE);
		fInfo = info;
	}

	@Override
	protected void addUserInputPages() {
		setDefaultPageTitle(getRefactoring().getName());
		addPage(new RenamePluginWizardPage(fInfo));
	}

}
