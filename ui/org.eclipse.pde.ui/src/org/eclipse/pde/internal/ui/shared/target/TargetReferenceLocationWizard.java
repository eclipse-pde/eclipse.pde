/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
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
import org.eclipse.pde.internal.core.target.TargetReferenceBundleContainer;
import org.eclipse.pde.ui.target.ITargetLocationWizard;

public class TargetReferenceLocationWizard extends Wizard implements ITargetLocationWizard {

	private EditTargetContainerPage page;
	private ITargetLocation fLocation;
	private ITargetDefinition target;
	private TargetReferenceBundleContainer bundleContainer;

	@Override
	public void setTarget(ITargetDefinition target) {
		this.target = target;
	}

	@Override
	public void addPages() {
		addPage(page = new EditTargetContainerPage(target, bundleContainer));
	}

	@Override
	public ITargetLocation[] getLocations() {
		return new ITargetLocation[] { fLocation };
	}

	@Override
	public boolean performFinish() {
		fLocation = new TargetReferenceBundleContainer(page.furiLocation.getText());
		if (target != null && bundleContainer != null) {
			ITargetLocation[] locations = target.getTargetLocations();
			for (int i = 0; i < locations.length; i++) {
				ITargetLocation location = locations[i];
				if (location == bundleContainer) {
					locations[i] = fLocation;
				}
			}
			target.setTargetLocations(locations);
		}
		return true;
	}

	public void setBundleContainer(TargetReferenceBundleContainer bundleContainer) {
		this.bundleContainer = bundleContainer;
	}

}
