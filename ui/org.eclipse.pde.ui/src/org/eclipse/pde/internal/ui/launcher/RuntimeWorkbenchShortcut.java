/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.util.ArrayList;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
 
/**
 * A launch short cut for the run-time workspace.
 */
public class RuntimeWorkbenchShortcut implements ILaunchShortcut {
	
	public void run() {
		launch(ILaunchManager.RUN_MODE);
	}
	
	public void debug() {
		launch(ILaunchManager.DEBUG_MODE);
	}

	/*
	 * @see ILaunchShortcut#launch(IEditorPart, String)
	 */
	public void launch(IEditorPart editor, String mode) {
		launch(mode);
	}

	/*
	 * @see ILaunchShortcut#launch(ISelection, String)
	 */
	public void launch(ISelection selection, String mode) {
		launch(mode);
	}
		
	/**
	 * Launches a configuration in the given mode
	 */
	protected void launch(String mode) {
		ILaunchConfiguration config = findLaunchConfiguration(mode);
		if (config != null) {
			DebugUITools.launch(config, mode);
		}			
	}
	
	/**
	 * Locate a configuration to relaunch.  If one cannot be found, create one.
	 * 
	 * @return a re-useable config or <code>null</code> if none
	 */
	protected ILaunchConfiguration findLaunchConfiguration(String mode) {
		ILaunchConfiguration[] configs =
			getLaunchConfigurations(getWorkbenchLaunchConfigType());
			
		if (configs.length == 0)
			return createConfiguration();

		if (configs.length == 1)
			return configs[0];

		// Prompt the user to choose a config. 
		return chooseConfiguration(configs, mode);
	}
	
	private ILaunchConfiguration[] getLaunchConfigurations(ILaunchConfigurationType configType) {
		ArrayList result = new ArrayList();
		try {
			ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfiguration[] configs = manager.getLaunchConfigurations(configType);
			for (int i = 0; i < configs.length; i++) {
				if (!DebugUITools.isPrivate(configs[i]))
					result.add(configs[i]);
			}
		} catch (CoreException e) {
		}
		return (ILaunchConfiguration[]) result.toArray(
			new ILaunchConfiguration[result.size()]);
	}
	
	/**
	 * Shows a selection dialog that allows the user to choose one of the specified
	 * launch configurations.  Return the chosen config, or <code>null</code> if the
	 * user cancelled the dialog.
	 */
	protected ILaunchConfiguration chooseConfiguration(ILaunchConfiguration[] configs, String mode) {
		IDebugModelPresentation labelProvider = DebugUITools.newDebugModelPresentation();
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), labelProvider);
		dialog.setElements(configs);
		dialog.setTitle(PDEPlugin.getResourceString("RuntimeWorkbenchShortcut.title"));  //$NON-NLS-1$
		if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			dialog.setMessage(PDEPlugin.getResourceString("RuntimeWorkbenchShortcut.select.debug")); //$NON-NLS-1$
		} else {
			dialog.setMessage(PDEPlugin.getResourceString("RuntimeWorkbenchShortcut.select.run"));  //$NON-NLS-1$
		}
		dialog.setMultipleSelection(false);
		int result= dialog.open();
		labelProvider.dispose();
		if (result == ElementListSelectionDialog.OK) {
			return (ILaunchConfiguration)dialog.getFirstResult();
		}
		return null;		
	}
	
	/**
	 * Creates a new configuration with default values.
	 */
	protected ILaunchConfiguration createConfiguration() {
		ILaunchConfiguration config = null;
		try {
			ILaunchConfigurationType configType= getWorkbenchLaunchConfigType();
			String computedName = getComputedName(configType.getName());
			ILaunchConfigurationWorkingCopy wc = configType.newInstance(null, computedName);  //$NON-NLS-1$
			wc.setAttribute(ILauncherSettings.LOCATION + "0", getDefaultWorkspaceLocation()); //$NON-NLS-1$
			wc.setAttribute(ILauncherSettings.VMARGS, ""); //$NON-NLS-1$
			wc.setAttribute(ILauncherSettings.PROGARGS, LauncherUtils.getDefaultProgramArguments());
			wc.setAttribute(ILauncherSettings.USECUSTOM, true);
			wc.setAttribute(ILauncherSettings.USEFEATURES, false);
			wc.setAttribute(
				IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER,
				"org.eclipse.pde.ui.workbenchClasspathProvider"); //$NON-NLS-1$
			wc.setAttribute(ILauncherSettings.DOCLEAR, false);
			wc.setAttribute(ILauncherSettings.ASKCLEAR, true);
			config= wc.doSave();		
		} catch (CoreException ce) {
			PDEPlugin.logException(ce);
		} 
		return config;
	}
	
	/**
	 * Returns the workbench config type
	 */
	protected ILaunchConfigurationType getWorkbenchLaunchConfigType() {
		ILaunchManager lm= DebugPlugin.getDefault().getLaunchManager();
		// constant?
		return lm.getLaunchConfigurationType("org.eclipse.pde.ui.RuntimeWorkbench");		 //$NON-NLS-1$
	}	
	
	private String getComputedName(String prefix) {
		ILaunchManager lm= DebugPlugin.getDefault().getLaunchManager();
		return lm.generateUniqueLaunchConfigurationNameFrom(prefix);
	}
	
	/**
	 * Convenience method to get the window that owns this action's Shell.
	 */
	protected Shell getShell() {
		return PDEPlugin.getActiveWorkbenchShell();
	}
	
	private String getDefaultWorkspaceLocation() {
		return LauncherUtils.getDefaultPath().append("runtime-workbench-workspace").toOSString();				 //$NON-NLS-1$
	}
	
}
