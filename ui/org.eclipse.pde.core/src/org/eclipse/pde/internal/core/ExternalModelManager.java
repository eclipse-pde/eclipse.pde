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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.target.P2TargetUtils;
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
	}

	public void setModels(IPluginModelBase[] models) {
		fModels = models;
	}

	/**
	 * Returns the URLs of all external plug-ins referenced by PDE target platform preferences.
	 * <p>
	 * Note this method is public for testing purposes only.
	 * </p>
	 * @return URLs of all external plug-ins referenced by PDE target platform preferences.
	 */
	public URL[] getPluginPaths() {
		PDEPreferencesManager pref = PDECore.getDefault().getPreferencesManager();
		boolean addPool = false;
		String baseLocation = pref.getString(ICoreConstants.PLATFORM_PATH);
		URL[] base = null;
		if (P2TargetUtils.BUNDLE_POOL.isPrefixOf(new Path(baseLocation))) {
			// if the base platform path is part of the bundle pool, use the bundle pool
			// preference info to restore bundles selectively
			addPool = true;
			base = new URL[0];
		} else {
			base = PluginPathFinder.getPluginPaths(baseLocation);
		}

		String value = pref.getString(ICoreConstants.ADDITIONAL_LOCATIONS);
		StringTokenizer tokenizer = new StringTokenizer(value, ","); //$NON-NLS-1$

		if (!addPool && tokenizer.countTokens() == 0)
			return base;

		List extraLocations = new ArrayList(tokenizer.countTokens());
		while (tokenizer.hasMoreTokens()) {
			String location = tokenizer.nextToken();
			if (P2TargetUtils.BUNDLE_POOL.isPrefixOf(new Path(location))) {
				addPool = true;
			} else {
				File dir = new File(location, "plugins"); //$NON-NLS-1$
				if (!dir.exists() || !dir.isDirectory())
					dir = new File(location);
				extraLocations.add(dir);
			}
		}
		URL[] additional = PluginPathFinder.scanLocations((File[]) extraLocations.toArray(new File[extraLocations.size()]));
		URL[] result = append(base, additional);

		// add pooled bundles (only if part of the profile)
		if (addPool) {
			String pooled = pref.getString(ICoreConstants.POOLED_URLS);
			if (pooled != null && pooled.trim().length() > 0) {
				if (ICoreConstants.VALUE_SAVED_NONE.equals(pooled)) {
					// none
				} else {
					tokenizer = new StringTokenizer(pooled, ","); //$NON-NLS-1$
					List urls = new ArrayList(tokenizer.countTokens());
					while (tokenizer.hasMoreTokens()) {
						String fileName = tokenizer.nextToken();
						try {
							urls.add(P2TargetUtils.BUNDLE_POOL.append("plugins").append(fileName).toFile().toURL()); //$NON-NLS-1$
						} catch (MalformedURLException e) {
							PDECore.log(e);
						}
					}
					additional = (URL[]) urls.toArray(new URL[urls.size()]);
					result = append(result, additional);
				}
			}
		}

		return result;
	}

	private URL[] append(URL[] base, URL[] additional) {
		if (additional.length == 0) {
			return base;
		}
		URL[] result = new URL[base.length + additional.length];
		System.arraycopy(base, 0, result, 0, base.length);
		System.arraycopy(additional, 0, result, base.length, additional.length);
		return result;
	}
}
