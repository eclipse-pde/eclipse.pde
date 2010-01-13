/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.io.File;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.site.PluginPathFinder;
import org.eclipse.pde.internal.core.ExternalFeatureModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.ITargetPlatformService;

/**
 * A container of the bundles contained in a feature.
 * 
 * @since 3.5
 */
public class FeatureBundleContainer extends DirectoryBundleContainer {

	/**
	 * Feature symbolic name 
	 */
	private String fId;

	/**
	 * Feature version or <code>null</code>
	 */
	private String fVersion;

	/**
	 * Constructs a new feature bundle container for the feature at the specified
	 * location. Plug-ins are resolved in the plug-ins directory of the given home
	 * directory. When version is unspecified, the most recent version is used.
	 * 
	 * @param home root directory containing the features directory which
	 *  may contain string substitution variables
	 * @param name feature symbolic name
	 * @param version feature version, or <code>null</code> if unspecified
	 */
	FeatureBundleContainer(String home, String name, String version) {
		super(home);
		fId = name;
		fVersion = version;
	}

	/**
	 * Returns the symbolic name of the feature this bundle container resolves from
	 * 
	 * @return string feature id (symbolic name)
	 */
	public String getFeatureId() {
		return fId;
	}

	/**
	 * Returns the version of the feature this bundle container resolves from if
	 * a version was specified.
	 * 
	 * @return string feature version or <code>null</code>
	 */
	public String getFeatureVersion() {
		return fVersion;
	}

	public InstallableUnitDescription[] getRootIUs() throws CoreException {
		InstallableUnitDescription[] allUnits = super.getRootIUs();
		if (allUnits.length == 0) {
			return allUnits;
		}

		IFeatureModel model = null;
		try {

			File location = resolveFeatureLocation();
			File manifest = new File(location, "feature.xml"); //$NON-NLS-1$
			if (!manifest.exists() || !manifest.isFile()) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.FeatureBundleContainer_2, fId)));
			}
			model = ExternalFeatureModelManager.createModel(manifest);
			if (model == null || !model.isLoaded()) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.FeatureBundleContainer_2, fId)));
			}
			// search bundles in plug-ins directory
			ITargetPlatformService service = (ITargetPlatformService) PDECore.getDefault().acquireService(ITargetPlatformService.class.getName());
			if (service == null) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.FeatureBundleContainer_4));
			}
			File dir = new File(manifest.getParentFile().getParentFile().getParentFile(), "plugins"); //$NON-NLS-1$
			if (!dir.exists() || !dir.isDirectory()) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.FeatureBundleContainer_5, fId)));
			}

			IFeature feature = model.getFeature();
			IFeaturePlugin[] plugins = feature.getPlugins();
			Map includedPlugins = new HashMap();
			for (int i = 0; i < plugins.length; i++) {
				IFeaturePlugin plugin = plugins[i];
				includedPlugins.put(plugin.getId(), plugin.getVersion());
			}

			List featureUnits = new ArrayList(includedPlugins.size());
			for (int i = 0; i < allUnits.length; i++) {
				String unitID = allUnits[i].getId();
				if (includedPlugins.containsKey(unitID)) {
					String pluginVersion = (String) includedPlugins.get(unitID);
					if (pluginVersion == null || Version.create(pluginVersion).equals(allUnits[i].getVersion())) {
						featureUnits.add(allUnits[i]);
					}
				}
			}

			return (InstallableUnitDescription[]) featureUnits.toArray(new InstallableUnitDescription[featureUnits.size()]);

		} finally {
			if (model != null) {
				model.dispose();
			}
		}

	}

	/**
	 * Resolves and returns the directory containing the feature.
	 * 
	 * @return feature directory
	 * @throws CoreException if unable to resolve
	 */
	private File resolveFeatureLocation() throws CoreException {
		IPath home = new Path(getLocation(true));
		File[] featurePaths = PluginPathFinder.getFeaturePaths(home.toOSString());
		if (featurePaths.length == 0) {
			// no features are included with the install/home location
			IPath path = home.append("features"); //$NON-NLS-1$
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.FeatureBundleContainer_0, path.toOSString())));
		}
		// if a specific version is specified, find it
		if (fVersion != null) {
			StringBuffer buf = new StringBuffer();
			String name = buf.append(fId).append("_").append(fVersion).toString(); //$NON-NLS-1$
			for (int i = 0; i < featurePaths.length; i++) {
				File feature = featurePaths[i];
				if (feature.getName().equals(name)) {
					return feature;
				}
			}
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.FeatureBundleContainer_1, fId)));
		}
		// use most recent version
		List versions = new ArrayList();
		StringBuffer buf = new StringBuffer();
		String prefix = buf.append(fId).append("_").toString(); //$NON-NLS-1$
		for (int i = 0; i < featurePaths.length; i++) {
			String name = featurePaths[i].getName();
			if (name.startsWith(prefix)) {
				versions.add(featurePaths[i]);
			}
		}
		if (versions.isEmpty()) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.FeatureBundleContainer_1, fId)));
		}
		Collections.sort(versions, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((File) o1).getName().compareTo(((File) o2).getName());
			}
		});
		return (File) versions.get(versions.size() - 1);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.DirectoryBundleContainer#isContentEqual(org.eclipse.pde.internal.core.target.provisional.IBundleContainer)
	 */
	public boolean isContentEqual(IBundleContainer container) {
		if (container instanceof FeatureBundleContainer) {
			FeatureBundleContainer fbc = (FeatureBundleContainer) container;
			return fId.equals(fbc.fId) && isNullOrEqual(fVersion, fVersion) && super.isContentEqual(container);
		}
		return false;
	}

	private boolean isNullOrEqual(Object o1, Object o2) {
		if (o1 == null) {
			return o2 == null;
		}
		if (o2 == null) {
			return false;
		}
		return o1.equals(o2);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("Feature ").append(fId).append(' ').append(fVersion);
		try {
			buf.append(' ').append(getLocation(false));
		} catch (CoreException e) {
			// Ignore during toString()
		}
		return buf.toString();
	}

}
