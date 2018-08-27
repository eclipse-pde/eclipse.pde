/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
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
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;

import junit.framework.Test;

/**
 * Tests valid tags on Java 8 interface methods
 */
public class ValidJava8InterfaceTagTests extends
		ValidInterfaceMethodTagTests {

	public ValidJava8InterfaceTagTests(String name) {
		super(name);
	}

	@Override
	protected String getTestCompliance() {
		return JavaCore.VERSION_1_8;
	}

	@Override
	protected IPath getTestSourcePath() {
		return new Path("tags").append("java8").append("interface").append("valid"); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$ //$NON-NLS-4$
	}

	@Override
	protected String getTestingProjectName() {
		return "java8tags"; //$NON-NLS-1$
	}

	/**
	 * @return the test for this class
	 */
	public static Test suite() {
		return buildTestSuite(ValidJava8InterfaceTagTests.class);
	}

	public void testNoOverrideOnDefaultI() {
		x1(true);
	}

	public void testNoOverrideOnDefaultF() {
		x1(false);
	}

	/**
	 * Tests the @nooverride tag on a default method
	 */
	private void x1(boolean inc) {
		deployTagTest("test1.java", inc, false); //$NON-NLS-1$
	}

	public void testValidTagsOnFunctionalInterfaceI() {
		x2(true);
	}

	public void testValidTagsOnFunctionalInterfaceF() {
		x2(false);
	}

	/**
	 * Tests a variety of tags on a functional interface
	 */
	private void x2(boolean inc) {
		deployTagTest("test2.java", inc, false); //$NON-NLS-1$
	}
}
