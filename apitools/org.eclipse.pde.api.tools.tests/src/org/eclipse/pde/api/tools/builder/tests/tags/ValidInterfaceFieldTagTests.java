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
 * Tests valid uses of @noreference on interface fields
 *
 * @since 1.0
 */
public class ValidInterfaceFieldTagTests extends ValidFieldTagTests {

	public ValidInterfaceFieldTagTests(String name) {
		super(name);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("interface"); //$NON-NLS-1$
	}

	/**
	 * @return the test for this class
	 */
	public static Test suite() {
		return buildTestSuite(ValidInterfaceFieldTagTests.class);
	}

	/**
	 * Tests the valid use of an @noreference tag on a field in an interface
	 * using an incremental build
	 */
	public void testValidInterfaceFieldTag1I() {
		x1(true);
	}

	/**
	 * Tests the valid use of an @noreference tag on a field in an interface
	 * using a full build
	 */
	public void testValidInterfaceFieldTag1F() {
		x1(false);
	}

	private void x1(boolean inc) {
		deployTagTest("test1.java", inc, false); //$NON-NLS-1$
	}

	/**
	 * Tests the valid use of an @noreference tag on a field in an inner interface
	 * using an incremental build
	 */
	public void testValidInterfaceFieldTag3I() {
		x3(true);
	}

	/**
	 * Tests the valid use of an @noreference tag on a field in an inner interface
	 * using a full build
	 */
	public void testValidInterfaceFieldTag3F() {
		x3(false);
	}

	private void x3(boolean inc) {
		deployTagTest("test3.java", inc, false); //$NON-NLS-1$
	}

	/**
	 * Tests the valid use of an @noreference tag on fields in inner / outer interfaces
	 * using an incremental build
	 */
	public void testValidInterfaceFieldTag4I() {
		x4(true);
	}

	/**
	 * Tests the valid use of an @noreference tag on fields in inner / outer interfaces
	 * using a full build
	 */
	public void testValidInterfaceFieldTag4F() {
		x4(false);
	}

	private void x4(boolean inc) {
		deployTagTest("test4.java", inc, false); //$NON-NLS-1$
	}

	/**
	 * Tests the valid use of an @noreference tag on fields in interfaces
	 * using an incremental build
	 */
	public void testValidInterfaceFieldTag5I() {
		x5(true);
	}

	/**
	 * Tests the valid use of an @noreference tag on fields in interfaces
	 * using a full build
	 */
	public void testValidInterfaceFieldTag5F() {
		x5(false);
	}

	private void x5(boolean inc) {
		deployTagTest("test5.java", inc, false); //$NON-NLS-1$
	}
}
