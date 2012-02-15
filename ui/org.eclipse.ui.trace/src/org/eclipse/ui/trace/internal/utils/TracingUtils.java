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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import org.eclipse.ui.trace.internal.TracingUIActivator;
import org.eclipse.ui.trace.internal.datamodel.TracingComponentDebugOption;
import org.osgi.framework.Bundle;

/**
 * Utility APIs for the Product Tracing UI.
 */
public class TracingUtils {

	/**
	 * Returns true if the specified {@link String} is a boolean value of 'true' or 'false'.
	 * 
	 * @param value
	 *            A {@link String} to check if it is the value 'true' or 'false'.
	 * @return Returns true if the specified {@link String} is the value 'true' or 'false'; Otherwise false is returned.
	 */
	public static boolean isValueBoolean(final String value) {
		return (value != null) && (value.toLowerCase().equals(TracingConstants.DEBUG_OPTION_VALUE_FALSE) || value.toLowerCase().equals(TracingConstants.DEBUG_OPTION_VALUE_TRUE));
	}

	/**
	 * Convert a {@link TracingComponentDebugOption} to a {@link String} for persistence.
	 * 
	 * @param debugOption
	 *            The {@link TracingComponentDebugOption} to convert to a string representation.
	 * @return A {@link String} representing the {@link TracingComponentDebugOption}.
	 */
	public static String convertToString(final TracingComponentDebugOption debugOption) {
		final StringBuffer buffer = new StringBuffer();
		if (debugOption != null) {
			// return an option as "key=value;"
			buffer.append(debugOption.getOptionPath());
			buffer.append(TracingConstants.DEBUG_OPTION_PATH_SEPARATOR);
			buffer.append(debugOption.getOptionPathValue());
			buffer.append(TracingConstants.DEBUG_OPTION_PREFERENCE_SEPARATOR);
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
	public static TracingComponentDebugOption convertToDebugOption(final String debugOptionAsString) {
		TracingComponentDebugOption debugOption = null;
		if (debugOptionAsString != null) {
			int separatorIndex = debugOptionAsString.indexOf(TracingConstants.DEBUG_OPTION_PATH_SEPARATOR);
			if (separatorIndex != -1) {
				String key = debugOptionAsString.substring(0, separatorIndex);
				String value = debugOptionAsString.substring(separatorIndex + 1, debugOptionAsString.length());
				// the debug option does not need a parent at this point
				debugOption = new TracingComponentDebugOption(key, value);
			}
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
	public static Map<String, String> convertToMap(final String debugOptions) {
		final Map<String, String> result = new HashMap<String, String>();
		if (debugOptions != null) {
			StringTokenizer optionTokens = new StringTokenizer(debugOptions, TracingConstants.DEBUG_OPTION_PREFERENCE_SEPARATOR);
			while (optionTokens.hasMoreTokens()) {
				TracingComponentDebugOption newOption = TracingUtils.convertToDebugOption(optionTokens.nextToken());
				if (newOption != null) {
					result.put(newOption.getOptionPath(), newOption.getOptionPathValue());
				}
			}
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
	public static Properties loadOptionsFromBundle(final Bundle bundle) {
		final Properties optionsProperties = new Properties();
		if (bundle != null) {
			URL optionsFile = bundle.getEntry(TracingConstants.OPTIONS_FILENAME);
			if (optionsFile != null) {
				// read the file
				InputStream optionsFileInStream = null;
				try {
					optionsFileInStream = optionsFile.openStream();
					optionsProperties.load(optionsFileInStream);
				} catch (IOException ioEx) {
					// couldn't read the .options file - can't do anything other than to log the exception
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
		return optionsProperties;
	}

}