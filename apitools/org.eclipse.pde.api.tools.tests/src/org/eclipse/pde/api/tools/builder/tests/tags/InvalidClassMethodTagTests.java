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
import org.eclipse.pde.api.tools.internal.builder.BuilderMessages;

import junit.framework.Test;

/**
 * Tests invalid javadoc tags on class methods
 *
 * @since 1.0
 */
public class InvalidClassMethodTagTests extends InvalidMethodTagTests {

	public InvalidClassMethodTagTests(String name) {
		super(name);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("class"); //$NON-NLS-1$
	}

	/**
	 * @return the test for this class
	 */
	public static Test suite() {
		return buildTestSuite(InvalidClassMethodTagTests.class);
	}

	public void testInvalidClassMethodTag1I() {
		x1(true);
	}


	public void testInvalidClassMethodTag1F() {
		x1(false);
	}

	/**
	 * Tests the unsupported @noimplement Javadoc tag on a variety of methods in a variety of inner / outer classes
	 */
	private void x1(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(16));
		setExpectedMessageArgs(new String[][] {
				{"@noimplement", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
		});
		deployTagTest("test1.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidClassMethodTag2I() {
		x2(true);
	}

	public void testInvalidClassMethodTag2F() {
		x2(false);
	}

	/**
	 * Tests the unsupported @noimplement Javadoc tag on a variety of methods in a class in the default package
	 */
	private void x2(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@noimplement", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
		});
		deployTagTest("test2.java", inc, true); //$NON-NLS-1$
	}

	public void testInvalidClassMethodTag3I() {
		x3(true);
	}

	public void testInvalidClassMethodTag3F() {
		x3(false);
	}

	/**
	 * Tests the unsupported @noextend Javadoc tag on a variety of methods in a variety of inner / outer classes
	 */
	private void x3(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(16));
		setExpectedMessageArgs(new String[][] {
				{"@noextend", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
		});
		deployTagTest("test3.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidClassMethodTag4I() {
		x4(true);
	}

	public void testInvalidClassMethodTag4F() {
		x4(false);
	}

	/**
	 * Tests the unsupported @noextend Javadoc tag on a variety of methods in a class in the default package
	 */
	private void x4(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@noextend", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
		});
		deployTagTest("test4.java", inc, true); //$NON-NLS-1$
	}

	public void testInvalidClassMethodTag5I() {
		x5(true);
	}

	public void testInvalidClassMethodTag5F() {
		x5(false);
	}

	/**
	 * Tests the unsupported @noinstantiate Javadoc tag on a variety of methods in a variety of inner / outer classes
	 */
	private void x5(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(16));
		setExpectedMessageArgs(new String[][] {
				{"@noinstantiate", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
		});
		deployTagTest("test5.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidClassMethodTag6I() {
		x6(true);
	}

	public void testInvalidClassMethodTag6F() {
		x6(false);
	}

	/**
	 * Tests the unsupported @noinstantiate Javadoc tag on a variety of methods in a class in the default package
	 */
	private void x6(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@noinstantiate", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_a_method}, //$NON-NLS-1$
		});
		deployTagTest("test6.java", inc, true); //$NON-NLS-1$
	}

	public void testInvalidClassMethodTag7I() {
		x7(true);
	}

	public void testInvalidClassMethodTag7F() {
		x7(false);
	}

	/**
	 * Tests the unsupported @nooverride Javadoc tag on private methods in a variety of inner /outer classes
	 */
	private void x7(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_private_method}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_private_method}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_private_method}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_private_method}, //$NON-NLS-1$
		});
		deployTagTest("test7.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidClassMethodTag8I() {
		x8(true);
	}

	public void testInvalidClassMethodTag8F() {
		x8(false);
	}

	/**
	 * Tests the unsupported @nooverride Javadoc tag on private methods in a class in the default package
	 */
	private void x8(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(1));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_private_method}, //$NON-NLS-1$
		});
		deployTagTest("test8.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidClassMethodTag9I() {
		x9(true);
	}

	public void testInvalidClassMethodTag9F() {
		x9(false);
	}

	/**
	 * Tests the unsupported @nooverride Javadoc tag on final methods in a variety of inner /outer classes
	 */
	private void x9(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_a_final_method}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_a_final_method}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_a_final_method}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_a_final_method}, //$NON-NLS-1$
		});
		deployTagTest("test9.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidClassMethodTag10I() {
		x10(true);
	}

	public void testInvalidClassMethodTag10F() {
		x10(false);
	}

	/**
	 * Tests the unsupported @nooverride Javadoc tag on final methods in a class in the default package
	 */
	private void x10(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(1));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_a_final_method}, //$NON-NLS-1$
		});
		deployTagTest("test10.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidClassMethodTag11I() {
		x11(true);
	}

	public void testInvalidClassMethodTag11F() {
		x11(false);
	}

	/**
	 * Tests the unsupported @noreference Javadoc tag on private methods in a variety of inner /outer classes
	 */
	private void x11(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@noreference", BuilderMessages.TagValidator_private_method}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_private_method}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_private_method}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_private_method}, //$NON-NLS-1$
		});
		deployTagTest("test11.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidClassMethodTag12I() {
		x12(true);
	}


	public void testInvalidClassMethodTag12F() {
		x12(false);
	}

	/**
	 * Tests the unsupported @noreference Javadoc tag on private methods in a class in the default package
	 */
	private void x12(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(1));
		setExpectedMessageArgs(new String[][] {
				{"@noreference", BuilderMessages.TagValidator_private_method}, //$NON-NLS-1$
		});
		deployTagTest("test12.java", inc, true); //$NON-NLS-1$
	}

	public void testInvalidClassMethodTag13I() {
		x13(true);
	}

	public void testInvalidClassMethodTag13F() {
		x13(false);
	}

	/**
	 * Tests the unsupported @nooverride tag on a static method
	 */
	private void x13(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(3));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_a_static_package_default_method}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_a_static_final_method}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_a_static_method} //$NON-NLS-1$
		});
		deployTagTest("test13.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidClassMethodTag14I() {
		x14(true);
	}

	public void testInvalidClassMethodTag14F() {
		x14(false);
	}

	/**
	 * Tests the unsupported @nooverride tag on a method in a final class
	 */
	private void x14(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_a_method_in_a_final_class}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_a_method_in_a_final_class}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_a_method_in_a_final_class}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_a_method_in_a_final_class}, //$NON-NLS-1$
		});
		deployTagTest("test14.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidClassMethodTag26I() {
		x26(true);
	}

	public void testInvalidClassMethodTag26F() {
		x26(false);
	}

	/**
	 * Tests the unsupported @nooverride Javadoc tag on package default methods in a variety of inner /outer classes
	 */
	private void x26(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_a_package_default_method}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_a_package_default_method}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_a_package_default_method}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_a_package_default_method}, //$NON-NLS-1$
		});
		deployTagTest("test26.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidClassMethodTag27I() {
		x27(true);
	}

	public void testInvalidClassMethodTag27F() {
		x27(false);
	}

	/**
	 * Tests the unsupported @nooverride Javadoc tag on package default methods in a class in the default package
	 */
	private void x27(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(1));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_a_package_default_method}, //$NON-NLS-1$
		});
		deployTagTest("test27.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidClassMethodTag28I() {
		x28(true);
	}

	public void testInvalidClassMethodTag28F() {
		x28(false);
	}

	/**
	 * Tests the unsupported @noreference Javadoc tag on package default methods in a variety of inner /outer classes
	 */
	private void x28(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@noreference", BuilderMessages.TagValidator_a_package_default_method}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_a_package_default_method}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_a_package_default_method}, //$NON-NLS-1$
				{"@noreference", BuilderMessages.TagValidator_a_package_default_method}, //$NON-NLS-1$
		});
		deployTagTest("test28.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidClassMethodTag29I() {
		x29(true);
	}


	public void testInvalidClassMethodTag29F() {
		x29(false);
	}

	/**
	 * Tests the unsupported @noreference Javadoc tag on package default methods in a class in the default package
	 */
	private void x29(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(1));
		setExpectedMessageArgs(new String[][] {
				{"@noreference", BuilderMessages.TagValidator_a_package_default_method}, //$NON-NLS-1$
		});
		deployTagTest("test29.java", inc, true); //$NON-NLS-1$
	}
}
