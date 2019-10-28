/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.pde.launching;

import java.util.HashSet;
import java.util.Set;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;

/**
 * Initializes launch configuration attributes for newly-created OSGi Framework
 * launch configurations
 * <p>
 * Clients may instantiate or subclass this class
 * </p>
 * <p>
 * This class originally existed in 3.3 as
 * <code>org.eclipse.pde.ui.launcher.OSGiLaunchConfigurationInitializer</code>.
 * </p>
 * @since 3.6
 */
public class OSGiLaunchConfigurationInitializer {

	protected static final String DEFAULT = "default"; //$NON-NLS-1$

	/**
	 * Initializes some attributes on a newly-created launch configuration
	 *
	 * @param configuration
	 * 			the launch configuration
	 */
	public void initialize(ILaunchConfigurationWorkingCopy configuration) {
		initializeFrameworkDefaults(configuration);
		initializeBundleState(configuration);
		initializeSourcePathProvider(configuration);
	}

	/**
	 * Sets the source provider ID
	 *
	 * @param configuration
	 * 			the launch configuration
	 */
	protected void initializeSourcePathProvider(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, PDESourcePathProvider.ID);
	}

	/**
	 * Initializes the start level and auto-start attributes
	 *
	 * @param configuration
	 * 			the launch configuration
	 */
	protected void initializeFrameworkDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(IPDELauncherConstants.DEFAULT_AUTO_START, true);
		configuration.setAttribute(IPDELauncherConstants.DEFAULT_START_LEVEL, 4);
	}

	/**
	 * Initializes the checked/unchecked state of bundles
	 *
	 * @param configuration
	 * 			the launch configuration
	 */
	protected void initializeBundleState(ILaunchConfigurationWorkingCopy configuration) {
		Set<String> targetBundles = new HashSet<>();
		Set<String> workspaceBundles = new HashSet<>();
		IPluginModelBase[] models = PluginRegistry.getActiveModels();
		for (IPluginModelBase model : models) {
			boolean inWorkspace = model.getUnderlyingResource() != null;
			appendBundle(inWorkspace ? workspaceBundles : targetBundles, model);
		}
		configuration.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES, workspaceBundles);
		configuration.setAttribute(IPDELauncherConstants.SELECTED_TARGET_BUNDLES, targetBundles);
		configuration.setAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true);
	}

	private void appendBundle(Set<String> bundleSet, IPluginModelBase model) {
		String id = model.getPluginBase().getId();
		String value = BundleLauncherHelper.writeBundleEntry(model, getStartLevel(id), getAutoStart(id));
		bundleSet.add(value);
	}

	/**
	 * Returns the bundle's start level
	 *
	 * @param bundleID
	 * 			the bundle ID
	 * @return the start level for the given bundle or the string <code>default</code>
	 */
	protected String getStartLevel(String bundleID) {
		return DEFAULT;
	}

	/**
	 * Returns whether the bundle should be started automatically
	 * @param bundleID
	 * 			the bundle ID
	 * @return <code>true</code>, <code>false</code>, or <code>default</code>
	 */
	protected String getAutoStart(String bundleID) {
		return DEFAULT;
	}

}
