/*******************************************************************************
 * Copyright (c) 2003, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Carsten Reckord <eclipse@reckord.de> - bug 288343
 *******************************************************************************/
package org.eclipse.pde.internal.junit.runtime;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.testing.ITestHarness;
import org.eclipse.ui.testing.TestableObject;

/**
 * A Workbench that runs a test suite specified in the
 * command line arguments.
 */
public class UITestApplication extends NonUIThreadTestApplication implements ITestHarness {

	private static final String DEFAULT_APP_3_0 = "org.eclipse.ui.ide.workbench"; //$NON-NLS-1$

	protected TestableObject fTestableObject;

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.junit.runtime.NonUIThreadTestApplication#getDefaultApplicationId()
	 */
	protected String getDefaultApplicationId() {
		// In 3.0, the default is the "org.eclipse.ui.ide.worbench" application.
		return DEFAULT_APP_3_0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.junit.runtime.NonUIThreadTestApplication#runApp(java.lang.Object, org.eclipse.equinox.app.IApplicationContext, java.lang.String[])
	 */
	protected Object runApp(Object app, IApplicationContext context, String[] args) throws Exception {
		fTestableObject = PlatformUI.getTestableObject();
		fTestableObject.setTestHarness(this);
		return super.runApp(app, context, args);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.testing.ITestHarness#runTests()
	 */
	public void runTests() {
		try {
			fTestableObject.testingStarting();
			fTestableObject.runTest(new Runnable() {
				public void run() {
					RemotePluginTestRunner.main(Platform.getCommandLineArgs());
				}
			});
		} finally {
			fTestableObject.testingFinished();
		}
	}

}
