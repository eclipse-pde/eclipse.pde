package org.eclipse.pde.internal.build;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

/**
 * XML template constants.
 */
public interface IXMLConstants {

	// general
	public static final String DEFAULT_FILENAME_SRC = ".src.zip";
	public static final String DEFAULT_FILENAME_LOG = ".log.zip";
	public static final String PROPERTY_ASSIGNMENT_PREFIX = "${";
	public static final String PROPERTY_ASSIGNMENT_SUFFIX = "}";
	public static final String PROPERTY_JAR_SUFFIX = ".jar";
	public static final String PROPERTY_SOURCE_PREFIX = "source.";
	public static final String PROPERTY_ZIP_SUFFIX = ".zip";
	public static final String JDT_COMPILER_ADAPTER = "org.eclipse.pde.internal.core.JDTCompilerAdapter";


	// element description variables (used in files like plugin.xml, e.g. $ws$)
	public static final String DESCRIPTION_VARIABLE_NL = "$nl$";
	public static final String DESCRIPTION_VARIABLE_OS = "$os$";
	public static final String DESCRIPTION_VARIABLE_WS = "$ws$";
	

	// targets
	public static final String TARGET_ALL_CHILDREN = "all.children";
	public static final String TARGET_ALL_FRAGMENTS = "all.fragments";
	public static final String TARGET_ALL_PLUGINS = "all.plugins";
	public static final String TARGET_BUILD_JARS = "build.jars";
	public static final String TARGET_BUILD_SOURCES = "build.sources";
	public static final String TARGET_BUILD_UPDATE_JAR = "build.update.jar";
	public static final String TARGET_BUILD_ZIPS = "build.zips";
	public static final String TARGET_CHILDREN = "children";
	public static final String TARGET_CLEAN = "clean";
	public static final String TARGET_FETCH = "fetch";
	public static final String TARGET_GATHER_BIN_PARTS = "gather.bin.parts";
	public static final String TARGET_GATHER_LOGS = "gather.logs";
	public static final String TARGET_GATHER_SOURCES = "gather.sources";
	public static final String TARGET_INIT = "init";
	public static final String TARGET_INSTALL = "install";
	public static final String TARGET_JAR = "jar";
	public static final String TARGET_MAIN = "main";
	public static final String TARGET_SRC = "src";
	public static final String TARGET_SRC_GATHER_WHOLE = "src.gather.whole";
	public static final String TARGET_TARGET = "target";
	public static final String TARGET_ZIP_DISTRIBUTION = "zip.distribution";
	public static final String TARGET_ZIP_LOGS = "zip.logs";
	public static final String TARGET_ZIP_SOURCES = "zip.sources";
	
	// properties
	public static final String PROPERTY_ARCH = "arch";
	public static final String PROPERTY_BASE = "base";
	public static final String PROPERTY_BASEDIR = "basedir";
	public static final String PROPERTY_BIN_EXCLUDES = "bin.excludes";
	public static final String PROPERTY_BIN_INCLUDES = "bin.includes";
	public static final String PROPERTY_BUILD_COMPILER = "build.compiler";
	public static final String PROPERTY_BUILD_ID = "build.id";
	public static final String PROPERTY_BUILD_QUALIFIER = "build.qualifier";
	public static final String PROPERTY_BUILD_TYPE = "build.type";
	public static final String PROPERTY_CUSTOM = "custom";
	public static final String PROPERTY_DESTINATION = "destination";
	public static final String PROPERTY_FEATURE = "feature";
	public static final String PROPERTY_FEATURE_BASE = "feature.base";
	public static final String PROPERTY_INCLUDE_CHILDREN = "includeChildren";
	public static final String PROPERTY_INSTALL = "install";
	public static final String PROPERTY_INSTALL_LOCATION = "install.location";
	public static final String PROPERTY_JAR_EXTERNAL = "jar.external";
	public static final String PROPERTY_JAR_ORDER = "jars.compile.order";
	public static final String PROPERTY_NL = "nl";
	public static final String PROPERTY_OS = "os";
	public static final String PROPERTY_QUIET = "quiet";
	public static final String PROPERTY_PLUGIN_PATH = "plugin.path";
	public static final String PROPERTY_SRC_EXCLUDES = "src.excludes";
	public static final String PROPERTY_SRC_INCLUDES = "src.includes";
	public static final String PROPERTY_TARGET = "target";
	public static final String PROPERTY_TEMPLATE = "template";
	public static final String PROPERTY_VERSION = "version";
	public static final String PROPERTY_WS = "ws";
	public static final String PROPERTY_ZIP_ARGUMENT = "zip.argument";
	public static final String PROPERTY_ZIP_EXTERNAL = "zip.external";
	public static final String PROPERTY_ZIP_FILE = "${zip.file}";
	public static final String PROPERTY_ZIP_PROGRAM = "zip.program";


	/**
	 * Persistent properties. Properties that should not have its
	 * value changed during a generate script batch.
	 */
	public static final String[] PERSISTENT_PROPERTIES = {
		PROPERTY_ZIP_ARGUMENT, PROPERTY_ZIP_EXTERNAL, PROPERTY_ZIP_PROGRAM,
		PROPERTY_JAR_EXTERNAL
	};
}