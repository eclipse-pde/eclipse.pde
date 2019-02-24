/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ui;

import org.eclipse.pde.internal.launching.ILaunchingPreferenceConstants;

/**
 * Listing of constants used in PDE preferences
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IPreferenceConstants extends ILaunchingPreferenceConstants {

	// Main preference page
	public static final String PROP_SHOW_OBJECTS = "Preferences.MainPage.showObjects"; //$NON-NLS-1$
	public static final String VALUE_USE_IDS = "useIds"; //$NON-NLS-1$
	public static final String VALUE_USE_NAMES = "useNames"; //$NON-NLS-1$
	public static final String PROP_SHOW_SOURCE_BUNDLES = "Preferences.MainPage.showSourceBundles"; //$NON-NLS-1$
	public static final String OVERWRITE_BUILD_FILES_ON_EXPORT = "Preferences.MainPage.overwriteBuildFilesOnExport"; //$NON-NLS-1$
	public static final String PROP_PROMPT_REMOVE_TARGET = "Preferences.MainPage.promptRemoveTarget"; //$NON-NLS-1$
	public static final String ADD_TO_JAVA_SEARCH = "Preferences.MainPage.addToJavaSearch"; //$NON-NLS-1$
	/**
	 * Boolean preference whether to display the active target platform in the main window status bar
	 */
	public static final String SHOW_TARGET_STATUS = "Preferences.MainPage.showTargetStatus"; //$NON-NLS-1$
	/**
	 * Boolean preference whether the workspace bundle overrides the target
	 * bundle for the same bundle id
	 */
	public static final String WORKSPACE_PLUGINS_OVERRIDE_TARGET = "Preferences.MainPage.workspacePluginsOverrideTarget";//$NON-NLS-1$
	/**
	 * Boolean preference whether API analysis has been disabled
	 */
	public static final String DISABLE_API_ANALYSIS_BUILDER = "Preferences.MainPage.disableAPIAnalysisBuilder";//$NON-NLS-1$


	// Editor Outline
	public static final String PROP_OUTLINE_SORTING = "PDEMultiPageContentOutline.SortingAction.isChecked"; //$NON-NLS-1$

	// Editor folding
	public static final String EDITOR_FOLDING_ENABLED = "editor.folding"; //$NON-NLS-1$

	// Dependencies view
	public static final String DEPS_VIEW_SHOW_CALLERS = "DependenciesView.show.callers"; //$NON-NLS-1$
	public static final String DEPS_VIEW_SHOW_LIST = "DependenciesView.show.list"; //$NON-NLS-1$
	public static final String DEPS_VIEW_SHOW_STATE = "DependenciesView.show.state"; //$NON-NLS-1$

	//Run Configurations - Plug-ins Tab - Feature launching
	public static final String FEATURE_SORT_COLUMN = "Preferences.RunConfigs.Feature.SortColumn"; //$NON-NLS-1$
	public static final String FEATURE_SORT_ORDER = "Preferences.RunConfigs.Feature.SortOrder"; //$NON-NLS-1$

	/**
	 * Preference key for the pattern that determines if a plugin project should be
	 * treated as test code.
	 */
	public static final String TEST_PLUGIN_PATTERN = "Preferences.MainPage.testPluginPattern";//$NON-NLS-1$

}
