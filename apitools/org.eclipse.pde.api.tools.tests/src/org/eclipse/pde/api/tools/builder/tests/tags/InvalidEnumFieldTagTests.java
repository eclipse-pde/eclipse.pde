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
 * Tests the use of invalid tags on enum fields
 * 
 * @since 3.4
 */
public class InvalidEnumFieldTagTests extends InvalidFieldTagTests {

	/**
	 * Constructor
	 * @param name
	 */
	public InvalidEnumFieldTagTests(String name) {
		super(name);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.tags.InvalidFieldTagTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("enum");
	}

	/**
	 * @return the test for this enum
	 */
	public static Test suite() {
		return buildTestSuite(InvalidEnumFieldTagTests.class);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#assertProblems(org.eclipse.pde.api.tools.builder.tests.ApiProblem[])
	 */
	protected void assertProblems(ApiProblem[] problems) {
		String message = null;
		for(int i = 0; i < problems.length; i++) {
			message = problems[i].getMessage();
			assertTrue("The problem message is not correct: "+message, message.endsWith("an enum field") || message.endsWith("an enum constant")
					|| message.endsWith("a private enum field"));
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getTestCompliance()
	 */
	protected String getTestCompliance() {
		return CompilerOptions.VERSION_1_5;
	}
	
	/**
	 * Tests an invalid @noreference tag on three final fields in an enum
	 * using an incremental build
	 */
	public void testInvalidEnumFieldTag1I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test1", true);
	}
	
	/**
	 * Tests an invalid @noreference tag on three final fields in an enum
	 * using a full build
	 */
	public void testInvalidEnumFieldTag1F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTagTest(TESTING_PACKAGE, "test1", true);
	}
	
	/**
	 * Tests an invalid @noreference tag on three final fields in an outer enum
	 * using an incremental build
	 */
	public void testInvalidEnumFieldTag2I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test2", true);
	}
	
