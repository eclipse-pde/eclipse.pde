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
 * Tests the valid @noreference tags on enum methods
 *
 * @since 1.0
 */
public class ValidEnumMethodTagTests extends ValidMethodTagTests {

	public ValidEnumMethodTagTests(String name) {
		super(name);
	}

	@Override
	protected String getTestCompliance() {
		return JavaCore.VERSION_1_5;
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("enum"); //$NON-NLS-1$
	}

	/**
	 * @return test for this class
	 */
	public static Test suite() {
		return buildTestSuite(ValidEnumMethodTagTests.class);
	}

	/**
	 * Tests the supported @noreference tag on enum methods
	 * using an incremental build
	 */
	public void testValidEnumMethodTag1I() {
		x1(true);
	}

	/**
	 * Tests the supported @noreference tag on enum methods
	 * using a full build
	 */
	public void testValidEnumMethodTag1F() {
		x1(false);
	}

	private void x1(boolean inc) {
		deployTagTest("test1.java", inc, false); //$NON-NLS-1$
	}

	/**
	 * Tests the supported @noreference tag on inner enum methods
	 * using an incremental build
	 */
	public void testValidEnumMethodTag3I() {
		x3(true);
	}

	/**
	 * Tests the supported @noreference tag on inner enum methods
	 * using a full build
	 */
	public void testValidEnumMethodTag3F() {
		x3(false);
	}

	private void x3(boolean inc) {
		deployTagTest("test3.java", inc, false); //$NON-NLS-1$
	}

	/**
	 * Tests the supported @noreference tag on a variety of inner / outer enum methods
	 * using an incremental build
	 */
	public void testValidEnumMethodTag4I() {
		x4(true);
	}

	/**
	 * Tests the supported @noreference tag on a variety of inner / outer enum methods
	 * using a full build
	 */
	public void testValidEnumMethodTag4F() {
		x4(false);
	}

	private void x4(boolean inc) {
		deployTagTest("test4.java", inc, false); //$NON-NLS-1$
	}

	/**
	 * Tests the supported @noreference tag on enum methods in the default package
	 * using an incremental build
	 */
	public void testValidEnumMethodTag5I() {
		x5(true);
	}

	/**
	 * Tests the supported @noreference tag on enum methods in the default package
	 * using a full build
	 */
	public void testValidEnumMethodTag5F() {
		x5(false);
	}

	private void x5(boolean inc) {
		deployTagTest("test5.java", inc, true); //$NON-NLS-1$
	}
}
