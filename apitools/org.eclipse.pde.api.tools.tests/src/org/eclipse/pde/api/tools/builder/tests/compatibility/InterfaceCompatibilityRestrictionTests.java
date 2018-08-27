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
public class InterfaceCompatibilityRestrictionTests extends InterfaceCompatibilityTests {

	/**
	 * Workspace relative path classes in bundle/project A
	 */
	protected static IPath WORKSPACE_CLASSES_PACKAGE_A = new Path("bundle.a/src/a/interfaces/restrictions"); //$NON-NLS-1$

	/**
	 * Package prefix for test classes
	 */
	protected static String PACKAGE_PREFIX = "a.interfaces.restrictions."; //$NON-NLS-1$

	/**
	 * Constructor
	 * @param name
	 */
	public InterfaceCompatibilityRestrictionTests(String name) {
		super(name);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("restrictions"); //$NON-NLS-1$
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(InterfaceCompatibilityRestrictionTests.class);
	}

	@Override
	protected int getDefaultProblemId() {
		return ApiProblemFactory.createProblemId(
				IApiProblem.CATEGORY_COMPATIBILITY,
				IDelta.INTERFACE_ELEMENT_TYPE,
				IDelta.ADDED,
				IDelta.RESTRICTIONS);
	}

	@Override
	protected String getTestingProjectName() {
		return "intercompat"; //$NON-NLS-1$
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
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveNoImplement.java"); //$NON-NLS-1$
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
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddNoImplement.java"); //$NON-NLS-1$
		int[] ids = new int[] {
				getDefaultProblemId()
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "AddNoImplement"}; //$NON-NLS-1$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	/**
	 * Tests removing a noextend annotation using an incremental build
	 */
	public void testRemoveNoExtendI() {
		xRemoveNoExtend(true);
	}

	/**
	 * Tests removing a noextend annotation using a full build
	 */
	public void testRemoveNoExtendF() {
		xRemoveNoExtend(false);
	}

	private void xRemoveNoExtend(boolean inc) {
		try {
			IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveNoExtend.java"); //$NON-NLS-1$
			// no problem expected
			performCompatibilityTest(filePath, inc);
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Tests adding a noextend annotation using an incremental build
	 */
	public void testAddNoExtendI() {
		xAddNoExtend(true);
	}

	/**
	 * Tests adding a noextend annotation using a full build
	 */
	public void testAddNoExtendF() {
		xAddNoExtend(false);
	}

	private void xAddNoExtend(boolean inc) {
		try {
			IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddNoExtend.java"); //$NON-NLS-1$
			int[] ids = new int[] {
					getDefaultProblemId()
			};
			setExpectedProblemIds(ids);
			String[][] args = new String[1][];
			args[0] = new String[]{PACKAGE_PREFIX + "AddNoExtend"}; //$NON-NLS-1$
			setExpectedMessageArgs(args);
			performCompatibilityTest(filePath, inc);
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Tests adding both noextend and noimplement annotations using an incremental build
	 */
	public void testAddNoExtendNoImplementI() {
		xAddNoExtendNoImplement(true);
	}

	/**
	 * Tests adding both noextend and noimplement annotations using a full build
	 */
	public void testAddNoExtendNoImplementF() {
		xAddNoExtendNoImplement(false);
	}

	private void xAddNoExtendNoImplement(boolean inc) {
		try {
			IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddNoExtendNoImplement.java"); //$NON-NLS-1$
			int[] ids = new int[] {
					getDefaultProblemId()
			};
			setExpectedProblemIds(ids);
			String[][] args = new String[1][];
			args[0] = new String[]{PACKAGE_PREFIX + "AddNoExtendNoImplement"}; //$NON-NLS-1$
			setExpectedMessageArgs(args);
			performCompatibilityTest(filePath, inc);
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Tests removing both noextend and noimplement annotations using an incremental build
	 */
	public void testRemoveNoExtendNoImplementI() {
		xRemoveNoExtendNoImplement(true);
	}

	/**
	 * Tests removing both noextend and noimplement annotations using a full build
	 */
	public void testRemoveNoExtendNoImplementF() {
		xRemoveNoExtendNoImplement(false);
	}

	private void xRemoveNoExtendNoImplement(boolean inc) {
		try {
			IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveNoExtendNoImplement.java"); //$NON-NLS-1$
			// no problem expected
			performCompatibilityTest(filePath, inc);
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
	}
}
