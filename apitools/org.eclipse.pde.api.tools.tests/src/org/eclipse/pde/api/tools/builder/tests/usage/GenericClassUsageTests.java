package org.eclipse.pde.api.tools.builder.tests.usage;

import junit.framework.Test;

import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * Test class usage using generics
 * 
 * @since 1.0.0
 */
public class GenericClassUsageTests extends ClassUsageTests {

	/**
	 * Constructor
	 * @param name
	 */
	public GenericClassUsageTests(String name) {
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
		return buildTestSuite(GenericClassUsageTests.class);
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
		deployTest(typename, inc);
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
		deployTest(typename, inc);
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
		deployTest(typename, inc);
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
		deployTest(typename, inc);
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
		deployTest(typename, inc);
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
		deployTest(typename, inc);
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
		deployTest(typename, inc);
	}
}
