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
	
	/**
	 * Tests an invalid @noreference tag on three final fields in a class
	 * using an incremental build
	 */
	public void test1I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test1", true);
	}
	
	/**
	 * Tests an invalid @noreference tag on three final fields in a class
	 * using a full build
	 */
	public void test1F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test1", true);
	}
	
	/**
	 * Tests an invalid @noreference tag on three final fields in an outer class
	 * using an incremental build
	 */
	public void test2I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test2", true);
	}
	
	/**
	 * Tests a invalid @noreference tag on three final fields in an outer class
	 * using a full build
	 */
	public void test2F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test2", true);
	}
	
	/**
	 * Tests an invalid @noreference tag on three final fields in an inner class
	 * using an incremental build
	 */
	public void test3I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test3", true);
	}
	
	/**
	 * Tests a invalid @noreference tag on three final fields in an inner class
	 * using a full build
	 */
	public void test3F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test3", true);
	}
	
	/**
	 * Tests an invalid @noreference tag on final fields in inner / outer classes
	 * using an incremental build
	 */
	public void test4I() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test4", true);
	}
	
	/**
	 * Tests a invalid @noreference tag on final fields in inner /outer class
	 * using a full build
	 */
	public void test4F() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test4", true);
	}
	
	/**
	 * Tests a valid @noreference tag on three final fields in a class in the default package
	 * using an incremental build
	 */
	public void test5I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test5", true);
	}
	
	/**
	 * Tests a valid @noreference tag on three final fields in a class in the default package
	 * using a full build
	 */
	public void test5F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test5", true);
	}
	
	/**
	 * Tests an invalid @noreference tag on three static final fields in a class
	 * using an incremental build
	 */
	public void test6I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test6", true);
	}
	
	/**
	 * Tests an invalid @noreference tag on three static final fields in a class
	 * using a full build
	 */
	public void test6F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test6", true);
	}
	
	/**
	 * Tests an invalid @noreference tag on three static final fields in an outer class
	 * using an incremental build
	 */
	public void test7I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test7", true);
	}
	
	/**
	 * Tests a invalid @noreference tag on three static final fields in an outer class
	 * using a full build
	 */
	public void test7F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test7", true);
	}
	
	/**
	 * Tests an invalid @noreference tag on three static final fields in an inner class
	 * using an incremental build
	 */
	public void test8I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test8", true);
	}
	
	/**
	 * Tests a invalid @noreference tag on three static final fields in an inner class
	 * using a full build
	 */
	public void test8F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test8", true);
	}
	
	/**
	 * Tests an invalid @noreference tag on static final fields in inner / outer classes
	 * using an incremental build
	 */
	public void test9I() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test9", true);
	}
	
	/**
	 * Tests a invalid @noreference tag on static final fields in inner /outer class
	 * using a full build
	 */
	public void test9F() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test9", true);
	}
	
	/**
	 * Tests a valid @noreference tag on three static final fields in a class in the default package
	 * using an incremental build
	 */
	public void test10I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test10", true);
	}
	
	/**
	 * Tests a valid @noreference tag on three static final fields in a class in the default package
	 * using a full build
	 */
	public void test10F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test10", true);
	}
	
	/**
	 * Tests an invalid @noextend tag on three fields in a class
	 * using an incremental build
	 */
	public void test11I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test11", true);
	}
	
	/**
	 * Tests an invalid @noextend tag on three fields in a class
	 * using a full build
	 */
	public void test11F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test11", true);
	}
	
	/**
	 * Tests an invalid @noextend tag on three fields in an outer class
	 * using an incremental build
	 */
	public void test12I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test12", true);
	}
	
	/**
	 * Tests a invalid @noextend tag on three fields in an outer class
	 * using a full build
	 */
	public void test12F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test12", true);
	}
	
	/**
	 * Tests an invalid @noextend tag on three fields in an inner class
	 * using an incremental build
	 */
	public void test13I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test13", true);
	}
	
	/**
	 * Tests a invalid @noextend tag on three fields in an inner class
	 * using a full build
	 */
	public void test13F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test13", true);
	}
	
	/**
	 * Tests an invalid @noextend tag on fields in inner / outer classes
	 * using an incremental build
	 */
	public void test14I() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test14", true);
	}
	
	/**
	 * Tests a invalid @noextend tag on fields in inner /outer class
	 * using a full build
	 */
	public void test14F() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test14", true);
	}
	
	/**
	 * Tests a valid @noextend tag on three fields in a class in the default package
	 * using an incremental build
	 */
	public void test15I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test15", true);
	}
	
	/**
	 * Tests a valid @noextend tag on three fields in a class in the default package
	 * using a full build
	 */
	public void test15F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test15", true);
	}
	
	/**
	 * Tests an invalid @noimplement tag on three fields in a class
	 * using an incremental build
	 */
	public void test16I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test16", true);
	}
	
	/**
	 * Tests an invalid @noimplement tag on three fields in a class
	 * using a full build
	 */
	public void test16F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test16", true);
	}
	
	/**
	 * Tests an invalid @noimplement tag on three fields in an outer class
	 * using an incremental build
	 */
	public void test17I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test17", true);
	}
	
	/**
	 * Tests a invalid @noimplement tag on three fields in an outer class
	 * using a full build
	 */
	public void test17F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test17", true);
	}
	
	/**
	 * Tests an invalid @noimplement tag on three fields in an inner class
	 * using an incremental build
	 */
	public void test18I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test18", true);
	}
	
	/**
	 * Tests a invalid @noimplement tag on three fields in an inner class
	 * using a full build
	 */
	public void test18F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test18", true);
	}
	
	/**
	 * Tests an invalid @noimplement tag on fields in inner / outer classes
	 * using an incremental build
	 */
	public void test19I() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test19", true);
	}
	
	/**
	 * Tests a invalid @noimplement tag on fields in inner /outer class
	 * using a full build
	 */
	public void test19F() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test19", true);
	}
	
	/**
	 * Tests a valid @noimplement tag on three fields in a class in the default package
	 * using an incremental build
	 */
	public void test20I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test20", true);
	}
	
	/**
	 * Tests a valid @noimplement tag on three fields in a class in the default package
	 * using a full build
	 */
	public void test20F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test20", true);
	}
	
	/**
	 * Tests an invalid @nooverride tag on three fields in a class
	 * using an incremental build
	 */
	public void test21I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test21", true);
	}
	
	/**
	 * Tests an invalid @nooverride tag on three fields in a class
	 * using a full build
	 */
	public void test21F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test21", true);
	}
	
	/**
	 * Tests an invalid @nooverride tag on three fields in an outer class
	 * using an incremental build
	 */
	public void test22I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test22", true);
	}
	
	/**
	 * Tests a invalid @nooverride tag on three fields in an outer class
	 * using a full build
	 */
	public void test22F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test22", true);
	}
	
	/**
	 * Tests an invalid @nooverride tag on three fields in an inner class
	 * using an incremental build
	 */
	public void test23I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test23", true);
	}
	
	/**
	 * Tests a invalid @nooverride tag on three fields in an inner class
	 * using a full build
	 */
	public void test23F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test23", true);
	}
	
	/**
	 * Tests an invalid @nooverride tag on fields in inner / outer classes
	 * using an incremental build
	 */
	public void test24I() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test24", true);
	}
	
	/**
	 * Tests a invalid @nooverride tag on fields in inner /outer class
	 * using a full build
	 */
	public void test24F() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test24", true);
	}
	
	/**
	 * Tests a valid @nooverride tag on three fields in a class in the default package
	 * using an incremental build
	 */
	public void test25I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test25", true);
	}
	
	/**
	 * Tests a valid @nooverride tag on three fields in a class in the default package
	 * using a full build
	 */
	public void test25F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test25", true);
	}
	
	/**
	 * Tests an invalid @noinstantiate tag on three fields in a class
	 * using an incremental build
	 */
	public void test26I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test26", true);
	}
	
	/**
	 * Tests an invalid @noinstantiate tag on three fields in a class
	 * using a full build
	 */
	public void test26F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test26", true);
	}
	
	/**
	 * Tests an invalid @noinstantiate tag on three fields in an outer class
	 * using an incremental build
	 */
	public void test27I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test27", true);
	}
	
	/**
	 * Tests a invalid @noinstantiate tag on three fields in an outer class
	 * using a full build
	 */
	public void test27F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test27", true);
	}
	
	/**
	 * Tests an invalid @noinstantiate tag on three fields in an inner class
	 * using an incremental build
	 */
	public void test28I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test28", true);
	}
	
	/**
	 * Tests a invalid @noinstantiate tag on three fields in an inner class
	 * using a full build
	 */
	public void test28F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test28", true);
	}
	
	/**
	 * Tests an invalid @noinstantiate tag on fields in inner / outer classes
	 * using an incremental build
	 */
	public void test29I() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test29", true);
	}
	
	/**
	 * Tests a invalid @noinstantiate tag on fields in inner /outer class
	 * using a full build
	 */
	public void test29F() {
		setExpectedProblemIds(getDefaultProblemSet(12));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test29", true);
	}
	
	/**
	 * Tests a valid @noinstantiate tag on three fields in a class in the default package
	 * using an incremental build
	 */
	public void test30I() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test30", true);
	}
	
	/**
	 * Tests a valid @noinstantiate tag on three fields in a class in the default package
	 * using a full build
	 */
	public void test30F() {
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployIncrementalBuildTest(TESTING_PACKAGE, "test30", true);
	}
}
