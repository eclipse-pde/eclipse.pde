/*******************************************************************************
 * Copyright (c) 2008, 2017  IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Chris Aniszczyk <zx@code9.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ua.ui;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class PDEUserAssistanceUIPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.pde.ua.ui";

	// The shared instance
	private static PDEUserAssistanceUIPlugin plugin;

	private PDEUserAssistanceLabelProvider fLabelProvider;

	/**
	 * The constructor
	 */
	public PDEUserAssistanceUIPlugin() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static PDEUserAssistanceUIPlugin getDefault() {
		return plugin;
	}

	public static Shell getActiveWorkbenchShell() {
		IWorkbenchWindow window = getActiveWorkbenchWindow();
		if (window != null) {
			return window.getShell();
		}
		return null;
	}

	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		return getDefault().getWorkbench().getActiveWorkbenchWindow();
	}

	public static void logException(Throwable e, final String title,
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
			status = new Status(IStatus.ERROR, PLUGIN_ID, IStatus.OK,
					message, e);
		}
		ResourcesPlugin.getPlugin().getLog().log(status);
		Display display = Display.getCurrent() != null ? Display.getCurrent()
				: Display.getDefault();
		final IStatus fstatus = status;
		display.asyncExec(() -> ErrorDialog.openError(null, title, null, fstatus));
	}

	public static void logException(Throwable e) {
		logException(e, null, null);
	}

	public static IWorkbenchPage getActivePage() {
		return getActiveWorkbenchWindow().getActivePage();
	}

	public PDEUserAssistanceLabelProvider getLabelProvider() {
		if (fLabelProvider == null)
			fLabelProvider = new PDEUserAssistanceLabelProvider();
		return fLabelProvider;
	}
}
