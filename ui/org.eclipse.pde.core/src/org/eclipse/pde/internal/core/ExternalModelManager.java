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
import java.io.*;
import java.net.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;

public class ExternalModelManager {
	private List fModels;
	private List fFragmentModels;
	private Vector fListeners = new Vector();
	private PDEState fState = null;
	private boolean fInitialized = false;
	
	public ExternalModelManager() {
		fModels = Collections.synchronizedList(new ArrayList());
		fFragmentModels = Collections.synchronizedList(new ArrayList());
	}

	public static String computeDefaultPlatformPath() {
		URL installURL = Platform.getInstallLocation().getURL();
		IPath ppath = new Path(installURL.getFile()).removeTrailingSeparator();
		return getCorrectPath(ppath.toOSString());
	}
	
	public static boolean isTargetEqualToHost(String platformPath) {
		return arePathsEqual(new Path(platformPath), new Path(computeDefaultPlatformPath()));
	}
	
	private static String getCorrectPath(String path) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < path.length(); i++) {
			char c = path.charAt(i);
			if (Platform.getOS().equals("win32")) { //$NON-NLS-1$
				if (i == 0 && c == '/')
					continue;
			}
			// Some VMs may return %20 instead of a space
			if (c == '%' && i + 2 < path.length()) {
				char c1 = path.charAt(i + 1);
				char c2 = path.charAt(i + 2);
				if (c1 == '2' && c2 == '0') {
					i += 2;
					buf.append(" "); //$NON-NLS-1$
					continue;
				}
			}
			buf.append(c);
		}
		return buf.toString();
	}	
	public static IPath getEclipseHome() {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		return new Path(preferences.getString(ICoreConstants.PLATFORM_PATH));
	}

	public static boolean arePathsEqual(IPath path1, IPath path2) {
		String device = path1.getDevice();
		if (device != null)
			path1 = path1.setDevice(device.toUpperCase());
		
		device = path2.getDevice();
		if (device != null)
			path2 = path2.setDevice(device.toUpperCase());
		
		return path1.equals(path2);
	}

	public void addModelProviderListener(IModelProviderListener listener) {
		fListeners.add(listener);
	}

	private Vector createSavedList(String saved) {
		Vector result = new Vector();
		StringTokenizer stok = new StringTokenizer(saved);
		while (stok.hasMoreTokens()) {
			result.add(stok.nextToken());
		}
		return result;
	}
	
	private void enableAll() {
		for (int i = 0; i < fModels.size(); i++)
			((IPluginModel)fModels.get(i)).setEnabled(true);
			
		for (int i = 0; i < fFragmentModels.size(); i++)
			((IFragmentModel)fFragmentModels.get(i)).setEnabled(true);			
	}

	public void fireModelProviderEvent(IModelProviderEvent e) {
		for (Iterator iter = fListeners.iterator(); iter.hasNext();) {
			IModelProviderListener listener = (IModelProviderListener) iter.next();
			listener.modelsChanged(e);
		}
	}

	public IPluginModelBase[] getAllModels() {
		loadModels(new NullProgressMonitor());
		IPluginModelBase[] allModels =
			new IPluginModelBase[fModels.size() + fFragmentModels.size()];
		System.arraycopy(fModels.toArray(), 0, allModels, 0, fModels.size());
		System.arraycopy(
			fFragmentModels.toArray(),
			0,
			allModels,
			fModels.size(),
			fFragmentModels.size());

		return allModels;
	}
	
	private void initializeAllModels() {
		Preferences pref = PDECore.getDefault().getPluginPreferences();
		String saved = pref.getString(ICoreConstants.CHECKED_PLUGINS);
		if (saved.equals(ICoreConstants.VALUE_SAVED_ALL))
			enableAll();
		else if (!saved.equals(ICoreConstants.VALUE_SAVED_NONE)) {
			Vector list = createSavedList(saved);
			for (int i = 0; i < fModels.size(); i++) {
				IPluginModel model = (IPluginModel) fModels.get(i);
				model.setEnabled(!list.contains(model.getPlugin().getId()));
			}
			for (int i = 0; i < fFragmentModels.size(); i++) {
				IFragmentModel fmodel = (IFragmentModel) fFragmentModels.get(i);
				fmodel.setEnabled(!list.contains(fmodel.getFragment().getId()));
			}
		}

	}

	private synchronized void loadModels(IProgressMonitor monitor) {
		if (fInitialized)
			return;
		Preferences pref = PDECore.getDefault().getPluginPreferences();
		URL[] pluginPaths =
			PluginPathFinder.getPluginPaths(
				pref.getString(ICoreConstants.PLATFORM_PATH));
		fState = new PDEState(pluginPaths, true, monitor);
		IPluginModelBase[] resolved = fState.getModels();
		for (int i = 0; i < resolved.length; i++) {
			if (resolved[i] instanceof IPluginModel) {
				fModels.add(resolved[i]);
			} else {
				fFragmentModels.add(resolved[i]);
			}
		}		
		initializeAllModels();
		fInitialized=true;
	}
	
	public void removeModelProviderListener(IModelProviderListener listener) {
		fListeners.remove(listener);
	}
			
	public void reset(PDEState state, IPluginModelBase[] newModels) {
		fState = state;
		PDECore.getDefault().getModelManager().addWorkspaceBundlesToState();
		fModels.clear();
		fFragmentModels.clear();
		for (int i = 0; i < newModels.length; i++) {
			if (newModels[i] instanceof IPluginModel)
				fModels.add(newModels[i]);
			else
				fFragmentModels.add(newModels[i]);
		}
	}
	
	public void shutdown() {
		int disabled = 0;
		StringBuffer saved = new StringBuffer();
		for (int i = 0; i < fModels.size(); i++) {
			IPluginModel model = (IPluginModel)fModels.get(i);
			if (!model.isEnabled()) {
				disabled += 1;
				if (saved.length() > 0) saved.append(" "); //$NON-NLS-1$
				saved.append(model.getPlugin().getId());
			}
		}
		for (int i = 0; i < fFragmentModels.size(); i++) {
			IFragmentModel fmodel = (IFragmentModel)fFragmentModels.get(i);
			if (!fmodel.isEnabled()) {
				disabled += 1;
				if (saved.length() > 0) saved.append(" "); //$NON-NLS-1$
				saved.append(fmodel.getFragment().getId());
			}
		}
		
		Preferences pref= PDECore.getDefault().getPluginPreferences();
		if (disabled == 0) {
			pref.setValue(ICoreConstants.CHECKED_PLUGINS, ICoreConstants.VALUE_SAVED_ALL);
		} else if (disabled == fModels.size() + fFragmentModels.size()) {
			pref.setValue(
				ICoreConstants.CHECKED_PLUGINS,
				ICoreConstants.VALUE_SAVED_NONE);
		} else {
			pref.setValue(ICoreConstants.CHECKED_PLUGINS, saved.toString());
		}
		
		PDECore.getDefault().savePluginPreferences();
		clearStaleStates();
	}
	
	private void clearStaleStates() {
		File dir = new File(PDECore.getDefault().getStateLocation().toOSString());
		File[] children = dir.listFiles();
		if (children != null) {
			for (int i = 0; i < children.length; i++) {
				File child = children[i];
				if (child.isDirectory()) {
					String name = child.getName();
					if (name.endsWith(".cache")  //$NON-NLS-1$
							&& name.length() != 6
							&& !name.equals(Long.toString(fState.getTimestamp()) + ".cache")) { //$NON-NLS-1$
						clearDirectory(child);
					}
				}
			}
		}
	}

	private void clearDirectory(File dir) {
		File[] children = dir.listFiles();
		if (children != null) {
			for (int i = 0; i < children.length; i++) {
				children[i].delete();
			}
		}
		dir.delete();
	}

	public PDEState getState() {
		loadModels(new NullProgressMonitor());
		return fState;
	}
}
