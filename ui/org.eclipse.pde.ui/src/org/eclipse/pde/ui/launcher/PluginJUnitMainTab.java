/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
	protected void createProgramBlock() {
		fProgramBlock = new JUnitProgramBlock(this);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
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

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		fDataBlock.setDefaults(config, true);
		fProgramBlock.setDefaults(config);
		fJreBlock.setDefaults(config);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		fDataBlock.performApply(config, true);
		fProgramBlock.performApply(config);
		fJreBlock.performApply(config);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getId()
	 */
	public String getId() {
		return IPDELauncherConstants.TAB_PLUGIN_JUNIT_MAIN_ID;
	}

}
