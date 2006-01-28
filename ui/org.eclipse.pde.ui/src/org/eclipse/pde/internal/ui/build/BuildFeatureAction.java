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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.build.AbstractScriptGenerator;
import org.eclipse.pde.internal.build.BuildScriptGenerator;
import org.eclipse.pde.internal.build.IXMLConstants;
import org.eclipse.pde.internal.core.ClasspathHelper;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.feature.FeatureChild;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureChild;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;

public class BuildFeatureAction extends BaseBuildAction {
	
	private IFeatureModel fFeatureModel;

	protected void makeScripts(IProgressMonitor monitor)
		throws InvocationTargetException, CoreException {
		
		ArrayList paths = new ArrayList();
		IFeatureModel[] models = PDECore.getDefault().getFeatureModelManager().getModels();
		for (int i = 0; i < models.length; i++) {
			paths.add(models[i].getInstallLocation() + IPath.SEPARATOR + "feature.xml"); //$NON-NLS-1$
			if (models[i].getUnderlyingResource() != null
					&& models[i].getUnderlyingResource().equals(fManifestFile))
				fFeatureModel = models[i];
		}
		
		BuildScriptGenerator generator = new BuildScriptGenerator();
		generator.setBuildingOSGi(true);
		generator.setChildren(true);
		AbstractScriptGenerator.setEmbeddedSource(AbstractScriptGenerator.getDefaultEmbeddedSource());

		String url = ClasspathHelper.getDevEntriesProperties(fManifestFile.getProject().getLocation().addTrailingSeparator().toString() + "dev.properties", false); //$NON-NLS-1$
		generator.setDevEntries(url);
		generator.setWorkingDirectory(fManifestFile.getProject().getLocation().toOSString());
		AbstractScriptGenerator.setConfigInfo(AbstractScriptGenerator.getDefaultConfigInfos()); //This needs to be set before we set the format
		generator.setArchivesFormat(AbstractScriptGenerator.getDefaultConfigInfos() + '-' + IXMLConstants.FORMAT_ANTZIP);
		generator.setElements(new String[] { "feature@" + fFeatureModel.getFeature().getId() + (fFeatureModel.getFeature().getVersion() == null ? "" : ":" + fFeatureModel.getFeature().getVersion()) }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		generator.setPluginPath(TargetPlatform.getFeaturePaths());
		generator.setPDEState(TargetPlatform.getState());
		generator.setNextId(TargetPlatform.getPDEState().getNextId());
		generator.setStateExtraData(TargetPlatform.getBundleClasspaths(TargetPlatform.getPDEState()));
		generator.setGenerateAssembleScript(false);
		generator.generate();	
	}
	
	private void refreshLocal(IFeature feature, IProgressMonitor monitor)
		throws CoreException {
		IFeaturePlugin[] references = feature.getPlugins();
		for (int i = 0; i < references.length; i++) {
			IPluginModelBase refmodel = feature.getReferencedModel(references[i]);
			if (refmodel != null) {
				IResource resource = refmodel.getUnderlyingResource();
				if (resource != null)
					resource.getProject().refreshLocal(IResource.DEPTH_ONE, monitor);
			}
		}
		IFeatureChild[] included = feature.getIncludedFeatures();
		for (int i = 0; i < included.length; i++) {
			IFeature child = ((FeatureChild) included[i])
					.getReferencedFeature();
			if (child != null && child != fFeatureModel.getFeature()) {
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
		refreshLocal(fFeatureModel.getFeature(), monitor);
	}
}
