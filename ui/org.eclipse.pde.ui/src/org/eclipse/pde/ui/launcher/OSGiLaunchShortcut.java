/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.launcher;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.launcher.OSGiFrameworkManager;
import org.eclipse.ui.IEditorPart;

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

	protected String getLaunchConfigurationTypeName() {
		return "org.eclipse.pde.ui.EquinoxLauncher";	 //$NON-NLS-1$
	}
	
	protected void initializeConfiguration(ILaunchConfigurationWorkingCopy configuration) {
		OSGiFrameworkManager manager = PDEPlugin.getDefault().getOSGiFrameworkManager();
		manager.getDefaultInitializer().initialize(configuration);
	}

	protected boolean isGoodMatch(ILaunchConfiguration configuration) {
		return true;
	}
	
}
