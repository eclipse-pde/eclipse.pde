/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.builder.tests.usage;

import junit.framework.Test;

import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * Tests a variety of method usage where the callee is restricted in some way (API javadoc tagging)
 * 
 * @since 1.0
 */
public class MethodUsageTests extends UsageTest {
	
	protected static final String METHOD_CLASS_NAME = "MethodUsageClass";
	protected static final String METHOD_INTERFACE_NAME = "MethodUsageInterface";
	
	/**
	 * Constructor
	 * @param name
	 */
	public MethodUsageTests(String name) {
		super(name);
	}

	/**
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getDefaultProblemId()
	 */
	@Override
	protected int getDefaultProblemId() {
		return -1;
	}
	
	/**
	 * Returns a standard method usage problem allowing the kind to be specified
	 * @param kind
	 * @return problem id for the specified kind
	 */
	protected int getProblemId(int kind, int flags) {
		return ApiProblemFactory.createProblemId(
				IApiProblem.CATEGORY_USAGE, 
				IElementDescriptor.METHOD, 
				kind, flags);
	}
	
	/**
	 * @see org.eclipse.pde.api.tools.builder.tests.usage.UsageTest#getTestSourcePath()
	 */
	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("method");
	}

	public static Test suite() {
		return buildTestSuite(MethodUsageTests.class);
	}
	
	/**
	 * Tests that restricted methods called from a class instance are properly reported
	 * using a full build
	 */
	public void testMethodUsageTests1F() {
		x1(false);
	}
	
	/**
	 * Tests that restricted methods called from a class instance are properly reported
	 * using an incremental build
	 */
	public void testMethodUsageTests1I() {
		x1(true);
	}
	
	private void x1(boolean inc) {
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD)
		});
		String typename = "testM1";
		setExpectedMessageArgs(new String[][] {
				{METHOD_CLASS_NAME, INNER_NAME1, "m1()"},
				{METHOD_CLASS_NAME, INNER_NAME1, "m3()"},
				{METHOD_CLASS_NAME, INNER_NAME2, "m1()"},
				{METHOD_CLASS_NAME, INNER_NAME2, "m3()"},
				{METHOD_CLASS_NAME, OUTER_NAME, "m1()"},
				{METHOD_CLASS_NAME, OUTER_NAME, "m3()"},
		});
		deployTest(typename, inc);
	}
	
	/**
	 * Tests that restricted methods extended from a class are properly reported
	 * using a full build
	 */
	public void testMethodUsageTests2F() {
		x2(false);
	}
	
	/**
	 * Tests that restricted methods called from a class are properly reported
	 * using an incremental build
	 */
	public void testMethodUsageTests2I() {
		x2(true);
	}
	
	private void x2(boolean inc) {
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_OVERRIDE, IApiProblem.NO_FLAGS),
				getProblemId(IApiProblem.ILLEGAL_OVERRIDE, IApiProblem.NO_FLAGS),
				getProblemId(IApiProblem.ILLEGAL_OVERRIDE, IApiProblem.NO_FLAGS),
				getProblemId(IApiProblem.ILLEGAL_OVERRIDE, IApiProblem.NO_FLAGS)
		});
		String typename = "testM2";
		setExpectedMessageArgs(new String[][] {
				{METHOD_CLASS_NAME, typename, "m2()"},
				{METHOD_CLASS_NAME, INNER_NAME1, "m2()"},
				{METHOD_CLASS_NAME, INNER_NAME2, "m2()"},
				{METHOD_CLASS_NAME, OUTER_NAME, "m2()"}
		});
		deployTest(typename, inc);
	}
	
	/**
	 * Tests that accessing restricted static methods from a class are properly reported
	 * using a full build
	 */
	public void testMethodUsageTests3F() {
		x3(false);
	}
	
	/**
	 * Tests that accessing restricted static methods from a class are properly reported
	 * using an incremental build
	 */
	public void testMethodUsageTests3I() {
		x3(true);
	}
	
	private void x3(boolean inc) {
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD)
		});
		String typename = "testM3";
		setExpectedMessageArgs(new String[][] {
				{METHOD_CLASS_NAME, INNER_NAME1, "m4()"},
				{METHOD_CLASS_NAME, INNER_NAME1, "m6()"},
				{METHOD_CLASS_NAME, INNER_NAME2, "m4()"},
				{METHOD_CLASS_NAME, INNER_NAME2, "m6()"},
				{METHOD_CLASS_NAME, OUTER_NAME, "m4()"},
				{METHOD_CLASS_NAME, OUTER_NAME, "m6()"},
				{METHOD_CLASS_NAME, typename, "m4()"},
				{METHOD_CLASS_NAME, typename, "m6()"}
		});
		deployTest(typename, inc);
	}
	
	/**
	 * Tests that accessing restricted static methods from an implementing class are properly reported
	 * using a full build
	 */
	public void testMethodUsageTests4F() {
		x4(false);
	}
	
	/**
	 * Tests that accessing restricted interface methods from an implementing class are properly reported
	 * using an incremental build
	 */
	public void testMethodUsageTests4I() {
		x4(true);
	}
	
	private void x4(boolean inc) {
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD)
		});
		String typename = "testM4";
		setExpectedMessageArgs(new String[][] {
				{METHOD_INTERFACE_NAME, INNER_NAME1, "m1()"},
				{METHOD_INTERFACE_NAME, INNER_NAME1, "m3()"},
				{METHOD_INTERFACE_NAME, INNER_NAME2, "m1()"},
				{METHOD_INTERFACE_NAME, INNER_NAME2, "m3()"},
				{METHOD_INTERFACE_NAME, OUTER_NAME, "m1()"},
				{METHOD_INTERFACE_NAME, OUTER_NAME, "m3()"},
		});
		deployTest(typename, inc);
	}
}
