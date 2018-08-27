/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.shared.target;

import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;

import org.eclipse.jface.wizard.Wizard;

/**
 * Wizard for adding a bundle container to a target.  Provides a selection page
 * where the user can choose the type of container to create.
 *
 * @see TargetLocationsGroup
 * @see ITargetLocation
 */
public class AddBundleContainerWizard extends Wizard {

	private ITargetDefinition fTarget;

	public AddBundleContainerWizard(ITargetDefinition target) {
		fTarget = target;
		setWindowTitle(Messages.AddBundleContainerSelectionPage_1);
		setForcePreviousAndNextButtons(true);
	}

	@Override
	public void addPages() {
		addPage(new AddBundleContainerSelectionPage(fTarget));
	}

	@Override
	public boolean performFinish() {
		// Handled by the individual container wizards
		return true;
	}

}
