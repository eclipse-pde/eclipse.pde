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
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

import junit.framework.Test;

/**
 * Tests that the builder correctly reports compatibility problems
 * for classes related to fields.
 *
 * @since 1.0
 */
public class ClassCompatibilityFieldTests extends ClassCompatibilityTests {

	/**
	 * Workspace relative path classes in bundle/project A
	 */
	protected static IPath WORKSPACE_CLASSES_PACKAGE_A = new Path("bundle.a/src/a/classes/fields"); //$NON-NLS-1$

	/**
	 * Package prefix for test classes
	 */
	protected static String PACKAGE_PREFIX = "a.classes.fields."; //$NON-NLS-1$

	public ClassCompatibilityFieldTests(String name) {
		super(name);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("fields"); //$NON-NLS-1$
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(ClassCompatibilityFieldTests.class);
	}

	@Override
	protected int getDefaultProblemId() {
		return ApiProblemFactory.createProblemId(
				IApiProblem.CATEGORY_COMPATIBILITY,
				IDelta.CLASS_ELEMENT_TYPE,
				IDelta.REMOVED,
				IDelta.FIELD);
	}

	@Override
	protected String getTestingProjectName() {
		return "classcompat"; //$NON-NLS-1$
	}

	/**
	 * Tests the removal of a public field from an API class.
	 */
	private void xRemovePublicField(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemovePublicField.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getDefaultProblemId()
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemovePublicField", "PUBLIC_FIELD"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemovePublicFieldI() throws Exception {
		xRemovePublicField(true);
	}

	public void testRemovePublicFieldF() throws Exception {
		xRemovePublicField(false);
	}

	/**
	 * Tests the removal of 2 public fields from an API class - incremental.
	 */
	public void testRemoveTwoPublicFieldsI() throws Exception {
		xRemoveTwoPublicFields(true);
	}

	/**
	 * Tests the removal of 2 public methods from an API class - full.
	 */
	public void testRemoveTwoPublicFieldsF() throws Exception {
		xRemoveTwoPublicFields(false);
	}

