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
 * for type parameters associated with classes.
 *
 * @since 1.0
 */
public class ClassCompatibilityTypeParameterTests extends ClassCompatibilityTests {

	/**
	 * Workspace relative path classes in bundle/project A
	 */
	protected static IPath WORKSPACE_CLASSES_PACKAGE_A = new Path("bundle.a/src/a/classes/typeparameters"); //$NON-NLS-1$

	/**
	 * Package prefix for test classes
	 */
	protected static String PACKAGE_PREFIX = "a.classes.typeparameters."; //$NON-NLS-1$

	public ClassCompatibilityTypeParameterTests(String name) {
		super(name);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("typeparameters"); //$NON-NLS-1$
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(ClassCompatibilityTypeParameterTests.class);
	}

	@Override
	protected int getDefaultProblemId() {
		return -1;
	}

	protected int getProblemId(int kind, int flags) {
		return ApiProblemFactory.createProblemId(
				IApiProblem.CATEGORY_COMPATIBILITY,
				IDelta.CLASS_ELEMENT_TYPE,
				kind,
				flags);
	}

	@Override
	protected String getTestingProjectName() {
		return "classcompat"; //$NON-NLS-1$
	}

	/**
	 * Tests adding a first/single type parameter to a class
	 */
	private void xAddFirstTypeParameter(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddFirstTypeParameter.java"); //$NON-NLS-1$
		// no problems expected
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddFirstTypeParameterI() throws Exception {
		xAddFirstTypeParameter(true);
	}

	public void testAddFirstTypeParameterF() throws Exception {
		xAddFirstTypeParameter(false);
	}

	/**
	 * Tests adding a second type parameter to a class
	 */
	private void xAddSecondaryTypeParameter(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddTypeParameter.java"); //$NON-NLS-1$
		int[] ids = new int[] {
				getProblemId(IDelta.ADDED, IDelta.TYPE_PARAMETER)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "AddTypeParameter", "K"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddSecondaryTypeParameterI() throws Exception {
		xAddSecondaryTypeParameter(true);
	}

	public void testAddSecondaryTypeParameterF() throws Exception {
		xAddSecondaryTypeParameter(false);
	}

	/**
	 * Tests removing a type parameter
	 */
	private void xRemoveTypeParameter(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveTypeParameter.java"); //$NON-NLS-1$
		int[] ids = new int[] {
				getProblemId(IDelta.REMOVED, IDelta.TYPE_PARAMETER)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemoveTypeParameter", "E"}; //$NON-NLS-1$ //$NON-NLS-2$
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
	 * Tests adding a class bound to a type parameter
	 */
	private void xAddClassBound(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddClassBound.java"); //$NON-NLS-1$
		int[] ids = new int[] {
				getProblemId(IDelta.ADDED, IDelta.CLASS_BOUND)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "AddClassBound", "E"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddClassBoundI() throws Exception {
		xAddClassBound(true);
	}

	public void testAddClassBoundF() throws Exception {
		xAddClassBound(false);
	}

	/**
	 * Tests removing a class bound to a type parameter
	 */
	private void xRemoveClassBound(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveClassBound.java"); //$NON-NLS-1$
		int[] ids = new int[] {
				getProblemId(IDelta.REMOVED, IDelta.CLASS_BOUND)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemoveClassBound", "E"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemoveClassBoundI() throws Exception {
		xRemoveClassBound(true);
	}

	public void testRemoveClassBoundF() throws Exception {
		xRemoveClassBound(false);
	}

	/**
	 * Tests adding an interface bound to a type parameter
	 */
	private void xAddInterfaceBound(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddInterfaceBound.java"); //$NON-NLS-1$
		int[] ids = new int[] {
				getProblemId(IDelta.ADDED, IDelta.INTERFACE_BOUND),
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "AddInterfaceBound", "E", PACKAGE_PREFIX + "IBound"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddInterfaceBoundI() throws Exception {
		xAddInterfaceBound(true);
	}

	public void testAddInterfaceBoundF() throws Exception {
		xAddInterfaceBound(false);
	}

	/**
	 * Tests removing an interface bound to a type parameter
	 */
	private void xRemoveInterfaceBound(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveInterfaceBound.java"); //$NON-NLS-1$
		int[] ids = new int[] {
				getProblemId(IDelta.REMOVED, IDelta.INTERFACE_BOUND),
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemoveInterfaceBound", "E", PACKAGE_PREFIX + "IBound"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemoveOnlyInterfaceBoundI() throws Exception {
		xRemoveInterfaceBound(true);
	}

	public void testRemoveOnlyInterfaceBoundF() throws Exception {
		xRemoveInterfaceBound(false);
	}

	/**
	 * Tests removing a secondary interface bound from a type parameter
	 */
	private void xRemoveSecondaryInterfaceBound(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveSecondInterfaceBound.java"); //$NON-NLS-1$
		int[] ids = new int[] {
				getProblemId(IDelta.REMOVED, IDelta.INTERFACE_BOUND)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemoveSecondInterfaceBound", "E", PACKAGE_PREFIX + "IBoundTwo"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemoveSecondaryInterfaceBoundI() throws Exception {
		xRemoveSecondaryInterfaceBound(true);
	}

	public void testRemoveSecondaryInterfaceBoundF() throws Exception {
		xRemoveSecondaryInterfaceBound(false);
	}

	/**
	 * Tests changing a class bound to a type parameter
	 */
	private void xChangeClassBound(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ChangeClassBound.java"); //$NON-NLS-1$
		int[] ids = new int[] {
				getProblemId(IDelta.CHANGED, IDelta.CLASS_BOUND)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "ChangeClassBound", "E"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testChangeClassBoundI() throws Exception {
		xChangeClassBound(true);
	}

	public void testChangeClassBoundF() throws Exception {
		xChangeClassBound(false);
	}

	/**
	 * Tests changing a class bound to a type parameter
	 */
	private void xChangeInterfaceBound(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ChangeInterfaceBound.java"); //$NON-NLS-1$
		int[] ids = new int[] {
				getProblemId(IDelta.CHANGED, IDelta.INTERFACE_BOUND)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "ChangeInterfaceBound", "E", PACKAGE_PREFIX + "IBound"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testChangeInterfaceBoundI() throws Exception {
		xChangeInterfaceBound(true);
	}

	public void testChangeInterfaceBoundF() throws Exception {
		xChangeClassBound(false);
	}

	/**
	 * Tests changing a class to an annotation
	 */
	private void xConvertToAnnotation(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ConvertToAnnotation.java"); //$NON-NLS-1$
		int[] ids = new int[] {
				getProblemId(IDelta.CHANGED, IDelta.TYPE_CONVERSION)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "ConvertToAnnotation", //$NON-NLS-1$
				Integer.toString(IDelta.CLASS_ELEMENT_TYPE),
				Integer.toString(IDelta.ANNOTATION_ELEMENT_TYPE)};
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testConvertToAnnotationI() throws Exception {
		xConvertToAnnotation(true);
	}

	public void testConvertToAnnotationF() throws Exception {
		xConvertToAnnotation(false);
	}

	/**
	 * Tests changing a class to an enum
	 */
	private void xConvertToEnum(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ConvertToEnum.java"); //$NON-NLS-1$
		int[] ids = new int[] {
				getProblemId(IDelta.CHANGED, IDelta.TYPE_CONVERSION)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "ConvertToEnum", //$NON-NLS-1$
				Integer.toString(IDelta.CLASS_ELEMENT_TYPE),
				Integer.toString(IDelta.ENUM_ELEMENT_TYPE)};
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testConvertToEnumI() throws Exception {
		xConvertToEnum(true);
	}

	public void testConvertToEnumF() throws Exception {
		xConvertToEnum(false);
	}

	/**
	 * Tests changing a class to an interface
	 */
	private void xConvertToInterface(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ConvertToInterface.java"); //$NON-NLS-1$
		int[] ids = new int[] {
				getProblemId(IDelta.CHANGED, IDelta.TYPE_CONVERSION)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "ConvertToInterface", //$NON-NLS-1$
				Integer.toString(IDelta.CLASS_ELEMENT_TYPE),
				Integer.toString(IDelta.INTERFACE_ELEMENT_TYPE)};
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testConvertToInterfaceI() throws Exception {
		xConvertToInterface(true);
	}

	public void testConvertToInterfaceF() throws Exception {
		xConvertToInterface(false);
	}
}
