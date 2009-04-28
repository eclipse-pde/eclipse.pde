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
package org.eclipse.pde.ui.tests.launcher;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @since 3.5
 */
public class AllLauncherTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test Suite for testing launching utils"); //$NON-NLS-1$
		suite.addTest(LaunchConfigurationHelperTestCase.suite());
		return suite;
	}

}
