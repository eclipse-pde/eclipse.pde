/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.build;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.build.AbstractScriptGenerator;
import org.eclipse.pde.internal.build.builder.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.feature.FeatureChild;
import org.eclipse.pde.internal.core.ifeature.*;

public class BuildFeatureAction extends BaseBuildAction {
	
	private IFeatureModel model;

	protected void makeScripts(IProgressMonitor monitor)
		throws InvocationTargetException, CoreException {
		ArrayList paths = new ArrayList();
		IFeatureModel[] models = PDECore.getDefault().getFeatureModelManager().getModels();
		for (int i = 0; i < models.length; i++) {
			paths.add(models[i].getInstallLocation() + IPath.SEPARATOR + "feature.xml"); //$NON-NLS-1$
			if (models[i].getUnderlyingResource() != null
					&& models[i].getUnderlyingResource().equals(fManifestFile))
				model = models[i];
		}
		
		String[] plugins = TargetPlatform.createPluginPath();
		String[] features = (String[]) paths.toArray(new String[paths.size()]);
		String[] all = new String[plugins.length + paths.size()];
		System.arraycopy(plugins, 0, all, 0, plugins.length);
		System.arraycopy(features, 0, all, plugins.length, features.length);
		
		BuildScriptGenerator generator = new BuildScriptGenerator();
		generator.setBuildingOSGi(PDECore.getDefault().getModelManager().isOSGiRuntime());
		generator.setChildren(true);
		AbstractScriptGenerator.setEmbeddedSource(AbstractScriptGenerator.getDefaultEmbeddedSource());
		AbstractBuildScriptGenerator.setBrandExecutable(false);

		String url = ClasspathHelper.getDevEntriesProperties(fManifestFile.getProject().getLocation().addTrailingSeparator().toString() + "dev.properties", false); //$NON-NLS-1$
		generator.setDevEntries(url);
		generator.setWorkingDirectory(fManifestFile.getProject().getLocation().toOSString());
		AbstractScriptGenerator.setOutputFormat(AbstractScriptGenerator.getDefaultOutputFormat());
		AbstractScriptGenerator.setConfigInfo(AbstractScriptGenerator.getDefaultConfigInfos());
		generator.setElements(new String[] {"feature@" + model.getFeature().getId()});	 //$NON-NLS-1$
		generator.setPluginPath(all);
		generator.setGenerateAssembleScript(false);
		generator.generate();	
	}
	
	private void refreshLocal(IFeature feature, IProgressMonitor monitor)
		throws CoreException {
		IFeaturePlugin[] references = feature.getPlugins();
		for (int i = 0; i < references.length; i++) {
			IPluginModelBase refmodel = feature.getReferencedModel(references[i]);
			if (refmodel != null) {
				refmodel.getUnderlyingResource().getProject().refreshLocal(
					IResource.DEPTH_ONE,
					monitor);
			}
		}
		IFeatureChild[] included = feature.getIncludedFeatures();
		for (int i = 0; i < included.length; i++) {
			IFeature child = ((FeatureChild) included[i])
					.getReferencedFeature();
			if (child != null && child != model.getFeature()) {
				IFeatureModel refmodel = child.getModel();
				if (refmodel != null) {
					refmodel.getUnderlyingResource().getProject().refreshLocal(
							IResource.DEPTH_ONE, monitor);
				}
				refreshLocal(child, monitor);

			}
		}
	}
	
	protected void refreshLocal(IProgressMonitor monitor)
		throws CoreException {
		super.refreshLocal(monitor);
		refreshLocal(model.getFeature(), monitor);
	}
}
