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
 * Tests that an API method leaking an internal type via a parameter
 * is correctly detected
 * 
 * @since 1.0
 */
public class MethodParameterLeak extends LeakTest {

	private int pid = -1;
	
	/**
	 * Constructor
	 * @param name
	 */
	public MethodParameterLeak(String name) {
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
					IApiProblem.LEAK_METHOD_PARAMETER);
		}
		return pid;
	}

	/**
	 * Builds the test suite for this class
	 */
	public static Test suite() {
		return buildTestSuite(MethodParameterLeak.class);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.leak.LeakTest#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("method");
	}
	
	/**
	 * Tests that a variety of methods leaking internal parameters are detected properly 
	 * using a full build
	 */
	public void testMethodParameterLeak1F() {
		x1(false);
	}
	
	/**
	 * Tests that a variety of methods leaking internal parameters are detected properly
	 * using an incremental build
	 */
	public void testMethodParameterLeak1I() {
		x1(true);
	}
	
	private void x1(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(6));
		String typename = "testMPL1";
		setExpectedMessageArgs(new String[][] {{TESTING_INTERNAL_CLASS_NAME, typename, "m1(internal)"},
				{TESTING_INTERNAL_CLASS_NAME, typename, "m2(internal)"}, {TESTING_INTERNAL_CLASS_NAME, typename, "m3(internal)"}, 
				{TESTING_INTERNAL_CLASS_NAME, typename, "m4(internal)"}, {TESTING_INTERNAL_CLASS_NAME, typename, "m5(internal)"}, 
				{TESTING_INTERNAL_CLASS_NAME, typename, "m6(internal)"}});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_CLASS_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+"."+typename}, 
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that a variety of private methods leaking internal parameters are ignored properly 
	 * using a full build
	 */
	public void testMethodParameterLeak2F() {
		x2(false);
	}
	
	/**
	 * Tests that a variety of private methods leaking internal parameters are ignored properly
	 * using an incremental build
	 */
	public void testMethodParameterLeak2I() {
		x2(true);
	}
	
	private void x2(boolean inc) {
		expectingNoProblems();
		String typename = "testMPL2";
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_CLASS_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				null, 
				false, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that a variety of methods leaking internal parameters are detected properly 
	 * using a full build
	 */
	public void testMethodParameterLeak3F() {
		x3(false);
	}
	
	/**
	 * Tests that a variety of methods leaking internal parameters are detected properly
	 * using an incremental build
	 */
	public void testMethodParameterLeak3I() {
		x3(true);
	}
	
	private void x3(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(6));
		String typename = "testMPL3";
		setExpectedMessageArgs(new String[][] {{TESTING_INTERNAL_INTERFACE_NAME, typename, "m1(Iinternal)"},
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m2(Iinternal)"}, {TESTING_INTERNAL_INTERFACE_NAME, typename, "m3(Iinternal)"}, 
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m4(Iinternal)"}, {TESTING_INTERNAL_INTERFACE_NAME, typename, "m5(Iinternal)"}, 
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m6(Iinternal)"}});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+"."+typename}, 
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	/**
	 * Tests that a variety of methods leaking internal parameters are detected properly 
	 * using a full build
	 */
	public void testMethodParameterLeak4F() {
		x4(false);
	}
	
	/**
	 * Tests that a variety of methods leaking internal parameters are detected properly
	 * using an incremental build
	 */
	public void testMethodParameterLeak4I() {
		x4(true);
	}
	
	private void x4(boolean inc) {
		expectingNoProblems();
		String typename = "testMPL4";
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				null, 
				false, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that a variety of methods leaking internal parameters are detected properly 
	 * using a full build
	 */
	public void testMethodParameterLeak5F() {
		x5(false);
	}
	
	/**
	 * Tests that a variety of methods leaking internal parameters are detected properly
	 * using an incremental build
	 */
	public void testMethodParameterLeak5I() {
		x5(true);
	}
	
	private void x5(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(12));
		String typename = "testMPL5";
		setExpectedMessageArgs(new String[][] {{TESTING_INTERNAL_INTERFACE_NAME, typename, "m1(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, typename, "m1(Iinternal, Object, double, internal)"},
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m2(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, typename, "m2(Iinternal, Object, double, internal)"}, 
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m3(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, typename, "m3(Iinternal, Object, double, internal)"}, 
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m4(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, typename, "m4(Iinternal, Object, double, internal)"},
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m5(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, typename, "m5(Iinternal, Object, double, internal)"},
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m6(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, typename, "m6(Iinternal, Object, double, internal)"}});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_INTERFACE_NAME, TESTING_INTERNAL_CLASS_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+"."+typename}, 
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that a variety of methods leaking internal parameters are detected properly 
	 * using a full build
	 */
	public void testMethodParameterLeak6F() {
		x6(false);
	}
	
	/**
	 * Tests that a variety of methods leaking internal parameters are detected properly
	 * using an incremental build
	 */
	public void testMethodParameterLeak6I() {
		x6(true);
	}
	
	private void x6(boolean inc) {
		expectingNoProblems();
		String typename = "testMPL6";
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				null, 
				false, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that a variety of methods leaking internal parameters are detected properly 
	 * using a full build
	 */
	public void testMethodParameterLeak7F() {
		x7(false);
	}
	
	/**
	 * Tests that a variety of methods leaking internal parameters are detected properly
	 * using an incremental build
	 */
	public void testMethodParameterLeak7I() {
		x7(true);
	}
	
	private void x7(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(12));
		String typename = "testMPL7";
		setExpectedMessageArgs(new String[][] {{TESTING_INTERNAL_INTERFACE_NAME, "inner", "m1(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner", "m1(Iinternal, Object, double, internal)"},
				{TESTING_INTERNAL_INTERFACE_NAME, "inner", "m2(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner", "m2(Iinternal, Object, double, internal)"}, 
				{TESTING_INTERNAL_INTERFACE_NAME, "inner", "m3(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner", "m3(Iinternal, Object, double, internal)"}, 
				{TESTING_INTERNAL_INTERFACE_NAME, "inner", "m4(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner", "m4(Iinternal, Object, double, internal)"},
				{TESTING_INTERNAL_INTERFACE_NAME, "inner", "m5(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner", "m5(Iinternal, Object, double, internal)"},
				{TESTING_INTERNAL_INTERFACE_NAME, "inner", "m6(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner", "m6(Iinternal, Object, double, internal)"}});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_INTERFACE_NAME, TESTING_INTERNAL_CLASS_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+"."+typename}, 
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that a variety of methods leaking internal parameters are detected properly 
	 * using a full build
	 */
	public void testMethodParameterLeak8F() {
		x8(false);
	}
	
	/**
	 * Tests that a variety of methods leaking internal parameters are detected properly
	 * using an incremental build
	 */
	public void testMethodParameterLeak8I() {
		x8(true);
	}
	
	private void x8(boolean inc) {
		expectingNoProblems();
		String typename = "testMPL8";
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				null, 
				false, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that a variety of methods leaking internal parameters are detected properly 
	 * using a full build
	 */
	public void testMethodParameterLeak9F() {
		x9(false);
	}
	
	/**
	 * Tests that a variety of methods leaking internal parameters are detected properly
	 * using an incremental build
	 */
	public void testMethodParameterLeak9I() {
		x9(true);
	}
	
	private void x9(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(12));
		String typename = "testMPL9";
		setExpectedMessageArgs(new String[][] {{TESTING_INTERNAL_INTERFACE_NAME, "inner2", "m1(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner2", "m1(Iinternal, Object, double, internal)"},
				{TESTING_INTERNAL_INTERFACE_NAME, "inner2", "m2(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner2", "m2(Iinternal, Object, double, internal)"}, 
				{TESTING_INTERNAL_INTERFACE_NAME, "inner2", "m3(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner2", "m3(Iinternal, Object, double, internal)"}, 
				{TESTING_INTERNAL_INTERFACE_NAME, "inner3", "m4(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner3", "m4(Iinternal, Object, double, internal)"},
				{TESTING_INTERNAL_INTERFACE_NAME, "inner3", "m5(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner3", "m5(Iinternal, Object, double, internal)"},
				{TESTING_INTERNAL_INTERFACE_NAME, "inner3", "m6(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner3", "m6(Iinternal, Object, double, internal)"}});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_INTERFACE_NAME, TESTING_INTERNAL_CLASS_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+"."+typename}, 
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that a variety of methods leaking internal parameters are detected properly 
	 * using a full build
	 */
	public void testMethodParameterLeak10F() {
		x10(false);
	}
	
	/**
	 * Tests that a variety of methods leaking internal parameters are detected properly
	 * using an incremental build
	 */
	public void testMethodParameterLeak10I() {
		x10(true);
	}
	
	private void x10(boolean inc) {
		expectingNoProblems();
		String typename = "testMPL10";
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				null, 
				false, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that problems for leaking parameters are still properly reported with an @nooverride tag on methods
	 * using a full build
	 */
	public void testMethodParameterLeak11F() {
		x11(false);
	}
	
	/**
	 * Tests that problems for leaking parameters are still properly reported with an @nooverride tag on methods
	 * using an incremental build
	 */
	public void testMethodParameterLeak11I() {
		x11(true);
	}
	
	private void x11(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(12));
		String typename = "testMPL11";
		setExpectedMessageArgs(new String[][] {
				{TESTING_INTERNAL_INTERFACE_NAME, "inner", "m1(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner", "m1(Iinternal, Object, double, internal)"},
				{TESTING_INTERNAL_INTERFACE_NAME, "inner", "m2(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner", "m2(Iinternal, Object, double, internal)"}, 
				{TESTING_INTERNAL_INTERFACE_NAME, "inner", "m3(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner", "m3(Iinternal, Object, double, internal)"}, 
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m1(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, typename, "m1(Iinternal, Object, double, internal)"},
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m2(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, typename, "m2(Iinternal, Object, double, internal)"},
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m3(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, typename, "m3(Iinternal, Object, double, internal)"}});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_INTERFACE_NAME, TESTING_INTERNAL_CLASS_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+"."+typename}, 
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that problems for leaking parameters are still properly reported with an @noreference tag on methods
	 * using a full build
	 */
	public void testMethodParameterLeak12F() {
		x12(false);
	}
	
	/**
	 * Tests that problems for leaking parameters are still properly reported with an @noreference tag on methods
	 * using an incremental build
	 */
	public void testMethodParameterLeak12I() {
		x12(true);
	}
	
	private void x12(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(12));
		String typename = "testMPL12";
		setExpectedMessageArgs(new String[][] {
				{TESTING_INTERNAL_INTERFACE_NAME, "inner", "m1(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner", "m1(Iinternal, Object, double, internal)"},
				{TESTING_INTERNAL_INTERFACE_NAME, "inner", "m2(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner", "m2(Iinternal, Object, double, internal)"}, 
				{TESTING_INTERNAL_INTERFACE_NAME, "inner", "m3(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner", "m3(Iinternal, Object, double, internal)"}, 
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m1(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, typename, "m1(Iinternal, Object, double, internal)"},
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m2(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, typename, "m2(Iinternal, Object, double, internal)"},
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m3(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, typename, "m3(Iinternal, Object, double, internal)"}});
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_INTERFACE_NAME, TESTING_INTERNAL_CLASS_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				new String[] {TESTING_PACKAGE+"."+typename}, 
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that a variety of methods leaking internal parameters are ignored when @noreference AND @nooverride tags are present 
	 * using a full build
	 */
	public void testMethodParameterLeak13F() {
		x13(false);
	}
	
	/**
	 * Tests that a variety of methods leaking internal parameters are ignored when @noreference AND @nooverride tags are present
	 * using an incremental build
	 */
	public void testMethodParameterLeak13I() {
		x13(true);
	}
	
	private void x13(boolean inc) {
		expectingNoProblems();
		String typename = "testMPL13";
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_INTERFACE_NAME}, 
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
		String typename = "testMPL14";
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
		String typename = "testMPL15";
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
		String typename = "testMPL16";
		deployLeakTest(new String[] {TESTING_PACKAGE, TESTING_PACKAGE_INTERNAL}, 
				new String[] {typename, TESTING_INTERNAL_INTERFACE_NAME}, 
				new String[] {TESTING_PACKAGE_INTERNAL}, 
				null, 
				false, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
}
