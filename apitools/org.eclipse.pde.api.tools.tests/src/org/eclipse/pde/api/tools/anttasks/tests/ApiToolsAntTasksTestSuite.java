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
 *******************************************************************************/
package org.eclipse.pde.api.tools.anttasks.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;


/**
 * Test suite that is run as a JUnit plugin test
 *
 * @since 1.0.0
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
		ApiToolingAnalysisAntTaskTests.class, ApiToolingCompareAntTaskTests.class, ApiToolingApiuseAntTaskTests.class,
		ApiToolingApiFreezeAntTaskTests.class
})
public class ApiToolsAntTasksTestSuite {
}
