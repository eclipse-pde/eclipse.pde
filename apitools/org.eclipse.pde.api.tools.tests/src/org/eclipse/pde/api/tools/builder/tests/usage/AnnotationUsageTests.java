/*******************************************************************************
 * Copyright (c) Aug 22, 2018 IBM Corporation and others.
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
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

import junit.framework.Test;

/**
 * Tests using restricted annotations
 *
 * @since 1.0.400
 */
public class AnnotationUsageTests extends UsageTest {

	static final String RESTRICTED_ANNOTATION_NAME = "NoRefAnnotation"; //$NON-NLS-1$

	public AnnotationUsageTests(String name) {
		super(name);
	}

	@Override
	protected int getDefaultProblemId() {
		return ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_REFERENCE, IApiProblem.ANNOTATION);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("annotation"); //$NON-NLS-1$
	}

	@Override
	protected String getTestCompliance() {
		return JavaCore.VERSION_1_5;
	}

	public static Test suite() {
		return buildTestSuite(AnnotationUsageTests.class);
	}

	/**
	 * Tests using a restricted annotation on a type during a full build
	 *
	 * @throws Exception
	 */
	public void testAnnotationUsage1F() throws Exception {
		x1(false);
	}

	/**
	 * Tests using a restricted annotation on a type during an incremental build
	 *
	 * @throws Exception
	 */
	public void testAnnotationUsage1I() throws Exception {
		x1(true);
	}

	private void x1(boolean inc) {
		String typename = "test1"; //$NON-NLS-1$
		int problemid = getDefaultProblemId();
		setExpectedProblemIds(new int[] { problemid });
		setExpectedLineMappings(
				new LineMapping[] { new LineMapping(18, problemid, new String[] { RESTRICTED_ANNOTATION_NAME }) });
		setExpectedMessageArgs(new String[][] { { RESTRICTED_ANNOTATION_NAME } });
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests using a restricted annotation on a field during a full build
	 *
	 * @throws Exception
	 */
	public void testAnnotationUsage2F() throws Exception {
		x2(false);
	}

	/**
	 * Tests using a restricted annotation on a field during an incremental
	 * build
	 *
	 * @throws Exception
	 */
	public void testAnnotationUsage2I() throws Exception {
		x2(true);
	}

	private void x2(boolean inc) {
		String typename = "test2"; //$NON-NLS-1$
		int problemid = getDefaultProblemId();
		setExpectedProblemIds(new int[] { problemid });
		setExpectedLineMappings(
				new LineMapping[] { new LineMapping(20, problemid, new String[] { RESTRICTED_ANNOTATION_NAME }) });
		setExpectedMessageArgs(new String[][] { { RESTRICTED_ANNOTATION_NAME } });
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests using a restricted annotation on a method during a full build
	 *
	 * @throws Exception
	 */
	public void testAnnotationUsage3F() throws Exception {
		x3(false);
	}

	/**
	 * Tests using a restricted annotation on a method during an incremental
	 * build
	 *
	 * @throws Exception
	 */
	public void testAnnotationUsage3I() throws Exception {
		x3(true);
	}

	private void x3(boolean inc) {
		String typename = "test3"; //$NON-NLS-1$
		int problemid = getDefaultProblemId();
		setExpectedProblemIds(new int[] { problemid });
		setExpectedLineMappings(
				new LineMapping[] { new LineMapping(20, problemid, new String[] { RESTRICTED_ANNOTATION_NAME }) });
		setExpectedMessageArgs(new String[][] { { RESTRICTED_ANNOTATION_NAME } });
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests using a restricted annotation on a member type during a full build
	 *
	 * @throws Exception
	 */
	public void testAnnotationUsage4F() throws Exception {
		x4(false);
	}

	/**
	 * Tests using a restricted annotation on a member during an incremental
	 * build
	 *
	 * @throws Exception
	 */
	public void testAnnotationUsage4I() throws Exception {
		x4(true);
	}

	private void x4(boolean inc) {
		String typename = "test4"; //$NON-NLS-1$
		int problemid = getDefaultProblemId();
		setExpectedProblemIds(new int[] { problemid });
		setExpectedLineMappings(
				new LineMapping[] { new LineMapping(20, problemid, new String[] { RESTRICTED_ANNOTATION_NAME }) });
		setExpectedMessageArgs(new String[][] { { RESTRICTED_ANNOTATION_NAME } });
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests using a restricted annotation on a secondary type during a full
	 * build
	 *
	 * @throws Exception
	 */
	public void testAnnotationUsage5F() throws Exception {
		x5(false);
	}

	/**
	 * Tests using a restricted annotation on a secondary type during an
	 * incremental build
	 *
	 * @throws Exception
	 */
	public void testAnnotationUsage5I() throws Exception {
		x5(true);
	}

	private void x5(boolean inc) {
		String typename = "test5"; //$NON-NLS-1$
		int problemid = getDefaultProblemId();
		setExpectedProblemIds(new int[] { problemid });
		setExpectedLineMappings(
				new LineMapping[] { new LineMapping(21, problemid, new String[] { RESTRICTED_ANNOTATION_NAME }) });
		setExpectedMessageArgs(new String[][] { { RESTRICTED_ANNOTATION_NAME } });
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests using a restricted annotation on a field in a member type during a
	 * full build
	 *
	 * @throws Exception
	 */
	public void testAnnotationUsage6F() throws Exception {
		x6(false);
	}

	/**
	 * Tests using a restricted annotation on a field in a member type during an
	 * incremental build
	 *
	 * @throws Exception
	 */
	public void testAnnotationUsage6I() throws Exception {
		x6(true);
	}

	private void x6(boolean inc) {
		String typename = "test6"; //$NON-NLS-1$
		int problemid = getDefaultProblemId();
		setExpectedProblemIds(new int[] { problemid });
		setExpectedLineMappings(
				new LineMapping[] { new LineMapping(21, problemid, new String[] { RESTRICTED_ANNOTATION_NAME }) });
		setExpectedMessageArgs(new String[][] { { RESTRICTED_ANNOTATION_NAME } });
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests using a restricted annotation on a method in a member type during a
	 * full build
	 *
	 * @throws Exception
	 */
	public void testAnnotationUsage7F() throws Exception {
		x7(false);
	}

	/**
	 * Tests using a restricted annotation on a method in a member type during
	 * an incremental build
	 *
	 * @throws Exception
	 */
	public void testAnnotationUsage7I() throws Exception {
		x7(true);
	}

	private void x7(boolean inc) {
		String typename = "test7"; //$NON-NLS-1$
		int problemid = getDefaultProblemId();
		setExpectedProblemIds(new int[] { problemid });
		setExpectedLineMappings(
				new LineMapping[] { new LineMapping(21, problemid, new String[] { RESTRICTED_ANNOTATION_NAME }) });
		setExpectedMessageArgs(new String[][] { { RESTRICTED_ANNOTATION_NAME } });
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests using a restricted annotation on a member type of a secondary type
	 * during a full build
	 *
	 * @throws Exception
	 */
	public void testAnnotationUsage8F() throws Exception {
		x8(false);
	}

	/**
	 * Tests using a restricted annotation on a member type or a secondary type
	 * during an incremental build
	 *
	 * @throws Exception
	 */
	public void testAnnotationUsage8I() throws Exception {
		x8(true);
	}

	private void x8(boolean inc) {
		String typename = "test8"; //$NON-NLS-1$
		int problemid = getDefaultProblemId();
		setExpectedProblemIds(new int[] { problemid });
		setExpectedLineMappings(
				new LineMapping[] { new LineMapping(23, problemid, new String[] { RESTRICTED_ANNOTATION_NAME }) });
		setExpectedMessageArgs(new String[][] { { RESTRICTED_ANNOTATION_NAME } });
		deployUsageTest(typename, inc);
	}

	/**
	 * Tests using a restricted annotation on a local type during a full build
	 *
	 * @throws Exception
	 */
	public void testAnnotationUsage9F() throws Exception {
		x9(false);
	}

	/**
	 * Tests using a restricted annotation on a local type during an incremental
	 * build
	 *
	 * @throws Exception
	 */
	public void testAnnotationUsage9I() throws Exception {
		x9(true);
	}

	private void x9(boolean inc) {
		String typename = "test9"; //$NON-NLS-1$
		int problemid = getDefaultProblemId();
		setExpectedProblemIds(new int[] { problemid });
		setExpectedLineMappings(
				new LineMapping[] { new LineMapping(21, problemid, new String[] { RESTRICTED_ANNOTATION_NAME }) });
		setExpectedMessageArgs(new String[][] { { RESTRICTED_ANNOTATION_NAME } });
		deployUsageTest(typename, inc);
	}
}
