/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.util.ArrayList;
import java.util.HashMap;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.pde.internal.core.ExternalFeatureModelManager;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.launching.IPDELauncherConstants;

/**
 * Provides a model for the FeatureBlock to display in the Plug-ins Tab of launch configs.  Model
 * provides a list of features that can come from a combination of the workspace and the target.
 * 
 * @see FeatureBlock
 */
public class FeatureBlockModelProvider {

	private static final String LOCATION_DEFAULT = "Default"; //$NON-NLS-1$
	private static final String LOCATION_EXTERNAL = "External"; //$NON-NLS-1$
	private static final String LOCATION_WORKSPACE = "Workspace"; //$NON-NLS-1$

	private class FeatureLaunchModel {
		public IFeatureModel FeatureModel;
		public String Location;
		public String PluginResolution;

		public FeatureLaunchModel(IFeatureModel model) {
			FeatureModel = model;
			Location = LOCATION_DEFAULT;
			PluginResolution = LOCATION_DEFAULT;
		}

		public String getVersion() {
			return FeatureModel.getFeature().getVersion();
		}

	}

	private HashMap fFeatureModels;
	private HashMap fWorkspaceFeatureMap;
	private HashMap fExternalFeatureMap;
	private String fDefaultFeatureLocation;
	private String fDefaultPluginResolution;

	public FeatureBlockModelProvider() {
		fFeatureModels = new HashMap();
		fWorkspaceFeatureMap = new HashMap();
		fExternalFeatureMap = new HashMap();
		fDefaultFeatureLocation = LOCATION_WORKSPACE;
		fDefaultPluginResolution = LOCATION_WORKSPACE;

		init(LOCATION_WORKSPACE);
	}

	public void init(String defaultLocation) {
		FeatureModelManager fmm = new FeatureModelManager();
		IFeatureModel[] workspaceModels = fmm.getWorkspaceModels();
		for (int i = 0; i < workspaceModels.length; i++) {
			String id = workspaceModels[i].getFeature().getId();
			fWorkspaceFeatureMap.put(id, workspaceModels[i]);
			put(id, workspaceModels[i]);
		}
		fmm.shutdown();

		ExternalFeatureModelManager efmm = new ExternalFeatureModelManager();
		efmm.startup();
		IFeatureModel[] externalModels = efmm.getModels();
		for (int i = 0; i < externalModels.length; i++) {
			String id = externalModels[i].getFeature().getId();
			fExternalFeatureMap.put(id, externalModels[i]);
			if (LOCATION_EXTERNAL.equalsIgnoreCase(defaultLocation) || (LOCATION_WORKSPACE.equalsIgnoreCase(defaultLocation) && !fWorkspaceFeatureMap.containsKey(id))) {
				put(id, externalModels[i]);
			}
		}
		efmm.shutdown();
	}

	private void put(String id, IFeatureModel model) {
		fFeatureModels.put(id, new FeatureLaunchModel(model));
	}

	public int size() {
		return fFeatureModels.size();
	}

	public boolean isCommon(String id) {
		if (fWorkspaceFeatureMap.containsKey(id) && fExternalFeatureMap.containsKey(id)) {
			return true;
		}
		return false;
	}

	public String getVersion(String id) {
		FeatureLaunchModel model = (FeatureLaunchModel) fFeatureModels.get(id);
		if (model != null)
			return model.getVersion();
		return null;
	}

	public String getLocation(String id) {
		FeatureLaunchModel model = (FeatureLaunchModel) fFeatureModels.get(id);
		if (model != null)
			return model.Location;
		return null;
	}

	public String getPluginResolution(String id) {
		FeatureLaunchModel model = (FeatureLaunchModel) fFeatureModels.get(id);
		if (model != null)
			return model.PluginResolution;
		return null;
	}

	public String[] getIDArray() {
		return (String[]) fFeatureModels.keySet().toArray(new String[size()]);
	}

	public IFeatureModel getFeatureModel(String id) {
		FeatureLaunchModel model = getFeatureLaunchModel(id);
		if (model != null)
			return model.FeatureModel;
		return null;
	}

	private FeatureLaunchModel getFeatureLaunchModel(String id) {
		return (FeatureLaunchModel) fFeatureModels.get(id);
	}

	public void setModel(String id, IFeatureModel featureModel) {
		FeatureLaunchModel model = getFeatureLaunchModel(id);
		if (model != null)
			model.FeatureModel = featureModel;
	}

	public void setLocation(String id, String location) {
		FeatureLaunchModel model = getFeatureLaunchModel(id);
		if (model != null)
			model.Location = location;
	}

	public void setModelFromLocation(String id, String location) {
		HashMap map = null;
		String modelLocation = location;
		if (LOCATION_DEFAULT.equalsIgnoreCase(location)) {
			location = fDefaultFeatureLocation;
		}
		if (LOCATION_WORKSPACE.equalsIgnoreCase(location)) {
			map = fWorkspaceFeatureMap;
		} else if (LOCATION_EXTERNAL.equalsIgnoreCase(location)) {
			map = fExternalFeatureMap;
		}

		if (map != null) {
			IFeatureModel model = (IFeatureModel) map.get(id);
			if (model != null) {
				setModel(id, model);
				setLocation(id, modelLocation);
			}
		}
	}

	public void setPluginResolution(String id, String location) {
		FeatureLaunchModel model = getFeatureLaunchModel(id);
		if (model != null)
			model.PluginResolution = location;
	}

	public String getDefaultFeatureLocation() {
		return fDefaultFeatureLocation;
	}

	public void setDefaultFeatureLocation(String location) {
		if (LOCATION_WORKSPACE.equalsIgnoreCase(location) || LOCATION_EXTERNAL.equalsIgnoreCase(location)) {
			fDefaultFeatureLocation = location;
		}
	}

	public String getDefaultPluginResolution() {
		return fDefaultPluginResolution;
	}

	public void setDefaultPluginResolution(String location) {
		if (LOCATION_WORKSPACE.equalsIgnoreCase(location) || LOCATION_EXTERNAL.equalsIgnoreCase(location)) {
			fDefaultPluginResolution = location;
		}
	}

	public ArrayList parseConfig(ILaunchConfiguration config) throws CoreException {
		ArrayList selectedFeatureList = new ArrayList();
		String value = config.getAttribute(IPDELauncherConstants.SELECTED_FEATURES, ""); //$NON-NLS-1$
		setDefaultFeatureLocation(config.getAttribute(IPDELauncherConstants.FEATURE_DEFAULT_LOCATION, LOCATION_WORKSPACE));
		setDefaultPluginResolution(config.getAttribute(IPDELauncherConstants.FEATURE_PLUGIN_RESOLUTION, LOCATION_WORKSPACE));

		if (value.length() != 0) {
			String[] features = value.split(";"); //$NON-NLS-1$
			if (features != null && features.length > 0) {
				for (int i = 0; i < features.length; i++) {
					String[] attributes = features[i].split(":"); //$NON-NLS-1$
					String id = attributes[0];
					selectedFeatureList.add(id);
					setModelFromLocation(id, attributes[1]);
					setPluginResolution(id, attributes[2]);
				}
			}
		}
		return selectedFeatureList;
	}
}
