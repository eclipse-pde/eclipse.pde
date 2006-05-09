/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.model.bundle;


import junit.framework.Test;
import junit.framework.TestSuite;

public class AllBundleModelTests {
	
	public static Test suite() {
		TestSuite suite = new TestSuite("Test Suite for testing the bundle model"); //$NON-NLS-1$
		suite.addTest(ImportPackageTestCase.suite());
		suite.addTest(ExportPackageTestCase.suite());
		suite.addTest(BundleClasspathTestCase.suite());
		suite.addTest(BundleActivatorTestCase.suite());
		suite.addTest(BundleNameTestCase.suite());
		suite.addTest(BundleLocalizationTestCase.suite());
		suite.addTest(LazyStartTestCase.suite());
		suite.addTest(RequireBundleTestCase.suite());
		suite.addTest(ExecutionEnvironmentTestCase.suite());
		suite.addTest(BundleSymbolicNameTestCase.suite());
		suite.addTest(BundleVendorTestCase.suite());
		suite.addTest(BundleVersionTestCase.suite());
		return suite;
	}

}
