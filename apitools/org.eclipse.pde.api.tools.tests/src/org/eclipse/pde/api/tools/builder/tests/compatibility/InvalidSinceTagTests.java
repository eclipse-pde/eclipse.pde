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
package org.eclipse.pde.api.tools.builder.tests.compatibility;

import junit.framework.Test;

import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

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
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("InvalidField.java");
		configureExpectedProblems(IDelta.FIELD_ELEMENT_TYPE, new String[]{"3.0", "1.0", "FIELD"});
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
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("InvalidMethod.java");
		configureExpectedProblems(IDelta.METHOD_ELEMENT_TYPE, new String[]{"3.0", "1.0", "method()"});
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
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("InvalidMemberType.java");
		configureExpectedProblems(IDelta.CLASS_ELEMENT_TYPE, new String[]{"3.0", "1.0", "MemberType"});
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
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("InvalidType.java");
		configureExpectedProblems(IDelta.CLASS_ELEMENT_TYPE, new String[]{"3.0", "1.0", PACKAGE_PREFIX + "InvalidType"});
		performCreationCompatibilityTest(filePath, incremental);
	}
	
	public void testInvalidTypeI() throws Exception {
		xInvalidType(true);
	}
	
	public void testInvalidTypeF() throws Exception {
		xInvalidType(false);
	}	
}
