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

public class Java8ConsRefInstantiateUsageTests extends Java8UsageTest {

	public Java8ConsRefInstantiateUsageTests(String name) {
		super(name);
	}

	/**
	 * @return the test class for this suite
	 */
	public static Test suite() {
		return buildTestSuite(Java8ConsRefInstantiateUsageTests.class);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("methodref"); //$NON-NLS-1$
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
	 * Tests illegal reference to class with no instantiate (full)
	 */
	public void testConsRefInstantiateF() {
		x1(false);
	}

	/**
	 * Tests illegal reference to class with no instantiate (incremental)
	 */
	public void testConsRefInstantiateI() {
		x1(true);
	}

	private void x1(boolean inc) {
		int[] pids = new int[] {

				getProblemId(IApiProblem.ILLEGAL_INSTANTIATE, IApiProblem.NO_FLAGS),
				getProblemId(IApiProblem.ILLEGAL_INSTANTIATE, IApiProblem.NO_FLAGS),
				getProblemId(IApiProblem.ILLEGAL_INSTANTIATE, IApiProblem.NO_FLAGS) };
		setExpectedProblemIds(pids);
		String typename = "testConstructorRefInstantiate"; //$NON-NLS-1$

		String[][] args = new String[][] {
				{ "ConstructorReference2", typename }, //$NON-NLS-1$
				{ "ConstructorReference2", typename }, //$NON-NLS-1$
				{ "ConstructorReference2", typename } //$NON-NLS-1$

		};
		setExpectedMessageArgs(args);
		setExpectedLineMappings(new LineMapping[] {
				new LineMapping(33, pids[0], args[0]), new LineMapping(35, pids[1], args[1]),
				new LineMapping(37, pids[2], args[2])

		});

		deployUsageTest(typename, inc);
	}


	/**
	 * Tests illegal reference to class with no annotation instantiate (full)
	 */
	public void testConsRefInstantiateAnnoF() {
		x2(false);
	}

	/**
	 * Tests illegal reference to class with no annotation instantiate
	 * (incremental)
	 */
	public void testConsRefInstantiateAnnoI() {
		x2(true);
	}

	private void x2(boolean inc) {
		int[] pids = new int[] {

				getProblemId(IApiProblem.ILLEGAL_INSTANTIATE, IApiProblem.NO_FLAGS),
				getProblemId(IApiProblem.ILLEGAL_INSTANTIATE, IApiProblem.NO_FLAGS),
				getProblemId(IApiProblem.ILLEGAL_INSTANTIATE, IApiProblem.NO_FLAGS) };
		setExpectedProblemIds(pids);
		String typename = "testConstructorRefInstantiateAnnotation"; //$NON-NLS-1$

		String[][] args = new String[][] {
				{ "ConstructorReferenceAnno2", typename }, //$NON-NLS-1$
				{ "ConstructorReferenceAnno2", typename }, //$NON-NLS-1$
				{ "ConstructorReferenceAnno2", typename } //$NON-NLS-1$

		};
		setExpectedMessageArgs(args);
		setExpectedLineMappings(new LineMapping[] {
				new LineMapping(33, pids[0], args[0]), new LineMapping(35, pids[1], args[1]),
				new LineMapping(37, pids[2], args[2])

		});

		deployUsageTest(typename, inc);
	}


}
