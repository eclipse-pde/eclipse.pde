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
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Preferences;
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
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

public class EquinoxLaunchShortcut implements ILaunchShortcut {
	
	private static final String CLASSPATH_PROVIDER = "org.eclipse.pde.ui.workbenchClasspathProvider"; //$NON-NLS-1$
	private static final String CONFIGURATION_TYPE = "org.eclipse.pde.ui.EquinoxLauncher"; //$NON-NLS-1$

	public void run(IProject project) {
		launch(PDECore.getDefault().getModelManager().findModel(project), ILaunchManager.RUN_MODE);
	}
	
	public void debug(IProject project) {
		launch(PDECore.getDefault().getModelManager().findModel(project), ILaunchManager.DEBUG_MODE);
	}
	
	public void launch(ISelection selection, String mode) {
		launch((IPluginModelBase)null, mode);
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
            	IPluginModelBase osgi = manager.findModel("org.eclipse.osgi"); //$NON-NLS-1$
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
		if (result == Window.OK) {
			return (ILaunchConfiguration)dialog.getFirstResult();
		}
		return null;		
	}
	
	private ILaunchConfiguration createNewConfiguration(IPluginModelBase[] selected, String mode) {
		ILaunchConfiguration config = null;
		try {
			ILaunchConfigurationType configType = getLaunchConfigurationType();
			String computedName = getComputedName("Equinox"); //$NON-NLS-1$
			ILaunchConfigurationWorkingCopy wc = configType.newInstance(null, computedName);  
			setJavaArguments(wc);
			wc.setAttribute(IPDELauncherConstants.TRACING_CHECKED, IPDELauncherConstants.TRACING_NONE);
			wc.setAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true);
			initializePluginState(wc, selected);
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, CLASSPATH_PROVIDER);
			config = wc.doSave();		
		} catch (CoreException ce) {
			PDEPlugin.logException(ce);
		} 
		return config;
	}
	
	private void setJavaArguments(ILaunchConfigurationWorkingCopy wc) {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		String progArgs = preferences.getString(ICoreConstants.PROGRAM_ARGS);
		if (progArgs.indexOf("-console") == -1) //$NON-NLS-1$
			progArgs = "-console " + progArgs; //$NON-NLS-1$
		wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, progArgs); //$NON-NLS-1$
		String vmArgs = preferences.getString(ICoreConstants.VM_ARGS);
		if (vmArgs.length() > 0)
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, vmArgs);
	}
	
	public static void initializePluginState(ILaunchConfigurationWorkingCopy wc, IPluginModelBase[] selected) {
		Map startLevelMap = getStartLevelMap();
		TreeMap pluginMap = new TreeMap();
		for (int i = 0; i < selected.length; i++)
			RuntimeWorkbenchShortcut.addPluginAndDependencies(selected[i], pluginMap);
		Object[] models = pluginMap.values().toArray();
		StringBuffer wsplugins = new StringBuffer();
		StringBuffer explugins = new StringBuffer();
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase model = (IPluginModelBase)models[i];
			String id = model.getPluginBase().getId();
			String value = "org.eclipse.osgi".equals(id) ? "@:" : "@default:default"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (startLevelMap.containsKey(id))
				value = (String)startLevelMap.get(id);
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
		
	}
	
	private static TreeMap getStartLevelMap() {
		TreeMap startLevels = new TreeMap();
		Properties props = TargetPlatform.getConfigIniProperties();
		if (props != null) {
			String value = (String)props.get("osgi.bundles"); //$NON-NLS-1$
			if (value != null) {
				StringTokenizer tokenizer = new StringTokenizer(value, ","); //$NON-NLS-1$
				while (tokenizer.hasMoreTokens()) {
					String tokenValue = tokenizer.nextToken();
					int index = tokenValue.indexOf("@"); //$NON-NLS-1$
					if (index > 0) {
						String plugin = tokenValue.substring(0,index).trim();
						startLevels.put(plugin, getStartValue(tokenValue.substring(index)));
					}
				}
			}
		} 
		return startLevels;
	}
	
	private static String getStartValue(String value) {
		StringBuffer buffer = new StringBuffer(value);
				
		StringBuffer result = new StringBuffer("@"); //$NON-NLS-1$
		result.append(":"); //$NON-NLS-1$
		
		int index = value.indexOf("start"); //$NON-NLS-1$
		result.append(Boolean.toString(index != -1));
		
		if (index != -1)
			buffer.delete(index, index + 5);
		
		int colon = value.indexOf(':');
		if (colon != -1)
			buffer.deleteCharAt(colon);
		
		// delete the first char '@'
		buffer.deleteCharAt(0);
		
		try {
			result.insert(1, Integer.parseInt(buffer.toString().trim()));
		} catch (NumberFormatException e) {
			result.insert(1, "default"); //$NON-NLS-1$
		}
		return result.toString();
	}

	private String getComputedName(String prefix) {
		ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
		return lm.generateUniqueLaunchConfigurationNameFrom(prefix);
	}

}
