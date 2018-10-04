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
 * related to members in interfaces.
 *
 * @since 1.0
 */
public class InterfaceCompatibilityMemberTests extends InterfaceCompatibilityTests {

	/**
	 * Workspace relative path classes in bundle/project A
	 */
	protected static IPath WORKSPACE_CLASSES_PACKAGE_A = new Path("bundle.a/src/a/interfaces/members"); //$NON-NLS-1$

	/**
	 * Package prefix for test classes
	 */
	protected static String PACKAGE_PREFIX = "a.interfaces.members."; //$NON-NLS-1$

	public InterfaceCompatibilityMemberTests(String name) {
		super(name);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("members"); //$NON-NLS-1$
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(InterfaceCompatibilityMemberTests.class);
	}

	@Override
	protected String getTestingProjectName() {
		return "intercompat"; //$NON-NLS-1$
	}

	public void testAddSuperInterfaceAI() throws Exception {
		xAddSuperInterfaceA(true);
	}

	public void testAddSuperInterfaceAF() throws Exception {
		xAddSuperInterfaceA(false);
	}

	/**
	 * Tests adding a field to an interface
	 */
	private void xAddField(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddField.java"); //$NON-NLS-1$
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_COMPATIBILITY,
						IDelta.INTERFACE_ELEMENT_TYPE,
						IDelta.ADDED,
						IDelta.FIELD)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "AddField", "ADDED_FIELD"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddFieldI() throws Exception {
		xAddField(true);
	}

	public void testAddFieldF() throws Exception {
		xAddField(false);
	}

