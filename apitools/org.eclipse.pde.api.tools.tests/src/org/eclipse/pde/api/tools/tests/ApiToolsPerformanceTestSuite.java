/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.api.tools.tests;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.eclipse.pde.api.tools.builder.tests.performance.ApiDescriptionTests;
import org.eclipse.pde.api.tools.builder.tests.performance.ExternalDependencyPerfTests;
import org.eclipse.pde.api.tools.builder.tests.performance.FullSourceBuildTests;
import org.eclipse.pde.api.tools.builder.tests.performance.IncrementalBuildTests;
import org.eclipse.pde.api.tools.builder.tests.performance.UseScanTests;


/**
 * Performance test suite that is run as a JUnit plug-in test
 */
@Suite
@SelectClasses({
		FullSourceBuildTests.class, ApiDescriptionTests.class, IncrementalBuildTests.class,
		ExternalDependencyPerfTests.class, UseScanTests.class
})
public class ApiToolsPerformanceTestSuite {

}
