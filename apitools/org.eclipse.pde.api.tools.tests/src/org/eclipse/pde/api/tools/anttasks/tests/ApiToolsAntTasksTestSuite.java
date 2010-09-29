/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.anttasks.tests;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * Test suite that is run as a JUnit plugin test
 * 
 * @since 1.0.0
 */
public class ApiToolsAntTasksTestSuite extends TestSuite {

	/**
	 * Returns the suite.  This is required to
	 * use the JUnit Launcher.
	 * @return the test
	 */
	public static Test suite() {
		return new ApiToolsAntTasksTestSuite();
	}
	
	/**
	 * Constructor
	 */
	public ApiToolsAntTasksTestSuite() {
		addTest(new TestSuite(ApiToolingAnalysisAntTaskTests.class));
		addTest(new TestSuite(ApiToolingCompareAntTaskTests.class));
		addTest(new TestSuite(ApiToolingApiuseAntTaskTests.class));
		addTest(new TestSuite(ApiToolingApiFreezeAntTaskTests.class));
	}
}
