/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
import org.eclipse.pde.api.tools.internal.builder.BuilderMessages;

/**
 * Tests the use of invalid tags on enum constants
 * 
 * @since 1.0
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
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getTestCompliance()
	 */
	protected String getTestCompliance() {
		return CompilerOptions.VERSION_1_5;
	}
	
	public void testInvalidEnumConstantTag1I() {
		x1(true);
	}
	
	public void testInvalidEnumConstantTag1F() {
		x1(false);
	}
	
	/**
	 * Tests a invalid @noreference tag on enum constants in inner / outer enums
	 * using a full build
	 */
	private void x1(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@noreference", BuilderMessages.TagValidator_an_enum_constant},
				{"@noreference", BuilderMessages.TagValidator_an_enum_constant},
				{"@noreference", BuilderMessages.TagValidator_an_enum_constant},
				{"@noreference", BuilderMessages.TagValidator_an_enum_constant}
		});
		deployTagTest(TESTING_PACKAGE, 
				"test13", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	/**
	 * Tests a valid @noreference tag on an enum constant in an enum in the default package
	 * using an incremental build
	 */
	public void testInvalidEnumConstantTag2I() {
		x2(true);
	}
	
	/**
	 * Tests a valid @noreference tag on an enum constant in an enum in the default package
	 * using a full build
	 */
	public void testInvalidEnumConstantTag2F() {
		x2(false);
	}
	
	private void x2(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs(new String[][] {
				{"@noreference", BuilderMessages.TagValidator_an_enum_constant}
		});
		deployTagTest("", 
				"test14", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}

	public void testInvalidEnumConstantTag3I() {
		x3(true);
	}

	public void testInvalidEnumConstantTag3F() {
		x3(false);
	}
	
	/**
	 * Tests an invalid @noextend tag on enum constants in inner / outer enums
	 * using an incremental build
	 */
	private void x3(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@noextend", BuilderMessages.TagValidator_an_enum_constant},
				{"@noextend", BuilderMessages.TagValidator_an_enum_constant},
				{"@noextend", BuilderMessages.TagValidator_an_enum_constant},
				{"@noextend", BuilderMessages.TagValidator_an_enum_constant}
		});
		deployTagTest(TESTING_PACKAGE, 
				"test15", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}

	public void testInvalidEnumConstantTag4I() {
		x4(false);
	}

	public void testInvalidEnumConstantTag4F() {
		x4(false);
	}

	/**
	 * Tests a valid @noextend tag on an enum constant in an enum in the default package
	 * using a full build
	 */
	private void x4(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs(new String[][] {
				{"@noextend", BuilderMessages.TagValidator_an_enum_constant}
		});
		deployTagTest("", 
				"test16", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	public void testInvalidEnumConstantTag5I() {
		x5(true);
	}

	public void testInvalidEnumConstantTag5F() {
		x5(false);
	}
	
	/**
	 * Tests an invalid @noimplement tag on enum constants in inner / outer enums
	 * using an incremental build
	 */
	private void x5(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@noimplement", BuilderMessages.TagValidator_an_enum_constant},
				{"@noimplement", BuilderMessages.TagValidator_an_enum_constant},
				{"@noimplement", BuilderMessages.TagValidator_an_enum_constant},
				{"@noimplement", BuilderMessages.TagValidator_an_enum_constant}
		});
		deployTagTest(TESTING_PACKAGE, 
				"test17", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	/**
	 * Tests a valid @noimplement tag on an enum constant in an enum in the default package
	 * using an incremental build
	 */
	public void testInvalidEnumConstantTag6I() {
		x6(true);
	}

	public void testInvalidEnumConstantTag6F() {
		x6(false);
	}
	
	/**
	 * Tests a valid @noimplement tag on an enum constant in an enum in the default package
	 * using a full build
	 */
	private void x6(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs(new String[][] {
				{"@noimplement", BuilderMessages.TagValidator_an_enum_constant}
		});
		deployTagTest("", 
				"test18", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}

	public void testInvalidEnumConstantTag7I() {
		x7(true);
	}
	
	public void testInvalidEnumConstantTag7F() {
		x7(false);
	}
	
	/**
	 * Tests a invalid @nooverride tag on enum constants in inner /outer enums
	 * using a full build
	 */
	private void x7(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_an_enum_constant},
				{"@nooverride", BuilderMessages.TagValidator_an_enum_constant},
				{"@nooverride", BuilderMessages.TagValidator_an_enum_constant},
				{"@nooverride", BuilderMessages.TagValidator_an_enum_constant}
		});
		deployTagTest(TESTING_PACKAGE, 
				"test19", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	public void testInvalidEnumConstantTag8I() {
		x8(true);
	}
	
	public void testInvalidEnumConstantTag8F() {
		x8(false);
	}
	
	/**
	 * Tests a valid @nooverride tag on an enum constant in an enum in the default package
	 * using an incremental build
	 */
	private void x8(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_an_enum_constant}
		});
		deployTagTest("", 
				"test20", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	public void testInvalidEnumConstantTag9I() {
		x9(true);
	}

	public void testInvalidEnumConstantTag9F() {
		x9(false);
	}
	
	/**
	 * Tests an invalid @noinstantiate tag on enum constants in inner / outer enums
	 * using an incremental build
	 */
	private void x9(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@noinstantiate", BuilderMessages.TagValidator_an_enum_constant},
				{"@noinstantiate", BuilderMessages.TagValidator_an_enum_constant},
				{"@noinstantiate", BuilderMessages.TagValidator_an_enum_constant},
				{"@noinstantiate", BuilderMessages.TagValidator_an_enum_constant}
		});
		deployTagTest(TESTING_PACKAGE, 
				"test21", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}

	public void testInvalidEnumConstantTag10I() {
		x10(true);
	}
	
	public void testInvalidEnumConstantTag10F() {
		x10(false);
	}
	
	/**
	 * Tests a valid @noinstantiate tag on an enum constant in an enum in the default package
	 * using a full build
	 */
	private void x10(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs(new String[][] {
				{"@noinstantiate", BuilderMessages.TagValidator_an_enum_constant}
		});
		deployTagTest("", 
				"test22", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
}
