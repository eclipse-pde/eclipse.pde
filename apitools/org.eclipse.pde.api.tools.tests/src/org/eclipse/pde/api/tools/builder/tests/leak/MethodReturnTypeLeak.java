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
 * Tests that an API method leaking an internal type as a return type 
 * is correctly flagged
 * 
 * @since 1.0
 */
public class MethodReturnTypeLeak extends LeakTest {

	private int pid = -1;
	
	/**
	 * Constructor
	 * @param name
	 */
	public MethodReturnTypeLeak(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getDefaultProblemId()
	 */
	protected int getDefaultProblemId() {
		if(pid == -1) {
			pid = ApiProblemFactory.createProblemId(
					IApiProblem.CATEGORY_USAGE, 
					IElementDescriptor.METHOD, 
					IApiProblem.API_LEAK, 
					IApiProblem.LEAK_RETURN_TYPE);
		}
		return pid;
	}

	/**
	 * Currently empty.
	 */
	public static Test suite() {
		return buildTestSuite(MethodReturnTypeLeak.class);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.leak.LeakTest#getTestSourcePath()
	 */
	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("method");
	}
	
	/**
	 * Tests that method with internal return types are properly detected
	 * using a full build
	 */
	public void testMethodReturnTypeLeak1F() {
		x1(false);
	}
	
	/**
	 * Tests that methods with internal return types are properly detected 
	 * using an incremental build
	 */
	public void testMethodReturnTypeLeak1I() {
		x1(true);
	}
	
	private void x1(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(4));
		String typename = "testMRL1";
		setExpectedMessageArgs(new String[][] {{TESTING_INTERNAL_CLASS_NAME, typename, "m1()"},
				{TESTING_INTERNAL_CLASS_NAME, typename, "m2()"},
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m3()"},
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m4()"}});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL, TESTING_PACKAGE_INTERNAL},
				new String[] {typename, TESTING_INTERNAL_CLASS_NAME, TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+"."+typename},
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD),
				true);
	}
	
	/**
	 * Tests that private methods with internal return types are properly ignored
	 * using a full build
	 */
	public void testMethodReturnTypeLeak2F() {
		x2(false);
	}
	
	/**
	 * Tests that private methods with internal return types are properly ignored 
	 * using an incremental build
	 */
	public void testMethodReturnTypeLeak2I() {
		x2(true);
	}
	
	private void x2(boolean inc) {
		expectingNoProblems();
		String typename = "testMRL2";
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_CLASS_NAME, TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				null, 
				false, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that methods in public static internal types with internal return types are properly detected
	 * using a full build
	 */
	public void testMethodReturnTypeLeak3F() {
		x3(false);
	}
	
	/**
	 * Tests that methods in public static internal types with internal return types are properly detected
	 * using an incremental build
	 */
	public void testMethodReturnTypeLeak3I() {
		x3(true);
	}
	
	private void x3(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(4));
		String typename = "testMRL3";
		String innertype = "inner";
		setExpectedMessageArgs(new String[][] {{TESTING_INTERNAL_CLASS_NAME, innertype, "m1()"},
				{TESTING_INTERNAL_CLASS_NAME, innertype, "m2()"},
				{TESTING_INTERNAL_INTERFACE_NAME, innertype, "m3()"},
				{TESTING_INTERNAL_INTERFACE_NAME, innertype, "m4()"}});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_CLASS_NAME, TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL},
				new String[] {TESTING_PACKAGE+"."+typename}, 
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that methods in public static internal types with internal return types are properly ignored
	 * using a full build
	 */
	public void testMethodReturnTypeLeak4F() {
		x4(false);
	}
	
	/**
	 * Tests that methods in public static internal types with internal return types are properly ignored
	 * using an incremental build
	 */
	public void testMethodReturnTypeLeak4I() {
		x4(true);
	}
	
