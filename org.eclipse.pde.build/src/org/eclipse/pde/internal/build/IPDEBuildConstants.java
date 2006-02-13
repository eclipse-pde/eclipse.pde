/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

/**
 * Generic constants for this plug-in classes.
 */
public interface IPDEBuildConstants {

	/** PDE Core plug-in id */
	public static final String PI_BOOT = "org.eclipse.core.boot"; //$NON-NLS-1$
	public static final String PI_BOOT_JAR_NAME = "boot.jar"; //$NON-NLS-1$
	public static final String PI_PDEBUILD = "org.eclipse.pde.build"; //$NON-NLS-1$
	public static final String PI_RUNTIME = "org.eclipse.core.runtime"; //$NON-NLS-1$
	public static final String PI_RUNTIME_JAR_NAME = "runtime.jar"; //$NON-NLS-1$

	/** file names */
	public final static String PROPERTIES_FILE = "build.properties"; //$NON-NLS-1$
	public final static String PERMISSIONS_FILE = "permissions.properties"; //$NON-NLS-1$
	public final static String ABOUT_HTML_FILE = "about.html"; //$NON-NLS-1$
	public final static String FEATURE_PROPERTIES_FILE = "feature.properties"; //$NON-NLS-1$
	public final static String SOURCE_PLUGIN_ATTRIBUTE = "sourcePlugin"; //$NON-NLS-1$
	public final static String MANIFEST_FOLDER = "META-INF"; //$NON-NLS-1$
	public final static String MANIFEST = "MANIFEST.MF"; //$NON-NLS-1$
	
	// command line arguments
	public static final String ARG_CVS_PASSFILE_LOCATION = "-cvspassfile"; //$NON-NLS-1$
	public static final String ARG_DEV_ENTRIES = "-dev"; //$NON-NLS-1$
	public static final String ARG_DIRECTORY_LOCATION = "-directory"; //$NON-NLS-1$
	public static final String ARG_ELEMENTS = "-elements"; //$NON-NLS-1$
	public static final String ARG_NO_CHILDREN = "-nochildren"; //$NON-NLS-1$
	public static final String ARG_PLUGIN_PATH = "-pluginpath"; //$NON-NLS-1$
	public static final String ARG_SCRIPT_NAME = "-scriptname"; //$NON-NLS-1$
	public static final String ARG_SOURCE_LOCATION = "-source"; //$NON-NLS-1$
	public static final String ARG_RECURSIVE_GENERATION = "-recursiveGeneration"; //$NON-NLS-1$

	// default values
	public final static String DEFAULT_BUILD_SCRIPT_FILENAME = "build.xml"; //$NON-NLS-1$
	public final static String DEFAULT_FEATURE_LOCATION = "features"; //$NON-NLS-1$
	public final static String DEFAULT_FETCH_SCRIPT_FILENAME = "fetch.xml"; //$NON-NLS-1$
	public final static String DEFAULT_ASSEMBLE_FILENAME = "assemble.xml"; //$NON-NLS-1$
	public final static String DEFAULT_PLUGIN_LOCATION = "plugins"; //$NON-NLS-1$
	public final static String DEFAULT_TEMPLATE_SCRIPT_FILENAME = "template.xml"; //$NON-NLS-1$
	public final static String GENERIC_VERSION_NUMBER = "0.0.0"; //$NON-NLS-1$ 
	public final static String ANY_STRING = "ANY"; //$NON-NLS-1$
	public final static String DEFAULT_ASSEMBLE_NAME = "assemble"; //$NON-NLS-1$
	public final static String DEFAULT_ASSEMBLE_ALL = "all.xml"; //$NON-NLS-1$
	public final static String DEFAULT_CUSTOM_TARGETS = "customTargets"; //$NON-NLS-1$
	public final static String DEFAULT_RETRIEVE_FILENAME_DESCRIPTOR = "retrieve.xml"; //$NON-NLS-1$
	public final static String DEFAULT_ISV_DOC_PLUGIN_SUFFIX = "doc.isv"; //$NON-NLS-1$
	public final static String DEFAULT_SOURCE_PLUGIN_SUFFIX = "source"; //$NON-NLS-1$
	public final static String DEFAULT_PACKAGER_DIRECTORY_FILENAME_DESCRIPTOR = "packager.directory.txt"; //$NON-NLS-1$
	public final static String DEFAULT_UNZIPPER_FILENAME_DESCRIPTOR = "unzipper.xml"; //$NON-NLS-1$
	public final static String DEFAULT_PLUGIN_REPOTAG_FILENAME_DESCRIPTOR = "pluginVersions.properties"; //$NON-NLS-1$
	public final static String DEFAULT_FEATURE_REPOTAG_FILENAME_DESCRIPTOR = "featureVersions.properties"; //$NON-NLS-1$
	public final static String DEFAULT_PLUGINS_POSTPROCESSINGSTEPS_FILENAME_DESCRIPTOR = "plugins.postProcessingSteps.properties"; //$NON-NLS-1$
	public final static String DEFAULT_FEATURES_POSTPROCESSINGSTEPS_FILENAME_DESCRIPTOR = "features.postProcessingSteps.properties"; //$NON-NLS-1$
	public final static String DEFAULT_CUSTOM_BUILD_CALLBACKS_FILE = "customBuildCallbacks.xml"; //$NON-NLS-1$
	public final static String DEFAULT_PRODUCT_ROOT_FILES_DIR = "productRootFiles"; //$NON-NLS-1$

