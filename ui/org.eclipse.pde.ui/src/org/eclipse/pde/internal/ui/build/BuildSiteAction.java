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

public class BuildSiteAction implements IObjectActionDelegate,
		IPreferenceConstants {

	private ISiteModel fModel;

	private IFile fSiteXML;
	
	private IWorkbenchPart fTargetPart;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction,
	 *      org.eclipse.ui.IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		fTargetPart = targetPart;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		if (fModel == null)
			return;
		ISiteFeature[] sbFeatures = fModel.getSite().getFeatures();
		sbFeatures[0].getModel().getSite().getFeatures();
		IFeatureModel[] models = getFeatureModels(sbFeatures);

		if (models.length > 0) {
			BuildSiteJob job = new BuildSiteJob(fTargetPart.getSite().getShell().getDisplay(), models, fModel);
			job.setUser(true);
			job.schedule();
			job.setProperty(IProgressConstants.ICON_PROPERTY,
					PDEPluginImages.DESC_SITE_OBJ);
		}
	}

	private IFeatureModel[] getFeatureModels(ISiteFeature[] sFeatures) {
		ArrayList list = new ArrayList();
		for (int i = 0; i < sFeatures.length; i++) {
			ISiteFeature siteFeature = sFeatures[i];
			IFeatureModel model = PDECore.getDefault().getFeatureModelManager()
					.findFeatureModelRelaxed(siteFeature.getId(),
							siteFeature.getVersion());
			if (model != null && model.getUnderlyingResource() != null)
				list.add(model);
		}
		return (IFeatureModel[]) list.toArray(new IFeatureModel[list.size()]);
	}

	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			Object obj = ((IStructuredSelection) selection).getFirstElement();
			if (obj != null && obj instanceof IFile) {
				fSiteXML = (IFile) obj;
				WorkspaceModelManager manager = PDECore.getDefault()
						.getWorkspaceModelManager();
				fModel = (ISiteModel) manager.getModel(fSiteXML);
				try {
					fModel.load();
				} catch (CoreException e) {
				}
			}
		}
	}

}