	/**
	 * Tests the removal of a public method from an API class - incremental.
	 */
	private void xRemoveTwoPublicFields(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveTwoPublicFields.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getDefaultProblemId(),
			getDefaultProblemId()
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[2][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemoveTwoPublicFields", "PUBLIC_FIELD1"}; //$NON-NLS-1$ //$NON-NLS-2$
		args[1] = new String[]{PACKAGE_PREFIX + "RemoveTwoPublicFields", "PUBLIC_FIELD2"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	/**
	 * Tests the removal of a protected field from an API class.
	 */
	private void xRemoveProtectedField(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveProtectedField.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getDefaultProblemId()
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemoveProtectedField", "PROTECTED_FIELD"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemoveProtectedFieldI() throws Exception {
		xRemoveProtectedField(true);
	}

	public void testRemoveProtectedFieldF() throws Exception {
		xRemoveProtectedField(false);
	}

	/**
	 * Tests the removal of a private field from an API class.
	 */
	private void xRemovePrivateField(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemovePrivateField.java"); //$NON-NLS-1$
		// there are no expected problems
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemovePrivateFieldI() throws Exception {
		xRemovePrivateField(true);
	}

	public void testRemovePrivateFieldF() throws Exception {
		xRemovePrivateField(false);
	}

	/**
	 * Tests the removal of a package protected field from an API class.
	 */
	private void xRemovePackageField(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemovePackageField.java"); //$NON-NLS-1$
		// there are no expected problems
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemovePackageFieldI() throws Exception {
		xRemovePackageField(true);
	}

	public void testRemovePackageFieldF() throws Exception {
		xRemovePackageField(false);
	}

	/**
	 * Tests the removal of a public field from an API class annotated as noextend - incremental.
	 */
	private void xRemovePublicFieldNoExtend(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemovePublicFieldNoExtend.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getDefaultProblemId()
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemovePublicFieldNoExtend", "PUBLIC_FIELD"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemovePublicFieldNoExtendI() throws Exception {
		xRemovePublicFieldNoExtend(true);
	}

	public void testRemovePublicFieldNoExtendF() throws Exception {
		xRemovePublicFieldNoExtend(false);
	}

	/**
	 * Tests the removal of a protected field from an API class annotated as noextend.
	 */
	private void xRemoveProtectedFieldNoExtend(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveProtectedFieldNoExtend.java"); //$NON-NLS-1$
		// no problems expected since the method is not accessible
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemoveProtectedFieldNoExtendI() throws Exception {
		xRemoveProtectedFieldNoExtend(true);
	}

	public void testRemoveProtectedFieldNoExtendF() throws Exception {
		xRemoveProtectedFieldNoExtend(false);
	}

	/**
	 * Tests the removal of a public field from an API class annotated as noinstantiate - incremental.
	 */
	private void xRemovePublicFieldNoInstantiate(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemovePublicFieldNoInstantiate.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getDefaultProblemId()
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemovePublicFieldNoInstantiate", "PUBLIC_FIELD"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemovePublicFieldNoInstantiateI() throws Exception {
		xRemovePublicFieldNoInstantiate(true);
	}

	public void testRemovePublicFieldNoInstantiateF() throws Exception {
		xRemovePublicFieldNoInstantiate(false);
	}

	/**
	 * Tests the removal of a protected field from an API class annotated as noinstantiate.
	 */
	private void xRemoveProtectedFieldNoInstantiate(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveProtectedFieldNoInstantiate.java"); //$NON-NLS-1$
		int[] ids = new int[] {
				getDefaultProblemId()
			};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemoveProtectedFieldNoInstantiate", "PROTECTED_FIELD"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemoveProtectedFieldNoInstantiateI() throws Exception {
		xRemoveProtectedFieldNoInstantiate(true);
	}

	public void testRemoveProtectedFieldNoInstantiateF() throws Exception {
		xRemoveProtectedFieldNoInstantiate(false);
	}

	/**
	 * Tests the removal of a public field from an API class annotated as
	 * noextend and noinstantiate.
	 */
	private void xRemovePublicFieldNoExtendNoInstatiate(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemovePublicFieldNoExtendNoInstantiate.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getDefaultProblemId()
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemovePublicFieldNoExtendNoInstantiate", "PUBLIC_FIELD"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemovePublicFieldNoExtendNoInstantiateI() throws Exception {
		xRemovePublicFieldNoExtendNoInstatiate(true);
	}

	public void testRemovePublicFieldNoExtendNoInstantiateF() throws Exception {
		xRemovePublicFieldNoExtendNoInstatiate(false);
	}

	/**
	 * Tests the removal of a protected field from an API class annotated as
	 * noextend and noinstantiate.
	 */
	private void xRemoveProtectedFieldNoExtendNoInstatiate(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveProtectedFieldNoExtendNoInstantiate.java"); //$NON-NLS-1$
		// no problems expected due to noextend
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemoveProtectedFieldNoExtendNoInstantiateI() throws Exception {
		xRemoveProtectedFieldNoExtendNoInstatiate(true);
	}

	public void testRemoveProtectedFieldNoExtendNoInstantiateF() throws Exception {
		xRemoveProtectedFieldNoExtendNoInstatiate(false);
	}

	/**
	 * Tests the removal of a public field from an API class tagged noreference.
	 */
	private void xRemovePublicFieldNoReference(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemovePublicFieldNoReference.java"); //$NON-NLS-1$
		// no problems since no references allowed
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemovePublicFieldNoReferenceI() throws Exception {
		xRemovePublicFieldNoReference(true);
	}

	public void testRemovePublicFieldNoReferencF() throws Exception {
		xRemovePublicFieldNoReference(false);
	}

	/**
	 * Tests the removal of a protected field from an API class tagged noreference.
	 */
	private void xRemoveProtectedFieldNoReference(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveProtectedFieldNoReference.java"); //$NON-NLS-1$
		// no problems since no references allowed
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemoveProtectedFieldNoReferenceI() throws Exception {
		xRemoveProtectedFieldNoReference(true);
	}

	public void testRemoveProtectedFieldNoReferencF() throws Exception {
		xRemoveProtectedFieldNoReference(false);
	}

	/**
	 * Tests the addition of a private field in an API class.
	 */
	private void xAddPrivateField(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddPrivateField.java"); //$NON-NLS-1$
		// there are no expected problems
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddPrivateFieldI() throws Exception {
		xAddPrivateField(true);
	}

	public void testAddPrivateFieldF() throws Exception {
		xAddPrivateField(false);
	}

	/**
	 * Tests the addition of a protected field in an API class.
	 */
	private void xAddProtectedField(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddProtectedField.java"); //$NON-NLS-1$
		// there is 1 expected problems
		int[] ids = new int[] { ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY,
				IDelta.CLASS_ELEMENT_TYPE, IDelta.ADDED, IDelta.FIELD) };
		setExpectedProblemIds(ids);
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddProtectedFieldI() throws Exception {
		xAddProtectedField(true);
	}

	public void testAddProtectedFieldF() throws Exception {
		xAddProtectedField(false);
	}

	/**
	 * Tests the addition of a public field in an API class.
	 */
	private void xAddPublicField(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddPublicField.java"); //$NON-NLS-1$
		// there is 1 expected problem
		int[] ids = new int[] { ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY,
				IDelta.CLASS_ELEMENT_TYPE, IDelta.ADDED, IDelta.FIELD) };
		setExpectedProblemIds(ids);
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddPublicFieldI() throws Exception {
		xAddPublicField(true);
	}

	public void testAddPublicFieldF() throws Exception {
		xAddPublicField(false);
	}
}
