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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.internal.core.target.FeatureBundleContainer;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * Wizard page for editing a feature bundle container, currently none of the options can be changed
 *
 */
public class EditFeatureContainerPage extends EditDirectoryContainerPage {

	public EditFeatureContainerPage(ITargetLocation container) {
		super(container, "EditFeatureContainer"); //$NON-NLS-1$
	}

	@Override
	protected String getDefaultTitle() {
		return Messages.EditFeatureContainerPage_0;
	}

	@Override
	protected String getDefaultMessage() {
		return Messages.EditFeatureContainerPage_1;
	}

	@Override
	protected void createLocationArea(Composite parent) {
		FeatureBundleContainer container = (FeatureBundleContainer) getBundleContainer();
		Composite comp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_HORIZONTAL);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IHelpContextIds.LOCATION_EDIT_FEATURE_WIZARD);

		SWTFactory.createLabel(comp, Messages.EditFeatureContainerPage_2, 1);
		Text text = SWTFactory.createText(comp, SWT.READ_ONLY | SWT.BORDER, 1);
		text.setText(container.getFeatureId());

		SWTFactory.createLabel(comp, Messages.EditFeatureContainerPage_3, 1);
		text = SWTFactory.createText(comp, SWT.READ_ONLY | SWT.BORDER, 1);
		text.setText(container.getFeatureVersion() != null ? container.getFeatureVersion() : Messages.EditFeatureContainerPage_4);

		SWTFactory.createLabel(comp, Messages.EditFeatureContainerPage_5, 1);
		text = SWTFactory.createText(comp, SWT.READ_ONLY | SWT.BORDER, 1);
		try {
			text.setText(container.getLocation(false));
		} catch (CoreException e) {
			setErrorMessage(e.getMessage());
			PDEPlugin.log(e);
		}
	}

	@Override
	protected void initializeInputFields(ITargetLocation container) {
		containerChanged(0);
	}

	@Override
	public void storeSettings() {
		// Do nothing, no settings
	}

	@Override
	protected boolean validateInput() {
		return true;
	}

	@Override
	protected ITargetLocation createContainer(ITargetLocation previous) throws CoreException {
		return getBundleContainer();
	}

}
