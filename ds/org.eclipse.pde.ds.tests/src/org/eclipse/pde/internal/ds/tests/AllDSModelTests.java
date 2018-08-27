/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ds.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllDSModelTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test Suite for org.eclipse.pde.ds.core"); //$NON-NLS-1$
		suite.addTestSuite(DSComponentTestCase.class);
		suite.addTestSuite(DSServiceTestCase.class);
		suite.addTestSuite(DSReferenceTestCase.class);
		suite.addTestSuite(DSProvideTestCase.class);
		suite.addTestSuite(DSPropertyTestCase.class);
		suite.addTestSuite(DSPropertiesTestCase.class);
		suite.addTestSuite(DSImplementationTestCase.class);
		suite.addTestSuite(DSObjectTestCase.class);
		suite.addTestSuite(DSv10tov11TestCase.class);
		return suite;
	}

}
