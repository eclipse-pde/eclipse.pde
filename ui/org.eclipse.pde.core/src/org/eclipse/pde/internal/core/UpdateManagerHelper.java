/*******************************************************************************
 *  Copyright (c) 2007, 2013 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.update.configurator.ConfiguratorUtils;
import org.eclipse.update.configurator.IPlatformConfiguration;

@SuppressWarnings("deprecation")
// The update manager is deprecated, but still supported in PDE launching
public class UpdateManagerHelper {

	private static class LocalSite {
		private final ArrayList<IPluginModelBase> fPlugins;
		private IPath fPath;

		public LocalSite(IPath path) {
			if (path.getDevice() != null) {
				fPath = path.setDevice(path.getDevice().toUpperCase(Locale.ENGLISH));
			} else {
				fPath = path;
			}
			fPlugins = new ArrayList<>();
		}

		public IPath getPath() {
			return fPath;
		}

		public URL getURL() throws MalformedURLException {
			return new URL("file:" + fPath.removeTrailingSeparator()); //$NON-NLS-1$
		}

		public void add(IPluginModelBase model) {
			fPlugins.add(model);
		}

		public String[] getRelativePluginList() {
			String[] list = new String[fPlugins.size()];
			for (int i = 0; i < fPlugins.size(); i++) {
				IPluginModelBase model = fPlugins.get(i);
				IPath location = new Path(model.getInstallLocation());
				// defect 37319
				if (location.segmentCount() > 2) {
					location = location.removeFirstSegments(location.segmentCount() - 2);
				}
				//31489 - entry must be relative
				list[i] = location.setDevice(null).makeRelative().toString();
			}
			return list;
		}
	}

	public static void createPlatformConfiguration(File configLocation, IPluginModelBase[] models, IPluginModelBase brandingPlugin) throws CoreException {
		try {
			IPlatformConfiguration platformConfiguration = ConfiguratorUtils.getPlatformConfiguration(null);

			// Compute local sites
			ArrayList<LocalSite> sites = new ArrayList<>();
			for (IPluginModelBase model : models) {
				IPath path = new Path(model.getInstallLocation()).removeLastSegments(2);
				addToSite(path, model, sites);
			}

			createConfigurationEntries(platformConfiguration, sites);

			if (brandingPlugin != null) {
				createFeatureEntries(platformConfiguration, brandingPlugin);
			}

			platformConfiguration.refresh();
			platformConfiguration.save(new URL("file:" + configLocation.getPath())); //$NON-NLS-1$
		} catch (Exception e) {
			// Wrap everything else in a core exception.
			String message = e.getMessage();
			if (message == null || message.length() == 0) {
				message = PDECoreMessages.TargetPlatform_exceptionThrown;
			}
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IStatus.ERROR, message, e));
		}
	}

	private static void addToSite(IPath path, IPluginModelBase model, ArrayList<LocalSite> sites) {
		if (path.getDevice() != null) {
			path = path.setDevice(path.getDevice().toUpperCase(Locale.ENGLISH));
		}
		for (int i = 0; i < sites.size(); i++) {
			LocalSite localSite = sites.get(i);
			if (localSite.getPath().equals(path)) {
				localSite.add(model);
				return;
			}
		}
		// First time - add site
		LocalSite localSite = new LocalSite(path);
		localSite.add(model);
		sites.add(localSite);
	}

	private static void createConfigurationEntries(IPlatformConfiguration config, ArrayList<LocalSite> sites) throws MalformedURLException {

		for (int i = 0; i < sites.size(); i++) {
			LocalSite localSite = sites.get(i);
			String[] plugins = localSite.getRelativePluginList();

			int policy = IPlatformConfiguration.ISitePolicy.USER_INCLUDE;
			IPlatformConfiguration.ISitePolicy sitePolicy = config.createSitePolicy(policy, plugins);
			IPlatformConfiguration.ISiteEntry siteEntry = config.createSiteEntry(localSite.getURL(), sitePolicy);
			config.configureSite(siteEntry);
		}
		config.isTransient(true);
	}

	private static void createFeatureEntries(IPlatformConfiguration config, IPluginModelBase plugin) throws MalformedURLException {
		String id = plugin.getPluginBase().getId();
		IFeatureModel featureModel = PDECore.getDefault().getFeatureModelManager().findFeatureModel(id);
		if (featureModel != null) {
			IFeature feature = featureModel.getFeature();
			IPlatformConfiguration.IFeatureEntry featureEntry = config.createFeatureEntry(id, feature.getVersion(), id, plugin.getPluginBase().getVersion(), true, null, new URL[] {new URL("file:" + plugin.getInstallLocation())}); //$NON-NLS-1$
			config.configureFeatureEntry(featureEntry);
		}
	}

}
