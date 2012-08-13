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
package org.eclipse.ui.trace.internal;

import org.eclipse.osgi.util.NLS;

/**
 * NLS messages for the Tracing preference page
 */
public class Messages extends NLS {

	/**
	 * The text to display if a component label is missing
	 */
	public static String missingLabelValue = "missingLabelValue"; //$NON-NLS-1$

	/**
	 * Title of the tracing tree
	 */
	public static String tracingTreeTile = "tracingTreeTitle"; //$NON-NLS-1$

	/**
	 * Description text for the preference page.
	 */
	public static String preferencePageDescription = "preferencePageDescription"; //$NON-NLS-1$

	/**
	 * 'Enable tracing' button
	 */
	public static String enableTracingButtonLabel = "enableTracingButtonLabel"; //$NON-NLS-1$

	/**
	 * 'Tracing Option' column header text
	 */
	public static String columnHeaderTracingString = "columnHeaderTracingString"; //$NON-NLS-1$

	/**
	 * 'Value' column header text
	 */
	public static String columnHeaderTracingValue = "columnHeaderTracingValue"; //$NON-NLS-1$

	/**
	 * 'Search for a trace string' filter text field value
	 */
	public static String filterSearchText = "filterSearchText"; //$NON-NLS-1$

	/**
	 * 'Browse...' button
	 */
	public static String tracingFileBrowseButton = "tracingFileBrowseButton"; //$NON-NLS-1$

	/**
	 * 'Tracing Options' group label
	 */
	public static String tracingOptionsGroup = "tracingOptionsGroup"; //$NON-NLS-1$

	/**
	 * Maximum number of backup tracing files label
	 */
	public static String tracingFileMaxCountLabel = "tracingFileMaxCountLabel"; //$NON-NLS-1$

	/**
	 * Maximum size of the tracing files label
	 */
	public static String tracingFileMaxSizeLabel = "tracingFileMaxSizeLabel"; //$NON-NLS-1$

	/**
	 * 'Specify the tracing file' label
	 */
	public static String tracingFileLabel = "tracingFileLabel"; //$NON-NLS-1$

	/**
	 * 'Invalid maximum value input' for the size field
	 */
	public static String tracingFileInvalidMaxSize = "tracingFileInvalidMaxSize"; //$NON-NLS-1$

	/**
	 * An invalid empty tracing file was specified
	 */
	public static String tracingFileInvalid = "tracingFileInvalid"; //$NON-NLS-1$

	/**
	 * 'Invalid maximum value input' for the count field
	 */
	public static String tracingFileInvalidMaxCount = "tracingFileInvalidMaxSize"; //$NON-NLS-1$

	private final static String BUNDLE_NAME = "org.eclipse.ui.trace.internal.messages"; //$NON-NLS-1$
	static {
		NLS.initializeMessages(Messages.BUNDLE_NAME, Messages.class);
	}
	public static String TracingComponentColumnEditingSupport_false;

	public static String TracingComponentColumnEditingSupport_true;

	public static String TracingPreferencePage_applicationLaunchedInDebugModeWarning;
}