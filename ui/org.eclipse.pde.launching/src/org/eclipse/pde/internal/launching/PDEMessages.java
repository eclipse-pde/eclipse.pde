/*******************************************************************************
 * Copyright (c) 2009 EclipseSource Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     EclipseSource Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.launching;

import org.eclipse.osgi.util.NLS;

public class PDEMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.pde.internal.launching.pderesources";//$NON-NLS-1$

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, PDEMessages.class);
	}

	public static String LauncherUtils_cannotLaunchApplication;

	public static String Launcher_error_code13;
	public static String Launcher_error_code15;

	public static String EclipsePluginValidationOperation_pluginMissing;
	public static String PluginValidation_error;

	public static String WorkbenchLauncherConfigurationDelegate_noJRE;
	public static String WorkbenchLauncherConfigurationDelegate_jrePathNotFound;
	public static String WorkbenchLauncherConfigurationDelegate_badFeatureSetup;
	public static String WorkbenchLauncherConfigurationDelegate_noStartup;
	public static String JUnitLaunchConfiguration_error_notaplugin;
	public static String JUnitLaunchConfiguration_error_missingPlugin;

	public static String OSGiLaunchConfiguration_cannotFindLaunchConfiguration;
	public static String OSGiLaunchConfiguration_selected;

	public static String EquinoxLaunchConfiguration_oldTarget;

	public static String VMHelper_cannotFindExecEnv;
}