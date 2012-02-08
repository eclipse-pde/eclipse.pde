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

import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugTrace;
import org.eclipse.ui.trace.internal.TracingUIActivator;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Utility API for handling {@link DebugOptions} operations
 */
public class DebugOptionsHandler {

	/**
	 * Accessor for the product's DebugOptions
	 * 
	 * @return The DebugOptions object for the product
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static DebugOptions getDebugOptions() {

		if (TracingUIActivator.DEBUG) {
			TRACE.traceEntry(null);
		}
		if (DebugOptionsHandler.debugTracker == null) {
			DebugOptionsHandler.debugTracker = new ServiceTracker(TracingUIActivator.getDefault().getBundle().getBundleContext(), DebugOptions.class.getName(), null);
			DebugOptionsHandler.debugTracker.open();
		}
		final DebugOptions debugOptions = (DebugOptions) DebugOptionsHandler.debugTracker.getService();
		if (TracingUIActivator.DEBUG) {
			TRACE.traceExit(null, debugOptions);
		}
		return debugOptions;
	}

	/**
	 * Accessor for determining if tracing for the product is enabled
	 * 
	 * @return Returns true of tracing is enabled for this product; Otherwise false.
	 */
	public static boolean isTracingEnabled() {

		if (TracingUIActivator.DEBUG) {
			TRACE.traceEntry(null);
		}
		boolean result = DebugOptionsHandler.getDebugOptions().isDebugEnabled();
		if (TracingUIActivator.DEBUG) {
			TRACE.traceExit(null, Boolean.valueOf(result));
		}
		return result;
	}

	/**
	 * Enable or Disable platform debugging
	 * 
	 * @param value
	 *            The value to enable or disable platform debugging. If it is set to true then platform debugging is
	 *            enabled.
	 */
	public static void setDebugEnabled(boolean value) {

		if (TracingUIActivator.DEBUG) {
			TRACE.traceEntry(null);
		}
		DebugOptionsHandler.getDebugOptions().setDebugEnabled(value);
		if (TracingUIActivator.DEBUG) {
			TRACE.traceExit(null);
		}
	}

	/**
	 * @return whether the current debug options were set from trace files rather than preferences
	 */
	public static boolean isLaunchInDebugMode() {
		return launchInDebugMode;
	}

	/**
	 * Sets the flag indicated the current debug options were set from trace files rather than preferences.
	 * This method should be called when the trace plug-in is activated.
	 * 
	 * @param mode whether current options were set from trace files
	 */
	public static void setLaunchInDebugMode(boolean mode) {
		launchInDebugMode = mode;
	}

	/** The debug service for this product */
	@SuppressWarnings("rawtypes")
	private static ServiceTracker debugTracker = null;

	/** Trace object for this bundle */
	protected static DebugTrace TRACE = TracingUIActivator.getDefault().getTrace();

	/**
	 * Flag is set in activator if debug options were set using debug mode and trace files rather than preferences
	 */
	private static boolean launchInDebugMode;
}