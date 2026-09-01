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
import org.junit.jupiter.api.Test;

/**
 * Tests invalid Javadoc tags on interface methods
 *
 * @since 1.0
 */
public class InvalidInterfaceMethodTagTests extends InvalidMethodTagTests {

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("interface"); //$NON-NLS-1$
	}

	@Test

	public void testInvalidInterfaceMethodTag1I() {
		x1(true);
	}

	@Test

	public void testInvalidInterfaceMethodTag1F() {
		x1(false);
	}

	/**
	 * Tests the unsupported @noextend tag on a variety of inner / outer interface methods
	 */
	private void x1(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(6));
		setExpectedMessageArgs(new String[][] {
				{"@noextend", BuilderMessages.TagValidator_an_interface_method}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_an_interface_method}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_an_interface_method}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_an_interface_method}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_an_interface_method}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_an_interface_method} //$NON-NLS-1$
		});
		deployTagTest("test1.java", inc, false); //$NON-NLS-1$
	}

	@Test

	public void testInvalidInterfaceMethodTag2I() {
		x2(true);
	}

	@Test

	public void testInvalidInterfaceMethodTag2F() {
		x2(false);
	}

	/**
	 * Tests the unsupported @noextend tag on interface methods in the default package
	 */
	private void x2(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(2));
		setExpectedMessageArgs(new String[][] {
				{"@noextend", BuilderMessages.TagValidator_an_interface_method}, //$NON-NLS-1$
				{"@noextend", BuilderMessages.TagValidator_an_interface_method} //$NON-NLS-1$
		});
		deployTagTest("test2.java", inc, true); //$NON-NLS-1$
	}

	@Test

	public void testInvalidInterfaceMethodTag3I() {
		x3(true);
	}

	@Test

	public void testInvalidInterfaceMethodTag3F() {
		x3(false);
	}

	/**
	 * Tests the unsupported @noinstantiate tag on a variety of inner / outer interface methods
	 */
	private void x3(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(6));
		setExpectedMessageArgs(new String[][] {
				{"@noinstantiate", BuilderMessages.TagValidator_an_interface_method}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_an_interface_method}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_an_interface_method}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_an_interface_method}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_an_interface_method}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_an_interface_method} //$NON-NLS-1$
		});
		deployTagTest("test3.java", inc, false); //$NON-NLS-1$
	}

	@Test

	public void testInvalidInterfaceMethodTag4I() {
		x4(true);
	}

	@Test

	public void testInvalidInterfaceMethodTag4F() {
		x4(false);
	}

	/**
	 * Tests the unsupported @noinstantiate tag on interface methods in the default package
	 */
	private void x4(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(2));
		setExpectedMessageArgs(new String[][] {
				{"@noinstantiate", BuilderMessages.TagValidator_an_interface_method}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_an_interface_method} //$NON-NLS-1$
		});
		deployTagTest("test4.java", inc, true); //$NON-NLS-1$
	}

	@Test

	public void testInvalidInterfaceMethodTag5I() {
		x5(true);
	}

	@Test

	public void testInvalidInterfaceMethodTag5F() {
		x5(false);
	}

	/**
	 * Tests the unsupported @noimplement tag on a variety of inner / outer interface methods
	 */
	private void x5(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(6));
		setExpectedMessageArgs(new String[][] {
				{"@noimplement", BuilderMessages.TagValidator_an_interface_method}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_an_interface_method}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_an_interface_method}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_an_interface_method}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_an_interface_method}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_an_interface_method} //$NON-NLS-1$
		});
		deployTagTest("test5.java", inc, false); //$NON-NLS-1$
	}

	@Test

	public void testInvalidInterfaceMethodTag6I() {
		x6(true);
	}

	@Test

	public void testInvalidInterfaceMethodTag6F() {
		x6(false);
	}

	/**
	 * Tests the unsupported @noimplement tag on interface methods in the default package
	 */
	private void x6(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(2));
		setExpectedMessageArgs(new String[][] {
				{"@noimplement", BuilderMessages.TagValidator_an_interface_method}, //$NON-NLS-1$
				{"@noimplement", BuilderMessages.TagValidator_an_interface_method} //$NON-NLS-1$
		});
		deployTagTest("test6.java", inc, true); //$NON-NLS-1$
	}

	@Test

	public void testInvalidInterfaceMethodTag7I() {
		x7(true);
	}

	@Test

	public void testInvalidInterfaceMethodTag7F() {
		x7(false);
	}

	/**
	 * Tests the unsupported @nooverride tag on a variety of inner / outer interface methods
	 */
	private void x7(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(6));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_nondefault_interface_method}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_nondefault_interface_method}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_nondefault_interface_method}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_nondefault_interface_method}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_nondefault_interface_method}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_nondefault_interface_method} //$NON-NLS-1$
		});
		deployTagTest("test7.java", inc, false); //$NON-NLS-1$
	}

	@Test

	public void testInvalidInterfaceMethodTag8I() {
		x8(true);
	}

	@Test

	public void testInvalidInterfaceMethodTag8F() {
		x8(false);
	}

	/**
	 * Tests the unsupported @nooverride tag on interface methods in the default package
	 */
	private void x8(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(2));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_nondefault_interface_method}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_nondefault_interface_method} //$NON-NLS-1$
		});
		deployTagTest("test8.java", inc, true); //$NON-NLS-1$
	}
}
