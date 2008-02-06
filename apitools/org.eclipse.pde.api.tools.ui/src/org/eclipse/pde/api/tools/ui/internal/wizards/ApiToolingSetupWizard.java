/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.wizards;

import org.eclipse.jface.wizard.Wizard;

/**
 * Wizard for updating the Javadoc tags of a java project using the component.xml file for the project
 * @since 1.0.0
 */
public class ApiToolingSetupWizard extends Wizard {

	/**
	 * Constructor
	 */
	public ApiToolingSetupWizard() {
		setWindowTitle(WizardMessages.UpdateJavadocTagsWizard_0);
		setNeedsProgressMonitor(true);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish() {
		return ((ApiToolingSetupWizardPage) getStartingPage()).finish();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		addPage(new ApiToolingSetupWizardPage());
	}
}
