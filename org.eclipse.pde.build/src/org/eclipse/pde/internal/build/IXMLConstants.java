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
 * XML template constants.
 */
public interface IXMLConstants {

	// general
	public static final String PROPERTY_ASSIGNMENT_PREFIX = "${"; //$NON-NLS-1$
	public static final String PROPERTY_ASSIGNMENT_SUFFIX = "}"; //$NON-NLS-1$
	public static final String JDT_COMPILER_ADAPTER = "org.eclipse.jdt.core.JDTCompilerAdapter"; //$NON-NLS-1$

	// element description variables (used in files like plugin.xml, e.g. $ws$)
	public static final String DESCRIPTION_VARIABLE_NL = "$nl$"; //$NON-NLS-1$
	public static final String DESCRIPTION_VARIABLE_OS = "$os$"; //$NON-NLS-1$
	public static final String DESCRIPTION_VARIABLE_WS = "$ws$"; //$NON-NLS-1$
	public static final String DESCRIPTION_VARIABLE_ARCH = "$arch$"; //$NON-NLS-1$ 

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
	public static final String TARGET_MAIN = "main"; //$NON-NLS-1$
	public static final String TARGET_PROPERTIES = "properties"; //$NON-NLS-1$
	public static final String TARGET_REFRESH = "refresh"; //$NON-NLS-1$
	public static final String TARGET_ZIP_DISTRIBUTION = "zip.distribution"; //$NON-NLS-1$
	public static final String TARGET_ZIP_LOGS = "zip.logs"; //$NON-NLS-1$
	public static final String TARGET_ZIP_PLUGIN = "zip.plugin"; //$NON-NLS-1$
	public static final String TARGET_ZIP_SOURCES = "zip.sources"; //$NON-NLS-1$
	public static final String TARGET_UPDATE_FEATURE_FILE = "update.feature"; //$NON-NLS-1$
	public static final String TARGET_ALL_FEATURES = "all.features"; //$NON-NLS-1$
	public static final String TARGET_FETCH_ELEMENT = "fetch.element"; //$NON-NLS-1$
	public static final String TARGET_FETCH_PLUGINS = "fetch.plugins"; //$NON-NLS-1$
	public static final String TARGET_FETCH_RECURSIVELY = "fetch.recursively"; //$NON-NLS-1$
	public static final String TARGET_GET_FROM_CVS = "getFromCVS"; //$NON-NLS-1$
	public static final String TARGET_EFFECTIVE_FETCH = "effectiveFetch"; //$NON-NLS-1$
	public static final String TARGET_JARUP = "jarUp"; //$NON-NLS-1$
	public static final String TARGET_JARING = "jarIng"; //$NON-NLS-1$
	public static final String TARGET_JARSIGNING = "jarSigning";  //$NON-NLS-1$
	public static final String TARGET_ROOTFILES_PREFIX = "rootFiles"; //$NON-NLS-1$
	// properties
	public static final String PROPERTY_ARCH = "arch"; //$NON-NLS-1$
	public static final String PROPERTY_BASE_ARCH = "basearch"; //$NON-NLS-1$
	public static final String PROPERTY_BASEDIR = "basedir"; //$NON-NLS-1$
	public static final String PROPERTY_BOOTCLASSPATH = "bootclasspath"; //$NON-NLS-1$
	public static final String PROPERTY_BUILD_COMPILER = "build.compiler"; //$NON-NLS-1$
	public static final String PROPERTY_BUILD_DIRECTORY = "buildDirectory"; //$NON-NLS-1$
	public static final String PROPERTY_BUILD_ID = "build.id"; //$NON-NLS-1$
	public static final String PROPERTY_BUILD_QUALIFIER = "build.qualifier"; //$NON-NLS-1$
	public static final String PROPERTY_BUILD_RESULT_FOLDER = "build.result.folder"; //$NON-NLS-1$
	public static final String PROPERTY_BUILD_TYPE = "build.type"; //$NON-NLS-1$
	public static final String PROPERTY_DESTINATION_TEMP_FOLDER = "destination.temp.folder"; //$NON-NLS-1$
	public static final String PROPERTY_ECLIPSE_RUNNING = "eclipse.running"; //$NON-NLS-1$
	public static final String PROPERTY_FEATURE = "feature"; //$NON-NLS-1$
	public static final String PROPERTY_ECLIPSE_BASE = "eclipse.base"; //$NON-NLS-1$
	public static final String PROPERTY_FEATURE_BASE = "feature.base"; //$NON-NLS-1$
	public static final String PROPERTY_FEATURE_DESTINATION = "feature.destination"; //$NON-NLS-1$
	public static final String PROPERTY_FEATURE_FULL_NAME = "feature.full.name"; //$NON-NLS-1$
	public static final String PROPERTY_FEATURE_TEMP_FOLDER = "feature.temp.folder"; //$NON-NLS-1$
	public static final String PROPERTY_FEATURE_VERSION_SUFFIX = "feature.version.suffix"; //$NON-NLS-1$
	public static final String PROPERTY_FULL_NAME = "full.name"; //$NON-NLS-1$
	public static final String PROPERTY_INCLUDE_CHILDREN = "include.children"; //$NON-NLS-1$
	public static final String PROPERTY_LAUNCHER_ICONS = "launcherIcons"; //$NON-NLS-1$
	public static final String PROPERTY_LAUNCHER_NAME = "launcherName"; //$NON-NLS-1$
	public static final String PROPERTY_PRODUCT = "product"; //$NON-NLS-1$
	