	private void x4(boolean inc) {
		expectingNoProblems();
		String typename = "testMRL4";
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_CLASS_NAME, TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				null, 
				false, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that methods in public static internal types with internal return types are properly detected
	 * using a full build
	 */
	public void testMethodReturnTypeLeak5F() {
		x5(false);
	}
	
	/**
	 * Tests that methods in public static internal types with internal return types are properly detected
	 * using an incremental build
	 */
	public void testMethodReturnTypeLeak5I() {
		x5(true);
	}
	
	private void x5(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(4));
		String typename = "testMRL5";
		String innertype = "inner2";
		setExpectedMessageArgs(new String[][] {{TESTING_INTERNAL_CLASS_NAME, innertype, "m1()"},
				{TESTING_INTERNAL_CLASS_NAME, innertype, "m2()"},
				{TESTING_INTERNAL_INTERFACE_NAME, innertype, "m3()"},
				{TESTING_INTERNAL_INTERFACE_NAME, innertype, "m4()"}});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_CLASS_NAME, TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+"."+typename},
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that methods in public static internal types with internal return types are properly ignored
	 * using a full build
	 */
	public void testMethodReturnTypeLeak6F() {
		x6(false);
	}
	
	/**
	 * Tests that methods in public static internal types with internal return types are properly ignored
	 * using an incremental build
	 */
	public void testMethodReturnTypeLeak6I() {
		x6(true);
	}
	
	public void x6(boolean inc) {
		expectingNoProblems();
		String typename = "testMRL6";
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_CLASS_NAME, TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				null, 
				false, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that methods in public static internal types with internal return types are properly detected
	 * using a full build
	 */
	public void testMethodReturnTypeLeak7F() {
		x7(false);
	}
	
	/**
	 * Tests that methods in public static internal types with internal return types are properly detected
	 * using an incremental build
	 */
	public void testMethodReturnTypeLeak7I() {
		x7(true);
	}
	
	private void x7(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(4));
		String typename = "testMRL7";
		String innertype = "inner2";
		setExpectedMessageArgs(new String[][] {
				{TESTING_INTERNAL_CLASS_NAME, innertype, "m1()"},
				{TESTING_INTERNAL_CLASS_NAME, innertype, "m2()"},
				{TESTING_INTERNAL_INTERFACE_NAME, innertype, "m3()"},
				{TESTING_INTERNAL_INTERFACE_NAME, innertype, "m4()"}});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_CLASS_NAME, TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+"."+typename},
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that methods in an abstract type with internal return types are properly detected
	 * using a full build
	 */
	public void testMethodReturnTypeLeak8F() {
		x8(false);
	}
	
	/**
	 * Tests that methods in an abstract type with internal return types are properly detected
	 * using an incremental build
	 */
	public void testMethodReturnTypeLeak8I() {
		x8(true);
	}
	
