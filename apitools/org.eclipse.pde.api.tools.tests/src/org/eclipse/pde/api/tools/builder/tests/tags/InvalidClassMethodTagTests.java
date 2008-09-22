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
import org.eclipse.pde.api.tools.builder.tests.ApiProblem;

/**
 * Tests invalid javadoc tags on class methods
 * 
 * @since 1.0
 */
public class InvalidClassMethodTagTests extends InvalidMethodTagTests {

	/**
	 * Constructor
	 * @param name
	 */
	public InvalidClassMethodTagTests(String name) {
		super(name);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("class");
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#assertProblems(org.eclipse.pde.api.tools.builder.tests.ApiProblem[])
	 */
	protected void assertProblems(ApiProblem[] problems) {
		String message = null;
		for(int i = 0; i < problems.length; i++) {
			message = problems[i].getMessage();
			assertTrue("The problem message is not correct: "+message, message.endsWith("a method") || message.endsWith("a private method") 
					|| message.endsWith("a final method") || message.endsWith("a static method") || message.endsWith("a static final method"));
		}
	}
	
	/**
	 * @return the test for this class
	 */
	public static Test suite() {
		return buildTestSuite(InvalidClassMethodTagTests.class);
	}
	
	/**
	 * Tests the unsupported @noimplement Javadoc tag on a variety of methods in a class 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag1I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest(TESTING_PACKAGE, "test1", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noimplement Javadoc tag on a variety of methods in a class 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag1F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest(TESTING_PACKAGE, "test1", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noimplement Javadoc tag on a variety of methods in an outer class 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag2I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest(TESTING_PACKAGE, "test2", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noimplement Javadoc tag on a variety of methods in an outer class 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag2F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest(TESTING_PACKAGE, "test2", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noimplement Javadoc tag on a variety of methods in an inner class 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag3I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest(TESTING_PACKAGE, "test3", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noimplement Javadoc tag on a variety of methods in an inner class 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag3F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest(TESTING_PACKAGE, "test3", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noimplement Javadoc tag on a variety of methods in a variety of inner / outer classes 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag4I() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployTagTest(TESTING_PACKAGE, "test4", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noimplement Javadoc tag on a variety of methods in a variety of inner / outer classes 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag4F() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployTagTest(TESTING_PACKAGE, "test4", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noimplement Javadoc tag on a variety of methods in a class in the default package 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag5I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest("", "test5", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noimplement Javadoc tag on a variety of methods in a class in the default package 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag5F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest("", "test5", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noextend Javadoc tag on a variety of methods in a class 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag6I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest(TESTING_PACKAGE, "test6", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noextend Javadoc tag on a variety of methods in a class 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag6F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest(TESTING_PACKAGE, "test6", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noextend Javadoc tag on a variety of methods in an outer class 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag7I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest(TESTING_PACKAGE, "test7", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noextend Javadoc tag on a variety of methods in an outer class 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag7F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest(TESTING_PACKAGE, "test7", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noextend Javadoc tag on a variety of methods in an inner class 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag8I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest(TESTING_PACKAGE, "test8", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noextend Javadoc tag on a variety of methods in an inner class 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag8F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest(TESTING_PACKAGE, "test8", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noextend Javadoc tag on a variety of methods in a variety of inner / outer classes 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag9I() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployTagTest(TESTING_PACKAGE, "test9", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noextend Javadoc tag on a variety of methods in a variety of inner / outer classes 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag9F() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployTagTest(TESTING_PACKAGE, "test9", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noextend Javadoc tag on a variety of methods in a class in the default package 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag10I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest("", "test10", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noextend Javadoc tag on a variety of methods in a class in the default package 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag10F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest("", "test10", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate Javadoc tag on a variety of methods in a class 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag11I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest(TESTING_PACKAGE, "test11", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate Javadoc tag on a variety of methods in a class 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag11F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest(TESTING_PACKAGE, "test11", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate Javadoc tag on a variety of methods in an outer class 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag12I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest(TESTING_PACKAGE, "test12", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate Javadoc tag on a variety of methods in an outer class 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag12F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest(TESTING_PACKAGE, "test12", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate Javadoc tag on a variety of methods in an inner class 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag13I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest(TESTING_PACKAGE, "test13", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate Javadoc tag on a variety of methods in an inner class 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag13F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest(TESTING_PACKAGE, "test13", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate Javadoc tag on a variety of methods in a variety of inner / outer classes 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag14I() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployTagTest(TESTING_PACKAGE, "test14", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate Javadoc tag on a variety of methods in a variety of inner / outer classes 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag14F() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployTagTest(TESTING_PACKAGE, "test14", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate Javadoc tag on a variety of methods in a class in the default package 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag15I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest("", "test15", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate Javadoc tag on a variety of methods in a class in the default package 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag15F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest("", "test15", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on private methods in a class
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag16I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test16", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on private methods in a class
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag16F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test16", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on private methods in an outer class
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag17I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test17", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on private methods in an outer class
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag17F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test17", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on private methods in an inner class
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag18I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test18", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on private methods in an inner class
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag18F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test18", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on private methods in a variety of inner /outer classes
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag19I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployTagTest(TESTING_PACKAGE, "test19", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on private methods in a variety of inner /outer classes
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag19F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployTagTest(TESTING_PACKAGE, "test19", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on private methods in a class in the default package
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag20I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest("", "test20", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on private methods in a class in the default package
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag20F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest("", "test20", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on final methods in a class
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag21I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test21", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on final methods in a class
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag21F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test21", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on final methods in an outer class
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag22I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test22", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on final methods in an outer class
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag22F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test22", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on final methods in an inner class
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag23I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test23", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on final methods in an inner class
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag23F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test23", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on final methods in a variety of inner /outer classes
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag24I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployTagTest(TESTING_PACKAGE, "test24", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on final methods in a variety of inner /outer classes
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag24F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployTagTest(TESTING_PACKAGE, "test24", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on final methods in a class in the default package
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag25I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest("", "test25", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on final methods in a class in the default package
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag25F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest("", "test25", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noreference Javadoc tag on private methods in a class
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag26I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test26", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noreference Javadoc tag on private methods in a class
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag26F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test26", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noreference Javadoc tag on private methods in an outer class
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag27I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test27", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noreference Javadoc tag on private methods in an outer class
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag27F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test27", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noreference Javadoc tag on private methods in an inner class
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag28I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test28", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noreference Javadoc tag on private methods in an inner class
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag28F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test28", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noreference Javadoc tag on private methods in a variety of inner /outer classes
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag29I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployTagTest(TESTING_PACKAGE, "test29", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noreference Javadoc tag on private methods in a variety of inner /outer classes
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag29F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployTagTest(TESTING_PACKAGE, "test29", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noreference Javadoc tag on private methods in a class in the default package
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag30I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest("", "test30", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noreference Javadoc tag on private methods in a class in the default package
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag30F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest("", "test30", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on a static method using an incremental build
	 */
	public void testInvalidClassMethodTag31I() {
		setExpectedProblemIds(getDefaultProblemIdSet(3));
		deployTagTest("", "test57", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on a static method using  full build
	 */
	public void testInvalidClassMethodTag31F() {
		setExpectedProblemIds(getDefaultProblemIdSet(3));
		deployTagTest("", "test57", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
}
