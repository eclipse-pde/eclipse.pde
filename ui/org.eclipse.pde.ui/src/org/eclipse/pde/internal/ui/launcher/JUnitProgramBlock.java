/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.pde.launching.IPDELauncherConstants;

import java.util.TreeSet;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.pde.internal.launching.IPDEConstants;
import org.eclipse.pde.internal.launching.launcher.LauncherUtils;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.ui.launcher.AbstractLauncherTab;

public class JUnitProgramBlock extends ProgramBlock {

	public JUnitProgramBlock(AbstractLauncherTab tab) {
		super(tab);
	}

	@Override
	protected String getApplicationAttribute() {
		return IPDELauncherConstants.APP_TO_TEST;
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		if (!LauncherUtils.requiresUI(config))
			config.setAttribute(IPDELauncherConstants.APPLICATION, IPDEConstants.CORE_TEST_APPLICATION);
		else
			super.setDefaults(config);
	}

	@Override
	protected String[] getApplicationNames() {
		TreeSet<String> result = new TreeSet<>();
		result.add(PDEUIMessages.JUnitProgramBlock_headless);
		String[] appNames = super.getApplicationNames();
		for (String appName : appNames) {
			result.add(appName);
		}
		return result.toArray(new String[result.size()]);
	}

	@Override
	protected void initializeApplicationSection(ILaunchConfiguration config) throws CoreException {
		String application = config.getAttribute(IPDELauncherConstants.APPLICATION, (String) null);
		if (IPDEConstants.CORE_TEST_APPLICATION.equals(application))
			fApplicationCombo.setText(PDEUIMessages.JUnitProgramBlock_headless);
		else
			super.initializeApplicationSection(config);
	}

	@Override
	protected void saveApplicationSection(ILaunchConfigurationWorkingCopy config) {
		if (fApplicationCombo.getText().equals(PDEUIMessages.JUnitProgramBlock_headless)) {
			String appName = fApplicationCombo.isEnabled() ? IPDEConstants.CORE_TEST_APPLICATION : null;
			config.setAttribute(IPDELauncherConstants.APPLICATION, appName);
			config.setAttribute(IPDELauncherConstants.APP_TO_TEST, (String) null);
		} else {
			config.setAttribute(IPDELauncherConstants.APPLICATION, (String) null);
			super.saveApplicationSection(config);
		}
	}

}
