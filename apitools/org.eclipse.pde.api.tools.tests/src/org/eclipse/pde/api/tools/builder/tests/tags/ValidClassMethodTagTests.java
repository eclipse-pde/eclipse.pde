/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.builder.tests.tags;

import junit.framework.Test;

import org.eclipse.core.runtime.IPath;

/**
 * Tests valid javadoc tags for class methods
 * 
 * @since 3.5
 */
public class ValidClassMethodTagTests extends ValidMethodTagTests {

	/**
	 * Constructor
	 * @param name
	 */
	public ValidClassMethodTagTests(String name) {
		super(name);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("class");
	}
	
	/**
	 * @return the test for this class
	 */
	public static Test suite() {
		return buildTestSuite(ValidClassMethodTagTests.class);
	}
	
	/**
	 * Tests valid javadoc tags on methods in a class 
	 * using an incremental build
	 */
	public void testValidClassMethodTag1I() {
		deployIncrementalBuildTest(TESTING_PACKAGE, "test1", false);
	}
	
	/**
	 * Tests valid javadoc tags on methods in a class 
	 * using a full build
	 */
	public void testValidClassMethodTag1F() {
		deployFullBuildTest(TESTING_PACKAGE, "test1", false);
	}
	
	/**
	 * Tests valid javadoc tags on methods in an outer class 
	 * using an incremental build
	 */
	public void testValidClassMethodTag2I() {
		deployIncrementalBuildTest(TESTING_PACKAGE, "test2", false);
	}
	
	/**
	 * Tests valid javadoc tags on methods in an outer class 
	 * using a full build
	 */
	public void testValidClassMethodTag2F() {
		deployFullBuildTest(TESTING_PACKAGE, "test2", false);
	}
	
	/**
	 * Tests valid javadoc tags on methods in an inner class 
	 * using an incremental build
	 */
	public void testValidClassMethodTag3I() {
		deployIncrementalBuildTest(TESTING_PACKAGE, "test3", false);
	}
	
	/**
	 * Tests valid javadoc tags on methods in an inner class 
	 * using a full build
	 */
	public void testValidClassMethodTag3F() {
		deployFullBuildTest(TESTING_PACKAGE, "test3", false);
	}
	
	/**
	 * Tests valid javadoc tags on methods in inner / outer classes 
	 * using an incremental build
	 */
	public void testValidClassMethodTag4I() {
		deployIncrementalBuildTest(TESTING_PACKAGE, "test4", false);
	}
	
	/**
	 * Tests valid javadoc tags on methods in inner /outer classes 
	 * using a full build
	 */
	public void testValidClassMethodTag4F() {
		deployFullBuildTest(TESTING_PACKAGE, "test4", false);
	}
	
	/**
	 * Tests valid javadoc tags on methods in a class in the default package 
	 * using an incremental build
	 */
	public void testValidClassMethodTag5I() {
		deployIncrementalBuildTest(TESTING_PACKAGE, "test5", false);
	}
	
	/**
	 * Tests valid javadoc tags on methods in a class in the default package 
	 * using a full build
	 */
	public void testValidClassMethodTag5F() {
		deployFullBuildTest(TESTING_PACKAGE, "test5", false);
	}
	
	/**
	 * Tests valid javadoc tags on constructors in a class 
	 * using an incremental build
	 */
	public void testValidClassMethodTag6I() {
		deployIncrementalBuildTest(TESTING_PACKAGE, "test6", false);
	}
	
	/**
	 * Tests valid javadoc tags on constructors in a class 
	 * using a full build
	 */
	public void testValidClassMethodTag6F() {
		deployFullBuildTest(TESTING_PACKAGE, "test6", false);
	}
	
	/**
	 * Tests valid javadoc tags on constructors in an outer class 
	 * using an incremental build
	 */
	public void testValidClassMethodTag7I() {
		deployIncrementalBuildTest(TESTING_PACKAGE, "test7", false);
	}
	
	/**
	 * Tests valid javadoc tags on constructors in an outer class 
	 * using a full build
	 */
	public void testValidClassMethodTag7F() {
		deployFullBuildTest(TESTING_PACKAGE, "test7", false);
	}
	
	/**
	 * Tests valid javadoc tags on constructors in an inner class 
	 * using an incremental build
	 */
	public void testValidClassMethodTag8I() {
		deployIncrementalBuildTest(TESTING_PACKAGE, "test8", false);
	}
	
	/**
	 * Tests valid javadoc tags on constructors in an inner class 
	 * using a full build
	 */
	public void testValidClassMethodTag8F() {
		deployFullBuildTest(TESTING_PACKAGE, "test8", false);
	}
	
	/**
	 * Tests valid javadoc tags on constructors in inner / outer classes 
	 * using an incremental build
	 */
	public void testValidClassMethodTag9I() {
		deployIncrementalBuildTest(TESTING_PACKAGE, "test9", false);
	}
	
	/**
	 * Tests valid javadoc tags on constructors in inner /outer classes 
	 * using a full build
	 */
	public void testValidClassMethodTag9F() {
		deployFullBuildTest(TESTING_PACKAGE, "test9", false);
	}
	
	/**
	 * Tests valid javadoc tags on constructors in a class in the default package 
	 * using an incremental build
	 */
	public void testValidClassMethodTag10I() {
		deployIncrementalBuildTest(TESTING_PACKAGE, "test10", false);
	}
	
	/**
	 * Tests valid javadoc tags on constructors in a class in the default package 
	 * using a full build
	 */
	public void testValidClassMethodTag10F() {
		deployFullBuildTest(TESTING_PACKAGE, "test10", false);
	}
}
