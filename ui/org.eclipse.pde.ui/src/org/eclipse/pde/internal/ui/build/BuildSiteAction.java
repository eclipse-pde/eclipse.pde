/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Created on Oct 6, 2003
 */
package org.eclipse.pde.internal.ui.build;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.*;
import org.eclipse.ui.progress.*;

/**
 * @author melhem
 */
public class BuildSiteAction implements IObjectActionDelegate, IPreferenceConstants {
	
	private ISiteBuildModel fBuildModel;
	private IFile fSiteXML;
	

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction, org.eclipse.ui.IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		if (fBuildModel == null)
			return;
		ISiteBuildFeature[] sbFeatures = fBuildModel.getSiteBuild().getFeatures();
		IFeatureModel[] models = getFeatureModels(sbFeatures);
		
		if (models.length > 0) {
			BuildSiteJob job = new BuildSiteJob(models, fSiteXML.getProject(), fBuildModel);
			job.setUser(true);
			job.schedule();
			job.setProperty(IProgressConstants.ICON_PROPERTY, PDEPluginImages.DESC_SITE_OBJ);
		}		
	}
	
	private IFeatureModel[] getFeatureModels(ISiteBuildFeature[] sbFeatures) {
		ArrayList list = new ArrayList();
		for (int i = 0; i < sbFeatures.length; i++) {
			IFeature feature = sbFeatures[i].getReferencedFeature();
			if (feature == null)
				continue;
			IFeatureModel model = feature.getModel();
			if (model != null && model.getUnderlyingResource() != null)
				list.add(model);
		}	
		return (IFeatureModel[])list.toArray(new IFeatureModel[list.size()]);		
	}
		
	public void selectionChanged(IAction action, ISelection selection) {
		fBuildModel = null;
		if (selection instanceof IStructuredSelection) {
			Object obj = ((IStructuredSelection) selection).getFirstElement();
			if (obj != null && obj instanceof IFile) {
				fSiteXML = (IFile)obj;
				IProject project = fSiteXML.getProject();
				WorkspaceModelManager manager =
					PDECore.getDefault().getWorkspaceModelManager();
				IResource buildFile =
					project.findMember(
						new Path(PDECore.SITEBUILD_DIR).append(
							PDECore.SITEBUILD_PROPERTIES));
				if (buildFile != null && buildFile instanceof IFile) {
					fBuildModel = (ISiteBuildModel) manager.getModel((IFile)buildFile);
					try {
						fBuildModel.load();
					} catch (CoreException e) {
					}
				}
			}
		}
	}
	
}
