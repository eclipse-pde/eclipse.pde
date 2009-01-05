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
 * Tests unsupported javadoc tags for annotations
 * 
 * @since 1.0
 */
public class InvalidAnnotationTagTests extends TagTest {

	/**
	 * Constructor
	 * @param name
	 */
	public InvalidAnnotationTagTests(String name) {
		super(name);
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(InvalidAnnotationTagTests.class);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("annotation");
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.tags.TagTest#getDefaultProblemId()
	 */
	protected int getDefaultProblemId() {
		return ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.UNSUPPORTED_TAG_USE, IApiProblem.NO_FLAGS);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getTestCompliance()
	 */
	protected String getTestCompliance() {
		return CompilerOptions.VERSION_1_5;
	}
	
	
	public void testInvalidAnnotationTag1I() {
		x1(true);
	}
	
	public void testInvalidAnnotationTag1F() {
		x1(false);
	}
	
	/**
	 * Tests having an @noreference tag on a variety of annotations in package a.b.c
	 */
	private void x1(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs("@noreference", BuilderMessages.TagValidator_an_annotation, 4);
		deployTagTest(TESTING_PACKAGE, 
				"test1", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	
	public void testInvalidAnnotationTag2I() {
		x2(true);
	}

	public void testInvalidAnnotationTag2F() {
		x2(false);
	}
	
	/**
	 * Tests having an @noreference tag on an annotation in the default package
	 */
	private void x2(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs("@noreference", BuilderMessages.TagValidator_an_annotation, 1);
		deployTagTest("", 
				"test2", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	public void testInvalidAnnotationTag3I() {
		x3(true);
	}
	
	public void testInvalidAnnotationTag3F() {
		x3(false);
	}
	
	/**
	 * Tests having an @noextend tag on a variety of inner / outer / top-level annotations in package a.b.c
	 */
	private void x3(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs("@noextend", BuilderMessages.TagValidator_an_annotation, 4);
		deployTagTest(TESTING_PACKAGE, 
				"test3", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	public void testInvalidAnnotationTag4I() {
		x4(true);
	}

	public void testInvalidAnnotationTag4F() {
		x4(false);
	}
	
	/**
	 * Tests having an @noextend tag on an annotation in the default package
	 */
	private void x4(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs("@noextend", BuilderMessages.TagValidator_an_annotation, 1);
		deployTagTest("", 
				"test4", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	public void testInvalidAnnotationTag5I() {
		x5(true);
	}
	
	public void testInvalidAnnotationTag5F() {
		x5(false);
	}
	
	/**
	 * Tests having an @nooverride tag on a variety of inner / outer / top-level annotations in package a.b.c
	 */
	private void x5(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs("@nooverride", BuilderMessages.TagValidator_an_annotation, 4);
		deployTagTest(TESTING_PACKAGE, 
				"test5", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}

	public void testInvalidAnnotationTag6I() {
		x6(true);
	}
	
	public void testInvalidAnnotationTag6F() {
		x6(false);
	}
	
	/**
	 * Tests having an @nooverride tag on an annotation in the default package
	 */
	private void x6(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs("@nooverride", BuilderMessages.TagValidator_an_annotation, 1);
		deployTagTest("", 
				"test6", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}

	public void testInvalidAnnotationTag7I() {
		x7(true);
	}

	public void testInvalidAnnotationTag7F() {
		x7(false);
	}
	
	/**
	 * Tests having a variety of invalid tags on a variety of inner / outer / top-level annotations in package a.b.c
	 */
	private void x7(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(16));
		setExpectedMessageArgs(new String[][] {
				{"@noinstantiate", BuilderMessages.TagValidator_an_annotation},
				{"@noreference", BuilderMessages.TagValidator_an_annotation},
				{"@noextend", BuilderMessages.TagValidator_an_annotation},
				{"@nooverride", BuilderMessages.TagValidator_an_annotation},
				{"@noinstantiate", BuilderMessages.TagValidator_an_annotation},
				{"@noreference", BuilderMessages.TagValidator_an_annotation},
				{"@noextend", BuilderMessages.TagValidator_an_annotation},
				{"@nooverride", BuilderMessages.TagValidator_an_annotation},
				{"@noinstantiate", BuilderMessages.TagValidator_an_annotation},
				{"@noreference", BuilderMessages.TagValidator_an_annotation},
				{"@noextend", BuilderMessages.TagValidator_an_annotation},
				{"@nooverride", BuilderMessages.TagValidator_an_annotation},
				{"@noinstantiate", BuilderMessages.TagValidator_an_annotation},
				{"@noreference", BuilderMessages.TagValidator_an_annotation},
				{"@noextend", BuilderMessages.TagValidator_an_annotation},
				{"@nooverride", BuilderMessages.TagValidator_an_annotation},
		});
		deployTagTest(TESTING_PACKAGE, 
				"test7", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	public void testInvalidAnnotationTag8I() {
		x8(true);
	}
	
	public void testInvalidAnnotationTag8F() {
		x8(false);
	}
	
	/**
	 * Tests having more than one invalid tag on an annotation in the default package
	 */
	private void x8(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@noinstantiate", BuilderMessages.TagValidator_an_annotation},
				{"@noreference", BuilderMessages.TagValidator_an_annotation},
				{"@noextend", BuilderMessages.TagValidator_an_annotation},
				{"@nooverride", BuilderMessages.TagValidator_an_annotation},
		});
		deployTagTest("", 
				"test8", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	public void testInvalidAnnotationTag9I() {
		x9(true);
	}
	
	public void testInvalidAnnotationTag9F() {
		x9(false);
	}
	
	/**
	 * Tests having an @noinstantiate tag on a variety of inner / outer / top-level annotations in package a.b.c
	 */
	private void x9(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs("@noinstantiate", BuilderMessages.TagValidator_an_annotation, 4);
		deployTagTest(TESTING_PACKAGE, 
				"test9", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	public void testInvalidAnnotationTag10I() {
		x10(true);
	}
	
	public void testInvalidAnnotationTag10F() {
		x10(false);
	}
	
	/**
	 * Tests having an @noinstantiate tag on an annotation in the default package
	 */
	private void x10(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs("@noinstantiate", BuilderMessages.TagValidator_an_annotation, 1);
		deployTagTest("", 
				"test10", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
	
	public void testInvalidAnnotationTag11I() {
		x11(true);
	}
	
	public void testInvalidAnnotationTag11F() {
		x11(false);
	}
	
	/**
	 * Tests having an @noimplement tag on a variety of inner / outer / top-level annotations in package a.b.c
	 */
	private void x11(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs("@noimplement", BuilderMessages.TagValidator_an_annotation, 4);
		deployTagTest(TESTING_PACKAGE, 
				"test11", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}

	public void testInvalidAnnotationTag12I() {
		x12(true);
	}
	
	public void testInvalidAnnotationTag12F() {
		x12(false);
	}
	
	/**
	 * Tests having an @noimplement tag on an annotation in the default package
	 */
	private void x12(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs("@noimplement", BuilderMessages.TagValidator_an_annotation, 1);
		deployTagTest("", 
				"test12", 
				true, 
				inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD, 
				true);
	}
}