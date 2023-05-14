/*******************************************************************************
 *  Copyright (c) 2000, 2021 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alexander Fedorov <alexander.fedorov@arsysop.ru> - Bug 489181
 *******************************************************************************/
package org.eclipse.pde.internal.runtime;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.service.resolver.PlatformAdmin;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class PDERuntimePlugin extends AbstractUIPlugin {

	public static final String ID = "org.eclipse.pde.runtime"; //$NON-NLS-1$

	private static PDERuntimePlugin inst;
	private BundleContext fContext;
	private ServiceTracker<?, PlatformAdmin> platformAdminTracker;

	public PDERuntimePlugin() {
		inst = this;
	}
	private static boolean isBundleAvailable(String bundleID) {
		Bundle bundle = Platform.getBundle(bundleID);
		return bundle != null && (bundle.getState() & (Bundle.ACTIVE | Bundle.STARTING | Bundle.RESOLVED)) != 0;
	}

	public static final boolean HAS_IDE_BUNDLES;
	static {
		boolean result = false;
		try {
			result = isBundleAvailable("org.eclipse.core.resources") //$NON-NLS-1$
					&& isBundleAvailable("org.eclipse.pde.core") //$NON-NLS-1$
					&& isBundleAvailable("org.eclipse.jdt.core") //$NON-NLS-1$
					&& isBundleAvailable("org.eclipse.help") //$NON-NLS-1$
					&& isBundleAvailable("org.eclipse.pde.ui") //$NON-NLS-1$
					&& isBundleAvailable("org.eclipse.jdt.ui"); //$NON-NLS-1$
		} catch (Throwable exception) { // do nothing
		}
		HAS_IDE_BUNDLES = result;
	}

	public static Shell getActiveWorkbenchShell() {
		IWorkbenchWindow activeWindow = getActiveWorkbenchWindow();
		if (activeWindow != null) {
			return activeWindow.getShell();
		}
		return null;
	}

	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	}

	public PlatformAdmin getPlatformAdmin() {
		if (platformAdminTracker == null) {
			return null;
		}
		return platformAdminTracker.getService();
	}

	public static PDERuntimePlugin getDefault() {
		return inst;
	}

	public static String getPluginId() {
		return getDefault().getBundle().getSymbolicName();
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		this.fContext = context;

		platformAdminTracker = new ServiceTracker<>(context, PlatformAdmin.class, null);
		platformAdminTracker.open();
	}

	public BundleContext getBundleContext() {
		return this.fContext;
	}

	public State getState() {
		return getPlatformAdmin().getState(false);
	}

	public static void log(Throwable e) {
		if (e instanceof InvocationTargetException)
			e = ((InvocationTargetException) e).getTargetException();
		IStatus status = null;
		if (e instanceof CoreException) {
			status = ((CoreException) e).getStatus();
		} else if (e.getMessage() != null) {
			status = Status.error(e.getMessage(), e);
		}
		if (status != null)
			getDefault().getLog().log(status);
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		if (platformAdminTracker != null) {
			platformAdminTracker.close();
			platformAdminTracker = null;
		}
	}

}
