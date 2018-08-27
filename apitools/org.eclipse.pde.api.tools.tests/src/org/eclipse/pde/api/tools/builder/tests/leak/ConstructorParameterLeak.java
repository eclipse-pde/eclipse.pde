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
package org.eclipse.pde.api.tools.builder.tests.leak;

import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

import junit.framework.Test;

/**
 * Tests that an API constructor that leaks an internal type is properly
 * detected.
 *
 * @since 1.0
 */
public class ConstructorParameterLeak extends LeakTest {

	private int pid = -1;

	public ConstructorParameterLeak(String name) {
		super(name);
	}

	@Override
	protected int getDefaultProblemId() {
		if(pid == -1) {
			pid = ApiProblemFactory.createProblemId(
					IApiProblem.CATEGORY_USAGE,
					IElementDescriptor.METHOD,
					IApiProblem.API_LEAK,
					IApiProblem.LEAK_CONSTRUCTOR_PARAMETER);
		}
		return pid;
	}

	/**
	 * The suite for the tests
	 */
	public static Test suite() {
		return buildTestSuite(ConstructorParameterLeak.class);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("method"); //$NON-NLS-1$
	}

	/**
	 * Tests that constructors leaking internal types via one or more of their parameters is properly detected
	 * using a full build
	 */
	public void testConstructorParameterLeak1F() {
		x1(false);
	}

	/**
	 * Tests that constructors leaking internal types via one or more of their parameters is properly detected
	 * using an incremental build
	 */
	public void testConstructorParameterLeak1I() {
		x1(true);
	}

