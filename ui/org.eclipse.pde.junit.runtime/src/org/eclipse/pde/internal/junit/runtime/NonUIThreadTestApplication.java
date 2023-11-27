/*******************************************************************************
 * Copyright (c) 2009, 2023 EclipseSource Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.junit.runtime;

import java.util.Objects;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.ui.testing.TestableObject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * A Workbench that runs a test suite specified in the
 * command line arguments.
 */
public class NonUIThreadTestApplication implements IApplication {

	private static final String DEFAULT_HEADLESSAPP = "org.eclipse.pde.junit.runtime.coretestapplication"; //$NON-NLS-1$

	protected IApplication fApplication;
	protected Object fTestHarness;
	protected boolean runInUIThreadAndRequirePlatformUI = false;
	protected String defaultApplicationId = DEFAULT_HEADLESSAPP;

	@Override
	public Object start(IApplicationContext context) throws Exception {
		String[] args = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);

		String appId = getApplicationToRun(args);
		IApplication app = Objects.requireNonNull(getApplication(appId));

		if (runInUIThreadAndRequirePlatformUI || !DEFAULT_HEADLESSAPP.equals(appId)) {
			// this means we are running a different application, which potentially can be UI application;
			// non-ui thread test app can also mean we are running UI tests but outside the UI thread;
			// this is a pattern used by SWT bot and worked before; we continue to support this
			// (see bug 340906 for details)
			installPlatformUITestHarness();
		}
		fApplication = app;
		return fApplication.start(context);
	}

	/**
	 * the non-UI thread test application also supports launching headless applications;
	 * this may mean that no UI bundle will be available; thus, in order to not
	 * introduce any dependency on UI code we first attempt to get the harness via service.
	 * If that doesn't work, we use reflection to call the workbench code, but don't fail
	 * if Platform UI is not available.
	 */
	private void installPlatformUITestHarness() throws ReflectiveOperationException {
		Object testableObject = getRegisteredTestableObject();
		if (testableObject == null) {
			try { // If the service doesn't return a testable object ask PlatformUI directly
				Class<?> platformUIClass = Class.forName("org.eclipse.ui.PlatformUI", true, getClass().getClassLoader()); //$NON-NLS-1$
				testableObject = platformUIClass.getMethod("getTestableObject").invoke(null); //$NON-NLS-1$
			} catch (ClassNotFoundException e) { // PlatformUI is not available
				if (runInUIThreadAndRequirePlatformUI) {
					throw e;
				}
			}
		}
		if (testableObject != null) {
			fTestHarness = new PlatformUITestHarness(testableObject, !runInUIThreadAndRequirePlatformUI);
		}
	}

	/**
	 * Returns a {@link TestableObject} provided by a TestableObject service or {@code null} if no implementation can be found.
	 * The TestableObject is used to hook tests into the application lifecycle.
	 * <p>
	 * It is recommended the testable object is obtained via service instead Workbench#getWorkbenchTestable() to avoid
	 * the tests having a dependency on the Workbench.
	 * </p>
	 * @return TestableObject provided via service or {@code null}
	 */
	private static Object getRegisteredTestableObject() {
		BundleContext context = FrameworkUtil.getBundle(NonUIThreadTestApplication.class).getBundleContext();
		ServiceReference<?> reference = context.getServiceReference("org.eclipse.ui.testing.TestableObject"); //$NON-NLS-1$
		if (reference != null) {
			try {
				return context.getService(reference);
			} finally {
				context.ungetService(reference);
			}
		}
		return null;
	}

	@Override
	public void stop() {
		if (fApplication != null) {
			fApplication.stop();
		}
		if (fTestHarness != null) {
			fTestHarness = null;
		}
	}

	/*
	 * return the application to run, or null if not even the default application
	 * is found.
	 */
	private IApplication getApplication(String appId) throws CoreException {
		// Find the name of the application as specified by the PDE JUnit launcher.
		// If no application is specified, the 3.0 default workbench application
		// is returned.
		IExtension extension = Platform.getExtensionRegistry().getExtension(Platform.PI_RUNTIME, Platform.PT_APPLICATIONS, appId);
		Objects.requireNonNull(extension);

		// If the extension does not have the correct grammar, return null.
		// Otherwise, return the application object.
		IConfigurationElement[] elements = extension.getConfigurationElements();
		if (elements.length > 0) {
			IConfigurationElement[] runs = elements[0].getChildren("run"); //$NON-NLS-1$
			if (runs.length > 0) {
				Object runnable = runs[0].createExecutableExtension("class"); //$NON-NLS-1$
				if (runnable instanceof IApplication) {
					return (IApplication) runnable;
				}
			}
		}
		return null;
	}

	/*
	 * The -testApplication argument specifies the application to be run.
	 * If the PDE JUnit launcher did not set this argument, then return
	 * the name of the default application.
	 * In 3.0, the default is the "org.eclipse.ui.ide.worbench" application.
	 *
	 * see bug 228044
	 */
	private String getApplicationToRun(String[] args) {
		String testApp = RemotePluginTestRunner.getArgumentValue(args, "-testApplication"); //$NON-NLS-1$
		if (testApp != null) {
			return testApp;
		}
		IProduct product = Platform.getProduct();
		return product != null ? product.getApplication() : defaultApplicationId;
	}

}
