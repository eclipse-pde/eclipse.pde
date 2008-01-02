/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
import java.net.URL;
import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.pde.core.plugin.IPluginModelBase;

public class ExternalModelManager extends AbstractModelManager {

	private IPluginModelBase[] fModels = new IPluginModelBase[0];

	protected IPluginModelBase[] getAllModels() {
		return fModels;
	}

	protected void initializeModels(IPluginModelBase[] models) {
		fModels = models;
		Preferences pref = PDECore.getDefault().getPluginPreferences();
		String saved = pref.getString(ICoreConstants.CHECKED_PLUGINS);
		if (saved.equals(ICoreConstants.VALUE_SAVED_ALL)) {
			for (int i = 0; i < fModels.length; i++)
				fModels[i].setEnabled(true);
		} else if (!saved.equals(ICoreConstants.VALUE_SAVED_NONE)) {
			Vector result = new Vector();
			StringTokenizer stok = new StringTokenizer(saved);
			while (stok.hasMoreTokens()) {
				result.add(stok.nextToken());
			}
			for (int i = 0; i < fModels.length; i++) {
				fModels[i].setEnabled(!result.contains(fModels[i].getPluginBase().getId()));
			}
		}
	}

	public void setModels(IPluginModelBase[] models) {
		fModels = models;
	}

	protected URL[] getPluginPaths() {
		Preferences pref = PDECore.getDefault().getPluginPreferences();
		URL[] base = PluginPathFinder.getPluginPaths(pref.getString(ICoreConstants.PLATFORM_PATH));

		String value = pref.getString(ICoreConstants.ADDITIONAL_LOCATIONS);
		StringTokenizer tokenizer = new StringTokenizer(value, ","); //$NON-NLS-1$

		if (tokenizer.countTokens() == 0)
			return base;

		File[] extraLocations = new File[tokenizer.countTokens()];
		for (int i = 0; i < extraLocations.length; i++) {
			String location = tokenizer.nextToken();
			File dir = new File(location, "plugins"); //$NON-NLS-1$
			if (!dir.exists() || !dir.isDirectory())
				dir = new File(location);
			extraLocations[i] = dir;
		}
		URL[] additional = PluginPathFinder.scanLocations(extraLocations);

		if (additional.length == 0)
			return base;

		URL[] result = new URL[base.length + additional.length];
		System.arraycopy(base, 0, result, 0, base.length);
		System.arraycopy(additional, 0, result, base.length, additional.length);

		return result;
	}

}
