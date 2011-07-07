/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
import org.eclipse.debug.ui.*;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab;
import org.eclipse.pde.internal.launching.IPDEConstants;
import org.eclipse.pde.internal.ui.IPDEUIConstants;

/**
 * Creates and initializes the tabs for the Eclipse Application launch configuration.
 * <p>
 * This class may be instantiated or subclassed by clients.
 * </p>
 * @since 3.3
 */
public class EclipseLauncherTabGroup extends AbstractPDELaunchConfigurationTabGroup {

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTabGroup#createTabs(org.eclipse.debug.ui.ILaunchConfigurationDialog, java.lang.String)
	 */
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		ILaunchConfigurationTab[] tabs = null;
		tabs = new ILaunchConfigurationTab[] {new MainTab(), new JavaArgumentsTab(), new PluginsTab(), new ConfigurationTab(), new TracingTab(), new EnvironmentTab(), new CommonTab()};
		setTabs(tabs);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		super.performApply(configuration);
		try {
			// if the configuration has the GENERATED_CONFIG flag, we need to see if we should remove the flag
			if (!(configuration.getAttribute(IPDEUIConstants.GENERATED_CONFIG, false)))
				return;
			ILaunchConfiguration original = configuration.getOriginal();
			// peformApply is called when opening the launch dialog the first time.  In this case the user has not modified the configuration so we should 
			// keep the GENERATED_CONFIG flag.  To check to see if this is the case, we need to see if an attribute used to initialize the launch config
			// is present in the original copy.  We do this by querying the config twice, with different default values.  If the values == eachother, we 
			// we know the value is present.  Since generated configs don't contain DOCLEARLOG, we know if DOCLEARLOG is present in the original copy the 
			// perform apply so save the initialization values has already been run and this is a user modification.
			boolean firstQuery = original.getAttribute(IPDEConstants.DOCLEARLOG, false);
			boolean secondQuery = original.getAttribute(IPDEConstants.DOCLEARLOG, true);
			if (original != null && firstQuery == secondQuery)
				configuration.setAttribute(IPDEUIConstants.GENERATED_CONFIG, false);
		} catch (CoreException e) {
		}
	}

}
