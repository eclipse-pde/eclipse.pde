/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.unittest.junit;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * The plug-in runtime class for the JUnit Plugin Unit Test plug-in.
 */
@SuppressWarnings("deprecation")
public class JUnitPluginTestPlugin extends AbstractUIPlugin {

	/**
	 * The single instance of this plug-in runtime class.
	 */
	private static JUnitPluginTestPlugin fgPlugin = null;

	public static final String PLUGIN_ID = "org.eclipse.pde.unittest.junit"; //$NON-NLS-1$

	public static final String UNIT_TEST_VIEW_SUPPORT_ID = "org.eclipse.pde.unittest.junit"; //$NON-NLS-1$

	private BundleContext fBundleContext;

	public JUnitPluginTestPlugin() {
		fgPlugin = this;
	}

	public static JUnitPluginTestPlugin getDefault() {
		return fgPlugin;
	}

	public static String getPluginId() {
		return PLUGIN_ID;
	}

	public static void log(Throwable e) {
		log(Status.error("Error", e)); //$NON-NLS-1$
	}

	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		fBundleContext = context;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		fBundleContext = null;
	}

	/**
	 * Returns a service with the specified name or <code>null</code> if none.
	 *
	 * @param serviceName name of service
	 * @return service object or <code>null</code> if none
	 */
	public Object getService(String serviceName) {
		ServiceReference<?> reference = fBundleContext.getServiceReference(serviceName);
		if (reference == null)
			return null;
		return fBundleContext.getService(reference);
	}

	/**
	 * Returns the bundle for a given bundle name, regardless whether the bundle is
	 * resolved or not.
	 *
	 * @param bundleName the bundle name
	 * @return the bundle
	 */
	public Bundle getBundle(String bundleName) {
		Bundle[] bundles = getBundles(bundleName, null);
		if (bundles != null && bundles.length > 0)
			return bundles[0];
		return null;
	}

	/**
	 * Returns the bundles for a given bundle name,
	 *
	 * @param bundleName the bundle name
	 * @param version    the version of the bundle
	 * @return the bundles of the given name
	 */
	public Bundle[] getBundles(String bundleName, String version) {
		Bundle[] bundles = Platform.getBundles(bundleName, version);
		if (bundles != null)
			return bundles;

		// Accessing unresolved bundle
		ServiceReference<PackageAdmin> serviceRef = fBundleContext.getServiceReference(PackageAdmin.class);
		PackageAdmin admin = fBundleContext.getService(serviceRef);
		bundles = admin.getBundles(bundleName, version);
		if (bundles != null && bundles.length > 0)
			return bundles;
		return null;
	}

	/**
	 * Returns this workbench window's shell.
	 *
	 * @return the shell containing this window's controls or <code>null</code> if
	 *         the shell has not been created yet or if the window has been closed
	 */
	public static Shell getActiveWorkbenchShell() {
		IWorkbenchWindow workBenchWindow = getActiveWorkbenchWindow();
		if (workBenchWindow == null)
			return null;
		return workBenchWindow.getShell();
	}

	/**
	 * Returns the active workbench window
	 *
	 * @return the active workbench window, or <code>null</code> if there is no
	 *         active workbench window or if called from a non-UI thread
	 */
	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		if (fgPlugin == null)
			return null;
		IWorkbench workBench = PlatformUI.getWorkbench();
		if (workBench == null)
			return null;
		return workBench.getActiveWorkbenchWindow();
	}

	/**
	 * Returns the currently active page for this workbench window.
	 *
	 * @return the active page, or <code>null</code> if none
	 */
	public static IWorkbenchPage getActivePage() {
		IWorkbenchWindow activeWorkbenchWindow = getActiveWorkbenchWindow();
		if (activeWorkbenchWindow == null)
			return null;
		return activeWorkbenchWindow.getActivePage();
	}

	/**
	 * Activates UnitTestBundle. Eclipse uses lazy bundle loading by default, which
	 * means a bundle will not be loaded in many cases until some of its class is
	 * used. This method allows the clients to instantiate the Unit Test bundle in
	 * order to make it setup its launch listeners that are used to create and
	 * activate Unit Test View. The Unit Test client bundles must call this method
	 * before a Unit Test launch is created (preferably right before creation of the
	 * launch in order to not make Eclipse to load the Unit Test bundle when it is
	 * not really required), To load the Unit Test bundle the clients, for example,
	 * might call this method inside their
	 * 'ILaunchConfigurationDelegate2.getLaunch(ILaunchConfiguration, String)'
	 * method of their launch configuration implementation.
	 */
	public static void activateUnitTestCoreBundle() {
		Assert.isNotNull(Platform.getBundle("org.eclipse.unittest.ui")); //$NON-NLS-1$
	}
}