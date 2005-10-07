/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

public interface IBuildPropertiesConstants {
	public final static String PERMISSIONS = "permissions"; //$NON-NLS-1$
	public final static String LINK = "link"; //$NON-NLS-1$
	public final static String EXECUTABLE = "executable"; //$NON-NLS-1$
	public final static String ROOT_PREFIX = "root."; //$NON-NLS-1$
	public final static String ROOT = "root"; //$NON-NLS-1$

	public final static String TRUE = "true"; //$NON-NLS-1$
	public final static String FALSE = "false"; //$NON-NLS-1$

	public static final String PROPERTY_JAR_EXTRA_CLASSPATH = "jars.extra.classpath"; //$NON-NLS-1$
	public static final String PROPERTY_JAR_ORDER = "jars.compile.order"; //$NON-NLS-1$
	public static final String PROPERTY_SOURCE_PREFIX = "source."; //$NON-NLS-1$
	public static final String PROPERTY_OUTPUT_PREFIX = "output."; //$NON-NLS-1$
	public static final String PROPERTY_EXTRAPATH_PREFIX = "extra."; //$NON-NLS-1$	
	public static final String PROPERTY_EXCLUDE_PREFIX = "exclude.";  //$NON-NLS-1$
	public static final String PROPERTY_JAR_SUFFIX = ".jar"; //$NON-NLS-1$
	public static final String PROPERTY_MANIFEST_PREFIX = "manifest."; //$NON-NLS-1$

	public static final String PROPERTY_QUALIFIER = "qualifier"; //$NON-NLS-1$
	public static final String PROPERTY_NONE = "none"; //$NON-NLS-1$
	public static final String PROPERTY_CONTEXT = "context"; //$NON-NLS-1$

	public final static String GENERATION_SOURCE_PREFIX = "generate."; //$NON-NLS-1$
	public final static String GENERATION_SOURCE_FEATURE_PREFIX = GENERATION_SOURCE_PREFIX + "feature@"; //$NON-NLS-1$
	public final static String GENERATION_SOURCE_PLUGIN_PREFIX = GENERATION_SOURCE_PREFIX + "plugin@"; //$NON-NLS-1$
	public final static String PROPERTY_SOURCE_FEATURE_NAME = "sourceFeature.name"; //$NON-NLS-1$
	
	public static final String PROPERTY_CUSTOM = "custom"; //$NON-NLS-1$
	public static final String PROPERTY_ZIP_SUFFIX = ".zip"; //$NON-NLS-1$

	public static final String PROPERTY_BIN_EXCLUDES = "bin.excludes"; //$NON-NLS-1$
	public static final String PROPERTY_BIN_INCLUDES = "bin.includes"; //$NON-NLS-1$

	public static final String PROPERTY_SRC_EXCLUDES = "src.excludes"; //$NON-NLS-1$
	public static final String PROPERTY_SRC_INCLUDES = "src.includes"; //$NON-NLS-1$

	public static final String DEFAULT_MATCH_ALL = "*"; //$NON-NLS-1$
	public static final String DEFAULT_FINAL_SHAPE = "*"; //$NON-NLS-1$
	
	public static final String PROPERTY_OVERWRITE_ROOTFILES = "overwriteRootFiles";  //$NON-NLS-1$
	
	public static final String PROPERTY_CUSTOM_BUILD_CALLBACKS = "customBuildCallbacks"; //$NON-NLS-1$
	
	//Internal usage only
	public static final String SOURCE_PLUGIN = "sourcePlugin"; //$NON-NLS-1$
}

