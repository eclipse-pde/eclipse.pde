/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.target.AbstractTargetHandle;
import org.eclipse.pde.internal.core.target.provisional.NameVersionDescriptor;

public class ExternalModelManager extends AbstractModelManager {

	private IPluginModelBase[] fModels = new IPluginModelBase[0];

	protected IPluginModelBase[] getAllModels() {
		return fModels;
	}

	protected void initializeModels(IPluginModelBase[] models) {
		fModels = models;
		PDEPreferencesManager pref = PDECore.getDefault().getPreferencesManager();
		String saved = pref.getString(ICoreConstants.CHECKED_PLUGINS);
		if (saved.equals(ICoreConstants.VALUE_SAVED_ALL)) {
			for (int i = 0; i < fModels.length; i++)
				fModels[i].setEnabled(true);
		} else if (!saved.equals(ICoreConstants.VALUE_SAVED_NONE)) {
			String versionString = pref.getString(ICoreConstants.CHECKED_VERSION_PLUGINS);
			Set versions = new HashSet();
			Set versionIds = new HashSet();
			if (versionString != null && versionString.trim().length() > 0) {
				if (!versionString.equals(ICoreConstants.VALUE_SAVED_NONE)) {
					// restore version information, if any
					StringTokenizer stok = new StringTokenizer(versionString);
					while (stok.hasMoreTokens()) {
						NameVersionDescriptor desc = NameVersionDescriptor.fromPortableString(stok.nextToken());
						versions.add(desc);
						versionIds.add(desc.getId());
					}
				}
			}
			Vector result = new Vector();
			StringTokenizer stok = new StringTokenizer(saved);
			while (stok.hasMoreTokens()) {
				result.add(stok.nextToken());
			}
			for (int i = 0; i < fModels.length; i++) {
				String id = fModels[i].getPluginBase().getId();
				if (versionIds.contains(id)) {
					fModels[i].setEnabled(!versions.contains(new NameVersionDescriptor(id, fModels[i].getPluginBase().getVersion())));
				} else {
					fModels[i].setEnabled(!result.contains(id));
				}
			}
		}
		// enable pooled bundles properly (only if part of the profile)
		String pooled = pref.getString(ICoreConstants.POOLED_BUNDLES);
		if (pooled != null && pooled.trim().length() > 0) {
			if (ICoreConstants.VALUE_SAVED_NONE.equals(pooled)) {
				// all pooled bundles are disabled
				for (int i = 0; i < fModels.length; i++) {
					if (AbstractTargetHandle.BUNDLE_POOL.isPrefixOf(new Path(fModels[i].getInstallLocation()))) {
						fModels[i].setEnabled(false);
					}
				}
			} else {
				StringTokenizer tokenizer = new StringTokenizer(pooled, ","); //$NON-NLS-1$
				Set enabled = new HashSet();
				while (tokenizer.hasMoreTokens()) {
					String id = tokenizer.nextToken();
					if (tokenizer.hasMoreTokens()) {
						String ver = tokenizer.nextToken();
						if (ICoreConstants.VALUE_SAVED_NONE.equals(ver)) { // indicates null version
							ver = null;
						}
						enabled.add(new NameVersionDescriptor(id, ver));
					}
				}
				for (int i = 0; i < fModels.length; i++) {
					if (AbstractTargetHandle.BUNDLE_POOL.isPrefixOf(new Path(fModels[i].getInstallLocation()))) {
						IPluginBase base = fModels[i].getPluginBase();
						NameVersionDescriptor desc = new NameVersionDescriptor(base.getId(), base.getVersion());
						fModels[i].setEnabled(enabled.contains(desc));
					}
				}
			}
		}
	}

	public void setModels(IPluginModelBase[] models) {
		fModels = models;
	}

	protected URL[] getPluginPaths() {
		PDEPreferencesManager pref = PDECore.getDefault().getPreferencesManager();
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
