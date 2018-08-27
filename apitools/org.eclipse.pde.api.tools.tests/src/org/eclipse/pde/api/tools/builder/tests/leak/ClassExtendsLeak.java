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
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

import junit.framework.Test;

/**
 * Tests that leaked members via extends for classes is properly detected
 *
 * @since 1.0
 */
public class ClassExtendsLeak extends LeakTest {

	protected final String TESTING_INTERNAL_PROTECTED_CLASS_NAME = "internalprotected"; //$NON-NLS-1$
	protected final String TESTING_INTERNAL_PUBLIC_FIELD_CLASS_NAME = "internalpublicfield"; //$NON-NLS-1$
	protected final String TESTING_INTERNAL_PROTECTED_FIELD_CLASS_NAME = "internalprotectedfield"; //$NON-NLS-1$
	private int pid = -1;

	public ClassExtendsLeak(String name) {
		super(name);
	}

	@Override
	protected int getDefaultProblemId() {
		if (pid == -1) {
			pid = ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.API_LEAK, IApiProblem.ILLEGAL_EXTEND);
		}
		return pid;
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("class"); //$NON-NLS-1$
	}

	/**
	 * @return the test for this class
	 */
	public static Test suite() {
		return buildTestSuite(ClassExtendsLeak.class);
	}

	@Override
	protected String getTestCompliance() {
		return JavaCore.VERSION_1_5;
	}

	/**
	 * Tests that an API class that extends an internal type is properly flagged
	 * as leaking using a full build
	 */
	public void testClassExtendsLeak1F() {
		x1(false);
	}

	/**
	 * Tests that an API class that extends an internal type is properly flagged
	 * as leaking using an incremental build
	 */
	public void testClassExtendsLeak1I() {
		x1(true);
	}

	private void x1(boolean inc) {
		setExpectedProblemIds(new int[] { getDefaultProblemId() });
		String typename = "test1"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] { {
				TESTING_INTERNAL_CLASS_NAME, typename } });
		deployLeakTest(typename + ".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that an outer type in an API class that extends an internal type is
	 * not flagged as a leak using a full build
	 */
	public void testClassExtendsLeak2F() {
		x2(false);
	}

	/**
	 * Tests that an outer type in an API class that extends an internal type is
	 * not flagged as a leak using an incremental build
	 */
	public void testClassExtendsLeak2I() {
		x2(true);
	}

	private void x2(boolean inc) {
		expectingNoProblems();
		String typename = "test2"; //$NON-NLS-1$
		deployLeakTest(typename + ".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that an inner type in an API class that extends an internal type is
	 * not flagged as a leak using a full build
	 */
	public void testClassExtendsLeak3F() {
		x3(false);
	}

	/**
	 * Tests that an inner type in an API class that extends an internal type is
	 * not flagged as a leak using an incremental build
	 */
	public void testClassExtendsLeak3I() {
		x3(true);
	}

	private void x3(boolean inc) {
		expectingNoProblems();
		String typename = "test3"; //$NON-NLS-1$
		deployLeakTest(typename + ".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that a static inner type in an API class that extends an internal
	 * type is not flagged as a leak using a full build
	 */
	public void testClassExtendsLeak4F() {
		x4(false);
	}

	/**
	 * Tests that a static inner type in an API class that extends an internal
	 * type is not flagged as a leak using an incremental build
	 */
	public void testClassExtendsLeak4I() {
		x4(true);
	}

	private void x4(boolean inc) {
		expectingNoProblems();
		String typename = "test4"; //$NON-NLS-1$
		deployLeakTest(typename + ".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that an API class that extends an internal type is flagged as a
	 * leak even with an @noextend tag being used using a full build
	 */
	public void testClassExtendsLeak5F() {
		x5(false);
	}

	/**
	 * Tests that an API class that extends an internal type is flagged as a
	 * leak even with an @noextend tag being used using an incremental build
	 */
	public void testClassExtendsLeak5I() {
		x5(true);
	}

	private void x5(boolean inc) {
		setExpectedProblemIds(new int[] { getDefaultProblemId() });
		String typename = "test5"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] { {
				TESTING_INTERNAL_CLASS_NAME, typename } });
		deployLeakTest(typename + ".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that an API class that extends an internal type is flagged as a
	 * leak even with an @noinstantiate tag being used using a full build
	 */
	public void testClassExtendsLeak6F() {
		x6(false);
	}

	/**
	 * Tests that an API class that extends an internal type is flagged as a
	 * leak even with an @noinstantiate tag being used using an incremental
	 * build
	 */
	public void testClassExtendsLeak6I() {
		x6(true);
	}

	private void x6(boolean inc) {
		setExpectedProblemIds(new int[] { getDefaultProblemId() });
		String typename = "test6"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] { {
				TESTING_INTERNAL_CLASS_NAME, typename } });
		deployLeakTest(typename + ".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that an API class that extends an internal type is flagged as a
	 * leak even with @noextend and @noinstantiate tags being used using a full
	 * build
	 */
	public void testClassExtendsLeak7F() {
		x7(false);
	}

	/**
	 * Tests that an API class that extends an internal type is flagged as a
	 * leak even with @noextend and @noinstantiate tags being used using an
	 * incremental build
	 */
	public void testClassExtendsLeak7I() {
		x7(true);
	}

	private void x7(boolean inc) {
		setExpectedProblemIds(new int[] { getDefaultProblemId() });
		String typename = "test7"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] { {
				TESTING_INTERNAL_CLASS_NAME, typename } });
		deployLeakTest(typename + ".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that a public static internal class that extends an internal type
	 * in an API class is flagged as a leak even with @noextend and @noinstantiate
	 * tags being used using a full build
	 */
	public void testClassExtendsLeak8F() {
		x8(false);
	}

	/**
	 * Tests that a public static internal class that extends an internal type
	 * in an API class is flagged as a leak even with @noextend and @noinstantiate
	 * tags being used using an incremental build
	 */
	public void testClassExtendsLeak8I() {
		x8(true);
	}

	private void x8(boolean inc) {
		setExpectedProblemIds(new int[] { getDefaultProblemId() });
		String typename = "test15"; //$NON-NLS-1$
		String innertype = "inner"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] { {
				TESTING_INTERNAL_CLASS_NAME, innertype } });
		deployLeakTest(typename + ".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that more than one public static internal class that extends an
	 * internal type in an API class is flagged as a leak even with @noextend
	 * and @noinstantiate tags being used using a full build
	 */
	public void testClassExtendsLeak9F() {
		x9(false);
	}

	/**
	 * Tests that more than one public static internal class that extends an
	 * internal type in an API class is flagged as a leak even with @noextend
	 * and @noinstantiate tags being used using an incremental build
	 */
	public void testClassExtendsLeak9I() {
		x9(true);
	}

	private void x9(boolean inc) {
		setExpectedProblemIds(new int[] {
				getDefaultProblemId(), getDefaultProblemId() });
		String typename = "test16"; //$NON-NLS-1$
		String innertype1 = "inner"; //$NON-NLS-1$
		String innertype2 = "inner2"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] {
				{ TESTING_INTERNAL_CLASS_NAME, innertype1 },
				{ TESTING_INTERNAL_CLASS_NAME, innertype2 } });
		deployLeakTest(typename + ".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that having an @noextend tag(s) on classes does not remove leak
	 * problems using a full build
	 */
	public void testClassExtendsLeak10F() {
		x10(false);
	}

	/**
	 * Tests that having an @noextend tag(s) on classes does not remove leak
	 * problems using an incremental build
	 */
	public void testClassExtendsLeak10I() {
		x10(true);
	}

	private void x10(boolean inc) {
		setExpectedProblemIds(new int[] {
				getDefaultProblemId(), getDefaultProblemId() });
		String typename = "test17"; //$NON-NLS-1$
		String innertype1 = "inner"; //$NON-NLS-1$
		String innertype2 = "inner2"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] {
				{ TESTING_INTERNAL_CLASS_NAME, innertype1 },
				{ TESTING_INTERNAL_CLASS_NAME, innertype2 } });
		deployLeakTest(typename + ".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that having an @noinstantiate tag(s) on classes does not remove
	 * leak problems using a full build
	 */
	public void testClassExtendsLeak11F() {
		x11(false);
	}

	/**
	 * Tests that having an @noinstantiate tag(s) on classes does not remove
	 * leak problems using an incremental build
	 */
	public void testClassExtendsLeak11I() {
		x11(true);
	}

	private void x11(boolean inc) {
		setExpectedProblemIds(new int[] {
				getDefaultProblemId(), getDefaultProblemId() });
		String typename = "test18"; //$NON-NLS-1$
		String innertype1 = "inner"; //$NON-NLS-1$
		String innertype2 = "inner2"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] {
				{ TESTING_INTERNAL_CLASS_NAME, innertype1 },
				{ TESTING_INTERNAL_CLASS_NAME, innertype2 } });
		deployLeakTest(typename + ".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that having an @noinstantiate and @noextend tag(s) on classes does
	 * not remove leak problems using a full build
	 */
	public void testClassExtendsLeak12F() {
		x12(false);
	}

	/**
	 * Tests that having an @noinstantiate and @noextend tag(s) on classes does
	 * not remove leak problems using an incremental build
	 */
	public void testClassExtendsLeak12I() {
		x12(true);
	}

	private void x12(boolean inc) {
		setExpectedProblemIds(new int[] {
				getDefaultProblemId(), getDefaultProblemId() });
		String typename = "test19"; //$NON-NLS-1$
		String innertype1 = "inner"; //$NON-NLS-1$
		String innertype2 = "inner2"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] {
				{ TESTING_INTERNAL_CLASS_NAME, innertype1 },
				{ TESTING_INTERNAL_CLASS_NAME, innertype2 } });
		deployLeakTest(typename + ".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that an API class that extends an internal type is *not* flagged as
	 * a leak when a @noextend tag being used using a full build, since only
	 * protected methods are exposed by the internal class.
	 */
	public void testClassExtendsLeak23F() {
		x23(false);
	}

	/**
	 * Tests that an API class that extends an internal type is *not* flagged as
	 * a leak when a @noextend tag being used using an incremental build, since
	 * only protected methods are exposed by the internal class.
	 */
	public void testClassExtendsLeak23I() {
		x23(true);
	}

	private void x23(boolean inc) {
		String typename = "test23"; //$NON-NLS-1$
		deployLeakTest(typename + ".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that an API class that extends an internal type is flagged as a
	 * leak since a @noextend tag is *not* used during a full build (since
	 * protected methods are exposed by the internal class, and extending is
	 * allowed).
	 */
	public void testClassExtendsLeak24F() {
		x24(false);
	}

	/**
	 * Tests that an API class that extends an internal type is flagged as a
	 * leak since a @noextend tag being *not* used during an incremental build,
	 * (since protected methods are exposed by the internal class, and extending
	 * is allowed).
	 */
	public void testClassExtendsLeak24I() {
		x24(true);
	}

	private void x24(boolean inc) {
		setExpectedProblemIds(new int[] { getDefaultProblemId() });
		String typename = "test24"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] { {
				TESTING_INTERNAL_PROTECTED_CLASS_NAME, typename } });
		deployLeakTest(typename + ".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that an API class that extends an internal type with a public field
	 * is a leak.
	 */
	public void testClassExtendsLeak25F() {
		x25(false);
	}

	/**
	 * Tests that an API class that extends an internal type with a public field
	 * is a leak.
	 */
	public void testClassExtendsLeak25I() {
		x25(true);
	}

	private void x25(boolean inc) {
		setExpectedProblemIds(new int[] { getDefaultProblemId() });
		String typename = "test25"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] { {
				TESTING_INTERNAL_PUBLIC_FIELD_CLASS_NAME, typename } });
		deployLeakTest(typename + ".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that an API class that extends an internal type with a public field
	 * is a leak, even with a @noextend tag.
	 */
	public void testClassExtendsLeak26F() {
		x26(false);
	}

	/**
	 * Tests that an API class that extends an internal type with a public field
	 * is a leak, even with a @noextend tag.
	 */
	public void testClassExtendsLeak26I() {
		x26(true);
	}

	private void x26(boolean inc) {
		setExpectedProblemIds(new int[] { getDefaultProblemId() });
		String typename = "test26"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] { {
				TESTING_INTERNAL_PUBLIC_FIELD_CLASS_NAME, typename } });
		deployLeakTest(typename + ".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that an API class that extends an internal type with a protected
	 * field is a leak (since it allows extending).
	 */
	public void testClassExtendsLeak27F() {
		x27(false);
	}

	/**
	 * Tests that an API class that extends an internal type with a protected
	 * field is a leak (since it allows extending)
	 */
	public void testClassExtendsLeak27I() {
		x27(true);
	}

	private void x27(boolean inc) {
		setExpectedProblemIds(new int[] { getDefaultProblemId() });
		String typename = "test27"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] { {
				TESTING_INTERNAL_PROTECTED_FIELD_CLASS_NAME, typename } });
		deployLeakTest(typename + ".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that an API class that extends an internal type with a protected
	 * field is *not* a leak (since it *disallows* extending).
	 */
	public void testClassExtendsLeak28F() {
		x28(false);
	}

	/**
	 * Tests that an API class that extends an internal type with a protected
	 * field is *not* a leak (since it *disallows* extending)
	 */
	public void testClassExtendsLeak28I() {
		x28(true);
	}

	private void x28(boolean inc) {
		expectingNoProblems();
		String typename = "test28"; //$NON-NLS-1$
		deployLeakTest(typename + ".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that an API class that extends an internal type with a private
	 * field is *not* a leak.
	 */
	public void testClassExtendsLeak29F() {
		x29(false);
	}

	/**
	 * Tests that an API class that extends an internal type with a private
	 * field is *not* a leak.
	 */
	public void testClassExtendsLeak29I() {
		x29(true);
	}

	private void x29(boolean inc) {
		expectingNoProblems();
		String typename = "test29"; //$NON-NLS-1$
		deployLeakTest(typename + ".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that an API class that extends an top level non public type is a
	 * leak.
	 */
	public void testClassExtendsLeak30F() {
		x30(false);
	}

	/**
	 * Tests that an API class that extends an top level non public type is a
	 * leak.
	 */
	public void testClassExtendsLeak30I() {
		x30(true);
	}

	private void x30(boolean inc) {
		setExpectedProblemIds(new int[] { getDefaultProblemId() });
		String typename = "test30"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] { { "outer30", typename } }); //$NON-NLS-1$
		deployLeakTest(typename + ".java", inc); //$NON-NLS-1$
	}

	public void testClassExtendsAndOverridesLeak31F() {
		x32(false);
	}

	public void testClassExtendsAndOverridesLeak31I() {
		x32(true);
	}

	/**
	 * Tests that a class that extends an internal class but overrides all of
	 * its methods is not reported as leaking.
	 *
	 * @param inc
	 */
	private void x32(boolean inc) {
		expectingNoProblems();
		String typename = "test32"; //$NON-NLS-1$
		deployLeakTest(typename + ".java", inc); //$NON-NLS-1$
	}

	/**
	 * Tests that an API class that extends a  noextend class is a leak
	 */
	public void testClassExtendsNoExtendClass33F() {
		x34(false);
	}


	public void testClassExtendsNoExtendClass13I() {
		x34(true);
	}

	private void x34(boolean inc) {
		int pid = ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE,
				IApiProblem.API_LEAK, IApiProblem.LEAK_BY_EXTENDING_NO_EXTEND_CLASS_TYPE);
		setExpectedProblemIds(new int[] { pid });
		String typename = "test34"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] { { "classNoExtend", typename } }); //$NON-NLS-1$
		deployLeakTest(typename + ".java", inc); //$NON-NLS-1$
	}

	/**
	 * Test that an API class that extends a  noextend class is not a  leak
	 * if it is noextend
	 */
	public void testNoExtendClassExtendsNoExtendClass14F() {
		x35(false);
	}


	public void testNoExtendClassExtendsNoExtendClass14I() {
		x35(true);
	}

	private void x35(boolean inc) {
		expectingNoProblems();
		String typename = "test35"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] { { "class1", typename } }); //$NON-NLS-1$
		deployLeakTest(typename + ".java", inc); //$NON-NLS-1$
	}

	/**
	 * Test that an API class that extends a noextend class is not a leak if it is
	 * final
	 */
	public void testFinalClassExtendsNoExtendClass14F() {
		x36(false);
	}

	public void testFinalClassExtendsNoExtendClass14I() {
		x36(true);
	}

	private void x36(boolean inc) {
		expectingNoProblems();
		String typename = "test36"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] { { "class1", typename } }); //$NON-NLS-1$
		deployLeakTest(typename + ".java", inc); //$NON-NLS-1$
	}

}
