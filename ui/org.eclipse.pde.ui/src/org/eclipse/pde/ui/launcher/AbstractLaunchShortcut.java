/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.launcher;

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
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

public abstract class AbstractLaunchShortcut implements ILaunchShortcut {

	protected void launch(String mode) {
		ILaunchConfiguration[] configs = getLaunchConfigurations();
		ILaunchConfiguration configuration = null;
		if (configs.length == 0) {
            configuration =  createNewConfiguration();
		} else if (configs.length == 1) {
			configuration = configs[0];
		} else {
			configuration = chooseConfiguration(configs, mode);
		}
		
		if (configuration != null)
			DebugUITools.launch(configuration, mode);		
	}
	
	protected ILaunchConfiguration[] getLaunchConfigurations() {
		ArrayList result = new ArrayList();
		try {
			ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfigurationType type =  manager.getLaunchConfigurationType(getLaunchConfigurationTypeName());		
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

	private ILaunchConfiguration chooseConfiguration(ILaunchConfiguration[] configs, String mode) {
		IDebugModelPresentation labelProvider = DebugUITools.newDebugModelPresentation();
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), labelProvider);
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
		return (result == Window.OK) ? (ILaunchConfiguration)dialog.getFirstResult() : null;
	}
	
	private ILaunchConfiguration createNewConfiguration() {
		try {
			ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfigurationType type = lm.getLaunchConfigurationType(getLaunchConfigurationTypeName());
			String name = lm.generateUniqueLaunchConfigurationNameFrom(type.getName());	
			ILaunchConfigurationWorkingCopy wc = type.newInstance(null, name);  
			initializeConfiguration(wc);
			return wc.doSave();		
		} catch (CoreException ce) {
			PDEPlugin.logException(ce);
		} 
		return null;
	}
	
	protected abstract void initializeConfiguration(ILaunchConfigurationWorkingCopy wc);
	
	protected abstract String getLaunchConfigurationTypeName();
	
	protected abstract boolean isGoodMatch(ILaunchConfiguration configuration);

}
