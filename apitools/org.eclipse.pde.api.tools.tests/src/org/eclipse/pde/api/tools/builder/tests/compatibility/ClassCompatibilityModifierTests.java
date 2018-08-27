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
public class ClassCompatibilityModifierTests extends ClassCompatibilityTests {

	/**
	 * Workspace relative path classes in bundle/project A
	 */
	protected static IPath WORKSPACE_CLASSES_PACKAGE_A = new Path("bundle.a/src/a/classes/modifiers"); //$NON-NLS-1$

	/**
	 * Package prefix for test classes
	 */
	protected static String PACKAGE_PREFIX = "a.classes.modifiers."; //$NON-NLS-1$

	/**
	 * Constructor
	 * @param name
	 */
	public ClassCompatibilityModifierTests(String name) {
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
		return buildTestSuite(ClassCompatibilityModifierTests.class);
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
				IDelta.CLASS_ELEMENT_TYPE,
				IDelta.CHANGED,
				flags);
	}

	@Override
	protected String getTestingProjectName() {
		return "classcompat"; //$NON-NLS-1$
	}

	/**
	 * Tests making a non-final class final
	 */
	private void xAddFinal(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddFinal.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getChangedProblemId(IDelta.NON_FINAL_TO_FINAL)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "AddFinal"}; //$NON-NLS-1$
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
	 * Tests making a non-final class with a noextend tag final
	 */
	private void xAddFinalNoExtend(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddFinalNoExtend.java"); //$NON-NLS-1$
		// no problems expected
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddFinalNoExtendI() throws Exception {
		xAddFinalNoExtend(true);
	}

	public void testAddFinalNoExtendF() throws Exception {
		xAddFinalNoExtend(false);
	}

	/**
	 * Tests making a non-final class with a no-extend tag final while removing the no-extend tag
	 */
	private void xAddFinalRemoveNoExtend(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddFinalRemoveNoExtend.java"); //$NON-NLS-1$
		// no problems expected
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddFinalRemoveNoExtendI() throws Exception {
		xAddFinalRemoveNoExtend(true);
	}

	public void testAddFinalRemoveNoExtendF() throws Exception {
		xAddFinalRemoveNoExtend(false);
	}

	/**
	 * Tests making a non-final class final that has the noinstantiate tag
	 */
	private void xAddFinalNoInstantiate(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddFinalNoInstantiate.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getChangedProblemId(IDelta.NON_FINAL_TO_FINAL)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "AddFinalNoInstantiate"}; //$NON-NLS-1$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddFinalNoInstantiateI() throws Exception {
		xAddFinalNoInstantiate(true);
	}

	public void testAddFinalNoInstantiateF() throws Exception {
		xAddFinalNoInstantiate(false);
	}

	/**
	 * Tests making a non-final class final that has the noinstantiate and
	 * noextend tag
	 */
	private void xAddFinalNoExtendNoInstantiate(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddFinalNoExtendNoInstantiate.java"); //$NON-NLS-1$
		// no problems expected
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddFinalNoExtendNoInstantiateI() throws Exception {
		xAddFinalNoExtendNoInstantiate(true);
	}

	public void testAddFinalNoExtendNoInstantiateF() throws Exception {
		xAddFinalNoExtendNoInstantiate(false);
	}

	/**
	 * Tests making a final class non-final
	 */
	private void xRemoveFinal(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveFinal.java"); //$NON-NLS-1$
		// no errors
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemoveFinalI() throws Exception {
		xRemoveFinal(true);
	}

	public void testRemoveFinalF() throws Exception {
		xRemoveFinal(false);
	}

	/**
	 * Tests making a final class non-final and adding the no-extend tag
	 */
	private void xRemoveFinalAddNoExtend(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveFinalAddNoExtend.java"); //$NON-NLS-1$
		// no errors
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemoveFinalAddNoExtendI() throws Exception {
		xRemoveFinalAddNoExtend(true);
	}

	public void testRemoveFinalAddNoExtendF() throws Exception {
		xRemoveFinalAddNoExtend(false);
	}

	/**
	 * Tests making a non-abstract class abstract
	 */
	private void xAddAbstract(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddAbstract.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getChangedProblemId(IDelta.NON_ABSTRACT_TO_ABSTRACT)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "AddAbstract"}; //$NON-NLS-1$
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
	 * Tests making a non-abstract class with a noextend tag abstract
	 */
	private void xAddAbstractNoExtend(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddAbstractNoExtend.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getChangedProblemId(IDelta.NON_ABSTRACT_TO_ABSTRACT)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "AddAbstractNoExtend"}; //$NON-NLS-1$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddAbstractNoExtendI() throws Exception {
		xAddAbstractNoExtend(true);
	}

	public void testAddAbstractNoExtendF() throws Exception {
		xAddAbstractNoExtend(false);
	}

	/**
	 * Tests making a non-abstract class abstract that has the noinstantiate tag
	 */
	private void xAddAbstractNoInstantiate(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddAbstractNoInstantiate.java"); //$NON-NLS-1$
		// no problems expected
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddAbstractNoInstantiateI() throws Exception {
		xAddAbstractNoInstantiate(true);
	}

	public void testAddAbstractNoInstantiateF() throws Exception {
		xAddAbstractNoInstantiate(false);
	}

	/**
	 * Tests making a non-abstract class abstract that has the noinstantiate and
	 * noextend tag
	 */
	private void xAddAbstractNoExtendNoInstantiate(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddAbstractNoExtendNoInstantiate.java"); //$NON-NLS-1$
		// no problems expected
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddAbstractNoExtendNoInstantiateI() throws Exception {
		xAddAbstractNoExtendNoInstantiate(true);
	}

	public void testAddAbstractNoExtendNoInstantiateF() throws Exception {
		xAddAbstractNoExtendNoInstantiate(false);
	}

	/**
	 * Tests making a public class package protected
	 */
	private void xPublicToPackageVisibility(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("PublicToPackageVisibility.java"); //$NON-NLS-1$
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_COMPATIBILITY,
						IDelta.CLASS_ELEMENT_TYPE,
						IDelta.REMOVED,
						IDelta.API_TYPE)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "PublicToPackageVisibility", "bundle.a_1.0.0"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testPublicToPackageVisibilityI() throws Exception {
		xPublicToPackageVisibility(true);
	}

	public void testPublicToPackageVisibilityF() throws Exception {
		xPublicToPackageVisibility(false);
	}

	/**
	 * Tests making a public inner class to package-access inner class in
	 * no-extend class.
	 */
	private void xPublicToPackageInnerClass(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("PublicToPackageInnerClass.java"); //$NON-NLS-1$
		int[] ids = new int[] { ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY,
				IDelta.CLASS_ELEMENT_TYPE, IDelta.CHANGED, IDelta.DECREASE_ACCESS) };
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[] { PACKAGE_PREFIX + "PublicToPackageInnerClass.A", "bundle.a_1.0.0" }; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testPublicToPackageInnerClassI() throws Exception {
		xPublicToPackageInnerClass(true);
	}

	public void testPublicToPackageInnerClassF() throws Exception {
		xPublicToPackageInnerClass(false);
	}

	/**
	 * Tests making a public inner class to protected inner class in no-extend
	 * class.
	 */
	private void xPublicToProtectedInnerClass(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("PublicToProtectedInnerClass.java"); //$NON-NLS-1$
		int[] ids = new int[] { ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY,
				IDelta.CLASS_ELEMENT_TYPE, IDelta.CHANGED, IDelta.DECREASE_ACCESS) };
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[] { PACKAGE_PREFIX + "PublicToProtectedInnerClass.A", "bundle.a_1.0.0" }; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testPublicToProtectedInnerClassI() throws Exception {
		xPublicToProtectedInnerClass(true);
	}

	public void testPublicToProtectedInnerClassF() throws Exception {
		xPublicToProtectedInnerClass(false);
	}

	/**
	 * Tests making a public inner class to private inner class in no-extend
	 * class.
	 */
	private void xPublicToPrivateInnerClass(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("PublicToPrivateInnerClass.java"); //$NON-NLS-1$
		int[] ids = new int[] { ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY,
				IDelta.CLASS_ELEMENT_TYPE, IDelta.CHANGED, IDelta.DECREASE_ACCESS) };
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[] { PACKAGE_PREFIX + "PublicToPrivateInnerClass.A", "bundle.a_1.0.0" }; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testPublicToPrivateInnerClassI() throws Exception {
		xPublicToPrivateInnerClass(true);
	}

	public void testPublicToPrivateInnerClassF() throws Exception {
		xPublicToPrivateInnerClass(false);
	}

	@Override
	protected int getDefaultProblemId() {
		// NOT USED
		return 0;
	}

}
