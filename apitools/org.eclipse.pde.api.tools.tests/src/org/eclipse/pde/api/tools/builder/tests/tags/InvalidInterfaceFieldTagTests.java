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
 * Tests invalid tag use on interface fields
 * 
 * @since 3.4
 */
public class InvalidInterfaceFieldTagTests extends InvalidFieldTagTests {

	/**
	 * Constructor
	 * @param name
	 */
	public InvalidInterfaceFieldTagTests(String name) {
		super(name);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.tags.InvalidFieldTagTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("interface");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#assertProblems(org.eclipse.pde.api.tools.builder.tests.ApiProblem[])
	 */
	protected void assertProblems(ApiProblem[] problems) {
		String message = null;
		for(int i = 0; i < problems.length; i++) {
			message = problems[i].getMessage();
			assertTrue("The problem message is not correct: "+message, message.endsWith("a field") || message.endsWith("a final field"));
		}
	}
	
	/**
	 * @return the test for this class
	 */
	public static Test suite() {
		return buildTestSuite(InvalidInterfaceFieldTagTests.class);
	}
	
	/**
	 * Tests the unsupported @noextend tag on an interface field
	 * using an incremental build
	 */
	public void testInvalidInterfaceFieldTag1I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test1", true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on an interface field
	 * using a full build
	 */
	public void testInvalidInterfaceFieldTag1F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test1", true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on an outer interface field
	 * using an incremental build
	 */
	public void testInvalidInterfaceFieldTag2I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test2", true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on an outer interface field
	 * using a full build
	 */
	public void testInvalidInterfaceFieldTag2F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test2", true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on an inner interface field
	 * using an incremental build
	 */
	public void testInvalidInterfaceFieldTag3I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test3", true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on an inner interface field
	 * using a full build
	 */
	public void testInvalidInterfaceFieldTag3F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test3", true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on a variety of inner / outer interface fields
	 * using an incremental build
	 */
	public void testInvalidInterfaceFieldTag4I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test4", true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on a variety of inner / outer interface fields
	 * using a full build
	 */
	public void testInvalidInterfaceFieldTag4F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test4", true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on an interface field in the default package
	 * using an incremental build
	 */
	public void testInvalidInterfaceFieldTag5I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest("", "test5", true);
	}
	
	/**
	 * Tests the unsupported @noextend tag on an interface field in the default package
	 * using a full build
	 */
	public void testInvalidInterfaceFieldTag5F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest("", "test5", true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on an interface field
	 * using an incremental build
	 */
	public void testInvalidInterfaceFieldTag6I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test6", true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on an interface field
	 * using a full build
	 */
	public void testInvalidInterfaceFieldTag6F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test6", true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on an outer interface field
	 * using an incremental build
	 */
	public void testInvalidInterfaceFieldTag7I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test7", true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on an outer interface field
	 * using a full build
	 */
	public void testInvalidInterfaceFieldTag7F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test7", true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on an inner interface field
	 * using an incremental build
	 */
	public void testInvalidInterfaceFieldTag8I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test8", true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on an inner interface field
	 * using a full build
	 */
	public void testInvalidInterfaceFieldTag8F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test8", true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on a variety of inner / outer interface fields
	 * using an incremental build
	 */
	public void testInvalidInterfaceFieldTag9I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test9", true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on a variety of inner / outer interface fields
	 * using a full build
	 */
	public void testInvalidInterfaceFieldTag9F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test9", true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on an interface field in the default package
	 * using an incremental build
	 */
	public void testInvalidInterfaceFieldTag10I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest("", "test10", true);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on an interface field in the default package
	 * using a full build
	 */
	public void testInvalidInterfaceFieldTag10F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest("", "test10", true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on an interface field
	 * using an incremental build
	 */
	public void testInvalidInterfaceFieldTag11I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test11", true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on an interface field
	 * using a full build
	 */
	public void testInvalidInterfaceFieldTag11F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test11", true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on an outer interface field
	 * using an incremental build
	 */
	public void testInvalidInterfaceFieldTag12I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test12", true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on an outer interface field
	 * using a full build
	 */
	public void testInvalidInterfaceFieldTag12F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test12", true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on an inner interface field
	 * using an incremental build
	 */
	public void testInvalidInterfaceFieldTag13I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test13", true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on an inner interface field
	 * using a full build
	 */
	public void testInvalidInterfaceFieldTag13F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test13", true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on a variety of inner / outer interface fields
	 * using an incremental build
	 */
	public void testInvalidInterfaceFieldTag14I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test14", true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on a variety of inner / outer interface fields
	 * using a full build
	 */
	public void testInvalidInterfaceFieldTag14F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test14", true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on an interface field in the default package
	 * using an incremental build
	 */
	public void testInvalidInterfaceFieldTag15I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest("", "test15", true);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on an interface field in the default package
	 * using a full build
	 */
	public void testInvalidInterfaceFieldTag15F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest("", "test15", true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on an interface field
	 * using an incremental build
	 */
	public void testInvalidInterfaceFieldTag16I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test16", true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on an interface field
	 * using a full build
	 */
	public void testInvalidInterfaceFieldTag16F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test16", true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on an outer interface field
	 * using an incremental build
	 */
	public void testInvalidInterfaceFieldTag17I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test17", true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on an outer interface field
	 * using a full build
	 */
	public void testInvalidInterfaceFieldTag17F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test17", true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on an inner interface field
	 * using an incremental build
	 */
	public void testInvalidInterfaceFieldTag18I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test18", true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on an inner interface field
	 * using a full build
	 */
	public void testInvalidInterfaceFieldTag18F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test18", true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on a variety of inner / outer interface fields
	 * using an incremental build
	 */
	public void testInvalidInterfaceFieldTag19I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test19", true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on a variety of inner / outer interface fields
	 * using a full build
	 */
	public void testInvalidInterfaceFieldTag19F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test19", true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on an interface field in the default package
	 * using an incremental build
	 */
	public void testInvalidInterfaceFieldTag20I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest("", "test20", true);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on an interface field in the default package
	 * using a full build
	 */
	public void testInvalidInterfaceFieldTag20F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest("", "test20", true);
	}
	
	/**
	 * Tests all the unsupported tags on a variety of interface fields
	 * using an incremental build
	 */
	public void testInvalidInterfaceFieldTag21I() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test21", true);
	}
	
	/**
	 * Tests all the unsupported tags on a variety of interface fields
	 * using a full build
	 */
	public void testInvalidInterfaceFieldTag21F() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployFullBuildTest(TESTING_PACKAGE, "test21", true);
	}
	
