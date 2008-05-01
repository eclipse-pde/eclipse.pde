/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;

public class BundleLauncherHelper {

	public static final char VERSION_SEPARATOR = '*';

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
			String idVersion = token.substring(0, index);
			int versionIndex = idVersion.indexOf(VERSION_SEPARATOR);
			String id = (versionIndex > 0) ? idVersion.substring(0, versionIndex) : idVersion;
			String version = (versionIndex > 0) ? idVersion.substring(versionIndex + 1) : null;
			if (set != null)
				set.add(id);
			ModelEntry entry = PluginRegistry.findEntry(id);
			if (entry != null) {
				IPluginModelBase[] models = entry.getWorkspaceModels();
				for (int i = 0; i < models.length; i++) {
					IPluginBase base = models[i].getPluginBase();
					if (base.getVersion().equals(version) || version == null)
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
				if (!deselectedPlugins.contains(models[i])) {
					if (set != null)
						set.add(id);
					if (!map.containsKey(models[i])) {
						map.put(models[i], "default:default"); //$NON-NLS-1$
					}
				}
			}
		}
		return map;
	}

	public static IPluginModelBase[] getWorkspaceBundles(ILaunchConfiguration configuration) throws CoreException {
		Map map = getWorkspaceBundleMap(configuration);
		return (IPluginModelBase[]) map.keySet().toArray(new IPluginModelBase[map.size()]);
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
			String idVersion = token.substring(0, index);
			int versionIndex = idVersion.indexOf(VERSION_SEPARATOR);
			String id = (versionIndex > 0) ? idVersion.substring(0, versionIndex) : idVersion;
			String version = (versionIndex > 0) ? idVersion.substring(versionIndex + 1) : null;
			if (set.contains(id))
				continue;
			ModelEntry entry = PluginRegistry.findEntry(id);
			if (entry != null) {
				IPluginModelBase[] models = entry.getExternalModels();
				for (int i = 0; i < models.length; i++) {
					IPluginBase base = models[i].getPluginBase();
					if (base.getVersion().equals(version) || version == null)
						map.put(models[i], token.substring(index + 1));
				}
			}
		}
		return map;
	}

	public static IPluginModelBase[] getTargetBundles(ILaunchConfiguration configuration) throws CoreException {
		Map map = getTargetBundleMap(configuration);
		return (IPluginModelBase[]) map.keySet().toArray(new IPluginModelBase[map.size()]);
	}

	public static Map getMergedMap(ILaunchConfiguration configuration) throws CoreException {
		Set set = new HashSet();
		Map map = getWorkspaceBundleMap(configuration, set);
		map.putAll(getTargetBundleMap(configuration, set));
		return map;
	}

	public static IPluginModelBase[] getMergedBundles(ILaunchConfiguration configuration) throws CoreException {
		Map map = getMergedMap(configuration);
		return (IPluginModelBase[]) map.keySet().toArray(new IPluginModelBase[map.size()]);
	}

	public static String writeBundles(IPluginModelBase model, String startLevel, String autoStart) {
		IPluginBase base = model.getPluginBase();
		String id = base.getId();
		StringBuffer buffer = new StringBuffer(id);

		ModelEntry entry = PluginRegistry.findEntry(id);
		if (entry.getActiveModels().length > 1) {
			buffer.append(VERSION_SEPARATOR);
			buffer.append(model.getPluginBase().getVersion());
		}

		if (startLevel != null || autoStart != null)
			buffer.append('@');
		if (startLevel != null)
			buffer.append(startLevel);
		if (startLevel != null && autoStart != null)
			buffer.append(':');
		if (autoStart != null)
			buffer.append(autoStart);
		return buffer.toString();
	}

}
