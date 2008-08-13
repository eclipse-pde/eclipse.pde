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
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * Tests that unsupported Javadoc tags on interfaces are reported properly
 * 
 * @since 3.4
 */
public class InvalidInterfaceTagTests extends TagTest {

	/**
	 * Constructor
	 * @param name
	 */
	public InvalidInterfaceTagTests(String name) {
		super(name);
	}
	
	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(InvalidInterfaceTagTests.class);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.tags.JavadocTagTest#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("interface");
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.tags.TagTest#getDefaultProblemId()
	 */
	protected int getDefaultProblemId() {
		return ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.T_REFERENCE_TYPE, IApiProblem.UNSUPPORTED_TAG_USE, IApiProblem.NO_FLAGS);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#assertProblems(org.eclipse.pde.api.tools.builder.tests.ApiProblem[])
	 */
	protected void assertProblems(ApiProblem[] problems) {
		String message = null;
		for(int i = 0; i < problems.length; i++) {
			message = problems[i].getMessage();
			assertTrue("The problem message is not correct: "+message, message.endsWith("an interface"));
		}
	}
	
	/**
	 * Tests having an @noreference tag on an interface in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidInterfaceTag1I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test1", true);
	}
	
	/**
	 * Tests having an @noreference tag on an interface in package a.b.c
	 * using a full build
	 */
	public void testInvalidInterfaceTag1F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTagTest(TESTING_PACKAGE, "test1", true);
	}
	
	/**
	 * Tests having an @noreference tag on an outer interface in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidInterfaceTag2I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test2", true);
	}
	
	/**
	 * Tests having an @noreference tag on an outer interface in package a.b.c
	 * using a full build
	 */
	public void testInvalidInterfaceTag2F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTagTest(TESTING_PACKAGE, "test2", true);
	}
	
	/**
	 * Tests having an @noreference tag on an inner interface in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidInterfaceTag3I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test3", true);
	}
	
	/**
	 * Tests having an @noreference tag on an inner interface in package a.b.c
	 * using a full build
	 */
	public void testInvalidInterfaceTag3F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTagTest(TESTING_PACKAGE, "test3", true);
	}
	
	/**
	 * Tests having an @noreference tag on a variety of inner / outer / top-level interfaces in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidInterfaceTag4I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test4", true);
	}
	
	/**
	 * Tests having an @noreference tag on a variety of inner / outer / top-level interfaces in package a.b.c
	 * using a full build
	 */
	public void testInvalidInterfaceTag4F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployFullBuildTagTest(TESTING_PACKAGE, "test4", true);
	}
	
	/**
	 * Tests having an @noreference tag on an interface in the default package
	 * using an incremental build
	 */
	public void testInvalidInterfaceTag5I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTagTest("", "test5", true);
	}
	
	/**
	 * Tests having an @noreference tag on an interface in the default package
	 * using a full build
	 */
	public void testInvalidInterfaceTag5F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTagTest("", "test5", true);
	}
	
	/**
	 * Tests having an @noextend tag on an interface in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidInterfaceTag6I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test6", true);
	}
	
	/**
	 * Tests having an @noextend tag on an interface in package a.b.c
	 * using a full build
	 */
	public void testInvalidInterfaceTag6F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTagTest(TESTING_PACKAGE, "test6", true);
	}

	/**
	 * Tests having an @noextend tag on an outer interface in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidInterfaceTag7I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test7", true);
	}
	
	/**
	 * Tests having an @noextend tag on an outer interface in package a.b.c
	 * using a full build
	 */
	public void testInvalidInterfaceTag7F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTagTest(TESTING_PACKAGE, "test7", true);
	}
	
	/**
	 * Tests having an @noextend tag on an inner interface in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidInterfaceTag8I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test8", true);
	}
	
	/**
	 * Tests having an @noextend tag on an inner interface in package a.b.c
	 * using a full build
	 */
	public void testInvalidInterfaceTag8F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTagTest(TESTING_PACKAGE, "test8", true);
	}
	
	/**
	 * Tests having an @noextend tag on a variety of inner / outer / top-level interfaces in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidInterfaceTag9I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test9", true);
	}
	
	/**
	 * Tests having an @noextend tag on a variety of inner / outer / top-level interfaces in package a.b.c
	 * using a full build
	 */
	public void testInvalidInterfaceTag9F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployFullBuildTagTest(TESTING_PACKAGE, "test9", true);
	}
	
	/**
	 * Tests having an @noextend tag on an interface in the default package
	 * using an incremental build
	 */
	public void testInvalidInterfaceTag10I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test10", true);
	}
	
	/**
	 * Tests having an @noextend tag on an interface in the default package
	 * using a full build
	 */
	public void testInvalidInterfaceTag10F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTagTest(TESTING_PACKAGE, "test10", true);
	}
	
	/**
	 * Tests having an @nooverride tag on an interface in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidInterfaceTag11I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test11", true);
	}
	
	/**
	 * Tests having an @nooverride tag on an interface in package a.b.c
	 * using a full build
	 */
	public void testInvalidInterfaceTag11F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTagTest(TESTING_PACKAGE, "test11", true);
	}
	
	/**
	 * Tests having an @nooverride tag on an outer interface in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidInterfaceTag12I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test12", true);
	}
	
	/**
	 * Tests having an @nooverride tag on an outer interface in package a.b.c
	 * using a full build
	 */
	public void testInvalidInterfaceTag12F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTagTest(TESTING_PACKAGE, "test12", true);
	}
	
	/**
	 * Tests having an @nooverride tag on an inner interface in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidInterfaceTag13I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test13", true);
	}
	
	/**
	 * Tests having an @nooverride tag on an inner interface in package a.b.c
	 * using a full build
	 */
	public void testInvalidInterfaceTag13F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTagTest(TESTING_PACKAGE, "test13", true);
	}
	
	/**
	 * Tests having an @nooverride tag on a variety of inner / outer / top-level interfaces in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidInterfaceTag14I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test14", true);
	}
	
	/**
	 * Tests having an @nooverride tag on a variety of inner / outer / top-level interfaces in package a.b.c
	 * using a full build
	 */
	public void testInvalidInterfaceTag14F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployFullBuildTagTest(TESTING_PACKAGE, "test14", true);
	}
	
	/**
	 * Tests having an @nooverride tag on an interface in the default package
	 * using an incremental build
	 */
	public void testInvalidInterfaceTag15I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test15", true);
	}
	
	/**
	 * Tests having an @nooverride tag on an interface in the default package
	 * using a full build
	 */
	public void testInvalidInterfaceTag15F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTagTest(TESTING_PACKAGE, "test15", true);
	}
	
	/**
	 * Tests having more than one invalid tag on an interface in the testing package a.b.c
	 * using an incremental build
	 */
	public void testInvalidInterfaceTag16I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test16", true);
	}
	
	/**
	 * Tests having more than one invalid tag on an interface in the testing package a.b.c
	 * using a full build
	 */
	public void testInvalidInterfaceTag16F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployFullBuildTagTest(TESTING_PACKAGE, "test16", true);
	}
	
	/**
	 * Tests having more than one invalid tag on an outer interface in the testing package a.b.c
	 * using an incremental build
	 */
	public void testInvalidInterfaceTag17I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test17", true);
	}
	
	/**
	 * Tests having more than one invalid tag on an outer interface in the testing package a.b.c
	 * using a full build
	 */
	public void testInvalidInterfaceTag17F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployFullBuildTagTest(TESTING_PACKAGE, "test17", true);
	}
	
	/**
	 * Tests having more than one invalid tag on an inner interface in the testing package a.b.c
	 * using an incremental build
	 */
	public void testInvalidInterfaceTag18I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test18", true);
	}
	
	/**
	 * Tests having more than one invalid tag on an inner interface in the testing package a.b.c
	 * using a full build
	 */
	public void testInvalidInterfaceTag18F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployFullBuildTagTest(TESTING_PACKAGE, "test18", true);
	}
	
	/**
	 * Tests having a variety of invalid tags on a variety of inner / outer / top-level interfaces in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidInterfaceTag19I() {
		setExpectedProblemIds(getDefaultProblemSet(16));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test19", true);
	}
	
	/**
	 * Tests having a variety of invalid tags on a variety of inner / outer / top-level interfaces in package a.b.c
	 * using a full build
	 */
	public void testInvalidInterfaceTag19F() {
		setExpectedProblemIds(getDefaultProblemSet(16));
		deployFullBuildTagTest(TESTING_PACKAGE, "test19", true);
	}
	
	/**
	 * Tests having more than one invalid tag on an interface in the default package
	 * using an incremental build
	 */
	public void testInvalidInterfaceTag20I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test20", true);
	}
	
	/**
	 * Tests having more than one invalid tag on an interface in the default package
	 * using a full build
	 */
	public void testInvalidInterfaceTag20F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployFullBuildTagTest(TESTING_PACKAGE, "test20", true);
	}
	
	/**
	 * Tests having an @noinstantiate tag on an interface in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidInterfaceTag21I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test21", true);
	}
	
	/**
	 * Tests having an @noinstantiate tag on an interface in package a.b.c
	 * using a full build
	 */
	public void testInvalidInterfaceTag21F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTagTest(TESTING_PACKAGE, "test21", true);
	}
	
	/**
	 * Tests having an @noinstantiate tag on an outer interface in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidInterfaceTag22I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test22", true);
	}
	
	/**
	 * Tests having an @noinstantiate tag on an outer interface in package a.b.c
	 * using a full build
	 */
	public void testInvalidInterfaceTag22F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTagTest(TESTING_PACKAGE, "test22", true);
	}
	
	/**
	 * Tests having an @noinstantiate tag on an inner interface in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidInterfaceTag23I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test23", true);
	}
	
	/**
	 * Tests having an @noinstantiate tag on an inner interface in package a.b.c
	 * using a full build
	 */
	public void testInvalidInterfaceTag23F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTagTest(TESTING_PACKAGE, "test23", true);
	}
	
	/**
	 * Tests having an @noinstantiate tag on a variety of inner / outer / top-level interfaces in package a.b.c
	 * using an incremental build
	 */
	public void testInvalidInterfaceTag24I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test24", true);
	}
	
	/**
	 * Tests having an @noinstantiate tag on a variety of inner / outer / top-level interfaces in package a.b.c
	 * using a full build
	 */
	public void testInvalidInterfaceTag24F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployFullBuildTagTest(TESTING_PACKAGE, "test24", true);
	}
	
	/**
	 * Tests having an @noinstantiate tag on an interface in the default package
	 * using an incremental build
	 */
	public void testInvalidInterfaceTag25I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTagTest("", "test25", true);
	}
	
	/**
	 * Tests having an @noinstantiate tag on an interface in the default package
	 * using a full build
	 */
	public void testInvalidInterfaceTag25F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTagTest("", "test25", true);
	}
}
