/*******************************************************************************
 *  Copyright (c) 2006, 2022 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.Map.Entry;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.pde.core.IModelProviderEvent;
import org.eclipse.pde.internal.core.feature.WorkspaceFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.project.PDEProject;

public class WorkspaceFeatureModelManager extends WorkspaceModelManager<IFeatureModel> {

	@Override
	protected boolean isInterestingProject(IProject project) {
		return isFeatureProject(project);
	}

	@Override
	protected void createModel(IProject project, boolean notify) {
		IFile featureXml = PDEProject.getFeatureXml(project);
		if (featureXml.exists()) {
			IFeatureModel model = new WorkspaceFeatureModel(featureXml);
			loadModel(model, false);
			getModelsMap().put(project, model);
			if (notify) {
				addChange(model, IModelProviderEvent.MODELS_ADDED);
			}
		}
	}

	@Override
	protected IFeatureModel removeModel(IProject project) {
		IFeatureModel model = getModelsMap().remove(project);
		addChange(model, IModelProviderEvent.MODELS_REMOVED);
		return model;
	}

	@Override
	protected void handleFileDelta(IResourceDelta delta) {
		IFile file = (IFile) delta.getResource();
		IProject project = file.getProject();
		IFile featureXml = PDEProject.getFeatureXml(project);
		if (file.equals(featureXml)) {
			IFeatureModel model = getModel(project);
			int kind = delta.getKind();
			if (kind == IResourceDelta.REMOVED && model != null) {
				removeModel(project);
			} else if (kind == IResourceDelta.ADDED || model == null) {
				createModel(file.getProject(), true);
			} else if (kind == IResourceDelta.CHANGED && (IResourceDelta.CONTENT & delta.getFlags()) != 0) {
				loadModel(model, true);
				addChange(model, IModelProviderEvent.MODELS_CHANGED);
			}
		}
	}

	protected IFeatureModel[] getFeatureModels() {
		initialize();
		return getModelsMap().values().toArray(new IFeatureModel[getModelsMap().size()]);
	}

	public void removeModel(IFeatureModel iFeatureModel) {
		for (Entry<IProject, IFeatureModel> entry : getModelsMap().entrySet()) {
			if (entry.getValue() == iFeatureModel) {
				this.removeModel(entry.getKey());
				break;
			}
		}
	}
}
