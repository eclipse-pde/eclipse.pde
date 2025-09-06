/*******************************************************************************
 * Copyright (c) 2009, 2015 EclipseSource Corporation, IBM Corporation, and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     EclipseSource Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.launching;

/**
 * Listing of constants used in PDE preferences for launching.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ILaunchingPreferenceConstants {

	public static final String VALUE_JUNIT_LAUNCH_WITH_TESTPLUGIN = "testPluginOnly"; //$NON-NLS-1$
	public static final String VALUE_JUNIT_LAUNCH_WITH_ALL = "allWorkspacePlugins"; //$NON-NLS-1$

	// Main preference page
	public static final String PROP_AUTO_MANAGE = "Preferences.Launching.automanageDependencies"; //$NON-NLS-1$
	public static final String PROP_AUTO_MANAGE_EXTENSIBLE_FRAGMENTS = "Preferences.Launching.automanageDependencies.includeExtensibleFragments"; //$NON-NLS-1$
	public static final String PROP_AUTO_MANAGE_PLATFORM_FRAGMENTS = "Preferences.Launching.automanageDependencies.includePlatformFragments"; //$NON-NLS-1$
	public static final String PROP_RUNTIME_WORKSPACE_LOCATION = "Preferences.Launching.runtimeWorkspaceLocation"; //$NON-NLS-1$
	public static final String PROP_RUNTIME_WORKSPACE_LOCATION_IS_CONTAINER = "Preferences.Launching.runtimeWorkspaceLocationIsContainer"; //$NON-NLS-1$
	public static final String PROP_JUNIT_WORKSPACE_LOCATION = "Preferences.Launching.junitWorkspaceLocation"; //$NON-NLS-1$
	public static final String PROP_JUNIT_LAUNCH_WITH = "Preferences.Launching.junitLaunchWith"; //$NON-NLS-1$
	public static final String PROP_JUNIT_AUTO_INCLUDE = "Preferences.Launching.junitAutoInclude"; //$NON-NLS-1$
	public static final String PROP_JUNIT_INCLUDE_OPTIONAL = "Preferences.Launching.junitIncludeOptional"; //$NON-NLS-1$
	public static final String PROP_JUNIT_ADD_NEW_WORKSPACE_PLUGINS = "Preferences.Launching.junitAddNewWorkspacePlugins"; //$NON-NLS-1$
	public static final String PROP_JUNIT_VALIDATE_LAUNCH = "Preferences.Launching.junitValidateLaunch"; //$NON-NLS-1$
	public static final String PROP_JUNIT_WORKSPACE_LOCATION_IS_CONTAINER = "Preferences.Launching.junitWorkspaceLocationIsContainer"; //$NON-NLS-1$
	/**
	 * Boolean preference whether add
	 * '-Dorg.eclipse.swt.graphics.Resource.reportNonDisposed=true' to VM
	 * arguments when creating a new launch configuration
	 */
	public static final String ADD_SWT_NON_DISPOSAL_REPORTING = "Preferences.Launching.addSwtNonDisposalReporting";//$NON-NLS-1$

	// OSGi Frameworks
	public static final String DEFAULT_OSGI_FRAMEOWRK = "Preference.default.osgi.framework"; //$NON-NLS-1$
}
