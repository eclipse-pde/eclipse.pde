/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.feature;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.build.builder.FeatureBuildScriptGenerator;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class BuildFeatureAction extends BaseBuildAction {
	
	private IFeatureModel model;

	protected void makeScripts(IProgressMonitor monitor)
		throws InvocationTargetException, CoreException {

		FeatureBuildScriptGenerator generator = new FeatureBuildScriptGenerator();
		
		String location = file.getProject().getLocation().toOSString();
		generator.setWorkingDirectory(location);
		generator.setFeatureRootLocation(location);
		generator.setDevEntries(new String[] { "bin" }); // FIXME: look at bug #5747
		
		ArrayList paths = new ArrayList();
		IFeatureModel[] models = PDECore.getDefault().getWorkspaceModelManager().getWorkspaceFeatureModels();
		for (int i = 0; i < models.length; i++) {
			try {
				paths.add(new URL("file:" + models[i].getInstallLocation() + Path.SEPARATOR + "feature.xml"));
			} catch (MalformedURLException e1) {
			}
			if (models[i].getUnderlyingResource().equals(file))
				model = models[i];
		}
		
		URL[] plugins = TargetPlatform.createPluginPath();
		URL[] features = (URL[]) paths.toArray(new URL[paths.size()]);
		URL[] all = new URL[plugins.length + paths.size()];
		System.arraycopy(plugins, 0, all, 0, plugins.length);
		System.arraycopy(features, 0, all, plugins.length, features.length);
		
		generator.setPluginPath(all);

		try {
			generator.setFeature(model.getFeature().getId());
			generator.generate();
		} catch (Exception e) {
			PDEPlugin.logException(e);
		}
	}

	private void refreshLocal(IFeature feature, IProgressMonitor monitor)
		throws CoreException {
		IFeaturePlugin[] references = feature.getPlugins();
		for (int i = 0; i < references.length; i++) {
			IPluginModelBase refmodel = feature.getReferencedModel(references[i]);
			if (refmodel != null) {
				refmodel.getUnderlyingResource().getProject().refreshLocal(
					IResource.DEPTH_INFINITE,
					monitor);
			}
		}
	}
	
	protected void refreshLocal(IProgressMonitor monitor)
		throws CoreException {
		super.refreshLocal(monitor);
		refreshLocal(model.getFeature(), monitor);
	}
	

}
