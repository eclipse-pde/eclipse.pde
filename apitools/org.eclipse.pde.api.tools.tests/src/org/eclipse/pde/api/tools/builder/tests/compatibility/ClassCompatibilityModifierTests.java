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
public class ClassCompatibilityModifierTests extends ClassCompatibilityTests {
	
	/**
	 * Workspace relative path classes in bundle/project A
	 */
	protected static IPath WORKSPACE_CLASSES_PACKAGE_A = new Path("org.eclipse.api.tools.tests.compatability.a/src/a/classes/modifiers");

	/**
	 * Package prefix for test classes
	 */
	protected static String PACKAGE_PREFIX = "a.classes.modifiers.";
	
	/**
	 * Constructor
	 * @param name
	 */
	public ClassCompatibilityModifierTests(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("modifiers");
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestingProjectName()
	 */
	protected String getTestingProjectName() {
		return "classcompat";
	}
	
	/**
	 * Tests making a non-final class final
	 */
	private void xAddFinal(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddFinal.java");
		int[] ids = new int[] {
			getChangedProblemId(IDelta.NON_FINAL_TO_FINAL)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "AddFinal"};
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
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddFinalNoExtend.java");
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
	 * Tests making a non-final class final that has the noinstantiate tag
	 */
	private void xAddFinalNoInstantiate(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddFinalNoInstantiate.java");
		int[] ids = new int[] {
			getChangedProblemId(IDelta.NON_FINAL_TO_FINAL)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "AddFinalNoInstantiate"};
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
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddFinalNoExtendNoInstantiate.java");
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
	 * Tests making a non-abstract class abstract
	 */
	private void xAddAbstract(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddAbstract.java");
		int[] ids = new int[] {
			getChangedProblemId(IDelta.NON_ABSTRACT_TO_ABSTRACT)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "AddAbstract"};
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
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddAbstractNoExtend.java");
		int[] ids = new int[] {
			getChangedProblemId(IDelta.NON_ABSTRACT_TO_ABSTRACT)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "AddAbstractNoExtend"};
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
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddAbstractNoInstantiate.java");
		// TODO: no problems expected
		int[] ids = new int[] {
			getChangedProblemId(IDelta.NON_ABSTRACT_TO_ABSTRACT)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "AddAbstractNoInstantiate"};
		setExpectedMessageArgs(args);
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
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddAbstractNoExtendNoInstantiate.java");
		// TODO: no problems expected
		int[] ids = new int[] {
			getChangedProblemId(IDelta.NON_ABSTRACT_TO_ABSTRACT)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "AddAbstractNoExtendNoInstantiate"};
		setExpectedMessageArgs(args);
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
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("PublicToPackageVisibility.java");
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_COMPATIBILITY,
						IDelta.CLASS_ELEMENT_TYPE,
						IDelta.REMOVED,
						IDelta.API_TYPE)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "PublicToPackageVisibility", "org.eclipse.api.tools.tests.compatability.a_1.0.0"};
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}
	
	public void testPublicToPackageVisibilityI() throws Exception {
		// TODO: incremental build case does not work - full build does.
		// xPublicToPackageVisibility(true);
	}	
	
	public void testPublicToPackageVisibilityF() throws Exception {
		xPublicToPackageVisibility(false);
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
