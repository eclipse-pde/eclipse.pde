/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.pde.core.IPluginSourcePathLocator;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.internal.core.target.TargetPlatformService;

/**
 * Investigates the current active target if a matching source bundle can be
 * found.
 */
public class TargetPlatformPluginSourcePathLocator implements IPluginSourcePathLocator {

	@Override
	public IPath locateSource(IPluginBase plugin) {
		// all bundles of the current target
		Iterator<TargetBundle> bundles = getBundles();
		// a map from the source target to the source bundle
		Map<BundleInfo, TargetBundle> sourceBundles = new HashMap<>();

		while (bundles.hasNext()) {
			TargetBundle bundle = bundles.next();
			if (bundle.isSourceBundle()) {
				// collect it in the map ...
				BundleInfo sourceTarget = bundle.getSourceTarget();
				if (sourceTarget != null) {
					sourceBundles.put(sourceTarget, bundle);
				}
			} else {
				BundleInfo bundleInfo = bundle.getBundleInfo();
				if (plugin.getId().equals(bundleInfo.getSymbolicName())
						&& plugin.getVersion().equals(bundleInfo.getVersion())) {
					// first check if we already have seen this source bundle
					// ...
					TargetBundle sourceBundle = sourceBundles.get(bundleInfo);
					if (sourceBundle != null) {
						return getBundlePath(sourceBundle.getBundleInfo());
					}
					// if not we need to look further...
					return findSourceBundle(bundles, bundle);
				}
			}

		}
		return null;
	}

	private static IPath findSourceBundle(Iterator<TargetBundle> bundles, TargetBundle bundle) {
		while (bundles.hasNext()) {
			if (bundle.isSourceBundle()) {
				BundleInfo sourceTarget = bundle.getSourceTarget();
				if (bundle.getBundleInfo().equals(sourceTarget)) {
					return getBundlePath(bundle.getBundleInfo());
				}
			}
		}
		return null;
	}

	/**
	 * Extracts a path from the bundle info if possible.
	 *
	 * @param bundleInfo
	 *            the bundle info to use, might be <code>null</code>
	 * @return the extracted path or <code>null</code> if no such path can be
	 *         computed
	 */
	private static IPath getBundlePath(BundleInfo bundleInfo) {
		if (bundleInfo == null) {
			return null;
		}
		URI location = bundleInfo.getLocation();
		if (location == null) {
			return null;
		}
		try {
			File file = new File(location);
			return new Path(file.getAbsolutePath());
		} catch (IllegalArgumentException e) {
			// not a local file uri...
			return null;
		}
	}

	/**
	 * @return an iterator over all current target bundles
	 */
	private static Iterator<TargetBundle> getBundles() {
		ITargetPlatformService service = TargetPlatformService.getDefault();
		try {
			ITargetDefinition definition = service.getWorkspaceTargetDefinition();
			if (definition.isResolved()) {
				TargetBundle[] bundles = definition.getAllBundles();
				if (bundles != null) {
					return Arrays.stream(bundles).iterator();
				}
			}
		} catch (CoreException e) {
			// can't do anything then here...
		}
		return Collections.emptyIterator();
	}

}