	//public static final String PROPERTY_INSTALL = "install"; //$NON-NLS-1$
	public static final String PROPERTY_NL = "nl"; //$NON-NLS-1$
	public static final String PROPERTY_BASE_NL = "basenl"; //$NON-NLS-1$ 
	public static final String PROPERTY_OS = "os"; //$NON-NLS-1$
	public static final String PROPERTY_BASE_OS = "baseos"; //$NON-NLS-1$
	public static final String PROPERTY_QUIET = "quiet"; //$NON-NLS-1$
	public static final String PROPERTY_PLUGIN_DESTINATION = "plugin.destination"; //$NON-NLS-1$
	public static final String PROPERTY_TARGET = "target"; //$NON-NLS-1$
	public static final String PROPERTY_TEMP_FOLDER = "temp.folder"; //$NON-NLS-1$
	public static final String PROPERTY_VERSION_SUFFIX = "version.suffix"; //$NON-NLS-1$
	public static final String PROPERTY_WS = "ws"; //$NON-NLS-1$
	public static final String PROPERTY_BASE_WS = "basews"; //$NON-NLS-1$
	public static final String PROPERTY_ARCHIVE_NAME = "archiveName"; //$NON-NLS-1$
	public static final String PROPERTY_BUILD_LABEL = "buildLabel"; //$NON-NLS-1$
	public static final String PROPERTY_JAVAC_FAIL_ON_ERROR = "javacFailOnError"; //$NON-NLS-1$ 
	public static final String PROPERTY_JAVAC_DEBUG_INFO = "javacDebugInfo"; //$NON-NLS-1$
	public static final String PROPERTY_JAVAC_VERBOSE = "javacVerbose"; //$NON-NLS-1$
	public static final String PROPERTY_JAVAC_SOURCE = "javacSource"; //$NON-NLS-1$
	public static final String PROPERTY_JAVAC_TARGET = "javacTarget"; //$NON-NLS-1$
	public static final String PROPERTY_PLUGIN_JAVAC_SOURCE = "pluginJavacSource"; //$NON-NLS-1$
	public static final String PROPERTY_PLUGIN_BOOTCLASSPATH = "pluginBootClasspath"; //$NON-NLS-1$
	public static final String PROPERTY_PLUGIN_JAVAC_TARGET = "pluginJavacTarget"; //$NON-NLS-1$
	public static final String PROPERTY_JAVAC_COMPILERARG = "compilerArg"; //$NON-NLS-1$
	public static final String PROPERTY_ARCHIVE_PREFIX = "archivePrefix"; //$NON-NLS-1$
	public static final String PROPERTY_PLUGIN_ARCHIVE_PREFIX = "pluginArchivePrefix"; //$NON-NLS-1$
	public static final String PROPERTY_FEATURE_ARCHIVE_PREFIX = "featureArchivePrefix"; //$NON-NLS-1$
	public static final String PROPERTY_COLLECTING_FOLDER = "collectingFolder"; //$NON-NLS-1$
	public static final String PROPERTY_ARCHIVE_FULLPATH = "archiveFullPath"; //$NON-NLS-1$
	public static final String PROPERTY_ARCHIVE_PARENT = "archiveParentFolder"; //$NON-NLS-1$
	public static final String PROPERTY_BUILD_ID_PARAM = "buildId"; //$NON-NLS-1$
	public static final String PROPERTY_ZIP_ARGS = "zipargs"; //$NON-NLS-1$
	public static final String PROPERTY_TAR_ARGS = "tarargs"; //$NON-NLS-1$
	public static final String PROPERTY_DOWNLOAD_DIRECTORY = "downloadDirectory"; //$NON-NLS-1$
	public static final String PROPERTY_RESOURCE_PATH = "resourcePath"; //$NON-NLS-1$
	public static final String PROPERTY_PLUGIN_TEMP = "pluginTemp"; //$NON-NLS-1$
	public static final String PROPERTY_BUILD_TEMP = "buildTempFolder"; //$NON-NLS-1$
	public static final String PROPERTY_PRE = "pre."; //$NON-NLS-1$
	public static final String PROPERTY_POST = "post."; //$NON-NLS-1$
	public static final String PROPERTY_SOURCE_FOLDER = "source.folder"; //$NON-NLS-1$
	public static final String PROPERTY_TARGET_FOLDER = "target.folder"; //$NON-NLS-1$
	public static final String PROPERTY_JAR_LOCATION = "jar.Location"; //$NON-NLS-1$
	public static final String PROPERTY_CLASSPATH = ".classpath"; //$NON-NLS-1$

	public static final String PROPERTY_ASSEMBLY_TMP = "assemblyTempDir"; //$NON-NLS-1$

	//Jar signing properties
	public static final String PROPERTY_SIGN_ALIAS = "sign.alias"; //$NON-NLS-1$
	public static final String PROPERTY_SIGN_KEYSTORE = "sign.keystore";  //$NON-NLS-1$
	public static final String PROPERTY_SIGN_STOREPASS ="sign.storepass"; //$NON-NLS-1$
	
	//JNLP generation properties
	public static final String PROPERTY_JNLP_CODEBASE = "jnlp.codebase"; //$NON-NLS-1$
	public static final String PROPERTY_JNLP_J2SE = "jnlp.j2se";  //$NON-NLS-1$
	
	//Output format supported
	public static final String FORMAT_TAR = "tar"; //$NON-NLS-1$
	public static final String FORMAT_ANTTAR = "antTar"; //$NON-NLS-1$
	public static final String FORMAT_ZIP = "zip"; //$NON-NLS-1$
	public static final String FORMAT_ANTZIP = "antZip"; //$NON-NLS-1$
	public static final String FORMAT_FOLDER = "folder"; //$NON-NLS-1$
}
