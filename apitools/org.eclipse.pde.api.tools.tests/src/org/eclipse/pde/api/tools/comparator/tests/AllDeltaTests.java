/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.comparator.tests;


import org.eclipse.pde.api.tools.tests.util.ProjectUtils;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test suite for all of the API tools test
 *
 * @since 1.0.0
 */
public class AllDeltaTests extends TestSuite {

	/**
	 * Returns the suite.  This is required to
	 * use the JUnit Launcher.
	 * @return the test
	 */
	public static Test suite() {
		return new AllDeltaTests();
	}

	/**
	 * Constructor
	 */
	public AllDeltaTests() {
		addTestSuite(FieldDeltaTests.class);
		addTestSuite(InterfaceDeltaTests.class);
		addTestSuite(ClassDeltaTests.class);
		addTestSuite(AnnotationDeltaTests.class);
		addTestSuite(EnumDeltaTests.class);
		addTestSuite(MethodDeltaTests.class);
		addTestSuite(MixedTypesDeltaTests.class);
		addTestSuite(BundlesDeltaTests.class);
		addTestSuite(RestrictionsDeltaTests.class);
		addTestSuite(ApiScopeDeltaTests.class);
		if (ProjectUtils.isJava8Compatible()) {
			addTestSuite(Java8DeltaTests.class);
		}
	}
}
