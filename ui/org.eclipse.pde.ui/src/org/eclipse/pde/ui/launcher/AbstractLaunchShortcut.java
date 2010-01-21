/*******************************************************************************
 *  Copyright (c) 2006, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.launcher;

import java.util.ArrayList;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.*;
import org.eclipse.debug.ui.*;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

/**
 * An abstract class subclassed by the Eclipse Application and OSGi Framework launch shortcuts.
 * <p>
 * This class may be subclassed by clients.
 * </p>
 * @since 3.3
 */
public abstract class AbstractLaunchShortcut implements ILaunchShortcut {

	/**
	 * Launches the application in the specified mode, or does nothing if the user canceled
	 * the launch when offered to select one of several available launch configurations.
	 * 
	 * @param mode 
	 * 			mode of launch (run, debug or profile)
	 * 
	 * @see org.eclipse.debug.core.ILaunchManager
	 */
	protected void launch(String mode) {
		ILaunchConfiguration configuration = findLaunchConfiguration(mode);
		if (configuration != null)
			DebugUITools.launch(configuration, mode);
	}

	/**
	 * This method first tries to locate existing launch configurations that are suitable
	 * for the application or framework being launched.
	 * <p>
	 * <ul>
	 * <li>If none are found, a new launch configuration is created and initialized</li>
	 * <li>If one is found, it is launched automatically</li>
	 * <li>If more than one is found, a selection dialog is presented to the user and the chosen
	 * one will be launched</li>
	 * </ul>
	 * </p>
	 * @param mode 
	 * 			mode of launch (run, debug or profile)
	 * 
	 * @return a launch configuration to run or <code>null</code> if launch is canceled
	 */
	protected ILaunchConfiguration findLaunchConfiguration(String mode) {
		ILaunchConfiguration[] configs = getLaunchConfigurations();
		ILaunchConfiguration configuration = null;
		if (configs.length == 0) {
			configuration = createNewConfiguration();
		} else if (configs.length == 1) {
			configuration = configs[0];
		} else {
			configuration = chooseConfiguration(configs, mode);
		}
		return configuration;
	}

	/**
	 * Returns a list of existing launch configurations that are suitable to launch to selected
	 * application or framework.
	 * 
	 * @return an array of launch configurations
	 */
	private ILaunchConfiguration[] getLaunchConfigurations() {
		ArrayList result = new ArrayList();
		try {
			ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfigurationType type = manager.getLaunchConfigurationType(getLaunchConfigurationTypeName());
			ILaunchConfiguration[] configurations = manager.getLaunchConfigurations(type);
			for (int i = 0; i < configurations.length; i++) {
				if (!DebugUITools.isPrivate(configurations[i]) && isGoodMatch(configurations[i])) {
					result.add(configurations[i]);
				}
			}
		} catch (CoreException e) {
		}
		return (ILaunchConfiguration[]) result.toArray(new ILaunchConfiguration[result.size()]);
	}

	/**
	 * Display to the user a list of matching existing launch configurations and return the user's selection.
	 * 
	 * @param configs  
	 * 			an array of matching existing launch configurations
	 * @param mode 
	 * 			mode of launch
	 * @return
	 * 			the launch configuration selected by the user or <code>null</code> if Cancel was pressed
	 */
	protected ILaunchConfiguration chooseConfiguration(ILaunchConfiguration[] configs, String mode) {
		IDebugModelPresentation labelProvider = DebugUITools.newDebugModelPresentation();
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), labelProvider);
		dialog.setElements(configs);
		dialog.setTitle(PDEUIMessages.RuntimeWorkbenchShortcut_title);
		if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			dialog.setMessage(PDEUIMessages.RuntimeWorkbenchShortcut_select_debug);
		} else {
			dialog.setMessage(PDEUIMessages.RuntimeWorkbenchShortcut_select_run);
		}
		dialog.setMultipleSelection(false);
		int result = dialog.open();
		labelProvider.dispose();
		return (result == Window.OK) ? (ILaunchConfiguration) dialog.getFirstResult() : null;
	}

	/**
	 * Create, initialize and return a new launch configuration of the given type
	 * 
	 * @return a new, properly-initialized launch configuration 
	 */
	private ILaunchConfiguration createNewConfiguration() {
		try {
			ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfigurationType type = lm.getLaunchConfigurationType(getLaunchConfigurationTypeName());
			String name = lm.generateLaunchConfigurationName(getName(type));
			ILaunchConfigurationWorkingCopy wc = type.newInstance(null, name);
			initializeConfiguration(wc);
			// set a flag to know the information in the new config was generated by PDE
			wc.setAttribute(IPDEUIConstants.GENERATED_CONFIG, true);
			return wc.doSave();
		} catch (CoreException ce) {
			PDEPlugin.logException(ce);
		}
		return null;
	}

	/**
	 * Returns the name assigned to the new launch configuration
	 * 
	 * @return a name for the new launch configuration
	 */
	protected String getName(ILaunchConfigurationType type) {
		return type.getName();
	}

	/**
	 * Initialize launch attributes on the new launch configuration.
	 * Must be overridden by subclasses.
	 * 
	 * @param wc 
	 * 			the launch configuration working copy to be initialize
	 * 
	 * @see IPDELauncherConstants
	 */
	protected abstract void initializeConfiguration(ILaunchConfigurationWorkingCopy wc);

	/**
	 * Returns the launch configuration type name.
	 * Must be overridden by subclasses
	 * 
	 * @return the launch configuration type name
	 */
	protected abstract String getLaunchConfigurationTypeName();

	/**
	 * Determines whether a given launch configuration is a good match given the current application or framework
	 * being launched.  This method must be overridden by subclasses.  Its purpose is to add criteria on 
	 * what makes a good match or not.
	 * 
	 * @param configuration 
	 * 			the launch configuration being evaluated
	 * @return
	 * 		<code>true</code> if the launch configuration is a good match for the application or 
	 * 		framework being launched, <code>false</code> otherwise.
	 */
	protected abstract boolean isGoodMatch(ILaunchConfiguration configuration);

}
