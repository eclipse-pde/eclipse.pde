/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.launcher;

import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.pde.internal.ui.launcher.JUnitProgramBlock;

/**
 * A launch configuration tab that displays and edits the main launching arguments
 * of a Plug-in JUnit test.
 * <p>
 * This class may be instantiated. This class is not intended to be subclassed by clients.
 * </p>
 * @since 3.2
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
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		fDataBlock.setDefaults(config, true);
		fProgramBlock.setDefaults(config);
		fJreBlock.setDefaults(config);
	}
	

}
