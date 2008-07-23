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
import org.eclipse.pde.api.tools.builder.tests.ApiProblem;

/**
 * Tests invalid javadoc tags on interface methods
 * 
 * @since 3.5
 */
public class InvalidInterfaceMethodTagTests extends InvalidMethodTagTests {

	/**
	 * Constructor
	 * @param name
	 */
	public InvalidInterfaceMethodTagTests(String name) {
		super(name);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("interface");
	}
	
	/**
	 * @return the test for this class
	 */
	public static Test suite() {
		return buildTestSuite(InvalidInterfaceMethodTagTests.class);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#assertProblems(org.eclipse.pde.api.tools.builder.tests.ApiProblem[])
	 */
	protected void assertProblems(ApiProblem[] problems) {
		for(int i = 0; i < problems.length; i++) {
			assertTrue("the message does not end correctly", problems[i].getMessage().endsWith("an interface method"));
		}
	}
	
	/**
	 * Tests the unsupported @noextend tag on interface methods
	 * using an incremental build
	 */
	public void test1I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test1", true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on interface methods
	 * using a full build
	 */
	public void test1F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test1", true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on outer interface methods
	 * using an incremental build
	 */
	public void test2I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test2", true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on outer interface methods
	 * using a full build
	 */
	public void test2F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test2", true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on inner interface methods
	 * using an incremental build
	 */
	public void test3I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test3", true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on inner interface methods
	 * using a full build
	 */
	public void test3F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test3", true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on a variety of inner / outer interface methods
	 * using an incremental build
	 */
	public void test4I() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test4", true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on a variety of inner / outer interface methods
	 * using a full build
	 */
	public void test4F() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test4", true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on interface methods in the default package
	 * using an incremental build
	 */
	public void test5I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest("", "test5", true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on interface methods in the default package
	 * using a full build
	 */
	public void test5F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest("", "test5", true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on interface methods
	 * using an incremental build
	 */
	public void test6I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test6", true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on interface methods
	 * using a full build
	 */
	public void test6F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test6", true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on outer interface methods
	 * using an incremental build
	 */
	public void test7I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test7", true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on on outer interface methods
	 * using a full build
	 */
	public void test7F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test7", true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on inner interface methods
	 * using an incremental build
	 */
	public void test8I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test8", true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on inner interface methods
	 * using a full build
	 */
	public void test8F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test8", true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on a variety of inner / outer interface methods
	 * using an incremental build
	 */
	public void test9I() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test9", true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on a variety of inner / outer interface methods
	 * using a full build
	 */
	public void test9F() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test9", true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on interface methods in the default package
	 * using an incremental build
	 */
	public void test10I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest("", "test10", true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on interface methods in the default package
	 * using a full build
	 */
	public void test10F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest("", "test10", true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on interface methods
	 * using an incremental build
	 */
	public void test11I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test11", true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on interface methods
	 * using a full build
	 */
	public void test11F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test11", true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on outer interface methods
	 * using an incremental build
	 */
	public void test12I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test12", true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on outer interface methods
	 * using a full build
	 */
	public void test12F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test12", true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on inner interface methods
	 * using an incremental build
	 */
	public void test13I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test13", true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on inner interface methods
	 * using a full build
	 */
	public void test13F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test13", true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on a variety of inner / outer interface methods
	 * using an incremental build
	 */
	public void test14I() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test14", true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on a variety of inner / outer interface methods
	 * using a full build
	 */
	public void test14F() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test14", true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on interface methods in the default package
	 * using an incremental build
	 */
	public void test15I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest("", "test15", true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on interface methods in the default package
	 * using a full build
	 */
	public void test15F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest("", "test15", true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on interface methods
	 * using an incremental build
	 */
	public void test16I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test16", true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on interface methods
	 * using a full build
	 */
	public void test16F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test16", true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on outer interface methods
	 * using an incremental build
	 */
	public void test17I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test17", true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on outer interface methods
	 * using a full build
	 */
	public void test17F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test17", true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on inner interface methods
	 * using an incremental build
	 */
	public void test18I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test18", true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on inner interface methods
	 * using a full build
	 */
	public void test18F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test18", true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on a variety of inner / outer interface methods
	 * using an incremental build
	 */
	public void test19I() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test19", true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on a variety of inner / outer interface methods
	 * using a full build
	 */
	public void test19F() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test19", true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on interface methods in the default package
	 * using an incremental build
	 */
	public void test20I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest("", "test20", true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on interface methods in the default package
	 * using a full build
	 */
	public void test20F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployIncrementalBuildTest("", "test20", true);
	}
	
	/**
	 * Tests all the unsupported tags on a variety of interface methods
	 * using an incremental build
	 */
	public void test21I() {
		setExpectedProblemIds(getDefaultProblemSet(24));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test21", true);
	}
	
	/**
	 * Tests all the unsupported tags on a variety of interface methods
	 * using a full build
	 */
	public void test21F() {
		setExpectedProblemIds(getDefaultProblemSet(24));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test21", true);
	}
}
