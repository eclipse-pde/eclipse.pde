/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.apiusescan.tests;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * Test suite that is run tests for External Dependency from API Use Scan reports 
 * 
 * @since 1.0.0
 */
public class ExternalDependencyTestSuite extends TestSuite {

	/**
	 * Returns the suite.  This is required to
	 * use the JUnit Launcher.
	 * @return the test
	 */
	public static Test suite() {
		return new ExternalDependencyTestSuite();
	}
	
	/**
	 * Constructor
	 */
	public ExternalDependencyTestSuite() {
		addTest(new TestSuite(ReferenceCountTests.class));
		addTest(new TestSuite(ExternalDependencyProblemMarkerTests.class));
	}
}
