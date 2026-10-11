/*******************************************************************************
 * Copyright (c) 2010 BestSolution.at and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Tom Schindl <tom.schindl@bestsolution.at> - initial API and implementation
 * Sopot Cela <sopotcela@gmail.com>
 ******************************************************************************/
package org.eclipse.e4.tools.emf.editor3x.extension;

import org.eclipse.e4.internal.tools.wizards.classes.AbstractNewClassWizard;
import org.eclipse.e4.internal.tools.wizards.classes.NewPartClassWizard;
import org.eclipse.e4.ui.model.application.ui.basic.impl.BasicPackageImpl;
import org.eclipse.emf.ecore.EClass;

@SuppressWarnings("restriction")
public class PartContributionEditor extends AbstractContributionEditor {

	@Override
	protected AbstractNewClassWizard createWizard(String contributionURI) {
		return new NewPartClassWizard(contributionURI);
	}

	@Override
	public boolean isSupported(EClass element) {
		return Util.isTypeOrSuper(BasicPackageImpl.Literals.PART, element);
	}
}
