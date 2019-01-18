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
import java.util.HashSet;
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
	private final ArrayList<IFeatureModelListener> fListeners;

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

		fProviderListener = e -> handleModelsChanged(e);
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
		IFeatureModel[] allModels = fActiveModels.getAll();
		ArrayList<IFeatureModel> valid = new ArrayList<>(allModels.length);
		for (final IFeatureModel model : allModels) {
			if (model.isValid()) {
				valid.add(model);
			}
		}
		return valid.toArray(new IFeatureModel[valid.size()]);
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
		return fWorkspaceManager.getFeatureModel(project);
	}

	/**
	 * Finds active model with a given id and version
	 *
	 * @param id
	 * @param version version number to find, newest version is returned for empty version.
	 * @return one IFeature model or null
	 */
	public IFeatureModel findFeatureModel(String id, String version) {
		init();
		IFeatureModel[] models = fActiveModels.get(id, version);

		if (VersionUtil.isEmptyVersion(version)) {
			return findFeatureModel(id);
		}

		for (final IFeatureModel model : models) {
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
	 * @param id
	 * @param version
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
	 * @param id
	 * @return IFeature model[]
	 */
	public IFeatureModel[] findFeatureModels(String id) {
		init();
		IFeatureModel[] models = fActiveModels.get(id);
		ArrayList<IFeatureModel> valid = new ArrayList<>(models.length);
		for (final IFeatureModel model : models) {
			if (model.isValid()) {
				valid.add(model);
			}
		}
		return valid.toArray(new IFeatureModel[valid.size()]);
	}

	public IFeatureModel findFeatureModel(String id) {
		IFeatureModel[] models = findFeatureModels(id);
		IFeatureModel result = null;
		for (final IFeatureModel model : models) {
			if (result == null) {
				result = model;
			} else {
				final String version = result.getFeature().getVersion();
				final String version2 = model.getFeature().getVersion();
				Version vid = Version.parseVersion(version);
				Version vid2 = Version.parseVersion(version2);
				if (VersionUtil.isGreaterOrEqualTo(vid2, vid)) {
					result = model;
				}
			}
		}
		return result;
	}

	private void handleModelsChanged(IModelProviderEvent e) {
		init();
		IFeatureModelDelta delta = processEvent(e);

		Object[] entries = fListeners.toArray();
		for (final Object entry : entries) {
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
			for (int i = 0; i < removed.length; i++) {
				if (!(removed[i] instanceof IFeatureModel)) {
					continue;
				}
				IFeatureModel model = (IFeatureModel) removed[i];
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
			for (int i = 0; i < added.length; i++) {
				if (!(added[i] instanceof IFeatureModel)) {
					continue;
				}
				IFeatureModel model = (IFeatureModel) added[i];
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
					if (fInactiveModels.get(id, version).length > 0) {
						// ignore duplicate external models
						continue;
					}
					IFeatureModel[] activeModels = fActiveModels.get(id, version);
					for (final IFeatureModel activeModel : activeModels) {
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
			for (int i = 0; i < changed.length; i++) {
				if (!(changed[i] instanceof IFeatureModel)) {
					continue;
				}
				IFeatureModel model = (IFeatureModel) changed[i];

				String id = model.getFeature().getId();
				String version = model.getFeature().getVersion();

				FeatureTable.Idver oldIdver = fActiveModels.get(model);
				if (oldIdver != null && !oldIdver.equals(id, version)) {
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
			for (int i = 0; i < changed.length; i++) {
				if (!(changed[i] instanceof IFeatureModel)) {
					continue;
				}
				IFeatureModel model = (IFeatureModel) changed[i];
				if (!delta.contains(model, IFeatureModelDelta.ADDED | IFeatureModelDelta.REMOVED)) {
					delta.add(model, IFeatureModelDelta.CHANGED);
				}

			}
		}
		return delta;
	}

	/**
	 * @param delta
	 * @param affectedIdVers
	 */
	private void adjustExternalVisibility(FeatureModelDelta delta, Set<Idver> affectedIdVers) {
		if (affectedIdVers != null) {
			for (final Idver idver : affectedIdVers) {
				IFeatureModel[] affectedModels = fActiveModels.get(idver);
				if (affectedModels.length > 1) {
					/*
					 * there must have been at least one workspace and one
					 * external model
					 */
					for (final IFeatureModel model : affectedModels) {
						if (model.getUnderlyingResource() == null) {
							// move external to inactive
							fActiveModels.remove(model);
							fInactiveModels.add(model);
							delta.add(model, IFeatureModelDelta.REMOVED);
						}
					}
				}

				if (affectedModels.length <= 0) {
					// no workspace model
					IFeatureModel[] models = fInactiveModels.get(idver);
					if (models.length > 0) {
						// external model exists, move it to active
						fInactiveModels.remove(models[0]);
						fActiveModels.add(models[0]);
						delta.add(models[0], IFeatureModelDelta.ADDED);
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
