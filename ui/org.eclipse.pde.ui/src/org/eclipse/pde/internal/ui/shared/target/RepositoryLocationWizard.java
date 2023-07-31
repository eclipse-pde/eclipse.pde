/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.internal.core.target.RepositoryBundleContainer;
import org.eclipse.pde.ui.target.ITargetLocationWizard;

public class RepositoryLocationWizard extends Wizard implements ITargetLocationWizard {

	private EditRepositoryContainerPage repositoryPage;
	private ITargetLocation wizardLocation;
	private ITargetDefinition target;
	private RepositoryBundleContainer bundleContainer;
	private SelectRepositoryContentPage selectionPage;

	public RepositoryLocationWizard() {
		setNeedsProgressMonitor(true);
	}

	@Override
	public void setTarget(ITargetDefinition target) {
		this.target = target;
	}

	@Override
	public void addPages() {
		addPage(repositoryPage = new EditRepositoryContainerPage(bundleContainer));
		addPage(selectionPage = new SelectRepositoryContentPage(repositoryPage));

		setWindowTitle(repositoryPage.getDefaultTitle());
	}

	@Override
	public ITargetLocation[] getLocations() {
		if (wizardLocation == null) {
			return new ITargetLocation[0];
		}
		return new ITargetLocation[] { wizardLocation };
	}

	@Override
	public boolean performFinish() {
		wizardLocation = selectionPage.getBundleContainer();
		if (target != null && bundleContainer != null) {
			ITargetLocation[] locations = target.getTargetLocations();
			for (int i = 0; i < locations.length; i++) {
				ITargetLocation location = locations[i];
				if (location == bundleContainer) {
					locations[i] = wizardLocation;
				}
			}
			target.setTargetLocations(locations);
		}
		return true;
	}

	public void setBundleContainer(RepositoryBundleContainer bundleContainer) {
		this.bundleContainer = bundleContainer;
	}

}
