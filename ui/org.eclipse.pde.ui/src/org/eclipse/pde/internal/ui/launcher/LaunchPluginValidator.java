/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.SearchablePluginsManager;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;
import org.eclipse.swt.widgets.Display;

public class LaunchPluginValidator {

	private static IPluginModelBase[] getSelectedWorkspacePlugins(ILaunchConfiguration configuration) throws CoreException {

		boolean usedefault = configuration.getAttribute(IPDELauncherConstants.USE_DEFAULT, true);
		boolean useFeatures = configuration.getAttribute(IPDELauncherConstants.USEFEATURES, false);

		IPluginModelBase[] models = PluginRegistry.getWorkspaceModels();

		if (usedefault || useFeatures || models.length == 0)
			return models;

		Collection result = null;
		Map bundles = BundleLauncherHelper.getWorkspaceBundleMap(configuration, null, IPDELauncherConstants.SELECTED_WORKSPACE_PLUGINS);
		result = bundles.keySet();
		return (IPluginModelBase[]) result.toArray(new IPluginModelBase[result.size()]);
	}

	/**
	 * 
	 * @param configuration launchConfiguration to get the attribute value
	 * @param attribute launch configuration attribute to containing plug-in information
	 * @return a TreeSet containing IPluginModelBase objects which are represented by the value of the attribute
	 * @throws CoreException
	 */
	public static Set parsePlugins(ILaunchConfiguration configuration, String attribute) throws CoreException {
		HashSet set = new HashSet();
		String ids = configuration.getAttribute(attribute, (String) null);
		if (ids != null) {
			String[] entries = ids.split(","); //$NON-NLS-1$
			Map unmatchedEntries = new HashMap();
			for (int i = 0; i < entries.length; i++) {
				int index = entries[i].indexOf('@');
				if (index < 0) { // if no start levels, assume default
					entries[i] = entries[i].concat("@default:default"); //$NON-NLS-1$
					index = entries[i].indexOf('@');
				}
				String idVersion = entries[i].substring(0, index);
				int versionIndex = entries[i].indexOf(BundleLauncherHelper.VERSION_SEPARATOR);
				String id = (versionIndex > 0) ? idVersion.substring(0, versionIndex) : idVersion;
				String version = (versionIndex > 0) ? idVersion.substring(versionIndex + 1) : null;
				ModelEntry entry = PluginRegistry.findEntry(id);
				if (entry != null) {
					IPluginModelBase matchingModels[] = attribute.equals(IPDELauncherConstants.SELECTED_TARGET_PLUGINS) ? entry.getExternalModels() : entry.getWorkspaceModels();
					for (int j = 0; j < matchingModels.length; j++) {
						if (matchingModels[j].isEnabled()) {
							// TODO Very similar logic to BundleLauncherHelper
							// the logic here is this (see bug 225644)
							// a) if we come across a bundle that has the right version, immediately add it
							// b) if there's no version, add it
							// c) if there's only one instance of that bundle in the list of ids... add it
							if (version == null || matchingModels[j].getPluginBase().getVersion().equals(version)) {
								set.add(matchingModels[j]);
							} else if (matchingModels.length == 1) {
								if (unmatchedEntries.remove(id) == null) {
									unmatchedEntries.put(id, matchingModels[j]);
								}
							}
						}
					}
				}
			}
			set.addAll(unmatchedEntries.values());
		}
		return set;
	}

	public static IProject[] getAffectedProjects(ILaunchConfiguration config) throws CoreException {
		// if restarting, no need to check projects for errors
		if (config.getAttribute(IPDEUIConstants.RESTART, false))
			return new IProject[0];
		ArrayList projects = new ArrayList();
		IPluginModelBase[] models = getSelectedWorkspacePlugins(config);
		for (int i = 0; i < models.length; i++) {
			IProject project = models[i].getUnderlyingResource().getProject();
			if (project.hasNature(JavaCore.NATURE_ID))
				projects.add(project);
		}

		// add fake "Java Search" project
		SearchablePluginsManager manager = PDECore.getDefault().getSearchablePluginsManager();
		IJavaProject proxy = manager.getProxyProject();
		if (proxy != null) {
			projects.add(proxy.getProject());
		}
		return (IProject[]) projects.toArray(new IProject[projects.size()]);
	}

	public static void runValidationOperation(final LaunchValidationOperation op, IProgressMonitor monitor) throws CoreException {
		op.run(monitor);
		if (op.hasErrors()) {
			final int[] result = new int[1];
			final Display display = LauncherUtils.getDisplay();
			display.syncExec(new Runnable() {
				public void run() {
					PluginStatusDialog dialog = new PluginStatusDialog(display.getActiveShell());
					dialog.showCancelButton(true);
					dialog.setInput(op.getInput());
					result[0] = dialog.open();
				}
			});
			if (result[0] == IDialogConstants.CANCEL_ID)
				throw new CoreException(Status.CANCEL_STATUS);
		}
	}

}
