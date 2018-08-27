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
import org.eclipse.jdt.core.JavaCore;

import junit.framework.Test;

/**
 * Tests valid use of @noreference tags in an enum
 *
 * @since 1.0
 */
public class ValidEnumFieldTagTests extends ValidFieldTagTests {

	public ValidEnumFieldTagTests(String name) {
		super(name);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("enum"); //$NON-NLS-1$
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(ValidEnumFieldTagTests.class);
	}

	@Override
	protected String getTestCompliance() {
		return JavaCore.VERSION_1_5;
	}

	/**
	 * Tests that @noreference is valid for non-final, non-static-final enum fields
	 * using an incremental build
	 */
	public void testValidEnumFieldTag1I() {
		x1(true);
	}

	/**
	 * Tests that @noreference is valid for non-final, non-static-final enum fields
	 * using a full build
	 */
	public void testValidEnumFieldTag1F() {
		x1(false);
	}

	private void x1(boolean inc) {
		deployTagTest("test1.java", inc, false); //$NON-NLS-1$
	}

	/**
	 * Tests that @noreference is valid for non-final, non-static-final fields
	 * in an inner enum using an incremental build
	 */
	public void testValidEnumFieldTag4I() {
		x4(true);
	}

	/**
	 * Tests that @noreference is valid for non-final, non-static-final fields
	 * in an inner enum using a full build
	 */
	public void testValidEnumFieldTag4F() {
		x4(false);
	}

	private void x4(boolean inc) {
		deployTagTest("test4.java", inc, false); //$NON-NLS-1$
	}

	/**
	 * Tests that @noreference is valid for non-final, non-static-final fields
	 * in an enum in the default package using an incremental build
	 */
	public void testValidEnumFieldTag3I() {
		x3(true);
	}

	/**
	 * Tests that @noreference is valid for non-final, non-static-final fields
	 * in an enum in the default package using a full build
	 */
	public void testValidEnumFieldTag3F() {
		x3(false);
	}

	private void x3(boolean inc) {
		deployTagTest("test3.java", inc, true); //$NON-NLS-1$
	}

	/**
	 * Tests that @noreference is valid for non-final, non-static-final fields
	 * in a variety of inner / outer enums using an incremental build
	 */
	public void testValidEnumFieldTag5I() {
		x5(true);
	}

	/**
	 * Tests that @noreference is valid for non-final, non-static-final fields
	 * in a variety of inner / outer enums using a full build
	 */
	public void testValidEnumFieldTag5F() {
		x5(false);
	}

	private void x5(boolean inc) {
		deployTagTest("test5.java", inc, false); //$NON-NLS-1$
	}
}
