/**********************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build;

import java.text.MessageFormat;
import java.util.*;
import org.eclipse.core.runtime.*;

/**
 * Utility class used to help with NLS'ing messages, creating progress monitors, etc.
 */
public class Policy {
	private static final String bundleName = "org.eclipse.pde.internal.build.messages"; //$NON-NLS-1$
	private static ResourceBundle bundle = ResourceBundle.getBundle(bundleName, Locale.getDefault());

	/**
	 * Lookup the message with the given ID in this catalog 
	 */
	public static String bind(String id) {
		return bind(id, (String[]) null);
	}
	/**
	 * Lookup the message with the given ID in this catalog and bind its
	 * substitution locations with the given string.
	 */
	public static String bind(String id, String binding) {
		return bind(id, new String[] { binding });
	}
	/**
	 * Lookup the message with the given ID in this catalog and bind its
	 * substitution locations with the given strings.
	 */
	public static String bind(String id, String binding1, String binding2) {
		return bind(id, new String[] { binding1, binding2 });
	}

	/**
	 * Lookup the message with the given ID in this catalog and bind its
	 * substitution locations with the given string values.
	 */
	public static String bind(String id, String[] bindings) {
		if (id == null)
			return "No message available"; //$NON-NLS-1$
		String message = null;
		try {
			message = bundle.getString(id);
		} catch (MissingResourceException e) {
			// If we got an exception looking for the message, fail gracefully by just returning
			// the id we were looking for.  In most cases this is semi-informative so is not too bad.
			return "Missing message: " + id + "in: " + bundleName; //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (bindings == null)
			return message;
		return MessageFormat.format(message, bindings);
	}

	/**
	 * Return a progress monitor for the given monitor. Ensures that the resulting
	 * monitor is not <code>null</code>.
	 * 
	 * @param monitor the monitor to wrap, or <code>null</code>
	 * @return IProgressMonitor
	 */
	public static IProgressMonitor monitorFor(IProgressMonitor monitor) {
		if (monitor == null)
			return new NullProgressMonitor();
		return monitor;
	}

	/**
	 * Create a sub progress monitor with the given units of work, for the given monitor.
	 * 
	 * @param monitor the parent monitor, or <code>null</code>
	 * @param ticks the number of units of work
	 * @return IProgressMonitor
	 */
	public static IProgressMonitor subMonitorFor(IProgressMonitor monitor, int ticks) {
		if (monitor == null)
			return new NullProgressMonitor();
		if (monitor instanceof NullProgressMonitor)
			return monitor;
		return new SubProgressMonitor(monitor, ticks);
	}

	/**
	 * Create a sub progress monitor with the given number of units of work and in the 
	 * given style, for the specified parent monitor.
	 * 
	 * @param monitor the parent monitor, or <code>null</code>
	 * @param ticks the number of units of work
	 * @param style the style of the sub progress monitor
	 * @return IProgressMonitor
	 */
	public static IProgressMonitor subMonitorFor(IProgressMonitor monitor, int ticks, int style) {
		if (monitor == null)
			return new NullProgressMonitor();
		if (monitor instanceof NullProgressMonitor)
			return monitor;
		return new SubProgressMonitor(monitor, ticks, style);
	}
}