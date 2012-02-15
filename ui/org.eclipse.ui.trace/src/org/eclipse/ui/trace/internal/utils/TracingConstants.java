/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.trace.internal.utils;

/**
 * A collection of constant values used by the tracing UI
 */
public class TracingConstants {

	/** The name of the bundle */
	public final static String BUNDLE_ID = "org.eclipse.ui.trace"; //$NON-NLS-1$

	/** The separator for option paths in key=value pairs */
	public final static String DEBUG_OPTION_PATH_SEPARATOR = "="; //$NON-NLS-1$

	/** The value for a debug option that is disabled */
	public final static String DEBUG_OPTION_VALUE_FALSE = "false"; //$NON-NLS-1$

	/** The value for a debug option that is enabled **/
	public final static String DEBUG_OPTION_VALUE_TRUE = "true"; //$NON-NLS-1$

	/** Tracing Component extension point name */
	public final static String TRACING_EXTENSION_POINT_NAME = "traceComponents"; //$NON-NLS-1$

	/** The name of the 'id' attribute for a Tracing Component */
	public final static String TRACING_EXTENSION_ID_ATTRIBUTE = "id"; //$NON-NLS-1$

	/** The name of the 'label' attribute for a Tracing Component */
	public final static String TRACING_EXTENSION_LABEL_ATTRIBUTE = "label"; //$NON-NLS-1$

	/** The name of the 'bundle' attribute for a Tracing Component */
	public final static String TRACING_EXTENSION_BUNDLE_ATTRIBUTE = "bundle"; //$NON-NLS-1$

	/** The name of the 'name' attribute for a bundle in a Tracing Component */
	public final static String TRACING_EXTENSION_BUNDLE_NAME_ATTRIBUTE = "name"; //$NON-NLS-1$

	/** The name of the 'consumed' attribute for a bundle in a Tracing Component */
	public final static String TRACING_EXTENSION_BUNDLE_CONSUMED_ATTRIBUTE = "consumed"; //$NON-NLS-1$

	/** An empty {@link String} array **/
	public final static String[] EMPTY_STRING_ARRAY = new String[0];

	/** An empty {@link String} **/
	public final static String EMPTY_STRING = ""; //$NON-NLS-1$

	/** The index of the label column in the tree */
	public final static int LABEL_COLUMN_INDEX = 0;

	/** The index of the value column in the tree */
	public final static int VALUE_COLUMN_INDEX = 1;

	/** The name of the .options file used to store the debug options for a bundle */
	public final static String OPTIONS_FILENAME = ".options"; //$NON-NLS-1$

	/** The system property used to specify size a trace file can grow before it is rotated */
	public static final String PROP_TRACE_SIZE_MAX = "eclipse.trace.size.max"; //$NON-NLS-1$

	/** The system property used to specify the maximum number of backup trace files to use */
	public static final String PROP_TRACE_FILE_MAX = "eclipse.trace.backup.max"; //$NON-NLS-1$

	/** The separator character for a debug option represented as a string, i.e. key1=value1;key2=value2;key3=value3; */
	public final static String DEBUG_OPTION_PREFERENCE_SEPARATOR = ";"; //$NON-NLS-1$

	/** The preference identifier for the tracing enablement state */
	public final static String PREFERENCE_ENABLEMENT_IDENTIFIER = "tracingEnabled"; //$NON-NLS-1$

	/** The preference identifier for the list of tracing entries */
	public final static String PREFERENCE_ENTRIES_IDENTIFIER = "tracingEntries"; //$NON-NLS-1$

	/** The preference identifier for the maximum size of the tracing files */
	public final static String PREFERENCE_MAX_FILE_SIZE_IDENTIFIER = "tracingMaxFileSize"; //$NON-NLS-1$

	/** The preference identifier for the maximum number of tracing files */
	public final static String PREFERENCE_MAX_FILE_COUNT_IDENTIFIER = "tracingMaxFileCount"; //$NON-NLS-1$

	/** The preference identifier for the location of tracing files */
	public final static String PREFERENCE_FILE_PATH = "tracingFilePath"; //$NON-NLS-1$

}