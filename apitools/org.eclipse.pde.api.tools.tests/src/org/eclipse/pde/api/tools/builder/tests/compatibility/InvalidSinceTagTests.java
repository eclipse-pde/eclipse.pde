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
package org.eclipse.pde.api.tools.builder.tests.compatibility;

import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

import junit.framework.Test;

/**
 * Tests that the builder correctly finds and reports missing since tags
 *
 * @since 1.0
 */
public class InvalidSinceTagTests extends SinceTagTest {


	/**
	 * Constructor
	 * @param name
	 */
	public InvalidSinceTagTests(String name) {
		super(name);
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(InvalidSinceTagTests.class);
	}

	protected void configureExpectedProblems(int elementType, String[] messageArgs) {
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
					IApiProblem.CATEGORY_SINCETAGS,
					elementType,
					IApiProblem.SINCE_TAG_INVALID,
					0)
			};
			setExpectedProblemIds(ids);
			String[][] args = new String[1][];
			args[0] = messageArgs;
			setExpectedMessageArgs(args);
	}

	/**
	 * Tests adding a field with a wrong since tag
	 */
	private void xInvalidField(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("InvalidField.java"); //$NON-NLS-1$
		configureExpectedProblems(IDelta.FIELD_ELEMENT_TYPE, new String[]{"3.0", "1.0", "FIELD"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		performCompatibilityTest(filePath, incremental);
	}

	public void testInvalidFieldI() throws Exception {
		xInvalidField(true);
	}

	public void testInvalidFieldF() throws Exception {
		xInvalidField(false);
	}

	/**
	 * Tests adding a method with a wrong since tag
	 */
	private void xInvalidMethod(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("InvalidMethod.java"); //$NON-NLS-1$
		configureExpectedProblems(IDelta.METHOD_ELEMENT_TYPE, new String[]{"3.0", "1.0", "method()"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		performCompatibilityTest(filePath, incremental);
	}

	public void testInvalidMethodI() throws Exception {
		xInvalidMethod(true);
	}

	public void testInvalidMethodF() throws Exception {
		xInvalidMethod(false);
	}

	/**
	 * Tests adding a member type with a wrong since tag
	 */
	private void xInvalidMemberType(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("InvalidMemberType.java"); //$NON-NLS-1$
		configureExpectedProblems(IDelta.CLASS_ELEMENT_TYPE, new String[]{"3.0", "1.0", "MemberType"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		performCompatibilityTest(filePath, incremental);
	}

	public void testInvalidMemberTypeI() throws Exception {
		xInvalidMemberType(true);
	}

	public void testInvalidMemberTypeF() throws Exception {
		xInvalidMemberType(false);
	}

	/**
	 * Tests adding a class with a wrong since tag
	 */
	private void xInvalidType(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("InvalidType.java"); //$NON-NLS-1$
		configureExpectedProblems(IDelta.CLASS_ELEMENT_TYPE, new String[]{"3.0", "1.0", PACKAGE_PREFIX + "InvalidType"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		performCreationCompatibilityTest(filePath, incremental);
	}

	public void testInvalidTypeI() throws Exception {
		xInvalidType(true);
	}

	public void testInvalidTypeF() throws Exception {
		xInvalidType(false);
	}
}
