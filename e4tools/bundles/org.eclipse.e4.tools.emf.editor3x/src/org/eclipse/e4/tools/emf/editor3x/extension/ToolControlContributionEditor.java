/*******************************************************************************
 * Copyright (c) 2014 BestSolution.at and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Dmitry Spiridenok <d.spiridenok@gmail.com> - Bug 412672
 ******************************************************************************/
package org.eclipse.e4.tools.emf.editor3x.extension;

import org.eclipse.e4.internal.tools.wizards.classes.AbstractNewClassWizard;
import org.eclipse.e4.internal.tools.wizards.classes.NewToolControlClassWizard;
import org.eclipse.e4.ui.model.application.ui.menu.impl.MenuPackageImpl;
import org.eclipse.emf.ecore.EClass;

@SuppressWarnings("restriction")
public class ToolControlContributionEditor extends AbstractContributionEditor {

	@Override
	protected AbstractNewClassWizard createWizard(String contributionURI) {
		return new NewToolControlClassWizard(contributionURI);
	}

	@Override
	public boolean isSupported(EClass element) {
		return Util.isTypeOrSuper(MenuPackageImpl.Literals.TOOL_CONTROL, element);
	}
}
