/*******************************************************************************
 * Copyright (c) 2009, 2015 EclipseSource Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     EclipseSource Corporation - initial API and implementation
 *     IBM - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.launching;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationListener;
import org.eclipse.pde.internal.core.PDEPreferencesManager;
import org.eclipse.pde.internal.launching.launcher.LaunchConfigurationListener;
import org.eclipse.pde.internal.launching.launcher.LaunchListener;
import org.eclipse.pde.internal.launching.launcher.LauncherUtils;
import org.eclipse.pde.internal.launching.launcher.OSGiFrameworkManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

public class PDELaunchingPlugin extends Plugin implements IPDEConstants {

	// Shared instance
	private static PDELaunchingPlugin fInstance;

	// Launches listener
	private LaunchListener fLaunchListener;

	private BundleContext fBundleContext;

	/**
	 * Utility class to help setup the launch configuration listener without
	 * loading the debug plugin
	 */
	private DebugPluginUtil fDebugPluginUtil;

	/**
	 * The shared text file document provider.
	 *
	 * @since 3.2
	 */
	private OSGiFrameworkManager fOSGiFrameworkManager;

	private PDEPreferencesManager fPreferenceManager;

	public PDELaunchingPlugin() {
		fInstance = this;
	}

	public PDEPreferencesManager getPreferenceManager() {
		if (fPreferenceManager == null) {
			fPreferenceManager = new PDEPreferencesManager(PLUGIN_ID);
		}
		return fPreferenceManager;
	}


	public static PDELaunchingPlugin getDefault() {
		return fInstance;
	}

	public static String getPluginId() {
		return getDefault().getBundle().getSymbolicName();
	}

	public static void log(IStatus status) {
		ResourcesPlugin.getPlugin().getLog().log(status);
	}

	public static void log(Throwable e) {
		if (e instanceof InvocationTargetException) {
			e = ((InvocationTargetException) e).getTargetException();
		}
		IStatus status = null;
		if (e instanceof CoreException) {
			status = ((CoreException) e).getStatus();
		} else {
			status = Status.error(e.getMessage(), e);
		}
		log(status);
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		fBundleContext = context;
		setupLaunchConfigurationListener();
	}

	/**
	 * Add the launch configuration listener if the debug plugin is started.
	 * Otherwise, setup a bundle listener to install the listener when the debug
	 * plugin loads.
	 *
	 * @param context
	 *            bundle context needed to get current bundles
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
				@Override
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

	@Override
	public void stop(BundleContext context) throws Exception {
		if (fLaunchListener != null) {
			fLaunchListener.shutdown();
		}
		if (fDebugPluginUtil != null) {
			fDebugPluginUtil.removeListener();
		}
		LauncherUtils.shutdown();
		super.stop(context);
	}

	public LaunchListener getLaunchListener() {
		if (fLaunchListener == null) {
			fLaunchListener = new LaunchListener();
		}
		return fLaunchListener;
	}

	public OSGiFrameworkManager getOSGiFrameworkManager() {
		if (fOSGiFrameworkManager == null) {
			fOSGiFrameworkManager = new OSGiFrameworkManager();
		}
		return fOSGiFrameworkManager;
	}

	/**
	 * Utility class that creates and controls a the PDE launch configuration
	 * listener. This is done in a separate class to avoid loading the debug
	 * plugin.
	 *
	 * @since 3.4
	 */
	private static class DebugPluginUtil {
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
