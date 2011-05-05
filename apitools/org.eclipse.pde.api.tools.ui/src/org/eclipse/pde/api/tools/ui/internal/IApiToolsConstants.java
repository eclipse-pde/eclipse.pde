/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal;

/**
 * @since 1.0.0
 */
public interface IApiToolsConstants {
	/**
	 * Empty String constant
	 */
	public static final String EMPTY_STRING = ""; //$NON-NLS-1$
	/**
	 * Plug-in identifier
	 */
	public static final String ID_API_TOOLS_UI_PLUGIN = "org.eclipse.pde.api.tools.ui"; //$NON-NLS-1$
	/**
	 * Id for the API baselines preference page.
	 * <br>
	 * Value is: <code>org.eclipse.pde.api.tools.ui.apiprofiles.prefpage</code>
	 */
	public static final String ID_BASELINES_PREF_PAGE = "org.eclipse.pde.api.tools.ui.apiprofiles.prefpage"; //$NON-NLS-1$
	/**
	 * Id for the API errors / warnings preference page
	 * <br>
	 * Value is: <code>org.eclipse.pde.api.tools.ui.apitools.errorwarnings.prefpage</code> 
	 */
	public static final String ID_ERRORS_WARNINGS_PREF_PAGE = "org.eclipse.pde.api.tools.ui.apitools.errorwarnings.prefpage"; //$NON-NLS-1$
	
	/**
	 * The id for the API errors / warnings property page
	 * <br>
	 * Value is: <code>org.eclipse.pde.api.tools.ui.apitools.warningspage</code>
	 */
	public static final String ID_ERRORS_WARNINGS_PROP_PAGE = "org.eclipse.pde.api.tools.ui.apitools.warningspage"; //$NON-NLS-1$
	
	/**
	 * The id for the API problem filters property page
	 * <br>
	 * Value is: <code>org.eclipse.pde.api.tools.ui.apitools.filterspage</code>
	 */
	public static final String ID_FILTERS_PROP_PAGE = "org.eclipse.pde.api.tools.ui.apitools.filterspage"; //$NON-NLS-1$
	
	/**
	 * Key for a compare api image
	 */
	public static final String IMG_ELCL_COMPARE_APIS = "IMG_ELCL_COMPARE_APIS"; //$NON-NLS-1$
	/**
	 * Key for a compare api disabled image
	 */
	public static final String IMG_ELCL_COMPARE_APIS_DISABLED = "IMG_ELCL_COMPARE_APIS_DISABLED"; //$NON-NLS-1$
	/**
	 * Key for filter resolution image
	 */
	public static final String IMG_ELCL_FILTER = "IMG_ELCL_FILTER"; //$NON-NLS-1$
	/**
	 * Key for the PDE Tools menu item for setting up API Tools
	 */
	public static final String IMG_ELCL_SETUP_APITOOLS = "IMG_ELCL_SETUP_APITOOLS"; //$NON-NLS-1$
	/**
	 * Key for the open page image
	 */
	public static final String IMG_ELCL_OPEN_PAGE = "IMG_ELCL_OPEN_PAGE"; //$NON-NLS-1$

	/**
	 * Key for enabled remove image
	 */
	public static final String IMG_ELCL_REMOVE = "IMG_ELCL_REMOVE"; //$NON-NLS-1$
	
	/**
	 * key for text edit image
	 */
	public static final String IMG_ELCL_TEXT_EDIT = "IMG_ELCL_TEXT_EDIT"; //$NON-NLS-1$
	
	/**
	 * Key for API component image.
	 */
	public static final String IMG_OBJ_API_COMPONENT = "IMG_OBJ_API_COMPONENT"; //$NON-NLS-1$
	
	/**
	 * Key for API search image
	 */
	public static final String IMG_OBJ_API_SEARCH = "IMG_OBJ_API_SEARCH"; //$NON-NLS-1$
	
	/**
	 * Key for API system component image
	 */
	public static final String IMG_OBJ_API_SYSTEM_LIBRARY = "IMG_OBJ_API_SYSTEM_LIBRARY"; //$NON-NLS-1$
	
	/**
	 * Key for bundle image
	 */
	public static final String IMG_OBJ_BUNDLE = "IMG_OBJ_BUNDLE"; //$NON-NLS-1$
	/**
	 * Key for a bundle version image
	 */
	public static final String IMG_OBJ_BUNDLE_VERSION = "IMG_OBJ_BUNDLE_VERSION"; //$NON-NLS-1$
	/**
	 * Key for Eclipse SDK/API profile image
	 */
	public static final String IMG_OBJ_ECLIPSE_PROFILE = "IMG_OBJ_ECLIPSE_PROFILE"; //$NON-NLS-1$
	/**
	 * Key for fragment image
	 */
	public static final String IMG_OBJ_FRAGMENT = "IMG_OBJ_FRAGMENT"; //$NON-NLS-1$		
	/**
	 * Error overlay.
	 */
	public static final String IMG_OVR_ERROR = "IMG_OVR_ERROR"; //$NON-NLS-1$
	
	/**
	 * Success overlay
	 */
	public static final String IMG_OVR_SUCCESS = "IMG_OVR_SUCCESS"; //$NON-NLS-1$
	
	/**
	 * Warning overlay
	 */
	public static final String IMG_OVR_WARNING = "IMG_OVR_WARNING"; //$NON-NLS-1$
	/**
	 * Wizard banner for editing an API baseline
	 */
	public static final String IMG_WIZBAN_PROFILE = "IMG_WIZBAN_PROFILE"; //$NON-NLS-1$
	/**
	 * Wizard banner for comparing a selected set of projects to a selected baseline
	 * @since 1.0.l
	 */
	public static final String IMG_WIZBAN_COMPARE_TO_BASELINE = "IMG_WIZBAN_COMPARE_TO_BASELINE"; //$NON-NLS-1$
	/**
	 * Key for enabled export image
	 */
	public static final String IMG_ELCL_EXPORT = "IMG_ELCL_EXPORT"; //$NON-NLS-1$
	/**
	 * Key for disabled export image
	 */
	public static final String IMG_DLCL_EXPORT = "IMG_DLCL_EXPORT"; //$NON-NLS-1$
	/**
	 * Key for enabled next navigation image
	 */
	public static final String IMG_ELCL_NEXT_NAV = "IMG_ELCL_NEXT_NAV"; //$NON-NLS-1$
	/**
	 * Key for enabled previous navigation image
	 */
	public static final String IMG_ELCL_PREV_NAV = "IMG_ELCL_PREV_NAV"; //$NON-NLS-1$
	/**
	 * Key for disabled next navigation image
	 */
	public static final String IMG_DLCL_NEXT_NAV = "IMG_DLCL_NEXT_NAV"; //$NON-NLS-1$
	/**
	 * Key for disabled previous navigation image
	 */
	public static final String IMG_DLCL_PREV_NAV = "IMG_DLCL_PREV_NAV"; //$NON-NLS-1$
	/**
	 * Key for enabled expand all image
	 */
	public static final String IMG_ELCL_EXPANDALL = "IMG_ELCL_EXPANDALL"; //$NON-NLS-1$
	/**
	 * Key for disabled expand all image
	 */
	public static final String IMG_DLCL_EXPANDALL = "IMG_DLCL_EXPANDALL"; //$NON-NLS-1$
}
