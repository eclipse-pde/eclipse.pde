/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

import java.util.*;
import org.eclipse.core.runtime.*;

/**
 * Utility class used to help with NLS'ing messages, creating progress monitors, etc.
 */
public class Policy {

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

	/**
	 * Print a debug message to the console. If the given boolean is
	 * <code>true</code> then pre-pend the message with the current date.
	 */
	public static void debug(boolean includeDate, String message) {
		if (includeDate)
			message = new Date(System.currentTimeMillis()).toString() + " - " + message; //$NON-NLS-1$
		System.out.println(message);
	}
}
