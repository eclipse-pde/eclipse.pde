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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.SearchablePluginsManager;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;
import org.eclipse.swt.widgets.Display;

public class LaunchPluginValidator {
	
	public static void checkBackwardCompatibility(ILaunchConfiguration configuration, boolean save) throws CoreException {		
		ILaunchConfigurationWorkingCopy wc = null;
		if (configuration.isWorkingCopy()) {
			wc = (ILaunchConfigurationWorkingCopy) configuration;
		} else {
			wc = configuration.getWorkingCopy();
		}
		
		String value = configuration.getAttribute("wsproject", (String)null);
		if (value != null) {
			wc.setAttribute("wsproject", (String)null);
			if (value.indexOf(';') != -1) {
				value = value.replace(';', ',');
			} else if (value.indexOf(':') != -1) {
				value = value.replace(':', ',');
			}
			value = (value.length() == 0 || value.equals(","))
						? null : value.substring(0, value.length() - 1);
			
			boolean automatic = configuration.getAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true);
			String attr = automatic 
							? IPDELauncherConstants.DESELECTED_WORKSPACE_PLUGINS
							: IPDELauncherConstants.SELECTED_WORKSPACE_PLUGINS;
			wc.setAttribute(attr, value);
		}

		String value2 = configuration.getAttribute("extplugins", (String)null);
		if (value2 != null) {
			wc.setAttribute("extplugins", (String)null);
			if (value2.indexOf(';') != -1)
				value2 = value2.replace(';', ',');	
			else if (value2.indexOf(':') != -1)
				value2 = value2.replace(':', ',');
			value2 = (value2.length() == 0 || value2.equals(","))
						? null : value2.substring(0, value2.length() - 1);
			wc.setAttribute(IPDELauncherConstants.SELECTED_TARGET_PLUGINS, value2);
		}
		
		if (save && (value != null || value2 != null))
			wc.doSave();
	}
	
	public static IPluginModelBase[] getSelectedWorkspacePlugins(ILaunchConfiguration configuration)
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
			StringTokenizer tok = new StringTokenizer(ids, ",");
			while (tok.hasMoreTokens())
				set.add(tok.nextToken());
		}
		return set;
	}

	public static TreeMap getPluginsToRun(ILaunchConfiguration config)
			throws CoreException {
		TreeMap map = null;
		ArrayList statusEntries = new ArrayList();

		if (config.getAttribute(IPDELauncherConstants.USE_DEFAULT, true)) {
			map = validatePlugins(PDECore.getDefault().getModelManager().getPlugins(),
					statusEntries);
		} else {
			map = validatePlugins(getSelectedPlugins(config), statusEntries);
		}

		final String requiredPlugin;
		if (PDECore.getDefault().getModelManager().isOSGiRuntime())
			requiredPlugin = "org.eclipse.osgi"; //$NON-NLS-1$
		else
			requiredPlugin = "org.eclipse.core.boot"; //$NON-NLS-1$

		if (!map.containsKey(requiredPlugin)) {
			final Display display = getDisplay();
			display.syncExec(new Runnable() {
				public void run() {
					MessageDialog.openError(
									display.getActiveShell(),
									PDEUIMessages.WorkbenchLauncherConfigurationDelegate_title,
									NLS.bind(PDEUIMessages.WorkbenchLauncherConfigurationDelegate_missingRequired,
											requiredPlugin));
				}
			});
			return null;
		}

		// alert user if any plug-ins are not loaded correctly.
		if (statusEntries.size() > 0) {
			final MultiStatus multiStatus = new MultiStatus(PDEPlugin.getPluginId(),
					IStatus.OK, (IStatus[]) statusEntries
							.toArray(new IStatus[statusEntries.size()]),
					PDEUIMessages.WorkbenchLauncherConfigurationDelegate_brokenPlugins,
					null);
			if (!ignoreValidationErrors(multiStatus)) {
				return null;
			}
		}
		return map;
	}

	public static IPluginModelBase[] getSelectedPlugins(ILaunchConfiguration config)
			throws CoreException {
		Map map = getSelectedPluginMap(config);
		return (IPluginModelBase[]) map.values().toArray(new IPluginModelBase[map.size()]);
	}
	
	public static Map getSelectedPluginMap(ILaunchConfiguration config)
			throws CoreException {

		checkBackwardCompatibility(config, true);
				
		TreeMap map = new TreeMap();
		IPluginModelBase[] wsmodels = getSelectedWorkspacePlugins(config);
		for (int i = 0; i < wsmodels.length; i++) {
			map.put(wsmodels[i].getPluginBase().getId(), wsmodels[i]);
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

	private static TreeMap validatePlugins(IPluginModelBase[] models,
			ArrayList statusEntries) {
		TreeMap map = new TreeMap();
		for (int i = 0; i < models.length; i++) {
			if (models[i].isLoaded()) {
				map.put(models[i].getPluginBase().getId(), models[i]);								
			} else {
				statusEntries.add(new Status(IStatus.WARNING, 
						PDEPlugin.getPluginId(), 
						IStatus.OK, 
						models[i].getPluginBase().getId(), 
						null));
			}
		}
		return map;
	}

	private static boolean ignoreValidationErrors(final MultiStatus status) {
		final boolean[] result = new boolean[1];
		getDisplay().syncExec(new Runnable() {
			public void run() {
				result[0] = MessageDialog.openConfirm(getDisplay().getActiveShell(),
						PDEUIMessages.WorkbenchLauncherConfigurationDelegate_title,
						status.getMessage());
			}
		});
		return result[0];
	}
	
	private static Display getDisplay() {
		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}
		return display;
	}


}
