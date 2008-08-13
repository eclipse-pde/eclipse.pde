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

import org.eclipse.core.resources.IncrementalProjectBuilder;
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
		deployTagTest(TESTING_PACKAGE, "test1", false, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests valid javadoc tags on methods in a class 
	 * using a full build
	 */
	public void testValidClassMethodTag1F() {
		deployTagTest(TESTING_PACKAGE, "test1", false, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests valid javadoc tags on methods in an outer class 
	 * using an incremental build
	 */
	public void testValidClassMethodTag2I() {
		deployTagTest(TESTING_PACKAGE, "test2", false, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests valid javadoc tags on methods in an outer class 
	 * using a full build
	 */
	public void testValidClassMethodTag2F() {
		deployTagTest(TESTING_PACKAGE, "test2", false, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests valid javadoc tags on methods in an inner class 
	 * using an incremental build
	 */
	public void testValidClassMethodTag3I() {
		deployTagTest(TESTING_PACKAGE, "test3", false, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests valid javadoc tags on methods in an inner class 
	 * using a full build
	 */
	public void testValidClassMethodTag3F() {
		deployTagTest(TESTING_PACKAGE, "test3", false, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests valid javadoc tags on methods in inner / outer classes 
	 * using an incremental build
	 */
	public void testValidClassMethodTag4I() {
		deployTagTest(TESTING_PACKAGE, "test4", false, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests valid javadoc tags on methods in inner /outer classes 
	 * using a full build
	 */
	public void testValidClassMethodTag4F() {
		deployTagTest(TESTING_PACKAGE, "test4", false, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests valid javadoc tags on methods in a class in the default package 
	 * using an incremental build
	 */
	public void testValidClassMethodTag5I() {
		deployTagTest(TESTING_PACKAGE, "test5", false, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests valid javadoc tags on methods in a class in the default package 
	 * using a full build
	 */
	public void testValidClassMethodTag5F() {
		deployTagTest(TESTING_PACKAGE, "test5", false, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests valid javadoc tags on constructors in a class 
	 * using an incremental build
	 */
	public void testValidClassMethodTag6I() {
		deployTagTest(TESTING_PACKAGE, "test6", false, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests valid javadoc tags on constructors in a class 
	 * using a full build
	 */
	public void testValidClassMethodTag6F() {
		deployTagTest(TESTING_PACKAGE, "test6", false, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests valid javadoc tags on constructors in an outer class 
	 * using an incremental build
	 */
	public void testValidClassMethodTag7I() {
		deployTagTest(TESTING_PACKAGE, "test7", false, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests valid javadoc tags on constructors in an outer class 
	 * using a full build
	 */
	public void testValidClassMethodTag7F() {
		deployTagTest(TESTING_PACKAGE, "test7", false, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests valid javadoc tags on constructors in an inner class 
	 * using an incremental build
	 */
	public void testValidClassMethodTag8I() {
		deployTagTest(TESTING_PACKAGE, "test8", false, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests valid javadoc tags on constructors in an inner class 
	 * using a full build
	 */
	public void testValidClassMethodTag8F() {
		deployTagTest(TESTING_PACKAGE, "test8", false, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests valid javadoc tags on constructors in inner / outer classes 
	 * using an incremental build
	 */
	public void testValidClassMethodTag9I() {
		deployTagTest(TESTING_PACKAGE, "test9", false, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests valid javadoc tags on constructors in inner /outer classes 
	 * using a full build
	 */
	public void testValidClassMethodTag9F() {
		deployTagTest(TESTING_PACKAGE, "test9", false, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests valid javadoc tags on constructors in a class in the default package 
	 * using an incremental build
	 */
	public void testValidClassMethodTag10I() {
		deployTagTest(TESTING_PACKAGE, "test10", false, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests valid javadoc tags on constructors in a class in the default package 
	 * using a full build
	 */
	public void testValidClassMethodTag10F() {
		deployTagTest(TESTING_PACKAGE, "test10", false, IncrementalProjectBuilder.FULL_BUILD, true);
	}
}
