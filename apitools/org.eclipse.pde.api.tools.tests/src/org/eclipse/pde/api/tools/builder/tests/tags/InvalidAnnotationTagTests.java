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
 * 
 */
public class InvalidAnnotationTagTests extends TagTest {

	/**
	 * Constructor
	 * @param name
	 */
	public InvalidAnnotationTagTests(String name) {
		super(name);
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(InvalidAnnotationTagTests.class);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("annotation");
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
	 * Tests having an @noreference tag on an annotation in package a.b.c
	 * using an incremental build
	 */
	public void test1I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test1", true);
	}
	
	/**
	 * Tests having an @noreference tag on an annotation in package a.b.c
	 * using a full build
	 */
	public void test1F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test1", true);
	}
	
	/**
	 * Tests having an @noreference tag on an outer annotation in package a.b.c
	 * using an incremental build
	 */
	public void test2I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test2", true);
	}
	
	/**
	 * Tests having an @noreference tag on an outer annotation in package a.b.c
	 * using a full build
	 */
	public void test2F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test2", true);
	}
	
	/**
	 * Tests having an @noreference tag on an inner annotation in package a.b.c
	 * using an incremental build
	 */
	public void test3I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test3", true);
	}
	
	/**
	 * Tests having an @noreference tag on an inner annotation in package a.b.c
	 * using a full build
	 */
	public void test3F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test3", true);
	}
	
	/**
	 * Tests having an @noreference tag on a variety of inner / outer / top-level annotations in package a.b.c
	 * using an incremental build
	 */
	public void test4I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test4", true);
	}
	
	/**
	 * Tests having an @noreference tag on a variety of inner / outer / top-level annotations in package a.b.c
	 * using a full build
	 */
	public void test4F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployFullBuildTest(TESTING_PACKAGE, "test4", true);
	}
	
	/**
	 * Tests having an @noreference tag on an annotation in the default package
	 * using an incremental build
	 */
	public void test5I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest("", "test5", true);
	}
	
	/**
	 * Tests having an @noreference tag on an annotation in the default package
	 * using a full build
	 */
	public void test5F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest("", "test5", true);
	}
	
	/**
	 * Tests having an @noextend tag on an annotation in package a.b.c
	 * using an incremental build
	 */
	public void test6I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test6", true);
	}
	
	/**
	 * Tests having an @noextend tag on an annotation in package a.b.c
	 * using a full build
	 */
	public void test6F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test6", true);
	}

	/**
	 * Tests having an @noextend tag on an outer annotation in package a.b.c
	 * using an incremental build
	 */
	public void test7I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test7", true);
	}
	
	/**
	 * Tests having an @noextend tag on an outer annotation in package a.b.c
	 * using a full build
	 */
	public void test7F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test7", true);
	}
	
	/**
	 * Tests having an @noextend tag on an inner annotation in package a.b.c
	 * using an incremental build
	 */
	public void test8I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test8", true);
	}
	
	/**
	 * Tests having an @noextend tag on an inner annotation in package a.b.c
	 * using a full build
	 */
	public void test8F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test8", true);
	}
	
	/**
	 * Tests having an @noextend tag on a variety of inner / outer / top-level annotations in package a.b.c
	 * using an incremental build
	 */
	public void test9I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test9", true);
	}
	
	/**
	 * Tests having an @noextend tag on a variety of inner / outer / top-level annotations in package a.b.c
	 * using a full build
	 */
	public void test9F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployFullBuildTest(TESTING_PACKAGE, "test9", true);
	}
	
	/**
	 * Tests having an @noextend tag on an annotation in the default package
	 * using an incremental build
	 */
	public void test10I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test10", true);
	}
	
	/**
	 * Tests having an @noextend tag on an annotation in the default package
	 * using a full build
	 */
	public void test10F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test10", true);
	}
	
	/**
	 * Tests having an @nooverride tag on an annotation in package a.b.c
	 * using an incremental build
	 */
	public void test11I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test11", true);
	}
	
	/**
	 * Tests having an @nooverride tag on an annotation in package a.b.c
	 * using a full build
	 */
	public void test11F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test11", true);
	}
	
	/**
	 * Tests having an @nooverride tag on an outer annotation in package a.b.c
	 * using an incremental build
	 */
	public void test12I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test12", true);
	}
	
	/**
	 * Tests having an @nooverride tag on an outer annotation in package a.b.c
	 * using a full build
	 */
	public void test12F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test12", true);
	}
	
	/**
	 * Tests having an @nooverride tag on an inner annotation in package a.b.c
	 * using an incremental build
	 */
	public void test13I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test13", true);
	}
	
	/**
	 * Tests having an @nooverride tag on an inner annotation in package a.b.c
	 * using a full build
	 */
	public void test13F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test13", true);
	}
	
	/**
	 * Tests having an @nooverride tag on a variety of inner / outer / top-level annotations in package a.b.c
	 * using an incremental build
	 */
	public void test14I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test14", true);
	}
	
	/**
	 * Tests having an @nooverride tag on a variety of inner / outer / top-level annotations in package a.b.c
	 * using a full build
	 */
	public void test14F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployFullBuildTest(TESTING_PACKAGE, "test14", true);
	}
	
	/**
	 * Tests having an @nooverride tag on an annotation in the default package
	 * using an incremental build
	 */
	public void test15I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test15", true);
	}
	
	/**
	 * Tests having an @nooverride tag on an annotation in the default package
	 * using a full build
	 */
	public void test15F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test15", true);
	}
	
	/**
	 * Tests having more than one invalid tag on an annotation in the testing package a.b.c
	 * using an incremental build
	 */
	public void test16I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test16", true);
	}
	
	/**
	 * Tests having more than one invalid tag on an annotation in the testing package a.b.c
	 * using a full build
	 */
	public void test16F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployFullBuildTest(TESTING_PACKAGE, "test16", true);
	}
	
	/**
	 * Tests having more than one invalid tag on an outer annotation in the testing package a.b.c
	 * using an incremental build
	 */
	public void test17I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test17", true);
	}
	
	/**
	 * Tests having more than one invalid tag on an outer annotation in the testing package a.b.c
	 * using a full build
	 */
	public void test17F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployFullBuildTest(TESTING_PACKAGE, "test17", true);
	}
	
	/**
	 * Tests having more than one invalid tag on an inner annotation in the testing package a.b.c
	 * using an incremental build
	 */
	public void test18I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test18", true);
	}
	
	/**
	 * Tests having more than one invalid tag on an inner annotation in the testing package a.b.c
	 * using a full build
	 */
	public void test18F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployFullBuildTest(TESTING_PACKAGE, "test18", true);
	}
	
	/**
	 * Tests having a variety of invalid tags on a variety of inner / outer / top-level annotations in package a.b.c
	 * using an incremental build
	 */
	public void test19I() {
		setExpectedProblemIds(getDefaultProblemSet(16));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test19", true);
	}
	
	/**
	 * Tests having a variety of invalid tags on a variety of inner / outer / top-level annotations in package a.b.c
	 * using a full build
	 */
	public void test19F() {
		setExpectedProblemIds(getDefaultProblemSet(16));
		deployFullBuildTest(TESTING_PACKAGE, "test19", true);
	}
	
	/**
	 * Tests having more than one invalid tag on an annotation in the default package
	 * using an incremental build
	 */
	public void test20I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test20", true);
	}
	
	/**
	 * Tests having more than one invalid tag on an annotation in the default package
	 * using a full build
	 */
	public void test20F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployFullBuildTest(TESTING_PACKAGE, "test20", true);
	}
	
	/**
	 * Tests having an @noinstantiate tag on an annotation in package a.b.c
	 * using an incremental build
	 */
	public void test21I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test21", true);
	}
	
	/**
	 * Tests having an @noinstantiate tag on an annotation in package a.b.c
	 * using a full build
	 */
	public void test21F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test21", true);
	}
	
	/**
	 * Tests having an @noinstantiate tag on an outer annotation in package a.b.c
	 * using an incremental build
	 */
	public void test22I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test22", true);
	}
	
	/**
	 * Tests having an @noinstantiate tag on an outer annotation in package a.b.c
	 * using a full build
	 */
	public void test22F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test22", true);
	}
	
	/**
	 * Tests having an @noinstantiate tag on an inner annotation in package a.b.c
	 * using an incremental build
	 */
	public void test23I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test23", true);
	}
	
	/**
	 * Tests having an @noinstantiate tag on an inner annotation in package a.b.c
	 * using a full build
	 */
	public void test23F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test23", true);
	}
	
	/**
	 * Tests having an @noinstantiate tag on a variety of inner / outer / top-level annotations in package a.b.c
	 * using an incremental build
	 */
	public void test24I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test24", true);
	}
	
	/**
	 * Tests having an @noinstantiate tag on a variety of inner / outer / top-level annotations in package a.b.c
	 * using a full build
	 */
	public void test24F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployFullBuildTest(TESTING_PACKAGE, "test24", true);
	}
	
	/**
	 * Tests having an @noinstantiate tag on an annotation in the default package
	 * using an incremental build
	 */
	public void test25I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest("", "test25", true);
	}
	
	/**
	 * Tests having an @noinstantiate tag on an annotation in the default package
	 * using a full build
	 */
	public void test25F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest("", "test25", true);
	}
	
	/**
	 * Tests having an @noimplement tag on an annotation in package a.b.c
	 * using an incremental build
	 */
	public void test26I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test26", true);
	}
	
	/**
	 * Tests having an @noimplement tag on an annotation in package a.b.c
	 * using a full build
	 */
	public void test26F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test26", true);
	}
	
	/**
	 * Tests having an @noimplement tag on an outer annotation in package a.b.c
	 * using an incremental build
	 */
	public void test27I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test27", true);
	}
	
	/**
	 * Tests having an @noimplement tag on an outer annotation in package a.b.c
	 * using a full build
	 */
	public void test27F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test27", true);
	}
	
	/**
	 * Tests having an @noimplement tag on an inner annotation in package a.b.c
	 * using an incremental build
	 */
	public void test28I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test28", true);
	}
	
	/**
	 * Tests having an @noimplement tag on an inner annotation in package a.b.c
	 * using a full build
	 */
	public void test28F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test28", true);
	}
	
	/**
	 * Tests having an @noimplement tag on a variety of inner / outer / top-level annotations in package a.b.c
	 * using an incremental build
	 */
	public void test29I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test29", true);
	}
	
	/**
	 * Tests having an @noimplement tag on a variety of inner / outer / top-level annotations in package a.b.c
	 * using a full build
	 */
	public void test29F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployFullBuildTest(TESTING_PACKAGE, "test29", true);
	}
	
	/**
	 * Tests having an @noimplement tag on an annotation in the default package
	 * using an incremental build
	 */
	public void test30I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest("", "test30", true);
	}
	
	/**
	 * Tests having an @noimplement tag on an annotation in the default package
	 * using a full build
	 */
	public void test30F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest("", "test30", true);
	}
}