	private void x8(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(4));
		String typename = "testMRL8";
		setExpectedMessageArgs(new String[][] {
				{TESTING_INTERNAL_CLASS_NAME, typename, "m1()"},
				{TESTING_INTERNAL_CLASS_NAME, typename, "m2()"},
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m3()"},
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m4()"}});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_CLASS_NAME, TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+"."+typename},
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that methods in a final type with internal return types are properly detected
	 * using a full build
	 */
	public void testMethodReturnTypeLeak9F() {
		x9(false);
	}
	
	/**
	 * Tests that methods in a final type with internal return types are properly detected
	 * using an incremental build
	 */
	public void testMethodReturnTypeLeak9I() {
		x9(true);
	}
	
	private void x9(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(4));
		String typename = "testMRL9";
		setExpectedMessageArgs(new String[][] {
				{TESTING_INTERNAL_CLASS_NAME, typename, "m1()"},
				{TESTING_INTERNAL_CLASS_NAME, typename, "m2()"},
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m3()"},
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m4()"}});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_CLASS_NAME, TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+"."+typename},
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that private methods in a final type with internal return types are properly ignored
	 * using a full build
	 */
	public void testMethodReturnTypeLeak10F() {
		x10(false);
	}
	
	/**
	 * Tests that private methods in a final type with internal return types are properly ignored
	 * using an incremental build
	 */
	public void testMethodReturnTypeLeak10I() {
		x10(true);
	}
	
	private void x10(boolean inc) {
		expectingNoProblems();
		String typename = "testMRL10";
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_CLASS_NAME, TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL},
				null,
				false, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that methods leaking return types are properly reported even with an @nooverride tag present
	 * using a full build
	 */
	public void testMethodReturnTypeLeak11F() {
		x11(false);
	}
	
	/**
	 * Tests that methods leaking return types are properly reported even with an @nooverride tag present
	 * using an incremental build
	 */
	public void testMethodReturnTypeLeak11I() {
		x11(true);
	}
	
	private void x11(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(8));
		String typename = "testMRL11";
		setExpectedMessageArgs(new String[][] {
				{TESTING_INTERNAL_CLASS_NAME, typename, "m1()"},
				{TESTING_INTERNAL_CLASS_NAME, typename, "m2()"},
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m3()"},
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m4()"},
				{TESTING_INTERNAL_CLASS_NAME, "inner", "m1()"},
				{TESTING_INTERNAL_CLASS_NAME, "inner", "m2()"},
				{TESTING_INTERNAL_INTERFACE_NAME, "inner", "m3()"},
				{TESTING_INTERNAL_INTERFACE_NAME, "inner", "m4()"}});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_CLASS_NAME, TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+"."+typename},
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	/**
	 * Tests that methods leaking return types are properly reported even with an @noreference tag present
	 * using a full build
	 */
	public void testMethodReturnLeak12F() {
		x12(false);
	}
	
	/**
	 * Tests that methods leaking return types are properly reported even with an @noreference tag present
	 * using an incremental build
	 */
	public void testMethodReturnType12I() {
		x12(true);
	}
	
	private void x12(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(8));
		String typename = "testMRL12";
		setExpectedMessageArgs(new String[][] {
				{TESTING_INTERNAL_CLASS_NAME, typename, "m1()"},
				{TESTING_INTERNAL_CLASS_NAME, typename, "m2()"},
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m3()"},
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m4()"},
				{TESTING_INTERNAL_CLASS_NAME, "inner", "m1()"},
				{TESTING_INTERNAL_CLASS_NAME, "inner", "m2()"},
				{TESTING_INTERNAL_INTERFACE_NAME, "inner", "m3()"},
				{TESTING_INTERNAL_INTERFACE_NAME, "inner", "m4()"}});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_CLASS_NAME, TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+"."+typename},
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that a variety of methods leaking return types are ignored when @noreference AND @nooverride tags are present
	 * using a full build
	 */
	public void testMethodReturnTypeLeak13F() {
		x13(false);
	}
	
	/**
	 * Tests that a variety of methods leaking return types are ignored when @noreference AND @nooverride tags are present
	 * using an incremental build
	 */
	public void testMethodReturnTypeLeak13I() {
		x13(true);
	}
	
	private void x13(boolean inc) {
		expectingNoProblems();
		String typename = "testMRL13";
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_CLASS_NAME, TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL},
				null,
				false, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that a method in a final class leaking internal parameters is ignored when a @noreference tag is present 
	 * using a full build
	 */
	public void testMethodParameterLeak14F() {
		x14(false);
	}
	
	/**
	 * Tests that a method in a final class leaking internal parameters is ignored when a @noreference tag is present 
	 * using an incremental build
	 */
	public void testMethodParameterLeak14I() {
		x14(true);
	}
	
	private void x14(boolean inc) {
		expectingNoProblems();
		String typename = "testMRL14";
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				null, 
				false, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that a method in an extend restricted class leaking internal parameters is ignored when a @noreference tag is present 
	 * using a full build
	 */
	public void testMethodParameterLeak15F() {
		x15(false);
	}
	
	/**
	 * Tests that a method in an extend restricted class leaking internal parameters is ignored when a @noreference tag is present 
	 * using an incremental build
	 */
	public void testMethodParameterLeak15I() {
		x15(true);
	}
	
	private void x15(boolean inc) {
		expectingNoProblems();
		String typename = "testMRL15";
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				null, 
				false, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that a static method leaking internal parameters is ignored when a @noreference tag is present 
	 * using a full build
	 */
	public void testMethodParameterLeak16F() {
		x16(false);
	}
	
	/**
	 * Tests that a static method leaking internal parameters is ignored when a @noreference tag is present 
	 * using an incremental build
	 */
	public void testMethodParameterLeak16I() {
		x16(true);
	}
	
	private void x16(boolean inc) {
		expectingNoProblems();
		String typename = "testMRL16";
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				null, 
				false, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
 }
