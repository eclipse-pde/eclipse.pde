/*******************************************************************************
 * Copyright (c) 2003, 2015 IBM Corporation and others.
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
/*
 * Created on Oct 6, 2003
 */
package org.eclipse.pde.internal.ui.build;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.exports.SiteBuildOperation;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.isite.ISiteFeature;
import org.eclipse.pde.internal.core.isite.ISiteModel;
import org.eclipse.pde.internal.core.site.WorkspaceSiteModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.site.SiteEditor;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressConstants;

/**
 * Generates an ant build file for an update site
 *
 */
public class BuildSiteAction implements IObjectActionDelegate {

	private ISiteModel fModel;

	private IFile fSiteXML;

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	@Override
	public void run(IAction action) {
		if (fModel == null)
			return;
		ISiteFeature[] sbFeatures = fModel.getSite().getFeatures();
		IFeatureModel[] models = getFeatureModels(sbFeatures);

		if (models.length > 0) {
			try {
				ensureContentSaved();
				fModel.load();
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
			Job job = new SiteBuildOperation(models, fModel, PDEUIMessages.BuildSiteJob_name);
			job.setUser(true);
			job.schedule();
			job.setProperty(IProgressConstants.ICON_PROPERTY, PDEPluginImages.DESC_SITE_OBJ);
		}
	}

	private IFeatureModel[] getFeatureModels(ISiteFeature[] sFeatures) {
		ArrayList<IFeatureModel> list = new ArrayList<>();
		for (ISiteFeature siteFeature : sFeatures) {
			IFeatureModel model = PDECore.getDefault().getFeatureModelManager().findFeatureModelRelaxed(siteFeature.getId(), siteFeature.getVersion());
			if (model != null)
				list.add(model);
		}
		return list.toArray(new IFeatureModel[list.size()]);
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			Object obj = ((IStructuredSelection) selection).getFirstElement();
			if (obj != null && obj instanceof IFile) {
				fSiteXML = (IFile) obj;
				fModel = new WorkspaceSiteModel(fSiteXML);
				try {
					fModel.load();
					ISiteFeature[] features = fModel.getSite().getFeatures();
					if (features.length <= 0)
						action.setEnabled(false);
				} catch (CoreException e) {
					action.setEnabled(false);
				}
			}
		}
	}

	private void ensureContentSaved() {
		if (fModel != null && fModel.getUnderlyingResource() != null) {
			IProject project = fModel.getUnderlyingResource().getProject();
			final SiteEditor editor = PDEModelUtility.getOpenUpdateSiteEditor(project);
			if (editor != null && editor.isDirty()) {
				try {
					IRunnableWithProgress op = monitor -> editor.doSave(monitor);
					PlatformUI.getWorkbench().getProgressService().runInUI(PDEPlugin.getActiveWorkbenchWindow(), op, PDEPlugin.getWorkspace().getRoot());
				} catch (InvocationTargetException e) {
					PDEPlugin.logException(e);
				} catch (InterruptedException e) {
				}
			}
		}
	}

}
