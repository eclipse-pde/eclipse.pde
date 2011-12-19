/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.trace.internal.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import org.eclipse.osgi.service.debug.DebugTrace;
import org.eclipse.ui.trace.internal.TracingUIActivator;
import org.eclipse.ui.trace.internal.datamodel.TracingComponentDebugOption;
import org.osgi.framework.Bundle;

/**
 * Utility APIs for the Product Tracing UI.
 */
public class TracingUtils {

	/**
	 * Returns true if the specified {@link TracingComponentDebugOption} is a boolean DebugOption (i.e. has a true or
	 * false value)
	 * 
	 * @param debugOption
	 *            A non-null {@link TracingComponentDebugOption}
	 * @return Returns true if the specified {@link TracingComponentDebugOption} has a value of "true" or "false";
	 *         Otherwise false is returned.
	 */
	public final static boolean isValueBoolean(final TracingComponentDebugOption debugOption) {

		if (TracingUIActivator.DEBUG) {
			TRACE.traceEntry(TracingConstants.TRACE_DEBUG_STRING, debugOption);
		}
		assert (debugOption != null);
		boolean result = TracingUtils.isValueBoolean(debugOption.getOptionPathValue());
		if (TracingUIActivator.DEBUG) {
			TRACE.traceExit(TracingConstants.TRACE_DEBUG_STRING, Boolean.valueOf(result));
		}
		return result;
	}

	/**
	 * Returns true if the specified {@link String} is a boolean value of 'true' or 'false'.
	 * 
	 * @param value
	 *            A {@link String} to check if it is the value 'true' or 'false'.
	 * @return Returns true if the specified {@link String} is the value 'true' or 'false'; Otherwise false is returned.
	 */
	public final static boolean isValueBoolean(final String value) {

		if (TracingUIActivator.DEBUG) {
			TRACE.traceEntry(TracingConstants.TRACE_DEBUG_STRING, value);
		}
		boolean isBoolean = false;
		if ((value != null) && (value.toLowerCase().equals(TracingConstants.DEBUG_OPTION_VALUE_FALSE) || value.toLowerCase().equals(TracingConstants.DEBUG_OPTION_VALUE_TRUE))) {
			isBoolean = true;
		}
		if (TracingUIActivator.DEBUG) {
			TRACE.traceExit(TracingConstants.TRACE_DEBUG_STRING, String.valueOf(isBoolean));
		}
		return isBoolean;
	}

	/**
	 * Convert a {@link TracingComponentDebugOption} to a {@link String} for persistence.
	 * 
	 * @param debugOption
	 *            The {@link TracingComponentDebugOption} to convert to a string representation.
	 * @return A {@link String} representing the {@link TracingComponentDebugOption}.
	 */
	public final static String convertToString(final TracingComponentDebugOption debugOption) {

		if (TracingUIActivator.DEBUG) {
			TRACE.traceEntry(TracingConstants.TRACE_DEBUG_STRING, debugOption);
		}
		final StringBuffer buffer = new StringBuffer();
		if (debugOption != null) {
			// return an option as "key=value;"
			buffer.append(debugOption.getOptionPath());
			buffer.append(TracingConstants.DEBUG_OPTION_PATH_SEPARATOR);
			buffer.append(debugOption.getOptionPathValue());
			buffer.append(TracingConstants.DEBUG_OPTION_PREFERENCE_SEPARATOR);
		}
		if (TracingUIActivator.DEBUG) {
			TRACE.traceExit(TracingConstants.TRACE_DEBUG_STRING, buffer.toString());
		}
		return buffer.toString();
	}

	/**
	 * Convert a {@link String} representing the persisted preference into a {@link TracingComponentDebugOption}.
	 * 
	 * @param debugOptionAsString
	 *            The {@link String} representation of the persisted {@link TracingComponentDebugOption}.
	 * @return A {@link TracingComponentDebugOption} with no parent set or <code>null</code> if debugOptionAsString is
	 *         null.
	 */
	public final static TracingComponentDebugOption convertToDebugOption(final String debugOptionAsString) {

		if (TracingUIActivator.DEBUG) {
			TRACE.traceEntry(TracingConstants.TRACE_DEBUG_STRING, debugOptionAsString);
		}
		TracingComponentDebugOption debugOption = null;
		if (debugOptionAsString != null) {
			int separatorIndex = debugOptionAsString.indexOf(TracingConstants.DEBUG_OPTION_PATH_SEPARATOR);
			if (TracingUIActivator.DEBUG) {
				TRACE.trace(TracingConstants.TRACE_DEBUG_STRING, "separatorIndex: " + separatorIndex); //$NON-NLS-1$
			}
			if (separatorIndex != -1) {
				String key = debugOptionAsString.substring(0, separatorIndex);
				String value = debugOptionAsString.substring(separatorIndex + 1, debugOptionAsString.length());
				if (TracingUIActivator.DEBUG) {
					TRACE.trace(TracingConstants.TRACE_DEBUG_STRING, "key: " + key); //$NON-NLS-1$
					TRACE.trace(TracingConstants.TRACE_DEBUG_STRING, "value: " + value); //$NON-NLS-1$
				}
				// the debug option does not need a parent at this point
				debugOption = new TracingComponentDebugOption(key, value);
			}
		}
		if (TracingUIActivator.DEBUG) {
			TRACE.traceExit(TracingConstants.TRACE_DEBUG_STRING, debugOption);
		}
		return debugOption;
	}

