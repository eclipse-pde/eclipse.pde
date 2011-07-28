/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * Test class usage for Java 7 code snippets
 * 
 * @since 1.0.100
 */
public class Java7MethodUsageTests extends MethodUsageTests {

	/**
	 * Constructor
	 * @param name
	 */
	public Java7MethodUsageTests(String name) {
		super(name);
	}

	/**
	 * @return the test class for this suite
	 */
	public static Test suite() {
		return buildTestSuite(Java7MethodUsageTests.class);
	}
	
	/**
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getTestCompliance()
	 */
	@Override
	protected String getTestCompliance() {
		return CompilerOptions.VERSION_1_7;
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().removeLastSegments(1).append("java7");
	}

	/**
	 * Tests illegal use of methods inside a string switch block
	 * (full)
	 */
	public void testStringSwitchF() {
		x1(false);
	}
	
	/**
	 * Tests illegal use of methods inside a string switch block
	 * (incremental)
	 */
	public void testStringSwitchI() {
		x1(true);
	}
	
	
	private void x1(boolean inc) {
		int[] pids = new int[] {
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD)
		};
		setExpectedProblemIds(pids);
		String typename = "testMStringSwitch";
		String[][] args = new String[][] {
				{METHOD_CLASS_NAME, typename, "m1()"},
				{METHOD_CLASS_NAME, typename, "m3()"},
				{METHOD_CLASS_NAME, typename, "m1()"},
				{METHOD_CLASS_NAME, typename, "m3()"},
				{METHOD_CLASS_NAME, typename, "m1()"},
				{METHOD_CLASS_NAME, typename, "m3()"},
		};
		setExpectedMessageArgs(args);
		setExpectedLineMappings(new LineMapping[] {
				new LineMapping(22, pids[0], args[0]),
				new LineMapping(23, pids[1], args[1]),
				new LineMapping(26, pids[2], args[2]),
				new LineMapping(27, pids[3], args[3]),
				new LineMapping(30, pids[4], args[4]),
				new LineMapping(31, pids[5], args[5])
		});
		deployUsageTest(typename, inc);
	}
	
	/**
	 * Tests illegal use of methods inside a multi catch block
	 * (full)
	 */
	public void testMultiCatchF() {
		x2(false);
	}
	
	/**
	 * Tests illegal use of methods inside a multi catch block
	 * (incremental)
	 */
	public void testMultiCatchI() {
		x2(true);
	}
	
	
	private void x2(boolean inc) {
		int[] pids = new int[] {
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD)
		};
		setExpectedProblemIds(pids);
		String typename = "testMMultiCatch";
		String[][] args = new String[][] {
				{"MultipleThrowableClass", typename, "m2()"}
		};
		setExpectedMessageArgs(args);
		setExpectedLineMappings(new LineMapping[] {
				new LineMapping(23, pids[0], args[0])
		});
		deployUsageTest(typename, inc);
	}
	
}
