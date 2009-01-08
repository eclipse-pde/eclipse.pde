/*******************************************************************************
 * Copyright (c) 2009 EclipseSource Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     EclipseSource Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.junit.runtime;

import org.eclipse.core.runtime.Platform;

/**
 * A Workbench that runs a test suite specified in the
 * command line arguments.
 */
public class NonUIThreadTestApplication extends UITestApplication {

	public void runTests() {
		fTestableObject.testingStarting();
		RemotePluginTestRunner.main(Platform.getCommandLineArgs());
		fTestableObject.testingFinished();
	}

}
