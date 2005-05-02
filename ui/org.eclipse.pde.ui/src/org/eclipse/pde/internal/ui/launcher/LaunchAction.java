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

import java.io.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.*;
import org.eclipse.debug.ui.*;
import org.eclipse.jdt.launching.*;
import org.eclipse.jface.action.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.dialogs.*;

public class LaunchAction extends Action {

	private IProduct fProduct;
	private String fMode;
	private String fPath;

	public LaunchAction(IProduct product, String path, String mode) {
		fProduct = product;
		fMode = mode;
		fPath = path;
	}
	
	public void run() {
		try {
			ILaunchConfiguration config = findLaunchConfiguration();
			if (config != null)
				DebugUITools.launch(config, fMode);
		} catch (CoreException e) {
		}
	}
	
	private ILaunchConfiguration findLaunchConfiguration() throws CoreException {
		ILaunchConfiguration[] configs = getLaunchConfigurations();
		
		if (configs.length == 0)
			return createConfiguration();

		ILaunchConfiguration config = null;
		if (configs.length == 1) {
			config = configs[0];
		} else {
			// Prompt the user to choose a config. 
			config = chooseConfiguration(configs);
		}
		
		if (config != null) {
			config = refreshConfiguration(config.getWorkingCopy());
		}
		return config;
	}

