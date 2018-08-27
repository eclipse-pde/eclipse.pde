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

import junit.framework.Test;

/**
 * Tests the tags that are valid on an interface
 *
 * @since 1.0
 */
public class ValidInterfaceTagTests extends InvalidInterfaceTagTests {

	public ValidInterfaceTagTests(String name) {
		super(name);
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(ValidInterfaceTagTests.class);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("valid"); //$NON-NLS-1$
	}

	public void testValidInterfaceTag1I() {
		x1(true);
	}

	public void testValidInterfaceTag1F() {
		x1(false);
	}

	/**
	 * Tests that @noimplement is a valid tag on an interface in the the testing
	 * package a.b.c
	 */
	private void x1(boolean inc) {
		deployTagTest("test1.java", inc, false); //$NON-NLS-1$
	}

	public void testValidInterfaceTag3I() {
		x3(true);
	}

	public void testValidInterfaceTag3F() {
		x3(false);
	}

	/**
	 * Tests that @noimplement is a valid tag on an interface in the the testing
	 * package a.b.c
	 */
	private void x3(boolean inc) {
		deployTagTest("test3.java", inc, false); //$NON-NLS-1$
	}

	public void testValidInterfaceTag4I() {
		x4(true);
	}

	public void testValidInterfaceTag4F() {
		x4(false);
	}

	/**
	 * Tests that @noimplement is a valid tag on a variety of inner / outer /
	 * top-level interfaces in the the testing package a.b.c
	 */
	private void x4(boolean inc) {
		deployTagTest("test4.java", inc, false); //$NON-NLS-1$
	}

	public void testValidInterfaceTag5I() {
		x5(true);
	}

	public void testValidInterfaceTag5F() {
		x5(false);
	}

	/**
	 * Tests that @noimplement is a valid tag on an interface in the the testing
	 * package a.b.c
	 */
	private void x5(boolean inc) {
		deployTagTest("test5.java", inc, false); //$NON-NLS-1$
	}

	@Override
	public void testInvalidInterfaceTag6I() {
		x6(true);
	}

	@Override
	public void testInvalidInterfaceTag6F() {
		x6(false);
	}

	/**
	 * Tests having an @noextend tag on an interface in package a.b.c
	 */
	private void x6(boolean inc) {
		deployTagTest("test6.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidInterfaceTag7I() {
		x7(true);
	}

	public void testInvalidInterfaceTag7F() {
		x7(false);
	}

	/**
	 * Tests having an @noreference tag on outer / inner interfaces in package
	 * a.b.c
	 */
	private void x7(boolean inc) {
		deployTagTest("test7.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidInterfaceTag8I() {
		x8(true);
	}

	public void testInvalidInterfaceTag8F() {
		x8(false);
	}

	/**
	 * Tests having an @noreference tag on outer/inner interfaces in package
	 * a.b.c
	 */
	private void x8(boolean inc) {
		deployTagTest("test8.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidInterfaceTag9I() {
		x9(true);
	}

	public void testInvalidInterfaceTag9F() {
		x9(false);
	}

	/**
	 * Tests having an @noextend tag on an outer interface in package a.b.c
	 */
	private void x9(boolean inc) {
		deployTagTest("test9.java", inc, false); //$NON-NLS-1$
	}
}
