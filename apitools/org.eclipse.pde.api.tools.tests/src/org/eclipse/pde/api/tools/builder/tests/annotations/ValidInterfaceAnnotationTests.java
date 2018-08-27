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

/**
 * Tests valid annotations on interfaces
 *
 * @since 1.0.600
 */
public class ValidInterfaceAnnotationTests extends InvalidInterfaceAnnotationTests {

	/**
	 * @param name
	 */
	public ValidInterfaceAnnotationTests(String name) {
		super(name);
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(ValidInterfaceAnnotationTests.class);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("valid"); //$NON-NLS-1$
	}

	/**
	 * Tests @NoImplement annotation on interfaces
	 *
	 * @throws Exception
	 */
	public void testNoImplementF() throws Exception {
		x1(false);
	}

	/**
	 * Tests @NoImplement annotation on interfaces
	 *
	 * @throws Exception
	 */
	public void testNoImplementI() throws Exception {
		x1(true);
	}

	private void x1(boolean inc) {
		deployAnnotationTest("test1.java", inc, false); //$NON-NLS-1$
	}

	/**
	 * Tests @NoExtend annotation on interfaces
	 *
	 * @throws Exception
	 */
	public void testNoExtendF() throws Exception {
		x2(false);
	}

	/**
	 * Tests @NoExtend annotation on interfaces
	 *
	 * @throws Exception
	 */
	public void testNoExtendI() throws Exception {
		x2(true);
	}

	private void x2(boolean inc) {
		deployAnnotationTest("test2.java", inc, false); //$NON-NLS-1$
	}

	/**
	 * Tests @NoReference annotation on interfaces
	 *
	 * @throws Exception
	 */
	public void testNoReferenceF() throws Exception {
		x3(false);
	}

	/**
	 * Tests @NoReference annotation on interfaces
	 *
	 * @throws Exception
	 */
	public void testNoReferenceI() throws Exception {
		x3(true);
	}

	private void x3(boolean inc) {
		deployAnnotationTest("test3.java", inc, false); //$NON-NLS-1$
	}
}
