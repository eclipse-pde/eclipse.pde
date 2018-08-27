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
 * Tests valid javadoc tags for class methods
 *
 * @since 1.0
 */
public class ValidClassMethodTagTests extends ValidMethodTagTests {

	public ValidClassMethodTagTests(String name) {
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
		return buildTestSuite(ValidClassMethodTagTests.class);
	}

	/**
	 * Tests valid javadoc tags on methods in a class
	 * using an incremental build
	 */
	public void testValidClassMethodTag1I() {
		x1(true);
	}

	/**
	 * Tests valid javadoc tags on methods in a class
	 * using a full build
	 */
	public void testValidClassMethodTag1F() {
		x1(false);
	}

	private void x1(boolean inc) {
		deployTagTest("test1.java", inc, false); //$NON-NLS-1$
	}

	/**
	 * Tests valid javadoc tags on methods in an inner class
	 * using an incremental build
	 */
	public void testValidClassMethodTag3I() {
		x3(true);
	}

	/**
	 * Tests valid javadoc tags on methods in an inner class
	 * using a full build
	 */
	public void testValidClassMethodTag3F() {
		x3(false);
	}

	private void x3(boolean inc) {
		deployTagTest("test3.java", inc, false); //$NON-NLS-1$
	}

	/**
	 * Tests valid javadoc tags on methods in inner / outer classes
	 * using an incremental build
	 */
	public void testValidClassMethodTag4I() {
		x4(true);
	}

	/**
	 * Tests valid javadoc tags on methods in inner /outer classes
	 * using a full build
	 */
	public void testValidClassMethodTag4F() {
		x4(false);
	}

	private void x4(boolean inc) {
		deployTagTest("test4.java", inc, false); //$NON-NLS-1$
	}

	/**
	 * Tests valid javadoc tags on methods in a class in the default package
	 * using an incremental build
	 */
	public void testValidClassMethodTag5I() {
		x5(true);
	}

	/**
	 * Tests valid javadoc tags on methods in a class in the default package
	 * using a full build
	 */
	public void testValidClassMethodTag5F() {
		x5(false);
	}

	private void x5(boolean inc) {
		deployTagTest("test5.java", inc, false); //$NON-NLS-1$
	}

	/**
	 * Tests valid javadoc tags on constructors in a class
	 * using an incremental build
	 */
	public void testValidClassMethodTag6I() {
		x6(true);
	}

	/**
	 * Tests valid javadoc tags on constructors in a class
	 * using a full build
	 */
	public void testValidClassMethodTag6F() {
		x6(false);
	}

	private void x6(boolean inc) {
		deployTagTest("test6.java", inc, false); //$NON-NLS-1$
	}

	/**
	 * Tests valid javadoc tags on constructors in an inner class
	 * using an incremental build
	 */
	public void testValidClassMethodTag8I() {
		x8(true);
	}

	/**
	 * Tests valid javadoc tags on constructors in an inner class
	 * using a full build
	 */
	public void testValidClassMethodTag8F() {
		x8(false);
	}

	private void x8(boolean inc) {
		deployTagTest("test8.java", inc, false); //$NON-NLS-1$
	}

	/**
	 * Tests valid javadoc tags on constructors in inner / outer classes
	 * using an incremental build
	 */
	public void testValidClassMethodTag9I() {
		x9(true);
	}

	/**
	 * Tests valid javadoc tags on constructors in inner /outer classes
	 * using a full build
	 */
	public void testValidClassMethodTag9F() {
		x9(false);
	}

	private void x9(boolean inc) {
		deployTagTest("test9.java", inc, false); //$NON-NLS-1$
	}

	/**
	 * Tests valid javadoc tags on constructors in a class in the default package
	 * using an incremental build
	 */
	public void testValidClassMethodTag10I() {
		x10(true);
	}

	/**
	 * Tests valid javadoc tags on constructors in a class in the default package
	 * using a full build
	 */
	public void testValidClassMethodTag10F() {
		x10(false);
	}

	private void x10(boolean inc) {
		deployTagTest("test10.java", inc, false); //$NON-NLS-1$
	}
}
