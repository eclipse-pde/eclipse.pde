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
 * Tests the use of invalid tags on enum constants
 * 
 * @since 3.4
 */
public class InvalidEnumConstantTagTests extends InvalidFieldTagTests {

	/**
	 * Constructor
	 * @param name
	 */
	public InvalidEnumConstantTagTests(String name) {
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
		return buildTestSuite(InvalidEnumConstantTagTests.class);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#assertProblems(org.eclipse.pde.api.tools.builder.tests.ApiProblem[])
	 */
	protected void assertProblems(ApiProblem[] problems) {
		String message = null;
		for(int i = 0; i < problems.length; i++) {
			message = problems[i].getMessage();
			assertTrue("The problem message is not correct: "+message, message.endsWith("an enum constant"));
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getTestCompliance()
	 */
	protected String getTestCompliance() {
		return CompilerOptions.VERSION_1_5;
	}
	
	/**
	 * Tests an invalid @noreference tag on an enum constant
	 * using an incremental build
	 */
	public void testInvalidEnumConstantTag1I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test31", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests an invalid @noreference tag on an enum constant
	 * using a full build
	 */
	public void testInvalidEnumConstantTag1F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test31", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests an invalid @noreference tag on an enum constant in an outer enum
	 * using an incremental build
	 */
	public void testInvalidEnumConstantTag2I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test32", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests a invalid @noreference tag on an enum constant in an outer enum
	 * using a full build
	 */
	public void testInvalidEnumConstantTag2F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test32", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests an invalid @noreference tag on an enum constant in an inner enum
	 * using an incremental build
	 */
	public void testInvalidEnumConstantTag3I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test33", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests a invalid @noreference tag on an enum constant in an inner enum
	 * using a full build
	 */
	public void testInvalidEnumConstantTag3F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test33", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests an invalid @noreference tag on enum constants in inner / outer enums
	 * using an incremental build
	 */
	public void testInvalidEnumConstantTag4I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest(TESTING_PACKAGE, "test34", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests a invalid @noreference tag on enum constants in inner / outer enums
	 * using a full build
	 */
	public void testInvalidEnumConstantTag4F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest(TESTING_PACKAGE, "test34", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests a valid @noreference tag on an enum constant in an enum in the default package
	 * using an incremental build
	 */
	public void testInvalidEnumConstantTag5I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test35", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests a valid @noreference tag on an enum constant in an enum in the default package
	 * using a full build
	 */
	public void testInvalidEnumConstantTag5F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test35", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests an invalid @noextend tag on an enum constant
	 * using an incremental build
	 */
	public void testInvalidEnumConstantTag6I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test36", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests an invalid @noextend tag on an enum constant
	 * using a full build
	 */
	public void testInvalidEnumConstantTag6F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test36", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests an invalid @noextend tag on an enum constant in an outer enum
	 * using an incremental build
	 */
	public void testInvalidEnumConstantTag7I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test37", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests a invalid @noextend tag on an enum constant in an outer enum
	 * using a full build
	 */
	public void testInvalidEnumConstantTag7F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test37", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests an invalid @noextend tag on an enum constant in an inner enum
	 * using an incremental build
	 */
	public void testInvalidEnumConstantTag8I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test38", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests a invalid @noextend tag on an enum constant in an inner enum
	 * using a full build
	 */
	public void testInvalidEnumConstantTag8F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test38", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests an invalid @noextend tag on enum constants in inner / outer enums
	 * using an incremental build
	 */
	public void testInvalidEnumConstantTag9I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest(TESTING_PACKAGE, "test39", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests a invalid @noextend tag on enum constants in inner /outer enums
	 * using a full build
	 */
	public void testInvalidEnumConstantTag9F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest(TESTING_PACKAGE, "test39", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests a valid @noextend tag on an enum constant in an enum in the default package
	 * using an incremental build
	 */
	public void testInvalidEnumConstantTag10I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test40", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests a valid @noextend tag on an enum constant in an enum in the default package
	 * using a full build
	 */
	public void testInvalidEnumConstantTag10F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test40", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests an invalid @noimplement tag on an enum constant
	 * using an incremental build
	 */
	public void testInvalidEnumConstantTag11I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test41", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests an invalid @noimplement tag on an enum constant
	 * using a full build
	 */
	public void testInvalidEnumConstantTag11F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test41", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests an invalid @noimplement tag on an enum constant in an outer enum
	 * using an incremental build
	 */
	public void testInvalidEnumConstantTag12I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test42", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests a invalid @noimplement tag on an enum constant in an outer enum
	 * using a full build
	 */
	public void testInvalidEnumConstantTag12F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test42", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests an invalid @noimplement tag on an enum constant in an inner enum
	 * using an incremental build
	 */
	public void testInvalidEnumConstantTag13I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test43", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests a invalid @noimplement tag on an enum constant in an inner enum
	 * using a full build
	 */
	public void testInvalidEnumConstantTag13F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test43", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests an invalid @noimplement tag on enum constants in inner / outer enums
	 * using an incremental build
	 */
	public void testInvalidEnumConstantTag14I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest(TESTING_PACKAGE, "test44", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests a invalid @noimplement tag on enum constants in inner /outer enums
	 * using a full build
	 */
	public void testInvalidEnumConstantTag14F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest(TESTING_PACKAGE, "test44", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests a valid @noimplement tag on an enum constant in an enum in the default package
	 * using an incremental build
	 */
	public void testInvalidEnumConstantTag15I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test45", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests a valid @noimplement tag on an enum constant in an enum in the default package
	 * using a full build
	 */
	public void testInvalidEnumConstantTag15F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test45", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests an invalid @nooverride tag on an enum constant in an enum
	 * using an incremental build
	 */
	public void testInvalidEnumConstantTag16I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test46", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests an invalid @nooverride tag on an enum constant in an enum
	 * using a full build
	 */
	public void testInvalidEnumConstantTag16F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test46", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests an invalid @nooverride tag on an enum constant in an outer enum
	 * using an incremental build
	 */
	public void testInvalidEnumConstantTag17I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test47", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests a invalid @nooverride tag on an enum constant in an outer enum
	 * using a full build
	 */
	public void testInvalidEnumConstantTag17F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test47", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests an invalid @nooverride tag on an enum constant in an inner enum
	 * using an incremental build
	 */
	public void testInvalidEnumConstantTag18I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test48", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests a invalid @nooverride tag on an enum constant in an inner enum
	 * using a full build
	 */
	public void testInvalidEnumConstantTag18F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test48", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests an invalid @nooverride tag on enum constants in inner / outer enums
	 * using an incremental build
	 */
	public void testInvalidEnumConstantTag19I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest(TESTING_PACKAGE, "test49", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests a invalid @nooverride tag on enum constants in inner /outer enums
	 * using a full build
	 */
	public void testInvalidEnumConstantTag19F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest(TESTING_PACKAGE, "test49", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests a valid @nooverride tag on an enum constant in an enum in the default package
	 * using an incremental build
	 */
	public void testInvalidEnumConstantTag20I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test50", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests a valid @nooverride tag on an enum constant in an enum in the default package
	 * using a full build
	 */
	public void testInvalidEnumConstantTag20F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test50", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests an invalid @noinstantiate tag on an enum constant in an enum
	 * using an incremental build
	 */
	public void testInvalidEnumConstantTag21I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test51", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests an invalid @noinstantiate tag on an enum constant in an enum
	 * using a full build
	 */
	public void testInvalidEnumConstantTag21F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test51", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests an invalid @noinstantiate tag on an enum constant in an outer enum
	 * using an incremental build
	 */
	public void testInvalidEnumConstantTag22I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test52", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests a invalid @noinstantiate tag on an enum constant in an outer enum
	 * using a full build
	 */
	public void testInvalidEnumConstantTag22F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test52", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests an invalid @noinstantiate tag on an enum constant in an inner enum
	 * using an incremental build
	 */
	public void testInvalidEnumConstantTag23I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test53", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests a invalid @noinstantiate tag on an enum constant in an inner enum
	 * using a full build
	 */
	public void testInvalidEnumConstantTag23F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test53", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests an invalid @noinstantiate tag on enum constants in inner / outer enums
	 * using an incremental build
	 */
	public void testInvalidEnumConstantTag24I() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest(TESTING_PACKAGE, "test54", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests a invalid @noinstantiate tag on enum constants in inner /outer enums
	 * using a full build
	 */
	public void testInvalidEnumConstantTag24F() {
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest(TESTING_PACKAGE, "test54", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
	
	/**
	 * Tests a valid @noinstantiate tag on an enum constant in an enum in the default package
	 * using an incremental build
	 */
	public void testInvalidEnumConstantTag25I() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test55", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests a valid @noinstantiate tag on an enum constant in an enum in the default package
	 * using a full build
	 */
	public void testInvalidEnumConstantTag25F() {
		setExpectedProblemIds(getDefaultProblemSet(1));
		deployTagTest(TESTING_PACKAGE, "test55", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}

	/**
	 * Tests an invalid @noinstantiate tag on enum constants in inner / outer enums
	 * using an incremental build
	 */
	public void testInvalidEnumConstantTag26I() {
		setExpectedProblemIds(getDefaultProblemSet(16));
		deployTagTest(TESTING_PACKAGE, "test56", true, IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
	}
	
	/**
	 * Tests a invalid @noinstantiate tag on enum constants in inner /outer enums
	 * using a full build
	 */
	public void testInvalidEnumConstantTag26F() {
		setExpectedProblemIds(getDefaultProblemSet(16));
		deployTagTest(TESTING_PACKAGE, "test56", true, IncrementalProjectBuilder.FULL_BUILD, true);
	}
}
