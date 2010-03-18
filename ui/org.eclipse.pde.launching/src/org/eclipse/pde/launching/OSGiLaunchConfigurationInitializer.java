/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.launching;

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
		StringBuffer explugins = new StringBuffer();
		StringBuffer wsplugins = new StringBuffer();
		IPluginModelBase[] models = PluginRegistry.getActiveModels();
		for (int i = 0; i < models.length; i++) {
			boolean inWorkspace = models[i].getUnderlyingResource() != null;
			appendBundle(inWorkspace ? wsplugins : explugins, models[i]);
		}
		configuration.setAttribute(IPDELauncherConstants.WORKSPACE_BUNDLES, wsplugins.toString());
		configuration.setAttribute(IPDELauncherConstants.TARGET_BUNDLES, explugins.toString());
		configuration.setAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true);
	}

	private void appendBundle(StringBuffer buffer, IPluginModelBase model) {
		if (buffer.length() > 0)
			buffer.append(","); //$NON-NLS-1$
		String id = model.getPluginBase().getId();
		String value = BundleLauncherHelper.writeBundleEntry(model, getStartLevel(id), getAutoStart(id));
		buffer.append(value);
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
