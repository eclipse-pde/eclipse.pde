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
package org.eclipse.pde.internal.ui.shared.target;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;

/**
 * Wizard for adding a bundle container to a target.  Provides a selection page
 * where the user can choose the type of container to create.
 * 
 * @see BundleContainerTable
 * @see IBundleContainer
 */
public class AddBundleContainerWizard extends Wizard {

	private ITargetDefinition fTarget;

	public AddBundleContainerWizard(ITargetDefinition target) {
		fTarget = target;
		setWindowTitle(Messages.AddBundleContainerSelectionPage_1);
		setForcePreviousAndNextButtons(true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		addPage(new AddBundleContainerSelectionPage(fTarget));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish() {
		// Handled by the individual container wizards
		return true;
	}

}
