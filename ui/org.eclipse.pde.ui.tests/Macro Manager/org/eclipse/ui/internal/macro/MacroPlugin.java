/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.macro;

import java.lang.reflect.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.plugin.*;
import org.osgi.framework.*;

/**
 * The main plugin class to be used in the desktop.
 */
public class MacroPlugin extends AbstractUIPlugin {
	//The shared instance.
	private static MacroPlugin plugin;
	
	private MacroManager recorder;
	/**
	 * The constructor.
	 */
	public MacroPlugin() {
		super();
		plugin = this;
		recorder = new MacroManager();
	}
	public MacroManager getMacroManager() {
		return recorder;
	}

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception {
		recorder.shutdown();
		super.stop(context);
	}

	/**
	 * Returns the shared instance.
	 */
	public static MacroPlugin getDefault() {
		return plugin;
	}

	public static void logException(Throwable e) {
		logException(e, null, null);
	}
	public static void logException(
			Throwable e,
			final String title,
			String message) {
			if (e instanceof InvocationTargetException) {
				e = ((InvocationTargetException) e).getTargetException();
			}
			IStatus status = null;
			if (e instanceof CoreException)
				status = ((CoreException) e).getStatus();
			else {
				if (message == null)
					message = e.getMessage();
				if (message == null)
					message = e.toString();
				status = new Status(IStatus.ERROR, "org.eclipse.pde.ui.tests", IStatus.OK, message, e);
			}
			ResourcesPlugin.getPlugin().getLog().log(status);
			Display display = Display.getCurrent();
			if (display==null)
				display = Display.getDefault();
			final IStatus fstatus = status;
			display.asyncExec(new Runnable() {
				public void run() {
					ErrorDialog.openError(null, title, null, fstatus);
				}
			});
		}
}
