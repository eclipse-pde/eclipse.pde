/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.pde.core.IModelProviderEvent;
import org.eclipse.pde.core.IModelProviderListener;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;

public class FeatureModelManager {
	private ExternalModelManager fExternalManager;

	private WorkspaceModelManager fWorkspaceManager;

	private IModelProviderListener fProviderListener;
	// List of IModelProviderListener
	private ArrayList fListeners;

	public FeatureModelManager() {
		fProviderListener = new IModelProviderListener() {
			public void modelsChanged(IModelProviderEvent e) {
				handleModelsChanged(e);
			}
		};
		fListeners = new ArrayList();
	}

	public void connect(WorkspaceModelManager wm, ExternalModelManager em) {
		fExternalManager = em;
		fWorkspaceManager = wm;
//		fExternalManager.addModelProviderListener(fProviderListener);
		fWorkspaceManager.addModelProviderListener(fProviderListener);

	}

	public void shutdown() {
		if (fWorkspaceManager != null)
			fWorkspaceManager.removeModelProviderListener(fProviderListener);
//		if (fExternalManager != null)
//			fExternalManager.removeModelProviderListener(fProviderListener);
	}

	/*
	 * @return all features (workspace and external)
	 */
	public IFeatureModel[] getAllFeatures() {
		IFeatureModel[] wModels = fExternalManager.getAllFeatureModels();
		ArrayList allModels = new ArrayList();
		allModels.addAll(Arrays.asList(wModels));
		IFeatureModel[] eModels = fWorkspaceManager.getFeatureModels();
		for (int i = 0; i < eModels.length; i++) {
			if (!isFeatureIncluded(allModels, eModels[i]))
				allModels.add(eModels[i]);
		}
		return (IFeatureModel[]) allModels.toArray(new IFeatureModel[allModels
				.size()]);
	}

	private boolean isFeatureIncluded(ArrayList models,
			IFeatureModel workspaceModel) {
		for (int i = 0; i < models.size(); i++) {
			if (!(models.get(i) instanceof IFeatureModel))
				continue;
			IFeatureModel model = (IFeatureModel) models.get(i);
			if (model.getFeature().getId().equals(
					workspaceModel.getFeature().getId())
					&& model.getFeature().getVersion().equals(
							workspaceModel.getFeature().getVersion()))
				return true;
		}
		return false;
	}

	private void handleModelsChanged(IModelProviderEvent e) {
		Object[] entries = fListeners.toArray();
		for (int i = 0; i < entries.length; i++) {
			((IModelProviderListener) entries[i]).modelsChanged(e);
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelProvider#addModelProviderListener(org.eclipse.pde.core.IModelProviderListener)
	 */
	public void addModelProviderListener(IModelProviderListener listener) {
		fListeners.add(listener);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelProvider#removeModelProviderListener(org.eclipse.pde.core.IModelProviderListener)
	 */
	public void removeModelProviderListener(IModelProviderListener listener) {
		fListeners.remove(listener);
	}


}
