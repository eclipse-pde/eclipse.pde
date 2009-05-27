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
import org.eclipse.pde.internal.core.util.IdUtil;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class RenameExtensionPointWizard extends RefactoringWizard {

	RefactoringInfo fInfo;

	public RenameExtensionPointWizard(Refactoring refactoring, RefactoringInfo info) {
		super(refactoring, RefactoringWizard.DIALOG_BASED_USER_INTERFACE);
		fInfo = info;
	}

	protected void addUserInputPages() {
		setDefaultPageTitle(getRefactoring().getName());
		addPage(new GeneralRenameIDWizardPage(PDEUIMessages.RenameExtensionPointWizard_pageTitle, fInfo) {

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
