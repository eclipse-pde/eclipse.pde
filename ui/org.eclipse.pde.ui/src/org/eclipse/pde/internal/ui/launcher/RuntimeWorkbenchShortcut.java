/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.*;
import org.eclipse.debug.ui.*;
import org.eclipse.jdt.debug.ui.JavaUISourceLocator;
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
		try { 
			ILaunchConfiguration config = findLaunchConfiguration(mode);
			if (config != null) {
				DebugUITools.saveAndBuildBeforeLaunch();
				config.launch(mode, null);
			}			
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	
	/**
	 * Locate a configuration to relaunch.  If one cannot be found, create one.
	 * 
	 * @return a re-useable config or <code>null</code> if none
	 */
	protected ILaunchConfiguration findLaunchConfiguration(String mode) {
		ILaunchConfigurationType configType= getWorkbenchLaunchConfigType();
		ILaunchConfiguration[] configs= new ILaunchConfiguration[0];
		try {
			configs= DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(configType);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		// if there is no config then create one
		int candidateCount= configs.length;
		if (candidateCount < 1) {
			return createConfiguration();
		} else if (candidateCount == 1) {
			return configs[0];
		} else {
			// Prompt the user to choose a config. 
			ILaunchConfiguration config= chooseConfiguration(configs, mode);
			if (config != null) {
				return config;
			}
		}
		return null;
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
			wc.setAttribute(ILauncherSettings.LOCATION + "0", LauncherUtils.getDefaultWorkspace()); //$NON-NLS-1$
			wc.setAttribute(ILauncherSettings.VMARGS, "");
			wc.setAttribute(ILauncherSettings.PROGARGS, LauncherUtils.getDefaultProgramArguments());
			wc.setAttribute(ILauncherSettings.USECUSTOM, true);
			wc.setAttribute(ILauncherSettings.USEFEATURES, false);
			wc.setAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID, JavaUISourceLocator.ID_PROMPTING_JAVA_SOURCE_LOCATOR);
			wc.setAttribute(
				IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER,
				"org.eclipse.pde.ui.workbenchClasspathProvider");
			wc.setAttribute(ILauncherSettings.DOCLEAR, false);
			wc.setAttribute(ILauncherSettings.ASKCLEAR, true);
			wc.setAttribute(ILauncherSettings.SHOW_SPLASH, true);
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
}
