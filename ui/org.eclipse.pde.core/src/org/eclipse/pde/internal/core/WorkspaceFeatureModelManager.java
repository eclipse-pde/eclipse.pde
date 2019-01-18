/*******************************************************************************
 *  Copyright (c) 2006, 2017 IBM Corporation and others.
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

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelProviderEvent;
import org.eclipse.pde.internal.core.feature.WorkspaceFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.project.PDEProject;

public class WorkspaceFeatureModelManager extends WorkspaceModelManager {

	@Override
	protected boolean isInterestingProject(IProject project) {
		return isFeatureProject(project);
	}

	@Override
	protected void createModel(IProject project, boolean notify) {
		IFile featureXml = PDEProject.getFeatureXml(project);
		if (featureXml.exists()) {
			IModel model = new WorkspaceFeatureModel(featureXml);
			loadModel(model, false);
			if (fModels == null) {
				fModels = new LinkedHashMap<>();
			}
			fModels.put(project, model);
			if (notify) {
				addChange(model, IModelProviderEvent.MODELS_ADDED);
			}
		}
	}

	@Override
	protected void handleFileDelta(IResourceDelta delta) {
		IFile file = (IFile) delta.getResource();
		IProject project = file.getProject();
		IFile featureXml = PDEProject.getFeatureXml(project);
		if (file.equals(featureXml)) {
			Object model = getModel(project);
			int kind = delta.getKind();
			if (kind == IResourceDelta.REMOVED && model != null) {
				removeModel(project);
			} else if (kind == IResourceDelta.ADDED || model == null) {
				createModel(file.getProject(), true);
			} else if (kind == IResourceDelta.CHANGED && (IResourceDelta.CONTENT & delta.getFlags()) != 0) {
				loadModel((IFeatureModel) model, true);
				addChange(model, IModelProviderEvent.MODELS_CHANGED);
			}
		}
	}

	@Override
	protected void addListeners() {
		int event = IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.POST_CHANGE;
		PDECore.getWorkspace().addResourceChangeListener(this, event);
	}

	@Override
	protected void removeListeners() {
		PDECore.getWorkspace().removeResourceChangeListener(this);
		super.removeListeners();
	}

	protected IFeatureModel[] getFeatureModels() {
		initialize();
		return fModels.values().toArray(new IFeatureModel[fModels.size()]);
	}

	protected IFeatureModel getFeatureModel(IProject project) {
		return (IFeatureModel) getModel(project);
	}

	public void removeModel(IFeatureModel iFeatureModel) {
		for (Entry<IProject, IModel> entry : fModels.entrySet()) {
			if (entry.getValue() == iFeatureModel) {
				this.removeModel(entry.getKey());
				break;
			}
		}
	}
}
