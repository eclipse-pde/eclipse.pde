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
package org.eclipse.pde.internal.ui.wizards.imports;

import org.eclipse.pde.internal.core.target.ResolvedBundle;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.target.provisional.IResolvedBundle;

/**
 * Used to locate source when performing an import that is *not* from the active
 * target platform.
 */
public class AlternateSourceLocations extends SourceLocationManager {

	/**
	 * All models to consider as source locations.
	 */
	private IPluginModelBase[] models;

	/**
	 * Resolved bundles corresponding to models
	 */
	private IResolvedBundle[] bundles;

	/**
	 * List of source locations that reference root folders containing
	 * sub-folders of source directories. This is the old-style source
	 * plug-in.
	 */
	private List oldSourceRoots;

	/**
	 * Constructs alternate source locations on the given plug-ins.
	 * 
	 * @param plugins models to consider as source locations
	 * @param rbs corresponding resolved bundles
	 */
	public AlternateSourceLocations(IPluginModelBase[] plugins, IResolvedBundle rbs[]) {
		models = plugins;
		bundles = rbs;
	}

	/**
	 * Returns a bundle manifest location manager that knows about source bundles in the current
	 * platform.
	 * @return bundle manifest source location manager
	 */
	protected BundleManifestSourceLocationManager initializeBundleManifestLocations() {
		BundleManifestSourceLocationManager manager = new BundleManifestSourceLocationManager();
		manager.setPlugins(models);
		return manager;
	}

	/**
	 * Returns a list of source locations referencing root folders containing source.
	 * These are old-style source plug-ins that contain a sub-folder for each plug-in
	 * that source is provided for.
	 * 
	 * @return collection of old-style source locations that have been contributed via
	 * 	extension point
	 */
	public List getExtensionLocations() {
		if (oldSourceRoots == null) {
			oldSourceRoots = new ArrayList();
			for (int i = 0; i < bundles.length; i++) {
				String path = ((ResolvedBundle) bundles[i]).getSourcePath();
				if (path != null) {
					oldSourceRoots.add(new SourceLocation(new Path(models[i].getInstallLocation()).append(path)));
				}
			}
		}
		return oldSourceRoots;
	}
}
