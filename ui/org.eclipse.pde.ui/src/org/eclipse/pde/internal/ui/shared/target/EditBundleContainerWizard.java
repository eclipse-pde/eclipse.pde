/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.internal.core.target.*;
import org.eclipse.pde.internal.ui.PDEPlugin;

/**
 * Wizard that opens an appropriate page for editing a specific type of bundle container
 *
 */
public class EditBundleContainerWizard extends Wizard {

	private ITargetDefinition fTarget;
	private ITargetLocation fContainer;
	private IEditBundleContainerPage fPage;

	public EditBundleContainerWizard(ITargetDefinition target, ITargetLocation container) {
		fTarget = target;
		fContainer = container;
		IDialogSettings settings = PDEPlugin.getDefault().getDialogSettings().getSection(AddBundleContainerSelectionPage.SETTINGS_SECTION);
		if (settings == null) {
			settings = PDEPlugin.getDefault().getDialogSettings().addNewSection(AddBundleContainerSelectionPage.SETTINGS_SECTION);
		}
		setDialogSettings(settings);
		setWindowTitle(Messages.EditBundleContainerWizard_0);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		if (fContainer instanceof DirectoryBundleContainer) {
			fPage = new EditDirectoryContainerPage(fContainer);
		} else if (fContainer instanceof ProfileBundleContainer) {
			fPage = new EditProfileContainerPage(fContainer);
		} else if (fContainer instanceof FeatureBundleContainer) {
			fPage = new EditFeatureContainerPage(fContainer);
		} else if (fContainer instanceof IUBundleContainer) {
			fPage = new EditIUContainerPage((IUBundleContainer) fContainer, fTarget);
		}
		if (fPage != null) {
			addPage(fPage);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish() {
		if (fPage != null) {
			fPage.storeSettings();

			// Add the new container or replace the old one
			ITargetLocation newContainer = fPage.getBundleContainer();
			if (newContainer != null) {
				ITargetLocation[] containers = fTarget.getTargetLocations();
				List newContainers = new ArrayList(containers.length);
				for (int i = 0; i < containers.length; i++) {
					if (!containers[i].equals(fContainer)) {
						newContainers.add(containers[i]);
					}
				}
				newContainers.add(newContainer);
				fTarget.setTargetLocations((ITargetLocation[]) newContainers.toArray(new ITargetLocation[newContainers.size()]));
			}

			return true;
		}
		return false;
	}

}