	/**
	 * Tests adding a field to a noimplement interface
	 */
	private void xAddFieldNoImplement(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddFieldNoImplement.java"); //$NON-NLS-1$
		expectingNoProblems();
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddFieldNoImplementI() throws Exception {
		xAddFieldNoImplement(true);
	}

	public void testAddFieldNoImplementF() throws Exception {
		xAddFieldNoImplement(false);
	}

	/**
	 * Tests adding a field to a noextend interface
	 */
	private void xAddFieldNoExtend(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddFieldNoExtend.java"); //$NON-NLS-1$
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_COMPATIBILITY,
						IDelta.INTERFACE_ELEMENT_TYPE,
						IDelta.ADDED,
						IDelta.FIELD)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "AddFieldNoExtend", "ADDED_FIELD"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddFieldNoExtendI() throws Exception {
		xAddFieldNoExtend(true);
	}

	public void testAddFieldNoExtendF() throws Exception {
		xAddFieldNoExtend(false);
	}

	/**
	 * Tests adding a field to a noextend / noimplement interface
	 */
	private void xAddFieldNoExtendNoImplement(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddFieldNoExtendNoImplement.java"); //$NON-NLS-1$
		//expecting no problems
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddFieldNoExtendNoImplementI() throws Exception {
		xAddFieldNoExtendNoImplement(true);
	}

	public void testAddFieldNoExtendNoImplementF() throws Exception {
		xAddFieldNoExtendNoImplement(false);
	}

	/**
	 * Tests adding a method to an interface
	 */
	private void xAddMethod(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddMethod.java"); //$NON-NLS-1$
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_COMPATIBILITY,
						IDelta.INTERFACE_ELEMENT_TYPE,
						IDelta.ADDED,
						IDelta.METHOD)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "AddMethod", "addMethod(String)"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddMethodI() throws Exception {
		xAddMethod(true);
	}

	public void testAddMethodF() throws Exception {
		xAddMethod(false);
	}

	/**
	 * Tests adding a method to a noimplement interface
	 */
	private void xAddMethodNoImplement(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddMethodNoImplement.java"); //$NON-NLS-1$
		expectingNoProblems();
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddMethodNoImplementI() throws Exception {
		xAddMethodNoImplement(true);
	}

	public void testAddMethodNoImplementF() throws Exception {
		xAddMethodNoImplement(false);
	}

	/**
	 * Tests adding a method to a noimplement interface
	 */
	private void xAddMethodNoExtend(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddMethodNoExtend.java"); //$NON-NLS-1$
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_COMPATIBILITY,
						IDelta.INTERFACE_ELEMENT_TYPE,
						IDelta.ADDED,
						IDelta.METHOD)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "AddMethodNoExtend", "addMethod(String)"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddMethodNoExtendI() throws Exception {
		xAddMethodNoExtend(true);
	}

	public void testAddMethodNoExtendF() throws Exception {
		xAddMethodNoExtend(false);
	}

	/**
	 * Tests adding a method to a noimplement interface
	 */
	private void xAddMethodNoExtendNoImplement(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddMethodNoExtendNoImplement.java"); //$NON-NLS-1$
		//expecting no problems
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddMethodNoExtendNoImplementI() throws Exception {
		xAddMethodNoExtendNoImplement(true);
	}

	public void testAddMethodNoExtendNoImplementF() throws Exception {
		xAddMethodNoExtendNoImplement(false);
	}

	/**
	 * Tests adding a  member type to an interface
	 */
	private void xAddMemberType(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddMemberType.java"); //$NON-NLS-1$
		// no problems expected
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddMemberTypeI() throws Exception {
		xAddMemberType(true);
	}

	public void testAddMemberTypeF() throws Exception {
		xAddMemberType(false);
	}

	/**
	 * Tests adding a  member type to a noimplement interface
	 */
	private void xAddMemberTypeNoImplement(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddMemberTypeNoImplement.java"); //$NON-NLS-1$
		// no problems expected
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddMemberTypeNoImplementI() throws Exception {
		xAddMemberTypeNoImplement(true);
	}

	public void testAddMemberTypeNoImplementF() throws Exception {
		xAddMemberTypeNoImplement(false);
	}

	/**
	 * Tests removing a field from an interface
	 */
	private void xRemoveField(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveField.java"); //$NON-NLS-1$
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_COMPATIBILITY,
						IDelta.INTERFACE_ELEMENT_TYPE,
						IDelta.REMOVED,
						IDelta.FIELD)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemoveField", "FIELD"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemoveFieldI() throws Exception {
		xRemoveField(true);
	}

	public void testRemoveFieldF() throws Exception {
		xRemoveField(false);
	}

	/**
	 * Tests removing a method from an interface
	 */
	private void xRemoveMethod(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveMethod.java"); //$NON-NLS-1$
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_COMPATIBILITY,
						IDelta.INTERFACE_ELEMENT_TYPE,
						IDelta.REMOVED,
						IDelta.METHOD)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemoveMethod", "removeMethod(String)"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemoveMethodI() throws Exception {
		xRemoveMethod(true);
	}

	public void testRemoveMethodF() throws Exception {
		xRemoveMethod(false);
	}

	/**
	 * Tests removing a member type from an interface
	 */
	private void xRemoveMemberType(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveMemberType.java"); //$NON-NLS-1$
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_COMPATIBILITY,
						IDelta.INTERFACE_ELEMENT_TYPE,
						IDelta.REMOVED,
						IDelta.TYPE_MEMBER)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{"a.interfaces.members.RemoveMemberType.MemberType", "bundle.a_1.0.0"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemoveMemberTypeI() throws Exception {
		xRemoveMemberType(true);
	}

	public void testRemoveMemberTypeF() throws Exception {
		xRemoveMemberType(false);
	}

	/**
	 * Tests adding a super interface
	 */
	private void xAddSuperInterfaceA(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddInterfaceA.java"); //$NON-NLS-1$
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_COMPATIBILITY,
						IDelta.INTERFACE_ELEMENT_TYPE,
						IDelta.ADDED, IDelta.SUPER_INTERFACE_WITH_METHODS)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{
				PACKAGE_PREFIX + "AddInterfaceA", //$NON-NLS-1$
				"a.classes.hierarchy.InterfaceA", //$NON-NLS-1$
				"methodA()"}; //$NON-NLS-1$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	/**
	 * Tests adding a super interface to a noimplement interface
	 */
	private void xAddSuperInterfaceANoImplement(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddInterfaceANoImplement.java"); //$NON-NLS-1$
		expectingNoProblems();
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddSuperInterfaceANoImplementI() throws Exception {
		xAddSuperInterfaceANoImplement(true);
	}

	public void testAddSuperInterfaceANoImplementF() throws Exception {
		xAddSuperInterfaceANoImplement(false);
	}

	/**
	 * Tests adding a super interface to a noimplement interface
	 */
	private void xAddSuperInterfaceANoExtend(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddInterfaceANoExtend.java"); //$NON-NLS-1$
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_COMPATIBILITY,
						IDelta.INTERFACE_ELEMENT_TYPE,
						IDelta.ADDED,
						IDelta.SUPER_INTERFACE_WITH_METHODS)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{
				PACKAGE_PREFIX + "AddInterfaceANoExtend", //$NON-NLS-1$
				"a.classes.hierarchy.InterfaceA", //$NON-NLS-1$
				"methodA()"}; //$NON-NLS-1$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddSuperInterfaceANoExtendI() throws Exception {
		xAddSuperInterfaceANoExtend(true);
	}

	public void testAddSuperInterfaceANoExtendF() throws Exception {
		xAddSuperInterfaceANoExtend(false);
	}

	/**
	 * Tests adding a super interface to a noimplement interface
	 */
	private void xAddSuperInterfaceANoExtendNoImplement(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddInterfaceANoExtendNoImplement.java"); //$NON-NLS-1$
		// no problems expected
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddSuperInterfaceANoExtendNoImplementI() throws Exception {
		xAddSuperInterfaceANoExtendNoImplement(true);
	}

	public void testAddSuperInterfaceANoExtendNoImplementF() throws Exception {
		xAddSuperInterfaceANoExtendNoImplement(false);
	}

	/**
	 * Tests removing a super interface
	 */
	private void xReduceSuperInterfaceABtoA(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ReduceFromABtoA.java"); //$NON-NLS-1$
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_COMPATIBILITY,
						IDelta.INTERFACE_ELEMENT_TYPE,
						IDelta.CHANGED,
						IDelta.CONTRACTED_SUPERINTERFACES_SET)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "ReduceFromABtoA"}; //$NON-NLS-1$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testReduceSuperInterfaceABtoAI() throws Exception {
		xReduceSuperInterfaceABtoA(true);
	}

	public void testReduceSuperInterfaceABtoAF() throws Exception {
		xReduceSuperInterfaceABtoA(false);
	}

	/**
	 * Tests removing all super interfaces
	 */
	private void xReduceSuperInterfaceABtoEmpty(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ReduceFromABtoEmpty.java"); //$NON-NLS-1$
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_COMPATIBILITY,
						IDelta.INTERFACE_ELEMENT_TYPE,
						IDelta.CHANGED,
						IDelta.CONTRACTED_SUPERINTERFACES_SET)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "ReduceFromABtoEmpty"}; //$NON-NLS-1$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testReduceSuperInterfaceABtoEmptyI() throws Exception {
		xReduceSuperInterfaceABtoEmpty(true);
	}

	public void testReduceSuperInterfaceABtoEmptyF() throws Exception {
		xReduceSuperInterfaceABtoEmpty(false);
	}

	/**
	 * Tests adding a method to a noimplement interface
	 */
	private void xAddMethodNoImplement2(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("K.java"); //$NON-NLS-1$
		updateWorkspaceFile(
				filePath,
				getUpdateFilePath(filePath.lastSegment()));
		filePath = WORKSPACE_CLASSES_PACKAGE_A.append("I.java"); //$NON-NLS-1$
		createWorkspaceFile(
				filePath,
				getUpdateFilePath(filePath.lastSegment()));
		filePath = WORKSPACE_CLASSES_PACKAGE_A.append("J.java"); //$NON-NLS-1$
		createWorkspaceFile(
				filePath,
				getUpdateFilePath(filePath.lastSegment()));
		filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddMethodNoImplement2.java"); //$NON-NLS-1$
		expectingNoProblems();
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddMethodNoImplement2I() throws Exception {
		xAddMethodNoImplement2(true);
	}

	public void testAddMethodNoImplement2F() throws Exception {
		xAddMethodNoImplement2(false);
	}
}
