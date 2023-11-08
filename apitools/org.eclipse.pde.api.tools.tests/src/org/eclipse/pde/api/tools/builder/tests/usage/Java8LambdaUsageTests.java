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
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

import junit.framework.Test;

/**
 * Tests labmda expression usage for Java 8 code snippets
 *
 */
public class Java8LambdaUsageTests extends Java8UsageTest {

	int pid = -1;

	public Java8LambdaUsageTests(String name) {
		super(name);
	}

	/**
	 * @return the test class for this suite
	 */
	public static Test suite() {
		return buildTestSuite(Java8LambdaUsageTests.class);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("lambda"); //$NON-NLS-1$
	}

	@Override
	protected int getDefaultProblemId() {
		if (pid == -1) {
			pid = ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_REFERENCE, IApiProblem.FIELD);
		}
		return pid;
	}

	/**
	 * Returns the problem id with the given kind
	 *
	 * @param kind
	 * @return the problem id
	 */
	protected int getProblemId(int kind, int flags) {
		return ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, kind, flags);
	}

	/**
	 * Tests illegal use of fields inside a lambda expression (full)
	 */
	public void testLambdaExpressionF() {
		x1(false);
	}

	/**
	 * Tests illegal use of fields inside a lambda expression (incremental)
	 */
	public void testLambdaExpressionI() {
		x1(true);
	}

	private void x1(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(2));
		String typename = "testLambdaExpression"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] {
				{ FieldUsageTests.FIELD_CLASS_NAME, typename, "f1" }, //$NON-NLS-1$
				{ FieldUsageTests.FIELD_CLASS_NAME, typename, "f2" }, //$NON-NLS-1$
		});
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests illegal use of fields inside a lambda statement (full)
	 */
	public void testLambdaStatementF() {
		x2(false);
	}

	/**
	 * Tests illegal use of fields inside a lambda statement (incremental)
	 */
	public void testLambdaStatementI() {
		x2(true);
	}

	private void x2(boolean inc) {
		setExpectedProblemIds(getDefaultProblemIdSet(2));
		String typename = "testLambdaStatement"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] {
				{ FieldUsageTests.FIELD_CLASS_NAME, typename, "f1" }, //$NON-NLS-1$
				{ FieldUsageTests.FIELD_CLASS_NAME, typename, "f2" }, //$NON-NLS-1$
		});
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests illegal use of fields inside a lambda block statement (full)
	 */
	public void testLambdaBlockStatementF() {
		x3(false);
	}

	/**
	 * Tests illegal use of fields inside a lambda block statement (incremental)
	 */
	public void testLambdaBlockStatementI() {
		x3(true);
	}


	private void x3(boolean inc) {
		int[] pids = new int[] {
				getProblemId(IApiProblem.ILLEGAL_INSTANTIATE, IApiProblem.NO_FLAGS),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.FIELD),
				getProblemId(IApiProblem.ILLEGAL_INSTANTIATE, IApiProblem.NO_FLAGS),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.FIELD) };
		setExpectedProblemIds(pids);

		String typename = "testLambdaBlockStatement"; //$NON-NLS-1$
		setExpectedMessageArgs(new String[][] {
				{ ClassUsageTests.CLASS_NAME, typename, "ClassUsageClass" }, //$NON-NLS-1$
				{ FieldUsageTests.FIELD_CLASS_NAME, typename, "f1" }, //$NON-NLS-1$
				{ "inner", typename }, //$NON-NLS-1$
				{ FieldUsageTests.FIELD_CLASS_NAME, typename, "f2" }, //$NON-NLS-1$
		});
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests whether use of a functional interface method with a no reference
	 * restriction is marked as illegal use (full) Currently this is not
	 * supported (Bug 431749)
	 */
	public void testLambdaRestrictedFunctionalInterfaceF() {
		x4(false);
	}

	/**
	 * Tests whether use of a functional interface method with a no reference
	 * restriction is marked as illegal use (incremental) Currently this is not
	 * supported (Bug 431749)
	 */
	public void testLambdaRestrictedFunctionalInterfaceI() {
		x4(true);
	}

	private void x4(boolean inc) {
		setExpectedProblemIds(new int[0]);
		setExpectedMessageArgs(new String[0][0]);
		String typename = "testLambdaRestrictedInterface"; //$NON-NLS-1$
		// int[] pids = new int[] { getProblemId(IApiProblem.ILLEGAL_REFERENCE,
		// IApiProblem.METHOD) };
		// setExpectedProblemIds(pids);
		// setExpectedMessageArgs(new String[][] {
		//				{ InterfaceUsageTests.INTERFACE_NAME, typename, "NoRefFunctionalInterface" } //$NON-NLS-1$
		// });
		deployUsageTest(typename, inc);
	}
}
