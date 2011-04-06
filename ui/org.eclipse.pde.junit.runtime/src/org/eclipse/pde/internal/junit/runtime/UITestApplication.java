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

import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.ui.PlatformUI;

/**
 * A Workbench that runs a test suite specified in the
 * command line arguments.
 */
public class UITestApplication extends NonUIThreadTestApplication {

	private static final String DEFAULT_APP_3_0 = "org.eclipse.ui.ide.workbench"; //$NON-NLS-1$

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
		// create UI test harness
		fTestHarness = new PlatformUITestHarness(PlatformUI.getTestableObject(), false);

		// continue application launch
		return super.runApp(app, context, args);
	}
}
