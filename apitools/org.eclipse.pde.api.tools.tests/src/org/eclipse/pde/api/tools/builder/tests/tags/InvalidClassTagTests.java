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
	public void test1I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test1", true);
	}
	
	/**
	 * Tests having an @noreference tag on a class in package a.b.c
	 * using a full build
	 */
	public void test1F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test1", true);
	}
	
	/**
	 * Tests having an @noreference tag on an outer type class in package a.b.c
	 * using an incremental build
	 */
	public void test2I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test2", true);
	}
	
	/**
	 * Tests having an @noreference tag on an outer type class in package a.b.c
	 * using a full build
	 */
	public void test2F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test2", true);
	}
	
	/**
	 * Tests having an @noreference tag on an inner type class in package a.b.c
	 * using an incremental build
	 */
	public void test3I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test3", true);
	}
	
	/**
	 * Tests having an @noreference tag on an inner type class in package a.b.c
	 * using a full build
	 */
	public void test3F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test3", true);
	}
	
	/**
	 * Tests having an @noreference tag on a variety of inner / outer / top-level classes in package a.b.c
	 * using an incremental build
	 */
	public void test4I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test4", true);
	}
	
	/**
	 * Tests having an @noreference tag on a variety of inner / outer / top-level classes in package a.b.c
	 * using a full build
	 */
	public void test4F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployFullBuildTest(TESTING_PACKAGE, "test4", true);
	}
	
	/**
	 * Tests having an @noreference tag on a class in the default package
	 * using an incremental build
	 */
	public void test5I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest("", "test5", true);
	}
	
	/**
	 * Tests having an @noreference tag on a class in the default package
	 * using a full build
	 */
	public void test5F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest("", "test5", true);
	}
	
	/**
	 * Tests having an @noimplement tag on a class in package a.b.c
	 * using an incremental build
	 */
	public void test6I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test6", true);
	}
	
	/**
	 * Tests having an @noimplement tag on a class in package a.b.c
	 * using a full build
	 */
	public void test6F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test6", true);
	}

	/**
	 * Tests having an @noimplement tag on an outer class in package a.b.c
	 * using an incremental build
	 */
	public void test7I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test7", true);
	}
	
	/**
	 * Tests having an @noimplement tag on an outer class in package a.b.c
	 * using a full build
	 */
	public void test7F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test7", true);
	}
	
	/**
	 * Tests having an @noimplement tag on an inner class in package a.b.c
	 * using an incremental build
	 */
	public void test8I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test8", true);
	}
	
	/**
	 * Tests having an @noimplement tag on an inner class in package a.b.c
	 * using a full build
	 */
	public void test8F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test8", true);
	}
	
	/**
	 * Tests having an @noimplement tag on a variety of inner / outer / top-level classes in package a.b.c
	 * using an incremental build
	 */
	public void test9I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test9", true);
	}
	
	/**
	 * Tests having an @noimplement tag on a variety of inner / outer / top-level classes in package a.b.c
	 * using a full build
	 */
	public void test9F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployFullBuildTest(TESTING_PACKAGE, "test9", true);
	}
	
	/**
	 * Tests having an @noimplement tag on a class in the default package
	 * using an incremental build
	 */
	public void test10I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test10", true);
	}
	
	/**
	 * Tests having an @noimplement tag on a class in the default package
	 * using a full build
	 */
	public void test10F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test10", true);
	}
	
	/**
	 * Tests having an @nooverride tag on a class in package a.b.c
	 * using an incremental build
	 */
	public void test11I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test11", true);
	}
	
	/**
	 * Tests having an @nooverride tag on a class in package a.b.c
	 * using a full build
	 */
	public void test11F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test11", true);
	}
	
	/**
	 * Tests having an @nooverride tag on an outer class in package a.b.c
	 * using an incremental build
	 */
	public void test12I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test12", true);
	}
	
	/**
	 * Tests having an @nooverride tag on an outer class in package a.b.c
	 * using a full build
	 */
	public void test12F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test12", true);
	}
	
	/**
	 * Tests having an @nooverride tag on an inner class in package a.b.c
	 * using an incremental build
	 */
	public void test13I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test13", true);
	}
	
	/**
	 * Tests having an @nooverride tag on an inner class in package a.b.c
	 * using a full build
	 */
	public void test13F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test13", true);
	}
	
	/**
	 * Tests having an @nooverride tag on a variety of inner / outer / top-level classes in package a.b.c
	 * using an incremental build
	 */
	public void test14I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test14", true);
	}
	
	/**
	 * Tests having an @nooverride tag on a variety of inner / outer / top-level classes in package a.b.c
	 * using a full build
	 */
	public void test14F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployFullBuildTest(TESTING_PACKAGE, "test14", true);
	}
	
	/**
	 * Tests having an @nooverride tag on a class in the default package
	 * using an incremental build
	 */
	public void test15I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test15", true);
	}
	
	/**
	 * Tests having an @nooverride tag on a class in the default package
	 * using a full build
	 */
	public void test15F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test15", true);
	}
	
	/**
	 * Tests having more than one invalid tag on a class in the testing package a.b.c
	 * using an incremental build
	 */
	public void test16I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test16", true);
	}
	
	/**
	 * Tests having more than one invalid tag on a class in the testing package a.b.c
	 * using a full build
	 */
	public void test16F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test16", true);
	}
	
	/**
	 * Tests having more than one invalid tag on an outer class in the testing package a.b.c
	 * using an incremental build
	 */
	public void test17I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test17", true);
	}
	
	/**
	 * Tests having more than one invalid tag on an outer class in the testing package a.b.c
	 * using a full build
	 */
	public void test17F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test17", true);
	}
	
	/**
	 * Tests having more than one invalid tag on an inner class in the testing package a.b.c
	 * using an incremental build
	 */
	public void test18I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test18", true);
	}
	
	/**
	 * Tests having more than one invalid tag on an inner class in the testing package a.b.c
	 * using a full build
	 */
	public void test18F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test18", true);
	}
	
	/**
	 * Tests having a variety of invalid tags on a variety of inner / outer / top-level classes in package a.b.c
	 * using an incremental build
	 */
	public void test19I() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test19", true);
	}
	
	/**
	 * Tests having a variety of invalid tags on a variety of inner / outer / top-level classes in package a.b.c
	 * using a full build
	 */
	public void test19F() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployFullBuildTest(TESTING_PACKAGE, "test19", true);
	}
	
	/**
	 * Tests having more than one invalid tag on a class in the default package
	 * using an incremental build
	 */
	public void test20I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest("", "test20", true);
	}
	
	/**
	 * Tests having more than one invalid tag on a class in the default package
	 * using a full build
	 */
	public void test20F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest("", "test20", true);
	}
}
