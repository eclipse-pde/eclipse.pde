package org.eclipse.pde.api.tools.builder.tests.usage;

import junit.framework.Test;

import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

public class Java8DefaultMethodUsageTests extends Java8UsageTest {

	/**
	 * Constructor
	 * 
	 * @param name
	 */
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
	 * Tests  implementing an interface with a default method and calling
	 * it(full)
	 */
	public void testNoRefAnnotationOnInterfaceWithDefaultMethodF() {
		x1(false);
	}

	/**
	 * Tests  implementing an interface with a default method and calling
	 * it (incremental)
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
		setExpectedLineMappings(new LineMapping[] {
				new LineMapping(20, pids[0], args[0])

		});
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests  an interface ref for a restricted default method (full)
	 */
	public void testNoRefAnnotationOnInterfaceWithDefaultMethod2F() {
		x2(false);
	}

	/**
	 * Tests an interface ref for a restricted default method (incremental)
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
		setExpectedLineMappings(new LineMapping[] { new LineMapping(22, pids[0], args[0])

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
	 * Tests an impl and direct ref to a restricted default method (incremental)
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
		setExpectedLineMappings(new LineMapping[] { new LineMapping(21, pids[0], args[0])

		});
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests an impl and interface ref to a restricted default method (full)
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
		setExpectedLineMappings(new LineMapping[] { new LineMapping(22, pids[0], args[0])

		});
		deployUsageTest(typename, inc);
	}

	// TODO uncomment this after fixing bug below.
	// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=433528
	// there should be 1 error
	// /**
	// * Tests no overriding restricted default methods (full)
	// */
	// public void testOverrideDefaultMethodF() {
	// x5(false);
	// }
	//
	// /**
	// * Test no overriding restricted default methods (incremental)
	// */
	// public void testOverrideDefaultMethodI() {
	// x5(true);
	// }
	//
	// private void x5(boolean inc) {
	// int[] pids = null;
	//
	// setExpectedProblemIds(pids);
	//		String typename = "test5"; //$NON-NLS-1$
	//
	// String[][] args = null;
	// setExpectedMessageArgs(args);
	// setExpectedLineMappings(null);
	// deployUsageTest(typename, inc);
	// }

	// TODO to add javadoc tag test cases

}
