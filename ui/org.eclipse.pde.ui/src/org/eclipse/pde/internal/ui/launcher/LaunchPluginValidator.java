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
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.SearchablePluginsManager;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;

public class LaunchPluginValidator {
	
	public static void checkBackwardCompatibility(ILaunchConfiguration configuration, boolean save) throws CoreException {		
		ILaunchConfigurationWorkingCopy wc = null;
		if (configuration.isWorkingCopy()) {
			wc = (ILaunchConfigurationWorkingCopy) configuration;
		} else {
			wc = configuration.getWorkingCopy();
		}
		
		String value = configuration.getAttribute("wsproject", (String)null); //$NON-NLS-1$
		if (value != null) {
			wc.setAttribute("wsproject", (String)null); //$NON-NLS-1$
			if (value.indexOf(';') != -1) {
				value = value.replace(';', ',');
			} else if (value.indexOf(':') != -1) {
				value = value.replace(':', ',');
			}
			value = (value.length() == 0 || value.equals(",")) //$NON-NLS-1$
						? null : value.substring(0, value.length() - 1);
			
			boolean automatic = configuration.getAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true);
			String attr = automatic 
							? IPDELauncherConstants.DESELECTED_WORKSPACE_PLUGINS
							: IPDELauncherConstants.SELECTED_WORKSPACE_PLUGINS;
			wc.setAttribute(attr, value);
		}

		String value2 = configuration.getAttribute("extplugins", (String)null); //$NON-NLS-1$
		if (value2 != null) {
			wc.setAttribute("extplugins", (String)null); //$NON-NLS-1$
			if (value2.indexOf(';') != -1)
				value2 = value2.replace(';', ',');	
			else if (value2.indexOf(':') != -1)
				value2 = value2.replace(':', ',');
			value2 = (value2.length() == 0 || value2.equals(",")) //$NON-NLS-1$
						? null : value2.substring(0, value2.length() - 1);
			wc.setAttribute(IPDELauncherConstants.SELECTED_TARGET_PLUGINS, value2);
		}
		
