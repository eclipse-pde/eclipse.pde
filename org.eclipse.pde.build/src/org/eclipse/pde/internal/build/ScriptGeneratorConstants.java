package org.eclipse.pde.internal.core;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
interface ScriptGeneratorConstants {
	
	// constants
	public static final String FILENAME_PROPERTIES = "build.properties";
	public static final String SEPARATOR_VERSION = "_";
	
	// targets
	public static final String TARGET_ALL = "all";
	public static final String TARGET_BIN = "bin";
	public static final String TARGET_CLEAN = "clean";
	public static final String TARGET_DOC = "doc";
	public static final String TARGET_LOG = "log";
	public static final String TARGET_JAR = "jar";
	public static final String TARGET_JAVADOC = "javadoc";
	public static final String TARGET_SRC = "src";
	public static final String TARGET_BIN_ZIP = "bin.zip";
	public static final String TARGET_SRC_ZIP = "src.zip";
	public static final String TARGET_LOG_ZIP = "log.zip";
	public static final String TARGET_DOC_ZIP = "doc.zip";

	// switches
	public static final String SWITCH_DELIMITER = "-";
	public static final String SWITCH_NOCHILDREN = "-nochildren";
	
	public static final String FILENAME_COMPONENT = "install.xml";
	public static final String FILENAME_CONFIGURATION = "install.xml";
	
	// substitutable properties
	public static final String BIN_INCLUDES = "bin.includes";
	public static final String BIN_EXCLUDES = "bin.excludes";
	public static final String JAVADOC_PACKAGES = "javadoc.packages";
	public static final String JAVADOC_EXCLUDEDPACKAGES = "javadoc.excludedpackages";
	public static final String SRC_INCLUDES = "src.includes";
	public static final String SRC_EXCLUDES = "src.excludes";
	
	// exception codes
	public final static int EXCEPTION_MODEL_PARSE = 1;
	public final static int EXCEPTION_PLUGIN_MISSING = 2;
	public final static int EXCEPTION_FRAGMENT_MISSING = 3;
	public final static int EXCEPTION_COMPONENT_INPUT = 4;
	public final static int EXCEPTION_COMPONENT_MISSING = 5;
	public final static int EXCEPTION_COMPONENT_PARSE = 6;
	public final static int EXCEPTION_CONFIGURATION_INPUT = 7;
	public final static int EXCEPTION_CONFIGURATION_MISSING = 8;
	public final static int EXCEPTION_CONFIGURATION_PARSE = 9;
	public final static int WARNING_PLUGIN_INCORRECTVERSION = 10;
	public final static int WARNING_FRAGMENT_INCORRECTVERSION = 11;
	public final static int WARNING_COMPONENT_INCORRECTVERSION = 12;
	public final static int WARNING_MISSING_SOURCE = 13;
	public final static int EXCEPTION_FILE_MISSING = 14;
	public final static int EXCEPTION_OUTPUT = 15;
	public final static int EXCEPTION_URL = 16;
}
