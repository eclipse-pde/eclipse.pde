/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.comparator.tests;


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
		addTest(new TestSuite(FieldDeltaTests.class));
		addTest(new TestSuite(InterfaceDeltaTests.class));
		addTest(new TestSuite(ClassDeltaTests.class));
		addTest(new TestSuite(AnnotationDeltaTests.class));
		addTest(new TestSuite(EnumDeltaTests.class));
		addTest(new TestSuite(MethodDeltaTests.class));
		addTest(new TestSuite(MixedTypesDeltaTests.class));
		addTest(new TestSuite(BundlesDeltaTests.class));
		addTest(new TestSuite(RestrictionsDeltaTests.class));
	}
}
