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
 * related to member types.
 * 
 * @since 1.0
 */
public class InterfaceCompatibilityRestrictionTests extends InterfaceCompatibilityTests {
	
	/**
	 * Workspace relative path classes in bundle/project A
	 */
	protected static IPath WORKSPACE_CLASSES_PACKAGE_A = new Path("bundle.a/src/a/interfaces/restrictions");

	/**
	 * Package prefix for test classes
	 */
	protected static String PACKAGE_PREFIX = "a.interfaces.restrictions.";
	
	/**
	 * Constructor
	 * @param name
	 */
	public InterfaceCompatibilityRestrictionTests(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("restrictions");
	}
	
	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(InterfaceCompatibilityRestrictionTests.class);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getDefaultProblemId()
	 */
	@Override
	protected int getDefaultProblemId() {
		return ApiProblemFactory.createProblemId(
				IApiProblem.CATEGORY_COMPATIBILITY,
				IDelta.INTERFACE_ELEMENT_TYPE,
				IDelta.ADDED,
				IDelta.RESTRICTIONS);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestingProjectName()
	 */
	protected String getTestingProjectName() {
		return "intercompat";
	}
	
	public void testRemoveNoImplementI() throws Exception {
		xRemoveNoImplement(true);
	}	
	
	public void testRemoveNoImplementF() throws Exception {
		xRemoveNoImplement(false);
	}
	/**
	 * Tests removing a noimplement annotation
	 */
	private void xRemoveNoImplement(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveNoImplement.java");
		// no problem expected
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddNoImplementI() throws Exception {
		xAddNoImplement(true);
	}	
	
	public void testAddNoImplementF() throws Exception {
		xAddNoImplement(false);
	}
	/**
	 * Test adding a noimplement annotation
	 */
	private void xAddNoImplement(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddNoImplement.java");
		int[] ids = new int[] {
				getDefaultProblemId()
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "AddNoImplement"};
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}
}
