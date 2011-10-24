/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.target;

import org.eclipse.pde.core.target.ITargetDefinition;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;

/**
 * Target definition wizard used to create a new target definition from
 * the new target platform preference page. 
 */
public class NewTargetDefinitionWizard2 extends Wizard {

	TargetCreationPage fPage;
	ITargetDefinition fDefinition;

	public NewTargetDefinitionWizard2() {
		super();
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_TARGET_WIZ);
		setWindowTitle(PDEUIMessages.NewTargetProfileWizard_title);
		setNeedsProgressMonitor(true);
	}

	public void addPages() {
		fPage = new TargetCreationPage("profile"); //$NON-NLS-1$
		addPage(fPage);
		addPage(new TargetDefinitionContentPage(null));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#canFinish()
	 */
	public boolean canFinish() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish() {
		return true;
	}

	/**
	 * Returns the target definition created by this wizard.
	 * 
	 * @return target definition or <code>null</code> if none
	 */
	public ITargetDefinition getTargetDefinition() {
		return fDefinition;
	}

	/**
	 * Sets the target being edited.
	 * 
	 * @param definition target
	 */
	public void setTargetDefinition(ITargetDefinition definition) {
		fDefinition = definition;
	}
}
