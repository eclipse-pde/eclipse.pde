/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.builder.tests.tags;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.api.tools.internal.builder.BuilderMessages;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

import junit.framework.Test;

/**
 * Tests unsupported javadoc tags for annotations
 *
 * @since 1.0
 */
public class InvalidAnnotationTagTests extends TagTest {

	public InvalidAnnotationTagTests(String name) {
		super(name);
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(InvalidAnnotationTagTests.class);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("annotation"); //$NON-NLS-1$
	}

	@Override
	protected int getDefaultProblemId() {
		return ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.UNSUPPORTED_TAG_USE, IApiProblem.NO_FLAGS);
	}

	@Override
	protected String getTestCompliance() {
		return JavaCore.VERSION_1_5;
	}

	public void testInvalidAnnotationTag3I() {
		x3(true);
	}

	public void testInvalidAnnotationTag3F() {
		x3(false);
	}

	/**
	 * Tests having an @noextend tag on a variety of inner / outer / top-level
	 * annotations in package a.b.c
	 */
	private void x3(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs("@noextend", BuilderMessages.TagValidator_an_annotation, 4); //$NON-NLS-1$
		String typename = "test3.java"; //$NON-NLS-1$
		deployTagTest(typename, inc, false);
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
		setExpectedMessageArgs("@noextend", BuilderMessages.TagValidator_an_annotation, 1); //$NON-NLS-1$
		String typename = "test4.java"; //$NON-NLS-1$
		deployTagTest(typename, inc, true);
	}

	public void testInvalidAnnotationTag5I() {
		x5(true);
	}

	public void testInvalidAnnotationTag5F() {
		x5(false);
	}

	/**
	 * Tests having an @nooverride tag on a variety of inner / outer / top-level
	 * annotations in package a.b.c
	 */
	private void x5(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs("@nooverride", BuilderMessages.TagValidator_an_annotation, 4); //$NON-NLS-1$
		String typename = "test5.java"; //$NON-NLS-1$
		deployTagTest(typename, inc, false);
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
		setExpectedMessageArgs("@nooverride", BuilderMessages.TagValidator_an_annotation, 1); //$NON-NLS-1$
		String typename = "test6.java"; //$NON-NLS-1$
		deployTagTest(typename, inc, true);
	}

	public void testInvalidAnnotationTag7I() {
		x7(true);
	}

	public void testInvalidAnnotationTag7F() {
		x7(false);
	}

	/**
	 * Tests having a variety of invalid tags on a variety of inner / outer /
	 * top-level annotations in package a.b.c
	 */
	private void x7(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(12));
		setExpectedMessageArgs(new String[][] {
				{ "@noinstantiate", BuilderMessages.TagValidator_an_annotation }, //$NON-NLS-1$
				/* {"@noreference", BuilderMessages.TagValidator_an_annotation}, */
				{ "@noextend", BuilderMessages.TagValidator_an_annotation }, //$NON-NLS-1$
				{ "@nooverride", BuilderMessages.TagValidator_an_annotation }, //$NON-NLS-1$
				{ "@noinstantiate", BuilderMessages.TagValidator_an_annotation }, //$NON-NLS-1$
				/* {"@noreference", BuilderMessages.TagValidator_an_annotation}, */
				{ "@noextend", BuilderMessages.TagValidator_an_annotation }, //$NON-NLS-1$
				{ "@nooverride", BuilderMessages.TagValidator_an_annotation }, //$NON-NLS-1$
				{ "@noinstantiate", BuilderMessages.TagValidator_an_annotation }, //$NON-NLS-1$
				/* {"@noreference", BuilderMessages.TagValidator_an_annotation}, */
				{ "@noextend", BuilderMessages.TagValidator_an_annotation }, //$NON-NLS-1$
				{ "@nooverride", BuilderMessages.TagValidator_an_annotation }, //$NON-NLS-1$
				{ "@noinstantiate", BuilderMessages.TagValidator_an_annotation }, //$NON-NLS-1$
				/* {"@noreference", BuilderMessages.TagValidator_an_annotation}, */
				{ "@noextend", BuilderMessages.TagValidator_an_annotation }, //$NON-NLS-1$
				{ "@nooverride", BuilderMessages.TagValidator_an_annotation }, //$NON-NLS-1$
		});
		String typename = "test7.java"; //$NON-NLS-1$
		deployTagTest(typename, inc, false);
	}

	public void testInvalidAnnotationTag8I() {
		x8(true);
	}

	public void testInvalidAnnotationTag8F() {
		x8(false);
	}

	/**
	 * Tests having more than one invalid tag on an annotation in the default
	 * package
	 */
	private void x8(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(3));
		setExpectedMessageArgs(new String[][] {
				{ "@noinstantiate", BuilderMessages.TagValidator_an_annotation }, //$NON-NLS-1$
				/* {"@noreference", BuilderMessages.TagValidator_an_annotation}, */
				{ "@noextend", BuilderMessages.TagValidator_an_annotation }, //$NON-NLS-1$
				{ "@nooverride", BuilderMessages.TagValidator_an_annotation }, //$NON-NLS-1$
		});
		String typename = "test8.java"; //$NON-NLS-1$
		deployTagTest(typename, inc, true);
	}

	public void testInvalidAnnotationTag9I() {
		x9(true);
	}

	public void testInvalidAnnotationTag9F() {
		x9(false);
	}

	/**
	 * Tests having an @noinstantiate tag on a variety of inner / outer /
	 * top-level annotations in package a.b.c
	 */
	private void x9(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs("@noinstantiate", BuilderMessages.TagValidator_an_annotation, 4); //$NON-NLS-1$
		String typename = "test9.java"; //$NON-NLS-1$
		deployTagTest(typename, inc, false);
	}

	public void testInvalidAnnotationTag10I() {
		x10(true);
	}

	public void testInvalidAnnotationTag10F() {
		x10(false);
	}

	/**
	 * Tests having an @noinstantiate tag on an annotation in the default
	 * package
	 */
	private void x10(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs("@noinstantiate", BuilderMessages.TagValidator_an_annotation, 1); //$NON-NLS-1$
		String typename = "test10.java"; //$NON-NLS-1$
		deployTagTest(typename, inc, true);
	}

	public void testInvalidAnnotationTag11I() {
		x11(true);
	}

	public void testInvalidAnnotationTag11F() {
		x11(false);
	}

	/**
	 * Tests having an @noimplement tag on a variety of inner / outer /
	 * top-level annotations in package a.b.c
	 */
	private void x11(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs("@noimplement", BuilderMessages.TagValidator_an_annotation, 4); //$NON-NLS-1$
		String typename = "test11.java"; //$NON-NLS-1$
		deployTagTest(typename, inc, false);
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
		setExpectedMessageArgs("@noimplement", BuilderMessages.TagValidator_an_annotation, 1); //$NON-NLS-1$
		String typename = "test12.java"; //$NON-NLS-1$
		deployTagTest(typename, inc, true);
	}

	public void testInvalidAnnotationTag13I() {
		x13(true);
	}

	public void testInvalidAnnotationTag13F() {
		x13(false);
	}

	/**
	 * Tests all tags are invalid when parent annotation is private or package
	 * default
	 */
	private void x13(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(2));
		setExpectedMessageArgs(new String[][] {
				{ "@noreference", BuilderMessages.TagValidator_annotation_field }, //$NON-NLS-1$
				{ "@noreference", BuilderMessages.TagValidator_enum_not_visible }, //$NON-NLS-1$
		});
		String typename = "test13.java"; //$NON-NLS-1$
		deployTagTest(typename, inc, true);
	}

	public void testInvalidAnnotationTag14I() {
		x14(true);
	}

	public void testInvalidAnnotationTag14F() {
		x14(false);
	}

	/**
	 * Tests for an invalid @noreference tag on a field in a visible member
	 * annotation
	 */
	private void x14(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(2));
		setExpectedMessageArgs(new String[][] {
				{ "@noreference", BuilderMessages.TagValidator_annotation_field }, //$NON-NLS-1$
				{ "@noreference", BuilderMessages.TagValidator_enum_not_visible } //$NON-NLS-1$
		});
		String typename = "test14.java"; //$NON-NLS-1$
		deployTagTest(typename, inc, true);
	}

	public void testInvalidAnnotationTag15I() {
		x15(true);
	}

	public void testInvalidAnnotationTag15F() {
		x15(false);
	}

	/**
	 * Tests for an invalid @noreference tag on a non-visible member annotation
	 */
	private void x15(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(2));
		setExpectedMessageArgs(new String[][] {
				{
						"@noreference", BuilderMessages.TagValidator_annotation_not_visible }, //$NON-NLS-1$
				{
						"@noreference", BuilderMessages.TagValidator_annotation_not_visible } //$NON-NLS-1$
		});
		String typename = "test15.java"; //$NON-NLS-1$
		deployTagTest(typename, inc, true);
	}

	public void testInvalidAnnotationTag16I() {
		x16(true);
	}

	public void testInvalidAnnotationTag16F() {
		x16(false);
	}

	/**
	 * Tests for an invalid @noreference tag on a non-visible member annotation
	 */
	private void x16(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs(new String[][] { {
				"@noreference", BuilderMessages.TagValidator_annotation_not_visible }, //$NON-NLS-1$
		});
		String typename = "test16.java"; //$NON-NLS-1$
		deployTagTest(typename, inc, true);
	}

	public void testInvalidAnnotationTag17I() {
		x17(true);
	}

	public void testInvalidAnnotationTag17F() {
		x17(false);
	}

	/**
	 * Tests for an invalid @noreference tag on a non-visible member annotation
	 */
	private void x17(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(2));
		setExpectedMessageArgs(new String[][] {
				{
						"@noreference", BuilderMessages.TagValidator_annotation_not_visible }, //$NON-NLS-1$
				{
						"@noreference", BuilderMessages.TagValidator_annotation_not_visible } //$NON-NLS-1$
		});
		String typename = "test17.java"; //$NON-NLS-1$
		deployTagTest(typename, inc, true);
	}
}