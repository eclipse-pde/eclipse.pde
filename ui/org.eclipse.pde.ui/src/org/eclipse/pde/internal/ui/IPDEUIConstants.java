/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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

public interface IPDEUIConstants {
	String PLUGIN_ID = "org.eclipse.pde.ui"; //$NON-NLS-1$

	String MANIFEST_EDITOR_ID = PLUGIN_ID + ".manifestEditor"; //$NON-NLS-1$
	String FEATURE_EDITOR_ID = PLUGIN_ID + ".featureEditor"; //$NON-NLS-1$
	String SITE_EDITOR_ID = PLUGIN_ID + ".siteEditor"; //$NON-NLS-1$
	String BUILD_EDITOR_ID = PLUGIN_ID + ".buildEditor"; //$NON-NLS-1$
	String SCHEMA_EDITOR_ID = PLUGIN_ID + ".schemaEditor"; //$NON-NLS-1$
	String PRODUCT_EDITOR_ID = PLUGIN_ID + ".productEditor"; //$NON-NLS-1$
	String PLUGINS_VIEW_ID = "org.eclipse.pde.ui.PluginsView"; //$NON-NLS-1$
	String DEPENDENCIES_VIEW_ID = "org.eclipse.pde.ui.DependenciesView"; //$NON-NLS-1$

	String RUN_LAUNCHER_ID = PLUGIN_ID + "." + "WorkbenchRunLauncher"; //$NON-NLS-1$ //$NON-NLS-2$
	String DEBUG_LAUNCHER_ID = PLUGIN_ID + "." + "WorkbenchDebugLauncher"; //$NON-NLS-1$ //$NON-NLS-2$
	String MARKER_SYSTEM_FILE_PATH = PLUGIN_ID + "."+ "systemFilePath"; //$NON-NLS-1$ //$NON-NLS-2$

	QualifiedName DEFAULT_EDITOR_PAGE_KEY =
		new QualifiedName(PLUGIN_ID, "default-editor-page");	 //$NON-NLS-1$
	QualifiedName DEFAULT_EDITOR_PAGE_KEY_NEW =
		new QualifiedName(PLUGIN_ID, "default-editor-page-new");	 //$NON-NLS-1$
}
