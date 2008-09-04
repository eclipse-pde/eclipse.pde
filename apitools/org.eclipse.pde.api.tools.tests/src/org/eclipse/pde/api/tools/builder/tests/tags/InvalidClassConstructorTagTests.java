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
 * Tests unsupported javadoc tags on class constructors
 * 
 * @since 1.0
 */
public class InvalidClassConstructorTagTests extends InvalidMethodTagTests {

	/**
	 * Constructor
	 * @param name
	 */
	public InvalidClassConstructorTagTests(String name) {
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
			assertTrue("The problem message is not correct: "+message, message.endsWith("a constructor") || message.endsWith("a private constructor"));
		}
	}
	
	/**
	 * @return the test for this class
	 */
	public static Test suite() {
		return buildTestSuite(InvalidClassConstructorTagTests.class);
	}
	
	/**
	 * Tests the unsupported @noreference Javadoc tag on private constructors in a class 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag1I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test31", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noreference Javadoc tag on private constructors in a class 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag1F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test31", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noreference Javadoc tag on private constructors in an outer class 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag2I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test32", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noreference Javadoc tag on private constructors in an outer class 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag2F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test32", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noreference Javadoc tag on private constructors in an inner class 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag3I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test33", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noreference Javadoc tag on private constructors in an inner class 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag3F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test33", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noreference Javadoc tag on private constructors in a variety of inner / outer classes 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag4I() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployTagTest(TESTING_PACKAGE, "test34", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noreference Javadoc tag on private constructors in a variety of inner / outer classes 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag4F() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployTagTest(TESTING_PACKAGE, "test34", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noreference Javadoc tag on private constructors in a class in the default package 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag5I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest("", "test35", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noreference Javadoc tag on private constructors in a class in the default package 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag5F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest("", "test35", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate Javadoc tag on constructors in a class 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag6I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test36", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate Javadoc tag on constructors in a class 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag6F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test36", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate Javadoc tag on constructors in an outer class 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag7I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test37", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate Javadoc tag on constructors in an outer class 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag7F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test37", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate Javadoc tag on constructors in an inner class 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag8I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test38", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate Javadoc tag on constructors in an inner class 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag8F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test38", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate Javadoc tag on constructors in a variety of inner / outer classes 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag9I() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployTagTest(TESTING_PACKAGE, "test39", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate Javadoc tag on constructors in a variety of inner / outer classes 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag9F() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployTagTest(TESTING_PACKAGE, "test39", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate Javadoc tag on constructors in a class in the default package 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag10I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest("", "test40", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate Javadoc tag on constructors in a class in the default package 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag10F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest("", "test40", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noextend Javadoc tag on constructors in a class 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag11I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test41", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noextend Javadoc tag on constructors in a class 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag11F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test41", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noextend Javadoc tag on constructors in an outer class 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag12I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test42", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noextend Javadoc tag on constructors in an outer class 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag12F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test42", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noextend Javadoc tag on constructors in an inner class 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag13I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test43", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noextend Javadoc tag on constructors in an inner class 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag13F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test43", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noextend Javadoc tag on constructors in a variety of inner / outer classes 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag14I() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployTagTest(TESTING_PACKAGE, "test44", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noextend Javadoc tag on constructors in a variety of inner / outer classes 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag14F() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployTagTest(TESTING_PACKAGE, "test44", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noextend Javadoc tag on constructors in a class in the default package 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag15I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest("", "test45", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noextend Javadoc tag on constructors in a class in the default package 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag15F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest("", "test45", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on constructors in a class 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag16I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test46", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on constructors in a class 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag16F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test46", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on constructors in an outer class 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag17I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test47", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on constructors in an outer class 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag17F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test47", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on constructors in an inner class 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag18I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test48", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on constructors in an inner class 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag18F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test48", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on constructors in a variety of inner / outer classes 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag19I() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployTagTest(TESTING_PACKAGE, "test49", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on constructors in a variety of inner / outer classes 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag19F() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployTagTest(TESTING_PACKAGE, "test49", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on constructors in a class in the default package 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag20I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest("", "test50", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride Javadoc tag on constructors in a class in the default package 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag20F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest("", "test50", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noimplement Javadoc tag on constructors in a class 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag21I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test51", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noimplement Javadoc tag on constructors in a class 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag21F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test51", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noimplement Javadoc tag on constructors in an outer class 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag22I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test52", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noimplement Javadoc tag on constructors in an outer class 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag22F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test52", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noimplement Javadoc tag on constructors in an inner class 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag23I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test53", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noimplement Javadoc tag on constructors in an inner class 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag23F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test53", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noimplement Javadoc tag on constructors in a variety of inner / outer classes 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag24I() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployTagTest(TESTING_PACKAGE, "test54", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noimplement Javadoc tag on constructors in a variety of inner / outer classes 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag24F() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployTagTest(TESTING_PACKAGE, "test54", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noimplement Javadoc tag on constructors in a class in the default package 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag25I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest("", "test55", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noimplement Javadoc tag on constructors in a class in the default package 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag25F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest("", "test55", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests multiple unsupported Javadoc tags on constructors in a class 
	 * is detected properly using an incremental build
	 */
	public void testInvalidClassMethodTag26I() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployTagTest(TESTING_PACKAGE, "test56", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests multiple unsupported Javadoc tags on constructors in a class 
	 * is detected properly using a full build
	 */
	public void testInvalidClassMethodTag26F() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployTagTest(TESTING_PACKAGE, "test56", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
}
