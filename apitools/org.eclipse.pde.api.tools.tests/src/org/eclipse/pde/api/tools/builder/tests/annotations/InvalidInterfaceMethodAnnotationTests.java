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
import org.eclipse.pde.api.tools.internal.builder.BuilderMessages;

/**
 * Tests invalid annotations on interface methods
 *
 * @since 1.0.600
 */
public class InvalidInterfaceMethodAnnotationTests extends MethodAnnotationTest {

	/**
	 * @param name
	 */
	public InvalidInterfaceMethodAnnotationTests(String name) {
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
		return buildTestSuite(InvalidInterfaceMethodAnnotationTests.class);
	}

	public void testNoInstantiateF() throws Exception {
		x1(false);
	}

	public void testNoInstantiateI() throws Exception {
		x1(true);
	}

	void x1(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(1));
		setExpectedMessageArgs(new String[][] { {
				"@NoInstantiate", BuilderMessages.TagValidator_an_interface_method }, //$NON-NLS-1$
		});
		deployAnnotationTestWithErrors("test2.java", inc, false); //$NON-NLS-1$
	}

	public void testNoImplementF() throws Exception {
		x2(false);
	}

	public void testNoImplementI() throws Exception {
		x2(true);
	}

	void x2(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(1));
		setExpectedMessageArgs(new String[][] { {
						"@NoImplement", BuilderMessages.TagValidator_an_interface_method }, //$NON-NLS-1$
		});
		deployAnnotationTestWithErrors("test3.java", inc, false); //$NON-NLS-1$
	}

	public void testNoExtendF() throws Exception {
		x3(false);
	}

	public void testNoExtendI() throws Exception {
		x3(true);
	}

	void x3(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(1));
		setExpectedMessageArgs(new String[][] { {
				"@NoExtend", BuilderMessages.TagValidator_an_interface_method }, //$NON-NLS-1$
		});
		deployAnnotationTestWithErrors("test4.java", inc, false); //$NON-NLS-1$
	}

	public void testNoOverrideF() throws Exception {
		x4(false);
	}

	public void testNoOverrideI() throws Exception {
		x4(true);
	}

	void x4(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(1));
		setExpectedMessageArgs(new String[][] { {
						"@NoOverride", BuilderMessages.TagValidator_nondefault_interface_method }, //$NON-NLS-1$
		});
		deployAnnotationTest("test5.java", inc, false); //$NON-NLS-1$
	}
}
