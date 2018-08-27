/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
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

import junit.framework.Test;

import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;


/**
 * Tests the performance of incrementally building Java annotations
 *
 * @since 1.0.0
 */
public class AnnotationIncrementalBuildTests extends EnumIncrementalBuildTests {

	/**
	 * Constructor
	 * @param name
	 */
	public AnnotationIncrementalBuildTests(String name) {
		super(name);
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(AnnotationIncrementalBuildTests.class);
	}

	/**
	 * Tests the performance of API problems on a Java annotation.
	 * <br>
	 * This test uses <code>java5.performance.api.TestAnnot</code>
	 * @throws Exception if something bad happens or unexpected problems are detected
	 */
	public void testIncrementalAnnot() throws Exception {
		int[] problemids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_COMPATIBILITY,
						IDelta.ANNOTATION_ELEMENT_TYPE,
						IDelta.REMOVED,
						IDelta.METHOD_WITH_DEFAULT_VALUE),
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_USAGE,
						IElementDescriptor.TYPE,
						IApiProblem.UNSUPPORTED_TAG_USE,
						IApiProblem.NO_FLAGS)
		};
		deployIncrementalPerformanceTest(
				"Incremetal Build (Annotation) - All",  //$NON-NLS-1$
				"test12",  //$NON-NLS-1$
				JAVA_5_PERF,
				"api.TestAnnot",  //$NON-NLS-1$
				problemids, 500);
	}

	/**
	 * Tests the performance of compatibility problem detection incrementally in a Java annotation
	 * <br>
	 * This test uses <code>java5.performance.api.TestAnnot</code>
	 *
	 * @throws Exception if something bad happens or unexpected problems are detected
	 */
	public void _testIncrementalAnnotCompat() throws Exception {
		int[] problemids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_COMPATIBILITY,
						IDelta.ANNOTATION_ELEMENT_TYPE,
						IDelta.REMOVED,
						IDelta.METHOD_WITH_DEFAULT_VALUE),
		};
		deployIncrementalPerformanceTest(
				"Incremetal Build (Annotation) - Compatibility",  //$NON-NLS-1$
				"test13",  //$NON-NLS-1$
				JAVA_5_PERF,
				"api.TestAnnot",  //$NON-NLS-1$
				problemids, 500);
	}

	/**
	 * Tests the performance of unsupported API tag problem detection incrementally in a Java annotation
	 * <br>
	 * This test uses <code>java5.performance.api.TestAnnot</code>
	 *
	 * @throws Exception if something bad happens or unexpected problems are detected
	 */
	public void _testIncrementalAnnotTags() throws Exception {
		int[] problemids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_USAGE,
						IElementDescriptor.TYPE,
						IApiProblem.UNSUPPORTED_TAG_USE,
						IApiProblem.NO_FLAGS),
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_USAGE,
						IElementDescriptor.METHOD,
						IApiProblem.UNSUPPORTED_TAG_USE,
						IApiProblem.NO_FLAGS)
		};
		deployIncrementalPerformanceTest(
				"Incremetal Build (Annotation) - Unsupported Tags",  //$NON-NLS-1$
				"test14",  //$NON-NLS-1$
				JAVA_5_PERF,
				"api.TestAnnot",  //$NON-NLS-1$
				problemids, 500);
	}
}
