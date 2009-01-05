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
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * Tests that the builder finds and properly reports invalid tags on enum fields
 *  
 *  @since 1.0
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
		return ApiProblemFactory.createProblemId(
				IApiProblem.CATEGORY_USAGE, 
				IElementDescriptor.TYPE, 
				IApiProblem.UNSUPPORTED_TAG_USE, 
				IApiProblem.NO_FLAGS);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getTestCompliance()
	 */
	protected String getTestCompliance() {
		return CompilerOptions.VERSION_1_5;
	}
	
	public void testInvalidEnumTag1I() {
		x1(true);
	}
	
	public void testInvalidEnumTag1F() {
		x1(false);
	}
	
	/**
	 * Tests having an @noreference tag on a variety of inner / outer / top-level enums in package a.b.c
	 */
	private void x1(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@noreference", BuilderMessages.TagValidator_an_enum},
				{"@noreference", BuilderMessages.TagValidator_an_enum},
				{"@noreference", BuilderMessages.TagValidator_an_enum},
				{"@noreference", BuilderMessages.TagValidator_an_enum}
		});
		deployTagTest(TESTING_PACKAGE, 
				"test1", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	public void testInvalidEnumTag2I() {
		x2(true);
	}
	
	public void testInvalidEnumTag2F() {
		x2(false);
	}
	
	/**
	 * Tests having an @noreference tag on an enum in the default package
	 */
	private void x2(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs(new String[][] {
				{"@noreference", BuilderMessages.TagValidator_an_enum}
		});
		deployTagTest("", 
				"test2", 
				true, 
				inc ? IncrementalProjectBuilder.FULL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	public void testInvalidEnumTag3I() {
		x3(true);
	}
	
	public void testInvalidEnumTag3F() {
		x3(false);
	}
	
	/**
	 * Tests having an @noextend tag on a variety of inner / outer / top-level enums in package a.b.c
	 */
	private void x3(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@noextend", BuilderMessages.TagValidator_an_enum},
				{"@noextend", BuilderMessages.TagValidator_an_enum},
				{"@noextend", BuilderMessages.TagValidator_an_enum},
				{"@noextend", BuilderMessages.TagValidator_an_enum}
		});
		deployTagTest(TESTING_PACKAGE, 
				"test3", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}

	public void testInvalidEnumTag4I() {
		x4(true);
	}
	
	public void testInvalidEnumTag4F() {
		x4(false);
	}
	
	/**
	 * Tests having an @noextend tag on an enum in the default package
	 */
	private void x4(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs(new String[][] {
				{"@noextend", BuilderMessages.TagValidator_an_enum}
		});
		deployTagTest("", 
				"test4", 
				true, 
				inc ? IncrementalProjectBuilder.FULL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}

	public void testInvalidEnumTag5I() {
		x5(true);
	}
	
	public void testInvalidEnumTag5F() {
		x5(false);
	}
	
	/**
	 * Tests having an @nooverride tag on a variety of inner / outer / top-level enums in package a.b.c
	 */
	private void x5(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_an_enum},
				{"@nooverride", BuilderMessages.TagValidator_an_enum},
				{"@nooverride", BuilderMessages.TagValidator_an_enum},
				{"@nooverride", BuilderMessages.TagValidator_an_enum}
		});
		deployTagTest(TESTING_PACKAGE, 
				"test5", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}

	public void testInvalidEnumTag6I() {
		x6(true);
	}
	
	public void testInvalidEnumTag6F() {
		x6(false);
	}
	
	/**
	 * Tests having an @nooverride tag on an enum in the default package
	 */
	private void x6(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_an_enum}
		});
		deployTagTest("", 
				"test6", 
				true, 
				inc ? IncrementalProjectBuilder.FULL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	public void testInvalidEnumTag7I() {
		x7(true);
	}
	
	public void testInvalidEnumTag7F() {
		x7(false);
	}
	
	/**
	 * Tests having an @noinstantiate on a variety of inner / outer / top-level enums in package a.b.c
	 */
	private void x7(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@noinstantiate", BuilderMessages.TagValidator_an_enum},
				{"@noinstantiate", BuilderMessages.TagValidator_an_enum},
				{"@noinstantiate", BuilderMessages.TagValidator_an_enum},
				{"@noinstantiate", BuilderMessages.TagValidator_an_enum}
		});
		deployTagTest(TESTING_PACKAGE, 
				"test7", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	public void testInvalidEnumTag8I() {
		x8(true);
	}
	
	public void testInvalidEnumTag8F() {
		x8(false);
	}
	
	/**
	 * Tests having an @noinstantiate on an enum in the default package
	 */
	private void x8(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs(new String[][] {
				{"@noinstantiate", BuilderMessages.TagValidator_an_enum}
		});
		deployTagTest("", 
				"test8", 
				true, 
				inc ? IncrementalProjectBuilder.FULL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}

	public void testInvalidEnumTag9I() {
		x9(true);
	}
	
	public void testInvalidEnumTag9F() {
		x9(false);
	}
	
	/**
	 * Tests having an @noimplement tag on a variety of inner / outer / top-level enums in package a.b.c
	 */
	private void x9(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@noimplement", BuilderMessages.TagValidator_an_enum},
				{"@noimplement", BuilderMessages.TagValidator_an_enum},
				{"@noimplement", BuilderMessages.TagValidator_an_enum},
				{"@noimplement", BuilderMessages.TagValidator_an_enum}
		});
		deployTagTest(TESTING_PACKAGE, 
				"test9", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	public void testInvalidEnumTag10I() {
		x10(true);
	}
	
	public void testInvalidEnumTag10F() {
		x10(false);
	}
	
	/**
	 * Tests having an @noimplement tag on an enum in the default package
	 */
	private void x10(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs(new String[][] {
				{"@noimplement", BuilderMessages.TagValidator_an_enum}
		});
		deployTagTest("", 
				"test10", 
				true, 
				inc ? IncrementalProjectBuilder.FULL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
}
