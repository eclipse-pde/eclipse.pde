/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.tests;


import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test suite for all of the API tools test 
 * 
 * @since 1.0.0
 */
public class AllTestSuite extends TestSuite {
	/**
	 * Returns the suite.  This is required to
	 * use the JUnit Launcher.
	 * @return the test
	 */
	public static Test suite() {
		TestSuite suite = new TestSuite(AllTestSuite.class.getName());
		suite.addTest(ApiToolsTestSuite.suite());
		suite.addTest(ApiToolsPluginTestSuite.suite());
		return suite;
	}
}
