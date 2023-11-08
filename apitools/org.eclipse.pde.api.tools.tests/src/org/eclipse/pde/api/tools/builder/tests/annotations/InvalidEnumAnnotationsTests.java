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
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

import junit.framework.Test;

/**
 * Tests illegal use of annotations on enums
 *
 * @since 1.0.400
 */
public class InvalidEnumAnnotationsTests extends AnnotationTest {

	/**
	 * Constructor
	 *
	 * @param name
	 */
	public InvalidEnumAnnotationsTests(String name) {
		super(name);
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(InvalidEnumAnnotationsTests.class);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("enum"); //$NON-NLS-1$
	}

	@Override
	protected int getDefaultProblemId() {
		return ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiProblem.NO_FLAGS);
	}

	public void testInvalidEnumTag1I() {
		x1(true);
	}

	public void testInvalidEnumTag1F() {
		x1(false);
	}

	/**
	 * Tests having an @NoReference annotation on a variety of inner / outer /
	 * top-level enums in package a.b.c
	 */
	private void x1(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(3));
		setExpectedMessageArgs(new String[][] {
				{ "@NoReference", BuilderMessages.TagValidator_enum_not_visible }, //$NON-NLS-1$
				{ "@NoReference", BuilderMessages.TagValidator_enum_not_visible }, //$NON-NLS-1$
				{ "@NoReference", BuilderMessages.TagValidator_enum_not_visible }, //$NON-NLS-1$
		});
		deployAnnotationTest("test1.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidEnumTag2I() {
		x3(true);
	}

	public void testInvalidEnumTag2F() {
		x3(false);
	}

	/**
	 * Tests having an @NoExtend annotation on a variety of inner / outer /
	 * top-level enums in package a.b.c
	 */
	private void x3(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{ "@NoExtend", BuilderMessages.TagValidator_an_enum }, //$NON-NLS-1$
				{ "@NoExtend", BuilderMessages.TagValidator_an_enum }, //$NON-NLS-1$
				{ "@NoExtend", BuilderMessages.TagValidator_an_enum }, //$NON-NLS-1$
				{ "@NoExtend", BuilderMessages.TagValidator_an_enum } //$NON-NLS-1$
		});
		deployAnnotationTest("test3.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidEnumTag3I() {
		x5(true);
	}

	public void testInvalidEnumTag3F() {
		x5(false);
	}

	/**
	 * Tests having an @NoOverride annotation on a variety of inner / outer /
	 * top-level enums in package a.b.c
	 */
	private void x5(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{ "@NoOverride", BuilderMessages.TagValidator_an_enum }, //$NON-NLS-1$
				{ "@NoOverride", BuilderMessages.TagValidator_an_enum }, //$NON-NLS-1$
				{ "@NoOverride", BuilderMessages.TagValidator_an_enum }, //$NON-NLS-1$
				{ "@NoOverride", BuilderMessages.TagValidator_an_enum } //$NON-NLS-1$
		});
		deployAnnotationTestWithErrors("test5.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidEnumTag4I() {
		x7(true);
	}

	public void testInvalidEnumTag4F() {
		x7(false);
	}

	/**
	 * Tests having an @NoInstantiate annotation on a variety of inner / outer /
	 * top-level enums in package a.b.c
	 */
	private void x7(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{ "@NoInstantiate", BuilderMessages.TagValidator_an_enum }, //$NON-NLS-1$
				{ "@NoInstantiate", BuilderMessages.TagValidator_an_enum }, //$NON-NLS-1$
				{ "@NoInstantiate", BuilderMessages.TagValidator_an_enum }, //$NON-NLS-1$
				{ "@NoInstantiate", BuilderMessages.TagValidator_an_enum } //$NON-NLS-1$
		});
		deployAnnotationTest("test7.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidEnumTag5I() {
		x9(true);
	}

	public void testInvalidEnumTag5F() {
		x9(false);
	}

	/**
	 * Tests having an @NoImplement annotation on a variety of inner / outer /
	 * top-level enums in package a.b.c
	 */
	private void x9(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{ "@NoImplement", BuilderMessages.TagValidator_an_enum }, //$NON-NLS-1$
				{ "@NoImplement", BuilderMessages.TagValidator_an_enum }, //$NON-NLS-1$
				{ "@NoImplement", BuilderMessages.TagValidator_an_enum }, //$NON-NLS-1$
				{ "@NoImplement", BuilderMessages.TagValidator_an_enum } //$NON-NLS-1$
		});
		deployAnnotationTest("test9.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidEnumTag6I() {
		x11(true);
	}

	public void testInvalidEnumTag6F() {
		x11(false);
	}

	/**
	 * Tests all annotations are invalid when parent enum is private or package
	 * default
	 */
	private void x11(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(2));
		setExpectedMessageArgs(new String[][] {
				{ "@NoReference", BuilderMessages.TagValidator_an_enum_constant }, //$NON-NLS-1$
				{ "@NoReference", BuilderMessages.TagValidator_enum_not_visible } //$NON-NLS-1$
		});
		deployAnnotationTest("test11.java", inc, true); //$NON-NLS-1$
	}
}