	/**
	 * Tests a invalid @noreference tag on three final fields in an outer enum
	 * using a full build
	 */
	public void testInvalidEnumFieldTag2F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTagTest(TESTING_PACKAGE, "test2", true);
	}
	
	/**
	 * Tests an invalid @noreference tag on three final fields in an inner enum
	 * using an incremental build
	 */
	public void testInvalidEnumFieldTag3I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test3", true);
	}
	
	/**
	 * Tests a invalid @noreference tag on three final fields in an inner enum
	 * using a full build
	 */
	public void testInvalidEnumFieldTag3F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTagTest(TESTING_PACKAGE, "test3", true);
	}
	
	/**
	 * Tests an invalid @noreference tag on final fields in inner / outer enums
	 * using an incremental build
	 */
	public void testInvalidEnumFieldTag4I() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test4", true);
	}
	
	/**
	 * Tests a invalid @noreference tag on final fields in inner /outer enums
	 * using a full build
	 */
	public void testInvalidEnumFieldTag4F() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployFullBuildTagTest(TESTING_PACKAGE, "test4", true);
	}
	
	/**
	 * Tests a valid @noreference tag on three final fields in an enum in the default package
	 * using an incremental build
	 */
	public void testInvalidEnumFieldTag5I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test5", true);
	}
	
	/**
	 * Tests a valid @noreference tag on three final fields in an enum in the default package
	 * using a full build
	 */
	public void testInvalidEnumFieldTag5F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTagTest(TESTING_PACKAGE, "test5", true);
	}
	
	/**
	 * Tests an invalid @noreference tag on three static final fields in an enum
	 * using an incremental build
	 */
	public void testInvalidEnumFieldTag6I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test6", true);
	}
	
	/**
	 * Tests an invalid @noreference tag on three static final fields in an enum
	 * using a full build
	 */
	public void testInvalidEnumFieldTag6F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTagTest(TESTING_PACKAGE, "test6", true);
	}
	
	/**
	 * Tests an invalid @noreference tag on three static final fields in an outer enum
	 * using an incremental build
	 */
	public void testInvalidEnumFieldTag7I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test7", true);
	}
	
	/**
	 * Tests a invalid @noreference tag on three static final fields in an outer enum
	 * using a full build
	 */
	public void testInvalidEnumFieldTag7F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTagTest(TESTING_PACKAGE, "test7", true);
	}
	
	/**
	 * Tests an invalid @noreference tag on three static final fields in an inner enum
	 * using an incremental build
	 */
	public void testInvalidEnumFieldTag8I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test8", true);
	}
	
	/**
	 * Tests a invalid @noreference tag on three static final fields in an inner enum
	 * using a full build
	 */
	public void testInvalidEnumFieldTag8F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTagTest(TESTING_PACKAGE, "test8", true);
	}
	
	/**
	 * Tests an invalid @noreference tag on static final fields in inner / outer enums
	 * using an incremental build
	 */
	public void testInvalidEnumFieldTag9I() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test9", true);
	}
	
	/**
	 * Tests a invalid @noreference tag on static final fields in inner /outer enums
	 * using a full build
	 */
	public void testInvalidEnumFieldTag9F() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployFullBuildTagTest(TESTING_PACKAGE, "test9", true);
	}
	
	/**
	 * Tests a valid @noreference tag on three static final fields in an enum in the default package
	 * using an incremental build
	 */
	public void testInvalidEnumFieldTag10I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test10", true);
	}
	
	/**
	 * Tests a valid @noreference tag on three static final fields in an enum in the default package
	 * using a full build
	 */
	public void testInvalidEnumFieldTag10F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTagTest(TESTING_PACKAGE, "test10", true);
	}
	
	/**
	 * Tests an invalid @noextend tag on three fields in an enum
	 * using an incremental build
	 */
	public void testInvalidEnumFieldTag11I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test11", true);
	}
	
	/**
	 * Tests an invalid @noextend tag on three fields in an enum
	 * using a full build
	 */
	public void testInvalidEnumFieldTag11F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTagTest(TESTING_PACKAGE, "test11", true);
	}
	
	/**
	 * Tests an invalid @noextend tag on three fields in an outer enum
	 * using an incremental build
	 */
	public void testInvalidEnumFieldTag12I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test12", true);
	}
	
	/**
	 * Tests a invalid @noextend tag on three fields in an outer enum
	 * using a full build
	 */
	public void testInvalidEnumFieldTag12F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTagTest(TESTING_PACKAGE, "test12", true);
	}
	
	/**
	 * Tests an invalid @noextend tag on three fields in an inner enum
	 * using an incremental build
	 */
	public void testInvalidEnumFieldTag13I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test13", true);
	}
	
	/**
	 * Tests a invalid @noextend tag on three fields in an inner enum
	 * using a full build
	 */
	public void testInvalidEnumFieldTag13F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTagTest(TESTING_PACKAGE, "test13", true);
	}
	
	/**
	 * Tests an invalid @noextend tag on fields in inner / outer enums
	 * using an incremental build
	 */
	public void testInvalidEnumFieldTag14I() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test14", true);
	}
	
	/**
	 * Tests a invalid @noextend tag on fields in inner /outer enums
	 * using a full build
	 */
	public void testInvalidEnumFieldTag14F() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployFullBuildTagTest(TESTING_PACKAGE, "test14", true);
	}
	
	/**
	 * Tests a valid @noextend tag on three fields in an enum in the default package
	 * using an incremental build
	 */
	public void testInvalidEnumFieldTag15I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test15", true);
	}
	
	/**
	 * Tests a valid @noextend tag on three fields in an enum in the default package
	 * using a full build
	 */
	public void testInvalidEnumFieldTag15F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTagTest(TESTING_PACKAGE, "test15", true);
	}
	
	/**
	 * Tests an invalid @noimplement tag on three fields in an enum
	 * using an incremental build
	 */
	public void testInvalidEnumFieldTag16I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test16", true);
	}
	
	/**
	 * Tests an invalid @noimplement tag on three fields in an enum
	 * using a full build
	 */
	public void testInvalidEnumFieldTag16F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTagTest(TESTING_PACKAGE, "test16", true);
	}
	
	/**
	 * Tests an invalid @noimplement tag on three fields in an outer enum
	 * using an incremental build
	 */
	public void testInvalidEnumFieldTag17I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test17", true);
	}
	
	/**
	 * Tests a invalid @noimplement tag on three fields in an outer enum
	 * using a full build
	 */
	public void testInvalidEnumFieldTag17F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTagTest(TESTING_PACKAGE, "test17", true);
	}
	
	/**
	 * Tests an invalid @noimplement tag on three fields in an inner enum
	 * using an incremental build
	 */
	public void testInvalidEnumFieldTag18I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test18", true);
	}
	
	/**
	 * Tests a invalid @noimplement tag on three fields in an inner enum
	 * using a full build
	 */
	public void testInvalidEnumFieldTag18F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTagTest(TESTING_PACKAGE, "test18", true);
	}
	
	/**
	 * Tests an invalid @noimplement tag on fields in inner / outer enums
	 * using an incremental build
	 */
	public void testInvalidEnumFieldTag19I() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test19", true);
	}
	
	/**
	 * Tests a invalid @noimplement tag on fields in inner /outer enums
	 * using a full build
	 */
	public void testInvalidEnumFieldTag19F() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployFullBuildTagTest(TESTING_PACKAGE, "test19", true);
	}
	
	/**
	 * Tests a valid @noimplement tag on three fields in an enum in the default package
	 * using an incremental build
	 */
	public void testInvalidEnumFieldTag20I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test20", true);
	}
	
	/**
	 * Tests a valid @noimplement tag on three fields in an enum in the default package
	 * using a full build
	 */
	public void testInvalidEnumFieldTag20F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTagTest(TESTING_PACKAGE, "test20", true);
	}
	
	/**
	 * Tests an invalid @nooverride tag on three fields in an enum
	 * using an incremental build
	 */
	public void testInvalidEnumFieldTag21I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test21", true);
	}
	
	/**
	 * Tests an invalid @nooverride tag on three fields in an enum
	 * using a full build
	 */
	public void testInvalidEnumFieldTag21F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTagTest(TESTING_PACKAGE, "test21", true);
	}
	
	/**
	 * Tests an invalid @nooverride tag on three fields in an outer enum
	 * using an incremental build
	 */
	public void testInvalidEnumFieldTag22I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test22", true);
	}
	
	/**
	 * Tests a invalid @nooverride tag on three fields in an outer enum
	 * using a full build
	 */
	public void testInvalidEnumFieldTag22F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTagTest(TESTING_PACKAGE, "test22", true);
	}
	
	/**
	 * Tests an invalid @nooverride tag on three fields in an inner enum
	 * using an incremental build
	 */
	public void testInvalidEnumFieldTag23I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test23", true);
	}
	
	/**
	 * Tests a invalid @nooverride tag on three fields in an inner enum
	 * using a full build
	 */
	public void testInvalidEnumFieldTag23F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTagTest(TESTING_PACKAGE, "test23", true);
	}
	
	/**
	 * Tests an invalid @nooverride tag on fields in inner / outer enums
	 * using an incremental build
	 */
	public void testInvalidEnumFieldTag24I() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test24", true);
	}
	
	/**
	 * Tests a invalid @nooverride tag on fields in inner /outer enums
	 * using a full build
	 */
	public void testInvalidEnumFieldTag24F() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployFullBuildTagTest(TESTING_PACKAGE, "test24", true);
	}
	
	/**
	 * Tests a valid @nooverride tag on three fields in an enum in the default package
	 * using an incremental build
	 */
	public void testInvalidEnumFieldTag25I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test25", true);
	}
	
	/**
	 * Tests a valid @nooverride tag on three fields in an enum in the default package
	 * using a full build
	 */
	public void testInvalidEnumFieldTag25F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTagTest(TESTING_PACKAGE, "test25", true);
	}
	
	/**
	 * Tests an invalid @noinstantiate tag on three fields in an enum
	 * using an incremental build
	 */
	public void testInvalidEnumFieldTag26I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test26", true);
	}
	
	/**
	 * Tests an invalid @noinstantiate tag on three fields in an enum
	 * using a full build
	 */
	public void testInvalidEnumFieldTag26F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTagTest(TESTING_PACKAGE, "test26", true);
	}
	
	/**
	 * Tests an invalid @noinstantiate tag on three fields in an outer enum
	 * using an incremental build
	 */
	public void testInvalidEnumFieldTag27I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test27", true);
	}
	
	/**
	 * Tests a invalid @noinstantiate tag on three fields in an outer enum
	 * using a full build
	 */
	public void testInvalidEnumFieldTag27F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTagTest(TESTING_PACKAGE, "test27", true);
	}
	
	/**
	 * Tests an invalid @noinstantiate tag on three fields in an inner enum
	 * using an incremental build
	 */
	public void testInvalidEnumFieldTag28I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test28", true);
	}
	
	/**
	 * Tests a invalid @noinstantiate tag on three fields in an inner enum
	 * using a full build
	 */
	public void testInvalidEnumFieldTag28F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTagTest(TESTING_PACKAGE, "test28", true);
	}
	
	/**
	 * Tests an invalid @noinstantiate tag on fields in inner / outer enums
	 * using an incremental build
	 */
	public void testInvalidEnumFieldTag29I() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test29", true);
	}
	
	/**
	 * Tests a invalid @noinstantiate tag on fields in inner /outer enums
	 * using a full build
	 */
	public void testInvalidEnumFieldTag29F() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployFullBuildTagTest(TESTING_PACKAGE, "test29", true);
	}
	
	/**
	 * Tests a valid @noinstantiate tag on three fields in an enum in the default package
	 * using an incremental build
	 */
	public void testInvalidEnumFieldTag30I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTagTest(TESTING_PACKAGE, "test30", true);
	}
	
	/**
	 * Tests a valid @noinstantiate tag on three fields in an enum in the default package
	 * using a full build
	 */
	public void testInvalidEnumFieldTag30F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTagTest(TESTING_PACKAGE, "test30", true);
	}

}
