/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.builder.tests.annotations;

import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.api.tools.internal.builder.BuilderMessages;

import junit.framework.Test;

/**
 * Tests invalid annotations used on annotation fields
 *
 * @since 1.0.400
 */
public class InvalidAnnotationFieldAnnotationTests extends FieldAnnotationTest {

	/**
	 * Constructor
	 *
	 * @param name
	 */
	public InvalidAnnotationFieldAnnotationTests(String name) {
		super(name);
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(InvalidAnnotationFieldAnnotationTests.class);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("annotation"); //$NON-NLS-1$
	}

	public void testInvalidAnnotationFieldAnnotation1I() {
		x1(true);
	}

	public void testInvalidAnnotationFieldAnnotation1F() {
		x1(false);
	}

	/**
	 * Tests the unsupported @NoExtend annotation on a variety of inner / outer
	 * annotation fields
	 */
	private void x1(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(3));
		setExpectedMessageArgs("@NoExtend", BuilderMessages.TagValidator_annotation_field, 3); //$NON-NLS-1$
		String typename = "test1.java"; //$NON-NLS-1$
		deployAnnotationTestWithErrors(typename, inc, false);
	}

	public void testInvalidAnnotationFieldAnnotation2I() {
		x2(true);
	}

	public void testInvalidAnnotationFieldAnnotation2F() {
		x2(false);
	}

	/**
	 * Tests the unsupported @NoExtend annotation on an annotation field in the
	 * default package
	 */
	private void x2(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs("@NoExtend", BuilderMessages.TagValidator_annotation_field, 1); //$NON-NLS-1$
		String typename = "test2.java"; //$NON-NLS-1$
		deployAnnotationTestWithErrors(typename, inc, true);
	}

	public void testInvalidAnnotationFieldAnnotation3I() {
		x3(true);
	}

	public void testInvalidAnnotationFieldAnnotation3F() {
		x3(false);
	}

	/**
	 * Tests the unsupported @NoInstantiate annotation on a variety of inner /
	 * outer annotation fields
	 */
	private void x3(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(3));
		setExpectedMessageArgs("@NoInstantiate", BuilderMessages.TagValidator_annotation_field, 3); //$NON-NLS-1$
		String typename = "test3.java"; //$NON-NLS-1$
		deployAnnotationTestWithErrors(typename, inc, false);
	}

	public void testInvalidAnnotationFieldAnnotation4I() {
		x4(true);
	}

	public void testInvalidAnnotationFieldAnnotation4F() {
		x4(false);
	}

	/**
	 * Tests the unsupported @NoInstantiate annotation on an annotation field in
	 * the default package
	 */
	private void x4(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs("@NoInstantiate", BuilderMessages.TagValidator_annotation_field, 1); //$NON-NLS-1$
		String typename = "test4.java"; //$NON-NLS-1$
		deployAnnotationTestWithErrors(typename, inc, true);
	}

	public void testInvalidAnnotationFieldAnnotation5I() {
		x5(true);
	}

	public void testInvalidAnnotationFieldAnnotation5F() {
		x5(false);
	}

	/**
	 * Tests the unsupported @NoImplement annotation on a variety of inner /
	 * outer annotation fields
	 */
	private void x5(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(3));
		setExpectedMessageArgs("@NoImplement", BuilderMessages.TagValidator_annotation_field, 3); //$NON-NLS-1$
		String typename = "test5.java"; //$NON-NLS-1$
		deployAnnotationTestWithErrors(typename, inc, false);
	}

	public void testInvalidAnnotationFieldAnnotation6I() {
		x6(true);
	}

	public void testInvalidAnnotationFieldAnnotation6F() {
		x6(false);
	}

	/**
	 * Tests the unsupported @NoImplement annotation on an annotation field in
	 * the default package
	 */
	private void x6(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs("@NoImplement", BuilderMessages.TagValidator_annotation_field, 1); //$NON-NLS-1$
		String typename = "test6.java"; //$NON-NLS-1$
		deployAnnotationTestWithErrors(typename, inc, true);
	}

	public void testInvalidAnnotationFieldAnnotation7I() {
		x7(true);
	}

	public void testInvalidAnnotationFieldAnnotation7F() {
		x7(false);
	}

	/**
	 * Tests the unsupported @NoOverride annotation on a variety of inner /
	 * outer annotation fields
	 */
	private void x7(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(3));
		setExpectedMessageArgs("@NoOverride", BuilderMessages.TagValidator_annotation_field, 3); //$NON-NLS-1$
		String typename = "test7.java"; //$NON-NLS-1$
		deployAnnotationTestWithErrors(typename, inc, false);
	}

	public void testInvalidAnnotationFieldAnnotation8I() {
		x8(true);
	}

	public void testInvalidAnnotationFieldAnnotation8F() {
		x8(false);
	}

	/**
	 * Tests the unsupported @NoOverride annotation on an annotation field in
	 * the default package
	 */
	private void x8(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs("@NoOverride", BuilderMessages.TagValidator_annotation_field, 1); //$NON-NLS-1$
		String typename = "test8.java"; //$NON-NLS-1$
		deployAnnotationTestWithErrors(typename, inc, true);
	}

	public void testInvalidAnnotationFieldAnnotation9I() {
		x9(true);
	}

	public void testInvalidAnnotationFieldAnnotation9F() {
		x9(false);
	}

	/**
	 * Tests all the unsupported annotations on a variety of annotation fields
	 */
	private void x9(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(15));
		setExpectedMessageArgs(new String[][] {
				{ "@NoOverride", BuilderMessages.TagValidator_annotation_field }, //$NON-NLS-1$
				{ "@NoImplement", BuilderMessages.TagValidator_annotation_field }, //$NON-NLS-1$
				{
						"@NoInstantiate", BuilderMessages.TagValidator_annotation_field }, //$NON-NLS-1$
				{ "@NoExtend", BuilderMessages.TagValidator_annotation_field }, //$NON-NLS-1$
				{ "@NoReference", BuilderMessages.TagValidator_annotation_field }, //$NON-NLS-1$
				{ "@NoOverride", BuilderMessages.TagValidator_annotation_field }, //$NON-NLS-1$
				{ "@NoImplement", BuilderMessages.TagValidator_annotation_field }, //$NON-NLS-1$
				{
						"@NoInstantiate", BuilderMessages.TagValidator_annotation_field }, //$NON-NLS-1$
				{ "@NoExtend", BuilderMessages.TagValidator_annotation_field }, //$NON-NLS-1$
				{ "@NoReference", BuilderMessages.TagValidator_annotation_field }, //$NON-NLS-1$
				{ "@NoOverride", BuilderMessages.TagValidator_annotation_field }, //$NON-NLS-1$
				{ "@NoImplement", BuilderMessages.TagValidator_annotation_field }, //$NON-NLS-1$
				{
						"@NoInstantiate", BuilderMessages.TagValidator_annotation_field }, //$NON-NLS-1$
				{ "@NoExtend", BuilderMessages.TagValidator_annotation_field }, //$NON-NLS-1$
				{ "@NoReference", BuilderMessages.TagValidator_annotation_field } //$NON-NLS-1$
		});
		String typename = "test9.java"; //$NON-NLS-1$
		deployAnnotationTestWithErrors(typename, inc, false);
	}

	public void testInvalidAnnotationFieldAnnotation10I() {
		x10(true);
	}

	public void testInvalidAnnotationFieldAnnotation10F() {
		x10(false);
	}

	/**
	 * Tests the unsupported @NoReference annotation on a variety of final inner
	 * / outer annotation fields
	 */
	private void x10(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(3));
		setExpectedMessageArgs("@NoReference", BuilderMessages.TagValidator_annotation_field, 3); //$NON-NLS-1$
		String typename = "test10.java"; //$NON-NLS-1$
		deployAnnotationTest(typename, inc, false);
	}

	public void testInvalidAnnotationFieldAnnotation11I() {
		x11(true);
	}

	public void testInvalidAnnotationFieldAnnotation11F() {
		x11(false);
	}

	/**
	 * Tests the unsupported @NoReference annotation on a final annotation field
	 * in the default package
	 */
	private void x11(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs("@NoReference", BuilderMessages.TagValidator_annotation_field, 1); //$NON-NLS-1$
		String typename = "test11.java"; //$NON-NLS-1$
		deployAnnotationTest(typename, inc, true);
	}

}
