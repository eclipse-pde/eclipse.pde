/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.core.target.TargetFeature;
import org.eclipse.pde.internal.core.ExternalFeatureModelManager;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;

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
	private final String fId;

	/**
	 * Feature version or <code>null</code>
	 */
	private final String fVersion;

	/**
	 * Install location which may contain string substitution variables
	 */
	private final String fHome;

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

	@Override
	public String getLocation(boolean resolve) throws CoreException {
		if (resolve) {
			return resolveHomeLocation().toOSString();
		}
		return fHome;
	}

	@Override
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

	@Override
	protected TargetBundle[] resolveBundles(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException {
		IFeatureModel model = null;
		try {
			if (monitor.isCanceled()) {
				return new TargetBundle[0];
			}

			TargetFeature[] features = resolveFeatures(definition, null);
			if (features.length == 0) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.FeatureBundleContainer_1, fId)));
			}
			File location = new File(features[0].getLocation());
			if (!location.exists()) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.FeatureBundleContainer_0, location.toString())));
			}
			File manifest = new File(location, ICoreConstants.FEATURE_FILENAME_DESCRIPTOR);
			if (!manifest.exists() || !manifest.isFile()) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.FeatureBundleContainer_2, fId)));
			}
			model = ExternalFeatureModelManager.createModel(manifest);
			if (model == null || !model.isLoaded()) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.FeatureBundleContainer_2, fId)));
			}
			// search bundles in plug-ins directory
			ITargetPlatformService service = PDECore.getDefault().acquireService(ITargetPlatformService.class);
			if (service == null) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.FeatureBundleContainer_4));
			}
//			File dir = new File(manifest.getParentFile().getParentFile().getParentFile(), "plugins"); //$NON-NLS-1$
//			if (!dir.exists() || !dir.isDirectory()) {
//				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.FeatureBundleContainer_5, fId)));
//			}
			if (monitor.isCanceled()) {
				return new TargetBundle[0];
			}

			ITargetLocation container = service.newProfileLocation(getLocation(false), null);
			container.resolve(definition, monitor);
			TargetBundle[] bundles = container.getBundles();
			IFeature feature = model.getFeature();
			IFeaturePlugin[] plugins = feature.getPlugins();
			List<NameVersionDescriptor> matchInfos = new ArrayList<>(plugins.length);
			for (IFeaturePlugin plugin : plugins) {
				if (monitor.isCanceled()) {
					return new TargetBundle[0];
				}
				// only include if plug-in matches environment
				if (isMatch(definition.getArch(), plugin.getArch(), Platform.getOSArch()) && isMatch(definition.getNL(), plugin.getNL(), Platform.getNL()) && isMatch(definition.getOS(), plugin.getOS(), Platform.getOS()) && isMatch(definition.getWS(), plugin.getWS(), Platform.getWS())) {
					matchInfos.add(new NameVersionDescriptor(plugin.getId(), plugin.getVersion()));
				}
			}

			List<?> result = TargetDefinition.getMatchingBundles(bundles, matchInfos.toArray(new NameVersionDescriptor[matchInfos.size()]), true);
			return result.toArray(new TargetBundle[result.size()]);
		} finally {
			if (model != null) {
				model.dispose();
			}
		}
	}

	@Override
	protected TargetFeature[] resolveFeatures(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException {
		if (definition instanceof TargetDefinition) {
			TargetFeature[] allFeatures = ((TargetDefinition) definition).resolveFeatures(getLocation(false), monitor);
			for (TargetFeature allFeature : allFeatures) {
				if (allFeature.getId().equals(fId)) {
					if (fVersion == null || allFeature.getVersion().equals(fVersion)) {
						return new TargetFeature[] {allFeature};
					}
				}
			}
		}
		return new TargetFeature[0];
	}

	/**
	 * Returns whether the given target environment setting matches that of a fragments.
	 *
	 * @param targetValue value in target definition
	 * @param fragmentValue value in fragment
	 * @param runningValue value of current running platform
	 * @return whether the fragment should be considered
	 */
	private boolean isMatch(String targetValue, String fragmentValue, String runningValue) {
		if (fragmentValue == null) {
			// unspecified, so it is a match
			return true;
		}
		if (targetValue == null) {
			return runningValue.equals(fragmentValue);
		}
		return targetValue.equals(fragmentValue);
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof FeatureBundleContainer) {
			FeatureBundleContainer fbc = (FeatureBundleContainer) o;
			return fHome.equals(fbc.fHome) && fId.equals(fbc.fId) && isNullOrEqual(fVersion, fVersion);
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hash = fHome.hashCode() + fId.hashCode();
		if (fVersion != null) {
			hash += fVersion.hashCode();
		}
		return hash;
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

	@Override
	public String toString() {
		return new StringBuilder("Feature ").append(fId).append(' ').append(fVersion).append(' ').append(fHome) //$NON-NLS-1$
				.toString();
	}

	@Override
	public String[] getVMArguments() {
		return null;
	}
}
