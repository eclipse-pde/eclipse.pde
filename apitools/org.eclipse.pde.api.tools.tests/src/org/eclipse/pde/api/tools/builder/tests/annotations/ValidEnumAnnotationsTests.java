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
 * Tests valid use of annotations on enums
 *
 * @since 1.0.400
 */
public class ValidEnumAnnotationsTests extends InvalidEnumAnnotationsTests {

	/**
	 * Constructor
	 *
	 * @param name
	 */
	public ValidEnumAnnotationsTests(String name) {
		super(name);
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(ValidEnumAnnotationsTests.class);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("valid"); //$NON-NLS-1$
	}

	public void testValidEnumTag1I() {
		x1(true);
	}

	@Override
	public void testInvalidEnumTag1F() {
		x1(false);
	}

	/**
	 * Tests having an @NoReference tag on an enum in the default package
	 */
	private void x1(boolean inc) {
		deployAnnotationTest("test1.java", inc, true); //$NON-NLS-1$
	}
}
