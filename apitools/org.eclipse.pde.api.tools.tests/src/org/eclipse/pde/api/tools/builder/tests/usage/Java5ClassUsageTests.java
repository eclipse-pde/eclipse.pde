/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
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

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * Test class usage using generics
 * 
 * @since 1.0.0
 */
public class Java5ClassUsageTests extends ClassUsageTests {

	protected static final String GENERIC_CLASS_NAME = "GenericClassUsageClass";
	
	/**
	 * Constructor
	 * @param name
	 */
	public Java5ClassUsageTests(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getTestCompliance()
	 */
	@Override
	protected String getTestCompliance() {
		return CompilerOptions.VERSION_1_5;
	}
	
	/**
	 * @return the test class for this suite
	 */
	public static Test suite() {
		return buildTestSuite(Java5ClassUsageTests.class);
	}
	
	/**
	 * Tests that illegal anonymous extends are found in a field declaration in a generic method
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=246672
	 * @param inc
	 */
	private void x1(boolean inc) {
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_EXTEND, IApiProblem.ANONYMOUS_TYPE)
		});
		String typename = "testGA1";
		setExpectedMessageArgs(new String[][] {
				{"x.y.z.testGA1.m1(Object[], List<String>)", CLASS_NAME}
		});
		deployUsageTest(typename, inc);
	}
	
	public void testAnonymousTypeGenericMethod1F() {
		x1(false);
	}
	
	public void testAnonymousTypeGenericMethod1I() {
		x1(true);
	}
	
	/**
	 * 
	 * Tests that illegal anonymous extends are found in a return statement in a generic method
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=246672
	 * @param inc
	 */
	private void x2(boolean inc) {
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_EXTEND, IApiProblem.ANONYMOUS_TYPE)
		});
		String typename = "testGA2";
		setExpectedMessageArgs(new String[][] {
				{"x.y.z.testGA2.m1(Object[], List<String>)", CLASS_NAME}
		});
		deployUsageTest(typename, inc);
	}
	
	public void testAnonymousTypeGenericMethod2F() {
		x2(false);
	}
	
	public void testAnonymousTypeGenericMethod2I() {
		x2(true);
	}
	
	/**
	 * 
	 * Tests that illegal anonymous extends are found in a field declaration in a generic constructor method
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=246672
	 * @param inc
	 */
	private void x3(boolean inc) {
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_EXTEND, IApiProblem.ANONYMOUS_TYPE)
		});
		String typename = "testGA3";
		setExpectedMessageArgs(new String[][] {
				{"x.y.z.testGA3.testGA3(Object[], List<String>)", CLASS_NAME}
		});
		deployUsageTest(typename, inc);
	}
	
	public void testAnonymousTypeGenericMethod3F() {
		x3(false);
	}
	
	public void testAnonymousTypeGenericMethod3I() {
		x3(true);
	}
	
	/**
	 * Tests that illegal anonymous extends are found in a field declaration in a generic type
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=246672
	 * @param inc
	 */
	private void x4(boolean inc) {
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_EXTEND, IApiProblem.ANONYMOUS_TYPE)
		});
		String typename = "testGA4";
		setExpectedMessageArgs(new String[][] {
				{"x.y.z.testGA4<T>", CLASS_NAME}
		});
		deployUsageTest(typename, inc);
	}
	
	public void testAnonymousTypeGenericField1F() {
		x4(false);
	}
	
	public void testAnonymousTypeGenericField1I() {
		x4(true);
	}
	
	/**
	 * Tests that illegal local type extends are found in a generic constructor method 
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=246672
	 * @param inc
	 */
	private void x5(boolean inc) {
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_EXTEND, IApiProblem.LOCAL_TYPE)
		});
		String typename = "testGA5";
		setExpectedMessageArgs(new String[][] {
				{"inner", "x.y.z.testGA5.testGA5(Object[], List<String>)", CLASS_NAME}
		});
		deployUsageTest(typename, inc);
	}
	
	public void testLocalTypeGeneicMethod1F() {
		x5(false);
	}
	
	public void testLocalTypeGeneircMethod1I() {
		x5(true);
	}
	
	/**
	 * Tests that illegal local type extends are found in a generic method 
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=246672
	 * @param inc
	 */
	private void x6(boolean inc) {
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_EXTEND, IApiProblem.LOCAL_TYPE)
		});
		String typename = "testGA6";
		setExpectedMessageArgs(new String[][] {
				{"inner", "x.y.z.testGA6.m1(Object[], List<String>)", CLASS_NAME}
		});
		deployUsageTest(typename, inc);
	}
	
	public void testLocalTypeGeneicMethod2F() {
		x6(false);
	}
	
	public void testLocalTypeGeneircMethod2I() {
		x6(true);
	}
	
	public void testGenericInstantiate1F() {
		x7(false);
	}
	
	public void testGenericInstantiate1I() {
		x7(true);
	}
	
	/**
	 * Tests that a problem is correctly created for an illegal instantiate when the 
	 * constructor being called has generics i.e.
	 * <pre>
	 * <code>Clazz clazz = new Clazz&lt;String&gt;();</code>
	 * </pre>
	 * 
	 * @param inc
	 */
	private void x7(boolean inc) {
		setExpectedProblemIds(new int[] {
			getProblemId(IApiProblem.ILLEGAL_INSTANTIATE, IApiProblem.NO_FLAGS),
			getProblemId(IApiProblem.ILLEGAL_INSTANTIATE, IApiProblem.NO_FLAGS)
		});
		String typename = "testC10";
		setExpectedMessageArgs(new String[][] {
				{GENERIC_CLASS_NAME+"<T>", typename},
				{GENERIC_CLASS_NAME+"<T>", INNER_NAME1}
		});
		deployUsageTest(typename, inc);
	}
	
	public void testLocalClassExtends1F() {
		x8(false);
	}
	
	public void testLocalClassExtends1I() {
		x8(true);
	}
	
	/**
	 * Tests that local types with the same name in different methods are correctly found
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=258101
	 * @param inc
	 */
	private void x8(boolean inc) {
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_EXTEND, IApiProblem.LOCAL_TYPE),
				getProblemId(IApiProblem.ILLEGAL_EXTEND, IApiProblem.LOCAL_TYPE),
				getProblemId(IApiProblem.ILLEGAL_EXTEND, IApiProblem.LOCAL_TYPE)
		});
		getEnv().getJavaProject(getTestingProjectName()).getOption(JavaCore.COMPILER_COMPLIANCE, true);
		setExpectedMessageArgs(new String[][] {
				{"inner", "x.y.z.testA7.m1()", CLASS_NAME},
				{"inner", "x.y.z.testA7.m2()", CLASS_NAME},
				{"inner", "x.y.z.testA7.m3()", CLASS_NAME}
		});
		setExpectedLineMappings(new LineMapping[] {
				new LineMapping(21, getExpectedProblemIds()[0], getExpectedMessageArgs()[0]),
				new LineMapping(28, getExpectedProblemIds()[1], getExpectedMessageArgs()[1]),
				new LineMapping(35, getExpectedProblemIds()[2], getExpectedMessageArgs()[2])
		});
		String typename = "testA7";
		deployUsageTest(typename, inc);
	}
	
	/**
	 * Tests an anonymous type defined in the return statement of a method illegally extending a
	 * restricted type using a full build.
	 * 
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=246672
	 */
	public void testAnonymousClassExtends2F() {
		x9(false);
	}
	
	/**
	 * Tests an anonymous type defined in the return statement of a method illegally extending a
	 * restricted type using an incremental build.
	 * 
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=246672
	 */
	public void testAnonymousClassExtends2I() {
		x9(true);
	}
	
	private void x9(boolean inc) {
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_EXTEND, IApiProblem.ANONYMOUS_TYPE)
		});
		String typename = "testA2";
		setExpectedMessageArgs(new String[][] {
				{"x.y.z.testA2.m1()", CLASS_NAME}	
		});
		deployUsageTest(typename, inc);
	}
	
	/**
	 * Tests an anonymous type defined in a method field illegally extending a
	 * restricted type using a full build.
	 * 
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=246672
	 */
	public void testAnonymousClassExtends3F() {
		x10(false);
	}
	
	/**
	 * Tests an anonymous type defined in a method field illegally extending a
	 * restricted type using an incremental build.
	 * 
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=246672
	 */
	public void testAnonymousClassExtends3I() {
		x10(true);
	}
	
	private void x10(boolean inc) {
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_EXTEND, IApiProblem.ANONYMOUS_TYPE)
		});
		String typename = "testA3";
		setExpectedMessageArgs(new String[][] {
				{"x.y.z.testA3.m1()", CLASS_NAME}	
		});
		deployUsageTest(typename, inc);
	}
	
	/**
	 * Tests a local type defined in a constructor illegally extending a
	 * restricted type using a full build.
	 * 
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=246672
	 */
	public void testAnonymousClassExtends5F() {
		x11(false);
	}
	
	/**
	 * Tests a local type defined in a constructor illegally extending a
	 * restricted type using an incremental build.
	 * 
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=246672
	 */
	public void testAnonymousClassExtends5I() {
		x11(true);
	}
	
	private void x11(boolean inc) {
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_EXTEND, IApiProblem.LOCAL_TYPE)
		});
		String typename = "testA5";
		setExpectedMessageArgs(new String[][] {
				{"inner", "x.y.z.testA5.testA5()", CLASS_NAME}	
		});
		deployUsageTest(typename, inc);
	}
	
	/**
	 * Tests a local anonymous field defined in a constructor illegally extending a
	 * restricted type using a full build.
	 * 
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=246672
	 */
	public void testAnonymousClassExtends6F() {
		x12(false);
	}
	
	/**
	 * Tests a local anonymous field defined in a constructor illegally extending a
	 * restricted type using an incremental build.
	 * 
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=246672
	 */
	public void testAnonymousClassExtends6I() {
		x12(true);
	}
	
	private void x12(boolean inc) {
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_EXTEND, IApiProblem.ANONYMOUS_TYPE)
		});
		String typename = "testA6";
		setExpectedMessageArgs(new String[][] {
				{"x.y.z.testA6.testA6()", CLASS_NAME}	
		});
		deployUsageTest(typename, inc);
	}
	
	/**
	 * Tests a local anonymous field defined in a static initializer illegally extending a
	 * restricted type using a full build.
	 * 
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=246672
	 */
	public void testAnonymousClassExtends7F() {
		x17(false);
	}
	
	/**
	 * Tests a local anonymous field defined in a static initializer illegally extending a
	 * restricted type using an incremental build.
	 * 
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=246672
	 */
	public void testAnonymousClassExtends7I() {
		x17(true);
	}
	
	private void x17(boolean inc) {
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_EXTEND, IApiProblem.ANONYMOUS_TYPE)
		});
		String typename = "testGA7";
		setExpectedMessageArgs(new String[][] {
				{"x.y.z.testGA7<T>", CLASS_NAME}
		});
		deployUsageTest(typename, inc);
	}
	public void testAnonymousClassExtendsGenericReturnF() {
		x16(false);
	}
	
	public void testAnonymousClassExtendsGenericReturnI() {
		x16(true);
	}
	
	/**
	 * Tests that an anonymous declaration is detected extending a restricted type
	 * within a method with a generic return type
	 * @param inc
	 */
	private void x16(boolean inc) {
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_EXTEND, IApiProblem.ANONYMOUS_TYPE)
		});
		String typename = "testA10";
		setExpectedMessageArgs(new String[][] {
				{"x.y.z.testA10.m1()", CLASS_NAME}	
		});
		deployUsageTest(typename, inc);
	}
	
	/**
	 * Tests a local type defined in a constructor illegally extending a
	 * restricted type using a full build.
	 * 
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=246672
	 */
	public void testLocalClassExtends3F() {
		x13(false);
	}
	
	/**
	 * Tests a local type defined in a constructor illegally extending a
	 * restricted type using an incremental build.
	 * 
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=246672
	 */
	public void testLocalClassExtends3I() {
		x13(true);
	}
	
	private void x13(boolean inc) {
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_EXTEND, IApiProblem.LOCAL_TYPE)
		});
		String typename = "testA5";
		setExpectedMessageArgs(new String[][] {
				{"inner", "x.y.z.testA5.testA5()", CLASS_NAME}	
		});
		deployUsageTest(typename, inc);
	}
	
	/**
	 * Tests a local type defined in a constructor illegally extending a
	 * restricted type using a full build.
	 * 
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=246672
	 */
	public void testLocalClassExtends2F() {
		x14(false);
	}
	
	/**
	 * Tests a local type defined in a constructor illegally extending a
	 * restricted type using an incremental build.
	 * 
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=246672
	 */
	public void testLocalClassExtends2I() {
		x14(true);
	}
	
	private void x14(boolean inc) {
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_EXTEND, IApiProblem.LOCAL_TYPE)
		});
		String typename = "testA8";
		setExpectedMessageArgs(new String[][] {
				{"inner", "x.y.z.testA8.m1()", CLASS_NAME}	
		});
		deployUsageTest(typename, inc);
	}
	
	public void testLocalClassExtendsGenericReturnF() {
		x15(false);
	}
	
	public void testLocalClassExtendsGenericReturnI() {
		x15(true);
	}
	
	/**
	 * Tests finding a problem with a local type extending a restricted type in a method 
	 * with a generic return type
	 * @param inc
	 */
	private void x15(boolean inc) {
		setExpectedProblemIds(new int[] {
				getProblemId(IApiProblem.ILLEGAL_EXTEND, IApiProblem.LOCAL_TYPE)
		});
		String typename = "testA9";
		setExpectedMessageArgs(new String[][] {
				{"inner", "x.y.z.testA9.m1()", CLASS_NAME}	
		});
		deployUsageTest(typename, inc);
	}
	
	/**
	 * Tests that the correct markers are created and placed for classes with inner types 
	 * that illegally implement interfaces
	 * 
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=403258
	 * @since 1.0.300
	 * @throws Exception
	 */
	public void testLocalClassIllegalImplements1I() throws Exception {
		x18(true);
	}
	
	/**
	 * Tests that the correct markers are created and placed for local types 
	 * that illegally implement interfaces
	 * 
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=403258
	 * @since 1.0.300
	 * @throws Exception
	 */
	public void testLocalClassIllegalImplements1F() throws Exception {
		x18(false);
	}
	
	private void x18(boolean inc) {
		int localId = getProblemId(IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.LOCAL_TYPE);
		int indId = getProblemId(IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.INDIRECT_LOCAL_REFERENCE);
		setExpectedProblemIds(new int[] {localId, indId, localId, indId});
		String typename = "testC11";
		setExpectedLineMappings(new LineMapping[] {
			new LineMapping(29, localId, new String[] {"local1", "x.y.z.testC11.method1()", "INoImpl2"}),
			new LineMapping(31, indId, new String[] {"local2", "x.y.z.testC11.method1()", "INoImpl5", "INoImpl2"}),
			new LineMapping(21, localId, new String[] {"local3", "x.y.z.testC11.inner1.method2()", "INoImpl3"}),
			new LineMapping(23, indId, new String[] {"local4", "x.y.z.testC11.inner1.method2()", "INoImpl6", "INoImpl2"})
		});
		setExpectedMessageArgs(new String[][] {
				{"local1", "x.y.z.testC11.method1()", "INoImpl2"},
				{"local2", "x.y.z.testC11.method1()", "INoImpl5", "INoImpl2"},
				{"local3", "x.y.z.testC11.inner1.method2()", "INoImpl3"},
				{"local4", "x.y.z.testC11.inner1.method2()", "INoImpl6", "INoImpl2"}
		});
		deployUsageTest(typename, inc);
	}
	
	/**
	 * Tests that the correct markers are created and placed for local types 
	 * that illegally implement interfaces, where there are more than one local type in the
	 * compilation unit indirectly implementing the same interface via the same proxy interface
	 * 
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=403258
	 * @since 1.0.300
	 * @throws Exception
	 */
	public void testLocalClassIllegaImplements2I() throws Exception {
		x19(true);
	}
	
	/**
	 * Tests that the correct markers are created and placed for local types 
	 * that illegally implement interfaces, where there are more than one local type in the
	 * compilation unit indirectly implementing the same interface via the same proxy interface
	 * 
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=403258
	 * @since 1.0.300
	 * @throws Exception
	 */
	public void testLocalClassIllegalImplements2F() throws Exception {
		x19(false);
	}
	
	private void x19(boolean inc) {
		int indId = getProblemId(IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.INDIRECT_LOCAL_REFERENCE);
		setExpectedProblemIds(new int[] {indId, indId});
		setExpectedLineMappings(new LineMapping[] {
				new LineMapping(24, indId, new String[] {"local2", "x.y.z.testC12.method1()", "INoImpl5", "INoImpl2"}),
				new LineMapping(18, indId, new String[] {"local4", "x.y.z.testC12.inner1.method2()", "INoImpl5", "INoImpl2"}),
			});
		setExpectedMessageArgs(new String[][] {
				{"local2", "x.y.z.testC12.method1()", "INoImpl5", "INoImpl2"},
				{"local4", "x.y.z.testC12.inner1.method2()", "INoImpl5", "INoImpl2"}
		});
		String typename = "testC12";
		deployUsageTest(typename, inc);
	}
	
	/**
	 * Tests that the correct markers are created and placed for anonymous types 
	 * that illegally implement interfaces
	 * 
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=403258
	 * @since 1.0.300
	 * @throws Exception
	 */
	public void testAnonymousClassIllegaImplements1I() throws Exception {
		x20(true);
	}
	
	/**
	 * Tests that the correct markers are created and placed for local types 
	 * that illegally implement interfaces, where there are more than one local type in the
	 * compilation unit indirectly implementing the same interface via the same proxy interface
	 * 
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=403258
	 * @since 1.0.300
	 * @throws Exception
	 */
	public void testAnonymousClassIllegalImplements1F() throws Exception {
		x20(false);
	}
	
	private void x20(boolean inc) {
		int indId = getProblemId(IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.ANONYMOUS_TYPE);
		setExpectedProblemIds(new int[] {indId, indId});
		setExpectedLineMappings(new LineMapping[] {
				new LineMapping(25, indId, new String[] {"x.y.z.testC13.testC13()", "INoImpl2"}),
				new LineMapping(19, indId, new String[] {"x.y.z.testC13.inner.method()", "INoImpl2"})
			});
		setExpectedMessageArgs(new String[][] {
				{"x.y.z.testC13.testC13()", "INoImpl2"},
				{"x.y.z.testC13.inner.method()", "INoImpl2"}
		});
		String typename = "testC13";
		deployUsageTest(typename, inc);
	}
}
