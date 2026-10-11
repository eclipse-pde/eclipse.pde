/*******************************************************************************
 * Copyright (c) 2016 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Simon Scholz <simon.scholz@vogella.com> - initial API and implementation
 ******************************************************************************/
package org.eclipse.e4.tools.emf.editor3x.extension;

import org.eclipse.e4.internal.tools.wizards.classes.AbstractNewClassWizard;
import org.eclipse.e4.internal.tools.wizards.classes.NewImperativeExpressionClassWizard;
import org.eclipse.e4.ui.model.application.ui.impl.UiPackageImpl;
import org.eclipse.emf.ecore.EClass;

@SuppressWarnings("restriction")
public class ImperativeExpressionContributionEditor extends AbstractContributionEditor {

	@Override
	protected AbstractNewClassWizard createWizard(String contributionURI) {
		return new NewImperativeExpressionClassWizard(contributionURI);
	}

	@Override
	public boolean isSupported(EClass element) {
		return Util.isTypeOrSuper(UiPackageImpl.Literals.IMPERATIVE_EXPRESSION, element);
	}
}
