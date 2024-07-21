/*******************************************************************************
 *  Copyright (c) 2000, 2017 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelProviderEvent;
import org.eclipse.pde.core.IModelProviderListener;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.internal.core.FeatureTable.Idver;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.target.P2TargetUtils;
import org.eclipse.pde.internal.core.util.VersionUtil;
import org.osgi.framework.Version;

/**
 * Manages all feature models in the workspace, and maximum one external feature
 * model for a given id and version. While workspace model(s) exist with the
 * same id and version as an external model, the external model is inactive (not
 * exposed).
 */

public class FeatureModelManager {

	/**
	 * All models in workspace, and those external models that have no
	 * corresponding workspace model with the same id and version
	 */
	private FeatureTable fActiveModels;

	/**
	 * External models masked by workspace models with the same id and version.
	 */
	private FeatureTable fInactiveModels;

	private ExternalFeatureModelManager fExternalManager;

	private boolean fReloadExternalNeeded = false;

	private final WorkspaceFeatureModelManager fWorkspaceManager;

	private IModelProviderListener fProviderListener;

	/**
	 * List of IFeatureModelListener
	 */
	private final List<IFeatureModelListener> fListeners;

	public FeatureModelManager() {
		fWorkspaceManager = new WorkspaceFeatureModelManager();
		fListeners = new ArrayList<>();
	}

	public synchronized void shutdown() {
		if (fWorkspaceManager != null) {
			fWorkspaceManager.removeModelProviderListener(fProviderListener);
		}
		if (fExternalManager != null) {
			fExternalManager.removeModelProviderListener(fProviderListener);
		}
	}

	public boolean isInitialized() {
		return (fActiveModels != null && !fReloadExternalNeeded);
	}

