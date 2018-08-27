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
 * related method modifiers and visibility.
 *
 * @since 1.0
 */
public class MethodCompatibilityModifierTests extends MethodCompatibilityTests {

	/**
	 * Workspace relative path classes in bundle/project A
	 */
	protected static IPath WORKSPACE_CLASSES_PACKAGE_A = new Path("bundle.a/src/a/methods/modifiers"); //$NON-NLS-1$

	/**
	 * Package prefix for test classes
	 */
	protected static String PACKAGE_PREFIX = "a.methods.modifiers."; //$NON-NLS-1$

	public MethodCompatibilityModifierTests(String name) {
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
		return buildTestSuite(MethodCompatibilityModifierTests.class);
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
				IDelta.METHOD_ELEMENT_TYPE,
				IDelta.CHANGED,
				flags);
	}

	/**
	 * Returns a problem id for a compatibility addition to a class based on the
	 * specified flags.
	 *
	 * @param flags
	 * @return problem id
	 */
	protected int getAddedProblemId(int flags) {
		return ApiProblemFactory.createProblemId(
				IApiProblem.CATEGORY_COMPATIBILITY,
				IDelta.METHOD_ELEMENT_TYPE,
				IDelta.ADDED,
				flags);
	}

	/**
	 * Tests making a non-final method final
	 */
	private void xAddFinal(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddFinal.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getChangedProblemId(IDelta.NON_FINAL_TO_FINAL)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "AddFinal", "method()"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddNoOverrideI() throws Exception {
		xAddNoOverride(true);
	}

	public void testAddNoOverrideF() throws Exception {
		xAddNoOverride(false);
	}

	public void testAddNoOverrideToFinalI() throws Exception {
		xAddNoOverrideToFinal(true);
	}

	public void testAddNoOverrideToFinalF() throws Exception {
		xAddNoOverrideToFinal(false);
	}

	public void testAddFinalI() throws Exception {
		xAddFinal(true);
	}

	public void testAddFinalF() throws Exception {
		xAddFinal(false);
	}

	/**
	 * Tests making a non-abstract method abstract
	 */
	private void xAddAbstract(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddAbstract.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getChangedProblemId(IDelta.NON_ABSTRACT_TO_ABSTRACT)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "AddAbstract", "method()"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddAbstractI() throws Exception {
		xAddAbstract(true);
	}

	public void testAddAbstractF() throws Exception {
		xAddAbstract(false);
	}

	/**
	 * Tests making a non-final no-override method final
	 */
	private void xAddFinalNoOverride(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddFinalNoOverride.java"); //$NON-NLS-1$
		// no problems expected since @nooverride
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddFinalNoOverrideI() throws Exception {
		xAddFinalNoOverride(true);
	}

	public void testAddFinalNoOverrideF() throws Exception {
		xAddFinalNoOverride(false);
	}

	/**
	 * Tests making a non-final no-override method final, and remove the no-override tag.
	 */
	private void xAddFinalRemoveNoOverride(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddFinalRemoveNoOverride.java"); //$NON-NLS-1$
		// no problems expected since @nooverride
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddFinalRemoveNoOverrideI() throws Exception {
		xAddFinalRemoveNoOverride(true);
	}

	public void testAddFinalRemoveNoOverrideF() throws Exception {
		xAddFinalRemoveNoOverride(false);
	}

	/**
	 * Tests making a non-final method final in a no-extend class
	 */
	private void xAddFinalNoExtend(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddFinalNoExtend.java"); //$NON-NLS-1$
		// should be no problems since @noextend
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddFinalNoExtendI() throws Exception {
		xAddFinalNoExtend(true);
	}

	public void testAddFinalNoExtendF() throws Exception {
		xAddFinalNoExtend(false);
	}

	/**
	 * Tests making a non-static method static
	 */
	private void xAddStatic(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddStatic.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getChangedProblemId(IDelta.NON_STATIC_TO_STATIC)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "AddStatic", "method()"}; //$NON-NLS-1$ //$NON-NLS-2$
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
	 * Tests making a non-static no-reference method static
	 */
	private void xAddStaticNoReference(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddStaticNoReference.java"); //$NON-NLS-1$
		// no problem expected
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddStaticNoReferenceI() throws Exception {
		xAddStaticNoReference(true);
	}

	public void testAddStaticNoReferenceF() throws Exception {
		xAddStaticNoReference(false);
	}

	/**
	 * Tests making a static method non-static
	 */
	private void xRemoveStatic(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveStatic.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getChangedProblemId(IDelta.STATIC_TO_NON_STATIC)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemoveStatic", "method()"}; //$NON-NLS-1$ //$NON-NLS-2$
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
		// no problem expected
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemoveStaticNoReferenceI() throws Exception {
		xRemoveStaticNoReference(true);
	}

	public void testRemoveStaticNoReferenceF() throws Exception {
		xRemoveStaticNoReference(false);
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
		args[0] = new String[]{PACKAGE_PREFIX + "ProtectedToPackage", "method()"}; //$NON-NLS-1$ //$NON-NLS-2$
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
		args[0] = new String[]{PACKAGE_PREFIX + "ProtectedToPrivate", "method()"}; //$NON-NLS-1$ //$NON-NLS-2$
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
	 * Tests changing a protected no-override method to private method
	 */
	private void xProtectedToPrivateNoOverride(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ProtectedToPrivateNoOverride.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getChangedProblemId(IDelta.DECREASE_ACCESS)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "ProtectedToPrivateNoOverride", "method()"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testProtectedToPrivateNoOverrideI() throws Exception {
		xProtectedToPrivateNoOverride(true);
	}

	public void testProtectedToPrivateNoOverrideF() throws Exception {
		xProtectedToPrivateNoOverride(false);
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
		args[0] = new String[]{PACKAGE_PREFIX + "PublicToPackage", "method()"}; //$NON-NLS-1$ //$NON-NLS-2$
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
		args[0] = new String[]{PACKAGE_PREFIX + "PublicToPrivate", "method()"}; //$NON-NLS-1$ //$NON-NLS-2$
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
	 * Tests changing a private method to public, no-reference
	 */
	private void xPrivateToPublic(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("PrivateToPublicNoReference.java"); //$NON-NLS-1$
		// no problems expected
		performCompatibilityTest(filePath, incremental);
	}

	public void testPrivateToPublicI() throws Exception {
		xPrivateToPublic(true);
	}

	public void testPrivateToPublicF() throws Exception {
		xPrivateToPublic(false);
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
		args[0] = new String[]{PACKAGE_PREFIX + "PublicToProtected", "method()"}; //$NON-NLS-1$ //$NON-NLS-2$
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
	 * Tests adding no-override
	 */
	private void xAddNoOverride(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddNoOverride.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getAddedProblemId(IDelta.RESTRICTIONS)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "AddNoOverride", "method()"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	/**
	 * Tests adding no-override to a final method (no-op)
	 */
	private void xAddNoOverrideToFinal(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddNoOverrideToFinal.java"); //$NON-NLS-1$
		// no problems
		performCompatibilityTest(filePath, incremental);
	}

	/**
	 * Tests adding no-reference
	 */
	private void xAddNoReference(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddNoReference.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			ApiProblemFactory.createProblemId(
				IApiProblem.CATEGORY_COMPATIBILITY,
				IDelta.METHOD_ELEMENT_TYPE,
				IDelta.REMOVED,
				IDelta.API_METHOD)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "AddNoReference", "method()"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddNoReferenceI() throws Exception {
		xAddNoReference(true);
	}

	public void testAddNoReferenceF() throws Exception {
		xAddNoReference(false);
	}

	public void testRemoveNoOverrideToFinalI() throws Exception {
		xRemoveNoOverrideToFinal(true);
	}

	public void testRemoveNoOverrideToFinalF() throws Exception {
		xRemoveNoOverrideToFinal(false);
	}
	/**
	 * Tests removing no-override to a final method (no-op)
	 */
	private void xRemoveNoOverrideToFinal(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveNoOverrideToFinal.java"); //$NON-NLS-1$
		// no problems
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemoveNoOverrideI() throws Exception {
		xRemoveNoOverride(true);
	}

	public void testRemoveNoOverrideF() throws Exception {
		xRemoveNoOverride(false);
	}
	/**
	 * Tests removing no-override to a final method (no-op)
	 */
	private void xRemoveNoOverride(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveNoOverride.java"); //$NON-NLS-1$
		performCompatibilityTest(filePath, incremental);
	}

	/**
	 * Tests changing a public method to package in @noextend class
	 */
	private void xPublicToPackage2(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("PublicToPackage2.java"); //$NON-NLS-1$
		int[] ids = new int[] { getChangedProblemId(IDelta.DECREASE_ACCESS) };
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[] { PACKAGE_PREFIX + "PublicToPackage2", "method()" }; //$NON-NLS-1$ //$NON-NLS-2$
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
		args[0] = new String[] { PACKAGE_PREFIX + "PublicToPrivate2", "method()" }; //$NON-NLS-1$ //$NON-NLS-2$
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
	 * Tests changing a public method to protected in @noextend class
	 */
	private void xPublicToProtected2(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("PublicToProtected2.java"); //$NON-NLS-1$
		int[] ids = new int[] { getChangedProblemId(IDelta.DECREASE_ACCESS) };
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[] { PACKAGE_PREFIX + "PublicToProtected2", "method()" }; //$NON-NLS-1$ //$NON-NLS-2$
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
