/**********************************************************************
 * Copyright (c) 2005 IBM Corporation and others. All rights reserved.   This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.core;

import org.eclipse.osgi.util.NLS;

public class PDECoreMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.pde.internal.core.pderesources";//$NON-NLS-1$

	public static String PluginModelManager_outOfSync;

	// Status text #####################################
	public static String ExternalModelManager_scanningProblems;
	public static String ExternalModelManager_processingPath;
	public static String Errors_SetupError;
	public static String Errors_SetupError_NoPlatformHome;
	public static String Errors_modelError;
	public static String BinaryRepositoryProvider_veto;
	public static String RequiredPluginsClasspathContainer_description;
	public static String ExternalJavaSearchClasspathContainer_description;
	public static String TargetPlatform_exceptionThrown;
	public static String TargetPlatformRegistryLoader_parsing;

	public static String FeatureInfo_description;
	public static String FeatureInfo_license;
	public static String FeatureInfo_copyright;
	public static String PluginObject_readOnlyChange;
	public static String FeatureObject_readOnlyChange;
	public static String SiteObject_readOnlyChange;


	public static String SearchablePluginsManager_saving;
	public static String BuildObject_readOnlyException;
	public static String BundleObject_readOnlyException;
	public static String PluginBase_librariesNotFoundException;
	public static String PluginParent_siblingsNotFoundException;
	public static String PluginBase_importsNotFoundException;
	public static String AbstractExtensions_extensionsNotFoundException;
	public static String SchemaCompositor_all;
	public static String SchemaCompositor_choice;
	public static String SchemaCompositor_group;
	public static String SchemaCompositor_sequence;
	public static String SiteBuildObject_readOnlyException;
	public static String PDEState_invalidFormat;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, PDECoreMessages.class);
	}
}