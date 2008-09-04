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
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.pde.api.tools.builder.tests.ApiProblem;

/**
 * Tests invalid tags on annotation methods.
 * 
 * @since 1.0
 */
public class InvalidAnnotationMethodTagTests extends InvalidMethodTagTests {

	/**
	 * Constructor
	 * @param name
	 */
	public InvalidAnnotationMethodTagTests(String name) {
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
		return super.getTestSourcePath().append("annotation");
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#assertProblems(org.eclipse.pde.api.tools.builder.tests.ApiProblem[])
	 */
	protected void assertProblems(ApiProblem[] problems) {
		String message = null;
		for(int i = 0; i < problems.length; i++) {
			message = problems[i].getMessage();
			assertTrue("the message does not end correctly: "+message, message.endsWith("an annotation method"));
		}
	}
	
	/**
	 * @return the test for this class
	 */
	public static Test suite() {
		return buildTestSuite(InvalidAnnotationMethodTagTests.class);
	}
	
	/**
	 * Tests the unsupported @noextend tag on annotation methods
	 * using an incremental build
	 */
	public void testInvalidAnnotationMethodTag1I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test1", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on annotation methods
	 * using a full build
	 */
	public void testInvalidAnnotationMethodTag1F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test1", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on outer annotation methods
	 * using an incremental build
	 */
	public void testInvalidAnnotationMethodTag2I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test2", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on outer annotation methods
	 * using a full build
	 */
	public void testInvalidAnnotationMethodTag2F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test2", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on inner annotation methods
	 * using an incremental build
	 */
	public void testInvalidAnnotationMethodTag3I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test3", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on inner annotation methods
	 * using a full build
	 */
	public void testInvalidAnnotationMethodTag3F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test3", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on a variety of inner / outer annotation methods
	 * using an incremental build
	 */
	public void testInvalidAnnotationMethodTag4I() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployTagTest(TESTING_PACKAGE, "test4", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on a variety of inner / outer annotation methods
	 * using a full build
	 */
	public void testInvalidAnnotationMethodTag4F() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployTagTest(TESTING_PACKAGE, "test4", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on annotation methods in the default package
	 * using an incremental build
	 */
	public void testInvalidAnnotationMethodTag5I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest("", "test5", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on annotation methods in the default package
	 * using a full build
	 */
	public void testInvalidAnnotationMethodTag5F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest("", "test5", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on annotation methods
	 * using an incremental build
	 */
	public void testInvalidAnnotationMethodTag6I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test6", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on annotation methods
	 * using a full build
	 */
	public void testInvalidAnnotationMethodTag6F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test6", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on outer annotation methods
	 * using an incremental build
	 */
	public void testInvalidAnnotationMethodTag7I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test7", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on on outer annotation methods
	 * using a full build
	 */
	public void testInvalidAnnotationMethodTag7F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test7", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on inner annotation methods
	 * using an incremental build
	 */
	public void testInvalidAnnotationMethodTag8I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test8", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on inner annotation methods
	 * using a full build
	 */
	public void testInvalidAnnotationMethodTag8F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test8", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on a variety of inner / outer annotation methods
	 * using an incremental build
	 */
	public void testInvalidAnnotationMethodTag9I() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployTagTest(TESTING_PACKAGE, "test9", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on a variety of inner / outer annotation methods
	 * using a full build
	 */
	public void testInvalidAnnotationMethodTag9F() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployTagTest(TESTING_PACKAGE, "test9", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on annotation methods in the default package
	 * using an incremental build
	 */
	public void testInvalidAnnotationMethodTag10I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest("", "test10", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on annotation methods in the default package
	 * using a full build
	 */
	public void testInvalidAnnotationMethodTag10F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest("", "test10", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on annotation methods
	 * using an incremental build
	 */
	public void testInvalidAnnotationMethodTag11I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test11", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on annotation methods
	 * using a full build
	 */
	public void testInvalidAnnotationMethodTag11F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test11", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on outer annotation methods
	 * using an incremental build
	 */
	public void testInvalidAnnotationMethodTag12I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test12", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on outer annotation methods
	 * using a full build
	 */
	public void testInvalidAnnotationMethodTag12F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test12", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on inner annotation methods
	 * using an incremental build
	 */
	public void testInvalidAnnotationMethodTag13I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test13", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on inner annotation methods
	 * using a full build
	 */
	public void testInvalidAnnotationMethodTag13F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test13", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on a variety of inner / outer annotation methods
	 * using an incremental build
	 */
	public void testInvalidAnnotationMethodTag14I() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployTagTest(TESTING_PACKAGE, "test14", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on a variety of inner / outer annotation methods
	 * using a full build
	 */
	public void testInvalidAnnotationMethodTag14F() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployTagTest(TESTING_PACKAGE, "test14", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on annotation methods in the default package
	 * using an incremental build
	 */
	public void testInvalidAnnotationMethodTag15I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest("", "test15", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on annotation methods in the default package
	 * using a full build
	 */
	public void testInvalidAnnotationMethodTag15F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest("", "test15", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on annotation methods
	 * using an incremental build
	 */
	public void testInvalidAnnotationMethodTag16I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test16", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on annotation methods
	 * using a full build
	 */
	public void testInvalidAnnotationMethodTag16F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test16", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on outer annotation methods
	 * using an incremental build
	 */
	public void testInvalidAnnotationMethodTag17I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test17", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on outer annotation methods
	 * using a full build
	 */
	public void testInvalidAnnotationMethodTag17F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test17", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on inner annotation methods
	 * using an incremental build
	 */
	public void testInvalidAnnotationMethodTag18I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test18", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on inner annotation methods
	 * using a full build
	 */
	public void testInvalidAnnotationMethodTag18F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test18", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on a variety of inner / outer annotation methods
	 * using an incremental build
	 */
	public void testInvalidAnnotationMethodTag19I() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployTagTest(TESTING_PACKAGE, "test19", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on a variety of inner / outer annotation methods
	 * using a full build
	 */
	public void testInvalidAnnotationMethodTag19F() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployTagTest(TESTING_PACKAGE, "test19", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on annotation methods in the default package
	 * using an incremental build
	 */
	public void testInvalidAnnotationMethodTag20I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest("", "test20", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on annotation methods in the default package
	 * using a full build
	 */
	public void testInvalidAnnotationMethodTag20F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest("", "test20", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests all the unsupported tags on a variety of annotation methods
	 * using an incremental build
	 */
	public void testInvalidAnnotationMethodTag21I() {
		setExpectedProblemIds(getDefaultProblemSet(30));
		deployTagTest(TESTING_PACKAGE, "test21", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests all the unsupported tags on a variety of annotation methods
	 * using a full build
	 */
	public void testInvalidAnnotationMethodTag21F() {
		setExpectedProblemIds(getDefaultProblemSet(30));
		deployTagTest(TESTING_PACKAGE, "test21", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noreference tag on annotation methods
	 * using an incremental build
	 */
	public void testInvalidAnnotationMethodTag22I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test22", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noreference tag on annotation methods
	 * using a full build
	 */
	public void testInvalidAnnotationMethodTag22F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test22", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noreference tag on outer annotation methods
	 * using an incremental build
	 */
	public void testInvalidAnnotationMethodTag23I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test23", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noreference tag on outer annotation methods
	 * using a full build
	 */
	public void testInvalidAnnotationMethodTag23F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test23", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noreference tag on annotation methods
	 * using an incremental build
	 */
	public void testInvalidAnnotationMethodTag24I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test24", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noreference tag on annotation methods
	 * using a full build
	 */
	public void testInvalidAnnotationMethodTag24F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest(TESTING_PACKAGE, "test24", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noreference tag on a variety of inner / outer annotation methods
	 * using an incremental build
	 */
	public void testInvalidAnnotationMethodTag25I() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployTagTest(TESTING_PACKAGE, "test25", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noreference tag on a variety of inner / outer annotation methods
	 * using a full build
	 */
	public void testInvalidAnnotationMethodTag25F() {
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployTagTest(TESTING_PACKAGE, "test25", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noreference tag on annotation methods in the default package
	 * using an incremental build
	 */
	public void testInvalidAnnotationMethodTag26I() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest("", "test26", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported @noreference tag on annotation methods in the default package
	 * using a full build
	 */
	public void testInvalidAnnotationMethodTag26F() {
		setExpectedProblemIds(getDefaultProblemSet(2));
		deployTagTest("", "test26", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported tags on an annotation method with a default value
	 * using an incremental build
	 */
	public void testInvalidAnnotationMethodTag27I() {
		setExpectedProblemIds(getDefaultProblemSet(15));
		deployTagTest("", "test27", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported tags on an annotation method with a default value
	 * using a full build
	 */
	public void testInvalidAnnotationMethodTag27F() {
		setExpectedProblemIds(getDefaultProblemSet(15));
		deployTagTest("", "test27", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported tags on an annotation method in the default package
	 * using an incremental build
	 */
	public void testInvalidAnnotationMethodTag28I() {
		setExpectedProblemIds(getDefaultProblemSet(15));
		deployTagTest("", "test28", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests the unsupported tags on an annotation method in the default package
	 * using a full build
	 */
	public void testInvalidAnnotationMethodTag28F() {
		setExpectedProblemIds(getDefaultProblemSet(15));
		deployTagTest("", "test28", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
}
