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
 * Tests that the builder correctly finds and reports constructor
 * compatibility problems
 *
 * @since 1.0
 */
public class ConstructorCompatibilityTests extends CompatibilityTest {

	/**
	 * Workspace relative path classes in bundle/project A
	 */
	protected static IPath WORKSPACE_CLASSES_PACKAGE_A = new Path("bundle.a/src/a/constructors"); //$NON-NLS-1$

	/**
	 * Package prefix for test classes
	 */
	protected static String PACKAGE_PREFIX = "a.constructors."; //$NON-NLS-1$

	public ConstructorCompatibilityTests(String name) {
		super(name);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("constructors"); //$NON-NLS-1$
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(ConstructorCompatibilityTests.class);
	}

	@Override
	protected int getDefaultProblemId() {
		return 0;
	}

	/**
	 * Returns a problem id for a compatibility change to a class based on the
	 * specified flags.
	 *
	 * @param flags
	 * @return problem id
	 */
	protected int getChangedProblemId(int flags) {
		return ApiProblemFactory.createProblemId(
				IApiProblem.CATEGORY_COMPATIBILITY,
				IDelta.CONSTRUCTOR_ELEMENT_TYPE,
				IDelta.CHANGED,
				flags);
	}

	@Override
	protected String getTestingProjectName() {
		return "constcompat"; //$NON-NLS-1$
	}

