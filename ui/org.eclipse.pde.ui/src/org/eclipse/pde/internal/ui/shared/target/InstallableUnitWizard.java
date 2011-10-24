/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.ui.target.ITargetLocationWizard;

/**
 * Wizard for selecting Installable Units. 
 * 
 * Contributed to provide UI for <code>IUBundleContainer</code> target location through extension to 
 * org.eclipse.pde.ui.targetProvisioner 
 *
 */
public class InstallableUnitWizard extends Wizard implements ITargetLocationWizard {

	private ITargetDefinition fTarget;

	private ITargetLocation fLocation;

	/**
	 * Section in the dialog settings for this wizard and the wizards created with selection
	 * Shared with the EditBundleContainerWizard
	 */
	static final String SETTINGS_SECTION = "editBundleContainerWizard"; //$NON-NLS-1$

	public InstallableUnitWizard() {
		setWindowTitle(Messages.AddBundleContainerSelectionPage_1);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.target.ITargetLocationWizard#setTarget(org.eclipse.pde.core.target.ITargetDefinition)
	 */
	public void setTarget(ITargetDefinition target) {
		fTarget = target;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		IDialogSettings settings = PDEPlugin.getDefault().getDialogSettings().getSection(SETTINGS_SECTION);
		if (settings == null) {
			settings = PDEPlugin.getDefault().getDialogSettings().addNewSection(SETTINGS_SECTION);
		}
		setDialogSettings(settings);
		addPage(new EditIUContainerPage(fTarget));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish() {
		fLocation = ((EditIUContainerPage) getPages()[0]).getBundleContainer();
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.ILocationWizard#getLocations()
	 */
	public ITargetLocation[] getLocations() {
		return new ITargetLocation[] {fLocation};
	}

}
