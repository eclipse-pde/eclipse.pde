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
 * Tests invalid annotations added to an annotation
 *
 * @since 1.0.400
 */
public class InvalidAnnotationAnnotationsTests extends AnnotationTest {

	/**
	 * Constructor
	 *
	 * @param name
	 */
	public InvalidAnnotationAnnotationsTests(String name) {
		super(name);
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(InvalidAnnotationAnnotationsTests.class);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("annotation"); //$NON-NLS-1$
	}

	@Override
	protected int getDefaultProblemId() {
		return ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiProblem.NO_FLAGS);
	}

	/**
	 * Tests an invalid @NoExtend annotation being used
	 *
	 * @throws Exception
	 */
	public void testInvalidNoExtendAnnotation1I() throws Exception {
		String typename = "test3.java"; //$NON-NLS-1$
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs("@NoExtend", BuilderMessages.TagValidator_an_annotation, 4); //$NON-NLS-1$
		deployAnnotationTest(typename, true, false);
	}

	/**
	 * Tests an invalid @NoExtend annotation being used
	 *
	 * @throws Exception
	 */
	public void testInvalidNoExtendAnnotation1F() throws Exception {
		String typename = "test3.java"; //$NON-NLS-1$
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs("@NoExtend", BuilderMessages.TagValidator_an_annotation, 4); //$NON-NLS-1$
		deployAnnotationTest(typename, false, false);
	}

	/**
	 * Tests a variety of invalid annotations being used
	 *
	 * @throws Exception
	 */
	public void testInvalidAnnotations1I() throws Exception {
		setExpectedProblemIds(getDefaultProblemSet(8));
		setExpectedMessageArgs(new String[][] {
				{ "@NoInstantiate", BuilderMessages.TagValidator_an_annotation }, //$NON-NLS-1$
				{ "@NoExtend", BuilderMessages.TagValidator_an_annotation }, //$NON-NLS-1$
				{ "@NoInstantiate", BuilderMessages.TagValidator_an_annotation }, //$NON-NLS-1$
				{ "@NoExtend", BuilderMessages.TagValidator_an_annotation }, //$NON-NLS-1$
				{ "@NoInstantiate", BuilderMessages.TagValidator_an_annotation }, //$NON-NLS-1$
				{ "@NoExtend", BuilderMessages.TagValidator_an_annotation }, //$NON-NLS-1$
				{ "@NoInstantiate", BuilderMessages.TagValidator_an_annotation }, //$NON-NLS-1$
				{ "@NoExtend", BuilderMessages.TagValidator_an_annotation }, //$NON-NLS-1$
		});
		String typename = "test7.java"; //$NON-NLS-1$
		deployAnnotationTest(typename, true, false);
	}

	/**
	 * Tests a variety of invalid annotations being used
	 *
	 * @throws Exception
	 */
	public void testInvalidAnnotations1F() throws Exception {
		setExpectedProblemIds(getDefaultProblemSet(8));
		setExpectedMessageArgs(new String[][] {
				{ "@NoInstantiate", BuilderMessages.TagValidator_an_annotation }, //$NON-NLS-1$
				{ "@NoExtend", BuilderMessages.TagValidator_an_annotation }, //$NON-NLS-1$
				{ "@NoInstantiate", BuilderMessages.TagValidator_an_annotation }, //$NON-NLS-1$
				{ "@NoExtend", BuilderMessages.TagValidator_an_annotation }, //$NON-NLS-1$
				{ "@NoInstantiate", BuilderMessages.TagValidator_an_annotation }, //$NON-NLS-1$
				{ "@NoExtend", BuilderMessages.TagValidator_an_annotation }, //$NON-NLS-1$
				{ "@NoInstantiate", BuilderMessages.TagValidator_an_annotation }, //$NON-NLS-1$
				{ "@NoExtend", BuilderMessages.TagValidator_an_annotation }, //$NON-NLS-1$
		});
		String typename = "test7.java"; //$NON-NLS-1$
		deployAnnotationTest(typename, false, false);
	}

	/**
	 * Tests an invalid @NoInstantiate annotation is being used
	 *
	 * @throws Exception
	 */
	public void testInvalidNoInstantiateAnnotation1I() throws Exception {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs("@NoInstantiate", BuilderMessages.TagValidator_an_annotation, 4); //$NON-NLS-1$
		String typename = "test9.java"; //$NON-NLS-1$
		deployAnnotationTest(typename, true, false);
	}

	/**
	 * Tests an invalid @NoInstantiate annotation is being used
	 *
	 * @throws Exception
	 */
	public void testInvalidNoInstantiateAnnotation1F() throws Exception {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs("@NoInstantiate", BuilderMessages.TagValidator_an_annotation, 4); //$NON-NLS-1$
		String typename = "test9.java"; //$NON-NLS-1$
		deployAnnotationTest(typename, false, false);
	}

	/**
	 * Tests an invalid @NoImplement annotation is being used
	 *
	 * @throws Exception
	 */
	public void testInvalidNoImplementAnnotation1I() throws Exception {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs("@NoImplement", BuilderMessages.TagValidator_an_annotation, 4); //$NON-NLS-1$
		String typename = "test11.java"; //$NON-NLS-1$
		deployAnnotationTest(typename, true, false);
	}

	/**
	 * Tests an invalid @NoImplement annotation is being used
	 *
	 * @throws Exception
	 */
	public void testInvalidNoImplementAnnotation1F() throws Exception {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs("@NoImplement", BuilderMessages.TagValidator_an_annotation, 4); //$NON-NLS-1$
		String typename = "test11.java"; //$NON-NLS-1$
		deployAnnotationTest(typename, false, false);
	}

	/**
	 * Tests that all annotations are invalid when the parent is not visible
	 *
	 * @throws Exception
	 */
	public void testPrivateParentAnnotations1I() throws Exception {
		setExpectedProblemIds(getDefaultProblemSet(3));
		setExpectedMessageArgs(new String[][] {
				{ "@NoReference", BuilderMessages.TagValidator_annotation_field }, //$NON-NLS-1$
				{ "@NoReference", BuilderMessages.TagValidator_enum_not_visible }, //$NON-NLS-1$
				{ "@NoInstantiate", BuilderMessages.TagValidator_an_interface }, //$NON-NLS-1$
		});
		String typename = "test13.java"; //$NON-NLS-1$
		deployAnnotationTest(typename, true, true);
	}

	/**
	 * Tests that all annotations are invalid when the parent is not visible
	 *
	 * @throws Exception
	 */
	public void testPrivateParentAnnotations1F() throws Exception {
		setExpectedProblemIds(getDefaultProblemSet(3));
		setExpectedMessageArgs(new String[][] {
				{ "@NoReference", BuilderMessages.TagValidator_annotation_field }, //$NON-NLS-1$
				{ "@NoReference", BuilderMessages.TagValidator_enum_not_visible }, //$NON-NLS-1$
				{ "@NoInstantiate", BuilderMessages.TagValidator_an_interface }, //$NON-NLS-1$
		});
		String typename = "test13.java"; //$NON-NLS-1$
		deployAnnotationTest(typename, false, true);
	}
}
