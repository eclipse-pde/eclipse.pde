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
 * related to member types.
 *
 * @since 1.0
 */
public class ClassCompatibilityMemberTypeTests extends ClassCompatibilityTests {

	/**
	 * Workspace relative path classes in bundle/project A
	 */
	protected static IPath WORKSPACE_CLASSES_PACKAGE_A = new Path("bundle.a/src/a/classes/membertypes"); //$NON-NLS-1$

	/**
	 * Package prefix for test classes
	 */
	protected static String PACKAGE_PREFIX = "a.classes.membertypes."; //$NON-NLS-1$

	public ClassCompatibilityMemberTypeTests(String name) {
		super(name);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("membertypes"); //$NON-NLS-1$
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(ClassCompatibilityMemberTypeTests.class);
	}

	@Override
	protected int getDefaultProblemId() {
		return ApiProblemFactory.createProblemId(
				IApiProblem.CATEGORY_COMPATIBILITY,
				IDelta.CLASS_ELEMENT_TYPE,
				IDelta.REMOVED,
				IDelta.TYPE_MEMBER);
	}

	protected int getReducedVisibilityId() {
		return ApiProblemFactory.createProblemId(
				IApiProblem.CATEGORY_COMPATIBILITY,
				IDelta.CLASS_ELEMENT_TYPE,
				IDelta.CHANGED,
				IDelta.DECREASE_ACCESS);
	}

	protected int getRemovedAPITypeId() {
		return ApiProblemFactory.createProblemId(
				IApiProblem.CATEGORY_COMPATIBILITY,
				IDelta.CLASS_ELEMENT_TYPE,
				IDelta.REMOVED,
				IDelta.API_TYPE);
	}

	@Override
	protected String getTestingProjectName() {
		return "classcompat"; //$NON-NLS-1$
	}

	/**
	 * Tests removing a public member type
	 */
	private void xRemovePublicMemberType(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemovePublicMemberType.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getDefaultProblemId()
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{"a.classes.membertypes.RemovePublicMemberType.PublicType", "bundle.a_1.0.0"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemovePublicMemberTypeI() throws Exception {
		xRemovePublicMemberType(true);
	}

	public void testRemovePublicMemberTypeF() throws Exception {
		xRemovePublicMemberType(false);
	}

	/**
	 * Tests removing a protected member type
	 */
	private void xRemoveProtectedMemberType(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveProtectedMemberType.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getDefaultProblemId()
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{"a.classes.membertypes.RemoveProtectedMemberType.ProtectedType", "bundle.a_1.0.0"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemoveProtectedMemberTypeI() throws Exception {
		xRemoveProtectedMemberType(true);
	}

	public void testRemoveProtectedMemberTypeF() throws Exception {
		xRemoveProtectedMemberType(false);
	}

	/**
	 * Tests removing a default/package visible member type
	 */
	private void xRemovePackageMemberType(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemovePackageMemberType.java"); //$NON-NLS-1$
		// no problems expected
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemovePackageMemberTypeI() throws Exception {
		xRemovePackageMemberType(true);
	}

	public void testRemovePackageMemberTypeF() throws Exception {
		xRemovePackageMemberType(false);
	}

	/**
	 * Tests removing a private member type
	 */
	private void xRemovePrivateMemberType(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemovePrivateMemberType.java"); //$NON-NLS-1$
		// no problems expected
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemovePrivateMemberTypeI() throws Exception {
		xRemovePrivateMemberType(true);
	}

	public void testRemovePrivateMemberTypeF() throws Exception {
		xRemovePrivateMemberType(false);
	}

	/**
	 * Tests removing a protected member type with the enclosing type annotated
	 * noextend.
	 */
	private void xRemoveProtectedMemberTypeNoExtend(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveProtectedMemberTypeNoExtend.java"); //$NON-NLS-1$
		// no problems expected
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemoveProtectedMemberTypeNoExtendI() throws Exception {
		xRemoveProtectedMemberTypeNoExtend(true);
	}

	public void testRemoveProtectedMemberTypeNoExtendF() throws Exception {
		xRemoveProtectedMemberTypeNoExtend(false);
	}

	/**
	 * Tests reducing visibility from public to protected
	 */
	private void xPublicToProtected(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("PublicToProtectedVisibility.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getReducedVisibilityId()
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "PublicToProtectedVisibility.PublicToProtected"}; //$NON-NLS-1$
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
	 * Tests reducing visibility from public to package
	 */
	private void xPublicToPackage(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("PublicToPackageVisibility.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getReducedVisibilityId()
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "PublicToPackageVisibility.PublicToPackage"}; //$NON-NLS-1$
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
	 * Tests reducing visibility from public to private
	 */
	private void xPublicToPrivate(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("PublicToPrivateVisibility.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getReducedVisibilityId()
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "PublicToPrivateVisibility.PublicToPrivate"}; //$NON-NLS-1$
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
	 * Tests reducing visibility from protected to package
	 */
	private void xProtectedToPackage(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ProtectedToPackageVisibility.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getReducedVisibilityId()
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "ProtectedToPackageVisibility.ProtectedToPackage"}; //$NON-NLS-1$
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
	 * Tests reducing visibility from protected to private
	 */
	private void xProtectedToPrivate(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ProtectedToPrivateVisibility.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getReducedVisibilityId()
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "ProtectedToPrivateVisibility.ProtectedToPrivate"}; //$NON-NLS-1$
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
	 * Tests reducing visibility from package to private
	 */
	private void xPackageToPrivate(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("PackageToPrivateVisibility.java"); //$NON-NLS-1$
		//no errors expected
		performCompatibilityTest(filePath, incremental);
	}

	public void testPackageToPrivateI() throws Exception {
		xPackageToPrivate(true);
	}

	public void testPackageToPrivateF() throws Exception {
		xPackageToPrivate(false);
	}

	/**
	 * Tests reducing visibility from protected to package for a noextend enclosing type
	 */
	private void xProtectedToPackageNoExtend(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ProtectedToPackageVisibilityNoExtend.java"); //$NON-NLS-1$
		// no problem expected
		performCompatibilityTest(filePath, incremental);
	}

	public void testProtectedToPackageNoExtendI() throws Exception {
		xProtectedToPackageNoExtend(true);
	}

	public void testProtectedToPackageNoExtendF() throws Exception {
		xProtectedToPackageNoExtend(false);
	}

	/**
	 * Tests reducing visibility from protected to private for a noextend enclosing type
	 */
	private void xProtectedToPrivateNoExtend(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ProtectedToPrivateVisibilityNoExtend.java"); //$NON-NLS-1$
		// no problem expected
		performCompatibilityTest(filePath, incremental);
	}

	public void testProtectedToPrivateNoExtendI() throws Exception {
		xProtectedToPrivateNoExtend(true);
	}

	public void testProtectedToPrivateNoExtendF() throws Exception {
		xProtectedToPrivateNoExtend(false);
	}
}
