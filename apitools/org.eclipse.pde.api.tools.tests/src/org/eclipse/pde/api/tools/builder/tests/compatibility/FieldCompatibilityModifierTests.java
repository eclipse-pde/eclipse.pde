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
 * related class hierarchies.
 *
 * @since 1.0
 */
public class FieldCompatibilityModifierTests extends FieldCompatibilityTests {

	/**
	 * Workspace relative path classes in bundle/project A
	 */
	protected static IPath WORKSPACE_CLASSES_PACKAGE_A = new Path("bundle.a/src/a/fields/modifiers"); //$NON-NLS-1$

	/**
	 * Package prefix for test classes
	 */
	protected static String PACKAGE_PREFIX = "a.fields.modifiers."; //$NON-NLS-1$

	public FieldCompatibilityModifierTests(String name) {
		super(name);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("modifiers"); //$NON-NLS-1$
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(FieldCompatibilityModifierTests.class);
	}

	@Override
	protected String getTestingProjectName() {
		return "classcompat"; //$NON-NLS-1$
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
				IDelta.FIELD_ELEMENT_TYPE,
				IDelta.CHANGED,
				flags);
	}

	/**
	 * Tests making a non-final field final
	 */
	private void xAddFinal(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddFinal.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getChangedProblemId(IDelta.NON_FINAL_TO_FINAL)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "AddFinal", "ADD_FINAL"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddFinalI() throws Exception {
		xAddFinal(true);
	}

	public void testAddFinalF() throws Exception {
		xAddFinal(false);
	}

	/**
	 * Tests making a non-final no-reference field final
	 */
	private void xAddFinalNoReference(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddFinalNoReference.java"); //$NON-NLS-1$
		// there is 1 expected problem
		int[] ids = new int[] { ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY,
				IDelta.CLASS_ELEMENT_TYPE, IDelta.ADDED, IDelta.FIELD) };
		setExpectedProblemIds(ids);
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddFinalNoReferenceI() throws Exception {
		xAddFinalNoReference(true);
	}

	public void testAddFinalNoReferenceF() throws Exception {
		xAddFinalNoReference(false);
	}

	/**
	 * Tests making a non-final field final
	 */
	private void xAddFinalOnStatic(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddFinalOnStatic.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getChangedProblemId(IDelta.NON_FINAL_TO_FINAL)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "AddFinalOnStatic", "ADD_FINAL"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddFinalOnStaticI() throws Exception {
		xAddFinalOnStatic(true);
	}

	public void testAddFinalOnStaticF() throws Exception {
		xAddFinalOnStatic(false);
	}

	/**
	 * Tests making a non-final no-reference field final
	 */
	private void xAddFinalOnStaticNoReference(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddFinalOnStaticNoReference.java"); //$NON-NLS-1$
		// there is 1 expected problem
		int[] ids = new int[] { ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY,
				IDelta.CLASS_ELEMENT_TYPE, IDelta.ADDED, IDelta.FIELD) };
		setExpectedProblemIds(ids);
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddFinalOnStaticNoReferenceI() throws Exception {
		xAddFinalOnStaticNoReference(true);
	}

	public void testAddFinalOnStaticNoReferenceF() throws Exception {
		xAddFinalOnStaticNoReference(false);
	}

	/**
	 * Tests making a non-final field final
	 */
	private void xRemoveFinalOnConstant(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveFinalOnConstant.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getChangedProblemId(IDelta.FINAL_TO_NON_FINAL_STATIC_CONSTANT)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemoveFinalOnConstant", "CONSTANT"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemoveFinalOnConstantI() throws Exception {
		xRemoveFinalOnConstant(true);
	}

	public void testRemoveFinalOnConstantF() throws Exception {
		xRemoveFinalOnConstant(false);
	}

	/**
	 * Tests making a non-static field static
	 */
	private void xAddStatic(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddStatic.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getChangedProblemId(IDelta.NON_STATIC_TO_STATIC)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "AddStatic", "ADD_STATIC"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddStaticI() throws Exception {
		xAddStatic(true);
	}

	public void testAddStaticF() throws Exception {
		xAddStatic(false);
	}

	/**
	 * Tests making a non-static no-reference field static
	 */
	private void xAddStaticNoReference(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddStaticNoReference.java"); //$NON-NLS-1$
		// expecting no errors
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddStaticNoReferenceI() throws Exception {
		xAddStaticNoReference(true);
	}

	public void testAddStaticNoReferenceF() throws Exception {
		xAddStaticNoReference(false);
	}

	/**
	 * Tests making a static field non-static
	 */
	private void xRemoveStatic(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveStatic.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getChangedProblemId(IDelta.STATIC_TO_NON_STATIC)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemoveStatic", "REMOVE_STATIC"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemoveStaticI() throws Exception {
		xRemoveStatic(true);
	}

	public void testRemoveStaticF() throws Exception {
		xRemoveStatic(false);
	}

	/**
	 * Tests making a static no-reference field non-static
	 */
	private void xRemoveStaticNoReference(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveStaticNoReference.java"); //$NON-NLS-1$
		// expecting no errors
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemoveStaticNoReferenceI() throws Exception {
		xRemoveStaticNoReference(true);
	}

	public void testRemoveStaticNoReferenceF() throws Exception {
		xRemoveStaticNoReference(false);
	}

	/**
	 * Tests changing a protected field to package protected
	 */
	private void xProtectedToPackage(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ProtectedToPackage.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getChangedProblemId(IDelta.DECREASE_ACCESS)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "ProtectedToPackage", "FIELD"}; //$NON-NLS-1$ //$NON-NLS-2$
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
	 * Tests changing a protected field to package protected when no-reference
	 */
	private void xProtectedToPackageNoReference(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ProtectedToPackageNoReference.java"); //$NON-NLS-1$
		// expecting no errors
		performCompatibilityTest(filePath, incremental);
	}

	public void testProtectedToPackageNoReferenceI() throws Exception {
		xProtectedToPackageNoReference(true);
	}

	public void testProtectedToPackageNoReferenceF() throws Exception {
		xProtectedToPackageNoReference(false);
	}

	/**
	 * Tests changing a protected field to package protected when no-reference, and remove
	 * the no-reference tag
	 */
	private void xProtectedToPackageRemoveNoReference(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ProtectedToPackageRemoveNoReference.java"); //$NON-NLS-1$
		// expecting no errors
		performCompatibilityTest(filePath, incremental);
	}

	public void testProtectedToPackageRemoveNoReferenceI() throws Exception {
		xProtectedToPackageRemoveNoReference(true);
	}

	public void testProtectedToPackageRemoveNoReferenceF() throws Exception {
		xProtectedToPackageRemoveNoReference(false);
	}

	/**
	 * Tests changing a protected field to private
	 */
	private void xProtectedToPrivate(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ProtectedToPrivate.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getChangedProblemId(IDelta.DECREASE_ACCESS)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "ProtectedToPrivate", "FIELD"}; //$NON-NLS-1$ //$NON-NLS-2$
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
	 * Tests changing a protected field to private field in a no-extend class
	 */
	private void xProtectedToPrivateNoExtend(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ProtectedToPrivateNoExtend.java"); //$NON-NLS-1$
		// expected no error
		performCompatibilityTest(filePath, incremental);
	}

	public void testProtectedToPrivateNoExtendI() throws Exception {
		xProtectedToPrivateNoExtend(true);
	}

	public void testProtectedToPrivateNoExtendF() throws Exception {
		xProtectedToPrivateNoExtend(false);
	}

	/**
	 * Tests changing a public field to package
	 */
	private void xPublicToPackage(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("PublicToPackage.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getChangedProblemId(IDelta.DECREASE_ACCESS)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "PublicToPackage", "FIELD"}; //$NON-NLS-1$ //$NON-NLS-2$
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
	 * Tests changing a public field to private
	 */
	private void xPublicToPrivate(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("PublicToPrivate.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getChangedProblemId(IDelta.DECREASE_ACCESS)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "PublicToPrivate", "FIELD"}; //$NON-NLS-1$ //$NON-NLS-2$
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
	 * Tests changing a public field to private
	 */
	private void xPublicToPrivateNoReference(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("PublicToPrivateNoReference.java"); //$NON-NLS-1$
		// expecting no errors
		performCompatibilityTest(filePath, incremental);
	}

	public void testPublicToPrivateNoReferenceI() throws Exception {
		xPublicToPrivateNoReference(true);
	}

	public void testPublicToPrivateNoReferenceF() throws Exception {
		xPublicToPrivateNoReference(false);
	}

	/**
	 * Tests changing a public field to protected
	 */
	private void xPublicToProtected(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("PublicToProtected.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getChangedProblemId(IDelta.DECREASE_ACCESS)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "PublicToProtected", "FIELD"}; //$NON-NLS-1$ //$NON-NLS-2$
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
	 * Tests changing the value of a constant
	 */
	private void xModifyValue(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ModifyValue.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getChangedProblemId(IDelta.VALUE)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "ModifyValue", "CONSTANT", "VALUE_1"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testModifyValueI() throws Exception {
		xModifyValue(true);
	}

	public void testModifyValueF() throws Exception {
		xModifyValue(false);
	}

	/**
	 * Tests changing the type of a field
	 */
	private void xChangeType(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ChangeType.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getChangedProblemId(IDelta.TYPE)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "ChangeType", "FIELD"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testChangeTypeI() throws Exception {
		xChangeType(true);
	}

	public void testChangeTypeF() throws Exception {
		xChangeType(false);
	}

	/**
	 * Tests changing the type of a protected field with a no-extend class
	 */
	private void xChangeTypeNoExtend(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ChangeTypeNoExtend.java"); //$NON-NLS-1$
		// should be no problems
		performCompatibilityTest(filePath, incremental);
	}

	public void testChangeTypeNoExtendI() throws Exception {
		xChangeTypeNoExtend(true);
	}

	public void testChangeTypeNoExtendF() throws Exception {
		xChangeTypeNoExtend(false);
	}

	/**
	 * Tests changing the type of a protected field annotated no-reference
	 */
	private void xChangeTypeNoReference(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ChangeTypeNoReference.java"); //$NON-NLS-1$
		// expecting no errors
		performCompatibilityTest(filePath, incremental);
	}

	public void testChangeTypeNoReferenceI() throws Exception {
		xChangeTypeNoReference(true);
	}

	public void testChangeTypeNoReferenceF() throws Exception {
		xChangeTypeNoReference(false);
	}

	/**
	 * Tests generalizing the type of a field
	 */
	private void xGeneralizeType(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("GeneralizeType.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getChangedProblemId(IDelta.TYPE)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "GeneralizeType", "FIELD"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testGeneralizeTypeI() throws Exception {
		xGeneralizeType(true);
	}

	public void testGeneralizeTypeF() throws Exception {
		xGeneralizeType(false);
	}

	/**
	 * Tests generalizing the type of a protected field with a no-extend class
	 */
	private void xGeneralizeTypeNoExtend(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("GeneralizeTypeNoExtend.java"); //$NON-NLS-1$
		// should be no problems
		performCompatibilityTest(filePath, incremental);
	}

	public void testGeneralizeTypeNoExtendI() throws Exception {
		xGeneralizeTypeNoExtend(true);
	}

	public void testGeneralizeTypeNoExtendF() throws Exception {
		xGeneralizeTypeNoExtend(false);
	}

	/**
	 * Tests generalizing the type of a protected field annotated no-reference
	 */
	private void xGeneralizeTypeNoReference(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("GeneralizeTypeNoReference.java"); //$NON-NLS-1$
		// expecting no errors
		performCompatibilityTest(filePath, incremental);
	}

	public void testGeneralizeTypeNoReferenceI() throws Exception {
		xGeneralizeTypeNoReference(true);
	}

	public void testGeneralizeTypeNoReferenceF() throws Exception {
		xGeneralizeTypeNoReference(false);
	}

	/**
	 * Tests specializing the type of a field
	 */
	private void xSpecializeType(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("SpecializeType.java"); //$NON-NLS-1$
		// should be a problem - @see bug 245150
		int[] ids = new int[] {
			getChangedProblemId(IDelta.TYPE)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "SpecializeType", "FIELD"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testSpecializeTypeI() throws Exception {
		xSpecializeType(true);
	}

	public void testSpecializeTypeF() throws Exception {
		xSpecializeType(false);
	}

	/**
	 * Tests remove a type argument
	 */
	private void xRemoveTypeArguments(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveTypeArguments.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			ApiProblemFactory.createProblemId(
				IApiProblem.CATEGORY_COMPATIBILITY,
				IDelta.FIELD_ELEMENT_TYPE,
				IDelta.REMOVED,
				IDelta.TYPE_ARGUMENT)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemoveTypeArguments.FIELD", "java.lang.String"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemoveTypeArgumentsI() throws Exception {
		xRemoveTypeArguments(true);
	}

	public void testRemoveTypeArgumentsF() throws Exception {
		xRemoveTypeArguments(false);
	}

	/**
	 * Tests adding a type parameter
	 */
	private void xAddTypeArguments(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddTypeArguments.java"); //$NON-NLS-1$
		// no problems expected
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddTypeArgumentsI() throws Exception {
		xAddTypeArguments(true);
	}

	public void testAddTypeArgumentsF() throws Exception {
		xAddTypeArguments(false);
	}

	/**
	 * Tests adding no-reference
	 */
	private void xAddNoReference(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddNoReference.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			ApiProblemFactory.createProblemId(
				IApiProblem.CATEGORY_COMPATIBILITY,
				IDelta.FIELD_ELEMENT_TYPE,
				IDelta.REMOVED,
				IDelta.API_FIELD)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "AddNoReference", "FIELD"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddNoReferenceI() throws Exception {
		xAddNoReference(true);
	}

	public void testAddNoReferenceF() throws Exception {
		xAddNoReference(false);
	}

	/**
	 * Tests changing a public field to package in @noextend class
	 */
	private void xPublicToPackage2(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("PublicToPackage2.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getChangedProblemId(IDelta.DECREASE_ACCESS)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[] { PACKAGE_PREFIX + "PublicToPackage2", "FIELD" }; //$NON-NLS-1$ //$NON-NLS-2$
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
	 * Tests changing a public field to private in @noextend class
	 */
	private void xPublicToPrivate2(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("PublicToPrivate2.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getChangedProblemId(IDelta.DECREASE_ACCESS)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[] { PACKAGE_PREFIX + "PublicToPrivate2", "FIELD" }; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testPublicToPrivate2I() throws Exception {
		xPublicToPrivate2(true);
	}

	public void testPublicToPrivate2F() throws Exception {
		xPublicToPrivate2(false);
	}

	/**
	 * Tests changing a public field to protected in @noextend class
	 */
	private void xPublicToProtected2(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("PublicToProtected2.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getChangedProblemId(IDelta.DECREASE_ACCESS)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[] { PACKAGE_PREFIX + "PublicToProtected2", "FIELD" }; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testPublicToProtected2I() throws Exception {
		xPublicToProtected2(true);
	}

	public void testPublicToProtected2F() throws Exception {
		xPublicToProtected2(false);
	}
}
