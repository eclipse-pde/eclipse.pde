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
import org.eclipse.pde.api.tools.internal.builder.BuilderMessages;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * Tests for the builder to ensure it find and reports unsupported
 * Javadoc tags on classes properly.
 * 
 * @since 1.0
 */
public class InvalidClassTagTests extends TagTest {

	/**
	 * Constructor
	 * @param name
	 */
	public InvalidClassTagTests(String name) {
		super(name);
	}
	
	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(InvalidClassTagTests.class);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("class");
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

	public void testInvalidClassTag1I() {
		x1(true);
	}
	
	public void testInvalidClassTag1F() {
		x1(false);
	}
	
	/**
	 * Tests having an @noreference tag on a variety of inner / outer / top-level classes in package a.b.c
	 * using a full build
	 */
	private void x1(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@noreference", BuilderMessages.TagValidator_a_class},
				{"@noreference", BuilderMessages.TagValidator_a_class},
				{"@noreference", BuilderMessages.TagValidator_a_class},
				{"@noreference", BuilderMessages.TagValidator_a_class}
		});
		deployTagTest(TESTING_PACKAGE,
				"test1", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}

	public void testInvalidClassTag2I() {
		x2(true);
	}

	public void testInvalidClassTag2F() {
		x2(false);
	}
	
	/**
	 * Tests having an @noreference tag on a class in the default package
	 * using an incremental build
	 */
	private void x2(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs(new String[][] {
				{"@noreference", BuilderMessages.TagValidator_a_class}
		});
		deployTagTest("",
				"test2", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}

	public void testInvalidClassTag3I() {
		x3(true);
	}
	
	public void testInvalidClassTag3F() {
		x3(false);
	}
	
	/**
	 * Tests having an @noimplement tag on a variety of inner / outer / top-level classes in package a.b.c
	 * using a full build
	 */
	private void x3(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@noimplement", BuilderMessages.TagValidator_a_class},
				{"@noimplement", BuilderMessages.TagValidator_a_class},
				{"@noimplement", BuilderMessages.TagValidator_a_class},
				{"@noimplement", BuilderMessages.TagValidator_a_class}
		});
		deployTagTest(TESTING_PACKAGE,
				"test3", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}

	public void testInvalidClassTag4I() {
		x4(true);
	}
	
	public void testInvalidClassTag4F() {
		x4(false);
	}
	
	/**
	 * Tests having an @noimplement tag on a class in the default package
	 * using a full build
	 */
	private void x4(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs(new String[][] {
				{"@noimplement", BuilderMessages.TagValidator_a_class}
		});
		deployTagTest("",
				"test4", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	public void testInvalidClassTag5I() {
		x5(true);
	}
	
	public void testInvalidClassTag5F() {
		x5(false);
	}
	
	/**
	 * Tests having an @nooverride tag on a variety of inner / outer / top-level classes in package a.b.c
	 * using a full build
	 */
	private void x5(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_a_class},
				{"@nooverride", BuilderMessages.TagValidator_a_class},
				{"@nooverride", BuilderMessages.TagValidator_a_class},
				{"@nooverride", BuilderMessages.TagValidator_a_class}
		});
		deployTagTest(TESTING_PACKAGE,
				"test5", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	public void testInvalidClassTag6I() {
		x6(true);
	}

	public void testInvalidClassTag6F() {
		x6(false);
	}
	
	/**
	 * Tests having an @nooverride tag on a class in the default package
	 * using an incremental build
	 */
	private void x6(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_a_class}
		});
		deployTagTest("",
				"test6", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
		
	public void testInvalidClassTag7I() {
		x7(true);
	}

	public void testInvalidClassTag7F() {
		x7(false);
	}
	
	/**
	 * Tests having an @noextend tag on a variety of inner / outer final classes
	 * using an incremental build
	 */
	private void x7(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(3));
		setExpectedMessageArgs(new String[][] {
				{"@noextend", BuilderMessages.TagValidator_a_final_class},
				{"@noextend", BuilderMessages.TagValidator_a_final_class},
				{"@noextend", BuilderMessages.TagValidator_a_final_class}
		});
		deployTagTest(TESTING_PACKAGE,
				"test7", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	public void testInvalidClassTag8I() {
		x8(true);
	}

	public void testInvalidClassTag8F() {
		x8(false);
	}
	
	/**
	 * Test having an @noinstantiate tag on an abstract class
	 */
	private void x8(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs(new String[][] {
				{"@noinstantiate", BuilderMessages.TagValidator_an_abstract_class},
		});
		deployTagTest(TESTING_PACKAGE,
				"test8", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
}
