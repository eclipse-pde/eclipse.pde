/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.builder.tests.usage;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

import junit.framework.Test;

public class Java8DefaultMethodUsageTests extends Java8UsageTest {

	public Java8DefaultMethodUsageTests(String name) {
		super(name);
	}

	/**
	 * @return the test class for this suite
	 */
	public static Test suite() {
		return buildTestSuite(Java8DefaultMethodUsageTests.class);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("interface"); //$NON-NLS-1$
	}

	// /**
	/**
	 * Returns the problem id with the given kind
	 *
	 * @param kind
	 * @return the problem id
	 */
	protected int getProblemId(int kind, int flags) {
		return ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.METHOD, kind, flags);
	}


	/**
	 * Tests implementing an interface with no ref anno with a default method and calling
	 * it(full)
	 */
	public void testNoRefAnnotationOnInterfaceWithDefaultMethodF() {
		x1(false);
	}

	/**
	 * Tests implementing an interface with no ref anno with a default method and calling it
	 * (incremental)
	 */
	public void testNoRefAnnotationOnInterfaceWithDefaultMethodI() {
		x1(true);
	}

	private void x1(boolean inc) {
		int[] pids = new int[] {

		getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD) };

		setExpectedProblemIds(pids);
		String typename = "test1"; //$NON-NLS-1$

		String[][] args = new String[][] { {
				"INoRefDefaultInterface", typename, "m1()" } //$NON-NLS-1$//$NON-NLS-2$

		};
		setExpectedMessageArgs(args);
		setExpectedLineMappings(new LineMapping[] { new LineMapping(23, pids[0], args[0])

		});
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests an interface with no ref anno for a restricted default method - 2 (full)
	 */
	public void testNoRefAnnotationOnInterfaceWithDefaultMethod2F() {
		x2(false);
	}

	/**
	 * Tests an interface with no ref anno for a restricted default method - 2(incremental)
	 */
	public void testNoRefAnnotationOnInterfaceWithDefaultMethod2I() {
		x2(true);
	}

	private void x2(boolean inc) {
		int[] pids = new int[] {

		getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD) };

		setExpectedProblemIds(pids);
		String typename = "test2"; //$NON-NLS-1$

		String[][] args = new String[][] { {
				"INoRefDefaultInterface", typename, "m1()" } //$NON-NLS-1$//$NON-NLS-2$

		};
		setExpectedMessageArgs(args);
		setExpectedLineMappings(new LineMapping[] { new LineMapping(25, pids[0], args[0])

		});
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests an impl and direct ref to a restricted default method (full)
	 */
	public void testNoRefAnnotationDefaultMethodF() {
		x3(false);
	}

	/**
	 * Tests an impl and direct ref to a restricted default method ( with no ref) (incremental)
	 */
	public void testNoRefAnnotationDefaultMethodI() {
		x3(true);
	}

	private void x3(boolean inc) {
		int[] pids = new int[] {

		getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD) };

		setExpectedProblemIds(pids);
		String typename = "test3"; //$NON-NLS-1$

		String[][] args = new String[][] { {
				"INoRefDefaultInterface2", typename, "m1()" } //$NON-NLS-1$//$NON-NLS-2$

		};
		setExpectedMessageArgs(args);
		setExpectedLineMappings(new LineMapping[] { new LineMapping(24, pids[0], args[0])

		});
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests an impl and interface ref to a restricted default method (with no ref) (full)
	 */
	public void testNoRefAnnotationDefaultMethod2F() {
		x4(false);
	}

	/**
	 * Tests an impl and interface ref to a restricted default method
	 * (incremental)
	 */
	public void testNoRefAnnotationDefaultMethod2I() {
		x4(true);
	}

	private void x4(boolean inc) {
		int[] pids = new int[] {

		getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD) };

		setExpectedProblemIds(pids);
		String typename = "test4"; //$NON-NLS-1$

		String[][] args = new String[][] { {
				"INoRefDefaultInterface2", typename, "m1()" } //$NON-NLS-1$//$NON-NLS-2$

		};
		setExpectedMessageArgs(args);
		setExpectedLineMappings(new LineMapping[] { new LineMapping(25, pids[0], args[0])

		});
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests no overriding restricted default methods  (full)
	 */
	public void testOverrideDefaultMethodF() {
		x5(false);
	}

	/**
	 * Test no overriding restricted default methods  (incremental)
	 */
	public void testOverrideDefaultMethodI() {
		x5(true);
	}

	private void x5(boolean inc) {
		int[] pids = new int[] { getProblemId(IApiProblem.ILLEGAL_OVERRIDE, IApiProblem.NO_FLAGS) };

		setExpectedProblemIds(pids);
		String typename = "test5"; //$NON-NLS-1$

		String[][] args = new String[][] { {
				"INoOverrideInterface2", typename, "m1()" } //$NON-NLS-1$//$NON-NLS-2$

		};
		setExpectedMessageArgs(args);
		setExpectedLineMappings(new LineMapping[] { new LineMapping(24, pids[0], args[0]) });
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests no overriding restricted default methods via inherited interface
	 * (full)
	 */
	public void testOverrideDefaultMethodInheritedF() {
		x6(false);
	}

	/**
	 * Test no overriding restricted default methods via inherited
	 * interface(incremental)
	 */
	public void testOverrideDefaultMethodInheritedI() {
		x6(true);
	}

	private void x6(boolean inc) {
		int[] pids = new int[] { getProblemId(IApiProblem.ILLEGAL_OVERRIDE, IApiProblem.NO_FLAGS) };

		setExpectedProblemIds(pids);
		String typename = "test6"; //$NON-NLS-1$

		String[][] args = new String[][] { {
				"INoOverrideInterface2", typename, "m1()" } //$NON-NLS-1$//$NON-NLS-2$

		};
		setExpectedMessageArgs(args);
		setExpectedLineMappings(new LineMapping[] { new LineMapping(25, pids[0], args[0]) });
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests no overriding restricted default methods via multiple inherited
	 * interface (full)
	 */
	public void testOverrideDefaultMethodMultipleInheritedF() {
		x7(false);
	}

	/**
	 * Test no overriding restricted default methods via multiple inherited
	 * interface(incremental)
	 */
	public void testOverrideDefaultMethodMultipleInheritedI() {
		x7(true);
	}

	private void x7(boolean inc) {
		int[] pids = new int[] { getProblemId(IApiProblem.ILLEGAL_OVERRIDE, IApiProblem.NO_FLAGS) };

		setExpectedProblemIds(pids);
		String typename = "test7"; //$NON-NLS-1$

		String[][] args = new String[][] { {
				"INoOverrideInterface2", typename, "m1()" } //$NON-NLS-1$//$NON-NLS-2$

		};
		setExpectedMessageArgs(args);
		setExpectedLineMappings(new LineMapping[] { new LineMapping(26, pids[0], args[0]) });
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests implementing an interface with noreference javadoc tag with a default method and calling
	 * it(full)
	 */
	public void testNoRefJavadocTagOnInterfaceWithDefaultMethodF() {
		x8(false);
	}

	/**
	 * Tests implementing an interface  with noreference javadoc tag with a default method and calling it
	 * (incremental)
	 */
	public void testNoRefJavadocTagOnInterfaceWithDefaultMethodI() {
		x8(true);
	}

	private void x8(boolean inc) {
		int[] pids = new int[] {

		getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD) };

		setExpectedProblemIds(pids);
		String typename = "test8"; //$NON-NLS-1$

		String[][] args = new String[][] { {
				"INoRefJavadocDefaultInterface", typename, "m1()" } //$NON-NLS-1$//$NON-NLS-2$

		};
		setExpectedMessageArgs(args);
		setExpectedLineMappings(new LineMapping[] { new LineMapping(23, pids[0], args[0])

		});
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests an interface ref (( javadoc tag) for a restricted default method - 2 (full)
	 */
	public void testNoRefJavadocOnInterfaceWithDefaultMethod2F() {
		x9(false);
	}

	/**
	 * Tests an interface ref (( javadoc tag) for a restricted default method - 2 (incremental)
	 */
	public void testNoRefJavadocOnInterfaceWithDefaultMethod2I() {
		x9(true);
	}

	private void x9(boolean inc) {
		int[] pids = new int[] {

		getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD) };

		setExpectedProblemIds(pids);
		String typename = "test9"; //$NON-NLS-1$

		String[][] args = new String[][] { {
				"INoRefJavadocDefaultInterface", typename, "m1()" } //$NON-NLS-1$//$NON-NLS-2$

		};
		setExpectedMessageArgs(args);
		setExpectedLineMappings(new LineMapping[] { new LineMapping(25, pids[0], args[0])

		});
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests an impl and direct ref to a restricted default method (no ref javadoc tag on method)(full)
	 */
	public void testNoRefJavadocDefaultMethodF() {
		x10(false);
	}

	/**
	 * Tests an impl and direct ref to a restricted default method  (no ref javadoc tag on method)(incremental)
	 */
	public void testNoRefJavadocDefaultMethodI() {
		x10(true);
	}

	private void x10(boolean inc) {
		int[] pids = new int[] {

		getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD) };

		setExpectedProblemIds(pids);
		String typename = "test10"; //$NON-NLS-1$

		String[][] args = new String[][] { {
				"INoRefJavadocDefaultInterface2", typename, "m1()" } //$NON-NLS-1$//$NON-NLS-2$

		};
		setExpectedMessageArgs(args);
		setExpectedLineMappings(new LineMapping[] { new LineMapping(24, pids[0], args[0])

		});
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests an impl and interface ref to a restricted default method  (no ref javadoc tag on method)(full)
	 */
	public void testNoRefJavadocDefaultMethod2F() {
		x11(false);
	}

	/**
	 * Tests an impl and interface ref to a restricted default method (no ref javadoc tag on method)s
	 * (incremental)
	 */
	public void testNoRefjavadocDefaultMethod2I() {
		x11(true);
	}

	private void x11(boolean inc) {
		int[] pids = new int[] {

		getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD) };

		setExpectedProblemIds(pids);
		String typename = "test11"; //$NON-NLS-1$

		String[][] args = new String[][] { {
				"INoRefJavadocDefaultInterface2", typename, "m1()" } //$NON-NLS-1$//$NON-NLS-2$

		};
		setExpectedMessageArgs(args);
		setExpectedLineMappings(new LineMapping[] { new LineMapping(25, pids[0], args[0])

		});
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests no overriding (javadoc tag) restricted default methods (full)
	 */
	public void testOverrideJavadocDefaultMethodF() {
		x12(false);
	}

	/**
	 * Test no overriding  (javadoc tag) restricted default methods (incremental)
	 */
	public void testOverrideJavadocDefaultMethodI() {
		x12(true);
	}

	private void x12(boolean inc) {
		int[] pids = new int[] { getProblemId(IApiProblem.ILLEGAL_OVERRIDE, IApiProblem.NO_FLAGS) };

		setExpectedProblemIds(pids);
		String typename = "test12"; //$NON-NLS-1$

		String[][] args = new String[][] { {
				"INoOverrideJavadocInterface2", typename, "m1()" } //$NON-NLS-1$//$NON-NLS-2$

		};
		setExpectedMessageArgs(args);
		setExpectedLineMappings(new LineMapping[] { new LineMapping(24, pids[0], args[0]) });
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests no overriding  (javadoc tag) restricted default methods via inherited interface
	 * (full)
	 */
	public void testOverrideJavadocDefaultMethodInheritedF() {
		x13(false);
	}

	/**
	 * Test no overriding  (javadoc tag) restricted default methods via inherited
	 * interface(incremental)
	 */
	public void testOverrideJavadocDefaultMethodInheritedI() {
		x13(true);
	}

	private void x13(boolean inc) {
		int[] pids = new int[] { getProblemId(IApiProblem.ILLEGAL_OVERRIDE, IApiProblem.NO_FLAGS) };

		setExpectedProblemIds(pids);
		String typename = "test13"; //$NON-NLS-1$

		String[][] args = new String[][] { {
				"INoOverrideJavadocInterface2", typename, "m1()" } //$NON-NLS-1$//$NON-NLS-2$

		};
		setExpectedMessageArgs(args);
		setExpectedLineMappings(new LineMapping[] { new LineMapping(25, pids[0], args[0]) });
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests no overriding  (javadoc tag) restricted default methods via multiple inherited
	 * interface (full)
	 */
	public void testOverrideJavadocDefaultMethodMultipleInheritedF() {
		x14(false);
	}

	/**
	 * Test no overriding restricted default methods via multiple inherited
	 * interface(incremental)
	 */
	public void testOverrideJavadocDefaultMethodMultipleInheritedI() {
		x14(true);
	}

	private void x14(boolean inc) {
		int[] pids = new int[] { getProblemId(IApiProblem.ILLEGAL_OVERRIDE, IApiProblem.NO_FLAGS) };

		setExpectedProblemIds(pids);
		String typename = "test14"; //$NON-NLS-1$

		String[][] args = new String[][] { {
				"INoOverrideJavadocInterface2", typename, "m1()" } //$NON-NLS-1$//$NON-NLS-2$

		};
		setExpectedMessageArgs(args);
		setExpectedLineMappings(new LineMapping[] { new LineMapping(26, pids[0], args[0]) });
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests that we do not find any problems referencing default methods in JDK
	 * types
	 *
	 * @throws Exception
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=438432
	 */
	public void testSystemComponentNoDefaultMethodsReportedF() throws Exception {
		x15(false);
	}

	/**
	 * Tests that we do not find any problems referencing default methods in JDK
	 * types
	 *
	 * @throws Exception
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=438432
	 */
	public void testSystemComponentNoDefaultMethodsReportedI() throws Exception {
		x15(true);
	}

	private void x15(boolean inc) throws Exception {
		String typename = "test15"; //$NON-NLS-1$
		expectingNoJDTProblems();
		IPath typepath = new Path(getTestingProjectName()).append(UsageTest.SOURCE_PATH).append(typename).addFileExtension("java"); //$NON-NLS-1$
		expectingNoProblemsFor(typepath);
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests that we find problems referencing default methods in other bundle
	 * types
	 *
	 * @throws Exception
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=438432
	 */
	public void testOtherBundleDefaultMethodCallF() throws Exception {
		x16(false);
	}

	/**
	 * Tests that we find problems referencing default methods in other bundle
	 * types
	 *
	 * @throws Exception
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=438432
	 */
	public void testOtherBundleDefaultMethodCallI() throws Exception {
		x16(true);
	}

	private void x16(boolean inc) {
		int[] pids = new int[] { getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD) };
		String typename = "test16"; //$NON-NLS-1$
		setExpectedProblemIds(pids);
		String[][] args = new String[][] { {
				"INoRefJavadocDefaultInterface2", typename, "m1()" } //$NON-NLS-1$//$NON-NLS-2$

		};
		setExpectedMessageArgs(args);
		setExpectedLineMappings(new LineMapping[] { new LineMapping(25, pids[0], args[0])

		});
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests that we find problems referencing default methods in other bundle
	 * types
	 *
	 * @throws Exception
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=438432
	 */
	public void testOtherBundleDefaultMethodCall2F() throws Exception {
		x17(false);
	}

	/**
	 * Tests that we find problems referencing default methods in other bundle
	 * types
	 *
	 * @throws Exception
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=438432
	 */
	public void testOtherBundleDefaultMethodCall2I() throws Exception {
		x17(true);
	}

	private void x17(boolean inc) {
		int[] pids = new int[] { getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD) };
		String typename = "test17"; //$NON-NLS-1$
		setExpectedProblemIds(pids);
		String[][] args = new String[][] { {
				"INoRefJavadocDefaultInterface", typename, "m1()" } //$NON-NLS-1$//$NON-NLS-2$

		};
		setExpectedMessageArgs(args);
		setExpectedLineMappings(new LineMapping[] { new LineMapping(25, pids[0], args[0])

		});
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests that we find problems referencing default methods in other bundle
	 * types
	 *
	 * @throws Exception
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=438432
	 */
	public void testOtherBundleDefaultMethodCall3F() throws Exception {
		x18(false);
	}

	/**
	 * Tests that we find problems referencing default methods in other bundle
	 * types
	 *
	 * @throws Exception
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=438432
	 */
	public void testOtherBundleDefaultMethodCall3I() throws Exception {
		x18(true);
	}

	private void x18(boolean inc) {
		int[] pids = new int[] { getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD) };
		String typename = "test18"; //$NON-NLS-1$
		setExpectedProblemIds(pids);
		String[][] args = new String[][] { {
				"INoRefJavadocDefaultInterface", typename, "m1()" } //$NON-NLS-1$//$NON-NLS-2$

		};
		setExpectedMessageArgs(args);
		setExpectedLineMappings(new LineMapping[] { new LineMapping(26, pids[0], args[0])

		});
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests that we find problems referencing default methods in other bundle
	 * types
	 *
	 * @throws Exception
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=438432
	 */
	public void testOtherBundleDefaultMethodCall4F() throws Exception {
		x19(false);
	}

	/**
	 * Tests that we find problems referencing default methods in other bundle
	 * types
	 *
	 * @throws Exception
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=438432
	 */
	public void testOtherBundleDefaultMethodCall4I() throws Exception {
		x19(true);
	}

	private void x19(boolean inc) {
		int[] pids = new int[] { getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD) };
		String typename = "test19"; //$NON-NLS-1$
		setExpectedProblemIds(pids);
		String[][] args = new String[][] { {
				"INoRefJavadocDefaultInterface", typename, "m1()" } //$NON-NLS-1$//$NON-NLS-2$

		};
		setExpectedMessageArgs(args);
		setExpectedLineMappings(new LineMapping[] { new LineMapping(25, pids[0], args[0])

		});
		deployUsageTest(typename, inc);
	}
}
