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
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * Tests that the builder correctly reports compatibility problems
 * related class hierarchies.
 * 
 * @since 3.4
 */
public class ClassCompatibilityHierarchyTests extends ClassCompatibilityTests {
	
	/**
	 * Workspace relative path classes in bundle/project A
	 */
	protected static IPath WORKSPACE_CLASSES_PACKAGE_A = new Path("bundle.a/src/a/classes/hierarchy");

	/**
	 * Package prefix for test classes
	 */
	protected static String PACKAGE_PREFIX = "a.classes.hierarchy.";
	
	/**
	 * Constructor
	 * @param name
	 */
	public ClassCompatibilityHierarchyTests(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("hierarchy");
	}
	
	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(ClassCompatibilityHierarchyTests.class);
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestingProjectName()
	 */
	protected String getTestingProjectName() {
		return "classcompat";
	}
	
	/**
	 * Tests the reduction of class hierarchy from C to A
	 */
	private void xReduceHierarchyCtoA(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ReduceFromCtoA.java");
		int[] ids = new int[] {
			getChangedProblemId(IDelta.CONTRACTED_SUPERCLASS_SET)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "ReduceFromCtoA"};
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}
	
	public void testReduceHierarchyCtoAI() throws Exception {
		xReduceHierarchyCtoA(true);
	}	
	
	public void testReduceHierarchyCtoAF() throws Exception {
		xReduceHierarchyCtoA(false);
	}
	
	/**
	 * Tests the reduction of class hierarchy from C to B
	 */
	private void xReduceHierarchyCtoB(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ReduceFromCtoB.java");
		int[] ids = new int[] {
			getChangedProblemId(IDelta.CONTRACTED_SUPERCLASS_SET)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "ReduceFromCtoB"};
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}
	
	public void testReduceHierarchyCtoBI() throws Exception {
		xReduceHierarchyCtoB(true);
	}	
	
	public void testReduceHierarchyCtoBF() throws Exception {
		xReduceHierarchyCtoB(false);
	}	
	
	/**
	 * Tests the reduction of class hierarchy from C to Object
	 */
	private void xReduceHierarchyCtoObject(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ReduceFromCtoObject.java");
		int[] ids = new int[] {
			getChangedProblemId(IDelta.SUPERCLASS) // TODO appears as changed superclass versus reduced
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "ReduceFromCtoObject"};
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}
	
	public void testReduceHierarchyCtoObjectI() throws Exception {
		xReduceHierarchyCtoObject(true);
	}	
	
	public void testReduceHierarchyCtoObjectF() throws Exception {
		xReduceHierarchyCtoObject(false);
	}	
	
	/**
	 * Tests the change of superclass from A to D
	 */
	private void xChangeHierarchyAtoD(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ChangedFromAtoD.java");
		int[] ids = new int[] {
			getChangedProblemId(IDelta.CONTRACTED_SUPERCLASS_SET) // TODO appears as reduced versus changed superclass
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "ChangedFromAtoD"};
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}
	
	public void testChangeHierarchyAtoDI() throws Exception {
		xChangeHierarchyAtoD(true);
	}	
	
	public void testChangeHierarchyAtoDF() throws Exception {
		xChangeHierarchyAtoD(false);
	}	
	
	/**
	 * Tests the reduction of super interfaces from A, B to A
	 */
	private void xReduceInterfaceABtoA(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ReduceInterfaceFromABtoA.java");
		int[] ids = new int[] {
			getChangedProblemId(IDelta.CONTRACTED_SUPERINTERFACES_SET)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "ReduceInterfaceFromABtoA"};
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}
	
	public void testReduceInterfaceABtoAI() throws Exception {
		xReduceInterfaceABtoA(true);
	}	
	
	public void testReduceInterfaceABtoAF() throws Exception {
		xReduceInterfaceABtoA(false);
	}	
	
	/**
	 * Tests the reduction of super interfaces from A, B to empty/none
	 */
	private void xReduceInterfaceABtoEmpty(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ReduceInterfaceFromABtoEmpty.java");
		int[] ids = new int[] {
			getChangedProblemId(IDelta.CONTRACTED_SUPERINTERFACES_SET)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "ReduceInterfaceFromABtoEmpty"};
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}
	
	public void testReduceInterfaceABtoEmptyI() throws Exception {
		xReduceInterfaceABtoEmpty(true);
	}	
	
	public void testReduceInterfaceABtoEmptyF() throws Exception {
		xReduceInterfaceABtoEmpty(false);
	}
	
	/**
	 * Tests the change of super interfaces from A to B
	 */
	private void xChangeInterfaceAtoB(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ChangeInterfaceFromAtoB.java");
		int[] ids = new int[] {
			getChangedProblemId(IDelta.CONTRACTED_SUPERINTERFACES_SET)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "ChangeInterfaceFromAtoB"};
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}
	
	public void testChangeInterfaceAtoBI() throws Exception {
		xChangeInterfaceAtoB(true);
	}	
	
	public void testChangeInterfaceAtoBF() throws Exception {
		xChangeInterfaceAtoB(false);
	}
	
	/**
	 * Tests the addition of super interface A
	 */
	private void xAddInterfaceA(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddInterfaceA.java");
		// no problems expected
		performCompatibilityTest(filePath, incremental);
	}
	
	public void testAddInterfaceAI() throws Exception {
		xAddInterfaceA(true);
	}	
	
	public void testAddInterfaceAF() throws Exception {
		xAddInterfaceA(false);
	}
	
	/**
	 * Tests pushing a method up the hierarchy
	 */
	private void xPushMethodUp(boolean incremental) throws Exception {
		// modify two files
		IPath file1 = WORKSPACE_CLASSES_PACKAGE_A.append("SuperClazz.java");
		updateWorkspaceFile(
				file1,
				getUpdateFilePath(file1.lastSegment()));
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("SubClazz.java");
		// TODO: no problems expected
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
					IApiProblem.CATEGORY_COMPATIBILITY,
					IDelta.CLASS_ELEMENT_TYPE,
					IDelta.REMOVED,
					IDelta.METHOD)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "SubClazz", "pushUp()"};
		setExpectedMessageArgs(args);		
		performCompatibilityTest(filePath, incremental);
	}
	
	public void ZZZtestPushMethodUpI() throws Exception {
		xPushMethodUp(true);
	}	
	
	public void ZZZtestPushMethodUpF() throws Exception {
		xPushMethodUp(false);
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getDefaultProblemId()
	 */
	@Override
	protected int getDefaultProblemId() {
		// NOT USED
		return 0;
	}
	
}
