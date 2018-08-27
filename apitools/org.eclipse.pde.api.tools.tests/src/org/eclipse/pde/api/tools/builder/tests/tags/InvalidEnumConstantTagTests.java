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
 * Tests the use of invalid tags on enum constants
 *
 * @since 1.0
 */
public class InvalidEnumConstantTagTests extends InvalidFieldTagTests {

	public InvalidEnumConstantTagTests(String name) {
		super(name);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("enum"); //$NON-NLS-1$
	}

	/**
	 * @return the test for this enum
	 */
	public static Test suite() {
		return buildTestSuite(InvalidEnumConstantTagTests.class);
	}

	@Override
	protected String getTestCompliance() {
		return JavaCore.VERSION_1_5;
	}

	public void testInvalidEnumConstantTag1I() {
		x1(true);
	}

	public void testInvalidEnumConstantTag1F() {
		x1(false);
	}

	/**
	 * Tests a invalid @noreference tag on enum constants in inner / outer enums
	 */
	private void x1(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{ "@noreference", BuilderMessages.TagValidator_an_enum_constant }, //$NON-NLS-1$
				{ "@noreference", BuilderMessages.TagValidator_an_enum_constant }, //$NON-NLS-1$
				{ "@noreference", BuilderMessages.TagValidator_an_enum_constant }, //$NON-NLS-1$
				{ "@noreference", BuilderMessages.TagValidator_an_enum_constant } //$NON-NLS-1$
		});
		deployTagTest("test13.java", inc, false); //$NON-NLS-1$
	}

	/**
	 * Tests a valid @noreference tag on an enum constant in an enum in the
	 * default package using an incremental build
	 */
	public void testInvalidEnumConstantTag2I() {
		x2(true);
	}

	/**
	 * Tests a valid @noreference tag on an enum constant in an enum in the
	 * default package using a full build
	 */
	public void testInvalidEnumConstantTag2F() {
		x2(false);
	}

	private void x2(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs(new String[][] { {
				"@noreference", BuilderMessages.TagValidator_an_enum_constant } //$NON-NLS-1$
		});
		deployTagTest("test14.java", inc, true); //$NON-NLS-1$
	}

	public void testInvalidEnumConstantTag3I() {
		x3(true);
	}

	public void testInvalidEnumConstantTag3F() {
		x3(false);
	}

	/**
	 * Tests an invalid @noextend tag on enum constants in inner / outer enums
	 * using an incremental build
	 */
	private void x3(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{ "@noextend", BuilderMessages.TagValidator_an_enum_constant }, //$NON-NLS-1$
				{ "@noextend", BuilderMessages.TagValidator_an_enum_constant }, //$NON-NLS-1$
				{ "@noextend", BuilderMessages.TagValidator_an_enum_constant }, //$NON-NLS-1$
				{ "@noextend", BuilderMessages.TagValidator_an_enum_constant } //$NON-NLS-1$
		});
		deployTagTest("test15.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidEnumConstantTag4I() {
		x4(false);
	}

	public void testInvalidEnumConstantTag4F() {
		x4(false);
	}

	/**
	 * Tests a valid @noextend tag on an enum constant in an enum in the default
	 * package using a full build
	 */
	private void x4(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs(new String[][] { {
				"@noextend", BuilderMessages.TagValidator_an_enum_constant } //$NON-NLS-1$
		});
		deployTagTest("test16.java", inc, true); //$NON-NLS-1$
	}

	public void testInvalidEnumConstantTag5I() {
		x5(true);
	}

	public void testInvalidEnumConstantTag5F() {
		x5(false);
	}

	/**
	 * Tests an invalid @noimplement tag on enum constants in inner / outer
	 * enums using an incremental build
	 */
	private void x5(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{ "@noimplement", BuilderMessages.TagValidator_an_enum_constant }, //$NON-NLS-1$
				{ "@noimplement", BuilderMessages.TagValidator_an_enum_constant }, //$NON-NLS-1$
				{ "@noimplement", BuilderMessages.TagValidator_an_enum_constant }, //$NON-NLS-1$
				{ "@noimplement", BuilderMessages.TagValidator_an_enum_constant } //$NON-NLS-1$
		});
		deployTagTest("test17.java", inc, false); //$NON-NLS-1$
	}

	/**
	 * Tests a valid @noimplement tag on an enum constant in an enum in the
	 * default package using an incremental build
	 */
	public void testInvalidEnumConstantTag6I() {
		x6(true);
	}

	public void testInvalidEnumConstantTag6F() {
		x6(false);
	}

	/**
	 * Tests a valid @noimplement tag on an enum constant in an enum in the
	 * default package using a full build
	 */
	private void x6(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs(new String[][] { {
				"@noimplement", BuilderMessages.TagValidator_an_enum_constant } //$NON-NLS-1$
		});
		deployTagTest("test18.java", inc, true); //$NON-NLS-1$
	}

	public void testInvalidEnumConstantTag7I() {
		x7(true);
	}

	public void testInvalidEnumConstantTag7F() {
		x7(false);
	}

	/**
	 * Tests a invalid @nooverride tag on enum constants in inner /outer enums
	 * using a full build
	 */
	private void x7(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{ "@nooverride", BuilderMessages.TagValidator_an_enum_constant }, //$NON-NLS-1$
				{ "@nooverride", BuilderMessages.TagValidator_an_enum_constant }, //$NON-NLS-1$
				{ "@nooverride", BuilderMessages.TagValidator_an_enum_constant }, //$NON-NLS-1$
				{ "@nooverride", BuilderMessages.TagValidator_an_enum_constant } //$NON-NLS-1$
		});
		deployTagTest("test19.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidEnumConstantTag8I() {
		x8(true);
	}

	public void testInvalidEnumConstantTag8F() {
		x8(false);
	}

	/**
	 * Tests a valid @nooverride tag on an enum constant in an enum in the
	 * default package using an incremental build
	 */
	private void x8(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs(new String[][] { {
				"@nooverride", BuilderMessages.TagValidator_an_enum_constant } //$NON-NLS-1$
		});
		deployTagTest("test20.java", inc, true); //$NON-NLS-1$
	}

	public void testInvalidEnumConstantTag9I() {
		x9(true);
	}

	public void testInvalidEnumConstantTag9F() {
		x9(false);
	}

	/**
	 * Tests an invalid @noinstantiate tag on enum constants in inner / outer
	 * enums using an incremental build
	 */
	private void x9(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{
						"@noinstantiate", BuilderMessages.TagValidator_an_enum_constant }, //$NON-NLS-1$
				{
						"@noinstantiate", BuilderMessages.TagValidator_an_enum_constant }, //$NON-NLS-1$
				{
						"@noinstantiate", BuilderMessages.TagValidator_an_enum_constant }, //$NON-NLS-1$
				{
						"@noinstantiate", BuilderMessages.TagValidator_an_enum_constant } //$NON-NLS-1$
		});
		deployTagTest("test21.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidEnumConstantTag10I() {
		x10(true);
	}

	public void testInvalidEnumConstantTag10F() {
		x10(false);
	}

	/**
	 * Tests a valid @noinstantiate tag on an enum constant in an enum in the
	 * default package using a full build
	 */
	private void x10(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs(new String[][] { {
				"@noinstantiate", BuilderMessages.TagValidator_an_enum_constant } //$NON-NLS-1$
		});
		deployTagTest("test22.java", inc, true); //$NON-NLS-1$
	}
}
