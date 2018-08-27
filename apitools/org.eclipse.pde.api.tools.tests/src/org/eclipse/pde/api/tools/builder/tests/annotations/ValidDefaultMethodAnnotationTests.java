/*******************************************************************************
 * Copyright (c) Apr 2, 2014 IBM Corporation and others.
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

import junit.framework.Test;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.JavaCore;

/**
 * Tests valid annotations on interface default methods
 *
 * @since 1.0.600
 */
public class ValidDefaultMethodAnnotationTests extends MethodAnnotationTest {

	/**
	 * @param name
	 */
	public ValidDefaultMethodAnnotationTests(String name) {
		super(name);
	}

	@Override
	protected String getTestCompliance() {
		return JavaCore.VERSION_1_8;
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("interface").append("valid"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	protected String getTestingProjectName() {
		return "java8tags"; //$NON-NLS-1$
	}

	/**
	 * @return the test for this class
	 */
	public static Test suite() {
		return buildTestSuite(ValidDefaultMethodAnnotationTests.class);
	}

	public void testNoOverrideAnnotOnDefaultI() {
		x1(true);
	}

	public void testNoOverrideAnnotOnDefaultF() {
		x1(false);
	}

	/**
	 * Tests the @NoOverride tag on a default method
	 */
	private void x1(boolean inc) {
		deployAnnotationTest("test1.java", inc, false); //$NON-NLS-1$
	}
}