	private void x1(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(4));
		String typename = "testCPL1"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] {{TESTING_INTERNAL_CLASS_NAME, typename}, {TESTING_INTERNAL_CLASS_NAME, typename},
				{TESTING_INTERNAL_CLASS_NAME, typename}, {TESTING_INTERNAL_CLASS_NAME, typename}});
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that private constructors leaking internal types via one or more of their parameters ignored
	 * using a full build
	 */
	public void testConstructorParameterLeak2F() {
		x2(false);
	}

	/**
	 * Tests that private constructors leaking internal types via one or more of their parameters ignored
	 * using an incremental build
	 */
	public void testConstructorParameterLeak2I() {
		x2(true);
	}

	private void x2(boolean inc) {
		expectingNoProblems();
		String typename = "testCPL2"; //$NON-NLS-1$
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that constructors leaking internal types via one or more of their parameters is properly detected
	 * using a full build
	 */
	public void testConstructorParameterLeak3F() {
		x3(false);
	}

	/**
	 * Tests that constructors leaking internal types via one or more of their parameters is properly detected
	 * using an incremental build
	 */
	public void testConstructorParameterLeak3I() {
		x3(true);
	}

	private void x3(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(4));
		String typename = "testCPL3"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] {{TESTING_INTERNAL_INTERFACE_NAME, typename}, {TESTING_INTERNAL_INTERFACE_NAME, typename},
				{TESTING_INTERNAL_INTERFACE_NAME, typename}, {TESTING_INTERNAL_INTERFACE_NAME, typename}});
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that private constructors leaking internal types via one or more of their parameters ignored
	 * using a full build
	 */
	public void testConstructorParameterLeak4F() {
		x4(false);
	}

	/**
	 * Tests that private constructors leaking internal types via one or more of their parameters ignored
	 * using an incremental build
	 */
	public void testConstructorParameterLeak4I() {
		x4(true);
	}

	private void x4(boolean inc) {
		expectingNoProblems();
		String typename = "testCPL4"; //$NON-NLS-1$
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that constructors leaking internal types via one or more of their parameters is properly detected
	 * using a full build
	 */
	public void testConstructorParameterLeak5F() {
		x5(false);
	}

	/**
	 * Tests that constructors leaking internal types via one or more of their parameters is properly detected
	 * using an incremental build
	 */
	public void testConstructorParameterLeak5I() {
		x5(true);
	}

	private void x5(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(8));
		String typename = "testCPL5"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] {{TESTING_INTERNAL_INTERFACE_NAME, typename}, {TESTING_INTERNAL_INTERFACE_NAME, typename},
				{TESTING_INTERNAL_INTERFACE_NAME, typename}, {TESTING_INTERNAL_INTERFACE_NAME, typename}, {TESTING_INTERNAL_CLASS_NAME, typename}, {TESTING_INTERNAL_CLASS_NAME, typename},
				{TESTING_INTERNAL_CLASS_NAME, typename}, {TESTING_INTERNAL_CLASS_NAME, typename}});
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that private constructors leaking internal types via one or more of their parameters ignored
	 * using a full build
	 */
	public void testConstructorParameterLeak6F() {
		x6(false);
	}

	/**
	 * Tests that private constructors leaking internal types via one or more of their parameters ignored
	 * using an incremental build
	 */
	public void testConstructorParameterLeak6I() {
		x6(true);
	}

	private void x6(boolean inc) {
		expectingNoProblems();
		String typename = "testCPL6"; //$NON-NLS-1$
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that constructors in public static inner types leaking internal types via one or more of their parameters is properly detected
	 * using a full build
	 */
	public void testConstructorParameterLeak7F() {
		x7(false);
	}

	/**
	 * Tests that constructors in public static inner types leaking internal types via one or more of their parameters is properly detected
	 * using an incremental build
	 */
	public void testConstructorParameterLeak7I() {
		x7(true);
	}

	private void x7(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(8));
		String typename = "testCPL7"; //$NON-NLS-1$
		String innertype = "inner"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] {{TESTING_INTERNAL_INTERFACE_NAME, innertype}, {TESTING_INTERNAL_INTERFACE_NAME, innertype},
				{TESTING_INTERNAL_INTERFACE_NAME, innertype}, {TESTING_INTERNAL_INTERFACE_NAME, innertype},
				{TESTING_INTERNAL_CLASS_NAME, innertype}, {TESTING_INTERNAL_CLASS_NAME, innertype},
				{TESTING_INTERNAL_CLASS_NAME, innertype}, {TESTING_INTERNAL_CLASS_NAME, innertype}});
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that private constructors in public static inner types leaking internal types via one or more of their parameters ignored
	 * using a full build
	 */
	public void testConstructorParameterLeak8F() {
		x8(false);
	}

	/**
	 * Tests that private constructors in public static inner types leaking internal types via one or more of their parameters ignored
	 * using an incremental build
	 */
	public void testConstructorParameterLeak8I() {
		x8(true);
	}

	private void x8(boolean inc) {
		expectingNoProblems();
		String typename = "testCPL8"; //$NON-NLS-1$
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that constructors in public static inner types leaking internal types via one or more of their parameters is properly detected
	 * using a full build
	 */
	public void testConstructorParameterLeak9F() {
		x9(false);
	}

	/**
	 * Tests that constructors in public static inner types leaking internal types via one or more of their parameters is properly detected
	 * using an incremental build
	 */
	public void testConstructorParameterLeak9I() {
		x9(true);
	}

	private void x9(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(8));
		String typename = "testCPL9"; //$NON-NLS-1$
		String innertype1 = "inner2"; //$NON-NLS-1$
		String innertype2 = "inner3"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] {{TESTING_INTERNAL_INTERFACE_NAME, innertype1}, {TESTING_INTERNAL_INTERFACE_NAME, innertype1},
				{TESTING_INTERNAL_INTERFACE_NAME, innertype2}, {TESTING_INTERNAL_INTERFACE_NAME, innertype2},
				{TESTING_INTERNAL_CLASS_NAME, innertype1}, {TESTING_INTERNAL_CLASS_NAME, innertype1},
				{TESTING_INTERNAL_CLASS_NAME, innertype2}, {TESTING_INTERNAL_CLASS_NAME, innertype2}});
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that private constructors in public static inner types leaking internal types via one or more of their parameters ignored
	 * using a full build
	 */
	public void testConstructorParameterLeak10F() {
		x10(false);
	}

	/**
	 * Tests that private constructors in public static inner types leaking internal types via one or more of their parameters ignored
	 * using an incremental build
	 */
	public void testConstructorParameterLeak10I() {
		x10(true);
	}

	private void x10(boolean inc) {
		expectingNoProblems();
		String typename = "testCPL10"; //$NON-NLS-1$
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that an @noreference tag on a constructor removes a leak problem
	 * from a certain inner constructor using a full build
	 */
	public void testConstructorParameterLeak11F() {
		x11(false);
	}

	/**
	 * Tests that an @noreference tag on a constructor removes a leak problem
	 * from a certain constructor using an incremental build
	 */
	public void testConstructorParameterLeak11I() {
		x11(true);
	}

	private void x11(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(6));
		String typename = "testCPL11"; //$NON-NLS-1$
		String innertype1 = "inner2"; //$NON-NLS-1$
		String innertype2 = "inner3"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] {
				{TESTING_INTERNAL_INTERFACE_NAME, innertype1},
				{TESTING_INTERNAL_CLASS_NAME, innertype1},
				{TESTING_INTERNAL_CLASS_NAME, innertype1},
				{TESTING_INTERNAL_INTERFACE_NAME, innertype2},
				{TESTING_INTERNAL_CLASS_NAME, innertype2},
				{TESTING_INTERNAL_CLASS_NAME, innertype2}});
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that an @noreference tag on a constructor removes a leak problem
	 * from a certain inner constructor using a full build
	 */
	public void testConstructorParameterLeak12F() {
		x12(false);
	}

	/**
	 * Tests that an @noreference tag on a constructor removes a leak problem
	 * from a certain constructor using an incremental build
	 */
	public void testConstructorParameterLeak12I() {
		x12(true);
	}

	private void x12(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(4));
		String typename = "testCPL12"; //$NON-NLS-1$
		String innertype1 = "inner2"; //$NON-NLS-1$
		String innertype2 = "inner3"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] {
				{TESTING_INTERNAL_INTERFACE_NAME, innertype1},
				{TESTING_INTERNAL_CLASS_NAME, innertype1},
				{TESTING_INTERNAL_INTERFACE_NAME, innertype2},
				{TESTING_INTERNAL_CLASS_NAME, innertype2}});
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that @noreference tags on constructors removes all leak problems
	 * using a full build
	 */
	public void testConstructorParameterLeak13F() {
		x13(false);
	}

	/**
	 * Tests that @noreference tags on constructors removes all leak problems
	 * using an incremental build
	 */
	public void testConstructorParameterLeak13I() {
		x13(true);
	}

	private void x13(boolean inc) {
		expectingNoProblems();
		String typename = "testCPL13"; //$NON-NLS-1$
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
	}


	/**
	 * Tests that a non public top level type parameter is a leak on a constructor
	 */
	public void testConstructorParameterLeak14F() {
		x14(false);
	}

	/**
	 * Tests that a non public top level type parameter is a leak on a constructor
	 */
	public void testConstructorParameterLeak14I() {
		x14(true);
	}

	private void x14(boolean inc) {
		setExpectedProblemIds(new int[] {getDefaultProblemId()});
		String typename = "testCPL14"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] {{"outercpl14", typename}}); //$NON-NLS-1$
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
	}
}
