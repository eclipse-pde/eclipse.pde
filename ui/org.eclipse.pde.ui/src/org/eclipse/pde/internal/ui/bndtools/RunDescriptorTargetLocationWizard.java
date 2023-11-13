/*******************************************************************************
 * Copyright (c) 2017, 2019 bndtools project and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Elias N Vasylenko <eliasvasylenko@gmail.com> - initial API and implementation
 *     BJ Hargrave <bj@bjhargrave.com> - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ui.bndtools;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.ui.target.ITargetLocationWizard;

public class RunDescriptorTargetLocationWizard extends Wizard implements ITargetLocationWizard {
	private ITargetDefinition				targetDefinition;
	private RunDescriptorTargetLocation		targetLocation;

	private RunDescriptorTargetLocationPage	targetLocationPage;

	public RunDescriptorTargetLocationWizard() {
		setWindowTitle("Run Descriptor Target Location");
	}

	@Override
	public void setTarget(ITargetDefinition targetDefinition) {
		this.targetDefinition = targetDefinition;
	}

	public void setTargetLocation(RunDescriptorTargetLocation targetLocation) {
		this.targetLocation = targetLocation;
	}

	@Override
	public void addPages() {
		targetLocationPage = new RunDescriptorTargetLocationPage(targetDefinition, targetLocation);
		addPage(targetLocationPage);
	}

	@Override
	public boolean performFinish() {
		targetLocation = targetLocationPage.getBundleContainer();
		return true;
	}

	@Override
	public ITargetLocation[] getLocations() {
		return new ITargetLocation[] {
			targetLocation
		};
	}
}