	/**
	 * Convert a {@link String} representing the persisted preference into a {@link Map} object.
	 * 
	 * @param debugOptions
	 *            The input {@link String} containing many encoded key=value pairs.
	 * @return A {@link Map} object containing the key=value pairs in the encoded debug options {@link String}
	 */
	public final static Map<String, String> convertToMap(final String debugOptions) {

		if (TracingUIActivator.DEBUG) {
			TRACE.traceEntry(TracingConstants.TRACE_DEBUG_STRING, debugOptions);
		}
		final Map<String, String> result = new HashMap<String, String>();
		if (debugOptions != null) {
			StringTokenizer optionTokens = new StringTokenizer(debugOptions, TracingConstants.DEBUG_OPTION_PREFERENCE_SEPARATOR);
			if (TracingUIActivator.DEBUG) {
				TRACE.trace(TracingConstants.TRACE_DEBUG_STRING, "debug options found: " + optionTokens.countTokens()); //$NON-NLS-1$
			}
			while (optionTokens.hasMoreTokens()) {
				TracingComponentDebugOption newOption = TracingUtils.convertToDebugOption(optionTokens.nextToken());
				if (newOption != null) {
					result.put(newOption.getOptionPath(), newOption.getOptionPathValue());
				}
			}
		}
		if (TracingUIActivator.DEBUG) {
			TRACE.traceExit(TracingConstants.TRACE_DEBUG_STRING, result);
		}
		return result;
	}

	/**
	 * Locates the .options file in the specified bundle and loads in the entries via {@link Properties} object.
	 * 
	 * @param bundle
	 *            The {@link Bundle} to access the debug options defined for it.
	 * @return Returns a {@link Properties} object containing the various options from the specified bundles .options
	 *         file. If the bundle does not have a .options file then an empty {@link Properties} object is returned.
	 */
	public final static Properties loadOptionsFromBundle(final Bundle bundle) {

		if (TracingUIActivator.DEBUG) {
			TRACE.traceEntry(TracingConstants.TRACE_DEBUG_STRING, bundle);
		}
		final Properties optionsProperties = new Properties();
		if (bundle != null) {
			URL optionsFile = bundle.getEntry(TracingConstants.OPTIONS_FILENAME);
			if (optionsFile != null) {
				if (TracingUIActivator.DEBUG) {
					TRACE.trace(TracingConstants.TRACE_DEBUG_STRING, "Reading in .options file found in: " + optionsFile.getPath()); //$NON-NLS-1$
				}
				// read the file
				InputStream optionsFileInStream = null;
				try {
					optionsFileInStream = optionsFile.openStream();
					optionsProperties.load(optionsFileInStream);
				} catch (IOException ioEx) {
					// couldn't read the .options file - can't do anything other than to log the exception
					if (TracingUIActivator.DEBUG) {
						TRACE.trace(TracingConstants.TRACE_DEBUG_STRING, "IOException while processing the .options file for bundle " + bundle, ioEx); //$NON-NLS-1$
					}
					TracingUIActivator.getDefault().logException(ioEx);
				} finally {
					if (optionsFileInStream != null) {
						try {
							optionsFileInStream.close();
						} catch (IOException ioEx) {
							// can't do anything other than to log the exception
							TracingUIActivator.getDefault().logException(ioEx);
						}
					}
				}
			}
		}
		if (TracingUIActivator.DEBUG) {
			TRACE.traceExit(TracingConstants.TRACE_DEBUG_STRING, optionsProperties);
		}
		return optionsProperties;
	}

	/** Trace object for this bundle */
	private final static DebugTrace TRACE = TracingUIActivator.getDefault().getTrace();
}