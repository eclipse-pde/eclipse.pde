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
package org.eclipse.pde.api.tools.ui.internal.use;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;

/**
 * Delegate to launch an API use scan
 */
public class ApiUseLaunchDelegate extends LaunchConfigurationDelegate {
	
	/**
	 * The source of bundles being searched - one of target definition, API baseline, or installation path.
	 */
	public static final String TARGET_KIND = ApiUIPlugin.PLUGIN_ID + ".TARGET_KIND"; //$NON-NLS-1$
	
	/**
	 * Sources of bundles (possible values for BUNDLE_SOURCE).
	 */
	public static final int KIND_TARGET_DEFINITION = 1;
	public static final int KIND_API_BASELINE = 2;
	public static final int KIND_INSTALL_PATH = 3;
	public static final int KIND_WORKSPACE = 4;
	
	/**
	 * Target definition handle
	 */
	public static final String TARGET_HANDLE = ApiUIPlugin.PLUGIN_ID + ".TARGET_HANDLE"; //$NON-NLS-1$
	public static final String BASELINE_NAME = ApiUIPlugin.PLUGIN_ID + ".BASELINE_NAME"; //$NON-NLS-1$
	public static final String INSTALL_PATH = ApiUIPlugin.PLUGIN_ID + ".INSTALL_PATH"; //$NON-NLS-1$
	
	/**
	 * Scope of bundles to search - a regular expression to match against bundle symbolic names.
	 * Unspecified indicates all bundles in the bundle source.
	 */
	public static final String SEARCH_SCOPE = ApiUIPlugin.PLUGIN_ID + ".SEARCH_SCOPE"; //$NON-NLS-1$
	
	/**
	 * Scope of bundles to search for references to. Unspecified indicates all bundles.
	 */
	public static final String TARGET_SCOPE = ApiUIPlugin.PLUGIN_ID + ".TARGET_SCOPE"; //$NON-NLS-1$
	
	/**
	 * Search modifiers
	 */
	public static final String SEARCH_MODIFIERS = ApiUIPlugin.PLUGIN_ID + ".SEARCH_MODIFIERS"; //$NON-NLS-1$
	
	/**
	 * Search modifiers
	 */
	public static final int MOD_API_REFERENCES			= 1;
	public static final int MOD_INTERNAL_REFERENCES		= 1 << 1;
	public static final int CLEAN_XML					= 1 << 2;
	public static final int CLEAN_HTML					= 1 << 3;
	public static final int CREATE_HTML					= 1 << 4;
	public static final int DISPLAY_REPORT				= 1 << 5;
	
	/**
	 * Path to root directory of XML reports
	 */
	public static final String REPORT_PATH = ApiUIPlugin.PLUGIN_ID + ".XML_PATH"; //$NON-NLS-1$
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ILaunchConfigurationDelegate#launch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		ApiUseScanJob job = new ApiUseScanJob(configuration);
		job.schedule();
		DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.LaunchConfigurationDelegate#buildForLaunch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public boolean buildForLaunch(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
		return false;
	}
	
}