	public final static String DEFAULT_PLUGIN_VERSION_FILENAME_PREFIX = "finalPluginsVersions"; //$NON-NLS-1$
	public final static String DEFAULT_FEATURE_VERSION_FILENAME_PREFIX = "finalFeaturesVersions"; //$NON-NLS-1$
	public final static String PROPERTIES_FILE_SUFFIX = ".properties"; //$NON-NLS-1$
	
	// Tag replaced in files
	public final static String REPLACED_PLUGIN_ID = "PLUGIN_ID"; //$NON-NLS-1$
	public final static String REPLACED_PLUGIN_VERSION = "PLUGIN_VERSION"; //$NON-NLS-1$
	public final static String REPLACED_FRAGMENT_VERSION = "FRAGMENT_VERSION"; //$NON-NLS-1$ 
	public final static String REPLACED_FRAGMENT_ID = "FRAGMENT_ID"; //$NON-NLS-1$

	// status constants	
	public final static int EXCEPTION_FEATURE_MISSING = 1;
	public final static int EXCEPTION_BUILDDIRECTORY_LOCATION_MISSING = 2;
	public final static int EXCEPTION_MALFORMED_URL = 3;
	public final static int EXCEPTION_MODEL_PARSE = 4;
	public final static int EXCEPTION_PLUGIN_MISSING = 5;
	public final static int EXCEPTION_READ_DIRECTORY = 6;
	public final static int EXCEPTION_WRITING_SCRIPT = 7;
	public final static int EXCEPTION_ELEMENT_MISSING = 8;
	public final static int EXCEPTION_ENTRY_MISSING = 9;
	public final static int EXCEPTION_READING_FILE = 10;
	public final static int EXCEPTION_SOURCE_LOCATION_MISSING = 11;
	public final static int EXCEPTION_WRITING_FILE = 12;
	public final static int EXCEPTION_INVALID_JAR_ORDER = 13;
	public final static int EXCEPTION_CLASSPATH_CYCLE = 14;
	public final static int EXCEPTION_STATE_PROBLEM = 15;
	public final static int EXCEPTION_GENERIC = 16;
	public final static int EXCEPTION_FEATURE_PARSE = 17;
	public final static int WARNING_MISSING_SOURCE = 20;
	public final static int WARNING_ELEMENT_NOT_FETCHED = 21;
	public final static int EXCEPTION_CONFIG_FORMAT = 22;

	//User object keys. BundleDescription.getUserObject()
	public final static String IS_COMPILED = "isCompiler"; //$NON-NLS-1$
	public final static String PLUGIN_ENTRY = "pluginEntry"; //$NON-NLS-1$
	public final static String WITH_DOT = "withDot";  //$NON-NLS-1$
	
	//Filter properties
	public final static String OSGI_WS = "osgi.ws";  //$NON-NLS-1$
	public final static String OSGI_OS = "osgi.os";  //$NON-NLS-1$
	public final static String OSGI_ARCH = "osgi.arch";  //$NON-NLS-1$
	public final static String OSGI_NL = "osgi.nl";  //$NON-NLS-1$
	
	//Eclipse specific manifest headers
	public final static String EXTENSIBLE_API = "Eclipse-ExtensibleAPI";  //$NON-NLS-1$
	public final static String PATCH_FRAGMENT = "Eclipse-PatchFragment"; //$NON-NLS-1$
	
	// fetch task extension point
	public final static String EXT_FETCH_TASK_FACTORIES = "org.eclipse.pde.build.fetchFactories"; //$NON-NLS-1$
	public final static String ATTR_ID = "id"; //$NON-NLS-1$
	public final static String ATTR_CLASS = "class"; //$NON-NLS-1$
	public final static String ELEM_FACTORY = "factory"; //$NON-NLS-1$
}
