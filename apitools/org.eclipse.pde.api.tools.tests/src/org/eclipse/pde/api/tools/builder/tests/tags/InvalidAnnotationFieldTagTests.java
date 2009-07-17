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

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.pde.api.tools.internal.builder.BuilderMessages;

/**
 * Tests the use of invalid tags in annotation fields and constants
 * 
 * @since 1.0
 */
public class InvalidAnnotationFieldTagTests extends InvalidFieldTagTests {

	/**
	 * Constructor
	 * @param name
	 */
	public InvalidAnnotationFieldTagTests(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.tags.InvalidFieldTagTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("annotation");
	}
	
	/**
	 * @return the test for this class
	 */
	public static Test suite() {
		return buildTestSuite(InvalidAnnotationFieldTagTests.class);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getTestCompliance()
	 */
	protected String getTestCompliance() {
		return CompilerOptions.VERSION_1_5;
	}
	
	public void testInvalidAnnotationFieldTag1I() {
		x1(true);
	}
		
	public void testInvalidAnnotationFieldTag1F() {
		x1(false);
	}
	
	/**
	 * Tests the unsupported @noextend tag on a variety of inner / outer annotation fields
	 */
	private void x1(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(3));
		setExpectedMessageArgs("@noextend", BuilderMessages.TagValidator_annotation_field, 3);
		String typename = "test1.java"; 
		deployTagTest(typename, inc, false);
	}
	
	public void testInvalidAnnotationFieldTag2I() {
		x2(true);
	}
	
	public void testInvalidAnnotationFieldTag2F() {
		x2(false);
	}
	
	/**
	 * Tests the unsupported @noextend tag on an annotation field in the default package
	 */
	private void x2(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs("@noextend", BuilderMessages.TagValidator_annotation_field, 1);
		String typename = "test2.java"; 
		deployTagTest(typename, inc, true);
	}
	
	public void testInvalidAnnotationFieldTag3I() {
		x3(true);
	}
	
	public void testInvalidAnnotationFieldTag3F() {
		x3(false);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on a variety of inner / outer annotation fields
	 */
	private void x3(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(3));
		setExpectedMessageArgs("@noinstantiate", BuilderMessages.TagValidator_annotation_field, 3);
		String typename = "test3.java"; 
		deployTagTest(typename, inc, false);
	}
	
	public void testInvalidAnnotationFieldTag4I() {
		x4(true);
	}
	
	public void testInvalidAnnotationFieldTag4F() {
		x4(false);
	}
	
	/**
	 * Tests the unsupported @noinstantiate tag on an annotation field in the default package
	 */
	private void x4(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs("@noinstantiate", BuilderMessages.TagValidator_annotation_field, 1);
		String typename = "test4.java"; 
		deployTagTest(typename, inc, true);
	}

	public void testInvalidAnnotationFieldTag5I() {
		x5(true);
	}

	public void testInvalidAnnotationFieldTag5F() {
		x5(false);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on a variety of inner / outer annotation fields
	 */
	private void x5(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(3));
		setExpectedMessageArgs("@noimplement", BuilderMessages.TagValidator_annotation_field, 3);
		String typename = "test5.java"; 
		deployTagTest(typename, inc, false);
	}
	
	public void testInvalidAnnotationFieldTag6I() {
		x6(true);
	}
	
	public void testInvalidAnnotationFieldTag6F() {
		x6(false);
	}
	
	/**
	 * Tests the unsupported @noimplement tag on an annotation field in the default package
	 */
	private void x6(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs("@noimplement", BuilderMessages.TagValidator_annotation_field, 1);
		String typename = "test6.java"; 
		deployTagTest(typename, inc, true);
	}

	public void testInvalidAnnotationFieldTag7I() {
		x7(true);
	}

	public void testInvalidAnnotationFieldTag7F() {
		x7(false);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on a variety of inner / outer annotation fields
	 */
	private void x7(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(3));
		setExpectedMessageArgs("@nooverride", BuilderMessages.TagValidator_annotation_field, 3);
		String typename = "test7.java"; 
		deployTagTest(typename, inc, false);
	}
	
	public void testInvalidAnnotationFieldTag8I() {
		x8(true);
	}
	
	public void testInvalidAnnotationFieldTag8F() {
		x8(false);
	}
	
	/**
	 * Tests the unsupported @nooverride tag on an annotation field in the default package
	 */
	private void x8(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs("@nooverride", BuilderMessages.TagValidator_annotation_field, 1);
		String typename = "test8.java"; 
		deployTagTest(typename, inc, true);
	}
	
	public void testInvalidAnnotationFieldTag9I() {
		x9(true);
	}
	
	public void testInvalidAnnotationFieldTag9F() {
		x9(false);
	}
	
	/**
	 * Tests all the unsupported tags on a variety of annotation fields
	 */
	private void x9(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(15));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_annotation_field},
				{"@noimplement", BuilderMessages.TagValidator_annotation_field},
				{"@noinstantiate", BuilderMessages.TagValidator_annotation_field},
				{"@noextend", BuilderMessages.TagValidator_annotation_field},
				{"@noreference", BuilderMessages.TagValidator_annotation_field},
				{"@nooverride", BuilderMessages.TagValidator_annotation_field},
				{"@noimplement", BuilderMessages.TagValidator_annotation_field},
				{"@noinstantiate", BuilderMessages.TagValidator_annotation_field},
				{"@noextend", BuilderMessages.TagValidator_annotation_field},
				{"@noreference", BuilderMessages.TagValidator_annotation_field},
				{"@nooverride", BuilderMessages.TagValidator_annotation_field},
				{"@noimplement", BuilderMessages.TagValidator_annotation_field},
				{"@noinstantiate", BuilderMessages.TagValidator_annotation_field},
				{"@noextend", BuilderMessages.TagValidator_annotation_field},
				{"@noreference", BuilderMessages.TagValidator_annotation_field}
		});
		String typename = "test9.java"; 
		deployTagTest(typename, inc, false);
	}
	
	public void testInvalidAnnotationFieldTag10I() {
		x10(true);
	}
	
	public void testInvalidAnnotationFieldTag10F() {
		x10(false);
	}
	
	/**
	 * Tests the unsupported @noreference tag on a variety of final inner / outer annotation fields
	 */
	private void x10(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(3));
		setExpectedMessageArgs("@noreference", BuilderMessages.TagValidator_a_final_annotation_field, 3);
		String typename = "test10.java"; 
		deployTagTest(typename, inc, false);
	}

	public void testInvalidAnnotationFieldTag11I() {
		x11(true);
	}
	
	public void testInvalidAnnotationFieldTag11F() {
		x11(false);
	}
	
	/**
	 * Tests the unsupported @noreference tag on a final annotation field in the default package
	 */
	private void x11(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs("@noreference", BuilderMessages.TagValidator_a_final_annotation_field, 1);
		String typename = "test11.java"; 
		deployTagTest(typename, inc, true);
	}
}
