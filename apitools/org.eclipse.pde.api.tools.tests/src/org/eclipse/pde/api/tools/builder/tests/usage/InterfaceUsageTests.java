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
package org.eclipse.pde.api.tools.builder.tests.usage;

import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

import junit.framework.Test;

/**
 * Tests a variety of interface uses, where the callee has API restrictions
 *
 * @since 1.0
 */
public class InterfaceUsageTests extends UsageTest {

	protected static final String INTERFACE_NAME = "InterfaceUsageInterface"; //$NON-NLS-1$

	public InterfaceUsageTests(String name) {
		super(name);
	}

	@Override
	protected int getDefaultProblemId() {
		return -1;
	}

	private int getProblemId(int kind, int flags) {
		return ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, kind, flags);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("interface"); //$NON-NLS-1$
	}

	public static Test suite() {
		return buildTestSuite(InterfaceUsageTests.class);
	}

	/**
	 * Tests that extending an @noimplement interface properly reports no usage
	 * problems using a full build
	 */
	public void testInterfaceUsageTests1F() {
		x1(false);
	}

	/**
	 * Tests that extending an @noimplement interface properly reports no usage
	 * problems using an incremental build
	 */
	public void testInterfaceUsageTests1I() {
		x1(true);
	}

	private void x1(boolean inc) {
		expectingNoProblems();
		deployUsageTest("testI1", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that implementing an @noimplement interface properly reports the
	 * usage problems using a full build
	 */
	public void testInterfaceUsageTests2F() {
		x2(false);
	}

	/**
	 * Tests that implementing an @noimplement interface properly reports the
	 * usage problems using an incremental build
	 */
	public void testInterfaceUsageTests2I() {
		x2(true);
	}

	private void x2(boolean inc) {
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS),
				getProblemId(IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS) });
		String typename = "testI2"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] {
				{ INTERFACE_NAME, OUTER_NAME }, { INTERFACE_NAME, typename } });
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests that extending an @noextend interface properly reports the usage
	 * problems using a full build
	 */
	public void testIllegalExtendInterfaceF() {
		x3(false);
	}

	/**
	 * Tests that extending an @noextend interface properly reports the usage
	 * problems using an incremental build
	 */
	public void testIllegalExtendInterfaceI() {
		x3(true);
	}

	private void x3(boolean inc) {
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_EXTEND, IApiProblem.NO_FLAGS),
				getProblemId(IApiProblem.ILLEGAL_EXTEND, IApiProblem.NO_FLAGS),
				getProblemId(IApiProblem.ILLEGAL_EXTEND, IApiProblem.NO_FLAGS) });
		String typename = "testI3"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] {
				{ "InterfaceUsageInterface2", typename }, //$NON-NLS-1$
				{ "Iinner", "Iouter" }, //$NON-NLS-1$ //$NON-NLS-2$
				{ "Iinner", "inner" } //$NON-NLS-1$ //$NON-NLS-2$
		});
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests that an interface tagged with &#64;noreference properly flags usage
	 * of its members as no reference
	 *
	 * @throws Exception
	 * @since 1.0.300
	 */
	public void testNoRefInterface1I() throws Exception {
		x4(true);
	}

	/**
	 * Tests that an interface tagged with &#64;noreference properly flags usage
	 * of its members as no reference
	 *
	 * @throws Exception
	 * @since 1.0.300
	 */
	public void testNoRefInterface1F() throws Exception {
		x4(false);
	}

	private void x4(boolean inc) {
		String typename = "testI4"; //$NON-NLS-1$
		setExpectedProblemIds(new int[] { getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD) });
		setExpectedMessageArgs(new String[][] { {
				"NoRefInterface", typename, "noRefInterfaceMethod()" } //$NON-NLS-1$ //$NON-NLS-2$
		});
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests that an interface tagged with &#64;noreference properly flags usage
	 * of its member interfaces as no reference
	 *
	 * @throws Exception
	 * @since 1.0.300
	 */
	public void testNoRefInterface2I() throws Exception {
		x5(true);
	}

	/**
	 * Tests that an interface tagged with &#64;noreference properly flags usage
	 * of its member interfaces as no reference
	 *
	 * @throws Exception
	 * @since 1.0.300
	 */
	public void testNoRefInterface2F() throws Exception {
		x5(false);
	}

	private void x5(boolean inc) {
		String typename = "testI5"; //$NON-NLS-1$
		setExpectedProblemIds(new int[] { getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD) });
		setExpectedMessageArgs(new String[][] { {
				"Inner", typename, "noRefInterfaceMethod()" } //$NON-NLS-1$ //$NON-NLS-2$
		});
		deployUsageTest(typename, inc);
	}
}
