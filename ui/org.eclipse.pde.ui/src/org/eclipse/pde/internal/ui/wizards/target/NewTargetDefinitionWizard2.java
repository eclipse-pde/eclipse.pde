/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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

	@Override
	public void addPages() {
		fPage = new TargetCreationPage("profile"); //$NON-NLS-1$
		addPage(fPage);
		addPage(new TargetDefinitionContentPage(null));
	}

	@Override
	public boolean canFinish() {
		return false;
	}

	@Override
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
