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
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.pde.api.tools.builder.tests.ApiProblem;

/**
 * Tests invalid javadoc tags on enum methods
 * 
 * @since 3.5
 */
public class InvalidEnumMethodTagTests extends InvalidMethodTagTests {

	/**
	 * Constructor
	 * @param name
	 */
	public InvalidEnumMethodTagTests(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getTestCompliance()
	 */
	protected String getTestCompliance() {
		return CompilerOptions.VERSION_1_5;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("enum");
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#assertProblems(org.eclipse.pde.api.tools.builder.tests.ApiProblem[])
	 */
	protected void assertProblems(ApiProblem[] problems) {
		String message = null;
		for(int i = 0; i < problems.length; i++) {
			message = problems[i].getMessage();
			assertTrue("The problem message is not correct: "+message, message.endsWith("an enum method"));
		}
	}
	
	/**
	 * @return the test for this class
	 */
	public static Test suite() {
		return buildTestSuite(InvalidEnumMethodTagTests.class);
	}
	
	/**
	 * Tests the unsupported @noextend tag on enum methods
	 * using an incremental build
	 */
	public void testInvalidEnumMethodTag1I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test1", true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on enum methods
	 * using a full build
	 */
	public void testInvalidEnumMethodTag1F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test1", true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on outer enum methods
	 * using an incremental build
	 */
	public void testInvalidEnumMethodTag2I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test2", true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on outer enum methods
	 * using a full build
	 */
	public void testInvalidEnumMethodTag2F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test2", true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on inner enum methods
	 * using an incremental build
	 */
	public void testInvalidEnumMethodTag3I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test3", true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on inner enum methods
	 * using a full build
	 */
	public void testInvalidEnumMethodTag3F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test3", true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on a variety of inner / outer enum methods
	 * using an incremental build
	 */
	public void testInvalidEnumMethodTag4I() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test4", true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on a variety of inner / outer enum methods
	 * using a full build
	 */
	public void testInvalidEnumMethodTag4F() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test4", true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on enum methods in the default package
	 * using an incremental build
	 */
	public void testInvalidEnumMethodTag5I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest("", "test5", true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on enum methods in the default package
	 * using a full build
	 */
	public void testInvalidEnumMethodTag5F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest("", "test5", true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on enum methods
	 * using an incremental build
	 */
	public void testInvalidEnumMethodTag6I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test6", true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on enum methods
	 * using a full build
	 */
	public void testInvalidEnumMethodTag6F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test6", true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on outer enum methods
	 * using an incremental build
	 */
	public void testInvalidEnumMethodTag7I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test7", true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on on outer enum methods
	 * using a full build
	 */
	public void testInvalidEnumMethodTag7F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test7", true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on inner enum methods
	 * using an incremental build
	 */
	public void testInvalidEnumMethodTag8I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test8", true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on inner enum methods
	 * using a full build
	 */
	public void testInvalidEnumMethodTag8F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test8", true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on a variety of inner / outer enum methods
	 * using an incremental build
	 */
	public void testInvalidEnumMethodTag9I() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test9", true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on a variety of inner / outer enum methods
	 * using a full build
	 */
	public void testInvalidEnumMethodTag9F() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test9", true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on enum methods in the default package
	 * using an incremental build
	 */
	public void testInvalidEnumMethodTag10I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest("", "test10", true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on enum methods in the default package
	 * using a full build
	 */
	public void testInvalidEnumMethodTag10F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest("", "test10", true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on enum methods
	 * using an incremental build
	 */
	public void testInvalidEnumMethodTag11I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test11", true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on enum methods
	 * using a full build
	 */
	public void testInvalidEnumMethodTag11F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test11", true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on outer enum methods
	 * using an incremental build
	 */
	public void testInvalidEnumMethodTag12I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test12", true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on outer enum methods
	 * using a full build
	 */
	public void testInvalidEnumMethodTag12F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test12", true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on inner enum methods
	 * using an incremental build
	 */
	public void testInvalidEnumMethodTag13I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test13", true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on inner enum methods
	 * using a full build
	 */
	public void testInvalidEnumMethodTag13F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test13", true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on a variety of inner / outer enum methods
	 * using an incremental build
	 */
	public void testInvalidEnumMethodTag14I() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test14", true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on a variety of inner / outer enum methods
	 * using a full build
	 */
	public void testInvalidEnumMethodTag14F() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test14", true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on enum methods in the default package
	 * using an incremental build
	 */
	public void testInvalidEnumMethodTag15I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest("", "test15", true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on enum methods in the default package
	 * using a full build
	 */
	public void testInvalidEnumMethodTag15F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest("", "test15", true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on enum methods
	 * using an incremental build
	 */
	public void testInvalidEnumMethodTag16I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test16", true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on enum methods
	 * using a full build
	 */
	public void testInvalidEnumMethodTag16F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test16", true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on outer enum methods
	 * using an incremental build
	 */
	public void testInvalidEnumMethodTag17I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test17", true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on outer enum methods
	 * using a full build
	 */
	public void testInvalidEnumMethodTag17F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test17", true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on inner enum methods
	 * using an incremental build
	 */
	public void testInvalidEnumMethodTag18I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test18", true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on inner enum methods
	 * using a full build
	 */
	public void testInvalidEnumMethodTag18F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test18", true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on a variety of inner / outer enum methods
	 * using an incremental build
	 */
	public void testInvalidEnumMethodTag19I() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test19", true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on a variety of inner / outer enum methods
	 * using a full build
	 */
	public void testInvalidEnumMethodTag19F() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test19", true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on enum methods in the default package
	 * using an incremental build
	 */
	public void testInvalidEnumMethodTag20I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest("", "test20", true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on enum methods in the default package
	 * using a full build
	 */
	public void testInvalidEnumMethodTag20F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest("", "test20", true);
	}
	
	/**
	 * Tests all the unsupported tags on a variety of enum methods
	 * using an incremental build
	 */
	public void testInvalidEnumMethodTag21I() {
		setExpectedProblemIds(getDefaultProblemSet(24));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test21", true);
	}
	
	/**
	 * Tests all the unsupported tags on a variety of enum methods
	 * using a full build
	 */
	public void testInvalidEnumMethodTag21F() {
		setExpectedProblemIds(getDefaultProblemSet(24));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test21", true);
	}
}
