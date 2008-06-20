/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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

import org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest;
import org.eclipse.pde.api.tools.model.tests.ApiFilterStoreTests;
import org.eclipse.pde.api.tools.problems.tests.ApiProblemTests;
import org.eclipse.pde.api.tools.util.tests.ApiDescriptionProcessorTests;
import org.eclipse.pde.api.tools.util.tests.ApiProfileManagerTests;
import org.eclipse.pde.api.tools.util.tests.PreferencesTests;
import org.eclipse.pde.api.tools.util.tests.ProjectCreationTests;


/**
 * Test suite that is run as a JUnit plugin test
 * 
 * @since 1.0.0
 */
public class ApiToolsPluginTestSuite extends TestSuite {

	/**
	 * Returns the suite.  This is required to
	 * use the JUnit Launcher.
	 * @return the test
	 */
	public static Test suite() {
		return new ApiToolsPluginTestSuite();
	}
	
	/**
	 * Constructor
	 */
	public ApiToolsPluginTestSuite() {
		addTest(new TestSuite(ProjectCreationTests.class));
		addTest(new TestSuite(ApiDescriptionProcessorTests.class));
		addTest(new TestSuite(PreferencesTests.class));
		addTest(new TestSuite(ApiProfileManagerTests.class));
		addTest(new TestSuite(ApiFilterStoreTests.class));
		addTest(new TestSuite(ApiProblemTests.class));
		addTest(ApiBuilderTest.suite());
	}
}
