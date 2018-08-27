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
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

import junit.framework.Test;

/**
 * Tests invalid duplicate tags placed on members
 *
 * @since 1.0.0
 */
public class InvalidDuplicateTagsTests extends TagTest {

	private int fPid = -1;

	public InvalidDuplicateTagsTests(String name) {
		super(name);
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
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(InvalidDuplicateTagsTests.class);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("duplicates"); //$NON-NLS-1$
	}

	@Override
	protected String getTestCompliance() {
		return JavaCore.VERSION_1_5;
	}

	/**
	 * Tests a class that has duplicate tags is properly detected using an
	 * incremental build
	 */
	public void testClassWithDuplicateTagsI() {
		x1(true);
	}

	/**
	 * Tests a class that has duplicate tags is properly detected using a full
	 * build
	 */
	public void testClassWithDuplicateTagsF() {
		x1(false);
	}

	private void x1(boolean inc) {
		setProblemId(IElementDescriptor.TYPE, IApiProblem.DUPLICATE_TAG_USE);
		setExpectedProblemIds(getDefaultProblemSet(2));
		setExpectedMessageArgs(new String[][] { { "@noextend" }, //$NON-NLS-1$
				{ "@noinstantiate" } //$NON-NLS-1$
		});
		deployTagTest("test1.java", inc, false); //$NON-NLS-1$
	}

	/**
	 * Tests that an interface with duplicate tags is properly using an
	 * incremental build
	 */
	public void testInterfaceWithDuplicateTagsI() {
		x2(true);
	}

	/**
	 * Tests that an interface with duplicate tags is properly detected using a
	 * full build
	 */
	public void testInterfaceWithDuplicateTagsF() {
		x2(false);
	}

	private void x2(boolean inc) {
		setProblemId(IElementDescriptor.TYPE, IApiProblem.DUPLICATE_TAG_USE);
		setExpectedProblemIds(getDefaultProblemSet(2));
		setExpectedMessageArgs(new String[][] { { "@noimplement" }, //$NON-NLS-1$
				{ "@noimplement" } //$NON-NLS-1$
		});
		deployTagTest("test2.java", inc, false); //$NON-NLS-1$
	}

	/**
	 * Tests that a class field with duplicate tags is properly detected using
	 * an incremental build
	 */
	public void testClassFieldWithDuplicateTagsI() {
		x3(true);
	}

	/**
	 * Tests that a class field with duplicate tags is properly detected using a
	 * full build
	 */
	public void testClassFieldWithDuplicateTagsF() {
		x3(false);
	}

	private void x3(boolean inc) {
		setProblemId(IElementDescriptor.FIELD, IApiProblem.DUPLICATE_TAG_USE);
		setExpectedProblemIds(getDefaultProblemSet(2));
		setExpectedMessageArgs(new String[][] { { "@noreference" }, //$NON-NLS-1$
				{ "@noreference" } //$NON-NLS-1$
		});
		deployTagTest("test3.java", inc, false); //$NON-NLS-1$
	}

	/**
	 * Tests that an interface field with duplicate tags is properly detected
	 * using an incremental build
	 */
	public void testInterfaceFieldWithDuplicateTagsI() {
		x4(true);
	}

	/**
	 * Tests that an interface field with duplicate tags is properly detected
	 * using a full build
	 */
	public void testInterfaceFieldWithDuplicateTagsF() {
		x4(false);
	}

	private void x4(boolean inc) {
		setProblemId(IElementDescriptor.FIELD, IApiProblem.DUPLICATE_TAG_USE);
		setExpectedProblemIds(getDefaultProblemSet(2));
		setExpectedMessageArgs(new String[][] { { "@noreference" }, //$NON-NLS-1$
				{ "@noreference" } //$NON-NLS-1$
		});
		deployTagTest("test4.java", inc, false); //$NON-NLS-1$
	}

	/**
	 * Tests that an enum field with duplicate tags is properly detected using
	 * an incremental build
	 */
	public void testEnumFieldWithDuplicateTagsI() {
		x5(true);
	}

	/**
	 * Tests that an enum field with duplicate tags is properly detected using a
	 * full build
	 */
	public void testEnumFieldWithDuplicateTagsF() {
		x5(false);
	}

	private void x5(boolean inc) {
		setProblemId(IElementDescriptor.FIELD, IApiProblem.DUPLICATE_TAG_USE);
		setExpectedProblemIds(getDefaultProblemSet(3));
		setExpectedMessageArgs(new String[][] { { "@noreference" }, //$NON-NLS-1$
				{ "@noreference" }, //$NON-NLS-1$
				{ "@noreference" } //$NON-NLS-1$
		});
		deployTagTest("test5.java", inc, false); //$NON-NLS-1$
	}

	/**
	 * Tests that a class method with duplicate tags is properly detected using
	 * an incremental build
	 */
	public void testClassMethodWithDuplicateTagsI() {
		x6(true);
	}

	/**
	 * Tests that a class method with duplicate tags is properly detected using
	 * a full build
	 */
	public void testClassMethodWithDuplicateTagsF() {
		x6(false);
	}

	private void x6(boolean inc) {
		setProblemId(IElementDescriptor.METHOD, IApiProblem.DUPLICATE_TAG_USE);
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] { { "@noreference" }, //$NON-NLS-1$
				{ "@noreference" }, //$NON-NLS-1$
				{ "@nooverride" }, //$NON-NLS-1$
				{ "@nooverride" } //$NON-NLS-1$
		});
		deployTagTest("test6.java", inc, false); //$NON-NLS-1$
	}

	/**
	 * Tests that an interface method with duplicate tags is properly detected
	 * using an incremental build
	 */
	public void testInterfaceMethodWithDuplicateTagsI() {
		x7(true);
	}

	/**
	 * Tests that an interface method with duplicate tags is properly detected
	 * using a full build
	 */
	public void testInterfaceMethodWithDuplicateTagsF() {
		x7(false);
	}

	private void x7(boolean inc) {
		setProblemId(IElementDescriptor.METHOD, IApiProblem.DUPLICATE_TAG_USE);
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] { { "@noreference" }, //$NON-NLS-1$
				{ "@noreference" }, //$NON-NLS-1$
				{ "@noreference" }, //$NON-NLS-1$
				{ "@noreference" }, //$NON-NLS-1$
		});
		deployTagTest("test7.java", inc, false); //$NON-NLS-1$
	}

	/**
	 * Tests that an enum method with duplicate tags is properly detected using
	 * an incremental build
	 */
	public void testEnumMethodWithDuplicateTagsI() {
		x8(true);
	}

	/**
	 * Tests that an interface method with duplicate tags is properly detected
	 * using a full build
	 */
	public void testEnumMethodWithDuplicateTagsF() {
		x8(false);
	}

	private void x8(boolean inc) {
		setProblemId(IElementDescriptor.METHOD, IApiProblem.DUPLICATE_TAG_USE);
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] { { "@noreference" }, //$NON-NLS-1$
				{ "@noreference" }, //$NON-NLS-1$
				{ "@noreference" }, //$NON-NLS-1$
				{ "@noreference" }, //$NON-NLS-1$
		});
		deployTagTest("test8.java", inc, false); //$NON-NLS-1$
	}
}
