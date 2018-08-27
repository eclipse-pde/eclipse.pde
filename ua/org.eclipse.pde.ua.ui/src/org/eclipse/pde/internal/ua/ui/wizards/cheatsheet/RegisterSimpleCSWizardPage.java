/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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

package org.eclipse.pde.internal.ua.ui.wizards.cheatsheet;

import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSModel;

public class RegisterSimpleCSWizardPage extends RegisterCSWizardPage {

	public RegisterSimpleCSWizardPage(ISimpleCSModel model) {
		super(model);
	}

	@Override
	public String getDataCheatSheetName() {
		return ((ISimpleCSModel) fCheatSheetModel).getSimpleCS().getTitle();
	}

	@Override
	public boolean isCompositeCheatSheet() {
		return false;
	}

}
