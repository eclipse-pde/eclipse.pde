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
 * Tests invalid tags on fields in classes
 * 
 * @since 3.4
 */
public class InvalidClassFieldTagTests extends InvalidFieldTagTests {
	
	/**
	 * Constructor
	 * @param name
	 */
	public InvalidClassFieldTagTests(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.tags.InvalidJavadocTagFieldTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("class");
	}
	
	/**
	 * @return the test suite for class fields
	 */
	public static Test suite() {
		return buildTestSuite(InvalidClassFieldTagTests.class);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#assertProblems(org.eclipse.pde.api.tools.builder.tests.ApiProblem[])
	 */
	protected void assertProblems(ApiProblem[] problems) {
		String message = null;
		for(int i = 0; i < problems.length; i++) {
			message = problems[i].getMessage();
			assertTrue("The problem message is not correct: "+message, message.endsWith("a field") || message.endsWith("a final field") 
					|| message.endsWith("a private field"));
		}
	}
	
	/**
	 * Tests an invalid @noreference tag on three final fields in a class
	 * using an incremental build
	 */
	public void testInvalidClassFieldTag1I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test1", true);
	}
	
	/**
	 * Tests an invalid @noreference tag on three final fields in a class
	 * using a full build
	 */
	public void testInvalidClassFieldTag1F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test1", true);
	}
	
	/**
	 * Tests an invalid @noreference tag on three final fields in an outer class
	 * using an incremental build
	 */
	public void testInvalidClassFieldTag2I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test2", true);
	}
	
	/**
	 * Tests a invalid @noreference tag on three final fields in an outer class
	 * using a full build
	 */
	public void testInvalidClassFieldTag2F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test2", true);
	}
	
	/**
	 * Tests an invalid @noreference tag on three final fields in an inner class
	 * using an incremental build
	 */
	public void testInvalidClassFieldTag3I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test3", true);
	}
	
	/**
	 * Tests a invalid @noreference tag on three final fields in an inner class
	 * using a full build
	 */
	public void testInvalidClassFieldTag3F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test3", true);
	}
	
	/**
	 * Tests an invalid @noreference tag on final fields in inner / outer classes
	 * using an incremental build
	 */
	public void testInvalidClassFieldTag4I() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test4", true);
	}
	
	/**
	 * Tests a invalid @noreference tag on final fields in inner /outer class
	 * using a full build
	 */
	public void testInvalidClassFieldTag4F() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployFullBuildTest(TESTING_PACKAGE, "test4", true);
	}
	
	/**
	 * Tests a valid @noreference tag on three final fields in a class in the default package
	 * using an incremental build
	 */
	public void testInvalidClassFieldTag5I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test5", true);
	}
	
	/**
	 * Tests a valid @noreference tag on three final fields in a class in the default package
	 * using a full build
	 */
	public void testInvalidClassFieldTag5F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test5", true);
	}
	
	/**
	 * Tests an invalid @noreference tag on three static final fields in a class
	 * using an incremental build
	 */
	public void testInvalidClassFieldTag6I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test6", true);
	}
	
	/**
	 * Tests an invalid @noreference tag on three static final fields in a class
	 * using a full build
	 */
	public void testInvalidClassFieldTag6F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test6", true);
	}
	
	/**
	 * Tests an invalid @noreference tag on three static final fields in an outer class
	 * using an incremental build
	 */
	public void testInvalidClassFieldTag7I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test7", true);
	}
	
	/**
	 * Tests a invalid @noreference tag on three static final fields in an outer class
	 * using a full build
	 */
	public void testInvalidClassFieldTag7F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test7", true);
	}
	
	/**
	 * Tests an invalid @noreference tag on three static final fields in an inner class
	 * using an incremental build
	 */
	public void testInvalidClassFieldTag8I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test8", true);
	}
	
	/**
	 * Tests a invalid @noreference tag on three static final fields in an inner class
	 * using a full build
	 */
	public void testInvalidClassFieldTag8F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test8", true);
	}
	
	/**
	 * Tests an invalid @noreference tag on static final fields in inner / outer classes
	 * using an incremental build
	 */
	public void testInvalidClassFieldTag9I() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test9", true);
	}
	
	/**
	 * Tests a invalid @noreference tag on static final fields in inner /outer class
	 * using a full build
	 */
	public void testInvalidClassFieldTag9F() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployFullBuildTest(TESTING_PACKAGE, "test9", true);
	}
	
	/**
	 * Tests a valid @noreference tag on three static final fields in a class in the default package
	 * using an incremental build
	 */
	public void testInvalidClassFieldTag10I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test10", true);
	}
	
	/**
	 * Tests a valid @noreference tag on three static final fields in a class in the default package
	 * using a full build
	 */
	public void testInvalidClassFieldTag10F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test10", true);
	}
	
	/**
	 * Tests an invalid @noextend tag on three fields in a class
	 * using an incremental build
	 */
	public void testInvalidClassFieldTag11I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test11", true);
	}
	
	/**
	 * Tests an invalid @noextend tag on three fields in a class
	 * using a full build
	 */
	public void testInvalidClassFieldTag11F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test11", true);
	}
	
	/**
	 * Tests an invalid @noextend tag on three fields in an outer class
	 * using an incremental build
	 */
	public void testInvalidClassFieldTag12I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test12", true);
	}
	
	/**
	 * Tests a invalid @noextend tag on three fields in an outer class
	 * using a full build
	 */
	public void testInvalidClassFieldTag12F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test12", true);
	}
	
	/**
	 * Tests an invalid @noextend tag on three fields in an inner class
	 * using an incremental build
	 */
	public void testInvalidClassFieldTag13I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test13", true);
	}
	
	/**
	 * Tests a invalid @noextend tag on three fields in an inner class
	 * using a full build
	 */
	public void testInvalidClassFieldTag13F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test13", true);
	}
	
	/**
	 * Tests an invalid @noextend tag on fields in inner / outer classes
	 * using an incremental build
	 */
	public void testInvalidClassFieldTag14I() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test14", true);
	}
	
	/**
	 * Tests a invalid @noextend tag on fields in inner /outer class
	 * using a full build
	 */
	public void testInvalidClassFieldTag14F() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployFullBuildTest(TESTING_PACKAGE, "test14", true);
	}
	
	/**
	 * Tests a valid @noextend tag on three fields in a class in the default package
	 * using an incremental build
	 */
	public void testInvalidClassFieldTag15I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test15", true);
	}
	
	/**
	 * Tests a valid @noextend tag on three fields in a class in the default package
	 * using a full build
	 */
	public void testInvalidClassFieldTag15F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test15", true);
	}
	
	/**
	 * Tests an invalid @noimplement tag on three fields in a class
	 * using an incremental build
	 */
	public void testInvalidClassFieldTag16I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test16", true);
	}
	
	/**
	 * Tests an invalid @noimplement tag on three fields in a class
	 * using a full build
	 */
	public void testInvalidClassFieldTag16F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test16", true);
	}
	
	/**
	 * Tests an invalid @noimplement tag on three fields in an outer class
	 * using an incremental build
	 */
	public void testInvalidClassFieldTag17I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test17", true);
	}
	
	/**
	 * Tests a invalid @noimplement tag on three fields in an outer class
	 * using a full build
	 */
	public void testInvalidClassFieldTag17F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test17", true);
	}
	
	/**
	 * Tests an invalid @noimplement tag on three fields in an inner class
	 * using an incremental build
	 */
	public void testInvalidClassFieldTag18I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test18", true);
	}
	
	/**
	 * Tests a invalid @noimplement tag on three fields in an inner class
	 * using a full build
	 */
	public void testInvalidClassFieldTag18F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test18", true);
	}
	
	/**
	 * Tests an invalid @noimplement tag on fields in inner / outer classes
	 * using an incremental build
	 */
	public void testInvalidClassFieldTag19I() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test19", true);
	}
	
	/**
	 * Tests a invalid @noimplement tag on fields in inner /outer class
	 * using a full build
	 */
	public void testInvalidClassFieldTag19F() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployFullBuildTest(TESTING_PACKAGE, "test19", true);
	}
	
	/**
	 * Tests a valid @noimplement tag on three fields in a class in the default package
	 * using an incremental build
	 */
	public void testInvalidClassFieldTag20I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test20", true);
	}
	
	/**
	 * Tests a valid @noimplement tag on three fields in a class in the default package
	 * using a full build
	 */
	public void testInvalidClassFieldTag20F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test20", true);
	}
	
	/**
	 * Tests an invalid @nooverride tag on three fields in a class
	 * using an incremental build
	 */
	public void testInvalidClassFieldTag21I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test21", true);
	}
	
	/**
	 * Tests an invalid @nooverride tag on three fields in a class
	 * using a full build
	 */
	public void testInvalidClassFieldTag21F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test21", true);
	}
	
	/**
	 * Tests an invalid @nooverride tag on three fields in an outer class
	 * using an incremental build
	 */
	public void testInvalidClassFieldTag22I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test22", true);
	}
	
	/**
	 * Tests a invalid @nooverride tag on three fields in an outer class
	 * using a full build
	 */
	public void testInvalidClassFieldTag22F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test22", true);
	}
	
	/**
	 * Tests an invalid @nooverride tag on three fields in an inner class
	 * using an incremental build
	 */
	public void testInvalidClassFieldTag23I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test23", true);
	}
	
	/**
	 * Tests a invalid @nooverride tag on three fields in an inner class
	 * using a full build
	 */
	public void testInvalidClassFieldTag23F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test23", true);
	}
	
	/**
	 * Tests an invalid @nooverride tag on fields in inner / outer classes
	 * using an incremental build
	 */
	public void testInvalidClassFieldTag24I() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test24", true);
	}
	
	/**
	 * Tests a invalid @nooverride tag on fields in inner /outer class
	 * using a full build
	 */
	public void testInvalidClassFieldTag24F() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployFullBuildTest(TESTING_PACKAGE, "test24", true);
	}
	
	/**
	 * Tests a valid @nooverride tag on three fields in a class in the default package
	 * using an incremental build
	 */
	public void testInvalidClassFieldTag25I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test25", true);
	}
	
	/**
	 * Tests a valid @nooverride tag on three fields in a class in the default package
	 * using a full build
	 */
	public void testInvalidClassFieldTag25F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test25", true);
	}
	
	/**
	 * Tests an invalid @noinstantiate tag on three fields in a class
	 * using an incremental build
	 */
	public void testInvalidClassFieldTag26I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test26", true);
	}
	
	/**
	 * Tests an invalid @noinstantiate tag on three fields in a class
	 * using a full build
	 */
	public void testInvalidClassFieldTag26F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test26", true);
	}
	
	/**
	 * Tests an invalid @noinstantiate tag on three fields in an outer class
	 * using an incremental build
	 */
	public void testInvalidClassFieldTag27I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test27", true);
	}
	
	/**
	 * Tests a invalid @noinstantiate tag on three fields in an outer class
	 * using a full build
	 */
	public void testInvalidClassFieldTag27F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test27", true);
	}
	
	/**
	 * Tests an invalid @noinstantiate tag on three fields in an inner class
	 * using an incremental build
	 */
	public void testInvalidClassFieldTag28I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test28", true);
	}
	
	/**
	 * Tests a invalid @noinstantiate tag on three fields in an inner class
	 * using a full build
	 */
	public void testInvalidClassFieldTag28F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test28", true);
	}
	
	/**
	 * Tests an invalid @noinstantiate tag on fields in inner / outer classes
	 * using an incremental build
	 */
	public void testInvalidClassFieldTag29I() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test29", true);
	}
	
	/**
	 * Tests a invalid @noinstantiate tag on fields in inner /outer class
	 * using a full build
	 */
	public void testInvalidClassFieldTag29F() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployFullBuildTest(TESTING_PACKAGE, "test29", true);
	}
	
	/**
	 * Tests a valid @noinstantiate tag on three fields in a class in the default package
	 * using an incremental build
	 */
	public void testInvalidClassFieldTag30I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test30", true);
	}
	
	/**
	 * Tests a valid @noinstantiate tag on three fields in a class in the default package
	 * using a full build
	 */
	public void testInvalidClassFieldTag30F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployFullBuildTest(TESTING_PACKAGE, "test30", true);
	}
}
