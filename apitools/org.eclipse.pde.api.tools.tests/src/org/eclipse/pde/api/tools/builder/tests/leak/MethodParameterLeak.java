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
 * Tests that an API method leaking an internal type via a parameter
 * is correctly detected
 *
 * @since 1.0
 */
public class MethodParameterLeak extends LeakTest {

	private int pid = -1;

	public MethodParameterLeak(String name) {
		super(name);
	}

	@Override
	protected int getDefaultProblemId() {
		if(pid == -1) {
			pid = ApiProblemFactory.createProblemId(
					IApiProblem.CATEGORY_USAGE,
					IElementDescriptor.METHOD,
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

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("method"); //$NON-NLS-1$
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
		String typename = "testMPL1"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] {{TESTING_INTERNAL_CLASS_NAME, typename, "m1(internal)"}, //$NON-NLS-1$
				{TESTING_INTERNAL_CLASS_NAME, typename, "m2(internal)"}, {TESTING_INTERNAL_CLASS_NAME, typename, "m3(internal)"},  //$NON-NLS-1$ //$NON-NLS-2$
				{TESTING_INTERNAL_CLASS_NAME, typename, "m4(internal)"}, {TESTING_INTERNAL_CLASS_NAME, typename, "m5(internal)"},  //$NON-NLS-1$ //$NON-NLS-2$
				{TESTING_INTERNAL_CLASS_NAME, typename, "m6(internal)"}}); //$NON-NLS-1$
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
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
		String typename = "testMPL2"; //$NON-NLS-1$
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
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
		String typename = "testMPL3"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] {{TESTING_INTERNAL_INTERFACE_NAME, typename, "m1(Iinternal)"}, //$NON-NLS-1$
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m2(Iinternal)"}, {TESTING_INTERNAL_INTERFACE_NAME, typename, "m3(Iinternal)"},  //$NON-NLS-1$ //$NON-NLS-2$
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m4(Iinternal)"}, {TESTING_INTERNAL_INTERFACE_NAME, typename, "m5(Iinternal)"},  //$NON-NLS-1$ //$NON-NLS-2$
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m6(Iinternal)"}}); //$NON-NLS-1$
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
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
		String typename = "testMPL4"; //$NON-NLS-1$
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
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
		String typename = "testMPL5"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] {{TESTING_INTERNAL_INTERFACE_NAME, typename, "m1(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, typename, "m1(Iinternal, Object, double, internal)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m2(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, typename, "m2(Iinternal, Object, double, internal)"},  //$NON-NLS-1$ //$NON-NLS-2$
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m3(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, typename, "m3(Iinternal, Object, double, internal)"},  //$NON-NLS-1$ //$NON-NLS-2$
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m4(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, typename, "m4(Iinternal, Object, double, internal)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m5(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, typename, "m5(Iinternal, Object, double, internal)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m6(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, typename, "m6(Iinternal, Object, double, internal)"}}); //$NON-NLS-1$ //$NON-NLS-2$
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
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
		String typename = "testMPL6"; //$NON-NLS-1$
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
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
		String typename = "testMPL7"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] {{TESTING_INTERNAL_INTERFACE_NAME, "inner", "m1(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner", "m1(Iinternal, Object, double, internal)"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				{TESTING_INTERNAL_INTERFACE_NAME, "inner", "m2(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner", "m2(Iinternal, Object, double, internal)"},  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				{TESTING_INTERNAL_INTERFACE_NAME, "inner", "m3(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner", "m3(Iinternal, Object, double, internal)"},  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				{TESTING_INTERNAL_INTERFACE_NAME, "inner", "m4(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner", "m4(Iinternal, Object, double, internal)"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				{TESTING_INTERNAL_INTERFACE_NAME, "inner", "m5(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner", "m5(Iinternal, Object, double, internal)"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				{TESTING_INTERNAL_INTERFACE_NAME, "inner", "m6(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner", "m6(Iinternal, Object, double, internal)"}}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
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
		String typename = "testMPL8"; //$NON-NLS-1$
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
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
		String typename = "testMPL9"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] {{TESTING_INTERNAL_INTERFACE_NAME, "inner2", "m1(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner2", "m1(Iinternal, Object, double, internal)"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				{TESTING_INTERNAL_INTERFACE_NAME, "inner2", "m2(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner2", "m2(Iinternal, Object, double, internal)"},  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				{TESTING_INTERNAL_INTERFACE_NAME, "inner2", "m3(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner2", "m3(Iinternal, Object, double, internal)"},  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				{TESTING_INTERNAL_INTERFACE_NAME, "inner3", "m4(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner3", "m4(Iinternal, Object, double, internal)"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				{TESTING_INTERNAL_INTERFACE_NAME, "inner3", "m5(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner3", "m5(Iinternal, Object, double, internal)"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				{TESTING_INTERNAL_INTERFACE_NAME, "inner3", "m6(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner3", "m6(Iinternal, Object, double, internal)"}}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
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
		String typename = "testMPL10"; //$NON-NLS-1$
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
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
		String typename = "testMPL11"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] {
				{TESTING_INTERNAL_INTERFACE_NAME, "inner", "m1(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner", "m1(Iinternal, Object, double, internal)"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				{TESTING_INTERNAL_INTERFACE_NAME, "inner", "m2(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner", "m2(Iinternal, Object, double, internal)"},   //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				{TESTING_INTERNAL_INTERFACE_NAME, "inner", "m3(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner", "m3(Iinternal, Object, double, internal)"},  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m1(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, typename, "m1(Iinternal, Object, double, internal)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m2(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, typename, "m2(Iinternal, Object, double, internal)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m3(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, typename, "m3(Iinternal, Object, double, internal)"}}); //$NON-NLS-1$ //$NON-NLS-2$
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
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
		String typename = "testMPL12"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] {
				{TESTING_INTERNAL_INTERFACE_NAME, "inner", "m1(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner", "m1(Iinternal, Object, double, internal)"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				{TESTING_INTERNAL_INTERFACE_NAME, "inner", "m2(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner", "m2(Iinternal, Object, double, internal)"},  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				{TESTING_INTERNAL_INTERFACE_NAME, "inner", "m3(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, "inner", "m3(Iinternal, Object, double, internal)"},  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m1(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, typename, "m1(Iinternal, Object, double, internal)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m2(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, typename, "m2(Iinternal, Object, double, internal)"}, //$NON-NLS-1$ //$NON-NLS-2$
				{TESTING_INTERNAL_INTERFACE_NAME, typename, "m3(Iinternal, Object, double, internal)"}, {TESTING_INTERNAL_CLASS_NAME, typename, "m3(Iinternal, Object, double, internal)"}}); //$NON-NLS-1$ //$NON-NLS-2$
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
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
		String typename = "testMPL13"; //$NON-NLS-1$
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
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
		String typename = "testMPL14"; //$NON-NLS-1$
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
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
		String typename = "testMPL15"; //$NON-NLS-1$
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
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
		String typename = "testMPL16"; //$NON-NLS-1$
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that a method parameter on a top-level non public type is properly ignored.
	 */
	private void x17(boolean inc) {
		expectingNoProblems();
		String typename = "testMPL17"; //$NON-NLS-1$
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that a method parameter on a top-level non public type is properly ignored (incremental).
	 */
	public void testMethodParameterLeak17I() {
		x17(true);
	}

	/**
	 * Tests that a method parameter on a top-level non public type is properly ignored (full).
	 */
	public void testMethodParameterLeak17F() {
		x17(false);
	}

	/**
	 * Tests that problems for leaking parameters are still properly reported with an @noreference tag on methods
	 * using a full build
	 */
	public void testMethodParameterLeak18F() {
		x18(false);
	}

	/**
	 * Tests that problems for leaking parameters are still properly reported with an @noreference tag on methods
	 * using an incremental build
	 */
	public void testMethodParameterLeak18I() {
		x18(true);
	}

	private void x18(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(1));
		String typename = "testMPL18"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] {{"outer18", typename, "methodLeak(outer18)"}}); //$NON-NLS-1$ //$NON-NLS-2$
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
	}

	public void tesMethodParameterLeak19F() {
		x19(false);
	}

	public void testMethodParameterLeak19I() {
		x19(true);
	}

	/**
	 * Tests that a protected method with an internal parameter is not reported in a final class
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=257113
	 * @param inc
	 */
	private void x19(boolean inc) {
		expectingNoProblems();
		String typename = "testMPL19"; //$NON-NLS-1$
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
	}

	public void tesMethodParameterLeak20F() {
		x20(false);
	}

	public void testMethodParameterLeak20I() {
		x20(true);
	}

	/**
	 * Tests that a protected method with an internal parameter is not reported in a final class
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=257113
	 * @param inc
	 */
	private void x20(boolean inc) {
		expectingNoProblems();
		String typename = "testMPL20"; //$NON-NLS-1$
		deployLeakTest(typename+".java", inc); //$NON-NLS-1$
	}
}
