/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
t https://www.eclipse.org/legal/epl-2.0/
t
t SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.build.tests;

import static org.junit.Assume.assumeTrue;

import org.eclipse.pde.build.internal.tests.p2.P2Tests;
import org.eclipse.pde.build.internal.tests.p2.PublishingTests;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ P2Tests.class, PublishingTests.class })
public class P2TestSuite {

	@BeforeClass
	public static void setUp() {
		assumeTrue(Boolean.valueOf(System.getProperty("pde.build.includeP2", "true")).booleanValue());
	}
}
