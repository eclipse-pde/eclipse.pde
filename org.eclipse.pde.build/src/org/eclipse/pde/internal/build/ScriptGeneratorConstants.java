package org.eclipse.pde.internal.core;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
public interface ScriptGeneratorConstants {
	
	// targets
	public static final String TARGET_ALL = "all";
	public static final String TARGET_BIN = "bin";
	public static final String TARGET_CLEAN = "clean";
	public static final String TARGET_DOC = "doc";
	public static final String TARGET_LOG = "log";
	public static final String TARGET_JAR = "jar";
	public static final String TARGET_JAVADOC = "javadoc";
	public static final String TARGET_SRC = "src";

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
	public final static int EXCEPTION_MODEL_PARSE = 0x1;
	public final static int EXCEPTION_PLUGIN_MISSING = 0x2;
	public final static int EXCEPTION_FRAGMENT_MISSING = 0x4;
	public final static int EXCEPTION_COMPONENT_PARSE = 0x8;
	public final static int EXCEPTION_COMPONENT_MISSING = 0x16;
	public final static int EXCEPTION_CONFIGURATION_PARSE = 0x32;
	public final static int EXCEPTION_CONFIGURATION_MISSING = 0x64;
	public final static int WARNING_PLUGIN_INCORRECTVERSION = 0x128;
	public final static int WARNING_FRAGMENT_INCORRECTVERSION = 0x256;
	public final static int WARNING_COMPONENT_INCORRECTVERSION = 0x512;
	public final static int EXCEPTION_FILE_MISSING = 0x1024;
}
