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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.internal.core.target.DirectoryBundleContainer;
import org.eclipse.pde.internal.core.target.FeatureBundleContainer;
import org.eclipse.pde.internal.core.target.IUBundleContainer;
import org.eclipse.pde.internal.core.target.ProfileBundleContainer;
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

	@Override
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

	@Override
	public boolean performFinish() {
		if (fPage != null) {
			fPage.storeSettings();

			// Add the new container or replace the old one
			ITargetLocation newContainer = fPage.getBundleContainer();
			if (newContainer != null && fTarget.getTargetLocations() != null) {
				ITargetLocation[] containers = fTarget.getTargetLocations();
				List<ITargetLocation> newContainers = new ArrayList<>(containers.length);
				for (int i = 0; i < containers.length; i++) {
					if (!containers[i].equals(fContainer)) {
						newContainers.add(containers[i]);
					}
				}
				newContainers.add(newContainer);
				fTarget.setTargetLocations(newContainers.toArray(new ITargetLocation[newContainers.size()]));
			}

			return true;
		}
		return false;
	}

}
