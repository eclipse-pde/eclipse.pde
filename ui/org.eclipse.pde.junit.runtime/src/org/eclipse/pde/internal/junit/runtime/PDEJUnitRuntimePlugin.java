/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.junit.runtime;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.testing.TestableObject;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The plug-in activator for the PDE JUnit Runtime plug-in
 * @since 4.3
 */
public class PDEJUnitRuntimePlugin extends AbstractUIPlugin {

	/**
	 *  Default instance of the receiver
	 */
	private static PDEJUnitRuntimePlugin inst;

	/**
	 *  The context within which this plugin was started.
	 */
	private BundleContext bundleContext;

	private ServiceTracker testableTracker = null;

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

	/* (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		bundleContext = context;
	}

	public void stop(BundleContext context) throws Exception {
		if (testableTracker != null) {
			testableTracker.close();
			testableTracker = null;
		}
		super.stop(context);
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
	 * @see PlatformUI#getTestableObject()
	 * @return TestableObject provided via service or <code>null</code>
	 */
	public TestableObject getTestableObject() {
		if (bundleContext == null)
			return null;
		if (testableTracker == null) {
			testableTracker = new ServiceTracker(bundleContext, TestableObject.class.getName(), null);
			testableTracker.open();
		}
		return (TestableObject) testableTracker.getService();
	}

}
