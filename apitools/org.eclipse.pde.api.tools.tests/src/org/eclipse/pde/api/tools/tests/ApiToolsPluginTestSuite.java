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

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.eclipse.pde.api.tools.anttasks.tests.ApiToolsAntTasksTestSuite;
import org.eclipse.pde.api.tools.applications.BundleJarFilesTest;
import org.eclipse.pde.api.tools.builder.tests.ApiBuilderTestSuite;
import org.eclipse.pde.api.tools.builder.tests.ApiTestingEnvironment;
import org.eclipse.pde.api.tools.model.tests.ApiFilterStoreTests;
import org.eclipse.pde.api.tools.model.tests.FilterStoreTests;
import org.eclipse.pde.api.tools.problems.tests.ApiProblemTests;
import org.eclipse.pde.api.tools.util.tests.ApiBaselineManagerTests;
import org.eclipse.pde.api.tools.util.tests.ApiDescriptionProcessorTests;
import org.eclipse.pde.api.tools.util.tests.PreferencesTests;
import org.eclipse.pde.api.tools.util.tests.ProjectCreationTests;
import org.eclipse.pde.api.tools.util.tests.TargetAsBaselineTests;
import org.junit.platform.suite.api.BeforeSuite;


/**
 * Test suite that is run as a JUnit plugin test
 *
 * @since 1.0.0
 */
@Suite
@SelectClasses({
		ProjectCreationTests.class, ApiDescriptionProcessorTests.class, PreferencesTests.class,
		ApiBaselineManagerTests.class, ApiFilterStoreTests.class, FilterStoreTests.class, ApiProblemTests.class,
		TargetAsBaselineTests.class, ApiBuilderTestSuite.class, ApiToolsAntTasksTestSuite.class,
		BundleJarFilesTest.class
})
public class ApiToolsPluginTestSuite {

	@BeforeSuite
	@SuppressWarnings("restriction")
	public static void setUpBeforeClass() throws Exception {
		org.eclipse.jdt.internal.core.search.processing.JobManager.VERBOSE = true;
		ApiTestingEnvironment.setTargetPlatform();
	}

}
