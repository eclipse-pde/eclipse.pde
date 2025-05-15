/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.build.tests;

import org.eclipse.pde.build.internal.tests.AssembleTests;
import org.eclipse.pde.build.internal.tests.FetchTests;
import org.eclipse.pde.build.internal.tests.ProductTests;
import org.eclipse.pde.build.internal.tests.ScriptGenerationTests;
import org.eclipse.pde.build.internal.tests.SourceTests;
import org.eclipse.pde.build.internal.tests.p2.LicenseTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ SourceTests.class, ScriptGenerationTests.class, ProductTests.class, LicenseTests.class,
		AssembleTests.class, P2TestSuite.class, FetchTests.class })
public class PDEBuildTestSuite {
}
