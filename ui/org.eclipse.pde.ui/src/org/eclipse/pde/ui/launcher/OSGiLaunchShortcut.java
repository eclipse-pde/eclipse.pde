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
package org.eclipse.pde.ui.launcher;

import org.eclipse.pde.launching.IPDELauncherConstants;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.internal.launching.PDELaunchingPlugin;
import org.eclipse.pde.internal.launching.launcher.OSGiFrameworkManager;
import org.eclipse.ui.IEditorPart;

/**
 * A launch shortcut capable of launching an OSGi frameowrk
 * <p>
 * This class may be instantiated or subclassed by clients.
 * </p>
 * @since 3.3
 */
public class OSGiLaunchShortcut extends AbstractLaunchShortcut {

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchShortcut#launch(org.eclipse.jface.viewers.ISelection, java.lang.String)
	 */
	public void launch(ISelection selection, String mode) {
		launch(mode);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchShortcut#launch(org.eclipse.ui.IEditorPart, java.lang.String)
	 */
	public void launch(IEditorPart editor, String mode) {
		launch(mode);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.ui.launcher.AbstractLaunchShortcut#getLaunchConfigurationTypeName()
	 */
	protected String getLaunchConfigurationTypeName() {
		return IPDELauncherConstants.OSGI_CONFIGURATION_TYPE;
	}

	/**
	 * Delegates to the initializer associated with the selected OSGI framework
	 * to initialize the launch configuration
	 * <p>
	 * Refer to the <code>org.eclipse.pde.ui.osgiFrameworks</code> extension point.
	 * </p>
	 * @see org.eclipse.pde.ui.launcher.AbstractLaunchShortcut#initializeConfiguration(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	protected void initializeConfiguration(ILaunchConfigurationWorkingCopy configuration) {
		OSGiFrameworkManager manager = PDELaunchingPlugin.getDefault().getOSGiFrameworkManager();
		manager.getDefaultInitializer().initialize(configuration);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.ui.launcher.AbstractLaunchShortcut#isGoodMatch(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	protected boolean isGoodMatch(ILaunchConfiguration configuration) {
		return true;
	}

}
