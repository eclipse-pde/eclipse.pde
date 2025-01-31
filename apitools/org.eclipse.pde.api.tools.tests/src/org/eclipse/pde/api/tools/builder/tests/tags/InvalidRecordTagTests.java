/*******************************************************************************
 * Copyright (c) 2025 ArSysOp.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Alexander Fedorov (ArSysOp) - initial API and implementation
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
 * Tests that the builder finds and properly reports invalid tags on records
 *
 * @since 1.4
 */
public class InvalidRecordTagTests extends TagTest {

	public InvalidRecordTagTests(String name) {
		super(name);
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(InvalidRecordTagTests.class);
	}

	@Override
	protected String getTestingProjectName() {
		return "java17tags"; //$NON-NLS-1$
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("record"); //$NON-NLS-1$
	}

	@Override
	protected int getDefaultProblemId() {
		return ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.UNSUPPORTED_TAG_USE, IApiProblem.NO_FLAGS);
	}

	@Override
	protected String getTestCompliance() {
		return JavaCore.VERSION_14;
	}

	public void testInvalidRecordTag1I() {
		x1(true);
	}

	public void testInvalidRecordTag1F() {
		x1(false);
	}

	/**
	 * Tests having an @noreference tag on a variety of inner / outer /
	 * top-level record in package a.b.c
	 */
	private void x1(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(3));
		setExpectedMessageArgs(new String[][] {
				{ "@noreference", BuilderMessages.TagValidator_record_not_visible }, //$NON-NLS-1$
				{ "@noreference", BuilderMessages.TagValidator_record_not_visible }, //$NON-NLS-1$
				{ "@noreference", BuilderMessages.TagValidator_record_not_visible }, //$NON-NLS-1$
		});
		deployTagTest("test1.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidRecordTag3I() {
		x3(true);
	}

	public void testInvalidRecordTag3F() {
		x3(false);
	}

	/**
	 * Tests having an @noextend tag on a variety of inner / outer / top-level
	 * records in package a.b.c
	 */
	private void x3(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{ "@noextend", BuilderMessages.TagValidator_a_record }, //$NON-NLS-1$
				{ "@noextend", BuilderMessages.TagValidator_a_record }, //$NON-NLS-1$
				{ "@noextend", BuilderMessages.TagValidator_a_record }, //$NON-NLS-1$
				{ "@noextend", BuilderMessages.TagValidator_a_record } //$NON-NLS-1$
		});
		deployTagTest("test3.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidRecordTag4I() {
		x4(true);
	}

	public void testInvalidRecordTag4F() {
		x4(false);
	}

	/**
	 * Tests having an @noextend tag on an record in the default package
	 */
	private void x4(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs(new String[][] { {
				"@noextend", BuilderMessages.TagValidator_a_record } //$NON-NLS-1$
		});
		deployTagTest("test4.java", inc, true); //$NON-NLS-1$
	}

	public void testInvalidRecordTag5I() {
		x5(true);
	}

	public void testInvalidRecordTag5F() {
		x5(false);
	}

	/**
	 * Tests having an @nooverride tag on a variety of inner / outer / top-level
	 * records in package a.b.c
	 */
	private void x5(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{ "@nooverride", BuilderMessages.TagValidator_a_record }, //$NON-NLS-1$
				{ "@nooverride", BuilderMessages.TagValidator_a_record }, //$NON-NLS-1$
				{ "@nooverride", BuilderMessages.TagValidator_a_record }, //$NON-NLS-1$
				{ "@nooverride", BuilderMessages.TagValidator_a_record } //$NON-NLS-1$
		});
		deployTagTest("test5.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidRecordTag6I() {
		x6(true);
	}

	public void testInvalidRecordTag6F() {
		x6(false);
	}

	/**
	 * Tests having an @nooverride tag on a record in the default package
	 */
	private void x6(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs(new String[][] { {
				"@nooverride", BuilderMessages.TagValidator_a_record } //$NON-NLS-1$
		});
		deployTagTest("test6.java", inc, true); //$NON-NLS-1$
	}

	public void testInvalidRecordTag7I() {
		x7(true);
	}

	public void testInvalidRecordTag7F() {
		x7(false);
	}

	/**
	 * Tests having an @noinstantiate on a variety of inner / outer / top-level
	 * records in package a.b.c
	 */
	private void x7(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{ "@noinstantiate", BuilderMessages.TagValidator_a_record }, //$NON-NLS-1$
				{ "@noinstantiate", BuilderMessages.TagValidator_a_record }, //$NON-NLS-1$
				{ "@noinstantiate", BuilderMessages.TagValidator_a_record }, //$NON-NLS-1$
				{ "@noinstantiate", BuilderMessages.TagValidator_a_record } //$NON-NLS-1$
		});
		deployTagTest("test7.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidRecordTag8I() {
		x8(true);
	}

	public void testInvalidRecordTag8F() {
		x8(false);
	}

	/**
	 * Tests having an @noinstantiate on a record in the default package
	 */
	private void x8(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs(new String[][] { {
				"@noinstantiate", BuilderMessages.TagValidator_a_record } //$NON-NLS-1$
		});
		deployTagTest("test8.java", inc, true); //$NON-NLS-1$
	}

	public void testInvalidRecordTag9I() {
		x9(true);
	}

	public void testInvalidRecordTag9F() {
		x9(false);
	}

	/**
	 * Tests having an @noimplement tag on a variety of inner / outer /
	 * top-level records in package a.b.c
	 */
	private void x9(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{ "@noimplement", BuilderMessages.TagValidator_a_record }, //$NON-NLS-1$
				{ "@noimplement", BuilderMessages.TagValidator_a_record }, //$NON-NLS-1$
				{ "@noimplement", BuilderMessages.TagValidator_a_record }, //$NON-NLS-1$
				{ "@noimplement", BuilderMessages.TagValidator_a_record } //$NON-NLS-1$
		});
		deployTagTest("test9.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidRecordTag10I() {
		x10(true);
	}

	public void testInvalidRecordTag10F() {
		x10(false);
	}

	/**
	 * Tests having an @noimplement tag on a record in the default package
	 */
	private void x10(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs(new String[][] { {
				"@noimplement", BuilderMessages.TagValidator_a_record } //$NON-NLS-1$
		});
		deployTagTest("test10.java", inc, true); //$NON-NLS-1$
	}

	public void testInvalidRecordTag11I() {
		x11(true);
	}

	public void testInvalidRecordTag11F() {
		x11(false);
	}

	/**
	 * Tests all tags are invalid when parent record is package default
	 */
	private void x11(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs(new String[][] {
				{ "@noreference", BuilderMessages.TagValidator_record_not_visible } //$NON-NLS-1$
		});
		deployTagTest("test11.java", inc, true); //$NON-NLS-1$
	}
}
