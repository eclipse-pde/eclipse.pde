package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.*;
import org.eclipse.debug.ui.*;
import org.eclipse.jdt.debug.ui.JavaUISourceLocator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
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
			wc.setAttribute(ILauncherSettings.LOCATION + "0", BasicLauncherTab.getDefaultWorkspace()); //$NON-NLS-1$
			wc.setAttribute(ILauncherSettings.VMARGS, "");
			wc.setAttribute(ILauncherSettings.PROGARGS, BasicLauncherTab.getDefaultProgramArguments());
			wc.setAttribute(ILauncherSettings.VMINSTALL, BasicLauncherTab.getDefaultVMInstallName());
			String appName = "org.eclipse.ui.workbench"; //$NON-NLS-1$
			wc.setAttribute(ILauncherSettings.APPLICATION, appName);
			wc.setAttribute(ILauncherSettings.USECUSTOM, true);
			wc.setAttribute(IDebugUIConstants.ATTR_TARGET_DEBUG_PERSPECTIVE, IDebugUIConstants.PERSPECTIVE_DEFAULT);
			wc.setAttribute(IDebugUIConstants.ATTR_TARGET_RUN_PERSPECTIVE, IDebugUIConstants.PERSPECTIVE_NONE);
			wc.setAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID, JavaUISourceLocator.ID_PROMPTING_JAVA_SOURCE_LOCATOR);
			wc.setAttribute(ILauncherSettings.DOCLEAR, false);
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
