/*******************************************************************************
 * Copyright (c) 2005, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.launching.launcher;

import java.util.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.launching.*;
import org.eclipse.pde.launching.IPDELauncherConstants;

public class LaunchPluginValidator {
	public static final int DISPLAY_VALIDATION_ERROR_CODE = 1001;

	private static IPluginModelBase[] getSelectedWorkspacePlugins(ILaunchConfiguration configuration) throws CoreException {

		boolean usedefault = configuration.getAttribute(IPDELauncherConstants.USE_DEFAULT, true);

		IPluginModelBase[] models = PluginRegistry.getWorkspaceModels();

		if (usedefault || models.length == 0)
			return models;

		Map<IPluginModelBase, String> bundles = BundleLauncherHelper.getWorkspaceBundleMap(configuration);
		Collection<IPluginModelBase> result = bundles.keySet();
		return result.toArray(new IPluginModelBase[result.size()]);
	}

	/**
	 * @return all affected projects, independently of their nature
	 */
	public static IProject[] getAffectedProjects(ILaunchConfiguration config) throws CoreException {
		return getAffectedProjects(config, true);
	}

	/**
	 * @param addFeatures {@code true} to add <b>feature</b> projects (if any) too, {@code false} to include only affected <b>Java</b> projects
	 * @return affected Java and feature projects (last one optional)
	 */
	public static IProject[] getAffectedProjects(ILaunchConfiguration config, boolean addFeatures) throws CoreException {
		// if restarting, no need to check projects for errors
		if (config.getAttribute(IPDEConstants.RESTART, false))
			return new IProject[0];
		ArrayList<IProject> projects = new ArrayList<>();
		IPluginModelBase[] models = getSelectedWorkspacePlugins(config);
		for (IPluginModelBase model : models) {
			IProject project = model.getUnderlyingResource().getProject();
			if (project.hasNature(JavaCore.NATURE_ID))
				projects.add(project);
		}

		if (addFeatures) {
			// add workspace feature project too (if any)
			IProject[] allProjects = PDECore.getWorkspace().getRoot().getProjects();
			for (int i = 0; i < allProjects.length; i++) {
				if (WorkspaceModelManager.isFeatureProject(allProjects[i]) && !projects.contains(allProjects[i]))
					projects.add(allProjects[i]);
			}
		}
		// add fake "Java Search" project
		SearchablePluginsManager manager = PDECore.getDefault().getSearchablePluginsManager();
		IJavaProject proxy = manager.getProxyProject();
		if (proxy != null) {
			projects.add(proxy.getProject());
		}
		return projects.toArray(new IProject[projects.size()]);
	}

	public static void runValidationOperation(final LaunchValidationOperation op, IProgressMonitor monitor) throws CoreException {
		op.run(monitor);
		if (op.hasErrors()) {
			String message = NLS.bind(PDEMessages.PluginValidation_error, op.getInput().toString());
			Status status = new Status(IStatus.ERROR, IPDEConstants.PLUGIN_ID, DISPLAY_VALIDATION_ERROR_CODE, message, null);
			IStatusHandler statusHandler = DebugPlugin.getDefault().getStatusHandler(status);
			if (statusHandler == null)
				PDELaunchingPlugin.log(status);
			else
				statusHandler.handleStatus(status, op);
		}
	}

}
