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
package org.eclipse.pde.ui.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.launcher.JUnitProgramBlock;
import org.eclipse.pde.launching.IPDELauncherConstants;

/**
 * A launch configuration tab that displays and edits the main launching arguments
 * of a Plug-in JUnit test.
 * <p>
 * This class may be instantiated. This class is not intended to be subclassed by clients.
 * </p>
 * @since 3.2
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
public class PluginJUnitMainTab extends MainTab {

	/**
	 * Overrides the implementation of the basis MainTab.
	 */
	@Override
	protected void createProgramBlock() {
		fProgramBlock = new JUnitProgramBlock(this);
	}

	@Override
	public void initializeFrom(ILaunchConfiguration config) {
		try {
			fDataBlock.initializeFrom(config, true);
			fProgramBlock.initializeFrom(config);
			fJreBlock.initializeFrom(config);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		} finally {
		}
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		fDataBlock.setDefaults(config, true);
		fProgramBlock.setDefaults(config);
		fJreBlock.setDefaults(config);
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		fDataBlock.performApply(config, true);
		fProgramBlock.performApply(config);
		fJreBlock.performApply(config);
	}

	@Override
	public String getId() {
		return IPDELauncherConstants.TAB_PLUGIN_JUNIT_MAIN_ID;
	}

}
