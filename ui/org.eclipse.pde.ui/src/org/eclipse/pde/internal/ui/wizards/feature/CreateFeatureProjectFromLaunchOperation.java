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
 
 package org.eclipse.pde.internal.ui.wizards.feature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.feature.WorkspaceFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.ui.launcher.LaunchPluginValidator;
import org.eclipse.swt.widgets.Shell;

public class CreateFeatureProjectFromLaunchOperation extends
		CreateFeatureProjectOperation {
	
	private ILaunchConfiguration fLaunchConfig;
	
	public CreateFeatureProjectFromLaunchOperation(IProject project, IPath location,
			FeatureData featureData, ILaunchConfiguration launchConfig, Shell shell) {
		super(project, location, featureData, null, shell);
		fLaunchConfig = launchConfig;
	}
	
	protected void configureFeature(IFeature feature,
			WorkspaceFeatureModel model) throws CoreException {
		fPlugins = getPlugins();
		super.configureFeature(feature, model);
	}
	
	private IPluginBase[] getPlugins() {
		IPluginModelBase[] models = null;
		try {
			models = LaunchPluginValidator.getPluginList(fLaunchConfig);
		} catch (CoreException e) {
		}
		IPluginBase[] result = new IPluginBase[models == null ? 0 : models.length];
		for (int i = 0; i < result.length; i++) 
			result[i] = models[i].getPluginBase(true);
		return result;
	}

}
