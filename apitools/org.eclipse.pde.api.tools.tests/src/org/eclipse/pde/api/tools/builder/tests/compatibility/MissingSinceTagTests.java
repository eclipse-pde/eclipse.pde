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
import org.junit.jupiter.api.Test;

/**
 * Tests that the builder correctly finds and reports missing since tags
 *
 * @since 1.0
 */
public class MissingSinceTagTests extends SinceTagTest {

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
	 * Tests adding a generic method
	 */
	private void xAddMethod2(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddMethod2.java"); //$NON-NLS-1$
		configureExpectedProblems(IDelta.METHOD_ELEMENT_TYPE, "foo(List<?>)"); //$NON-NLS-1$
		performCompatibilityTest(filePath, incremental);
	}

	@Test

	public void testAddMethod2I() throws Exception {
		xAddMethod2(true);
	}

	@Test

	public void testAddMethod2F() throws Exception {
		xAddMethod2(false);
	}
	/**
	 * Tests adding a non-visible method
	 */
	private void xAddNonVisibleMethod2(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddNonVisibleMethod2.java"); //$NON-NLS-1$
		configureExpectedProblems(IDelta.METHOD_ELEMENT_TYPE, "method()"); //$NON-NLS-1$
		performCompatibilityTest(filePath, incremental);
	}

	@Test

	public void testAddNonVisibleMethod2I() throws Exception {
		xAddNonVisibleMethod2(true);
	}

	@Test

	public void testAddNonVisibleMethod2F() throws Exception {
		xAddNonVisibleMethod2(false);
	}

	/**
	 * Tests adding a field
	 */
	private void xAddField(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddField.java"); //$NON-NLS-1$
		configureExpectedProblems(IDelta.FIELD_ELEMENT_TYPE, "FIELD"); //$NON-NLS-1$
		performCompatibilityTest(filePath, incremental);
	}

	@Test

	public void testAddFieldI() throws Exception {
		xAddField(true);
	}

	@Test

	public void testAddFieldF() throws Exception {
		xAddField(false);
	}

	/**
	 * Tests adding a private field
	 */
	private void xAddPrivateField(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddPrivateField.java"); //$NON-NLS-1$
		// no problems expected
		performCompatibilityTest(filePath, incremental);
	}

	@Test

	public void testPrivateAddFieldI() throws Exception {
		xAddPrivateField(true);
	}

	@Test

	public void testPrivateAddFieldF() throws Exception {
		xAddPrivateField(false);
	}

	/**
	 * Tests adding a member type
	 */
	private void xAddMemberType(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddMemberType.java"); //$NON-NLS-1$
		configureExpectedProblems(IDelta.CLASS_ELEMENT_TYPE, "MemberType"); //$NON-NLS-1$
		performCompatibilityTest(filePath, incremental);
	}

	@Test

	public void testAddMemberTypeI() throws Exception {
		xAddMemberType(true);
	}

	@Test

	public void testAddMemberTypeF() throws Exception {
		xAddMemberType(false);
	}

	/**
	 * Tests adding a method
	 */
	private void xAddMethod(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddMethod.java"); //$NON-NLS-1$
		configureExpectedProblems(IDelta.METHOD_ELEMENT_TYPE, "method()"); //$NON-NLS-1$
		performCompatibilityTest(filePath, incremental);
	}

	@Test

	public void testAddMethodI() throws Exception {
		xAddMethod(true);
	}

	@Test

	public void testAddMethodF() throws Exception {
		xAddMethod(false);
	}

	/**
	 * Tests adding a method that is part of a newly implemented interface
	 */
	private void xAddNewInterfaceMethod(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddNewInterfaceMethod.java"); //$NON-NLS-1$
		// configureExpectedProblems(IDelta.METHOD_ELEMENT_TYPE, "methodA()");
		// //$NON-NLS-1$
		performCompatibilityTest(filePath, incremental);
	}

	@Test

	public void testAddNewInterfaceMethodI() throws Exception {
		xAddNewInterfaceMethod(true);
	}

	@Test

	public void testAddNewInterfaceMethodF() throws Exception {
		xAddNewInterfaceMethod(false);
	}

	/**
	 * Tests adding a method inherited/overridden from a super type. There should be no since tag
	 * required.
	 */
	private void xAddInheritedMethod(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddInheritedMethod.java"); //$NON-NLS-1$
		// no problem expected
		performCompatibilityTest(filePath, incremental);
	}

	@Test

	public void testAddInheritedMethodI() throws Exception {
		xAddInheritedMethod(true);
	}

	@Test

	public void testAddInheritedMethodF() throws Exception {
		xAddInheritedMethod(false);
	}

	/**
	 * Tests that @noreference should not exclude all overriding methods from
	 * API change analysis - See bug 507701
	 */
	private void xAddInheritedMethod2(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddInheritedMethod2.java"); //$NON-NLS-1$
		// expect a since tag
		configureExpectedProblems(IDelta.METHOD_ELEMENT_TYPE, "newMethod()"); //$NON-NLS-1$
		performCompatibilityTest(filePath, incremental);
	}

	@Test

	public void testAddInheritedMethodI2() throws Exception {
		xAddInheritedMethod2(true);
	}

	@Test

	public void testAddInheritedMethodF2() throws Exception {
		xAddInheritedMethod2(false);
	}

	/**
	 * Tests adding a non-visible method
	 */
	private void xAddNonVisibleMethod(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddNonVisibleMethod.java"); //$NON-NLS-1$
		// no problems expected
		performCompatibilityTest(filePath, incremental);
	}

	@Test

	public void testAddNonVisibleMethodI() throws Exception {
		xAddNonVisibleMethod(true);
	}

	@Test

	public void testAddNonVisibleMethodF() throws Exception {
		xAddNonVisibleMethod(false);
	}

	/**
	 * Tests adding a class
	 */
	private void xAddType(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddType.java"); //$NON-NLS-1$
		configureExpectedProblems(IDelta.CLASS_ELEMENT_TYPE, PACKAGE_PREFIX + "AddType"); //$NON-NLS-1$
		performCreationCompatibilityTest(filePath, incremental);
	}

	@Test

	public void testAddTypeI() throws Exception {
		xAddType(true);
	}

	@Test

	public void testAddTypeF() throws Exception {
		xAddType(false);
	}
}
