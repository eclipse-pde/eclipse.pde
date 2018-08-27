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
 * Tests the tags that are valid on a class
 *
 * @since 1.0
 */
public class ValidClassTagTests extends InvalidClassTagTests {

	public ValidClassTagTests(String name) {
		super(name);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("valid"); //$NON-NLS-1$
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(ValidClassTagTests.class);
	}

	/**
	 * Tests that @noextend and @noinstantiate are valid tags on a class in the
	 * the testing package a.b.c using an incremental build
	 */
	public void testValidClassTag1I() {
		x1(true);
	}

	/**
	 * Tests that @noextend and @noinstantiate are valid tags on a class in the
	 * the testing package a.b.c using a full build
	 */
	public void testValidClassTag1F() {
		x1(false);
	}

	private void x1(boolean inc) {
		deployTagTest("test1.java", inc, false); //$NON-NLS-1$
	}



	/**
	 * Tests that @noextend and @noinstantiate are valid tags on an inner class in the
	 * the testing package a.b.c using an incremental build
	 */
	public void testValidClassTag5I() {
		x5(true);
	}

	/**
	 * Tests that @noextend and @noinstantiate are valid tags on an inner class in the
	 * the testing package a.b.c using a full build
	 */
	public void testValidClassTag5F() {
		x5(false);
	}

	private void x5(boolean inc) {
		deployTagTest("test5.java", inc, false); //$NON-NLS-1$
	}
}
