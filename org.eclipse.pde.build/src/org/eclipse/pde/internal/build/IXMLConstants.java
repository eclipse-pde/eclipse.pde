/**********************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build;
/**
 * XML template constants.
 */
public interface IXMLConstants {

	// general
	public static final String DEFAULT_FILENAME_SRC = ".src.zip"; //$NON-NLS-1$
	public static final String DEFAULT_FILENAME_LOG = ".log.zip"; //$NON-NLS-1$
	public static final String PROPERTY_ASSIGNMENT_PREFIX = "${"; //$NON-NLS-1$
	public static final String PROPERTY_ASSIGNMENT_SUFFIX = "}"; //$NON-NLS-1$
	public static final String PROPERTY_JAR_SUFFIX = ".jar"; //$NON-NLS-1$
	public static final String PROPERTY_SOURCE_PREFIX = "source."; //$NON-NLS-1$
	public static final String PROPERTY_ZIP_SUFFIX = ".zip"; //$NON-NLS-1$
	public static final String JDT_COMPILER_ADAPTER = "org.eclipse.jdt.core.JDTCompilerAdapter"; //$NON-NLS-1$


	// element description variables (used in files like plugin.xml, e.g. $ws$)
	public static final String DESCRIPTION_VARIABLE_NL = "$nl$"; //$NON-NLS-1$
	public static final String DESCRIPTION_VARIABLE_OS = "$os$"; //$NON-NLS-1$
	public static final String DESCRIPTION_VARIABLE_WS = "$ws$"; //$NON-NLS-1$
	

	// targets
	public static final String TARGET_ALL_CHILDREN = "all.children"; //$NON-NLS-1$
	public static final String TARGET_ALL_FRAGMENTS = "all.fragments"; //$NON-NLS-1$
	public static final String TARGET_ALL_PLUGINS = "all.plugins"; //$NON-NLS-1$
	public static final String TARGET_BUILD_JARS = "build.jars"; //$NON-NLS-1$
	public static final String TARGET_BUILD_SOURCES = "build.sources"; //$NON-NLS-1$
	public static final String TARGET_BUILD_UPDATE_JAR = "build.update.jar"; //$NON-NLS-1$
	public static final String TARGET_BUILD_ZIPS = "build.zips"; //$NON-NLS-1$
	public static final String TARGET_CHILDREN = "children"; //$NON-NLS-1$
	public static final String TARGET_CLEAN = "clean"; //$NON-NLS-1$
	public static final String TARGET_FETCH = "fetch"; //$NON-NLS-1$
	public static final String TARGET_GATHER_BIN_PARTS = "gather.bin.parts"; //$NON-NLS-1$
	public static final String TARGET_GATHER_LOGS = "gather.logs"; //$NON-NLS-1$
	public static final String TARGET_GATHER_SOURCES = "gather.sources"; //$NON-NLS-1$
	public static final String TARGET_INIT = "init"; //$NON-NLS-1$
	public static final String TARGET_INSTALL = "install"; //$NON-NLS-1$
	public static final String TARGET_MAIN = "main"; //$NON-NLS-1$
	public static final String TARGET_PROPERTIES = "properties"; //$NON-NLS-1$
	public static final String TARGET_REFRESH = "refresh"; //$NON-NLS-1$
	public static final String TARGET_SRC_GATHER_WHOLE = "src.gather.whole"; //$NON-NLS-1$
	public static final String TARGET_TARGET = "target"; //$NON-NLS-1$
	public static final String TARGET_ZIP_DISTRIBUTION = "zip.distribution"; //$NON-NLS-1$
	public static final String TARGET_ZIP_LOGS = "zip.logs"; //$NON-NLS-1$
	public static final String TARGET_ZIP_PLUGIN = "zip.plugin"; //$NON-NLS-1$
	public static final String TARGET_ZIP_SOURCES = "zip.sources"; //$NON-NLS-1$
	
	// properties
	public static final String PROPERTY_ARCH = "arch"; //$NON-NLS-1$
	public static final String PROPERTY_BASEDIR = "basedir"; //$NON-NLS-1$
	public static final String PROPERTY_BIN_EXCLUDES = "bin.excludes"; //$NON-NLS-1$
	public static final String PROPERTY_BIN_INCLUDES = "bin.includes"; //$NON-NLS-1$
	public static final String PROPERTY_BOOTCLASSPATH = "bootclasspath"; //$NON-NLS-1$
	public static final String PROPERTY_BUILD_COMPILER = "build.compiler"; //$NON-NLS-1$
	public static final String PROPERTY_BUILD_ID = "build.id"; //$NON-NLS-1$
	public static final String PROPERTY_BUILD_QUALIFIER = "build.qualifier"; //$NON-NLS-1$
	public static final String PROPERTY_BUILD_RESULT_FOLDER = "build.result.folder"; //$NON-NLS-1$
	public static final String PROPERTY_BUILD_TYPE = "build.type"; //$NON-NLS-1$
	public static final String PROPERTY_CUSTOM = "custom"; //$NON-NLS-1$
	public static final String PROPERTY_DESTINATION_TEMP_FOLDER = "destination.temp.folder"; //$NON-NLS-1$
	public static final String PROPERTY_ECLIPSE_RUNNING = "eclipse.running"; //$NON-NLS-1$
	public static final String PROPERTY_FEATURE = "feature"; //$NON-NLS-1$
	public static final String PROPERTY_FEATURE_BASE = "feature.base"; //$NON-NLS-1$
	public static final String PROPERTY_FEATURE_DESTINATION = "feature.destination"; //$NON-NLS-1$
	public static final String PROPERTY_FEATURE_FULL_NAME = "feature.full.name"; //$NON-NLS-1$
	public static final String PROPERTY_FEATURE_TEMP_FOLDER = "feature.temp.folder"; //$NON-NLS-1$
	public static final String PROPERTY_FEATURE_VERSION_SUFFIX = "feature.version.suffix"; //$NON-NLS-1$
	public static final String PROPERTY_FULL_NAME = "full.name"; //$NON-NLS-1$
	public static final String PROPERTY_INCLUDE_CHILDREN = "include.children"; //$NON-NLS-1$
	public static final String PROPERTY_INSTALL = "install"; //$NON-NLS-1$
	public static final String PROPERTY_INSTALL_LOCATION = "install.location"; //$NON-NLS-1$
	public static final String PROPERTY_JAR_EXTRA_CLASSPATH = "jars.extra.classpath"; //$NON-NLS-1$
	public static final String PROPERTY_JAR_ORDER = "jars.compile.order"; //$NON-NLS-1$
	public static final String PROPERTY_NL = "nl"; //$NON-NLS-1$
	public static final String PROPERTY_OS = "os"; //$NON-NLS-1$
	public static final String PROPERTY_QUIET = "quiet"; //$NON-NLS-1$
	public static final String PROPERTY_SRC_EXCLUDES = "src.excludes"; //$NON-NLS-1$
	public static final String PROPERTY_SRC_INCLUDES = "src.includes"; //$NON-NLS-1$
	public static final String PROPERTY_PLUGIN_DESTINATION = "plugin.destination"; //$NON-NLS-1$
	public static final String PROPERTY_TARGET = "target"; //$NON-NLS-1$
	public static final String PROPERTY_TEMP_FOLDER = "temp.folder"; //$NON-NLS-1$
	public static final String PROPERTY_VERSION_SUFFIX = "version.suffix"; //$NON-NLS-1$
	public static final String PROPERTY_WS = "ws"; //$NON-NLS-1$
}