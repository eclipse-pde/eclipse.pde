/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationListener;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.pde.internal.ui.launcher.*;
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
import org.osgi.framework.*;

public class PDEPlugin extends AbstractUIPlugin implements IPDEUIConstants {

	// Shared instance
	private static PDEPlugin fInstance;

	// Launches listener
	private LaunchListener fLaunchListener;

	private BundleContext fBundleContext;

	private Hashtable fCounters;

	// Provides Launch Configurations log files to Log View
	private ILogFileProvider fLogFileProvider;

	// Shared colors for all forms
	private FormColors fFormColors;
	private PDELabelProvider fLabelProvider;

	/**
	 * Utility class to help setup the launch configuration listener
	 * without loading the debug plugin
	 */
	private DebugPluginUtil fDebugPluginUtil;

	/**
	 * The shared text file document provider.
	 * @since 3.2
	 */
	private IDocumentProvider fTextFileDocumentProvider;

	private OSGiFrameworkManager fOSGiFrameworkManager;

	public PDEPlugin() {
		fInstance = this;
	}

	public URL getInstallURL() {
		return getDefault().getBundle().getEntry("/"); //$NON-NLS-1$
	}

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

	private IWorkbenchPage internalGetActivePage() {
		return getWorkbench().getActiveWorkbenchWindow().getActivePage();
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
		fBundleContext = context;
		setupLaunchConfigurationListener();
		fLogFileProvider = new PDELogFileProvider();
		LogFilesManager.addLogFileProvider(fLogFileProvider);
	}

	/**
	 * Add the launch configuration listener if the debug plugin
	 * is started.  Otherwise, setup a bundle listener to install
	 * the listener when the debug plugin loads.
	 * @param context bundle context needed to get current bundles
	 */
	private void setupLaunchConfigurationListener() {
		boolean listenerStarted = false;
		Bundle bundle = Platform.getBundle("org.eclipse.debug.core"); //$NON-NLS-1$
		if (bundle != null && bundle.getState() == Bundle.ACTIVE) {
			fDebugPluginUtil = new DebugPluginUtil();
			fDebugPluginUtil.addListener();
			listenerStarted = true;
		}
		if (!listenerStarted) {
			fBundleContext.addBundleListener(new BundleListener() {
				public void bundleChanged(BundleEvent event) {
					if (event.getType() == BundleEvent.STARTED && "org.eclipse.debug.core".equals(event.getBundle().getSymbolicName())) { //$NON-NLS-1$
						fDebugPluginUtil = new DebugPluginUtil();
						fDebugPluginUtil.addListener();
						fBundleContext.removeBundleListener(this);
					}
				}
			});
		}
	}

	public BundleContext getBundleContext() {
		return fBundleContext;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		if (fLaunchListener != null)
			fLaunchListener.shutdown();
		if (fFormColors != null) {
			fFormColors.dispose();
			fFormColors = null;
		}
		if (fLabelProvider != null) {
			fLabelProvider.dispose();
			fLabelProvider = null;
		}
		if (fDebugPluginUtil != null) {
			fDebugPluginUtil.removeListener();
		}
		if (fLogFileProvider != null) {
			LogFilesManager.removeLogFileProvider(fLogFileProvider);
			fLogFileProvider = null;
		}
		LauncherUtils.shutdown();
		super.stop(context);
	}

	public PDELabelProvider getLabelProvider() {
		if (fLabelProvider == null)
			fLabelProvider = new PDELabelProvider();
		return fLabelProvider;
	}

	public LaunchListener getLaunchListener() {
		if (fLaunchListener == null)
			fLaunchListener = new LaunchListener();
		return fLaunchListener;
	}

	public OSGiFrameworkManager getOSGiFrameworkManager() {
		if (fOSGiFrameworkManager == null)
			fOSGiFrameworkManager = new OSGiFrameworkManager();
		return fOSGiFrameworkManager;
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
	 * Utility class that creates and controls a the PDE launch configuration listener.
	 * This is done in a separate class to avoid loading the debug plugin.
	 * @since 3.4
	 */
	private class DebugPluginUtil {
		private ILaunchConfigurationListener fLaunchConfigurationListener;

		public void addListener() {
			if (fLaunchConfigurationListener == null) {
				fLaunchConfigurationListener = new LaunchConfigurationListener();
			}
			DebugPlugin.getDefault().getLaunchManager().addLaunchConfigurationListener(fLaunchConfigurationListener);
		}

		public void removeListener() {
			if (fLaunchConfigurationListener != null) {
				DebugPlugin.getDefault().getLaunchManager().removeLaunchConfigurationListener(fLaunchConfigurationListener);
				fLaunchConfigurationListener = null;
			}
		}
	}
}
