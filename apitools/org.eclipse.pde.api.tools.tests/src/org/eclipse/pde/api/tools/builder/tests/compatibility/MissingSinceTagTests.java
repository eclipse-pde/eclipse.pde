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
public class MissingSinceTagTests extends SinceTagTest {
	

	/**
	 * Constructor
	 * @param name
	 */
	public MissingSinceTagTests(String name) {
		super(name);
	}
	
	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(MissingSinceTagTests.class);
	}
	
	protected void configureExpectedProblems(int elementType, String messageArg) {
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
					IApiProblem.CATEGORY_SINCETAGS,
					elementType,
					IApiProblem.SINCE_TAG_MISSING,
					0)
			};
			setExpectedProblemIds(ids);
			String[][] args = new String[1][];
			args[0] = new String[]{messageArg};
			setExpectedMessageArgs(args);		
	}
	/**
	 * Tests adding a non-visible method
	 */
	private void xAddNonVisibleMethod2(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddNonVisibleMethod2.java");
		configureExpectedProblems(IDelta.METHOD_ELEMENT_TYPE, "method()");
		performCompatibilityTest(filePath, incremental);
	}
	
	public void testAddNonVisibleMethod2I() throws Exception {
		xAddNonVisibleMethod2(true);
	}
	
	public void testAddNonVisibleMethod2F() throws Exception {
		xAddNonVisibleMethod2(false);
	}

	/**
	 * Tests adding a field
	 */
	private void xAddField(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddField.java");
		configureExpectedProblems(IDelta.FIELD_ELEMENT_TYPE, "FIELD");
		performCompatibilityTest(filePath, incremental);
	}
	
	public void testAddFieldI() throws Exception {
		xAddField(true);
	}
	
	public void testAddFieldF() throws Exception {
		xAddField(false);
	}	
	
	/**
	 * Tests adding a private field
	 */
	private void xAddPrivateField(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddPrivateField.java");
		// no problems expected
		performCompatibilityTest(filePath, incremental);
	}
	
	public void testPrivateAddFieldI() throws Exception {
		xAddPrivateField(true);
	}
	
	public void testPrivateAddFieldF() throws Exception {
		xAddPrivateField(false);
	}	
	
	/**
	 * Tests adding a member type
	 */
	private void xAddMemberType(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddMemberType.java");
		configureExpectedProblems(IDelta.CLASS_ELEMENT_TYPE, "MemberType");
		performCompatibilityTest(filePath, incremental);
	}
	
	public void testAddMemberTypeI() throws Exception {
		xAddMemberType(true);
	}
	
	public void testAddMemberTypeF() throws Exception {
		xAddMemberType(false);
	}
	
	/**
	 * Tests adding a method
	 */
	private void xAddMethod(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddMethod.java");
		configureExpectedProblems(IDelta.METHOD_ELEMENT_TYPE, "method()");
		performCompatibilityTest(filePath, incremental);
	}
	
	public void testAddMethodI() throws Exception {
		xAddMethod(true);
	}
	
	public void testAddMethodF() throws Exception {
		xAddMethod(false);
	}	
	
	/**
	 * Tests adding a method that is part of a newly implemented interface
	 */
	private void xAddNewInterfaceMethod(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddNewInterfaceMethod.java");
		configureExpectedProblems(IDelta.METHOD_ELEMENT_TYPE, "methodA()");
		performCompatibilityTest(filePath, incremental);
	}
	
	public void testAddNewInterfaceMethodI() throws Exception {
		xAddNewInterfaceMethod(true);
	}
	
	public void testAddNewInterfaceMethodF() throws Exception {
		xAddNewInterfaceMethod(false);
	}	
	
	/**
	 * Tests adding a method inherited/overridden from a super type. There should be no since tag
	 * required.
	 */
	private void xAddInheritedMethod(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddInheritedMethod.java");
		// no problem expected
		performCompatibilityTest(filePath, incremental);
	}
	
	public void testAddInheritedMethodI() throws Exception {
		xAddInheritedMethod(true);
	}
	
	public void testAddInheritedMethodF() throws Exception {
		xAddInheritedMethod(false);
	}
	
	/**
	 * Tests adding a non-visible method
	 */
	private void xAddNonVisibleMethod(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddNonVisibleMethod.java");
		// no problems expected
		performCompatibilityTest(filePath, incremental);
	}
	
	public void testAddNonVisibleMethodI() throws Exception {
		xAddNonVisibleMethod(true);
	}
	
	public void testAddNonVisibleMethodF() throws Exception {
		xAddNonVisibleMethod(false);
	}	
	
	/**
	 * Tests adding a class
	 */
	private void xAddType(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddType.java");
		configureExpectedProblems(IDelta.CLASS_ELEMENT_TYPE, PACKAGE_PREFIX + "AddType");
		performCreationCompatibilityTest(filePath, incremental);
	}
	
	public void testAddTypeI() throws Exception {
		xAddType(true);
	}
	
	public void testAddTypeF() throws Exception {
		xAddType(false);
	}		
}
