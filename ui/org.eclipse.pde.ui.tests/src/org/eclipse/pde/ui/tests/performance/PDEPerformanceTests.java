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

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.pde.ui.tests.performance.parts.InitializeModelsPerfTest;
import org.eclipse.pde.ui.tests.performance.parts.OpenManifestEditorPerfTest;
import org.eclipse.pde.ui.tests.performance.parts.SchemaPerfTest;

public class PDEPerformanceTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Performance Test Suite for org.eclipse.pde.ui"); //$NON-NLS-1$
		suite.addTest(OpenManifestEditorPerfTest.suite());
		suite.addTest(SchemaPerfTest.suite());
		suite.addTest(InitializeModelsPerfTest.suite());
		return suite;
	}

}
