/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
import org.eclipse.pde.core.IModelProviderEvent;
import org.eclipse.pde.core.IModelProviderListener;
import org.eclipse.pde.internal.core.feature.ExternalFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;

public class ExternalFeatureModelManager implements
		Preferences.IPropertyChangeListener {

	/**
	 * 
	 * @param manifest
	 * @return ExternalFeatureModel or null
	 */
	private static IFeatureModel createModel(File manifest) {
		ExternalFeatureModel model = new ExternalFeatureModel();
		model.setInstallLocation(manifest.getParent());
		InputStream stream = null;
		try {
			stream = new FileInputStream(manifest);
			model.load(stream, false);
			return model;
		} catch (Exception e) {
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
				}
			}
		}
		return null;
	}

	/**
	 * @param monitor
	 * @return IFeatureModel[]
	 */
	private static IFeatureModel[] createModels(String platformHome) {
		URL[] featurePaths = PluginPathFinder.getFeaturePaths(platformHome);
		Map uniqueFeatures = new HashMap();
		for (int i = 0; i < featurePaths.length; i++) {
			File manifest = new File(featurePaths[i].getFile(), "feature.xml"); //$NON-NLS-1$
			IFeatureModel model = createModel(manifest);
			if (model != null && model.isLoaded()) {
				IFeature feature = model.getFeature();
				uniqueFeatures.put(
						feature.getId() + "_" + feature.getVersion(), model); //$NON-NLS-1$
			}
		}
		Collection models = uniqueFeatures.values();
		return (IFeatureModel[]) models
				.toArray(new IFeatureModel[models.size()]);
	}

	private Vector fListeners = new Vector();

	private IFeatureModel[] fModels;

	private String fPlatformHome;

	private Preferences fPref;

	public ExternalFeatureModelManager() {
		fPref = PDECore.getDefault().getPluginPreferences();
	}

	public void addModelProviderListener(IModelProviderListener listener) {
		fListeners.add(listener);
	}

	private boolean equalPaths(String path1, String path2) {
		if (path1 == null) {
			if (path2 == null) {
				return true;
			}
			return false;
		}
		if (path2 == null) {
			return false;
		}
		return new File(path1).equals(new File(path2));

	}

	private void fireModelProviderEvent(IModelProviderEvent e) {
		for (Iterator iter = fListeners.iterator(); iter.hasNext();) {
			IModelProviderListener listener = (IModelProviderListener) iter
					.next();
			listener.modelsChanged(e);
		}
	}

	/**
	 * @param propertyValue
	 * @return String or null
	 */
	private String getPathString(Object propertyValue) {
		if (propertyValue != null && propertyValue instanceof String) {
			String path = (String) propertyValue;
			if (path.length() > 0) {
				return path;
			}
		}
		return null;
	}

	private void loadModels(String platformHome) {
		IFeatureModel[] newModels;
		if (platformHome != null && platformHome.length() > 0) {
			newModels = createModels(platformHome);
		} else {
			newModels = new IFeatureModel[0];
		}

		fPlatformHome = platformHome;

		IFeatureModel[] oldModels = fModels != null ? fModels
				: new IFeatureModel[0];
		fModels = newModels;
		notifyListeners(oldModels, newModels);
	}

	private void notifyListeners(IFeatureModel[] oldModels,
			IFeatureModel[] newFeatureModels) {
		if (oldModels.length > 0 || newFeatureModels.length > 0) {
			int type = 0;
			if (oldModels.length > 0)
				type |= IModelProviderEvent.MODELS_REMOVED;
			if (newFeatureModels.length > 0)
				type |= IModelProviderEvent.MODELS_ADDED;
			ModelProviderEvent replacedFeatures = new ModelProviderEvent(this,
					type, newFeatureModels, oldModels, null);
			fireModelProviderEvent(replacedFeatures);
		}

	}

	private synchronized void platformPathChanged(String newHome) {
		if (!equalPaths(newHome, fPlatformHome)) {
			loadModels(newHome);
		}
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (!ICoreConstants.PLATFORM_PATH.equals(event.getProperty())) {
			return;
		}
		String newHome = getPathString(event.getNewValue());
		platformPathChanged(newHome);
	}

	public void removeModelProviderListener(IModelProviderListener listener) {
		fListeners.remove(listener);
	}

	public synchronized void shutdown() {
		fPref.removePropertyChangeListener(this);
		loadModels(null);
	}

	public synchronized void startup() {
		fPref.addPropertyChangeListener(this);
		loadModels(fPref.getString(ICoreConstants.PLATFORM_PATH));
	}
}
