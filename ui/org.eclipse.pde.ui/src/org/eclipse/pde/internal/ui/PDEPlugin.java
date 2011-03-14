/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Hashtable;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.pde.internal.core.PDEPreferencesManager;
import org.eclipse.pde.internal.ui.launcher.PDELogFileProvider;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.internal.views.log.ILogFileProvider;
import org.eclipse.ui.internal.views.log.LogFilesManager;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.osgi.framework.BundleContext;

public class PDEPlugin extends AbstractUIPlugin implements IPDEUIConstants {

	// Shared instance
	private static PDEPlugin fInstance;

	private Hashtable fCounters;

	// Provides Launch Configurations log files to Log View
	private ILogFileProvider fLogFileProvider;

	// Shared colors for all forms
	private FormColors fFormColors;
	private PDELabelProvider fLabelProvider;

	/**
	 * The shared text file document provider.
	 * @since 3.2
	 */
	private IDocumentProvider fTextFileDocumentProvider;

	private PDEPreferencesManager fPreferenceManager;

	public PDEPlugin() {
		fInstance = this;
	}

	public PDEPreferencesManager getPreferenceManager() {
		if (fPreferenceManager == null) {
			fPreferenceManager = new PDEPreferencesManager(PLUGIN_ID);
		}
		return fPreferenceManager;
	}

	public URL getInstallURL() {
		return getDefault().getBundle().getEntry("/"); //$NON-NLS-1$
	}

	/**
	 * @return The active workbench page or <code>null</code> if the workbench is shutting down
	 */
	public static IWorkbenchPage getActivePage() {
		return getDefault().internalGetActivePage();
	}

	public static Shell getActiveWorkbenchShell() {
		IWorkbenchWindow window = getActiveWorkbenchWindow();
		if (window != null) {
			return window.getShell();
		}
		return null;
	}

	/**
	 * Returns the currently active window for the workbench (if any). Returns
	 * <code>null</code> if there is no active workbench window. Returns
	 * <code>null</code> if called from a non-UI thread.
	 * 
	 * @return the active workbench window, or <code>null</code> if there is
	 *         no active workbench window or if called from a non-UI thread
	 */
	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		return getDefault().getWorkbench().getActiveWorkbenchWindow();
	}

	public static PDEPlugin getDefault() {
		return fInstance;
	}

	public Hashtable getDefaultNameCounters() {
		if (fCounters == null)
			fCounters = new Hashtable();
		return fCounters;
	}

	public static String getPluginId() {
		return getDefault().getBundle().getSymbolicName();
	}

	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	/**
	 * @return The active workbench page or <code>null</code> if the workbench is shutting down
	 */
	private IWorkbenchPage internalGetActivePage() {
		IWorkbenchWindow workbenchWin = getWorkbench().getActiveWorkbenchWindow();
		return workbenchWin != null ? workbenchWin.getActivePage() : null;
	}

	public static void log(IStatus status) {
		ResourcesPlugin.getPlugin().getLog().log(status);
	}

	public static void logErrorMessage(String message) {
		log(new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, message, null));
	}

	public static void logException(Throwable e, final String title, String message) {
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
			status = new Status(IStatus.ERROR, getPluginId(), IStatus.OK, message, e);
		}
		ResourcesPlugin.getPlugin().getLog().log(status);
		Display display = SWTUtil.getStandardDisplay();
		final IStatus fstatus = status;
		display.asyncExec(new Runnable() {
			public void run() {
				ErrorDialog.openError(null, title, null, fstatus);
			}
		});
	}

	public static void logException(Throwable e) {
		logException(e, null, null);
	}

	public static void log(Throwable e) {
		if (e instanceof InvocationTargetException)
			e = ((InvocationTargetException) e).getTargetException();
		IStatus status = null;
		if (e instanceof CoreException)
			status = ((CoreException) e).getStatus();
		else
			status = new Status(IStatus.ERROR, getPluginId(), IStatus.OK, e.getMessage(), e);
		log(status);
	}

	public FormColors getFormColors(Display display) {
		if (fFormColors == null) {
			fFormColors = new FormColors(display);
			fFormColors.markShared();
		}
		return fFormColors;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		fLogFileProvider = new PDELogFileProvider();
		LogFilesManager.addLogFileProvider(fLogFileProvider);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		if (fFormColors != null) {
			fFormColors.dispose();
			fFormColors = null;
		}
		if (fLabelProvider != null) {
			fLabelProvider.dispose();
			fLabelProvider = null;
		}
		if (fLogFileProvider != null) {
			LogFilesManager.removeLogFileProvider(fLogFileProvider);
			fLogFileProvider = null;
		}
		Utilities.shutdown();
		super.stop(context);
	}

	public PDELabelProvider getLabelProvider() {
		if (fLabelProvider == null)
			fLabelProvider = new PDELabelProvider();
		return fLabelProvider;
	}

	public static boolean isFullNameModeEnabled() {
		IPreferenceStore store = getDefault().getPreferenceStore();
		return store.getString(IPreferenceConstants.PROP_SHOW_OBJECTS).equals(IPreferenceConstants.VALUE_USE_NAMES);
	}

	/**
	 * Returns the shared text file document provider for this plug-in.
	 * 
	 * @return the shared text file document provider
	 * @since 3.2
	 */
	public synchronized IDocumentProvider getTextFileDocumentProvider() {
		if (fTextFileDocumentProvider == null)
			fTextFileDocumentProvider = new TextFileDocumentProvider();
		return fTextFileDocumentProvider;
	}

	/**
	 * Returns a section in the PDE UI plug-in's dialog settings. If the section doesn't exist yet, it is created.
	 *
	 * @param name the name of the section
	 * @return the section of the given name
	 * @since 3.6.100
	 */
	public IDialogSettings getDialogSettingsSection(String name) {
		IDialogSettings dialogSettings = getDialogSettings();
		IDialogSettings section = dialogSettings.getSection(name);
		if (section == null) {
			section = dialogSettings.addNewSection(name);
		}
		return section;
	}
}
