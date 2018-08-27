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
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

import junit.framework.Test;

/**
 * Tests detection of duplicate annotations being used
 *
 * @since 1.0.400
 */
public class InvalidDuplicateAnnotationTests extends AnnotationTest {

	private int fPid = 0;

	/**
	 * Constructor
	 *
	 * @param name
	 */
	public InvalidDuplicateAnnotationTests(String name) {
		super(name);
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(InvalidDuplicateAnnotationTests.class);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("duplicates"); //$NON-NLS-1$
	}

	@Override
	protected int getDefaultProblemId() {
		return fPid;
	}

	/**
	 * Must be called before a call {@link #getDefaultProblemId()}
	 *
	 * @param element
	 * @param kind
	 */
	private void setProblemId(int element, int kind) {
		fPid = ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, element, kind, IApiProblem.NO_FLAGS);
	}

	/**
	 * Tests a class that has duplicate annotations is properly detected using
	 * an incremental build
	 */
	public void testClassWithDuplicateAnnotationsI() {
		x1(true);
	}

	/**
	 * Tests a class that has duplicate annotations is properly detected using a
	 * full build
	 */
	public void testClassWithDuplicateAnnotationsF() {
		x1(false);
	}

	private void x1(boolean inc) {
		setProblemId(IElementDescriptor.TYPE, IApiProblem.DUPLICATE_ANNOTATION_USE);
		setExpectedProblemIds(getDefaultProblemSet(2));
		setExpectedMessageArgs(new String[][] { { "@NoExtend" }, //$NON-NLS-1$
				{ "@NoInstantiate" } //$NON-NLS-1$
		});
		String sourcename = "test1.java"; //$NON-NLS-1$
		deployAnnotationTestWithErrors(sourcename, inc, false);
	}

	/**
	 * Tests that an interface with duplicate annotations is properly using an
	 * incremental build
	 */
	public void testInterfaceWithDuplicateAnnotationsI() {
		x2(true);
	}

	/**
	 * Tests that an interface with duplicate annotations is properly detected
	 * using a full build
	 */
	public void testInterfaceWithDuplicateAnnotationsF() {
		x2(false);
	}

	private void x2(boolean inc) {
		setProblemId(IElementDescriptor.TYPE, IApiProblem.DUPLICATE_ANNOTATION_USE);
		setExpectedProblemIds(getDefaultProblemSet(2));
		setExpectedMessageArgs(new String[][] { { "@NoImplement" }, //$NON-NLS-1$
				{ "@NoImplement" } //$NON-NLS-1$
		});
		String sourcename = "test2.java"; //$NON-NLS-1$
		deployAnnotationTestWithErrors(sourcename, inc, false);
	}

	/**
	 * Tests that a class field with duplicate annotations is properly detected
	 * using an incremental build
	 */
	public void testClassFieldWithDuplicateAnnotationsI() {
		x3(true);
	}

	/**
	 * Tests that a class field with duplicate annotations is properly detected
	 * using a full build
	 */
	public void testClassFieldWithDuplicateAnnotationsF() {
		x3(false);
	}

	private void x3(boolean inc) {
		setProblemId(IElementDescriptor.FIELD, IApiProblem.DUPLICATE_ANNOTATION_USE);
		setExpectedProblemIds(getDefaultProblemSet(2));
		setExpectedMessageArgs(new String[][] { { "@NoReference" }, //$NON-NLS-1$
				{ "@NoReference" } //$NON-NLS-1$
		});
		String sourcename = "test3.java"; //$NON-NLS-1$
		deployAnnotationTestWithErrors(sourcename, inc, false);
	}

	/**
	 * Tests that an interface field with duplicate annotations is properly
	 * detected using an incremental build
	 */
	public void testInterfaceFieldWithDuplicateAnnotationsI() {
		x4(true);
	}

	/**
	 * Tests that an interface field with duplicate annotations is properly
	 * detected using a full build
	 */
	public void testInterfaceFieldWithDuplicateAnnotationsF() {
		x4(false);
	}

	private void x4(boolean inc) {
		setProblemId(IElementDescriptor.FIELD, IApiProblem.DUPLICATE_ANNOTATION_USE);
		setExpectedProblemIds(getDefaultProblemSet(2));
		setExpectedMessageArgs(new String[][] { { "@NoReference" }, //$NON-NLS-1$
				{ "@NoReference" } //$NON-NLS-1$
		});
		String sourcename = "test4.java"; //$NON-NLS-1$
		deployAnnotationTestWithErrors(sourcename, inc, false);
	}

	/**
	 * Tests that an enum field with duplicate annotations is properly detected
	 * using an incremental build
	 */
	public void testEnumFieldWithDuplicateAnnotationsI() {
		x5(true);
	}

	/**
	 * Tests that an enum field with duplicate annotations is properly detected
	 * using a full build
	 */
	public void testEnumFieldWithDuplicateAnnotationsF() {
		x5(false);
	}

	private void x5(boolean inc) {
		setProblemId(IElementDescriptor.FIELD, IApiProblem.DUPLICATE_ANNOTATION_USE);
		setExpectedProblemIds(getDefaultProblemSet(3));
		setExpectedMessageArgs(new String[][] { { "@NoReference" }, //$NON-NLS-1$
				{ "@NoReference" }, //$NON-NLS-1$
				{ "@NoReference" } //$NON-NLS-1$
		});
		String sourcename = "test5.java"; //$NON-NLS-1$
		deployAnnotationTestWithErrors(sourcename, inc, false);
	}

}
