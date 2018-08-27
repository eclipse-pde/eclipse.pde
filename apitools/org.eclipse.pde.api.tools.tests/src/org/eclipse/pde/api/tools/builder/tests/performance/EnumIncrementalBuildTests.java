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
package org.eclipse.pde.api.tools.builder.tests.performance;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

import junit.framework.Test;

/**
 * This class extends the incremental performance tests for Java 5 constructs.
 * This test class uses the JDT APT core plugin
 *
 * @since 1.0.0
 */
public class EnumIncrementalBuildTests extends IncrementalBuildTests {

	/**
	 * Id for the JDT APT core plugin. <br>
	 * Value is: <code>java5.performance</code>
	 */
	protected static final String JAVA_5_PERF = "java5.performance"; //$NON-NLS-1$

	public EnumIncrementalBuildTests(String name) {
		super(name);
	}

	@Override
	protected String getTestCompliance() {
		return JavaCore.VERSION_1_5;
	}

	/**
	 * @return the test to run in this class.
	 */
	public static Test suite() {
		return buildTestSuite(EnumIncrementalBuildTests.class);
	}

	/**
	 * Tests a Java 5 construct with multiple API problems using an incremental
	 * build. <br>
	 * This test uses <code>java5.performance.api.TestEnum</code>
	 *
	 * @throws Exception if something bad happens or unexpected problems are
	 *             detected
	 */
	public void testIncremantalEnum() throws Exception {
		int[] problemids = new int[] {
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY, IDelta.ENUM_ELEMENT_TYPE, IDelta.REMOVED, IDelta.ENUM_CONSTANT),
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_INSTANTIATE, IApiProblem.NO_FLAGS),
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.METHOD, IApiProblem.API_LEAK, IApiProblem.LEAK_RETURN_TYPE),
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.UNSUPPORTED_TAG_USE, IApiProblem.NO_FLAGS) };
		deployIncrementalPerformanceTest("Incremetal Build (Enum) - All", //$NON-NLS-1$
				"test7", //$NON-NLS-1$
				JAVA_5_PERF, "api.TestEnum", //$NON-NLS-1$
				problemids, 500);
	}

	/**
	 * Tests the performance of removing an enum constant from an Enum. <br>
	 * This test uses <code>java5.performance.api.TestEnum</code>
	 *
	 * @throws Exception if something bad happens or unexpected problems are
	 *             detected
	 */
	public void _testIncrementalEnumCompat() throws Exception {
		int[] problemids = new int[] { ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY, IDelta.ENUM_ELEMENT_TYPE, IDelta.REMOVED, IDelta.ENUM_CONSTANT) };
		deployIncrementalPerformanceTest("Incremental Build (Enum) - Compatibility", //$NON-NLS-1$
				"test8", //$NON-NLS-1$
				JAVA_5_PERF, "api.TestEnum", //$NON-NLS-1$
				problemids, 500);
	}

	/**
	 * Tests the performance of API leak detection incrementally on an Enum <br>
	 * This test uses <code>java5.performance.api.TestEnum</code>
	 *
	 * @throws Exception if something bad happens or unexpected problems are
	 *             detected
	 */
	public void _testIncremetalEnumLeak() throws Exception {
		int[] problemids = new int[] { ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.METHOD, IApiProblem.API_LEAK, IApiProblem.LEAK_RETURN_TYPE) };
		deployIncrementalPerformanceTest("Incremtal Build (Enum) - API Leak", //$NON-NLS-1$
				"test9", //$NON-NLS-1$
				JAVA_5_PERF, "api.TestEnum", //$NON-NLS-1$
				problemids, 500);
	}

	/**
	 * Tests the performance of API usage detection incrementally on an Enum <br>
	 * This test uses <code>java5.performance.api.TestEnum</code>
	 *
	 * @throws Exception if something bad happens or unexpected problems are
	 *             detected
	 */
	public void _testIncrementalEnumUsage() throws Exception {
		int[] problemids = new int[] { ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_INSTANTIATE, IApiProblem.NO_FLAGS) };
		deployIncrementalPerformanceTest("Incremtal Build (Enum) - API Usage", //$NON-NLS-1$
				"test10", //$NON-NLS-1$
				JAVA_5_PERF, "api.TestEnum", //$NON-NLS-1$
				problemids, 500);
	}

	/**
	 * Tests the performance of invalid tags on an enum + enum elements <br>
	 * This test uses <code>java5.performance.api.TestEnum</code>
	 *
	 * @throws Exception if something bad happens or unexpected problems are
	 *             detected
	 */
	public void _testIncrementalEnumTags() throws Exception {
		int[] problemids = new int[] {
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.UNSUPPORTED_TAG_USE, IApiProblem.NO_FLAGS),
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.FIELD, IApiProblem.UNSUPPORTED_TAG_USE, IApiProblem.NO_FLAGS), };
		deployIncrementalPerformanceTest("Incremtal Build (Enum) - Unsupported Tags", //$NON-NLS-1$
				"test11", //$NON-NLS-1$
				JAVA_5_PERF, "api.TestEnum", //$NON-NLS-1$
				problemids, 500);
	}
}
