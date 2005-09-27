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
package org.eclipse.pde.internal.ui.launcher;

import java.util.ArrayList;
import java.util.TreeMap;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

public class EquinoxLaunchShortcut implements ILaunchShortcut {
	
	private static final String CLASSPATH_PROVIDER = "org.eclipse.pde.ui.workbenchClasspathProvider"; //$NON-NLS-1$
	private static final String CONFIGURATION_TYPE = "org.eclipse.pde.ui.EquinoxLauncher";

	public void run(IPluginModelBase model) {
		launch(model, ILaunchManager.RUN_MODE);
	}
	
	public void debug(IPluginModelBase model) {
		launch(model, ILaunchManager.DEBUG_MODE);
	}
	
	public void launch(ISelection selection, String mode) {
		IPluginModelBase model = null;
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection)selection;
			if (!ssel.isEmpty()) {
				Object obj = ssel.getFirstElement();
				if (obj instanceof IAdaptable) {
					IResource resource = (IResource)((IAdaptable)obj).getAdapter(IResource.class);
					if (resource != null) {
						model = PDECore.getDefault().getModelManager().findModel(resource.getProject());
					}
				}
			}
		}
		launch(model, mode);
	}

	public void launch(IEditorPart editor, String mode) {
		launch((IPluginModelBase)null, mode);
	}
	
	private void launch(IPluginModelBase model, String mode) {
		ILaunchConfiguration[] configs = getLaunchConfigurations();
		ILaunchConfiguration configuration = null;
		if (configs.length == 0) {
			PluginModelManager manager = PDECore.getDefault().getModelManager();
			IPluginModelBase[] models = (model == null) ? manager.getWorkspaceModels() : new IPluginModelBase[] {model};
            if (models.length == 0) {
            	IPluginModelBase osgi = manager.findModel("org.eclipse.osgi");
            	if (osgi != null)
            		models = new IPluginModelBase[] {osgi};
            }
            configuration =  createNewConfiguration(models, mode);
		} else if (configs.length == 1) {
			configuration = configs[0];
		} else {
			configuration = chooseConfiguration(configs, mode);
		}
		
		if (configuration != null)
			DebugUITools.launch(configuration, mode);		
	}
	
	private ILaunchConfiguration[] getLaunchConfigurations() {
		ArrayList result = new ArrayList();
		try {
			ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfiguration[] configs = manager.getLaunchConfigurations(getLaunchConfigurationType());
			for (int i = 0; i < configs.length; i++) {
				if (!DebugUITools.isPrivate(configs[i])) {
							result.add(configs[i]);
				}
			}
		} catch (CoreException e) {
		}
		return (ILaunchConfiguration[]) result.toArray(new ILaunchConfiguration[result.size()]);
	}
	
	private ILaunchConfigurationType getLaunchConfigurationType() {
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		return  manager.getLaunchConfigurationType(CONFIGURATION_TYPE);		
	}
	
	protected ILaunchConfiguration chooseConfiguration(ILaunchConfiguration[] configs, String mode) {
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
		int result= dialog.open();
		labelProvider.dispose();
		if (result == ElementListSelectionDialog.OK) {
			return (ILaunchConfiguration)dialog.getFirstResult();
		}
		return null;		
	}
	
	private ILaunchConfiguration createNewConfiguration(IPluginModelBase[] selected, String mode) {
		ILaunchConfiguration config = null;
		try {
			ILaunchConfigurationType configType = getLaunchConfigurationType();
			String computedName = getComputedName("Equinox");
			ILaunchConfigurationWorkingCopy wc = configType.newInstance(null, computedName);  
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, ""); //$NON-NLS-1$
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, "-console"); //$NON-NLS-1$
			wc.setAttribute(IPDELauncherConstants.TRACING_CHECKED, IPDELauncherConstants.TRACING_NONE);
			wc.setAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true);
			
			TreeMap map = new TreeMap();
			for (int i = 0; i < selected.length; i++)
				RuntimeWorkbenchShortcut.addPluginAndDependencies(selected[i], map);
			Object[] models = map.values().toArray();
			StringBuffer wsplugins = new StringBuffer();
			StringBuffer explugins = new StringBuffer();
			for (int i = 0; i < models.length; i++) {
				IPluginModelBase model = (IPluginModelBase)models[i];
				String id = model.getPluginBase().getId();
				String value = "org.eclipse.osgi".equals(id) ? "@:" : "@default:default";
				if (model.getUnderlyingResource() == null) {
					if (explugins.length() > 0)
						explugins.append(","); //$NON-NLS-1$
					explugins.append(id);
					explugins.append(value);
				} else {
					if (wsplugins.length() > 0)
						wsplugins.append(","); //$NON-NLS-1$
					wsplugins.append(id);
					wsplugins.append(value);
				}
			}
			wc.setAttribute(IPDELauncherConstants.WORKSPACE_BUNDLES, wsplugins.toString());
			wc.setAttribute(IPDELauncherConstants.TARGET_BUNDLES, explugins.toString());
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, CLASSPATH_PROVIDER);
			config = wc.doSave();		
		} catch (CoreException ce) {
			PDEPlugin.logException(ce);
		} 
		return config;
	}

	private String getComputedName(String prefix) {
		ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
		return lm.generateUniqueLaunchConfigurationNameFrom(prefix);
	}

}
