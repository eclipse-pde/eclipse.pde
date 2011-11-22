/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.build;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.feature.FeatureChild;
import org.eclipse.pde.internal.core.ifeature.*;

/**
 * Action to generate an ant build file for a feature that contains the ant targets to build this bundle
 */
public class GenerateFeatureBuildFileAction extends BaseBuildAction {

	private IFeatureModel fFeatureModel;

	protected void makeScripts(IProgressMonitor monitor) throws InvocationTargetException, CoreException {

		IFeatureModel[] models = PDECore.getDefault().getFeatureModelManager().getModels();
		for (int i = 0; i < models.length; i++) {
			if (models[i].getUnderlyingResource() != null) {
				IResource underlying = models[i].getUnderlyingResource();
				if (underlying.equals(fManifestFile) || underlying.getProject().equals(fManifestFile.getProject()))
					fFeatureModel = models[i];
			}
		}

		BuildScriptGenerator generator = new BuildScriptGenerator();
		generator.setBuildingOSGi(true);
		generator.setChildren(true);
		AbstractScriptGenerator.setEmbeddedSource(AbstractScriptGenerator.getDefaultEmbeddedSource());

		String url = ClasspathHelper.getDevEntriesProperties(fManifestFile.getProject().getLocation().addTrailingSeparator().toString() + "dev.properties", false); //$NON-NLS-1$
		generator.setDevEntries(url);
		generator.setWorkingDirectory(fManifestFile.getProject().getLocation().toOSString());
		String configInfo = TargetPlatform.getOS() + ", " + TargetPlatform.getWS() + ", " + TargetPlatform.getOSArch(); //$NON-NLS-1$ //$NON-NLS-2$
		AbstractScriptGenerator.setConfigInfo(configInfo); //This needs to be set before we set the format
		generator.setArchivesFormat(AbstractScriptGenerator.getDefaultConfigInfos() + '-' + IXMLConstants.FORMAT_ANTZIP);
		generator.setElements(new String[] {"feature@" + fFeatureModel.getFeature().getId() + (fFeatureModel.getFeature().getVersion() == null ? "" : ":" + fFeatureModel.getFeature().getVersion())}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		generator.setPluginPath(TargetPlatformHelper.getFeaturePaths());
		generator.setPDEState(TargetPlatformHelper.getState());
		generator.setNextId(TargetPlatformHelper.getPDEState().getNextId());
		generator.setStateExtraData(TargetPlatformHelper.getBundleClasspaths(TargetPlatformHelper.getPDEState()), TargetPlatformHelper.getPatchMap(TargetPlatformHelper.getPDEState()));
		generator.setGenerateAssembleScript(false);
		generator.generate();
	}

	private void refreshLocal(IFeature feature, IProgressMonitor monitor) throws CoreException {
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
			IFeature child = ((FeatureChild) included[i]).getReferencedFeature();
			if (child != null && child != fFeatureModel.getFeature()) {
				IFeatureModel refmodel = child.getModel();
				if (refmodel != null && refmodel.getUnderlyingResource() != null) {
					refmodel.getUnderlyingResource().getProject().refreshLocal(IResource.DEPTH_ONE, monitor);
				}
				refreshLocal(child, monitor);

			}
		}
	}

	protected void refreshLocal(IProgressMonitor monitor) throws CoreException {
		super.refreshLocal(monitor);
		refreshLocal(fFeatureModel.getFeature(), monitor);
	}
}
