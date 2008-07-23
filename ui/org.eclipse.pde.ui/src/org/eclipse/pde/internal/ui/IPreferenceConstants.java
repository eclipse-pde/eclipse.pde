/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui;

/**
 * Listing of constants used in PDE preferences
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IPreferenceConstants {

	// Main preference page	
	public static final String PROP_SHOW_OBJECTS = "Preferences.MainPage.showObjects"; //$NON-NLS-1$
	public static final String VALUE_USE_IDS = "useIds"; //$NON-NLS-1$
	public static final String VALUE_USE_NAMES = "useNames"; //$NON-NLS-1$
	public static final String PROP_AUTO_MANAGE = "Preferences.MainPage.automanageDependencies"; //$NON-NLS-1$
	public static final String OVERWRITE_BUILD_FILES_ON_EXPORT = "Preferences.MainPage.overwriteBuildFilesOnExport"; //$NON-NLS-1$

	// Editor Outline
	public static final String PROP_OUTLINE_SORTING = "PDEMultiPageContentOutline.SortingAction.isChecked"; //$NON-NLS-1$

	// Editor folding
	public static final String EDITOR_FOLDING_ENABLED = "editor.folding"; //$NON-NLS-1$

	// Dependencies view
	public static final String DEPS_VIEW_SHOW_CALLERS = "DependenciesView.show.callers"; //$NON-NLS-1$
	public static final String DEPS_VIEW_SHOW_LIST = "DependenciesView.show.list"; //$NON-NLS-1$
	public static final String DEPS_VIEW_SHOW_STATE = "DependenciesView.show.state"; //$NON-NLS-1$

	// OSGi Frameworks
	public static final String DEFAULT_OSGI_FRAMEOWRK = "Preference.default.osgi.framework"; //$NON-NLS-1$
}
