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
 * Tests the use of invalid tags on enum fields
 * 
 * @since 1.0
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
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getTestCompliance()
	 */
	protected String getTestCompliance() {
		return CompilerOptions.VERSION_1_5;
	}
	
	public void testInvalidEnumFieldTag1I() {
		x1(true);
	}

	public void testInvalidEnumFieldTag1F() {
		x1(false);
	}
	
	/**
	 * Tests an invalid @noreference tag on three final fields in an enum
	 * using an incremental build
	 */
	private void x1(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(12));
		setExpectedMessageArgs(new String[][] {
				{"@noreference", BuilderMessages.TagValidator_enum_field},
				{"@noreference", BuilderMessages.TagValidator_enum_field},
				{"@noreference", BuilderMessages.TagValidator_enum_field},
				{"@noreference", BuilderMessages.TagValidator_enum_field},
				{"@noreference", BuilderMessages.TagValidator_enum_field},
				{"@noreference", BuilderMessages.TagValidator_enum_field},
				{"@noreference", BuilderMessages.TagValidator_enum_field},
				{"@noreference", BuilderMessages.TagValidator_enum_field},
				{"@noreference", BuilderMessages.TagValidator_private_enum_field},
				{"@noreference", BuilderMessages.TagValidator_private_enum_field},
				{"@noreference", BuilderMessages.TagValidator_private_enum_field},
				{"@noreference", BuilderMessages.TagValidator_private_enum_field}
		});
		deployTagTest(TESTING_PACKAGE, 
				"test1", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}

	public void testInvalidEnumFieldTag2I() {
		x2(true);
	}
	
	public void testInvalidEnumFieldTag2F() {
		x2(false);
	}
	
	/**
	 * Tests a valid @noreference tag on three final fields in an enum in the default package
	 * using a full build
	 */
	private void x2(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(3));
		setExpectedMessageArgs(new String[][] {
				{"@noreference", BuilderMessages.TagValidator_enum_field},
				{"@noreference", BuilderMessages.TagValidator_enum_field},
				{"@noreference", BuilderMessages.TagValidator_private_enum_field}
		});
		deployTagTest("", 
				"test2", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}

	public void testInvalidEnumFieldTag3I() {
		x3(true);
	}
	
	public void testInvalidEnumFieldTag3F() {
		x3(false);
	}
	
	/**
	 * Tests a invalid @noreference tag on static final fields in inner /outer enums
	 * using a full build
	 */
	private void x3(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(12));
		setExpectedMessageArgs(new String[][] {
				{"@noreference", BuilderMessages.TagValidator_enum_field},
				{"@noreference", BuilderMessages.TagValidator_enum_field},
				{"@noreference", BuilderMessages.TagValidator_enum_field},
				{"@noreference", BuilderMessages.TagValidator_enum_field},
				{"@noreference", BuilderMessages.TagValidator_enum_field},
				{"@noreference", BuilderMessages.TagValidator_enum_field},
				{"@noreference", BuilderMessages.TagValidator_enum_field},
				{"@noreference", BuilderMessages.TagValidator_enum_field},
				{"@noreference", BuilderMessages.TagValidator_private_enum_field},
				{"@noreference", BuilderMessages.TagValidator_private_enum_field},
				{"@noreference", BuilderMessages.TagValidator_private_enum_field},
				{"@noreference", BuilderMessages.TagValidator_private_enum_field}
		});
		deployTagTest(TESTING_PACKAGE, 
				"test3", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}

	public void testInvalidEnumFieldTag4I() {
		x4(true);
	}
	
	public void testInvalidEnumFieldTag4F() {
		x4(false);	
	}
	
	/**
	 * Tests a valid @noreference tag on three static final fields in an enum in the default package
	 * using a full build
	 */
	private void x4(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(3));
		setExpectedMessageArgs(new String[][] {
				{"@noreference", BuilderMessages.TagValidator_enum_field},
				{"@noreference", BuilderMessages.TagValidator_enum_field},
				{"@noreference", BuilderMessages.TagValidator_private_enum_field}
		});
		deployTagTest("", 
				"test4", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	public void testInvalidEnumFieldTag5I() {
		x5(true);
	}
	
	public void testInvalidEnumFieldTag5F() {
		x5(false);
	}
	
	/**
	 * Tests an invalid @noextend tag on fields in inner / outer enums
	 * using an incremental build
	 */
	private void x5(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(12));
		setExpectedMessageArgs(new String[][] {
				{"@noextend", BuilderMessages.TagValidator_enum_field},
				{"@noextend", BuilderMessages.TagValidator_enum_field},
				{"@noextend", BuilderMessages.TagValidator_enum_field},
				{"@noextend", BuilderMessages.TagValidator_enum_field},
				{"@noextend", BuilderMessages.TagValidator_enum_field},
				{"@noextend", BuilderMessages.TagValidator_enum_field},
				{"@noextend", BuilderMessages.TagValidator_enum_field},
				{"@noextend", BuilderMessages.TagValidator_enum_field},
				{"@noextend", BuilderMessages.TagValidator_private_enum_field},
				{"@noextend", BuilderMessages.TagValidator_private_enum_field},
				{"@noextend", BuilderMessages.TagValidator_private_enum_field},
				{"@noextend", BuilderMessages.TagValidator_private_enum_field}
		});
		deployTagTest(TESTING_PACKAGE, 
				"test5", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}

	public void testInvalidEnumFieldTag6I() {
		x6(true);
	}

	public void testInvalidEnumFieldTag6F() {
		x6(false);
	}
	
	/**
	 * Tests a valid @noextend tag on three fields in an enum in the default package
	 * using a full build
	 */
	private void x6(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(3));
		setExpectedMessageArgs(new String[][] {
				{"@noextend", BuilderMessages.TagValidator_enum_field},
				{"@noextend", BuilderMessages.TagValidator_enum_field},
				{"@noextend", BuilderMessages.TagValidator_private_enum_field}
		});
		deployTagTest("", 
				"test6", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	public void testInvalidEnumFieldTag7I() {
		x7(true);
	}

	public void testInvalidEnumFieldTag7F() {
		x7(false);
	}
	
	/**
	 * Tests a invalid @noimplement tag on fields in inner /outer enums
	 * using a full build
	 */
	private void x7(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(12));
		setExpectedMessageArgs(new String[][] {
				{"@noimplement", BuilderMessages.TagValidator_enum_field},
				{"@noimplement", BuilderMessages.TagValidator_enum_field},
				{"@noimplement", BuilderMessages.TagValidator_enum_field},
				{"@noimplement", BuilderMessages.TagValidator_enum_field},
				{"@noimplement", BuilderMessages.TagValidator_enum_field},
				{"@noimplement", BuilderMessages.TagValidator_enum_field},
				{"@noimplement", BuilderMessages.TagValidator_enum_field},
				{"@noimplement", BuilderMessages.TagValidator_enum_field},
				{"@noimplement", BuilderMessages.TagValidator_private_enum_field},
				{"@noimplement", BuilderMessages.TagValidator_private_enum_field},
				{"@noimplement", BuilderMessages.TagValidator_private_enum_field},
				{"@noimplement", BuilderMessages.TagValidator_private_enum_field}
		});
		deployTagTest(TESTING_PACKAGE, 
				"test7", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}

	public void testInvalidEnumFieldTag8I() {
		x8(true);
	}
	
	public void testInvalidEnumFieldTag8F() {
		x8(false);
	}
	
	/**
	 * Tests a valid @noimplement tag on three fields in an enum in the default package
	 * using a full build
	 */
	private void x8(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(3));
		setExpectedMessageArgs(new String[][] {
				{"@noimplement", BuilderMessages.TagValidator_enum_field},
				{"@noimplement", BuilderMessages.TagValidator_enum_field},
				{"@noimplement", BuilderMessages.TagValidator_private_enum_field}
		});
		deployTagTest("", 
				"test8", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	public void testInvalidEnumFieldTag9I() {
		x9(true);
	}

	public void testInvalidEnumFieldTag9F() {
		x9(false);
	}
	
	/**
	 * Tests an invalid @nooverride tag on fields in inner / outer enums
	 * using an incremental build
	 */
	private void x9(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(12));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_enum_field},
				{"@nooverride", BuilderMessages.TagValidator_enum_field},
				{"@nooverride", BuilderMessages.TagValidator_enum_field},
				{"@nooverride", BuilderMessages.TagValidator_enum_field},
				{"@nooverride", BuilderMessages.TagValidator_enum_field},
				{"@nooverride", BuilderMessages.TagValidator_enum_field},
				{"@nooverride", BuilderMessages.TagValidator_enum_field},
				{"@nooverride", BuilderMessages.TagValidator_enum_field},
				{"@nooverride", BuilderMessages.TagValidator_private_enum_field},
				{"@nooverride", BuilderMessages.TagValidator_private_enum_field},
				{"@nooverride", BuilderMessages.TagValidator_private_enum_field},
				{"@nooverride", BuilderMessages.TagValidator_private_enum_field}
		});
		deployTagTest(TESTING_PACKAGE, 
				"test9", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	public void testInvalidEnumFieldTag10I() {
		x10(true);
	}

	public void testInvalidEnumFieldTag10F() {
		x10(false);
	}
	
	/**
	 * Tests a valid @nooverride tag on three fields in an enum in the default package
	 * using an incremental build
	 */
	private void x10(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(3));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_enum_field},
				{"@nooverride", BuilderMessages.TagValidator_enum_field},
				{"@nooverride", BuilderMessages.TagValidator_private_enum_field}
		});
		deployTagTest("", 
				"test10", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	public void testInvalidEnumFieldTag11I() {
		x11(true);
	}
	
	public void testInvalidEnumFieldTag11F() {
		x11(false);
	}
	
	/**
	 * Tests a invalid @noinstantiate tag on fields in inner /outer enums
	 * using a full build
	 */
	private void x11(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(12));
		setExpectedMessageArgs(new String[][] {
				{"@noinstantiate", BuilderMessages.TagValidator_enum_field},
				{"@noinstantiate", BuilderMessages.TagValidator_enum_field},
				{"@noinstantiate", BuilderMessages.TagValidator_enum_field},
				{"@noinstantiate", BuilderMessages.TagValidator_enum_field},
				{"@noinstantiate", BuilderMessages.TagValidator_enum_field},
				{"@noinstantiate", BuilderMessages.TagValidator_enum_field},
				{"@noinstantiate", BuilderMessages.TagValidator_enum_field},
				{"@noinstantiate", BuilderMessages.TagValidator_enum_field},
				{"@noinstantiate", BuilderMessages.TagValidator_private_enum_field},
				{"@noinstantiate", BuilderMessages.TagValidator_private_enum_field},
				{"@noinstantiate", BuilderMessages.TagValidator_private_enum_field},
				{"@noinstantiate", BuilderMessages.TagValidator_private_enum_field}
		});
		deployTagTest(TESTING_PACKAGE, 
				"test11", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	public void testInvalidEnumFieldTag12I() {
		x12(true);
	}
	
	public void testInvalidEnumFieldTag12F() {
		x12(false);
	}

	/**
	 * Tests a valid @noinstantiate tag on three fields in an enum in the default package
	 * using a full build
	 */
	private void x12(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(3));
		setExpectedMessageArgs(new String[][] {
				{"@noinstantiate", BuilderMessages.TagValidator_enum_field},
				{"@noinstantiate", BuilderMessages.TagValidator_enum_field},
				{"@noinstantiate", BuilderMessages.TagValidator_private_enum_field}
		});
		deployTagTest("", 
				"test12", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
}
