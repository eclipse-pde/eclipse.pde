/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.performance;

import org.eclipse.pde.ui.tests.performance.parts.*;

import junit.framework.*;

public class PDEPerformanceTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Performance Test Suite for org.eclipse.pde.ui"); //$NON-NLS-1$
		suite.addTest(InitializeModelsPerfTest.suite());
		suite.addTest(SchemaPerfTest.suite());
		suite.addTest(PluginsViewPerfTest.suite());
		suite.addTest(OpenManifestEditorPerfTest.suite());
		return suite;
	}

}