	private ILaunchConfiguration refreshConfiguration(ILaunchConfigurationWorkingCopy wc) throws CoreException {
		wc.setAttribute(ILauncherSettings.PRODUCT, fProduct.getId());
		wc.setAttribute(ILauncherSettings.VMARGS, getVMArguments()); 
		wc.setAttribute(ILauncherSettings.PROGARGS, getProgramArguments());
		StringBuffer wsplugins = new StringBuffer();
		StringBuffer explugins = new StringBuffer();
		IPluginModelBase[] models = getModels();
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase model = models[i];
			String id = model.getPluginBase().getId();
			if (model.getUnderlyingResource() == null) {
				explugins.append(id + File.pathSeparatorChar);
			} else {
				wsplugins.append(id + File.pathSeparatorChar);
			}
		}
		wc.setAttribute(ILauncherSettings.WSPROJECT, wsplugins.toString());
		wc.setAttribute(ILauncherSettings.EXTPLUGINS, explugins.toString());
		String configIni = getTemplateConfigIni();
		wc.setAttribute(ILauncherSettings.CONFIG_GENERATE_DEFAULT, configIni == null);
		if (configIni != null)
			wc.setAttribute(ILauncherSettings.CONFIG_TEMPLATE_LOCATION, configIni);
		return wc.doSave();
	}
	
	private String getProgramArguments() {
		IArgumentsInfo info = fProduct.getLauncherArguments();
		return (info != null) ? CoreUtility.normalize(info.getProgramArguments()) : ""; //$NON-NLS-1$
	}
	
	private String getVMArguments() {
		IArgumentsInfo info = fProduct.getLauncherArguments();
		return (info != null) ? CoreUtility.normalize(info.getVMArguments()) : ""; //$NON-NLS-1$
	}	
	
	private IPluginModelBase[] getModels() {
		HashMap map = new HashMap();
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		if (fProduct.useFeatures()) {
			IFeatureModel[] features = getUniqueFeatures();
			for (int i = 0; i < features.length; i++) {
				addFeaturePlugins(features[i].getFeature(), map);
			}
		} else {
			IProductPlugin[] plugins = fProduct.getPlugins();
			for (int i = 0; i < plugins.length; i++) {
				String id = plugins[i].getId();
				if (id == null || map.containsKey(id))
					continue;
				IPluginModelBase model = manager.findModel(id);
				if (model != null)
					map.put(id, model);				
			}
		}
		return (IPluginModelBase[])map.values().toArray(new IPluginModelBase[map.size()]);
	}
	
	private IFeatureModel[] getUniqueFeatures() {
		ArrayList list = new ArrayList();
		IProductFeature[] features = fProduct.getFeatures();
		for (int i = 0; i < features.length; i++) {
			String id = features[i].getId();
			String version = features[i].getVersion();
			addFeatureAndChildren(id, version, list);
		}
		return (IFeatureModel[])list.toArray(new IFeatureModel[list.size()]);
	}
	
	private void addFeatureAndChildren(String id, String version, List list) {
		FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
		IFeatureModel model = manager.findFeatureModel(id, version);
		if (model == null || list.contains(model))
			return;
		
		list.add(model);
		
		IFeatureChild[] children = model.getFeature().getIncludedFeatures();
		for (int i = 0; i < children.length; i++) {
			addFeatureAndChildren(children[i].getId(), children[i].getVersion(), list);
		}	
	}
	
	private void addFeaturePlugins(IFeature feature, HashMap map) {
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		IFeaturePlugin[] plugins = feature.getPlugins();
		for (int i = 0; i < plugins.length; i++) {
			String id = plugins[i].getId();
			if (id == null || map.containsKey(id))
				continue;
			IPluginModelBase model = manager.findModel(id);
			if (model != null)
				map.put(id, model);
		}
	}
	
	private String getTemplateConfigIni() {
		IConfigurationFileInfo info = fProduct.getConfigurationFileInfo();
		if (info != null  && info.getUse().equals("custom")) { //$NON-NLS-1$
			String path = getExpandedPath(info.getPath());
			if (path != null) {
				File file = new File(path);
				if (file.exists() && file.isFile())
					return file.getAbsolutePath();
			}
		}
		return null;
	}
	
	private String getExpandedPath(String path) {
		if (path == null || path.length() == 0)
			return null;
		IResource resource = PDEPlugin.getWorkspace().getRoot().findMember(new Path(path));
		if (resource != null) {
			IPath fullPath = resource.getLocation();
			return fullPath == null ? null : fullPath.toOSString();
		}
		return null;
	}


	private ILaunchConfiguration chooseConfiguration(ILaunchConfiguration[] configs) {
		IDebugModelPresentation labelProvider = DebugUITools.newDebugModelPresentation();
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), labelProvider);
		dialog.setElements(configs);
		dialog.setTitle(PDEUIMessages.RuntimeWorkbenchShortcut_title);  //$NON-NLS-1$
		if (fMode.equals(ILaunchManager.DEBUG_MODE)) {
			dialog.setMessage(PDEUIMessages.RuntimeWorkbenchShortcut_select_debug); //$NON-NLS-1$
		} else {
			dialog.setMessage(PDEUIMessages.RuntimeWorkbenchShortcut_select_run);  //$NON-NLS-1$
		}
		dialog.setMultipleSelection(false);
		int result= dialog.open();
		labelProvider.dispose();
		if (result == ElementListSelectionDialog.OK) {
			return (ILaunchConfiguration)dialog.getFirstResult();
		}
		return null;		
	}

	private ILaunchConfiguration createConfiguration() throws CoreException {
		ILaunchConfigurationType configType = getWorkbenchLaunchConfigType();
		String computedName = getComputedName(new Path(fPath).lastSegment());
		ILaunchConfigurationWorkingCopy wc = configType.newInstance(null, computedName);  //$NON-NLS-1$
		wc.setAttribute(ILauncherSettings.LOCATION + "0", RuntimeWorkbenchShortcut.getDefaultWorkspaceLocation(computedName)); //$NON-NLS-1$
		wc.setAttribute(ILauncherSettings.USEFEATURES, false);
		wc.setAttribute(ILauncherSettings.USE_DEFAULT, false);
		wc.setAttribute(ILauncherSettings.DOCLEAR, false);
		wc.setAttribute(ILauncherSettings.ASKCLEAR, true);
		wc.setAttribute(ILauncherSettings.USE_PRODUCT, true);
		wc.setAttribute(ILauncherSettings.AUTOMATIC_ADD, false);
		wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, RuntimeWorkbenchShortcut.CLASSPATH_PROVIDER);
		wc.setAttribute(ILauncherSettings.PRODUCT_FILE, fPath);
		return refreshConfiguration(wc);		
	}
	
	private String getComputedName(String prefix) {
		ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
		return lm.generateUniqueLaunchConfigurationNameFrom(prefix);
	}
	
	private ILaunchConfiguration[] getLaunchConfigurations() throws CoreException {
		ArrayList result = new ArrayList();
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = manager.getLaunchConfigurationType(RuntimeWorkbenchShortcut.CONFIGURATION_TYPE);	
		ILaunchConfiguration[] configs = manager.getLaunchConfigurations(type);
		for (int i = 0; i < configs.length; i++) {
			if (!DebugUITools.isPrivate(configs[i])) {
				String path = configs[i].getAttribute(ILauncherSettings.PRODUCT_FILE, ""); //$NON-NLS-1$
				if (new Path(fPath).equals(new Path(path))) {
					result.add(configs[i]);
				}
			}
		}
		return (ILaunchConfiguration[]) result.toArray(new ILaunchConfiguration[result.size()]);
	}
	
	protected ILaunchConfigurationType getWorkbenchLaunchConfigType() {
		ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
		return lm.getLaunchConfigurationType(RuntimeWorkbenchShortcut.CONFIGURATION_TYPE);	
	}	
	
}
