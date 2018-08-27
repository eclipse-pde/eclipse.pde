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
package org.eclipse.ui.trace.internal;

import java.io.File;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.runnable.StartupMonitor;
import org.eclipse.ui.trace.internal.utils.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * The activator class controls the plug-in life cycle
 */
//XXX: Does not extend AbstractUIPlugin to avoid activating the UI layer at load time (bug 431223).
public class TracingUIActivator extends Plugin {

	/**
	 * The constructor
	 */
	public TracingUIActivator() {

		// empty constructor
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static TracingUIActivator getDefault() {

		return TracingUIActivator.plugin;
	}

	@Override
	public void start(final BundleContext context) throws Exception {

		super.start(context);
		TracingUIActivator.plugin = this;

		if (DebugOptionsHandler.isTracingEnabled()) {
			// Tracing options have been enabled using options file and debug mode
			// Set option so we know debug mode is set, not preferences
			DebugOptionsHandler.setLaunchInDebugMode(true);

		} else {
			// bug 395632: see if the instance location is defined.  if not then defer accessing
			// the preferences until it is defined by being notified via the org.eclipse.osgi.service.runnable.StartupMonitor
			// service
			Location instanceLocation = Platform.getInstanceLocation();
			if (instanceLocation == null || !instanceLocation.isSet()) {
				// register a startup monitor to notify us when the application is running
				final TracingStartupMonitor startupMonitor = new TracingStartupMonitor();
				final Dictionary<String, ?> properties = new Hashtable<String, Object>(1);
				ServiceRegistration<StartupMonitor> registration = context.registerService(StartupMonitor.class, startupMonitor, properties);
				startupMonitor.setRegistration(registration);
			} else {
				this.initPreferences();
			}
		}
	}

	@Override
	public void stop(final BundleContext context) throws Exception {

		TracingUIActivator.plugin = null;
		super.stop(context);
	}

	/**
	 * Log the specified {@link Exception} to the workspace logging file.
	 *
	 * @param ex
	 *            The {@link Exception} to log
	 */
	public final void logException(final Exception ex) {

		if (ex != null) {
			final IStatus errorStatus = new Status(IStatus.ERROR, TracingConstants.BUNDLE_ID, ex.getMessage(), ex);
			this.getLog().log(errorStatus);
		}
	}

	/**
	 * Initialize the tracing preferences if tracing is enabled.
	 */
	protected final void initPreferences() {

		if (PreferenceHandler.isTracingEnabled()) {
			// User has previously enabled tracing options
			DebugOptionsHandler.setDebugEnabled(true);
			DebugOptionsHandler.getDebugOptions().setFile(new File(PreferenceHandler.getFilePath()));
			System.setProperty(TracingConstants.PROP_TRACE_SIZE_MAX, String.valueOf(PreferenceHandler.getMaxFileSize()));
			System.setProperty(TracingConstants.PROP_TRACE_FILE_MAX, String.valueOf(PreferenceHandler.getMaxFileCount()));

			Map<String, String> prefs = PreferenceHandler.getPreferenceProperties();
			DebugOptionsHandler.getDebugOptions().setOptions(prefs);
		}
	}

	/** The shared instance */
	private static TracingUIActivator plugin = null;

}