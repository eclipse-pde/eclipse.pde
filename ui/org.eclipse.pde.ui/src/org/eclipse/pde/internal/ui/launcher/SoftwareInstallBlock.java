/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.ui.launcher.AbstractLauncherTab;
import org.eclipse.pde.ui.launcher.ConfigurationTab;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

/**
 * Block on the {@link ConfigurationTab} that allows the user to choose 
 * between generating a p2 metadata profile for the launch or simply
 * reusing the profile from the host.
 * 
 * @since 3.6
 */
public class SoftwareInstallBlock {

	private Button fGenerateProfileButton;
	private AbstractLauncherTab fTab;

	/**
	 * Constructor
	 * @param tab the tab that this block is added to, used to apply changes
	 */
	public SoftwareInstallBlock(AbstractLauncherTab tab) {
		fTab = tab;
	}

	/**
	 * Create the UI elements for this block
	 * @param parent parent composite
	 */
	public void createControl(Composite parent) {
		Group group = SWTFactory.createGroup(parent, PDEUIMessages.ProfileBlock_0, 1, 1, GridData.FILL_HORIZONTAL);
		fGenerateProfileButton = SWTFactory.createCheckButton(group, PDEUIMessages.ProfileBlock_1, null, false, 1);
		fGenerateProfileButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fTab.updateLaunchConfigurationDialog();
			}
		});
	}

	/**
	 * Sets up any default configuration attributes.
	 * @param configuration configuration to modify
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		// No defaults required
	}

	/**
	 * Initializes the state of UI components from the configuration attributes
	 * @param configuration the configuration to get attributes from
	 * @throws CoreException if an error occurs getting an attribute
	 */
	public void initializeFrom(ILaunchConfiguration configuration) throws CoreException {
		fGenerateProfileButton.setSelection(configuration.getAttribute(IPDELauncherConstants.GENERATE_PROFILE, false));
	}

	/**
	 * Sets attributes on the configuration based on the current state of the UI elements
	 * @param configuration configuration to modify
	 */
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		if (fGenerateProfileButton.getSelection()) {
			configuration.setAttribute(IPDELauncherConstants.GENERATE_PROFILE, true);
		} else {
			configuration.removeAttribute(IPDELauncherConstants.GENERATE_PROFILE);
		}
	}

	/**
	 * @return a string error message or <code>null</code> if the block contents are valid
	 */
	public String validate() {
		return null;
	}

}
