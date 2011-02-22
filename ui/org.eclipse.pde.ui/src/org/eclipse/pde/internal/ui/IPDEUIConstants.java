/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ui;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.pde.internal.launching.IPDEConstants;

public interface IPDEUIConstants extends IPDEConstants {
	String PLUGIN_ID = "org.eclipse.pde.ui"; //$NON-NLS-1$

	String MANIFEST_EDITOR_ID = PLUGIN_ID + ".manifestEditor"; //$NON-NLS-1$
	String FEATURE_EDITOR_ID = PLUGIN_ID + ".featureEditor"; //$NON-NLS-1$
	String SITE_EDITOR_ID = PLUGIN_ID + ".siteEditor"; //$NON-NLS-1$
	String CATEGORY_EDITOR_ID = PLUGIN_ID + ".categoryEditor"; //$NON-NLS-1$
	String BUILD_EDITOR_ID = PLUGIN_ID + ".buildEditor"; //$NON-NLS-1$
	String SCHEMA_EDITOR_ID = PLUGIN_ID + ".schemaEditor"; //$NON-NLS-1$
	String PRODUCT_EDITOR_ID = PLUGIN_ID + ".productEditor"; //$NON-NLS-1$
	String TARGET_EDITOR_ID = PLUGIN_ID + ".targetEditor"; //$NON-NLS-1$
	String PLUGINS_VIEW_ID = "org.eclipse.pde.ui.PluginsView"; //$NON-NLS-1$
	String DEPENDENCIES_VIEW_ID = "org.eclipse.pde.ui.DependenciesView"; //$NON-NLS-1$
	String PERSPECTIVE_ID = "org.eclipse.pde.ui.PDEPerspective"; //$NON-NLS-1$

	String RUN_LAUNCHER_ID = PLUGIN_ID + "." + "WorkbenchRunLauncher"; //$NON-NLS-1$ //$NON-NLS-2$
	String DEBUG_LAUNCHER_ID = PLUGIN_ID + "." + "WorkbenchDebugLauncher"; //$NON-NLS-1$ //$NON-NLS-2$
	String MARKER_SYSTEM_FILE_PATH = PLUGIN_ID + "." + "systemFilePath"; //$NON-NLS-1$ //$NON-NLS-2$

	QualifiedName PROPERTY_EDITOR_PAGE_KEY = new QualifiedName(PLUGIN_ID, "editor-page-key"); //$NON-NLS-1$
	QualifiedName PROPERTY_MANIFEST_EDITOR_PAGE_KEY = new QualifiedName(PLUGIN_ID, "manifest-editor-page-key"); //$NON-NLS-1$
	QualifiedName DEFAULT_PRODUCT_EXPORT_LOCATION = new QualifiedName(PLUGIN_ID, "product-export-location"); //$NON-NLS-1$
	QualifiedName DEFAULT_PRODUCT_EXPORT_DIR = new QualifiedName(PLUGIN_ID, "product-export-type"); //$NON-NLS-1$
	QualifiedName DEFAULT_PRODUCT_EXPORT_ROOT = new QualifiedName(PLUGIN_ID, "product-export-root"); //$NON-NLS-1$

	String PLUGIN_DOC_ROOT = "/org.eclipse.pde.doc.user/"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute key. The value is a boolean specifying
	 * whether the workspace log for an Eclipse application should be cleared
	 * prior to launching.
	 * 
	 * TODO, move to IPDELauncherConstants in 3.4
	 */
	String GENERATED_CONFIG = "pde.generated.config"; //$NON-NLS-1$
}
