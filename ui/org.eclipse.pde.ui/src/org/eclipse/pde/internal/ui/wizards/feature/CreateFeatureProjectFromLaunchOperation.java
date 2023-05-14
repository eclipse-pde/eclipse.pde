/*******************************************************************************
 * Copyright (c) 2007, 2022 IBM Corporation and others.
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
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.feature;

import java.util.Collections;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.feature.WorkspaceFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.ui.launcher.EclipseLaunchShortcut;
import org.eclipse.swt.widgets.Shell;

public class CreateFeatureProjectFromLaunchOperation extends CreateFeatureProjectOperation {

	private ILaunchConfiguration fLaunchConfig;

	public CreateFeatureProjectFromLaunchOperation(IProject project, IPath location, FeatureData featureData, ILaunchConfiguration launchConfig, Shell shell) {
		super(project, location, featureData, null, shell);
		fLaunchConfig = launchConfig;
	}

	@Override
	protected void configureFeature(IFeature feature, WorkspaceFeatureModel model) throws CoreException {
		fPlugins = getPlugins();
		super.configureFeature(feature, model);
	}

	private IPluginBase[] getPlugins() {
		Set<IPluginModelBase> models = Collections.emptySet();
		try {
			ILaunchConfigurationType type = fLaunchConfig.getType();
			String id = type.getIdentifier();
			// if it is an Eclipse launch
			if (id.equals(EclipseLaunchShortcut.CONFIGURATION_TYPE)) {
				models = BundleLauncherHelper.getMergedBundleMap(fLaunchConfig, false).keySet();
			} else if (id.equals(IPDELauncherConstants.OSGI_CONFIGURATION_TYPE)) {
				// else if it is an OSGi launch
				models = BundleLauncherHelper.getMergedBundleMap(fLaunchConfig, true).keySet();
			}
		} catch (CoreException e) {
		}
		return models.stream().map(IPluginModelBase::getPluginBase).toArray(IPluginBase[]::new);
	}

}
