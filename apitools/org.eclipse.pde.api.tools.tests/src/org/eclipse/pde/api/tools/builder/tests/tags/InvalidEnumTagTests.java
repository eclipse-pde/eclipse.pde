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
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * Tests that the builder finds and properly reports invalid tags on enum fields
 *  
 *  @since 3.4
 */
public class InvalidEnumTagTests extends TagTest {

	/**
	 * Constructor
	 * @param name
	 */
	public InvalidEnumTagTests(String name) {
		super(name);
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(InvalidEnumTagTests.class);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("enum");
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.tags.TagTest#getDefaultProblemId()
	 */
	protected int getDefaultProblemId() {
		return ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.T_REFERENCE_TYPE, IApiProblem.UNSUPPORTED_TAG_USE, IApiProblem.NO_FLAGS);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getTestCompliance()
	 */
	protected String getTestCompliance() {
		return CompilerOptions.VERSION_1_5;
	}
	
	/**
	 * Tests having an @noreference tag on an enum in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidEnumTag1I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test1", true);
	}
	
	/**
	 * Tests having an @noreference tag on an enum in package a.b.c
	 * using a full build
	 */
	public void testInvalidEnumTag1F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test1", true);
	}
	
	/**
	 * Tests having an @noreference tag on an outer enum in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidEnumTag2I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test2", true);
	}
	
	/**
	 * Tests having an @noreference tag on an outer enum in package a.b.c
	 * using a full build
	 */
	public void testInvalidEnumTag2F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test2", true);
	}
	
	/**
	 * Tests having an @noreference tag on an inner enum in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidEnumTag3I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test3", true);
	}
	
	/**
	 * Tests having an @noreference tag on an inner enum in package a.b.c
	 * using a full build
	 */
	public void testInvalidEnumTag3F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test3", true);
	}
	
	/**
	 * Tests having an @noreference tag on a variety of inner / outer / top-level enums in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidEnumTag4I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test4", true);
	}
	
	/**
	 * Tests having an @noreference tag on a variety of inner / outer / top-level enums in package a.b.c
	 * using a full build
	 */
	public void testInvalidEnumTag4F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployFullBuildTest(TESTING_PACKAGE, "test4", true);
	}
	
	/**
	 * Tests having an @noreference tag on an enum in the default package
	 * using an incremental build
	 */
	public void testInvalidEnumTag5I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest("", "test5", true);
	}
	
	/**
	 * Tests having an @noreference tag on an enum in the default package
	 * using a full build
	 */
	public void testInvalidEnumTag5F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest("", "test5", true);
	}
	
	/**
	 * Tests having an @noextend tag on an enum in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidEnumTag6I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test6", true);
	}
	
	/**
	 * Tests having an @noextend tag on an enum in package a.b.c
	 * using a full build
	 */
	public void testInvalidEnumTag6F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test6", true);
	}

	/**
	 * Tests having an @noextend tag on an outer enum in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidEnumTag7I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test7", true);
	}
	
	/**
	 * Tests having an @noextend tag on an outer enum in package a.b.c
	 * using a full build
	 */
	public void testInvalidEnumTag7F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test7", true);
	}
	
	/**
	 * Tests having an @noextend tag on an inner enum in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidEnumTag8I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test8", true);
	}
	
	/**
	 * Tests having an @noextend tag on an inner enum in package a.b.c
	 * using a full build
	 */
	public void testInvalidEnumTag8F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test8", true);
	}
	
	/**
	 * Tests having an @noextend tag on a variety of inner / outer / top-level enums in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidEnumTag9I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test9", true);
	}
	
	/**
	 * Tests having an @noextend tag on a variety of inner / outer / top-level enums in package a.b.c
	 * using a full build
	 */
	public void testInvalidEnumTag9F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployFullBuildTest(TESTING_PACKAGE, "test9", true);
	}
	
	/**
	 * Tests having an @noextend tag on an enum in the default package
	 * using an incremental build
	 */
	public void testInvalidEnumTag10I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test10", true);
	}
	
	/**
	 * Tests having an @noextend tag on an enum in the default package
	 * using a full build
	 */
	public void testInvalidEnumTag10F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test10", true);
	}
	
	/**
	 * Tests having an @nooverride tag on an enum in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidEnumTag11I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test11", true);
	}
	
	/**
	 * Tests having an @nooverride tag on an enum in package a.b.c
	 * using a full build
	 */
	public void testInvalidEnumTag11F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test11", true);
	}
	
	/**
	 * Tests having an @nooverride tag on an outer enum in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidEnumTag12I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test12", true);
	}
	
	/**
	 * Tests having an @nooverride tag on an outer enum in package a.b.c
	 * using a full build
	 */
	public void testInvalidEnumTag12F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test12", true);
	}
	
	/**
	 * Tests having an @nooverride tag on an inner enum in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidEnumTag13I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test13", true);
	}
	
	/**
	 * Tests having an @nooverride tag on an inner enum in package a.b.c
	 * using a full build
	 */
	public void testInvalidEnumTag13F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test13", true);
	}
	
	/**
	 * Tests having an @nooverride tag on a variety of inner / outer / top-level enums in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidEnumTag14I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test14", true);
	}
	
	/**
	 * Tests having an @nooverride tag on a variety of inner / outer / top-level enums in package a.b.c
	 * using a full build
	 */
	public void testInvalidEnumTag14F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployFullBuildTest(TESTING_PACKAGE, "test14", true);
	}
	
	/**
	 * Tests having an @nooverride tag on an enum in the default package
	 * using an incremental build
	 */
	public void testInvalidEnumTag15I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test15", true);
	}
	
	/**
	 * Tests having an @nooverride tag on an enum in the default package
	 * using a full build
	 */
	public void testInvalidEnumTag15F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test15", true);
	}
	
	/**
	 * Tests having more than one invalid tag on an enum in the testing package a.b.c
	 * using an incremental build
	 */
	public void testInvalidEnumTag16I() {
		setExpectedProblemIds(getDefaultProblemSet(5));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test16", true);
	}
	
	/**
	 * Tests having more than one invalid tag on an enum in the testing package a.b.c
	 * using a full build
	 */
	public void testInvalidEnumTag16F() {
		setExpectedProblemIds(getDefaultProblemSet(5));
		deployFullBuildTest(TESTING_PACKAGE, "test16", true);
	}
	
	/**
	 * Tests having more than one invalid tag on an outer enum in the testing package a.b.c
	 * using an incremental build
	 */
	public void testInvalidEnumTag17I() {
		setExpectedProblemIds(getDefaultProblemSet(5));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test17", true);
	}
	
	/**
	 * Tests having more than one invalid tag on an outer enum in the testing package a.b.c
	 * using a full build
	 */
	public void testInvalidEnumTag17F() {
		setExpectedProblemIds(getDefaultProblemSet(5));
		deployFullBuildTest(TESTING_PACKAGE, "test17", true);
	}
	
	/**
	 * Tests having more than one invalid tag on an inner enum in the testing package a.b.c
	 * using an incremental build
	 */
	public void testInvalidEnumTag18I() {
		setExpectedProblemIds(getDefaultProblemSet(5));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test18", true);
	}
	
	/**
	 * Tests having more than one invalid tag on an inner enum in the testing package a.b.c
	 * using a full build
	 */
	public void testInvalidEnumTag18F() {
		setExpectedProblemIds(getDefaultProblemSet(5));
		deployFullBuildTest(TESTING_PACKAGE, "test18", true);
	}
	
	/**
	 * Tests having a variety of invalid tags on a variety of inner / outer / top-level enums in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidEnumTag19I() {
		setExpectedProblemIds(getDefaultProblemSet(20));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test19", true);
	}
	
	/**
	 * Tests having a variety of invalid tags on a variety of inner / outer / top-level enums in package a.b.c
	 * using a full build
	 */
	public void testInvalidEnumTag19F() {
		setExpectedProblemIds(getDefaultProblemSet(20));
		deployFullBuildTest(TESTING_PACKAGE, "test19", true);
	}
	
	/**
	 * Tests having more than one invalid tag on an enum in the default package
	 * using an incremental build
	 */
	public void testInvalidEnumTag20I() {
		setExpectedProblemIds(getDefaultProblemSet(5));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test20", true);
	}
	
	/**
	 * Tests having more than one invalid tag on an enum in the default package
	 * using a full build
	 */
	public void testInvalidEnumTag20F() {
		setExpectedProblemIds(getDefaultProblemSet(5));
		deployFullBuildTest(TESTING_PACKAGE, "test20", true);
	}
	
	/**
	 * Tests having an @noinstantiate tag on an enum in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidEnumTag21I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test21", true);
	}
	
	/**
	 * Tests having an @noinstantiate tag on an enum in package a.b.c
	 * using a full build
	 */
	public void testInvalidEnumTag21F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test21", true);
	}
	
	/**
	 * Tests having an @noinstantiate tag on an outer enum in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidEnumTag22I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test22", true);
	}
	
	/**
	 * Tests having an @noinstantiate tag on an outer enum in package a.b.c
	 * using a full build
	 */
	public void testInvalidEnumTag22F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test22", true);
	}
	
	/**
	 * Tests having an @noinstantiate tag on an inner enum in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidEnumTag23I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test23", true);
	}
	
	/**
	 * Tests having an @noinstantiate tag on an inner enum in package a.b.c
	 * using a full build
	 */
	public void testInvalidEnumTag23F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test23", true);
	}
	
	/**
	 * Tests having an @noinstantiate tag on a variety of inner / outer / top-level enums in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidEnumTag24I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test24", true);
	}
	
	/**
	 * Tests having an @noinstantiate tag on a variety of inner / outer / top-level enums in package a.b.c
	 * using a full build
	 */
	public void testInvalidEnumTag24F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployFullBuildTest(TESTING_PACKAGE, "test24", true);
	}
	
	/**
	 * Tests having an @noinstantiate tag on an enum in the default package
	 * using an incremental build
	 */
	public void testInvalidEnumTag25I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest("", "test25", true);
	}
	
	/**
	 * Tests having an @noinstantiate tag on an enum in the default package
	 * using a full build
	 */
	public void testInvalidEnumTag25F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest("", "test25", true);
	}
	
	/**
	 * Tests having an @noimplement tag on an enum in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidEnumTag26I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test26", true);
	}
	
	/**
	 * Tests having an @noimplement tag on an enum in package a.b.c
	 * using a full build
	 */
	public void testInvalidEnumTag26F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test26", true);
	}
	
	/**
	 * Tests having an @noimplement tag on an outer enum in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidEnumTag27I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test27", true);
	}
	
	/**
	 * Tests having an @noimplement tag on an outer enum in package a.b.c
	 * using a full build
	 */
	public void testInvalidEnumTag27F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test27", true);
	}
	
	/**
	 * Tests having an @noimplement tag on an inner enum in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidEnumTag28I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test28", true);
	}
	
	/**
	 * Tests having an @noimplement tag on an inner enum in package a.b.c
	 * using a full build
	 */
	public void testInvalidEnumTag28F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test28", true);
	}
	
	/**
	 * Tests having an @noimplement tag on a variety of inner / outer / top-level enums in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidEnumTag29I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test29", true);
	}
	
	/**
	 * Tests having an @noimplement tag on a variety of inner / outer / top-level enums in package a.b.c
	 * using a full build
	 */
	public void testInvalidEnumTag29F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployFullBuildTest(TESTING_PACKAGE, "test29", true);
	}
	
	/**
	 * Tests having an @noimplement tag on an enum in the default package
	 * using an incremental build
	 */
	public void testInvalidEnumTag30I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest("", "test30", true);
	}
	
	/**
	 * Tests having an @noimplement tag on an enum in the default package
	 * using a full build
	 */
	public void testInvalidEnumTag30F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest("", "test30", true);
	}
}
