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

import org.eclipse.pde.api.tools.builder.tests.performance.PerformanceTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;


/**
 * Performance test suite that is run as a JUnit plug-in test
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
		PerformanceTest.class
})
public class ApiToolsPerformanceTestSuite {

}
