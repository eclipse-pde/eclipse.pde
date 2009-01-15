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
package org.eclipse.pde.internal.core.target.impl;

import java.io.File;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.osgi.util.NLS;
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
public class FeatureBundleContainer extends AbstractBundleContainer {

	/**
	 * Constant describing the type of bundle container 
	 */
	public static final String TYPE = "Feature"; //$NON-NLS-1$

	/**
	 * Feature symbolic name 
	 */
	private String fId;

	/**
	 * Feature version or <code>null</code>
	 */
	private String fVersion;

	/**
	 * Install location which may contain string substitution variables
	 */
	private String fHome;

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
		fId = name;
		fVersion = version;
		fHome = home;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#getHomeLocation()
	 */
	public String getHomeLocation() throws CoreException {
		return resolveHomeLocation().toOSString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer#getType()
	 */
	public String getType() {
		return TYPE;
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

	/**
	 * Returns the home location with all variables resolved as a path.
	 * 
	 * @return resolved home location
	 * @throws CoreException
	 */
	private IPath resolveHomeLocation() throws CoreException {
		return new Path(resolveVariables(fHome));
	}

	/**
	 * Resolves and returns the directory containing the feature.
	 * 
	 * @return feature directory
	 * @throws CoreException if unable to resolve
	 */
	private File resolveFeatureLocation() throws CoreException {
		File features = resolveHomeLocation().append("features").toFile(); //$NON-NLS-1$
		if (!features.exists() || features.isFile()) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.FeatureBundleContainer_0, features.toString())));
		}
		// if a specific version is specified, use it
		if (fVersion != null) {
			StringBuffer buf = new StringBuffer();
			String name = buf.append(fId).append("_").append(fVersion).toString(); //$NON-NLS-1$
			return new File(features, name);
		}
		// use most recent version
		String[] list = features.list();
		List versions = new ArrayList();
		StringBuffer buf = new StringBuffer();
		String prefix = buf.append(fId).append("_").toString(); //$NON-NLS-1$
		for (int i = 0; i < list.length; i++) {
			String name = list[i];
			if (name.startsWith(prefix)) {
				versions.add(name);
			}
		}
		if (versions.isEmpty()) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.FeatureBundleContainer_1, fId)));
		}
		Collections.sort(versions);
		String name = (String) versions.get(versions.size() - 1);
		return new File(features, name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer#resolveAllBundles(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected BundleInfo[] resolveAllBundles(IProgressMonitor monitor) throws CoreException {
		return resolveBundles0(monitor, false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer#resolveAllSourceBundles(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected BundleInfo[] resolveAllSourceBundles(IProgressMonitor monitor) throws CoreException {
		return resolveBundles0(monitor, true);
	}

	private BundleInfo[] resolveBundles0(IProgressMonitor monitor, boolean source) throws CoreException {
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
			IBundleContainer container = service.newDirectoryContainer(dir.getAbsolutePath());
			BundleInfo[] bundles = null;
			if (source) {
				bundles = container.resolveSourceBundles(null);
			} else {
				bundles = container.resolveBundles(null);
			}
			Map bundleMap = new HashMap();
			for (int i = 0; i < bundles.length; i++) {
				BundleInfo info = bundles[i];
				List list = (List) bundleMap.get(info.getSymbolicName());
				if (list == null) {
					list = new ArrayList();
					bundleMap.put(info.getSymbolicName(), list);
				}
				list.add(info);
			}
			IFeature feature = model.getFeature();
			IFeaturePlugin[] plugins = feature.getPlugins();
			List results = new ArrayList();
			for (int i = 0; i < plugins.length; i++) {
				IFeaturePlugin plugin = plugins[i];
				List list = (List) bundleMap.get(plugin.getId());
				if (list != null) {
					Iterator iterator = list.iterator();
					boolean added = false;
					while (iterator.hasNext()) {
						BundleInfo info = (BundleInfo) iterator.next();
						if (info.getVersion().equals(plugin.getVersion())) {
							results.add(info);
							added = true;
							break;
						}
					}
					if (!added) {
						// use first one
						results.add(list.get(0));
					}
				} else {
					// TODO: missing plug-in, we should probably include a status with resolution
				}

			}
			return (BundleInfo[]) results.toArray(new BundleInfo[results.size()]);
		} finally {
			if (model != null) {
				model.dispose();
			}
		}
	}

}
