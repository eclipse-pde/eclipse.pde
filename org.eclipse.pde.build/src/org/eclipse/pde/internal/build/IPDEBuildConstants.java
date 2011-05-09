/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

import org.eclipse.pde.internal.publishing.Constants;

/**
 * Generic constants for this plug-in classes.
 */
public interface IPDEBuildConstants {

	/** from PlatformURLPluginConnection and PlatformURLFragmentConnection **/
	public static final String PLUGIN = "plugin"; //$NON-NLS-1$
	public static final String FRAGMENT = "fragment"; //$NON-NLS-1$
	public static final String FEATURE = "feature"; //$NON-NLS-1$
	public static final String VERSION = "version"; //$NON-NLS-1$
	public static final String ID = "id"; //$NON-NLS-1$
	public static final String LABEL = "label"; //$NON-NLS-1$

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

	public static final String FILE_SCHEME = "file"; //$NON-NLS-1$
	public static final String PROFILE = "profile"; //$NON-NLS-1$
	public static final String PROFILE_GZ = "profile.gz"; //$NON-NLS-1$

	// default values
	public final static String PROPERTY_GENERIC_TARGETS = "genericTargets"; //$NON-NLS-1$
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
	public final static String DEFAULT_SOURCE_REFERENCES_FILENAME_DESCRIPTOR = "sourceReferences.properties"; //$NON-NLS-1$
	public final static String DEFAULT_PLUGINS_POSTPROCESSINGSTEPS_FILENAME_DESCRIPTOR = "plugins.postProcessingSteps.properties"; //$NON-NLS-1$
	public final static String DEFAULT_FEATURES_POSTPROCESSINGSTEPS_FILENAME_DESCRIPTOR = "features.postProcessingSteps.properties"; //$NON-NLS-1$
	public final static String DEFAULT_CUSTOM_BUILD_CALLBACKS_FILE = "customBuildCallbacks.xml"; //$NON-NLS-1$
	public final static String DEFAULT_PRODUCT_ROOT_FILES_DIR = "productRootFiles"; //$NON-NLS-1$

	public final static String DEFAULT_COMPILE_NAME = "compile"; //$NON-NLS-1$

	public final static String DEFAULT_PLUGIN_VERSION_FILENAME_PREFIX = "finalPluginsVersions"; //$NON-NLS-1$
	public final static String DEFAULT_FEATURE_VERSION_FILENAME_PREFIX = "finalFeaturesVersions"; //$NON-NLS-1$
	public final static String PROPERTIES_FILE_SUFFIX = ".properties"; //$NON-NLS-1$

	public final static String[] DEFAULT_SOURCE_FILE_EXTENSIONS = new String[] {"*.java"}; //$NON-NLS-1$

	// Tag replaced in files
	public final static String REPLACED_PLUGIN_ID = "PLUGIN_ID"; //$NON-NLS-1$
	public final static String REPLACED_PLUGIN_VERSION = "PLUGIN_VERSION"; //$NON-NLS-1$
	public final static String REPLACED_FRAGMENT_VERSION = "FRAGMENT_VERSION"; //$NON-NLS-1$ 
	public final static String REPLACED_FRAGMENT_ID = "FRAGMENT_ID"; //$NON-NLS-1$
	public final static String REPLACED_PLATFORM_FILTER = "PLATFORM_FILTER"; //$NON-NLS-1$

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
	public final static int EXCEPTION_FEATURE_PARSE = Constants.EXCEPTION_FEATURE_PARSE;
	public final static int WARNING_MISSING_SOURCE = 20;
	public final static int WARNING_ELEMENT_NOT_FETCHED = 21;
	public final static int EXCEPTION_CONFIG_FORMAT = 22;
	public final static int EXCEPTION_PRODUCT_FORMAT = 23;
	public final static int EXCEPTION_PRODUCT_FILE = 24;
	public final static int WARNING_PLUGIN_ALTERED = 25;
	public final static int WARNING_OLD_ANT = 26;

	//User object keys. BundleDescription.getUserObject()
	public final static String IS_COMPILED = "isCompiler"; //$NON-NLS-1$
	public final static String PLUGIN_ENTRY = "pluginEntry"; //$NON-NLS-1$
	public final static String OLD_BUNDLE_LOCATION = "oldBundleLocation"; //$NON-NLS-1$
	public final static String WITH_DOT = "withDot"; //$NON-NLS-1$

