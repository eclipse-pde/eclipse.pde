/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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

import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

public class Java8MethodConstRefUsageTests extends Java8UsageTest {
	/**
	 * Constructor
	 * 
	 * @param name
	 */
	public Java8MethodConstRefUsageTests(String name) {
		super(name);
	}

	/**
	 * @return the test class for this suite
	 */
	public static Test suite() {
		return buildTestSuite(Java8MethodConstRefUsageTests.class);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("methodref"); //$NON-NLS-1$
	}

	/**
	 * Returns a standard method usage problem allowing the kind to be specified
	 * 
	 * @param kind
	 * @return problem id for the specified kind
	 */
	protected int getProblemId(int kind, int flags) {
		return ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.METHOD, kind, flags);
	}

	/**
	 * Tests illegal references to method reference and constructor reference
	 * (full)
	 */
	public void testMethodConstructorRefF() {
		x1(false);
	}

	/**
	 * Tests illegal references to method reference and constructor reference
	 * (incremental)
	 */
	public void testMethodConstructorRefI() {
		x1(true);
	}

	private void x1(boolean inc) {
		int[] pids = new int[] {

				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.CONSTRUCTOR_METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.CONSTRUCTOR_METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.CONSTRUCTOR_METHOD) };
		setExpectedProblemIds(pids);
		String typename = "testMethodReference"; //$NON-NLS-1$

		String[][] args = new String[][] {
				{ "MethodReference", typename, "method1()" }, //$NON-NLS-1$ //$NON-NLS-2$
				{ "MethodReference", typename, "method2()" }, //$NON-NLS-1$ //$NON-NLS-2$
				{ "ConstructorReference()", typename, null }, //$NON-NLS-1$ 
				{ "ConstructorReference(String)", typename, null }, //$NON-NLS-1$
				{ "ConstructorReference(List<String>)", typename, null }, //$NON-NLS-1$ 

		};
		setExpectedMessageArgs(args);
		setExpectedLineMappings(new LineMapping[] {
				new LineMapping(31, pids[0], args[0]),
				new LineMapping(34, pids[1], args[1]),
				new LineMapping(36, pids[2], args[2]),
				new LineMapping(38, pids[3], args[3]),
				new LineMapping(40, pids[4], args[4]),

		});

		deployUsageTest(typename, inc);
	}

	/**
	 * Tests illegal references to method reference (full)
	 */
	public void testMethodConstructorRef2F() {
		x2(false);
	}

	/**
	 * Tests illegal references to method reference (incremental)
	 */
	public void testMethodConstructorRef2I() {
		x2(true);
	}

	private void x2(boolean inc) {
		int[] pids = new int[] {

				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD) };
		setExpectedProblemIds(pids);
		String typename = "testMethodReference2"; //$NON-NLS-1$

		String[][] args = new String[][] {
				{ "MR", typename, "mrCompare(String, String)" }, //$NON-NLS-1$ //$NON-NLS-2$
				{ "MR", typename, "mrCompare2(String, String)" }, //$NON-NLS-1$ //$NON-NLS-2$
				{ "MR", typename, "con(Supplier<T>)" } }; //$NON-NLS-1$ //$NON-NLS-2$

		
		setExpectedMessageArgs(args);
		setExpectedLineMappings(new LineMapping[] {
				new LineMapping(27, pids[0], args[0]),
				new LineMapping(29, pids[1], args[1]),
				new LineMapping(31, pids[2], args[2]),


		});

		deployUsageTest(typename, inc);
	}

	/**
	 * Tests illegal annotation references to method reference and constructor
	 * reference (full)
	 */
	public void testMethodConstructorRefAnnoF() {
		x3(false);
	}

	/**
	 * Tests illegal annotation references to method reference and constructor
	 * reference (incremental)
	 */
	public void testMethodConstructorRefAnnoI() {
		x3(true);
	}

	private void x3(boolean inc) {
		int[] pids = new int[] {

				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.CONSTRUCTOR_METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.CONSTRUCTOR_METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.CONSTRUCTOR_METHOD) };
		setExpectedProblemIds(pids);
		String typename = "testMethodReferenceAnnotation"; //$NON-NLS-1$

		String[][] args = new String[][] {
				{ "MethodReferenceAnnotation", typename, "method1()" }, //$NON-NLS-1$ //$NON-NLS-2$
				{ "MethodReferenceAnnotation", typename, "method2()" }, //$NON-NLS-1$ //$NON-NLS-2$
				{ "ConstructorReferenceAnnotation()", typename, null }, //$NON-NLS-1$ 
				{ "ConstructorReferenceAnnotation(String)", typename, null }, //$NON-NLS-1$
				{
						"ConstructorReferenceAnnotation(List<String>)", typename, null } //$NON-NLS-1$

		};
		setExpectedMessageArgs(args);
		setExpectedLineMappings(new LineMapping[] {
				new LineMapping(31, pids[0], args[0]),
				new LineMapping(34, pids[1], args[1]),
				new LineMapping(36, pids[2], args[2]),
				new LineMapping(38, pids[3], args[3]),
				new LineMapping(40, pids[4], args[4]),

		});

		deployUsageTest(typename, inc);
	}

	/**
	 * Tests illegal annotation references to method reference (full)
	 */
	public void testMethodConstructorRefAnno2F() {
		x4(false);
	}

	/**
	 * Tests illegal annotation references to method reference (incremental)
	 */
	public void testMethodConstructorRefAnno2I() {
		x4(true);
	}

	private void x4(boolean inc) {
		int[] pids = new int[] {

				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD),
				getProblemId(IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD) };
		setExpectedProblemIds(pids);
		String typename = "testMethodReferenceAnnotation2"; //$NON-NLS-1$

		String[][] args = new String[][] {
				{ "MRAnnotation", typename, "mrCompare(String, String)" }, //$NON-NLS-1$ //$NON-NLS-2$
				{ "MRAnnotation", typename, "mrCompare2(String, String)" }, //$NON-NLS-1$ //$NON-NLS-2$
				{ "MRAnnotation", typename, "con(Supplier<T>)" } }; //$NON-NLS-1$ //$NON-NLS-2$

		setExpectedMessageArgs(args);
		setExpectedLineMappings(new LineMapping[] {
				new LineMapping(27, pids[0], args[0]),
				new LineMapping(29, pids[1], args[1]),
				new LineMapping(31, pids[2], args[2]),

		});

		deployUsageTest(typename, inc);
	}

}
