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
import java.net.URL;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.pde.core.IModelProviderEvent;
import org.eclipse.pde.core.IModelProviderListener;
import org.eclipse.pde.core.plugin.IPluginModelBase;

public class ExternalModelManager {
	
	private Vector fListeners = new Vector();	
	private IPluginModelBase[] fModels = new IPluginModelBase[0];
	

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
		for (int i = 0; i < fModels.length; i++)
			fModels[i].setEnabled(true);	
	}

	public void fireModelProviderEvent(IModelProviderEvent e) {
		for (Iterator iter = fListeners.iterator(); iter.hasNext();) {
			IModelProviderListener listener = (IModelProviderListener) iter.next();
			listener.modelsChanged(e);
		}
	}

	protected IPluginModelBase[] getAllModels() {
		return fModels;
	}
	
	protected void initializeModels(IPluginModelBase[] models) {
		fModels = models;
		Preferences pref = PDECore.getDefault().getPluginPreferences();
		String saved = pref.getString(ICoreConstants.CHECKED_PLUGINS);
		if (saved.equals(ICoreConstants.VALUE_SAVED_ALL))
			enableAll();
		else if (!saved.equals(ICoreConstants.VALUE_SAVED_NONE)) {
			Vector list = createSavedList(saved);
			for (int i = 0; i < fModels.length; i++) {
				fModels[i].setEnabled(!list.contains(fModels[i].getPluginBase().getId()));
			}
		}
	}
	
	protected void setModels(IPluginModelBase[] models) {
		fModels = models;
	}

	public static URL[] getPluginPaths() {
		Preferences pref = PDECore.getDefault().getPluginPreferences();
		return PluginPathFinder.getPluginPaths(pref.getString(ICoreConstants.PLATFORM_PATH));	
	}
	
	public void removeModelProviderListener(IModelProviderListener listener) {
		fListeners.remove(listener);
	}
			
	public void shutdown() {
		int disabled = 0;
		StringBuffer saved = new StringBuffer();
		for (int i = 0; i < fModels.length; i++) {
			IPluginModelBase model = fModels[i];
			if (!model.isEnabled()) {
				disabled += 1;
				if (saved.length() > 0) saved.append(" "); //$NON-NLS-1$
				saved.append(model.getPluginBase().getId());
			}
		}

		Preferences pref= PDECore.getDefault().getPluginPreferences();
		if (disabled == 0) {
			pref.setValue(ICoreConstants.CHECKED_PLUGINS, ICoreConstants.VALUE_SAVED_ALL);
		} else if (disabled == fModels.length) {
			pref.setValue(
				ICoreConstants.CHECKED_PLUGINS,
				ICoreConstants.VALUE_SAVED_NONE);
		} else {
			pref.setValue(ICoreConstants.CHECKED_PLUGINS, saved.toString());
		}
		
		PDECore.getDefault().savePluginPreferences();
	}
	
}
