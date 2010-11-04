/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.build.tests;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.pde.build.internal.tests.*;
import org.eclipse.pde.build.internal.tests.p2.*;

public class PDEBuildTestSuite {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test Suite for org.eclipse.pde.build"); //$NON-NLS-1$
		suite.addTestSuite(SourceTests.class);
		suite.addTestSuite(ScriptGenerationTests.class);
		suite.addTestSuite(ProductTests.class);
		suite.addTestSuite(P2Tests.class);
		suite.addTestSuite(PublishingTests.class);
		suite.addTestSuite(LicenseTests.class);
		suite.addTest(AssembleTests.suite());

		if (System.getProperty("pde.build.includeFetch") != null)
			suite.addTestSuite(FetchTests.class);
		return suite;
	}

}
