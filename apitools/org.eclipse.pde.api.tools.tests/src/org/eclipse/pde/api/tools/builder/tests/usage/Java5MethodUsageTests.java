/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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

import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * Tests Java 5 method accesses
 * 
 * @since 1.0.1
 */
public class Java5MethodUsageTests extends MethodUsageTests {

	protected static final String GENERIC_METHOD_CLASS_NAME = "GenericMethodUsageClass<T>";
	protected static final String GENERIC_METHOD_CLASS_NAME2 = "GenericMethodUsageClass2";
	protected static final String METHOD_ENUM_NAME = "MethodUsageEnum";
	
	/**
	 * Constructor
	 * @param name
	 */
	public Java5MethodUsageTests(String name) {
		super(name);
	}

	/**
	 * @return the test class for this suite
	 */
	public static Test suite() {
		return buildTestSuite(Java5MethodUsageTests.class);
	}
	
	/**
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getTestCompliance()
	 */
	@Override
	protected String getTestCompliance() {
		return CompilerOptions.VERSION_1_5;
	}

	public void testMethodUsageTests1F() {
		x1(false);
	}
		
	public void testMethodUsageTests1I() {
		x1(true);
	}
	
	/**
	 * Tests that accessing restricted enum methods are properly reported
	 */
	private void x1(boolean inc) {
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
		});
		String typename = "testM5";
		setExpectedMessageArgs(new String[][] {
				{METHOD_ENUM_NAME, INNER_NAME1, "m1()"},
				{METHOD_ENUM_NAME, INNER_NAME1, "m3()"},
				{METHOD_ENUM_NAME, INNER_NAME1, "m4()"},
				{METHOD_ENUM_NAME, INNER_NAME2, "m1()"},
				{METHOD_ENUM_NAME, INNER_NAME2, "m3()"},
				{METHOD_ENUM_NAME, INNER_NAME2, "m4()"},
				{METHOD_ENUM_NAME, OUTER_NAME, "m1()"},
				{METHOD_ENUM_NAME, OUTER_NAME, "m3()"},
				{METHOD_ENUM_NAME, OUTER_NAME, "m4()"}
		});
		deployTest(typename, inc);
	}

	public void testMethodUsageTests2F() {
		x2(false);
	}
	
	public void testMethodUsageTests2I() {
		x2(true);
	}

	/**
	 * Tests that accessing restricted generic methods are properly reported
	 */
	private void x2(boolean inc) {
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
		});
		String typename = "testM6";
		setExpectedMessageArgs(new String[][] {
				{GENERIC_METHOD_CLASS_NAME, INNER_NAME1, "m1()"},
				{GENERIC_METHOD_CLASS_NAME, INNER_NAME1, "m2(T)"},
				{GENERIC_METHOD_CLASS_NAME, INNER_NAME2, "m1()"},
				{GENERIC_METHOD_CLASS_NAME, INNER_NAME2, "m2(T)"},
				{GENERIC_METHOD_CLASS_NAME, OUTER_NAME, "m1()"},
				{GENERIC_METHOD_CLASS_NAME, OUTER_NAME, "m2(T)"},
		});
		deployTest(typename, inc);
	}

	public void testMethodUsageTests3F() {
		x3(false);
	}

	public void testMethodUsageTests3I() {
		x3(true);
	}
	
	/**
	 * Tests that accessing restricted methods that has a generic type as a parameter
	 * are properly reported
	 */
	private void x3(boolean inc) {
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
		});
		String typename = "testM7";
		setExpectedMessageArgs(new String[][] {
				{GENERIC_METHOD_CLASS_NAME2, INNER_NAME1, "m1(GenericClassUsageClass<?>)"},
				{GENERIC_METHOD_CLASS_NAME2, INNER_NAME2, "m1(GenericClassUsageClass<?>)"},
				{GENERIC_METHOD_CLASS_NAME2, OUTER_NAME, "m1(GenericClassUsageClass<?>)"},
		});
		deployTest(typename, inc);
	}
}