	/**
	 * Tests the unsupported @noreference tag on a final interface field
	 * using an incremental build
	 */
	public void testInvalidInterfaceFieldTag22I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test22", true);
	}
	
	/**
	 * Tests the unsupported @noreference tag on a final interface field
	 * using a full build
	 */
	public void testInvalidInterfaceFieldTag22F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test22", true);
	}
	
	/**
	 * Tests the unsupported @noreference tag on a final outer interface field
	 * using an incremental build
	 */
	public void testInvalidInterfaceFieldTag23I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test23", true);
	}
	
	/**
	 * Tests the unsupported @noreference tag on a final outer interface field
	 * using a full build
	 */
	public void testInvalidInterfaceFieldTag23F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test23", true);
	}
	
	/**
	 * Tests the unsupported @noreference tag on a final inner interface field
	 * using an incremental build
	 */
	public void testInvalidInterfaceFieldTag24I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test24", true);
	}
	
	/**
	 * Tests the unsupported @noreference tag on a final inner interface field
	 * using a full build
	 */
	public void testInvalidInterfaceFieldTag24F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test24", true);
	}
	
	/**
	 * Tests the unsupported @noreference tag on a variety of final inner / outer interface fields
	 * using an incremental build
	 */
	public void testInvalidInterfaceFieldTag25I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test25", true);
	}
	
	/**
	 * Tests the unsupported @noreference tag on a variety of final inner / outer interface fields
	 * using a full build
	 */
	public void testInvalidInterfaceFieldTag25F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test25", true);
	}
	
	/**
	 * Tests the unsupported @noreference tag on a final interface field in the default package
	 * using an incremental build
	 */
	public void testInvalidInterfaceFieldTag26I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest("", "test26", true);
	}
	
	/**
	 * Tests the unsupported @noreference tag on a final interface field in the default package
	 * using a full build
	 */
	public void testInvalidInterfaceFieldTag26F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest("", "test26", true);
	}
	
	/**
	 * Tests the unsupported @noreference tag on a static final interface field
	 * using an incremental build
	 */
	public void testInvalidInterfaceFieldTag27I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test27", true);
	}
	
	/**
	 * Tests the unsupported @noreference tag on a static final interface field
	 * using a full build
	 */
	public void testInvalidInterfaceFieldTag27F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test27", true);
	}
	
	/**
	 * Tests the unsupported @noreference tag on a static final outer interface field
	 * using an incremental build
	 */
	public void testInvalidInterfaceFieldTag28I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test28", true);
	}
	
	/**
	 * Tests the unsupported @noreference tag on a static final outer interface field
	 * using a full build
	 */
	public void testInvalidInterfaceFieldTag28F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test28", true);
	}
	
	/**
	 * Tests the unsupported @noreference tag on a static final inner interface field
	 * using an incremental build
	 */
	public void testInvalidInterfaceFieldTag29I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test29", true);
	}
	
	/**
	 * Tests the unsupported @noreference tag on a static final inner interface field
	 * using a full build
	 */
	public void testInvalidInterfaceFieldTag29F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest(TESTING_PACKAGE, "test29", true);
	}
	
	/**
	 * Tests the unsupported @noreference tag on a variety of static final inner / outer interface fields
	 * using an incremental build
	 */
	public void testInvalidInterfaceFieldTag30I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test30", true);
	}
	
	/**
	 * Tests the unsupported @noreference tag on a variety of static final inner / outer interface fields
	 * using a full build
	 */
	public void testInvalidInterfaceFieldTag30F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test30", true);
	}
	
	/**
	 * Tests the unsupported @noreference tag on a static final interface field in the default package
	 * using an incremental build
	 */
	public void testInvalidInterfaceFieldTag31I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployIncrementalBuildTest("", "test31", true);
	}
	
	/**
	 * Tests the unsupported @noreference tag on a static final interface field in the default package
	 * using a full build
	 */
	public void testInvalidInterfaceFieldTag31F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployFullBuildTest("", "test31", true);
	}
}
