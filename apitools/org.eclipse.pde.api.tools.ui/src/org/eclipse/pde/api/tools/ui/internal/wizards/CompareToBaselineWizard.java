/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;

/**
 * The wizard for comparing a selected set of projects against a selected {@link IApiBaseline}
 * 
 * @since 1.0.100
 */
public class CompareToBaselineWizard extends Wizard {

	private IStructuredSelection selection = null;
	
	/**
	 * Constructor
	 * @param selection
	 * @param title
	 */
	public CompareToBaselineWizard(IStructuredSelection selection, String title) {
		this.selection = selection;
		setWindowTitle(title);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		addPage(new CompareToBaselineWizardPage(this.selection));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish() {
		return ((CompareToBaselineWizardPage)getStartingPage()).finish();
	}
}
