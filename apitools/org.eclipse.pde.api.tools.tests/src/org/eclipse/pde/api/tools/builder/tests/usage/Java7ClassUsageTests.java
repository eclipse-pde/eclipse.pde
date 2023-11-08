/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.builder.tests.usage;

import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

import junit.framework.Test;

/**
 * Test class usage for Java 7 code snippets
 *
 * @since 1.0.100
 */
public class Java7ClassUsageTests extends Java7UsageTest {

	public Java7ClassUsageTests(String name) {
		super(name);
	}

	/**
	 * @return the test class for this suite
	 */
	public static Test suite() {
		return buildTestSuite(Java7ClassUsageTests.class);
	}

	/**
	 * Returns the problem id with the given kind
	 *
	 * @param kind
	 * @return the problem id
	 */
	protected int getProblemId(int kind, int flags) {
		return ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, kind, flags);
	}

	/**
	 * Tests illegal use of classes inside a string switch block (full)
	 */
	public void testStringSwitchF() {
		x1(false);
	}

	/**
	 * Tests illegal use of classes inside a string switch block
	 * (incremental)
	 */
	public void testStringSwitchI() {
		x1(true);
	}

	private void x1(boolean inc) {
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_INSTANTIATE, IApiProblem.NO_FLAGS),
				getProblemId(IApiProblem.ILLEGAL_INSTANTIATE, IApiProblem.NO_FLAGS),
				getProblemId(IApiProblem.ILLEGAL_INSTANTIATE, IApiProblem.NO_FLAGS)
		});
		String typename = "testCStringSwitch"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] {
				{ ClassUsageTests.CLASS_NAME, typename },
				{ ClassUsageTests.CLASS_NAME, typename },
				{ ClassUsageTests.CLASS_NAME, typename }
		});
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests illegal use of classes inside a multi catch block
	 * (full)
	 */
	public void testMultiCatchF() {
		x2(false);
	}

	/**
	 * Tests illegal use of classes inside a multi catch block
	 * (incremental)
	 */
	public void testMultiCatchI() {
		x2(true);
	}

	private void x2(boolean inc) {
		String exceptionTypeName = "ExceptionA"; //$NON-NLS-1$
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_INSTANTIATE, IApiProblem.NO_FLAGS),
				getProblemId(IApiProblem.ILLEGAL_INSTANTIATE, IApiProblem.NO_FLAGS)
		});
		String typename = "testCMultiCatch"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] {
				{exceptionTypeName, typename},
				{exceptionTypeName, typename}
		});
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests illegal use of classes inside a try with resources block
	 * (full)
	 */
	public void testTryWithF() {
		x3(false);
	}

	/**
	 * Tests illegal use of classes inside a try with resources block
	 * (incremental)
	 */
	public void testTryWithI() {
		x3(true);
	}

	private void x3(boolean inc) {
		String resourceTypeName = "TryWithResourcesClass"; //$NON-NLS-1$
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_INSTANTIATE, IApiProblem.NO_FLAGS),
				getProblemId(IApiProblem.ILLEGAL_INSTANTIATE, IApiProblem.NO_FLAGS)
		});
		String typename = "testCTryWith"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] {
				{resourceTypeName, typename},
				{resourceTypeName, typename}
		});
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests illegal use of classes instantiated with the diamond operator
	 * (full)
	 */
	public void testDiamondF() {
		x4(false);
	}

	/**
	 * Tests illegal use of classes instantiated with the diamond operator
	 * (incremental)
	 */
	public void testDiamondI() {
		x4(true);
	}

	private void x4(boolean inc) {
		String resourceTypeName = "GenericClassUsageClass<T>"; //$NON-NLS-1$
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_INSTANTIATE, IApiProblem.NO_FLAGS)
		});
		String typename = "testCDiamond"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] {
				{resourceTypeName, typename}
		});
		deployUsageTest(typename, inc);
	}
}