		String version = configuration.getAttribute("pde.version", (String) null); //$NON-NLS-1$
		boolean upgrade = (TargetPlatform.isRuntimeRefactored1() && version == null)
					   || (TargetPlatform.isRuntimeRefactored2() && !"3.2a".equals(version)); //$NON-NLS-1$
		if (upgrade) {
			if (TargetPlatform.isRuntimeRefactored2())
				wc.setAttribute("pde.version", "3.2a"); //$NON-NLS-1$ //$NON-NLS-2$
			else
				wc.setAttribute("pde.version", "3.2"); //$NON-NLS-1$ //$NON-NLS-2$
			boolean usedefault = configuration.getAttribute(IPDELauncherConstants.USE_DEFAULT, true);
			boolean useFeatures = configuration.getAttribute(IPDELauncherConstants.USEFEATURES, false);
			boolean automaticAdd = configuration.getAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true);
			if (!usedefault && !useFeatures) {
				ArrayList list = new ArrayList();
				if (version == null) {
					list.add("org.eclipse.core.contenttype"); //$NON-NLS-1$
					list.add("org.eclipse.core.jobs"); //$NON-NLS-1$
					list.add("org.eclipse.equinox.common"); //$NON-NLS-1$
					list.add("org.eclipse.equinox.preferences"); //$NON-NLS-1$
					list.add("org.eclipse.equinox.registry"); //$NON-NLS-1$
				}
				if (TargetPlatform.isRuntimeRefactored2())
					list.add("org.eclipse.core.runtime.compatibility.registry"); //$NON-NLS-1$
													
				StringBuffer extensions = new StringBuffer(configuration.getAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_PLUGINS, "")); //$NON-NLS-1$
				StringBuffer target = new StringBuffer(configuration.getAttribute(IPDELauncherConstants.SELECTED_TARGET_PLUGINS, "")); //$NON-NLS-1$
				PluginModelManager manager = PDECore.getDefault().getModelManager();
				for (int i = 0; i < list.size(); i++) {
					String plugin = list.get(i).toString();
					IPluginModelBase model = manager.findModel(plugin);
					if (model == null)
						continue;
					if (model.getUnderlyingResource() != null) {
						if (automaticAdd)
							continue;
						if (extensions.length() > 0)
							extensions.append(","); //$NON-NLS-1$
						extensions.append(plugin);
					} else {
						if (target.length() > 0)
							target.append(","); //$NON-NLS-1$
						target.append(plugin);
					}					
				}
				if (extensions.length() > 0)
					wc.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_PLUGINS, extensions.toString());
				if (target.length() > 0)
					wc.setAttribute(IPDELauncherConstants.SELECTED_TARGET_PLUGINS, target.toString());
			}
		}
		
		if (save && (value != null || value2 != null || upgrade))
			wc.doSave();
	}
	
	private static IPluginModelBase[] getSelectedWorkspacePlugins(ILaunchConfiguration configuration)
			throws CoreException {
		
		boolean usedefault = configuration.getAttribute(IPDELauncherConstants.USE_DEFAULT, true);
		boolean useFeatures = configuration.getAttribute(IPDELauncherConstants.USEFEATURES, false);
		
		IPluginModelBase[] models = PDECore.getDefault().getModelManager().getWorkspaceModels();
		
		if (usedefault || useFeatures || models.length == 0)
			return models;
		
		ArrayList list = new ArrayList();
		if (configuration.getAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true)) {
			TreeSet deselected = parsePlugins(configuration,
									IPDELauncherConstants.DESELECTED_WORKSPACE_PLUGINS);
			if (deselected.size() == 0)
				return models;
			for (int i = 0; i < models.length; i++) {
				String id = models[i].getPluginBase().getId();
				if (id != null && !deselected.contains(id)) 
					list.add(models[i]);
			}		
		} else {
			TreeSet selected = parsePlugins(configuration, 
									IPDELauncherConstants.SELECTED_WORKSPACE_PLUGINS);
			for (int i = 0; i < models.length; i++) {
				String id = models[i].getPluginBase().getId();
				if (id != null && selected.contains(id)) 
					list.add(models[i]);
			}
		}
		return (IPluginModelBase[])list.toArray(new IPluginModelBase[list.size()]);
	}

	public static TreeSet parsePlugins(ILaunchConfiguration configuration, String attribute)
			throws CoreException {
		TreeSet set = new TreeSet();
		String ids = configuration.getAttribute(attribute, (String) null);
		if (ids != null) {
			StringTokenizer tok = new StringTokenizer(ids, ","); //$NON-NLS-1$
			while (tok.hasMoreTokens())
				set.add(tok.nextToken());
		}
		return set;
	}

	public static IPluginModelBase[] getPluginList(ILaunchConfiguration config) throws CoreException {
		Map map = getPluginsToRun(config);
		return (IPluginModelBase[])map.values().toArray(new IPluginModelBase[map.size()]);
	}
	
	public static Map getPluginsToRun(ILaunchConfiguration config)
			throws CoreException {

		checkBackwardCompatibility(config, true);
				
		TreeMap map = new TreeMap();
		if (config.getAttribute(IPDELauncherConstants.USE_DEFAULT, true)) {
			IPluginModelBase[] models = PDECore.getDefault().getModelManager().getPlugins();
			for (int i = 0; i < models.length; i++) {
				String id = models[i].getPluginBase().getId();
				if (id != null)
					map.put(id, models[i]);
			}
			return map;
		}
		
		if (config.getAttribute(IPDELauncherConstants.USEFEATURES, false)) {
			IPluginModelBase[] models = PDECore.getDefault().getModelManager().getWorkspaceModels();
			for (int i = 0; i < models.length; i++) {
				String id = models[i].getPluginBase().getId();
				if (id != null)
					map.put(id, models[i]);
			}
			return map;
		}
		
		IPluginModelBase[] wsmodels = getSelectedWorkspacePlugins(config);
		for (int i = 0; i < wsmodels.length; i++) {
			String id = wsmodels[i].getPluginBase().getId();
			if (id != null)
				map.put(id, wsmodels[i]);
		}

		Set exModels = parsePlugins(config, IPDELauncherConstants.SELECTED_TARGET_PLUGINS);
		IPluginModelBase[] exmodels = PDECore.getDefault().getModelManager().getExternalModels();
		for (int i = 0; i < exmodels.length; i++) {
			String id = exmodels[i].getPluginBase().getId();
			if (id != null && exModels.contains(id) && !map.containsKey(id))
				map.put(id, exmodels[i]);
		}
		return map;
	}

	public static IProject[] getAffectedProjects(ILaunchConfiguration config)
			throws CoreException {
		ArrayList projects = new ArrayList();
		IPluginModelBase[] models = getSelectedWorkspacePlugins(config);
		for (int i = 0; i < models.length; i++) {
			IProject project = models[i].getUnderlyingResource().getProject();
			if (project.hasNature(JavaCore.NATURE_ID))
				projects.add(project);			
		}

		// add fake "Java Search" project
		SearchablePluginsManager manager = PDECore.getDefault().getModelManager()
				.getSearchablePluginsManager();
		IJavaProject proxy = manager.getProxyProject();
		if (proxy != null) {
			IProject project = proxy.getProject();
			if (project.isOpen())
				projects.add(project);
		}
		return (IProject[]) projects.toArray(new IProject[projects.size()]);
	}

}
