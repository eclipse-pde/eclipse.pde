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
}
