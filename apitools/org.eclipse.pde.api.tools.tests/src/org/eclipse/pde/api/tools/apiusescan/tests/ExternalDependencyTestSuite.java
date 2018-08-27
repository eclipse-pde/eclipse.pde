/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.apiusescan.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;


/**
 * Test suite that is run tests for External Dependency from API Use Scan reports
 *
 * @since 1.0.0
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
		ReferenceCountTests.class, ExternalDependencyProblemMarkerTests.class
})
public class ExternalDependencyTestSuite {

}
