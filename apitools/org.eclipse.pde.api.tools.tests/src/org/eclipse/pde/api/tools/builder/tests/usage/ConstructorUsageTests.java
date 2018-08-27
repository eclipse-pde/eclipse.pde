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
 * Tests a variety of restricted constructor usages, where the callee has noreference restrictions
 *
 * @since 1.0
 */
public class ConstructorUsageTests extends UsageTest {

	protected static final String CONST_CLASS_NAME = "ConstructorUsageClass"; //$NON-NLS-1$

	private static int pid = -1;

	public ConstructorUsageTests(String name) {
		super(name);
	}

	@Override
	protected int getDefaultProblemId() {
		if(pid == -1) {
			pid = ApiProblemFactory.createProblemId(
					IApiProblem.CATEGORY_USAGE,
					IElementDescriptor.METHOD,
					IApiProblem.ILLEGAL_REFERENCE,
					IApiProblem.CONSTRUCTOR_METHOD);
		}
		return pid;
	}

	public static Test suite() {
		return buildTestSuite(ConstructorUsageTests.class);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("method"); //$NON-NLS-1$
	}

	/**
	 * Tests that calls the a variety of restricted constructors are properly reported as problems
	 * using a full build
	 */
	public void testConstructorUsageTests1F() {
		x1(false);
	}

	/**
	 * Tests that calls the a variety of restricted constructors are properly reported as problems
	 * using an incremental build
	 */
	public void testConstructorUsageTests1I() {
		x1(true);
	}

	private void x1(boolean inc) {
		//TODO uncomment once https://bugs.eclipse.org/bugs/show_bug.cgi?id=247028 has been fixed
		setExpectedProblemIds(getDefaultProblemIdSet(12));
		String typename = "testCN1"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] {
				{CONST_CLASS_NAME+"()", typename}, //$NON-NLS-1$
				{CONST_CLASS_NAME+"(int, Object, char[])", typename}, //$NON-NLS-1$
				{"inner()", typename}, //$NON-NLS-1$
				{CONST_CLASS_NAME+"()", INNER_NAME1}, //$NON-NLS-1$
				{CONST_CLASS_NAME+"(int, Object, char[])", INNER_NAME1}, //$NON-NLS-1$
				{"inner()", INNER_NAME1}, //$NON-NLS-1$
				{CONST_CLASS_NAME+"()", INNER_NAME2}, //$NON-NLS-1$
				{CONST_CLASS_NAME+"(int, Object, char[])", INNER_NAME2}, //$NON-NLS-1$
				{"inner()", INNER_NAME2}, //$NON-NLS-1$
				{CONST_CLASS_NAME+"()", OUTER_NAME}, //$NON-NLS-1$
				{CONST_CLASS_NAME+"(int, Object, char[])", OUTER_NAME}, //$NON-NLS-1$
				{"inner()", OUTER_NAME}, //$NON-NLS-1$
		});
		deployUsageTest(typename, inc);
	}
}