	//Filter properties
	public final static String OSGI_WS = "osgi.ws"; //$NON-NLS-1$
	public final static String OSGI_OS = "osgi.os"; //$NON-NLS-1$
	public final static String OSGI_ARCH = "osgi.arch"; //$NON-NLS-1$
	public final static String OSGI_NL = "osgi.nl"; //$NON-NLS-1$

	//Eclipse specific manifest headers
	public final static String EXTENSIBLE_API = "Eclipse-ExtensibleAPI"; //$NON-NLS-1$
	public final static String PATCH_FRAGMENT = "Eclipse-PatchFragment"; //$NON-NLS-1$
	public final static String ECLIPSE_SOURCE_BUNDLE = "Eclipse-SourceBundle"; //$NON-NLS-1$
	public final static String ECLIPSE_PLATFORM_FILTER = "Eclipse-PlatformFilter"; //$NON-NLS-1$
	public final static String ECLIPSE_BUNDLE_SHAPE = Constants.ECLIPSE_BUNDLE_SHAPE;
	public final static String ECLIPSE_SOURCE_REF = "Eclipse-SourceReferences"; //$NON-NLS-1$
	public final static String PDE_SOURCE_REF = "${PDE_SOURCE_REF}"; //$NON-NLS-1$

	//Some Bundle IDs we care about
	public final static String BUNDLE_OSGI = "org.eclipse.osgi"; //$NON-NLS-1$
	public static final String BUNDLE_EQUINOX_LAUNCHER = Constants.BUNDLE_EQUINOX_LAUNCHER;
	public static final String BUNDLE_EQUINOX_COMMON = "org.eclipse.equinox.common"; //$NON-NLS-1$
	public static final String BUNDLE_CORE_RUNTIME = "org.eclipse.core.runtime"; //$NON-NLS-1$
	public static final String BUNDLE_UPDATE_CONFIGURATOR = "org.eclipse.update.configurator"; //$NON-NLS-1$
	public static final String BUNDLE_SIMPLE_CONFIGURATOR = "org.eclipse.equinox.simpleconfigurator"; //$NON-NLS-1$
	public static final String BUNDLE_DS = "org.eclipse.equinox.ds"; //$NON-NLS-1$
	public static final String FEATURE_PLATFORM_LAUNCHERS = "org.eclipse.platform.launchers"; //$NON-NLS-1$
	public static final String FEATURE_EQUINOX_EXECUTABLE = "org.eclipse.equinox.executable"; //$NON-NLS-1$

	// fetch task extension point
	public final static String EXT_FETCH_TASK_FACTORIES = "org.eclipse.pde.build.fetchFactories"; //$NON-NLS-1$
	public final static String ATTR_ID = "id"; //$NON-NLS-1$
	public final static String ATTR_CLASS = "class"; //$NON-NLS-1$
	public final static String ELEM_FACTORY = "factory"; //$NON-NLS-1$

	//container feature used in building .product files
	public final static String CONTAINER_FEATURE = "org.eclipse.pde.build.container.feature"; //$NON-NLS-1$
	public final static String UI_CONTAINER_FEATURE = "org.eclipse.pde.container.feature"; //$NON-NLS-1$

	public final static String PDE_CORE_PREFS = ".settings/org.eclipse.pde.core.prefs"; //$NON-NLS-1$
	public final static String JDT_CORE_PREFS = ".settings/org.eclipse.jdt.core.prefs"; //$NON-NLS-1$
	public final static String BUNDLE_ROOT_PATH = "BUNDLE_ROOT_PATH"; //$NON-NLS-1$

	public static final String PROPERTY_RESOLVER_MODE = "osgi.resolverMode"; //$NON-NLS-1$
	public static final String PROPERTY_RESOLVE_OPTIONAL = "osgi.resolveOptional"; //$NON-NLS-1$
	public static final String VALUE_DEVELOPMENT = "development"; //$NON-NLS-1$

	public static final String LICENSE_DEFAULT_EXCLUDES = ",.project,build.properties,feature.xml,feature.properties,feature_*.properties"; //$NON-NLS-1$
}
