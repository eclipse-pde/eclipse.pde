/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.pde.build.internal.tests.p2.P2Tests;
import org.eclipse.pde.build.internal.tests.p2.PublishingTests;

public class P2TestSuite {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test Suite for p2 bits of org.eclipse.pde.build"); //$NON-NLS-1$
		suite.addTestSuite(P2Tests.class);
		suite.addTestSuite(PublishingTests.class);
		return suite;
	}

}
