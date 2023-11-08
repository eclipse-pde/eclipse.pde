/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.ui.internal.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;

/**
 * The wizard for comparing a selected set of projects against a selected
 * {@link IApiBaseline}
 *
 * @since 1.0.100
 */
public class CompareToBaselineWizard extends Wizard {

	private IStructuredSelection selection = null;

	/**
	 * Constructor
	 *
	 * @param selection
	 * @param title
	 */
	public CompareToBaselineWizard(IStructuredSelection selection, String title) {
		this.selection = selection;
		setWindowTitle(title);
	}

	@Override
	public void addPages() {
		addPage(new CompareToBaselineWizardPage(this.selection));
	}

	@Override
	public boolean performFinish() {
		return ((CompareToBaselineWizardPage) getStartingPage()).finish();
	}
}
