/*******************************************************************************
 *  Copyright (c) 2006, 2015 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.junit.runtime;

import org.eclipse.core.runtime.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.testing.ITestHarness;
import org.eclipse.ui.testing.TestableObject;
import org.junit.Assert;

/**
 * A Workbench that runs a test suite specified in the
 * command line arguments.
 */
public class LegacyUITestApplication implements IPlatformRunnable, ITestHarness {

	private static final String DEFAULT_APP_3_0 = "org.eclipse.ui.ide.workbench"; //$NON-NLS-1$

	private TestableObject fTestableObject;

	@Override
	public Object run(final Object args) throws Exception {
		IPlatformRunnable application = getApplication((String[]) args);

		Assert.assertNotNull(application);

		fTestableObject = PlatformUI.getTestableObject();
		fTestableObject.setTestHarness(this);
		return application.run(args);
	}

	/*
	 * return the application to run, or null if not even the default application
	 * is found.
	 */
	private IPlatformRunnable getApplication(String[] args) throws CoreException {
		// Find the name of the application as specified by the PDE JUnit launcher.
		// If no application is specified, the 3.0 default workbench application
		// is returned.
		IExtension extension = Platform.getExtensionRegistry().getExtension(Platform.PI_RUNTIME, Platform.PT_APPLICATIONS, getApplicationToRun(args));

		Assert.assertNotNull(extension);

		// If the extension does not have the correct grammar, return null.
		// Otherwise, return the application object.
		IConfigurationElement[] elements = extension.getConfigurationElements();
		if (elements.length > 0) {
			IConfigurationElement[] runs = elements[0].getChildren("run"); //$NON-NLS-1$
			if (runs.length > 0) {
				Object runnable = runs[0].createExecutableExtension("class"); //$NON-NLS-1$
				if (runnable instanceof IPlatformRunnable)
					return (IPlatformRunnable) runnable;
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
	 */
	private String getApplicationToRun(String[] args) {
		IProduct product = Platform.getProduct();
		if (product != null)
			return product.getApplication();
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-testApplication") && i < args.length - 1) //$NON-NLS-1$
				return args[i + 1];
		}
		return DEFAULT_APP_3_0;
	}

	@Override
	public void runTests() {
		fTestableObject.testingStarting();
		fTestableObject.runTest(() -> RemotePluginTestRunner.main(Platform.getCommandLineArgs()));
		fTestableObject.testingFinished();
	}

}
