/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;

public class BundleLauncherHelper {

	public static final char VERSION_SEPARATOR = '*';

	public static Map getWorkspaceBundleMap(ILaunchConfiguration configuration) throws CoreException {
		return getWorkspaceBundleMap(configuration, null, IPDELauncherConstants.WORKSPACE_BUNDLES);
	}

	public static Map getTargetBundleMap(ILaunchConfiguration configuration) throws CoreException {
		return getTargetBundleMap(configuration, null, IPDELauncherConstants.TARGET_BUNDLES);
	}

	public static Map getMergedBundleMap(ILaunchConfiguration configuration) throws CoreException {
		Set set = new HashSet();
		Map map = getWorkspaceBundleMap(configuration, set, IPDELauncherConstants.WORKSPACE_BUNDLES);
		map.putAll(getTargetBundleMap(configuration, set, IPDELauncherConstants.TARGET_BUNDLES));
		return map;
	}

	public static IPluginModelBase[] getMergedBundles(ILaunchConfiguration configuration) throws CoreException {
		Map map = getMergedBundleMap(configuration);
		return (IPluginModelBase[]) map.keySet().toArray(new IPluginModelBase[map.size()]);
	}

	public static Map getWorkspaceBundleMap(ILaunchConfiguration configuration, Set set, String attribute) throws CoreException {
		String selected = configuration.getAttribute(attribute, ""); //$NON-NLS-1$
		Map map = new HashMap();
		StringTokenizer tok = new StringTokenizer(selected, ","); //$NON-NLS-1$
		while (tok.hasMoreTokens()) {
			String token = tok.nextToken();
			int index = token.indexOf('@');
			if (index < 0) { // if no start levels, assume default
				token = token.concat("@default:default"); //$NON-NLS-1$
				index = token.indexOf('@');
			}
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

					// TODO Very similar to logic in LaunchPluginValidator
					// match only if...
					// a) if we have the same version
					// b) no version
					// c) all else fails, if there's just one bundle available, use it
					if (base.getVersion().equals(version) || version == null || models.length == 1)
						addBundleToMap(map, models[i], token.substring(index + 1));
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
					if (!map.containsKey(models[i]))
						addBundleToMap(map, models[i], "default:default"); //$NON-NLS-1$
				}
			}
		}
		return map;
	}

	/**
	 * Adds the given bundle and start information to the map.  This will override anything set
	 * for system bundles, and set their start level to the appropriate level
	 * @param map The map to add the bundles too
	 * @param bundle The bundle to add
	 * @param substring the start information in the form level:autostart
	 */
	private static void addBundleToMap(Map map, IPluginModelBase bundle, String sl) {
		BundleDescription desc = bundle.getBundleDescription();
		boolean defaultsl = (sl == null || sl.equals("default:default")); //$NON-NLS-1$
		if (desc != null && defaultsl) {
			String modelName = desc.getSymbolicName();
			if (IPDEBuildConstants.BUNDLE_DS.equals(modelName)) {
				map.put(bundle, "1:true"); //$NON-NLS-1$ 
			} else if (IPDEBuildConstants.BUNDLE_SIMPLE_CONFIGURATOR.equals(modelName)) {
				map.put(bundle, "1:true"); //$NON-NLS-1$
			} else if (IPDEBuildConstants.BUNDLE_EQUINOX_COMMON.equals(modelName)) {
				map.put(bundle, "2:true"); //$NON-NLS-1$
			} else if (IPDEBuildConstants.BUNDLE_OSGI.equals(modelName)) {
				map.put(bundle, "-1:true"); //$NON-NLS-1$
			} else if (IPDEBuildConstants.BUNDLE_UPDATE_CONFIGURATOR.equals(modelName)) {
				map.put(bundle, "3:true"); //$NON-NLS-1$
			} else if (IPDEBuildConstants.BUNDLE_CORE_RUNTIME.equals(modelName)) {
				if (TargetPlatformHelper.getTargetVersion() > 3.1) {
					map.put(bundle, "default:true"); //$NON-NLS-1$
				} else {
					map.put(bundle, "2:true"); //$NON-NLS-1$
				}
			} else {
				map.put(bundle, sl);
			}
		} else {
			map.put(bundle, sl);
		}

	}

	public static Map getTargetBundleMap(ILaunchConfiguration configuration, Set set, String attribute) throws CoreException {
		String selected = configuration.getAttribute(attribute, ""); //$NON-NLS-1$
		Map map = new HashMap();
		StringTokenizer tok = new StringTokenizer(selected, ","); //$NON-NLS-1$
		while (tok.hasMoreTokens()) {
			String token = tok.nextToken();
			int index = token.indexOf('@');
			if (index < 0) { // if no start levels, assume default
				token = token.concat("@default:default"); //$NON-NLS-1$
				index = token.indexOf('@');
			}
			String idVersion = token.substring(0, index);
			int versionIndex = idVersion.indexOf(VERSION_SEPARATOR);
			String id = (versionIndex > 0) ? idVersion.substring(0, versionIndex) : idVersion;
			String version = (versionIndex > 0) ? idVersion.substring(versionIndex + 1) : null;
			if (set != null && set.contains(id))
				continue;
			ModelEntry entry = PluginRegistry.findEntry(id);
			if (entry != null) {
				IPluginModelBase[] models = entry.getExternalModels();
				for (int i = 0; i < models.length; i++) {
					if (models[i].isEnabled()) {
						IPluginBase base = models[i].getPluginBase();
						// match only if...
						// a) if we have the same version
						// b) no version
						// c) all else fails, if there's just one bundle available, use it
						if (base.getVersion().equals(version) || version == null || models.length == 1)
							addBundleToMap(map, models[i], token.substring(index + 1));
					}
				}
			}
		}
		return map;
	}

	public static String writeBundleEntry(IPluginModelBase model, String startLevel, String autoStart) {
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