	/**
	 * Tests changing a protected method to package protected
	 */
	private void xProtectedToPackage(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ProtectedToPackage.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getChangedProblemId(IDelta.DECREASE_ACCESS)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "ProtectedToPackage", "ProtectedToPackage()"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testProtectedToPackageI() throws Exception {
		xProtectedToPackage(true);
	}

	public void testProtectedToPackageF() throws Exception {
		xProtectedToPackage(false);
	}

	/**
	 * Tests changing a protected method to package protected when no-reference
	 */
	private void xProtectedToPackageNoReference(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ProtectedToPackageNoReference.java"); //$NON-NLS-1$
		// no problem expected
		performCompatibilityTest(filePath, incremental);
	}

	public void testProtectedToPackageNoReferenceI() throws Exception {
		xProtectedToPackageNoReference(true);
	}

	public void testProtectedToPackageNoReferenceF() throws Exception {
		xProtectedToPackageNoReference(false);
	}

	/**
	 * Tests changing a protected method to private
	 */
	private void xProtectedToPrivate(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ProtectedToPrivate.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getChangedProblemId(IDelta.DECREASE_ACCESS)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "ProtectedToPrivate", "ProtectedToPrivate()"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testProtectedToPrivateI() throws Exception {
		xProtectedToPrivate(true);
	}

	public void testProtectedToPrivateF() throws Exception {
		xProtectedToPrivate(false);
	}

	/**
	 * Tests changing a protected method to private method in a no-extend class
	 */
	private void xProtectedToPrivateNoExtend(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ProtectedToPrivateNoExtend.java"); //$NON-NLS-1$
		// no expected errors
		performCompatibilityTest(filePath, incremental);
	}

	public void testProtectedToPrivateNoExtendI() throws Exception {
		xProtectedToPrivateNoExtend(true);
	}

	public void testProtectedToPrivateNoExtendF() throws Exception {
		xProtectedToPrivateNoExtend(false);
	}

	/**
	 * Tests changing a public method to package
	 */
	private void xPublicToPackage(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("PublicToPackage.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getChangedProblemId(IDelta.DECREASE_ACCESS)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "PublicToPackage", "PublicToPackage()"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testPublicToPackageI() throws Exception {
		xPublicToPackage(true);
	}

	public void testPublicToPackageF() throws Exception {
		xPublicToPackage(false);
	}

	/**
	 * Tests changing a public method to private
	 */
	private void xPublicToPrivate(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("PublicToPrivate.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getChangedProblemId(IDelta.DECREASE_ACCESS)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "PublicToPrivate", "PublicToPrivate()"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testPublicToPrivateI() throws Exception {
		xPublicToPrivate(true);
	}

	public void testPublicToPrivateF() throws Exception {
		xPublicToPrivate(false);
	}

	/**
	 * Tests changing a public method to private
	 */
	private void xPublicToPrivateNoReference(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("PublicToPrivateNoReference.java"); //$NON-NLS-1$
		// no problem expected
		performCompatibilityTest(filePath, incremental);
	}

	public void testPublicToPrivateNoReferenceI() throws Exception {
		xPublicToPrivateNoReference(true);
	}

	public void testPublicToPrivateNoReferenceF() throws Exception {
		xPublicToPrivateNoReference(false);
	}

	/**
	 * Tests changing a public method to protected
	 */
	private void xPublicToProtected(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("PublicToProtected.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getChangedProblemId(IDelta.DECREASE_ACCESS)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "PublicToProtected", "PublicToProtected()"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testPublicToProtectedI() throws Exception {
		xPublicToProtected(true);
	}

	public void testPublicToProtectedF() throws Exception {
		xPublicToProtected(false);
	}

	/**
	 * Tests adding a type parameter to a constructor
	 */
	private void xAddTypeParameter(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddTypeParameter.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			ApiProblemFactory.createProblemId(
				IApiProblem.CATEGORY_COMPATIBILITY,
				IDelta.CONSTRUCTOR_ELEMENT_TYPE,
				IDelta.ADDED,
				IDelta.TYPE_PARAMETER)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "AddTypeParameter.AddTypeParameter(T)", "U"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddTypeParameterI() throws Exception {
		xAddTypeParameter(true);
	}

	public void testAddTypeParameterF() throws Exception {
		xAddTypeParameter(false);
	}

	/**
	 * Tests removing a type parameter from a constructor
	 */
	private void xRemoveTypeParameter(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveTypeParameter.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			ApiProblemFactory.createProblemId(
				IApiProblem.CATEGORY_COMPATIBILITY,
				IDelta.CONSTRUCTOR_ELEMENT_TYPE,
				IDelta.REMOVED,
				IDelta.TYPE_PARAMETER)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemoveTypeParameter.RemoveTypeParameter(T)", "U"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemoveTypeParameterI() throws Exception {
		xRemoveTypeParameter(true);
	}

	public void testRemoveTypeParameterF() throws Exception {
		xRemoveTypeParameter(false);
	}

	/**
	 * Tests converting variable arguments to an array
	 */
	private void xVarArgsToArray(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("VarArgsToArray.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			ApiProblemFactory.createProblemId(
				IApiProblem.CATEGORY_COMPATIBILITY,
				IDelta.CONSTRUCTOR_ELEMENT_TYPE,
				IDelta.CHANGED,
				IDelta.VARARGS_TO_ARRAY)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "VarArgsToArray", "VarArgsToArray(int, int[])"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testVarArgsToArrayI() throws Exception {
		xVarArgsToArray(true);
	}

	public void testVarArgsToArrayF() throws Exception {
		xVarArgsToArray(false);
	}

	/**
	 * Tests converting an array to variable arguments
	 */
	private void xArrayToVarArgs(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ArrayToVarArgs.java"); //$NON-NLS-1$
		// no problems
		performCompatibilityTest(filePath, incremental);
	}

	public void testArrayToVarArgsI() throws Exception {
		xArrayToVarArgs(true);
	}

	public void testArrayToVarArgsF() throws Exception {
		xArrayToVarArgs(false);
	}

	/**
	 * Tests changing a public method to protected in @noextend class
	 */
	private void xPublicToProtected2(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("PublicToProtected2.java"); //$NON-NLS-1$
		int[] ids = new int[] { getChangedProblemId(IDelta.DECREASE_ACCESS) };
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[] { PACKAGE_PREFIX + "PublicToProtected2", "PublicToProtected2()" }; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testPublicToProtected2I() throws Exception {
		xPublicToProtected2(true);
	}

	public void testPublicToProtected2F() throws Exception {
		xPublicToProtected2(false);
	}

	/**
	 * Tests changing a public method to package in @noextend class
	 */
	private void xPublicToPackage2(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("PublicToPackage2.java"); //$NON-NLS-1$
		int[] ids = new int[] { getChangedProblemId(IDelta.DECREASE_ACCESS) };
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[] { PACKAGE_PREFIX + "PublicToPackage2", "PublicToPackage2()" }; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testPublicToPackage2I() throws Exception {
		xPublicToPackage2(true);
	}

	public void testPublicToPackage2F() throws Exception {
		xPublicToPackage2(false);
	}

	/**
	 * Tests changing a public method to private in @noextend class
	 */
	private void xPublicToPrivate2(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("PublicToPrivate2.java"); //$NON-NLS-1$
		int[] ids = new int[] { getChangedProblemId(IDelta.DECREASE_ACCESS) };
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[] { PACKAGE_PREFIX + "PublicToPrivate2", "PublicToPrivate2()" }; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testPublicToPrivate2I() throws Exception {
		xPublicToPrivate2(true);
	}

	public void testPublicToPrivate2F() throws Exception {
		xPublicToPrivate2(false);
	}

}
