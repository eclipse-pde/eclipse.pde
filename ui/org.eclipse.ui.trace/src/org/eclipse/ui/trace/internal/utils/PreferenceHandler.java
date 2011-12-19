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

import java.util.*;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.*;
import org.eclipse.osgi.service.debug.DebugTrace;
import org.eclipse.ui.trace.internal.TracingUIActivator;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Tracing preference handler.
 */
@SuppressWarnings("deprecation")
public class PreferenceHandler extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {

		if (TracingUIActivator.DEBUG_PREFERENCES) {
			TRACE.traceEntry(TracingConstants.TRACE_PREFERENCES_STRING);
		}
		PreferenceHandler.setDefaultPreferences();
		if (TracingUIActivator.DEBUG_PREFERENCES) {
			TRACE.traceExit(TracingConstants.TRACE_PREFERENCES_STRING);
		}
	}

	/**
	 * Accessor for the preference store for this bundle
	 * 
	 * @return The {@link IEclipsePreferences} preference node at the instance scope for this bundle or null if it does
	 *         not exist.
	 */
	public final static IEclipsePreferences getPreferences() {

		if (TracingUIActivator.DEBUG_PREFERENCES) {
			TRACE.traceEntry(TracingConstants.TRACE_PREFERENCES_STRING);
		}
		IEclipsePreferences node = new InstanceScope().getNode(TracingConstants.BUNDLE_ID);
		if (TracingUIActivator.DEBUG_PREFERENCES) {
			TRACE.traceExit(TracingConstants.TRACE_PREFERENCES_STRING, node);
		}
		return node;
	}

	/**
	 * Access the current persisted {@link TracingConstants#PREFERENCE_ENTRIES_IDENTIFIER} entry in the preferences as
	 * {@link Map}.
	 * 
	 * @return A {@link Map} containing the currently persisted {@link TracingConstants#PREFERENCE_ENTRIES_IDENTIFIER}
	 *         preferences.
	 */
	public final static Map<String, String> getPreferenceProperties() {

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
	public final static void setDefaultPreferences() {

		if (TracingUIActivator.DEBUG_PREFERENCES) {
			TRACE.traceEntry(TracingConstants.TRACE_PREFERENCES_STRING);
		}
		final Map<String, String> prefValues = new HashMap<String, String>(5);
		// tracing is off by default
		prefValues.put(TracingConstants.PREFERENCE_ENABLEMENT_IDENTIFIER, Boolean.toString(false));
		// see org.eclipse.osgi.framework.debug.EclipseDebugTrace#DEFAULT_TRACE_FILES
		prefValues.put(TracingConstants.PREFERENCE_MAX_FILE_COUNT_IDENTIFIER, Integer.toString(10));
		// see org.eclipse.osgi.framework.debug.EclipseDebugTrace#DEFAULT_TRACE_FILE_SIZE
		prefValues.put(TracingConstants.PREFERENCE_MAX_FILE_SIZE_IDENTIFIER, Integer.toString(1000));
		// no trace entries
		prefValues.put(TracingConstants.PREFERENCE_ENTRIES_IDENTIFIER, TracingConstants.EMPTY_STRING);
		PreferenceHandler.savePreferences(prefValues);
		if (TracingUIActivator.DEBUG_PREFERENCES) {
			TRACE.traceExit(TracingConstants.TRACE_PREFERENCES_STRING);
		}
	}

	/**
	 * Flush the preference store to disk.
	 * 
	 * @param entries
	 *            A {@link Map} of entries to persist to the preferences. The key of the {@link Map} is the key of the
	 *            preference and the value of the {@link Map} is the value for this key in the preferences.
	 */
	public final static void savePreferences(final Map<String, String> entries) {

		if (TracingUIActivator.DEBUG_PREFERENCES) {
			TRACE.traceEntry(TracingConstants.TRACE_PREFERENCES_STRING, entries);
		}
		final IEclipsePreferences preferences = PreferenceHandler.getPreferences();
		if (entries != null) {
			// persist each entry
			final Iterator<Map.Entry<String, String>> entriesIterator = entries.entrySet().iterator();
			while (entriesIterator.hasNext()) {
				Map.Entry<String, String> entry = entriesIterator.next();
				preferences.put(entry.getKey(), entry.getValue());
				if (TracingUIActivator.DEBUG_PREFERENCES) {
					TRACE.trace(TracingConstants.TRACE_PREFERENCES_STRING, "Set preference '" + entry.getKey() + "' to the value: " + entry.getValue()); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
		try {
			preferences.flush();
		} catch (BackingStoreException backingStoreEx) {
			if (TracingUIActivator.DEBUG_PREFERENCES) {
				TRACE.trace(TracingConstants.TRACE_PREFERENCES_STRING, "Failed to save the preferences", backingStoreEx); //$NON-NLS-1$
			}
			TracingUIActivator.getDefault().logException(backingStoreEx);
		}
		if (TracingUIActivator.DEBUG_PREFERENCES) {
			TRACE.traceExit(TracingConstants.TRACE_PREFERENCES_STRING);
		}
	}

	/**
	 * Is tracing enabled in the preferences store
	 * 
	 * @return <code>true</code> if tracing is enabled in the preferences; Otherwise, <code>false</code> is returned.
	 */
	public final static boolean isTracingEnabled() {

		if (TracingUIActivator.DEBUG_PREFERENCES) {
			TRACE.traceEntry(TracingConstants.TRACE_PREFERENCES_STRING);
		}
		final IScopeContext[] lookupOrder = new IScopeContext[] {new InstanceScope()};
		IPreferencesService prefService = Platform.getPreferencesService();
		prefService.setDefaultLookupOrder(TracingConstants.BUNDLE_ID, null, new String[] {InstanceScope.SCOPE});
		boolean tracingEnabled = prefService.getBoolean(TracingConstants.BUNDLE_ID, TracingConstants.PREFERENCE_ENABLEMENT_IDENTIFIER, false, lookupOrder);
		if (TracingUIActivator.DEBUG_PREFERENCES) {
			TRACE.traceExit(TracingConstants.TRACE_PREFERENCES_STRING, Boolean.valueOf(tracingEnabled));
		}
		return tracingEnabled;
	}

	/**
	 * Accessor for the maximum file count in the preference store
	 * 
	 * @return The maximum file count in the preference store or the value 10 if it's not defined.
	 */
	public final static int getMaxFileCount() {

		if (TracingUIActivator.DEBUG_PREFERENCES) {
			TRACE.traceEntry(TracingConstants.TRACE_PREFERENCES_STRING);
		}
		final IScopeContext[] lookupOrder = new IScopeContext[] {new InstanceScope()};
		IPreferencesService prefService = Platform.getPreferencesService();
		prefService.setDefaultLookupOrder(TracingConstants.BUNDLE_ID, null, new String[] {InstanceScope.SCOPE});
		int maxCount = prefService.getInt(TracingConstants.BUNDLE_ID, TracingConstants.PREFERENCE_MAX_FILE_COUNT_IDENTIFIER, 10, lookupOrder);
		if (TracingUIActivator.DEBUG_PREFERENCES) {
			TRACE.traceExit(TracingConstants.TRACE_PREFERENCES_STRING, Integer.valueOf(maxCount));
		}
		return maxCount;
	}

	/**
	 * Accessor for the maximum file size in the preference store
	 * 
	 * @return The maximum file size in the preference store or the value 1000 if it's not defined.
	 */
	public final static int getMaxFileSize() {

		if (TracingUIActivator.DEBUG_PREFERENCES) {
			TRACE.traceEntry(TracingConstants.TRACE_PREFERENCES_STRING);
		}
		final IScopeContext[] lookupOrder = new IScopeContext[] {new InstanceScope()};
		IPreferencesService prefService = Platform.getPreferencesService();
		prefService.setDefaultLookupOrder(TracingConstants.BUNDLE_ID, null, new String[] {InstanceScope.SCOPE});
		int maxSize = prefService.getInt(TracingConstants.BUNDLE_ID, TracingConstants.PREFERENCE_MAX_FILE_SIZE_IDENTIFIER, 1000, lookupOrder);
		if (TracingUIActivator.DEBUG_PREFERENCES) {
			TRACE.traceExit(TracingConstants.TRACE_PREFERENCES_STRING, Integer.valueOf(maxSize));
		}
		return maxSize;
	}

	/**
	 * Accessor for the file path in the preference store
	 * 
	 * @return The file path in the preference store or the default value if it's not defined.
	 */
	public final static String getFilePath() {

		if (TracingUIActivator.DEBUG_PREFERENCES) {
			TRACE.traceEntry(TracingConstants.TRACE_PREFERENCES_STRING);
		}
		final IScopeContext[] lookupOrder = new IScopeContext[] {new InstanceScope()};
		IPreferencesService prefService = Platform.getPreferencesService();
		prefService.setDefaultLookupOrder(TracingConstants.BUNDLE_ID, null, new String[] {InstanceScope.SCOPE});
		String filePath = prefService.getString(TracingConstants.BUNDLE_ID, TracingConstants.PREFERENCE_FILE_PATH, DebugOptionsHandler.getDebugOptions().getFile().getAbsolutePath(), lookupOrder);
		if (TracingUIActivator.DEBUG_PREFERENCES) {
			TRACE.traceExit(TracingConstants.TRACE_PREFERENCES_STRING, Integer.valueOf(filePath));
		}
		return filePath;
	}

	/** Trace object for this bundle */
	protected final static DebugTrace TRACE = TracingUIActivator.getDefault().getTrace();
}