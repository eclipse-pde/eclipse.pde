/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.tests;

import org.eclipse.pde.api.tools.builder.tests.performance.PerformanceTest;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * Performance test suite that is run as a JUnit plug-in test
 */
public class ApiToolsPerformanceTestSuite extends TestSuite {

	/**
	 * Returns the suite.  This is required to
	 * use the JUnit Launcher.
	 * @return the test
	 */
	public static Test suite() {
		return new ApiToolsPerformanceTestSuite();
	}


	public ApiToolsPerformanceTestSuite() {
		addTest(PerformanceTest.suite());
	}
}
