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
package org.eclipse.pde.api.tools.builder.tests.leak;

import junit.framework.Test;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * Tests that an API constructor that leaks an internal type is properly
 * detected.
 * 
 * @since 1.0
 */
public class ConstructorParameterLeak extends LeakTest {

	private int pid = -1;
	
	/**
	 * Constructor
	 * @param name
	 */
	public ConstructorParameterLeak(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getDefaultProblemId()
	 */
	protected int getDefaultProblemId() {
		if(pid == -1) {
			pid = ApiProblemFactory.createProblemId(
					IApiProblem.CATEGORY_USAGE, 
					IElementDescriptor.T_METHOD, 
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
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.leak.LeakTest#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("method");
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
		String typename = "testCPL1";
		setExpectedMessageArgs(new String[][] {{TESTING_INTERNAL_CLASS_NAME, typename}, {TESTING_INTERNAL_CLASS_NAME, typename},
				{TESTING_INTERNAL_CLASS_NAME, typename}, {TESTING_INTERNAL_CLASS_NAME, typename}});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_CLASS_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+"."+typename}, 
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
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
		String typename = "testCPL2";
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_CLASS_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				null, 
				false, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
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
		String typename = "testCPL3";
		setExpectedMessageArgs(new String[][] {{TESTING_INTERNAL_INTERFACE_NAME, typename}, {TESTING_INTERNAL_INTERFACE_NAME, typename},
				{TESTING_INTERNAL_INTERFACE_NAME, typename}, {TESTING_INTERNAL_INTERFACE_NAME, typename}});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+"."+typename}, 
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
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
		String typename = "testCPL4";
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_CLASS_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				null, 
				false, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
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
		String typename = "testCPL5";
		setExpectedMessageArgs(new String[][] {{TESTING_INTERNAL_INTERFACE_NAME, typename}, {TESTING_INTERNAL_INTERFACE_NAME, typename},
				{TESTING_INTERNAL_INTERFACE_NAME, typename}, {TESTING_INTERNAL_INTERFACE_NAME, typename}, {TESTING_INTERNAL_CLASS_NAME, typename}, {TESTING_INTERNAL_CLASS_NAME, typename},
				{TESTING_INTERNAL_CLASS_NAME, typename}, {TESTING_INTERNAL_CLASS_NAME, typename}});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_CLASS_NAME, TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+"."+typename}, 
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
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
		String typename = "testCPL6";
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_CLASS_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				null, 
				false, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
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
		String typename = "testCPL7";
		String innertype = "inner";
		setExpectedMessageArgs(new String[][] {{TESTING_INTERNAL_INTERFACE_NAME, innertype}, {TESTING_INTERNAL_INTERFACE_NAME, innertype},
				{TESTING_INTERNAL_INTERFACE_NAME, innertype}, {TESTING_INTERNAL_INTERFACE_NAME, innertype}, 
				{TESTING_INTERNAL_CLASS_NAME, innertype}, {TESTING_INTERNAL_CLASS_NAME, innertype}, 
				{TESTING_INTERNAL_CLASS_NAME, innertype}, {TESTING_INTERNAL_CLASS_NAME, innertype}});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_INTERFACE_NAME, TESTING_INTERNAL_CLASS_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+"."+typename}, 
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
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
		String typename = "testCPL8";
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_CLASS_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				null, 
				false, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
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
		String typename = "testCPL9";
		String innertype1 = "inner2";
		String innertype2 = "inner3";
		setExpectedMessageArgs(new String[][] {{TESTING_INTERNAL_INTERFACE_NAME, innertype1}, {TESTING_INTERNAL_INTERFACE_NAME, innertype1},
				{TESTING_INTERNAL_INTERFACE_NAME, innertype2}, {TESTING_INTERNAL_INTERFACE_NAME, innertype2}, 
				{TESTING_INTERNAL_CLASS_NAME, innertype1}, {TESTING_INTERNAL_CLASS_NAME, innertype1}, 
				{TESTING_INTERNAL_CLASS_NAME, innertype2}, {TESTING_INTERNAL_CLASS_NAME, innertype2}});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_INTERFACE_NAME, TESTING_INTERNAL_CLASS_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+"."+typename}, 
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
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
		String typename = "testCPL10";
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_CLASS_NAME, TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				null, 
				false, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
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
		String typename = "testCPL11";
		String innertype1 = "inner2";
		String innertype2 = "inner3";
		setExpectedMessageArgs(new String[][] {
				{TESTING_INTERNAL_INTERFACE_NAME, innertype1}, 
				{TESTING_INTERNAL_CLASS_NAME, innertype1},
				{TESTING_INTERNAL_CLASS_NAME, innertype1},
				{TESTING_INTERNAL_INTERFACE_NAME, innertype2}, 
				{TESTING_INTERNAL_CLASS_NAME, innertype2}, 
				{TESTING_INTERNAL_CLASS_NAME, innertype2}});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_INTERFACE_NAME, TESTING_INTERNAL_CLASS_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+"."+typename}, 
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
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
		String typename = "testCPL12";
		String innertype1 = "inner2";
		String innertype2 = "inner3";
		setExpectedMessageArgs(new String[][] {
				{TESTING_INTERNAL_INTERFACE_NAME, innertype1}, 
				{TESTING_INTERNAL_CLASS_NAME, innertype1},
				{TESTING_INTERNAL_INTERFACE_NAME, innertype2}, 
				{TESTING_INTERNAL_CLASS_NAME, innertype2}});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_INTERFACE_NAME, TESTING_INTERNAL_CLASS_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+"."+typename}, 
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
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
		String typename = "testCPL13";
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_CLASS_NAME, TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				null, 
				false, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
}
