/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.junit.runtime;

import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.testing.TestableObject;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The plug-in activator for the PDE JUnit Runtime plug-in.
 * <p>
 * The PDE JUnit runtime allows JUnit tests to be run with an OSGi runtime.
 * It supports the following use cases:
 * <pre>
 * 1) Headless tests (no UI, no workbench)
 * 	  Runs NonUIThreadTestApplication with no testable object
 *
 * 2) e4 UI tests (e4 UI, no workbench)
 *    Runs NonUIThreadTestApplication with a testable object from e4 service
 *
 * 3) UI tests run in the non UI thread (UI, workbench)
 *    Runs NonUIThreadTestApplication with a testable object from e4 service or PlatformUI
 *
 * 4) UI tests run in the UI thread (UI, workbench)
 *    Runs UITestApplication with a testable object from e4 service or PlatformUI
 *
 * 5) Headless tests with no application (no UI, no workbench, no application)
 *    Runs directly with no application
 *
 * 6) Legacy UI test application (deprecated)
 *    Runs LegacyUITestApplication with an IPlatformRunnable
 * </pre>
 * @since 4.3
 */
public class PDEJUnitRuntimePlugin implements BundleActivator {

	/**
	 * The testable object is accessed via service and a string name to avoid depending on UI code.  The
	 */
	private static final String TESTABLE_OBJECT_SERVICE_NAME = "org.eclipse.ui.testing.TestableObject"; //$NON-NLS-1$

	/**
	 *  Default instance of the receiver
	 */
	private static PDEJUnitRuntimePlugin inst;

	/**
	 *  The context within which this plugin was started.
	 */
	private BundleContext bundleContext;

	private ServiceTracker<Object, Object> testableTracker = null;

	public PDEJUnitRuntimePlugin() {
		super();
		inst = this;
	}

	/**
	 * Return the default instance of this plug-in activator. This represents the runtime plug-in.
	 * @return PdeJUnitRuntimePlugin the runtime plug-in or <code>null</code> if the plug-in isn't started
	 * @see AbstractUIPlugin for the typical implementation pattern for plug-in classes.
	 */
	public static PDEJUnitRuntimePlugin getDefault() {
		return inst;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		bundleContext = context;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (testableTracker != null) {
			testableTracker.close();
			testableTracker = null;
		}
	}

	/**
	 * Returns a {@link TestableObject} provided by a TestableObject
	 * service or <code>null</code> if a service implementation cannot
	 * be found.  The TestableObject is used to hook tests into the
	 * application lifecycle.
	 * <p>
	 * It is recommended the testable object is obtained via service
	 * over {@link Workbench#getWorkbenchTestable()} to avoid the
	 * tests having a dependency on the Workbench.
	 * </p>
	 * @return TestableObject provided via service or <code>null</code>
	 */
	public Object getTestableObject() {
		if (bundleContext == null)
			return null;
		if (testableTracker == null) {
			testableTracker = new ServiceTracker<Object, Object>(bundleContext, TESTABLE_OBJECT_SERVICE_NAME, null);
			testableTracker.open();
		}
		return testableTracker.getService();
	}

}
