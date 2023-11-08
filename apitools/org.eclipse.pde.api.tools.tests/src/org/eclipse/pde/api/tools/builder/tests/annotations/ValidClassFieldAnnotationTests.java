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

import java.io.File;

import org.eclipse.core.runtime.IPath;

import junit.framework.Test;

/**
 * Tests valid annotations on class fields
 *
 * @since 1.0.400
 */
public class ValidClassFieldAnnotationTests extends FieldAnnotationTest {

	/**
	 * Constructor
	 *
	 * @param name
	 */
	public ValidClassFieldAnnotationTests(String name) {
		super(name);
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(ValidClassFieldAnnotationTests.class);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("valid").append(File.separator).append("class"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests a valid @NoReference annotation on three fields in a class using an
	 * incremental build
	 */
	public void testValidClassFieldTag1I() {
		x1(true);
	}

	/**
	 * Tests a valid @NoReference annotation on three fields in a class using a
	 * full build
	 */
	public void testValidClassFieldTag1F() {
		x1(false);
	}

	private void x1(boolean inc) {
		deployAnnotationTest("test1.java", inc, false); //$NON-NLS-1$
	}

	/**
	 * Tests a valid @NoReference annotation on three static fields in a class
	 * using an incremental build
	 */
	public void testValidClassFieldTag2I() {
		x2(true);
	}

	/**
	 * Tests a valid @NoReference annotation on three static fields in a class
	 * using a full build
	 */
	public void testValidClassFieldTag2F() {
		x2(false);
	}

	private void x2(boolean inc) {
		deployAnnotationTest("test2.java", inc, false); //$NON-NLS-1$
	}

	/**
	 * Tests a valid @NoReference annotation on three fields in an inner class
	 * using an incremental build
	 */
	public void testValidClassFieldTag5I() {
		x5(true);
	}

	/**
	 * Tests a valid @NoReference annotation on three fields in an inner class
	 * using a full build
	 */
	public void testValidClassFieldTag5F() {
		x5(false);
	}

	private void x5(boolean inc) {
		deployAnnotationTest("test5.java", inc, false); //$NON-NLS-1$
	}

	/**
	 * Tests a valid @NoReference annotation on three static fields in an inner
	 * class using an incremental build
	 */
	public void testValidClassFieldTag6I() {
		x6(true);
	}

	/**
	 * Tests a valid @NoReference annotation on three static fields in an inner
	 * class using a full build
	 */
	public void testValidClassFieldTag6F() {
		x6(false);
	}

	private void x6(boolean inc) {
		deployAnnotationTest("test6.java", inc, false); //$NON-NLS-1$
	}

	/**
	 * Tests a valid @NoReference annotation on a variety of fields in inner and
	 * outer classes using an incremental build
	 */
	public void testValidClassFieldTag7I() {
		x7(true);
	}

	/**
	 * Tests a valid @NoReference annotation on a variety of fields in inner and
	 * outer classes using a full build
	 */
	public void testValidClassFieldTag7F() {
		x7(false);
	}

	private void x7(boolean inc) {
		deployAnnotationTest("test7.java", inc, false); //$NON-NLS-1$
	}

	/**
	 * Tests a valid @NoReference annotation on three fields in a class in the
	 * default package using an incremental build
	 */
	public void testValidClassFieldTag8I() {
		x8(true);
	}

	/**
	 * Tests a valid @NoReference annotation on three fields in a class in the
	 * default package using a full build
	 */
	public void testValidClassFieldTag8F() {
		x8(false);
	}

	private void x8(boolean inc) {
		deployAnnotationTest("test8.java", inc, true); //$NON-NLS-1$
	}
}
