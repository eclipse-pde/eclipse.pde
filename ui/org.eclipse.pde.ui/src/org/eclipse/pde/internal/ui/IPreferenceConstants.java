/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Created on Feb 20, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package org.eclipse.pde.internal.ui;

public interface IPreferenceConstants {
	
	// Java build path control preference page
	public static final String PROP_PLUGIN_PROJECT_UPDATE =
		"Preferences.BuildpathPage.pluginProjectUpdate";
	public static final String PROP_FRAGMENT_PROJECT_UPDATE =
		"Preferences.BuildpathPage.fragmentProjectUpdate";
	public static final String PROP_MANIFEST_UPDATE =
		"Preferences.BuildpathPage.manifestUpdate";
	public static final String PROP_CONVERSION_UPDATE =
		"Preferences.BuildpathPage.conversionUpdate";		
	public static final String PROP_CLASSPATH_CONTAINERS =
		"Preferences.BuildpathPage.useClasspathContainers";

	// editor preference page
	public static final String P_USE_SOURCE_PAGE = "useSourcePage";

	// Main preference page
	public static final String PROP_SHOW_OBJECTS =
		"Preferences.MainPage.showObjects";
	public static final String VALUE_USE_IDS = "useIds";
	public static final String VALUE_USE_NAMES = "useNames";
	public static final String PROP_BUILD_SCRIPT_NAME =
		"Preferences.MainPage.buildScriptName";
	public static final String PROP_ADD_TODO = 
		"Preferences.MainPage.addTodo";

}
