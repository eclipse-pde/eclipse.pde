/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.pde.core.IModelProviderEvent;
import org.eclipse.pde.internal.core.feature.WorkspaceFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;

public class WorkspaceFeatureModelManager extends WorkspaceModelManager {

	protected boolean isInterestingProject(IProject project) {
		return isFeatureProject(project);
	}

	protected void createModel(IProject project, boolean notify) {
		if (project.exists(ICoreConstants.FEATURE_PATH)) {
			WorkspaceFeatureModel model = new WorkspaceFeatureModel(project.getFile(ICoreConstants.FEATURE_PATH));
			loadModel(model, false);
			if (fModels == null)
				fModels = new HashMap();
			fModels.put(project, model);
			if (notify)
				addChange(model, IModelProviderEvent.MODELS_ADDED);
		}
	}

	protected void handleFileDelta(IResourceDelta delta) {
		IFile file = (IFile) delta.getResource();
		if (file.getProjectRelativePath().equals(ICoreConstants.FEATURE_PATH)) {
			IProject project = file.getProject();
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

	protected void addListeners() {
		int event = IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.POST_CHANGE;
		PDECore.getWorkspace().addResourceChangeListener(this, event);
	}

	protected void removeListeners() {
		PDECore.getWorkspace().removeResourceChangeListener(this);
		super.removeListeners();
	}

	protected IFeatureModel[] getFeatureModels() {
		initialize();
		return (IFeatureModel[]) fModels.values().toArray(new IFeatureModel[fModels.size()]);
	}

	protected IFeatureModel getFeatureModel(IProject project) {
		return (IFeatureModel) getModel(project);
	}

}
