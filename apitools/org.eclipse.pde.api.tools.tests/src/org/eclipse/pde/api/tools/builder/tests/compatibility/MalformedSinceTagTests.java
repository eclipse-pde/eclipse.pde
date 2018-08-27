/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.builder.tests.compatibility;

import junit.framework.Test;

import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * Tests that the builder correctly finds and reports malformed since tags
 *
 * @since 1.0
 */
public class MalformedSinceTagTests extends SinceTagTest {


	/**
	 * Constructor
	 * @param name
	 */
	public MalformedSinceTagTests(String name) {
		super(name);
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(MalformedSinceTagTests.class);
	}

	protected void configureExpectedProblems(int elementType, String[] messageArgs) {
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
					IApiProblem.CATEGORY_SINCETAGS,
					elementType,
					IApiProblem.SINCE_TAG_MALFORMED,
					0)
			};
			setExpectedProblemIds(ids);
			String[][] args = new String[1][];
			args[0] = messageArgs;
			setExpectedMessageArgs(args);
	}

	/**
	 * Tests adding a field with a malformed since tag
	 */
	private void xMalformedField(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("MalformedField.java"); //$NON-NLS-1$
		configureExpectedProblems(IDelta.FIELD_ELEMENT_TYPE, new String[]{"2.0a", "FIELD"}); //$NON-NLS-1$ //$NON-NLS-2$
		performCompatibilityTest(filePath, incremental);
	}

	public void testMalformedFieldI() throws Exception {
		xMalformedField(true);
	}

	public void testMalformedFieldF() throws Exception {
		xMalformedField(false);
	}

	/**
	 * Tests adding a method with a malformed since tag
	 */
	private void xMalformedMethod(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("MalformedMethod.java"); //$NON-NLS-1$
		configureExpectedProblems(IDelta.METHOD_ELEMENT_TYPE, new String[]{"2.0a", "method()"}); //$NON-NLS-1$ //$NON-NLS-2$
		performCompatibilityTest(filePath, incremental);
	}

	public void testMalformedMethodI() throws Exception {
		xMalformedMethod(true);
	}

	public void testMalformedMethodF() throws Exception {
		xMalformedMethod(false);
	}

	/**
	 * Tests adding a member type with a malformed since tag
	 */
	private void xMalformedMemberType(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("MalformedMemberType.java"); //$NON-NLS-1$
		configureExpectedProblems(IDelta.CLASS_ELEMENT_TYPE, new String[]{"2.0a", "MemberType"}); //$NON-NLS-1$ //$NON-NLS-2$
		performCompatibilityTest(filePath, incremental);
	}

	public void testMalformedMemberTypeI() throws Exception {
		xMalformedMemberType(true);
	}

	public void testMalformedMemberTypeF() throws Exception {
		xMalformedMemberType(false);
	}

	/**
	 * Tests adding a class with a malformed since tag
	 */
	private void xMalformedType(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("MalformedType.java"); //$NON-NLS-1$
		configureExpectedProblems(IDelta.CLASS_ELEMENT_TYPE, new String[]{"2.0a", PACKAGE_PREFIX + "MalformedType"}); //$NON-NLS-1$ //$NON-NLS-2$
		performCreationCompatibilityTest(filePath, incremental);
	}

	public void testMalformedTypeI() throws Exception {
		xMalformedType(true);
	}

	public void testMalformedTypeF() throws Exception {
		xMalformedType(false);
	}

	/**
	 * Tests adding a method with a malformed since tag like abc1.0
	 */
	private void xMalformedTag(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("MalformedTag.java"); //$NON-NLS-1$
		configureExpectedProblems(IDelta.CLASS_ELEMENT_TYPE, new String[]{"abc1.0", "method()"}); //$NON-NLS-1$ //$NON-NLS-2$
		performCompatibilityTest(filePath, incremental);
	}

	public void testMalformedTagI() throws Exception {
		xMalformedTag(true);
	}

	public void testMalformedTagF() throws Exception {
		xMalformedTag(false);
	}

	/**
	 * Tests valid since tag expressions
	 */
	private void xValidSinceTags(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ValidSinceTags.java"); //$NON-NLS-1$
		// no problems expected
		performCompatibilityTest(filePath, incremental);
	}

	public void testValidSinceTagsI() throws Exception {
		xValidSinceTags(true);
	}

	public void testValidSinceTagsF() throws Exception {
		xValidSinceTags(false);
	}
}
