/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
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
	public static final int KIND_HTML_ONLY = 4;
	
	/**
	 * Target definition handle
	 */
	public static final String TARGET_HANDLE = ApiUIPlugin.PLUGIN_ID + ".TARGET_HANDLE"; //$NON-NLS-1$
	public static final String BASELINE_NAME = ApiUIPlugin.PLUGIN_ID + ".BASELINE_NAME"; //$NON-NLS-1$
	public static final String INSTALL_PATH = ApiUIPlugin.PLUGIN_ID + ".INSTALL_PATH"; //$NON-NLS-1$
	
	/**
	 * Addition/overrides to API descriptions. Lists of package patterns to indicate
	 * internal or API references. 
	 */
	public static final String INTERNAL_PATTERNS_LIST = ApiUIPlugin.PLUGIN_ID + ".INTERNAL_PATTERNS_LIST"; //$NON-NLS-1$
	public static final String API_PATTERNS_LIST = ApiUIPlugin.PLUGIN_ID + ".API_PATTERNS_LIST"; //$NON-NLS-1$
	public static final String JAR_PATTERNS_LIST = ApiUIPlugin.PLUGIN_ID + ".JAR_PATTERNS_LIST"; //$NON-NLS-1$
	public static final String REPORT_PATTERNS_LIST = ApiUIPlugin.PLUGIN_ID + ".REPORT_PATTERNS_LIST"; //$NON-NLS-1$
	public static final String REPORT_TO_PATTERNS_LIST = ApiUIPlugin.PLUGIN_ID + ".TO_PATTERNS_LIST"; //$NON-NLS-1$
	
	/**
	 * Type of report to produce.  Integer value selected between possible values {@link #REPORT_KIND_CONSUMER} and
	 * {@link #REPORT_KIND_PRODUCER}.
	 */
	public static final String REPORT_TYPE = ApiUIPlugin.PLUGIN_ID + ".REPORT_TYPE"; //$NON-NLS-1$
	public static final int REPORT_KIND_PRODUCER = 1;
	public static final int REPORT_KIND_CONSUMER = 2;
	
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
	 * Path to root directory of XML reports
	 */
	public static final String REPORT_PATH = ApiUIPlugin.PLUGIN_ID + ".XML_PATH"; //$NON-NLS-1$
	
	/**
	 * Human-readable description of the report
	 */
	public static final String DESCRIPTION = ApiUIPlugin.PLUGIN_ID + ".DESCRIPTION"; //$NON-NLS-1$
	
	/**
	 * Search modifiers
	 */
	public static final int MOD_API_REFERENCES			= 1;
	public static final int MOD_INTERNAL_REFERENCES		= 1 << 1;
	public static final int MOD_ILLEGAL_USE				= 1 << 6;
	public static final int CLEAN_XML					= 1 << 2;
	public static final int CLEAN_HTML					= 1 << 3;
	public static final int CREATE_HTML					= 1 << 4;
	public static final int DISPLAY_REPORT				= 1 << 5;
	
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