	private synchronized void init() {
		if (fActiveModels != null) {
			if (fReloadExternalNeeded) {
				fReloadExternalNeeded = false;
				fExternalManager.initialize();
			}
			return;
		}

		fActiveModels = new FeatureTable();
		fInactiveModels = new FeatureTable();

		fProviderListener = this::handleModelsChanged;
		fWorkspaceManager.addModelProviderListener(fProviderListener);

		IFeatureModel[] models = fWorkspaceManager.getFeatureModels();
		for (IFeatureModel model : models) {
			// add all workspace models, including invalid or duplicate (save
			// id, ver)
			fActiveModels.add(model);
		}

		fExternalManager = new ExternalFeatureModelManager();
		fExternalManager.addModelProviderListener(fProviderListener);
		fReloadExternalNeeded = false;

		ITargetDefinition unresolvedRepoBasedtarget = null;
		try {
			unresolvedRepoBasedtarget = TargetPlatformHelper.getUnresolvedRepositoryBasedWorkspaceTarget();
		} catch (CoreException e) {
			PDECore.log(e);
		}
		if (unresolvedRepoBasedtarget != null && !P2TargetUtils.isProfileValid(unresolvedRepoBasedtarget)) {

			WorkspaceJob initializeExternalManager = new WorkspaceJob(PDECoreMessages.FeatureModelManager_initializingFeatureTargetPlatform) {
				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) {
					fExternalManager.initialize();
					return Status.OK_STATUS;
				}
			};
			initializeExternalManager.schedule();
		} else {
			fExternalManager.initialize();
		}

	}

	/*
	 * @return all active features
	 */
	public IFeatureModel[] getModels() {
		init();
		return fActiveModels.getAllValidFeatures();
	}

	/**
	 * @return all models in the workspace model manager
	 */
	public IFeatureModel[] getWorkspaceModels() {
		init();
		return fWorkspaceManager.getFeatureModels();
	}

	/**
	 * @return all models in the external model manager
	 */
	public IFeatureModel[] getExternalModels() {
		init();
		return fExternalManager.getModels();
	}

	public IFeatureModel getFeatureModel(IProject project) {
		init();
		return fWorkspaceManager.getModel(project);
	}

	/**
	 * Finds active model with a given id and version
	 *
	 * @param version version number to find, newest version is returned for empty version.
	 * @return one IFeature model or null
	 */
	public IFeatureModel findFeatureModel(String id, String version) {
		init();
		if (VersionUtil.isEmptyVersion(version)) {
			return findFeatureModel(id);
		}
		List<IFeatureModel> models = fActiveModels.get(id, version);
		for (IFeatureModel model : models) {
			if (model.isValid()) {
				return model;
			}
		}

		return null;
	}

	/**
	 * Finds active model with the given id and version. If feature is not
	 * found, but a feature with qualifier set to qualifier exists it will be
	 * returned.
	 *
	 * @return IFeatureModel or null
	 */
	public IFeatureModel findFeatureModelRelaxed(String id, String version) {
		IFeatureModel model = findFeatureModel(id, version);
		if (model != null) {
			return model;
		}
		try {
			Version pvi = Version.parseVersion(version);
			return findFeatureModel(id, pvi.getMajor() + "." //$NON-NLS-1$
					+ pvi.getMinor() + "." //$NON-NLS-1$
					+ pvi.getMicro() + ".qualifier"); //$NON-NLS-1$

		} catch (IllegalArgumentException e) {
			// handle the case where the version is not in proper format (bug 203795)
			return null;
		}
	}

	/**
	 * Finds active models with a given id
	 *
	 * @return IFeature model[]
	 */
	public List<IFeatureModel> findFeatureModels(String id) {
		init();
		return fActiveModels.getAllValidFeatures(id);
	}

	private static final Comparator<IFeatureModel> FEATURE_VERSION = Comparator
			.comparing(f -> Version.parseVersion(f.getFeature().getVersion()));

	public IFeatureModel findFeatureModel(String id) {
		List<IFeatureModel> models = findFeatureModels(id);
		return models.stream().max(FEATURE_VERSION).orElse(null);
	}

	private void handleModelsChanged(IModelProviderEvent e) {
		init();
		IFeatureModelDelta delta = processEvent(e);

		Object[] entries = fListeners.toArray();
		for (Object entry : entries) {
			((IFeatureModelListener) entry).modelsChanged(delta);
		}
	}

	private synchronized IFeatureModelDelta processEvent(IModelProviderEvent e) {
		FeatureModelDelta delta = new FeatureModelDelta();
		/*
		 * Set of Idvers for which there might be necessary to move a model
		 * between active models and inactive models
		 */
		Set<Idver> affectedIdVers = null;
		if ((e.getEventTypes() & IModelProviderEvent.MODELS_REMOVED) != 0) {
			IModel[] removed = e.getRemovedModels();
			for (IModel element : removed) {
				if (!(element instanceof IFeatureModel model)) {
					continue;
				}
				FeatureTable.Idver idver = fActiveModels.remove(model);
				if (idver != null) {
					// may need to activate another model
					if (affectedIdVers == null) {
						affectedIdVers = new HashSet<>();
					}
					affectedIdVers.add(idver);
					delta.add(model, IFeatureModelDelta.REMOVED);
				} else {
					fInactiveModels.remove(model);
				}
			}
		}
		if ((e.getEventTypes() & IModelProviderEvent.MODELS_ADDED) != 0) {
			IModel[] added = e.getAddedModels();
			for (IModel element : added) {
				if (!(element instanceof IFeatureModel model)) {
					continue;
				}
				if (model.getUnderlyingResource() != null) {
					FeatureTable.Idver idver = fActiveModels.add(model);
					delta.add(model, IFeatureModelDelta.ADDED);
					// may need to deactivate another model
					if (affectedIdVers == null) {
						affectedIdVers = new HashSet<>();
					}
					affectedIdVers.add(idver);
				} else {
					if (!model.isValid()) {
						// ignore invalid external models
						continue;
					}
					String id = model.getFeature().getId();
					String version = model.getFeature().getVersion();
					if (!fInactiveModels.get(id, version).isEmpty()) {
						// ignore duplicate external models
						continue;
					}
					List<IFeatureModel> activeModels = fActiveModels.get(id, version);
					for (IFeatureModel activeModel : activeModels) {
						if (activeModel.getUnderlyingResource() == null) {
							// ignore duplicate external models
							continue;
						}
					}
					FeatureTable.Idver idver = fInactiveModels.add(model);
					// may need to activate this model
					if (affectedIdVers == null) {
						affectedIdVers = new HashSet<>();
					}
					affectedIdVers.add(idver);
				}
			}
		}

		/* 1. Reinsert with a new id and version, if necessary */
		if ((e.getEventTypes() & IModelProviderEvent.MODELS_CHANGED) != 0) {
			IModel[] changed = e.getChangedModels();
			for (IModel element : changed) {
				if (!(element instanceof IFeatureModel model)) {
					continue;
				}
				String id = model.getFeature().getId();
				String version = model.getFeature().getVersion();

				FeatureTable.Idver oldIdver = fActiveModels.get(model);
				if (oldIdver != null
						&& (!Objects.equals(oldIdver.id(), id) || !Objects.equals(oldIdver.version(), version))) {
					// version changed
					FeatureTable.Idver idver = fActiveModels.add(model);
					if (affectedIdVers == null) {
						affectedIdVers = new HashSet<>();
					}
					affectedIdVers.add(oldIdver);
					affectedIdVers.add(idver);
				}
				/*
				 * no need to check inactive models, because external features
				 * do not chance or version
				 */

			}
		}
		/* 2. Move features between active and inactive tables if necessary */
		adjustExternalVisibility(delta, affectedIdVers);
		/*
		 * 3. Changed models that do result in FeatureModelDelta.ADDED or
		 * FeatureModelDelta.Removed fire FeatureModelDelta.CHANGED
		 */
		if ((e.getEventTypes() & IModelProviderEvent.MODELS_CHANGED) != 0) {
			IModel[] changed = e.getChangedModels();
			for (IModel element : changed) {
				if (!(element instanceof IFeatureModel model)) {
					continue;
				}
				if (!delta.contains(model, IFeatureModelDelta.ADDED | IFeatureModelDelta.REMOVED)) {
					delta.add(model, IFeatureModelDelta.CHANGED);
				}

			}
		}
		return delta;
	}

	private void adjustExternalVisibility(FeatureModelDelta delta, Set<Idver> affectedIdVers) {
		if (affectedIdVers != null) {
			for (Idver idver : affectedIdVers) {
				List<IFeatureModel> affectedModels = fActiveModels.get(idver);
				if (affectedModels.size() > 1) {
					/*
					 * there must have been at least one workspace and one
					 * external model
					 */
					for (IFeatureModel model : affectedModels) {
						if (model.getUnderlyingResource() == null) {
							// move external to inactive
							fActiveModels.remove(model);
							fInactiveModels.add(model);
							delta.add(model, IFeatureModelDelta.REMOVED);
						}
					}
				}

				if (affectedModels.isEmpty()) {
					// no workspace model
					List<IFeatureModel> models = fInactiveModels.get(idver);
					if (!models.isEmpty()) {
						IFeatureModel firstModel = models.get(0);
						// external model exists, move it to active
						fInactiveModels.remove(firstModel);
						fActiveModels.add(firstModel);
						delta.add(firstModel, IFeatureModelDelta.ADDED);
					}
				}
			}
		}
	}

	public void addFeatureModelListener(IFeatureModelListener listener) {
		if (!fListeners.contains(listener)) {
			fListeners.add(listener);
		}
	}

	public void removeFeatureModelListener(IFeatureModelListener listener) {
		if (fListeners.contains(listener)) {
			fListeners.remove(listener);
		}
	}

	public void targetReloaded() {
		fReloadExternalNeeded = true;
	}

	public IFeatureModel getDeltaPackFeature() {
		IFeatureModel model = findFeatureModel("org.eclipse.equinox.executable"); //$NON-NLS-1$
		if (model == null) {
			model = findFeatureModel("org.eclipse.platform.launchers"); //$NON-NLS-1$
		}
		return model;
	}

	public void removeFromWorkspaceFeature(IFeatureModel iFeatureModel) {
		if (fWorkspaceManager != null) {
			fWorkspaceManager.removeModel(iFeatureModel);
		}
	}
}
