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
		if (DebugOptionsHandler.debugTracker == null) {
			DebugOptionsHandler.debugTracker = new ServiceTracker(TracingUIActivator.getDefault().getBundle().getBundleContext(), DebugOptions.class.getName(), null);
			DebugOptionsHandler.debugTracker.open();
		}
		return (DebugOptions) DebugOptionsHandler.debugTracker.getService();
	}

	/**
	 * Accessor for determining if tracing for the product is enabled
	 * 
	 * @return Returns true of tracing is enabled for this product; Otherwise false.
	 */
	public static boolean isTracingEnabled() {
		return DebugOptionsHandler.getDebugOptions().isDebugEnabled();
	}

	/**
	 * Enable or Disable platform debugging
	 * 
	 * @param value
	 *            The value to enable or disable platform debugging. If it is set to true then platform debugging is
	 *            enabled.
	 */
	public static void setDebugEnabled(boolean value) {
		DebugOptionsHandler.getDebugOptions().setDebugEnabled(value);
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

	/**
	 * Flag is set in activator if debug options were set using debug mode and trace files rather than preferences
	 */
	private static boolean launchInDebugMode;
}