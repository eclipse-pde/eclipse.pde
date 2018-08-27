/*******************************************************************************
 * Copyright (c) 2008, 208 IBM Corporation and others.
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
import org.eclipse.pde.api.tools.internal.builder.BuilderMessages;

import junit.framework.Test;

/**
 * Tests invalid tags on fields in classes
 *
 * @since 1.0
 */
public class InvalidClassFieldTagTests extends InvalidFieldTagTests {

	public InvalidClassFieldTagTests(String name) {
		super(name);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("class"); //$NON-NLS-1$
	}

	/**
	 * @return the test suite for class fields
	 */
	public static Test suite() {
		return buildTestSuite(InvalidClassFieldTagTests.class);
	}

	public void testInvalidClassFieldTag1I() {
		x1(true);
	}

	public void testInvalidClassFieldTag1F() {
		x1(false);
	}

	/**
	 * Tests an invalid @noreference tag on final fields in inner / outer classes
	 * using an incremental build
	 */
	private void x1(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(14));
		setExpectedMessageArgs(new String[][] {
				{"@noreference", BuilderMessages.TagValidator_private_field}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_private_field}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_private_field}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_a_field_that_is_not_visible}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_a_field_that_is_not_visible}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_a_field_that_is_not_visible}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_a_field_that_is_not_visible}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_a_field_that_is_not_visible}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_a_field_that_is_not_visible}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_private_field}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_a_package_default_field}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_a_package_default_field}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_a_package_default_field}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_a_package_default_field} //$NON-NLS-1$
		});
		deployTagTest("test1.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidClassFieldTag2I() {
		x2(true);
	}

	public void testInvalidClassFieldTag2F() {
		x2(false);
	}

	/**
	 * Tests a valid @noreference tag on three final fields in a class in the default package
	 * using an incremental build
	 */
	private void x2(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(2));
		setExpectedMessageArgs(new String[][] {
				{"@noreference", BuilderMessages.TagValidator_private_field}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_a_package_default_field} //$NON-NLS-1$
		});
		deployTagTest("test2.java", inc, true); //$NON-NLS-1$
	}

	public void testInvalidClassFieldTag3I() {
		x3(true);
	}

	public void testInvalidClassFieldTag3F() {
		x3(false);
	}

	/**
	 * Tests a invalid @noreference tag on static final fields in inner /outer class
	 * using a full build
	 */
	private void x3(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(14));
		setExpectedMessageArgs(new String[][] {
				{"@noreference", BuilderMessages.TagValidator_a_field_that_is_not_visible}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_a_field_that_is_not_visible}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_a_field_that_is_not_visible}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_a_field_that_is_not_visible}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_a_field_that_is_not_visible}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_a_field_that_is_not_visible}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_private_field}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_private_field}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_private_field}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_private_field}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_a_package_default_field}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_a_package_default_field}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_a_package_default_field}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_a_package_default_field} //$NON-NLS-1$
		});
		deployTagTest("test3.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidClassFieldTag4I() {
		x4(true);
	}

	public void testInvalidClassFieldTag4F() {
		x4(false);
	}

	/**
	 * Tests a valid @noreference tag on three static final fields in a class in the default package
	 * using a full build
	 */
	private void x4(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(2));
		setExpectedMessageArgs(new String[][] {
				{"@noreference", BuilderMessages.TagValidator_private_field}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_a_package_default_field} //$NON-NLS-1$
		});
		deployTagTest("test4.java", inc, true); //$NON-NLS-1$
	}

	public void testInvalidClassFieldTag5I() {
		x5(true);
	}

	public void testInvalidClassFieldTag5F() {
		x5(false);
	}

	/**
	 * Tests a invalid @noextend tag on fields in inner /outer class
	 * using a full build
	 */
	private void x5(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(16));
		setExpectedMessageArgs(new String[][] {
				{"@noextend", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_field} //$NON-NLS-1$
		});
		deployTagTest("test5.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidClassFieldTag6I() {
		x6(true);
	}

	public void testInvalidClassFieldTag6F() {
		x6(false);
	}

	/**
	 * Tests a invalid @noextend tag on three fields in a class in the default package
	 * using an incremental build
	 */
	private void x6(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@noextend", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_field} //$NON-NLS-1$
		});
		deployTagTest("test6.java", inc, true); //$NON-NLS-1$
	}

	public void testInvalidClassFieldTag7I() {
		x7(true);
	}

	public void testInvalidClassFieldTag7F() {
		x7(false);
	}

	/**
	 * Tests an invalid @noimplement tag on fields in inner / outer classes
	 * using an incremental build
	 */
	private void x7(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(16));
		setExpectedMessageArgs(new String[][] {
				{"@noimplement", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_field} //$NON-NLS-1$
		});
		deployTagTest("test7.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidClassFieldTag8I() {
		x8(true);
	}

	public void testInvalidClassFieldTag8F() {
		x8(false);
	}

	/**
	 * Tests a invalid @noimplement tag on three fields in a class in the default package
	 * using a full build
	 */
	private void x8(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@noimplement", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_field} //$NON-NLS-1$
		});
		deployTagTest("test8.java", inc, true); //$NON-NLS-1$
	}

	public void testInvalidClassFieldTag9I() {
		x9(true);
	}

	public void testInvalidClassFieldTag9F() {
		x9(false);
	}

	/**
	 * Tests a invalid @nooverride tag on fields in inner /outer class
	 * using a full build
	 */
	private void x9(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(16));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_a_field} //$NON-NLS-1$
		});
		deployTagTest("test9.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidClassFieldTag10I() {
		x10(true);
	}

	public void testInvalidClassFieldTag10F() {
		x10(false);
	}

	/**
	 * Tests a valid @nooverride tag on three fields in a class in the default package
	 * using a full build
	 */
	private void x10(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_a_field} //$NON-NLS-1$
		});
		deployTagTest("test10.java", inc, true); //$NON-NLS-1$
	}

	public void testInvalidClassFieldTag11I() {
		x11(true);
	}

	public void testInvalidClassFieldTag11F() {
		x11(false);
	}

	/**
	 * Tests a invalid @noinstantiate tag on fields in inner /outer class
	 * using a full build
	 */
	private void x11(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(16));
		setExpectedMessageArgs(new String[][] {
				{"@noinstantiate", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_field} //$NON-NLS-1$
		});
		deployTagTest("test11.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidClassFieldTag12I() {
		x12(true);
	}

	public void testInvalidClassFieldTag12F() {
		x12(false);
	}

	/**
	 * Tests a valid @noinstantiate tag on three fields in a class in the default package
	 * using an incremental build
	 */
	private void x12(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@noinstantiate", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_field}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_field} //$NON-NLS-1$
		});
		deployTagTest("test12.java", inc, true); //$NON-NLS-1$
	}
}
