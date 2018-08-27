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

import junit.framework.Test;

/**
 * Tests invalid javadoc tags on enum methods
 *
 * @since 1.0
 */
public class InvalidEnumMethodTagTests extends InvalidMethodTagTests {

	public InvalidEnumMethodTagTests(String name) {
		super(name);
	}

	@Override
	protected String getTestCompliance() {
		return JavaCore.VERSION_1_5;
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("enum"); //$NON-NLS-1$
	}

	/**
	 * @return the test for this class
	 */
	public static Test suite() {
		return buildTestSuite(InvalidEnumMethodTagTests.class);
	}

	public void testInvalidEnumMethodTag1I() {
		x1(true);
	}

	public void testInvalidEnumMethodTag1F() {
		x1(false);
	}

	/**
	 * Tests the unsupported @noextend tag on a variety of inner / outer enum methods
	 */
	public void x1(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(6));
		setExpectedMessageArgs(new String[][] {
				{"@noextend", BuilderMessages.TagValidator_an_enum_method}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_an_enum_method}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_an_enum_method}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_an_enum_method}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_an_enum_method}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_an_enum_method} //$NON-NLS-1$
		});
		deployTagTest("test1.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidEnumMethodTag2I() {
		x2(true);
	}

	public void testInvalidEnumMethodTag2F() {
		x2(false);
	}

	/**
	 * Tests the unsupported @noextend tag on enum methods in the default package
	 */
	private void x2(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(2));
		setExpectedMessageArgs(new String[][] {
				{"@noextend", BuilderMessages.TagValidator_an_enum_method}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_an_enum_method} //$NON-NLS-1$
		});
		deployTagTest("test2.java", inc, true); //$NON-NLS-1$
	}

	public void testInvalidEnumMethodTag3I() {
		x3(true);
	}

	public void testInvalidEnumMethodTag3F() {
		x3(false);
	}

	/**
	 * Tests the unsupported @noinstantiate tag on a variety of inner / outer enum methods
	 */
	public void x3(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(6));
		setExpectedMessageArgs(new String[][] {
				{"@noinstantiate", BuilderMessages.TagValidator_an_enum_method}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_an_enum_method}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_an_enum_method}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_an_enum_method}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_an_enum_method}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_an_enum_method} //$NON-NLS-1$
		});
		deployTagTest("test3.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidEnumMethodTag4I() {
		x4(true);
	}

	public void testInvalidEnumMethodTag4F() {
		x4(false);
	}

	/**
	 * Tests the unsupported @noinstantiate tag on enum methods in the default package
	 */
	private void x4(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(2));
		setExpectedMessageArgs(new String[][] {
				{"@noinstantiate", BuilderMessages.TagValidator_an_enum_method}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_an_enum_method} //$NON-NLS-1$
		});
		deployTagTest("test4.java", inc, true); //$NON-NLS-1$
	}

	public void testInvalidEnumMethodTag5I() {
		x5(true);
	}

	public void testInvalidEnumMethodTag5F() {
		x5(false);
	}

	/**
	 * Tests the unsupported @noimplement tag on a variety of inner / outer enum methods
	 */
	public void x5(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(6));
		setExpectedMessageArgs(new String[][] {
				{"@noimplement", BuilderMessages.TagValidator_an_enum_method}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_an_enum_method}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_an_enum_method}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_an_enum_method}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_an_enum_method}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_an_enum_method} //$NON-NLS-1$
		});
		deployTagTest("test5.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidEnumMethodTag6I() {
		x6(true);
	}

	public void testInvalidEnumMethodTag6F() {
		x6(false);
	}

	/**
	 * Tests the unsupported @noimplement tag on enum methods in the default package
	 */
	private void x6(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(2));
		setExpectedMessageArgs(new String[][] {
				{"@noimplement", BuilderMessages.TagValidator_an_enum_method}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_an_enum_method} //$NON-NLS-1$
		});
		deployTagTest("test6.java", inc, true); //$NON-NLS-1$
	}

	public void testInvalidEnumMethodTag7I() {
		x7(true);
	}

	public void testInvalidEnumMethodTag7F() {
		x7(false);
	}

	/**
	 * Tests the unsupported @nooverride tag on a variety of inner / outer enum methods
	 */
	public void x7(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(6));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_an_enum_method}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_an_enum_method}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_an_enum_method}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_an_enum_method}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_an_enum_method}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_an_enum_method} //$NON-NLS-1$
		});
		deployTagTest("test7.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidEnumMethodTag8I() {
		x8(true);
	}

	public void testInvalidEnumMethodTag8F() {
		x8(false);
	}

	/**
	 * Tests the unsupported @nooverride tag on enum methods in the default package
	 */
	private void x8(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(2));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_an_enum_method}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_an_enum_method} //$NON-NLS-1$
		});
		deployTagTest("test8.java", inc, true); //$NON-NLS-1$
	}

	public void testInvalidEnumMethodTag9I() {
		x9(true);
	}

	public void testInvalidEnumMethodTag9F() {
		x9(false);
	}

	/**
	 * Tests the unsupported @noimplement tag on private enum methods
	 */
	private void x9(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(2));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_an_enum_method}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_an_enum_method} //$NON-NLS-1$
		});
		deployTagTest("test9.java", inc, true); //$NON-NLS-1$
	}
}
