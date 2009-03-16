/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.builder.tests.performance;

import junit.framework.Test;

import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * This class extends the incremental performance tests for Java 5 constructs.
 * This test class uses the JDT APT core plugin
 * 
 * @since 1.0.0
 */
public class EnumIncrementalBuildTests extends IncrementalBuildTests {

	/**
	 * Id for the JDT APT core plugin.
	 * <br>
	 * Value is: <code>java5.performance</code>
	 */
	protected static final String JAVA_5_PERF = "java5.performance";
	
	/**
	 * Constructor
	 * @param name
	 */
	public EnumIncrementalBuildTests(String name) {
		super(name);
	}

	/**
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getTestCompliance()
	 */
	@Override
	protected String getTestCompliance() {
		return CompilerOptions.VERSION_1_5;
	}
	
	/**
	 * @return the test to run in this class.
	 */
	public static Test suite() {
		return buildTestSuite(EnumIncrementalBuildTests.class);
	}
	
	/**
	 * Tests a Java 5 construct with multiple API problems using an
	 * incremental build.
	 * <br>
	 * This test uses <code>java5.performance.api.TestEnum</code>
	 * 
	 * @throws Exception if something bad happens or unexpected problems are detected
	 */
	public void testIncremantalEnum() throws Exception {
		int[] problemids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_COMPATIBILITY, 
						IDelta.ENUM_ELEMENT_TYPE, 
						IDelta.REMOVED, 
						IDelta.ENUM_CONSTANT),
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_USAGE, 
						IElementDescriptor.TYPE, 
						IApiProblem.ILLEGAL_INSTANTIATE, 
						IApiProblem.NO_FLAGS),
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_USAGE, 
						IElementDescriptor.METHOD, 
						IApiProblem.API_LEAK, 
						IApiProblem.LEAK_RETURN_TYPE),
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_USAGE, 
						IElementDescriptor.TYPE, 
						IApiProblem.UNSUPPORTED_TAG_USE, 
						IApiProblem.NO_FLAGS)
		};
		deployIncrementalPerformanceTest(
				"Incremetal Build (Enum) - All", 
				"test7", 
				JAVA_5_PERF, 
				"api.TestEnum", 
				problemids);
	}
	
	/**
	 * Tests the performance of removing an enum constant from an Enum.
	 * <br>
	 * This test uses <code>java5.performance.api.TestEnum</code>
	 * 
	 * @throws Exception if something bad happens or unexpected problems are detected
	 */
	public void _testIncrementalEnumCompat() throws Exception {
		int[] problemids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_COMPATIBILITY, 
						IDelta.ENUM_ELEMENT_TYPE, 
						IDelta.REMOVED, 
						IDelta.ENUM_CONSTANT)
		};
		deployIncrementalPerformanceTest(
				"Incremental Build (Enum) - Compatibility", 
				"test8", 
				JAVA_5_PERF, 
				"api.TestEnum", 
				problemids);
	}
	
	/**
	 * Tests the performance of API leak detection incrementally on an Enum
	 * <br>
	 * This test uses <code>java5.performance.api.TestEnum</code>
	 * @throws Exception if something bad happens or unexpected problems are detected
	 */
	public void _testIncremetalEnumLeak() throws Exception {
		int[] problemids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_USAGE, 
						IElementDescriptor.METHOD, 
						IApiProblem.API_LEAK, 
						IApiProblem.LEAK_RETURN_TYPE)
		};
		deployIncrementalPerformanceTest(
				"Incremtal Build (Enum) - API Leak", 
				"test9",
				JAVA_5_PERF, 
				"api.TestEnum", 
				problemids);
	}
	
	/**
	 * Tests the performance of API usage detection incrementally on an Enum
	 * <br>
	 * This test uses <code>java5.performance.api.TestEnum</code>
	 * @throws Exception if something bad happens or unexpected problems are detected
	 */
	public void _testIncrementalEnumUsage() throws Exception {
		int[] problemids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_USAGE, 
						IElementDescriptor.TYPE, 
						IApiProblem.ILLEGAL_INSTANTIATE, 
						IApiProblem.NO_FLAGS)
		};
		deployIncrementalPerformanceTest(
				"Incremtal Build (Enum) - API Usage", 
				"test10", 
				JAVA_5_PERF, 
				"api.TestEnum", 
				problemids);
	}
	
	/**
	 * Tests the performance of invalid tags on an enum + enum elements
	 * <br>
	 * This test uses <code>java5.performance.api.TestEnum</code>
	 * 
	 * @throws Exception if something bad happens or unexpected problems are detected
	 */
	public void _testIncrementalEnumTags() throws Exception {
		int[] problemids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_USAGE, 
						IElementDescriptor.TYPE, 
						IApiProblem.UNSUPPORTED_TAG_USE, 
						IApiProblem.NO_FLAGS),
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_USAGE, 
						IElementDescriptor.FIELD, 
						IApiProblem.UNSUPPORTED_TAG_USE, 
						IApiProblem.NO_FLAGS),
		};
		deployIncrementalPerformanceTest(
				"Incremtal Build (Enum) - Unsupported Tags", 
				"test11", 
				JAVA_5_PERF, 
				"api.TestEnum", 
				problemids);
	}
}
