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
package org.eclipse.pde.internal.ui;

import org.eclipse.core.runtime.QualifiedName;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public interface IPDEUIConstants {
	String PLUGIN_ID = "org.eclipse.pde.ui";

	String MANIFEST_EDITOR_ID = PLUGIN_ID + ".manifestEditor";
	String FRAGMENT_EDITOR_ID = PLUGIN_ID + ".fragmentEditor";
	String FEATURE_EDITOR_ID = PLUGIN_ID + ".featureEditor";
	String SITE_EDITOR_ID = PLUGIN_ID + ".siteEditor";
	String JARS_EDITOR_ID = PLUGIN_ID + ".jarsEditor";
	String BUILD_EDITOR_ID = PLUGIN_ID + ".buildEditor";
	String SCHEMA_EDITOR_ID = PLUGIN_ID + ".schemaEditor";
	String PLUGINS_VIEW_ID = "org.eclipse.pde.ui.PluginsView";
	String DEPENDENCIES_VIEW_ID = "org.eclipse.pde.ui.DependenciesView";

	String RUN_LAUNCHER_ID = PLUGIN_ID + "." + "WorkbenchRunLauncher";
	String DEBUG_LAUNCHER_ID = PLUGIN_ID + "." + "WorkbenchDebugLauncher";
	String MARKER_SYSTEM_FILE_PATH = PLUGIN_ID + "."+ "systemFilePath";

	QualifiedName DEFAULT_EDITOR_PAGE_KEY =
		new QualifiedName(PLUGIN_ID, "default-editor-page");	
	QualifiedName DEFAULT_EDITOR_PAGE_KEY_NEW =
		new QualifiedName(PLUGIN_ID, "default-editor-page-new");	
}