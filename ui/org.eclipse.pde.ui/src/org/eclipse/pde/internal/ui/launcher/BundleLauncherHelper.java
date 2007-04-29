/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;

public class BundleLauncherHelper {

	public static Map getWorkspaceBundleMap(ILaunchConfiguration configuration) throws CoreException {
		return getWorkspaceBundleMap(configuration, null);
	}
	
	public static Map getWorkspaceBundleMap(ILaunchConfiguration configuration, Set set) throws CoreException {
		String selected = configuration.getAttribute(IPDELauncherConstants.WORKSPACE_BUNDLES, ""); //$NON-NLS-1$
		Map map = new HashMap();
		StringTokenizer tok = new StringTokenizer(selected, ","); //$NON-NLS-1$
		while (tok.hasMoreTokens()) {
			String token = tok.nextToken();
			int index = token.indexOf('@');
			String id = token.substring(0, index);
			if (set != null)
				set.add(id);
			ModelEntry entry = PluginRegistry.findEntry(id);
			if (entry != null) {
				IPluginModelBase[] models = entry.getWorkspaceModels();
				for (int i = 0; i < models.length; i++) {
					map.put(models[i], token.substring(index + 1));
				}
			}
		}
		
		if (configuration.getAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true)) {
			Set deselectedPlugins = LaunchPluginValidator.parsePlugins(configuration, IPDELauncherConstants.DESELECTED_WORKSPACE_PLUGINS);
			IPluginModelBase[] models = PluginRegistry.getWorkspaceModels();
			for (int i = 0; i < models.length; i++) {
				String id = models[i].getPluginBase().getId();
				if (id == null)
					continue;
				if (set != null)
					set.add(id);
				if (!map.containsKey(models[i]) && !deselectedPlugins.contains(id)) {
					map.put(models[i], "default:default"); //$NON-NLS-1$
				}
			}
		}
		return map;
	}
	
	public static IPluginModelBase[] getWorkspaceBundles(ILaunchConfiguration configuration) throws CoreException {
		Map map = getWorkspaceBundleMap(configuration);
		return (IPluginModelBase[])map.keySet().toArray(new IPluginModelBase[map.size()]);
	}
	
	public static Map getTargetBundleMap(ILaunchConfiguration configuration) throws CoreException {
		return getTargetBundleMap(configuration, new HashSet());
	}
	
	public static Map getTargetBundleMap(ILaunchConfiguration configuration, Set set) throws CoreException {
		String selected = configuration.getAttribute(IPDELauncherConstants.TARGET_BUNDLES, ""); //$NON-NLS-1$
		Map map = new HashMap();
		StringTokenizer tok = new StringTokenizer(selected, ","); //$NON-NLS-1$
		while (tok.hasMoreTokens()) {
			String token = tok.nextToken();
			int index = token.indexOf('@');
			String id = token.substring(0, index);
			if (set.contains(id))
				continue;
			ModelEntry entry = PluginRegistry.findEntry(id);
			if (entry != null) {
				IPluginModelBase[] models = entry.getExternalModels();
				for (int i = 0; i < models.length; i++) {
					map.put(models[i], token.substring(index + 1));
				}
			}
		}
		return map;
	}
	
	public static IPluginModelBase[] getTargetBundles(ILaunchConfiguration configuration) throws CoreException {
		Map map = getTargetBundleMap(configuration);
		return (IPluginModelBase[])map.keySet().toArray(new IPluginModelBase[map.size()]);
	}

	public static Map getMergedMap(ILaunchConfiguration configuration) throws CoreException {
		Set set = new HashSet();
		Map map = getWorkspaceBundleMap(configuration, set);
		map.putAll(getTargetBundleMap(configuration, set));		
		return map;
	}
	
	public static IPluginModelBase[] getMergedBundles(ILaunchConfiguration configuration) throws CoreException {
		Map map = getMergedMap(configuration);
		return (IPluginModelBase[])map.keySet().toArray(new IPluginModelBase[map.size()]);
	}

}
