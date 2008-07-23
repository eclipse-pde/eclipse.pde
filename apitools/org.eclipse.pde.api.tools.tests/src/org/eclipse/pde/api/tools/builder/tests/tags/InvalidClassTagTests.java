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
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * Tests for the builder to ensure it find and reports unsupported
 * Javadoc tags on classes properly.
 * 
 * @since 3.4
 */
public class InvalidClassTagTests extends TagTest {

	/**
	 * Constructor
	 * @param name
	 */
	public InvalidClassTagTests(String name) {
		super(name);
	}
	
	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(InvalidClassTagTests.class);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("class");
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.tags.TagTest#getDefaultProblemId()
	 */
	protected int getDefaultProblemId() {
		return ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.T_REFERENCE_TYPE, IApiProblem.UNSUPPORTED_TAG_USE, IApiProblem.NO_FLAGS);
	}
	
	/**
	 * Tests having an @noreference tag on a class in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidClassTag1I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test1", true);
	}
	
	/**
	 * Tests having an @noreference tag on a class in package a.b.c
	 * using a full build
	 */
	public void testInvalidClassTag1F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test1", true);
	}
	
	/**
	 * Tests having an @noreference tag on an outer type class in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidClassTag2I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test2", true);
	}
	
	/**
	 * Tests having an @noreference tag on an outer type class in package a.b.c
	 * using a full build
	 */
	public void testInvalidClassTag2F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test2", true);
	}
	
	/**
	 * Tests having an @noreference tag on an inner type class in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidClassTag3I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test3", true);
	}
	
	/**
	 * Tests having an @noreference tag on an inner type class in package a.b.c
	 * using a full build
	 */
	public void testInvalidClassTag3F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test3", true);
	}
	
	/**
	 * Tests having an @noreference tag on a variety of inner / outer / top-level classes in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidClassTag4I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test4", true);
	}
	
	/**
	 * Tests having an @noreference tag on a variety of inner / outer / top-level classes in package a.b.c
	 * using a full build
	 */
	public void testInvalidClassTag4F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployFullBuildTest(TESTING_PACKAGE, "test4", true);
	}
	
	/**
	 * Tests having an @noreference tag on a class in the default package
	 * using an incremental build
	 */
	public void testInvalidClassTag5I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest("", "test5", true);
	}
	
	/**
	 * Tests having an @noreference tag on a class in the default package
	 * using a full build
	 */
	public void testInvalidClassTag5F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest("", "test5", true);
	}
	
	/**
	 * Tests having an @noimplement tag on a class in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidClassTag6I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test6", true);
	}
	
	/**
	 * Tests having an @noimplement tag on a class in package a.b.c
	 * using a full build
	 */
	public void testInvalidClassTag6F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test6", true);
	}

	/**
	 * Tests having an @noimplement tag on an outer class in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidClassTag7I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test7", true);
	}
	
	/**
	 * Tests having an @noimplement tag on an outer class in package a.b.c
	 * using a full build
	 */
	public void testInvalidClassTag7F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test7", true);
	}
	
	/**
	 * Tests having an @noimplement tag on an inner class in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidClassTag8I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test8", true);
	}
	
	/**
	 * Tests having an @noimplement tag on an inner class in package a.b.c
	 * using a full build
	 */
	public void testInvalidClassTag8F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test8", true);
	}
	
	/**
	 * Tests having an @noimplement tag on a variety of inner / outer / top-level classes in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidClassTag9I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test9", true);
	}
	
	/**
	 * Tests having an @noimplement tag on a variety of inner / outer / top-level classes in package a.b.c
	 * using a full build
	 */
	public void testInvalidClassTag9F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployFullBuildTest(TESTING_PACKAGE, "test9", true);
	}
	
	/**
	 * Tests having an @noimplement tag on a class in the default package
	 * using an incremental build
	 */
	public void testInvalidClassTag10I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test10", true);
	}
	
	/**
	 * Tests having an @noimplement tag on a class in the default package
	 * using a full build
	 */
	public void testInvalidClassTag10F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test10", true);
	}
	
	/**
	 * Tests having an @nooverride tag on a class in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidClassTag11I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test11", true);
	}
	
	/**
	 * Tests having an @nooverride tag on a class in package a.b.c
	 * using a full build
	 */
	public void testInvalidClassTag11F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test11", true);
	}
	
	/**
	 * Tests having an @nooverride tag on an outer class in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidClassTag12I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test12", true);
	}
	
	/**
	 * Tests having an @nooverride tag on an outer class in package a.b.c
	 * using a full build
	 */
	public void testInvalidClassTag12F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test12", true);
	}
	
	/**
	 * Tests having an @nooverride tag on an inner class in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidClassTag13I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test13", true);
	}
	
	/**
	 * Tests having an @nooverride tag on an inner class in package a.b.c
	 * using a full build
	 */
	public void testInvalidClassTag13F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test13", true);
	}
	
	/**
	 * Tests having an @nooverride tag on a variety of inner / outer / top-level classes in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidClassTag14I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test14", true);
	}
	
	/**
	 * Tests having an @nooverride tag on a variety of inner / outer / top-level classes in package a.b.c
	 * using a full build
	 */
	public void testInvalidClassTag14F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployFullBuildTest(TESTING_PACKAGE, "test14", true);
	}
	
	/**
	 * Tests having an @nooverride tag on a class in the default package
	 * using an incremental build
	 */
	public void testInvalidClassTag15I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test15", true);
	}
	
	/**
	 * Tests having an @nooverride tag on a class in the default package
	 * using a full build
	 */
	public void testInvalidClassTag15F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test15", true);
	}
	
	/**
	 * Tests having more than one invalid tag on a class in the testing package a.b.c
	 * using an incremental build
	 */
	public void testInvalidClassTag16I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test16", true);
	}
	
	/**
	 * Tests having more than one invalid tag on a class in the testing package a.b.c
	 * using a full build
	 */
	public void testInvalidClassTag16F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test16", true);
	}
	
	/**
	 * Tests having more than one invalid tag on an outer class in the testing package a.b.c
	 * using an incremental build
	 */
	public void testInvalidClassTag17I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test17", true);
	}
	
	/**
	 * Tests having more than one invalid tag on an outer class in the testing package a.b.c
	 * using a full build
	 */
	public void testInvalidClassTag17F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test17", true);
	}
	
	/**
	 * Tests having more than one invalid tag on an inner class in the testing package a.b.c
	 * using an incremental build
	 */
	public void testInvalidClassTag18I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test18", true);
	}
	
	/**
	 * Tests having more than one invalid tag on an inner class in the testing package a.b.c
	 * using a full build
	 */
	public void testInvalidClassTag18F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test18", true);
	}
	
	/**
	 * Tests having a variety of invalid tags on a variety of inner / outer / top-level classes in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidClassTag19I() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test19", true);
	}
	
	/**
	 * Tests having a variety of invalid tags on a variety of inner / outer / top-level classes in package a.b.c
	 * using a full build
	 */
	public void testInvalidClassTag19F() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployFullBuildTest(TESTING_PACKAGE, "test19", true);
	}
	
	/**
	 * Tests having more than one invalid tag on a class in the default package
	 * using an incremental build
	 */
	public void testInvalidClassTag20I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest("", "test20", true);
	}
	
	/**
	 * Tests having more than one invalid tag on a class in the default package
	 * using a full build
	 */
	public void testInvalidClassTag20F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest("", "test20", true);
	}
}
