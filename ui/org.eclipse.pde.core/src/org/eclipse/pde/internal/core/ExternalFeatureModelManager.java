/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.pde.internal.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IModelProviderEvent;
import org.eclipse.pde.core.IModelProviderListener;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.TargetFeature;
import org.eclipse.pde.internal.core.feature.ExternalFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.target.Messages;

/**
 * Manages the features known to the PDE state that come from the target platform.
 * <p>
 * Contains utility methods to create feature models for locations.
 * </p>
 */
public class ExternalFeatureModelManager {

	/**
	 * Creates a feature model for the feature based on the given feature XML
	 * file.
	 *
	 * @param manifest feature XML file in the local file system
	 * @return {@link ExternalFeatureModel} containing information loaded from the xml
	 * @throws CoreException if there is a problem reading the feature xml
	 */
	public static IFeatureModel createModel(File manifest) throws CoreException {
		ExternalFeatureModel model = new ExternalFeatureModel();
		model.setInstallLocation(manifest.getParent());
		try (InputStream stream = new BufferedInputStream(new FileInputStream(manifest))) {
			model.load(stream, false);
			return model;
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.TargetFeature_FileDoesNotExist, manifest)));
		}
	}

	private final ListenerList<IModelProviderListener> fListeners = new ListenerList<>();

	private IFeatureModel[] fModels;

	public void addModelProviderListener(IModelProviderListener listener) {
		fListeners.add(listener);
	}

	private void fireModelProviderEvent(IModelProviderEvent e) {
		for (IModelProviderListener listener : fListeners) {
			listener.modelsChanged(e);
		}
	}

	/**
	 * Loads new feature models from preferences and notifies listeners.
	 */
	public void initialize() {
		// Do the model loading in a synch block in case other changes cause the models to load
		IFeatureModel[] oldModels = null;
		synchronized (this) {
			oldModels = fModels != null ? fModels : new IFeatureModel[0];
			long startTime = System.currentTimeMillis();
			fModels = getExternalModels();
			if (PDECore.DEBUG_MODEL) {
				System.out.println("Time to load features from target platform (ms): " + (System.currentTimeMillis() - startTime)); //$NON-NLS-1$
				System.out.println("External features loaded: " + fModels.length); //$NON-NLS-1$
			}
		}
		// Release lock when notifying listeners. See bug 270891.
		notifyListeners(oldModels, fModels);
	}

	/**
	 * Returns a list of feature models from the target platform or an empty
	 * array if there is a problem.
	 *
	 * @return list of external feature models, possibly empty
	 */
	private IFeatureModel[] getExternalModels() {
		// Plug-in resolution was cancelled by the user so skip resolving the target
		if (PDECore.getDefault().getModelManager().isCancelled()) {
			return new IFeatureModel[0];
		}

		ITargetDefinition target = null;
		try {
			target = TargetPlatformHelper.getWorkspaceTargetResolved(null);
		} catch (CoreException e) {
			PDECore.log(e);
			return new IFeatureModel[0];
		}

		// Resolution cancelled
		if (target == null) {
			return new IFeatureModel[0];
		}

		List<IFeatureModel> result = new ArrayList<>();
		TargetFeature[] features = target.getAllFeatures();
		if (features != null) {
			for (TargetFeature feature : features) {
				String location = feature.getLocation();
				File manifest = new File(location, ICoreConstants.FEATURE_FILENAME_DESCRIPTOR);
				if (!manifest.exists() || !manifest.isFile()) {
					continue;
				}
				try {
					IFeatureModel model = createModel(manifest);
					if (model != null && model.isLoaded()) {
						result.add(model);
					}
				} catch (CoreException e) {
					PDECore.log(e);
				}
			}
		}
		return result.toArray(new IFeatureModel[result.size()]);
	}

	private void notifyListeners(IFeatureModel[] oldModels, IFeatureModel[] newFeatureModels) {
		if (oldModels.length > 0 || newFeatureModels.length > 0) {
			int type = 0;
			if (oldModels.length > 0) {
				type |= IModelProviderEvent.MODELS_REMOVED;
			}
			if (newFeatureModels.length > 0) {
				type |= IModelProviderEvent.MODELS_ADDED;
			}
			ModelProviderEvent replacedFeatures = new ModelProviderEvent(this, type, newFeatureModels, oldModels, null);
			fireModelProviderEvent(replacedFeatures);
		}

	}

	public void removeModelProviderListener(IModelProviderListener listener) {
		fListeners.remove(listener);
	}

	public IFeatureModel[] getModels() {
		return fModels;
	}

	public static TargetFeature[] createFeatures(String platformHome, ArrayList<?> additionalLocations, IProgressMonitor monitor) {
		if (platformHome != null && platformHome.length() > 0) {
			URL[] featureURLs = PluginPathFinder.getFeaturePaths(platformHome);

			if (additionalLocations.isEmpty()) {
				return createFeatures(featureURLs, monitor);
			}

			File[] dirs = new File[additionalLocations.size()];
			for (int i = 0; i < dirs.length; i++) {
				String directory = additionalLocations.get(i).toString();
				File dir = new File(directory, "features"); //$NON-NLS-1$
				if (!dir.exists()) {
					dir = new File(directory);
				}
				dirs[i] = dir;
			}

			URL[] newUrls = PluginPathFinder.scanLocations(dirs);

			URL[] result = new URL[featureURLs.length + newUrls.length];
			System.arraycopy(featureURLs, 0, result, 0, featureURLs.length);
			System.arraycopy(newUrls, 0, result, featureURLs.length, newUrls.length);
			return createFeatures(result, monitor);
		}
		return new TargetFeature[0];
	}

	private static TargetFeature[] createFeatures(URL[] featurePaths, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, featurePaths.length);
		Map<String, TargetFeature> uniqueFeatures = new LinkedHashMap<>();
		for (URL featurePath : featurePaths) {
			File manifest = new File(featurePath.getFile(), ICoreConstants.FEATURE_FILENAME_DESCRIPTOR);
			if (!manifest.exists() || !manifest.isFile()) {
				subMonitor.split(1);
				continue;
			}
			try {
				TargetFeature model = new TargetFeature(manifest);
				uniqueFeatures.put(model.getId() + "_" + model.getVersion(), model); //$NON-NLS-1$
			} catch (CoreException e) {
				// Ignore bad files in the collection
			}
			subMonitor.split(1);
		}
		Collection<TargetFeature> models = uniqueFeatures.values();
		return models.toArray(new TargetFeature[models.size()]);
	}
}
