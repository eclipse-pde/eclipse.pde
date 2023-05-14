/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.trace.internal.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.trace.internal.TracingUIActivator;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Tracing preference handler.
 */
public class PreferenceHandler extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		setDefaultPreferences();
	}

	/**
	 * Accessor for the preference store for this bundle
	 *
	 * @return The {@link IEclipsePreferences} preference node at the instance scope for this bundle or null if it does
	 *         not exist.
	 */
	public static IEclipsePreferences getPreferences() {
		return InstanceScope.INSTANCE.getNode(TracingConstants.BUNDLE_ID);
	}

	/**
	 * Access the current persisted {@link TracingConstants#PREFERENCE_ENTRIES_IDENTIFIER} entry in the preferences as
	 * {@link Map}.
	 *
	 * @return A {@link Map} containing the currently persisted {@link TracingConstants#PREFERENCE_ENTRIES_IDENTIFIER}
	 *         preferences.
	 */
	public static Map<String, String> getPreferenceProperties() {
		IEclipsePreferences tracingPrefs = InstanceScope.INSTANCE.getNode(TracingConstants.BUNDLE_ID);
		final String componentsAsString = tracingPrefs.get(TracingConstants.PREFERENCE_ENTRIES_IDENTIFIER, TracingConstants.EMPTY_STRING);
		Map<String, String> options = null;
		if (!componentsAsString.equals(TracingConstants.EMPTY_STRING)) {
			options = TracingUtils.convertToMap(componentsAsString);
		} else {
			options = Collections.emptyMap();
		}
		return options;
	}

	/**
	 * Set the default preferences
	 */
	public static void setDefaultPreferences() {
		final Map<String, String> prefValues = new HashMap<>(5);
		// tracing is off by default
		prefValues.put(TracingConstants.PREFERENCE_ENABLEMENT_IDENTIFIER, Boolean.toString(false));
		// see org.eclipse.osgi.framework.debug.EclipseDebugTrace#DEFAULT_TRACE_FILES
		prefValues.put(TracingConstants.PREFERENCE_MAX_FILE_COUNT_IDENTIFIER, Integer.toString(10));
		// see org.eclipse.osgi.framework.debug.EclipseDebugTrace#DEFAULT_TRACE_FILE_SIZE
		prefValues.put(TracingConstants.PREFERENCE_MAX_FILE_SIZE_IDENTIFIER, Integer.toString(1000));
		prefValues.put(TracingConstants.PREFERENCE_OUTPUT_STANDARD_STREAM, Boolean.toString(false));
		// no trace entries
		prefValues.put(TracingConstants.PREFERENCE_ENTRIES_IDENTIFIER, TracingConstants.EMPTY_STRING);
		savePreferences(prefValues);
	}

	/**
	 * Flush the preference store to disk.
	 *
	 * @param entries
	 *            A {@link Map} of entries to persist to the preferences. The key of the {@link Map} is the key of the
	 *            preference and the value of the {@link Map} is the value for this key in the preferences.
	 */
	public static void savePreferences(final Map<String, String> entries) {
		final IEclipsePreferences preferences = PreferenceHandler.getPreferences();
		if (entries != null) {
			// persist each entry
			final Iterator<Map.Entry<String, String>> entriesIterator = entries.entrySet().iterator();
			while (entriesIterator.hasNext()) {
				Map.Entry<String, String> entry = entriesIterator.next();
				preferences.put(entry.getKey(), entry.getValue());
			}
		}
		try {
			preferences.flush();
		} catch (BackingStoreException backingStoreEx) {
			TracingUIActivator.getDefault().logException(backingStoreEx);
		}
	}

	/**
	 * Is tracing enabled in the preferences store
	 *
	 * @return <code>true</code> if tracing is enabled in the preferences; Otherwise, <code>false</code> is returned.
	 */
	public static boolean isTracingEnabled() {
		final IScopeContext[] lookupOrder = new IScopeContext[] { InstanceScope.INSTANCE };
		IPreferencesService prefService = Platform.getPreferencesService();
		prefService.setDefaultLookupOrder(TracingConstants.BUNDLE_ID, null, new String[] {InstanceScope.SCOPE});
		return prefService.getBoolean(TracingConstants.BUNDLE_ID, TracingConstants.PREFERENCE_ENABLEMENT_IDENTIFIER, false, lookupOrder);
	}

	/**
	 * Accessor for the maximum file count in the preference store
	 *
	 * @return The maximum file count in the preference store or the value 10 if it's not defined.
	 */
	public static int getMaxFileCount() {
		final IScopeContext[] lookupOrder = new IScopeContext[] { InstanceScope.INSTANCE };
		IPreferencesService prefService = Platform.getPreferencesService();
		prefService.setDefaultLookupOrder(TracingConstants.BUNDLE_ID, null, new String[] {InstanceScope.SCOPE});
		return prefService.getInt(TracingConstants.BUNDLE_ID, TracingConstants.PREFERENCE_MAX_FILE_COUNT_IDENTIFIER, 10, lookupOrder);
	}

	/**
	 * Accessor for the maximum file size in the preference store
	 *
	 * @return The maximum file size in the preference store or the value 1000 if it's not defined.
	 */
	public static int getMaxFileSize() {
		final IScopeContext[] lookupOrder = new IScopeContext[] { InstanceScope.INSTANCE };
		IPreferencesService prefService = Platform.getPreferencesService();
		prefService.setDefaultLookupOrder(TracingConstants.BUNDLE_ID, null, new String[] {InstanceScope.SCOPE});
		return prefService.getInt(TracingConstants.BUNDLE_ID, TracingConstants.PREFERENCE_MAX_FILE_SIZE_IDENTIFIER, 1000, lookupOrder);
	}

	/**
	 * Accessor for the file path in the preference store
	 *
	 * @return The file path in the preference store or the default value if it's not defined.
	 */
	public static String getFilePath() {
		final IScopeContext[] lookupOrder = new IScopeContext[] { InstanceScope.INSTANCE };
		IPreferencesService prefService = Platform.getPreferencesService();
		prefService.setDefaultLookupOrder(TracingConstants.BUNDLE_ID, null, new String[] {InstanceScope.SCOPE});
		return prefService.getString(TracingConstants.BUNDLE_ID, TracingConstants.PREFERENCE_FILE_PATH, DebugOptionsHandler.getDebugOptions().getFile() == null ? null : DebugOptionsHandler.getDebugOptions().getFile().getAbsolutePath(), lookupOrder);
	}

	/**
	 * Accessor for the output to standard output stream selection in the preference store
	 *
	 * @return The output to standard output stream selection in the preference store or the default value if it's not defined.
	 */
	public static String getOutputToStandardStream() {
		final IScopeContext[] lookupOrder = new IScopeContext[] { InstanceScope.INSTANCE };
		IPreferencesService prefService = Platform.getPreferencesService();
		prefService.setDefaultLookupOrder(TracingConstants.BUNDLE_ID, null, new String[] {InstanceScope.SCOPE});
		return prefService.getString(TracingConstants.BUNDLE_ID, TracingConstants.PREFERENCE_OUTPUT_STANDARD_STREAM, "false", lookupOrder); //$NON-NLS-1$
	}

}