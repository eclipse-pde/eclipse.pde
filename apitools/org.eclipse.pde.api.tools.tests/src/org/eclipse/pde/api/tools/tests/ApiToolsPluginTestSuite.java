/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Manumitting Technologies Inc - bug 324310
 *******************************************************************************/
package org.eclipse.pde.api.tools.tests;

import org.eclipse.pde.api.tools.anttasks.tests.ApiToolsAntTasksTestSuite;
import org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest;
import org.eclipse.pde.api.tools.builder.tests.ApiTestingEnvironment;
import org.eclipse.pde.api.tools.model.tests.ApiFilterStoreTests;
import org.eclipse.pde.api.tools.model.tests.FilterStoreTests;
import org.eclipse.pde.api.tools.problems.tests.ApiProblemTests;
import org.eclipse.pde.api.tools.util.tests.ApiBaselineManagerTests;
import org.eclipse.pde.api.tools.util.tests.ApiDescriptionProcessorTests;
import org.eclipse.pde.api.tools.util.tests.PreferencesTests;
import org.eclipse.pde.api.tools.util.tests.ProjectCreationTests;
import org.eclipse.pde.api.tools.util.tests.TargetAsBaselineTests;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;


/**
 * Test suite that is run as a JUnit plugin test
 *
 * @since 1.0.0
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
		ProjectCreationTests.class, ApiDescriptionProcessorTests.class, PreferencesTests.class,
		ApiBaselineManagerTests.class, ApiFilterStoreTests.class, FilterStoreTests.class, ApiProblemTests.class,
		TargetAsBaselineTests.class, ApiBuilderTest.class, ApiToolsAntTasksTestSuite.class
})
public class ApiToolsPluginTestSuite {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ApiTestingEnvironment.setTargetPlatform();
	}

}
