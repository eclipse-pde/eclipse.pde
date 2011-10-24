/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.*;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IModelProviderEvent;
import org.eclipse.pde.core.IModelProviderListener;
import org.eclipse.pde.core.target.TargetFeature;
import org.eclipse.pde.internal.core.feature.ExternalFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.target.Messages;
import org.osgi.framework.Version;

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
		InputStream stream = null;
		try {
			stream = new BufferedInputStream(new FileInputStream(manifest));
			model.load(stream, false);
			return model;
		} catch (FileNotFoundException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.TargetFeature_FileDoesNotExist, manifest)));
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public static IFeatureModel[] createModels(String platformHome, ArrayList additionalLocations, IProgressMonitor monitor) {
		if (platformHome != null && platformHome.length() > 0) {
			URL[] featureURLs = PluginPathFinder.getFeaturePaths(platformHome);

			if (additionalLocations.size() == 0)
				return createModels(featureURLs, monitor);

			File[] dirs = new File[additionalLocations.size()];
			for (int i = 0; i < dirs.length; i++) {
				String directory = additionalLocations.get(i).toString();
				File dir = new File(directory, "features"); //$NON-NLS-1$
				if (!dir.exists())
					dir = new File(directory);
				dirs[i] = dir;
			}

			URL[] newUrls = PluginPathFinder.scanLocations(dirs);

			URL[] result = new URL[featureURLs.length + newUrls.length];
			System.arraycopy(featureURLs, 0, result, 0, featureURLs.length);
			System.arraycopy(newUrls, 0, result, featureURLs.length, newUrls.length);
			return createModels(result, monitor);
		}
		return new IFeatureModel[0];
	}

	private static IFeatureModel[] createModels(URL[] featurePaths, IProgressMonitor monitor) {
		if (monitor == null)
			monitor = new NullProgressMonitor();
		monitor.beginTask("", featurePaths.length); //$NON-NLS-1$
		Map uniqueFeatures = new HashMap();
		for (int i = 0; i < featurePaths.length; i++) {
			File manifest = new File(featurePaths[i].getFile(), ICoreConstants.FEATURE_FILENAME_DESCRIPTOR);
			if (!manifest.exists() || !manifest.isFile()) {
				monitor.worked(1);
				continue;
			}
			try {
				IFeatureModel model = createModel(manifest);
				if (model != null && model.isLoaded()) {
					IFeature feature = model.getFeature();
					uniqueFeatures.put(feature.getId() + "_" + feature.getVersion(), model); //$NON-NLS-1$
				}
			} catch (CoreException e) {
				PDECore.log(e);
			}
			monitor.worked(1);
		}
		Collection models = uniqueFeatures.values();
		return (IFeatureModel[]) models.toArray(new IFeatureModel[models.size()]);
	}

	private ListenerList fListeners = new ListenerList();

	private IFeatureModel[] fModels;

	private PDEPreferencesManager fPref;

	public ExternalFeatureModelManager() {
		fPref = PDECore.getDefault().getPreferencesManager();
	}

	public void addModelProviderListener(IModelProviderListener listener) {
		fListeners.add(listener);
	}

	private void fireModelProviderEvent(IModelProviderEvent e) {
		Object[] listeners = fListeners.getListeners();
		for (int i = 0; i < listeners.length; i++) {
			IModelProviderListener listener = (IModelProviderListener) listeners[i];
			listener.modelsChanged(e);
		}
	}

	/**
	 * Loads new feature models from preferences and notifies listeners.
	 */
	public void initialize() {
		// Load all features from the platform path and addditional locations then filter by the list of external features, if available
		String platformHome = fPref.getString(ICoreConstants.PLATFORM_PATH);
		String additionalLocations = fPref.getString(ICoreConstants.ADDITIONAL_LOCATIONS);
		String externalFeaturesString = fPref.getString(ICoreConstants.EXTERNAL_FEATURES);

		IFeatureModel[] oldModels = null;
		IFeatureModel[] newModels = null;
		// Do the model loading in a synch block in case other changes cause the models to load
		synchronized (this) {
			oldModels = fModels != null ? fModels : new IFeatureModel[0];
			IFeatureModel[] allModels = createModels(platformHome, parseAdditionalLocations(additionalLocations), null);
			if (externalFeaturesString == null || externalFeaturesString.trim().length() == 0) {
				fModels = allModels;
			} else {
				// To allow multiple versions of features, create a map of feature ids to a list of models
				Map modelMap = new HashMap();
				for (int i = 0; i < allModels.length; i++) {
					String id = allModels[i].getFeature().getId();
					if (modelMap.containsKey(id)) {
						List list = (List) modelMap.get(id);
						list.add(allModels[i]);
					} else {
						List list = new ArrayList();
						list.add(allModels[i]);
						modelMap.put(id, list);
					}
				}

				// Loop through the filter list, finding an exact match in the available models or highest version match
				Set filteredModels = new HashSet();
				String[] entries = externalFeaturesString.split(","); //$NON-NLS-1$
				for (int i = 0; i < entries.length; i++) {
					String[] parts = entries[i].split("@"); //$NON-NLS-1$
					if (parts.length > 0) {
						String id = parts[0];
						List possibilities = (List) modelMap.get(id);
						if (possibilities != null) {
							IFeatureModel candidate = null;
							for (Iterator iterator = possibilities.iterator(); iterator.hasNext();) {
								IFeatureModel current = (IFeatureModel) iterator.next();
								if (candidate == null) {
									candidate = current;
								} else if (parts.length > 1 && parts[1].equals(current.getFeature().getVersion())) {
									candidate = current;
								} else {
									Version currentVersion = Version.parseVersion(current.getFeature().getVersion());
									Version candidateVersion = Version.parseVersion(candidate.getFeature().getVersion());
									if (currentVersion.compareTo(candidateVersion) == 1) {
										candidate = current;
									}
								}
							}
							if (candidate != null) {
								filteredModels.add(candidate);
							}
						}
					}
				}
				fModels = (IFeatureModel[]) filteredModels.toArray(new IFeatureModel[filteredModels.size()]);
			}
			newModels = new IFeatureModel[fModels.length];
			System.arraycopy(fModels, 0, newModels, 0, fModels.length);
		}
		// Release lock when notifying listeners. See bug 270891.
		notifyListeners(oldModels, newModels);
	}

	private ArrayList parseAdditionalLocations(String additionalLocations) {
		ArrayList result = new ArrayList();
		StringTokenizer tokenizer = new StringTokenizer(additionalLocations, ","); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens()) {
			result.add(tokenizer.nextToken().trim());
		}
		return result;
	}

	private void notifyListeners(IFeatureModel[] oldModels, IFeatureModel[] newFeatureModels) {
		if (oldModels.length > 0 || newFeatureModels.length > 0) {
			int type = 0;
			if (oldModels.length > 0)
				type |= IModelProviderEvent.MODELS_REMOVED;
			if (newFeatureModels.length > 0)
				type |= IModelProviderEvent.MODELS_ADDED;
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

	public static TargetFeature[] createFeatures(String platformHome, ArrayList additionalLocations, IProgressMonitor monitor) {
		if (platformHome != null && platformHome.length() > 0) {
			URL[] featureURLs = PluginPathFinder.getFeaturePaths(platformHome);

			if (additionalLocations.size() == 0)
				return createFeatures(featureURLs, monitor);

			File[] dirs = new File[additionalLocations.size()];
			for (int i = 0; i < dirs.length; i++) {
				String directory = additionalLocations.get(i).toString();
				File dir = new File(directory, "features"); //$NON-NLS-1$
				if (!dir.exists())
					dir = new File(directory);
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
		if (monitor == null)
			monitor = new NullProgressMonitor();
		monitor.beginTask("", featurePaths.length); //$NON-NLS-1$
		Map uniqueFeatures = new HashMap();
		for (int i = 0; i < featurePaths.length; i++) {
			File manifest = new File(featurePaths[i].getFile(), ICoreConstants.FEATURE_FILENAME_DESCRIPTOR);
			if (!manifest.exists() || !manifest.isFile()) {
				monitor.worked(1);
				continue;
			}
			try {
				TargetFeature model = new TargetFeature(manifest);
				uniqueFeatures.put(model.getId() + "_" + model.getVersion(), model); //$NON-NLS-1$
			} catch (CoreException e) {
				// Ignore bad files in the collection
			}
			monitor.worked(1);
		}
		Collection models = uniqueFeatures.values();
		return (TargetFeature[]) models.toArray(new TargetFeature[models.size()]);
	}
}
