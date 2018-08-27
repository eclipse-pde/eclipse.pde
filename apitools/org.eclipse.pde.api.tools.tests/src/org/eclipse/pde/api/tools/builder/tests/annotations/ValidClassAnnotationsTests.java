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

import org.eclipse.core.runtime.IPath;

import junit.framework.Test;

/**
 * Tests a variety of valid annotation use on classes
 *
 * @since 1.0.400
 */
public class ValidClassAnnotationsTests extends InvalidClassAnnotationsTests {

	/**
	 * Constructor
	 *
	 * @param name
	 */
	public ValidClassAnnotationsTests(String name) {
		super(name);
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(ValidClassAnnotationsTests.class);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("valid"); //$NON-NLS-1$
	}

	/**
	 * Tests all the valid annotations on a class
	 *
	 * @throws Exception
	 */
	public void testValidClassAnnotations1I() throws Exception {
		deployAnnotationTest("test1.java", true, false); //$NON-NLS-1$
	}

	/**
	 * Tests all the valid annotations on a class
	 *
	 * @throws Exception
	 */
	public void testValidClassAnnotations1F() throws Exception {
		deployAnnotationTest("test1.java", false, false); //$NON-NLS-1$
	}

	/**
	 * Tests all the valid annotations on a class in the default package
	 *
	 * @throws Exception
	 */
	public void testValidClassAnnotations2I() throws Exception {
		deployAnnotationTest("test5.java", true, true); //$NON-NLS-1$
	}

	/**
	 * Tests all the valid annotations on a class in the default package
	 *
	 * @throws Exception
	 */
	public void testValidClassAnnotations2F() throws Exception {
		deployAnnotationTest("test5.java", false, true); //$NON-NLS-1$
	}
}
