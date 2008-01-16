/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.cheatsheet;

import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSModel;

/**
 * RegisterCompCSWizardPage
 *
 */
public class RegisterCompCSWizardPage extends RegisterCSWizardPage {

	/**
	 * @param model
	 */
	public RegisterCompCSWizardPage(ICompCSModel model) {
		super(model);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.cheatsheet.RegisterCSWizardPage#getDataName()
	 */
	public String getDataCheatSheetName() {
		return ((ICompCSModel) fCheatSheetModel).getCompCS().getFieldName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.cheatsheet.RegisterCSWizardPage#isCompositeCheatSheet()
	 */
	public boolean isCompositeCheatSheet() {
		return true;
	}

}
