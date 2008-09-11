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

import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

import junit.framework.Test;

/**
 * Tests a variety of class usages where the callee has API restrictions
 * 
 * @since 1.0
 */
public class ClassUsageTests extends UsageTest {

	protected static final String CLASS_NAME = "ClassUsageClass";
	
	/**
	 * Constructor
	 * @param name
	 */
	public ClassUsageTests(String name) {
		super(name);
	}

	/**
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getDefaultProblemId()
	 */
	@Override
	protected int getDefaultProblemId() {
		return 0;
	}

	/**
	 * Returns the problem id with the given kind
	 * @param kind
	 * @return the problem id
	 */
	private int getProblemId(int kind) {
		return ApiProblemFactory.createProblemId(
				IApiProblem.CATEGORY_USAGE, 
				IElementDescriptor.T_REFERENCE_TYPE, 
				kind, 
				IApiProblem.NO_FLAGS);
	}
	
	public static Test suite() {
		return buildTestSuite(ClassUsageTests.class);
	}
	
	/**
	 * @see org.eclipse.pde.api.tools.builder.tests.usage.UsageTest#getTestSourcePath()
	 */
	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("class");
	}
	
	/**
	 * Tests that classes the extend a restricted class are properly flagged 
	 * using a full build
	 */
	public void testClassUsageTests1F() {
		x1(false);
	}
	
	/**
	 * Tests the classes the extend a restricted class are properly flagged
	 * using an incremental build
	 */
	public void testClassUsageTests1I() {
		x1(true);
	}
	
	private void x1(boolean inc) {
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_EXTEND),
				getProblemId(IApiProblem.ILLEGAL_EXTEND),
				getProblemId(IApiProblem.ILLEGAL_EXTEND),
				getProblemId(IApiProblem.ILLEGAL_EXTEND)
		});
		String typename = "testC1";
		setExpectedMessageArgs(new String[][] {
				{CLASS_NAME, typename},
				{CLASS_NAME, INNER_NAME1},
				{CLASS_NAME, INNER_NAME2},
				{CLASS_NAME, OUTER_NAME}
		});
		deployTest(typename, inc);
	}
	
	/**
	 * Tests that classes the instantiate a restricted class are properly flagged 
	 * using a full build
	 */
	public void testClassUsageTests2F() {
		x2(false);
	}
	
	/**
	 * Tests the classes the instantiate a restricted class are properly flagged
	 * using an incremental build
	 */
	public void testClassUsageTests2I() {
		x2(true);
	}
	
	private void x2(boolean inc) {
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_INSTANTIATE),
				getProblemId(IApiProblem.ILLEGAL_INSTANTIATE),
				getProblemId(IApiProblem.ILLEGAL_INSTANTIATE),
				getProblemId(IApiProblem.ILLEGAL_INSTANTIATE)
		});
		String typename = "testC2";
		setExpectedMessageArgs(new String[][] {
				{CLASS_NAME, typename},
				{CLASS_NAME, INNER_NAME1},
				{CLASS_NAME, INNER_NAME2},
				{CLASS_NAME, OUTER_NAME}
		});
		deployTest(typename, inc);
	}
}
