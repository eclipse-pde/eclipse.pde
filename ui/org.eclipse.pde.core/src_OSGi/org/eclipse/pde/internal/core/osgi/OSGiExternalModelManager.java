/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.osgi;
import java.net.URL;
import java.util.*;

import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;

/**
 */
public class OSGiExternalModelManager implements IExternalModelManager {
	private Vector models = new Vector();
	private Vector fmodels = new Vector();
	private Vector listeners = new Vector();
	private boolean initialized;

	public static String computeDefaultPlatformPath() {
		URL installURL = BootLoader.getInstallURL();
		IPath ppath = new Path(installURL.getFile()).removeTrailingSeparator();
		return getCorrectPath(ppath.toOSString());
	}

	private static String getCorrectPath(String path) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < path.length(); i++) {
			char c = path.charAt(i);
			if (BootLoader.getOS().equals("win32")) {
				if (i == 0 && c == '/')
					continue;
			}
			// Some VMs may return %20 instead of a space
			if (c == '%' && i + 2 < path.length()) {
				char c1 = path.charAt(i + 1);
				char c2 = path.charAt(i + 2);
				if (c1 == '2' && c2 == '0') {
					i += 2;
					buf.append(" ");
					continue;
				}
			}
			buf.append(c);
		}
		return buf.toString();
	}
	
	public static IPath getEclipseHome(IProgressMonitor monitor) {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		return new Path(preferences.getString(ICoreConstants.PLATFORM_PATH));
	}

	public OSGiExternalModelManager() {
		loadModels(new NullProgressMonitor());
	}

	public void addModelProviderListener(IModelProviderListener listener) {
		listeners.add(listener);
	}

	private Vector createSavedList(String saved) {
		Vector result = new Vector();
		StringTokenizer stok = new StringTokenizer(saved);
		while (stok.hasMoreTokens()) {
			result.add(stok.nextToken());
		}
		return result;
	}

	public void enableAll() {
		for (int i = 0; i < models.size(); i++)
			((IPluginModel)models.get(i)).setEnabled(true);
			
		for (int i = 0; i < fmodels.size(); i++)
			((IFragmentModel)fmodels.get(i)).setEnabled(true);			
	}

	public IPluginExtensionPoint findExtensionPoint(String fullID) {
		if (fullID == null || fullID.length() == 0)
			return null;
		// separate plugin ID first
		int lastDot = fullID.lastIndexOf('.');
		if (lastDot == -1)
			return null;
		String pluginID = fullID.substring(0, lastDot);
		IPlugin plugin = findPlugin(pluginID);
		if (plugin == null)
			return null;
		String pointID = fullID.substring(lastDot + 1);
		IPluginExtensionPoint[] points = plugin.getExtensionPoints();
		for (int i = 0; i < points.length; i++) {
			IPluginExtensionPoint point = points[i];
			if (point.getId().equals(pointID))
				return point;
		}
		return null;
	}

	public IPlugin findPlugin(String id) {
		for (int i = 0; i < models.size(); i++) {
			IPlugin plugin = ((IPluginModel)models.get(i)).getPlugin();
			if (plugin.getId().equals(id))
				return plugin;
		}
		return null;
	}

	public void fireModelProviderEvent(IModelProviderEvent e) {
		for (Iterator iter = listeners.iterator(); iter.hasNext();) {
			IModelProviderListener listener = (IModelProviderListener) iter.next();
			listener.modelsChanged(e);
		}
	}

	public IFragmentModel[] getFragmentModels() {
		return (IFragmentModel[]) fmodels.toArray(
			new IFragmentModel[fmodels.size()]);
	}
	
	public IFeatureModel[] getFeatureModels() {
		return new IFeatureModel[0];
	}

	public IFragment[] getFragmentsFor(String pluginID, String pluginVersion) {
		ArrayList result = new ArrayList();

		for (int i = 0; i < fmodels.size(); i++) {
			IFragment fragment = ((IFragmentModel) fmodels.get(i)).getFragment();
			if (PDECore
				.compare(
					fragment.getPluginId(),
					fragment.getPluginVersion(),
					pluginID,
					pluginVersion,
					fragment.getRule()))
				result.add(fragment);
		}

		return (IFragment[]) result.toArray(new IFragment[result.size()]);
	}

	public IPluginModelBase[] getAllModels() {
		IPluginModelBase[] allModels =
			new IPluginModelBase[models.size() + fmodels.size()];
		System.arraycopy(getPluginModels(), 0, allModels, 0, models.size());
		System.arraycopy(
			getFragmentModels(),
			0,
			allModels,
			models.size(),
			fmodels.size());

		return allModels;
	}
	
	public IPluginModelBase[] getAllEnabledModels() {
		ArrayList result = new ArrayList();
		for (int i = 0; i < models.size(); i++) {
			IPluginModelBase model = (IPluginModelBase) models.get(i);
			if (model.isEnabled())
				result.add(model);
		}
		for (int i = 0; i < fmodels.size(); i++) {
			IPluginModelBase fmodel = (IPluginModelBase) fmodels.get(i);
			if (fmodel.isEnabled())
				result.add(fmodel);
		}
		return (IPluginModelBase[]) result.toArray(new IPluginModelBase[result.size()]);
	}
	

	public IPluginModel[] getPluginModels() {
		return (IPluginModel[]) models.toArray(new IPluginModel[models.size()]);
	}

	public boolean hasEnabledModels() {
		for (int i = 0; i < models.size(); i++) {
			if (((IPluginModel)models.get(i)).isEnabled())
				return true;
		}
		return false;
	}
	
	private void initializeAllModels() {
		Preferences pref = PDECore.getDefault().getPluginPreferences();
		String saved = pref.getString(ICoreConstants.CHECKED_PLUGINS);
		if (saved.equals(ICoreConstants.VALUE_SAVED_ALL))
			enableAll();
		else if (!saved.equals(ICoreConstants.VALUE_SAVED_NONE)) {
			Vector list = createSavedList(saved);
			for (int i = 0; i < models.size(); i++) {
				IPluginModel model = (IPluginModel) models.get(i);
				model.setEnabled(!list.contains(model.getPlugin().getId()));
			}
			for (int i = 0; i < fmodels.size(); i++) {
				IFragmentModel fmodel = (IFragmentModel) fmodels.get(i);
				fmodel.setEnabled(!list.contains(fmodel.getFragment().getId()));
			}
		}

	}

	private void loadModels(IProgressMonitor monitor) {
		Preferences pref = PDECore.getDefault().getPluginPreferences();
		String[] pluginPaths =
			PluginPathFinder.getPluginPaths(
				pref.getString(ICoreConstants.PLATFORM_PATH));
		EclipseHomeInitializer.resetEclipseHomeVariables();

		if (pref.getString(ICoreConstants.TARGET_MODE).equals(ICoreConstants.VALUE_USE_THIS)
			  && !Platform.isRunningOSGi())
			RegistryLoader.reloadFromLive(models, fmodels, monitor);
		else
			RegistryLoader.reload(pluginPaths, models, fmodels, monitor);
		initializeAllModels();
		initialized=true;
	}
	
	public boolean isInitialized() {
		return initialized;
	}
	
	public void removeModelProviderListener(IModelProviderListener listener) {
		listeners.remove(listener);
	}
			
	public void resetModels(Vector models, Vector fmodels) {
		this.models = models;
		this.fmodels = fmodels;
	}
	
	public void shutdown() {
		int disabled = 0;
		StringBuffer saved = new StringBuffer();
		for (int i = 0; i < models.size(); i++) {
			IPluginModel model = (IPluginModel)models.get(i);
			if (!model.isEnabled()) {
				disabled += 1;
				if (saved.length() > 0) saved.append(" ");
				saved.append(model.getPlugin().getId());
			}
		}
		for (int i = 0; i < fmodels.size(); i++) {
			IFragmentModel fmodel = (IFragmentModel)fmodels.get(i);
			if (!fmodel.isEnabled()) {
				disabled += 1;
				if (saved.length() > 0) saved.append(" ");
				saved.append(fmodel.getFragment().getId());
			}
		}
		
		Preferences pref= PDECore.getDefault().getPluginPreferences();
		if (disabled == 0) {
			pref.setValue(ICoreConstants.CHECKED_PLUGINS, ICoreConstants.VALUE_SAVED_ALL);
		} else if (disabled == models.size() + fmodels.size()) {
			pref.setValue(
				ICoreConstants.CHECKED_PLUGINS,
				ICoreConstants.VALUE_SAVED_NONE);
		} else {
			pref.setValue(ICoreConstants.CHECKED_PLUGINS, saved.toString());
		}
		
		PDECore.getDefault().savePluginPreferences();
	}
	
}
