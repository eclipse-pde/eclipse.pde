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
import org.eclipse.pde.internal.core.util.IdUtil;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class RenameExtensionPointWizard extends RefactoringWizard {

	RefactoringInfo fInfo;

	public RenameExtensionPointWizard(Refactoring refactoring, RefactoringInfo info) {
		super(refactoring, RefactoringWizard.DIALOG_BASED_USER_INTERFACE);
		fInfo = info;
	}

	@Override
	protected void addUserInputPages() {
		setDefaultPageTitle(getRefactoring().getName());
		addPage(new GeneralRenameIDWizardPage(PDEUIMessages.RenameExtensionPointWizard_pageTitle, fInfo) {

			@Override
			protected String validateId(String id) {
				String schemaVersion = fInfo.getBase().getPluginBase().getSchemaVersion();
				if (schemaVersion == null || Float.parseFloat(schemaVersion) >= 3.2) {
					if (!IdUtil.isValidCompositeID(id))
						return PDEUIMessages.BaseExtensionPointMainPage_invalidCompositeID;
				} else if (!IdUtil.isValidSimpleID(id))
					return PDEUIMessages.BaseExtensionPointMainPage_invalidSimpleID;
				return null;
			}

		});
	}

}
