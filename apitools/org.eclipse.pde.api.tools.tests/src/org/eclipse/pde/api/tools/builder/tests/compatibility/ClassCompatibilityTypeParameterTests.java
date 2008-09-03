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
 * for type parameters associated with classes.
 * 
 * @since 3.4
 */
public class ClassCompatibilityTypeParameterTests extends ClassCompatibilityTests {
	
	/**
	 * Workspace relative path classes in bundle/project A
	 */
	protected static IPath WORKSPACE_CLASSES_PACKAGE_A = new Path("bundle.a/src/a/classes/typeparameters");

	/**
	 * Package prefix for test classes
	 */
	protected static String PACKAGE_PREFIX = "a.classes.typeparameters.";
	
	/**
	 * Constructor
	 * @param name
	 */
	public ClassCompatibilityTypeParameterTests(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("typeparameters");
	}
	
	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(ClassCompatibilityTypeParameterTests.class);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getDefaultProblemId()
	 */
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestingProjectName()
	 */
	protected String getTestingProjectName() {
		return "classcompat";
	}
	
	/**
	 * Tests adding a first/single type parameter to a class
	 */
	private void xAddFirstTypeParameter(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddFirstTypeParameter.java");
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
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddTypeParameter.java");
		int[] ids = new int[] {
			getProblemId(IDelta.ADDED, IDelta.TYPE_PARAMETER)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "AddTypeParameter", "K"};
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
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveTypeParameter.java");
		int[] ids = new int[] {
			getProblemId(IDelta.REMOVED, IDelta.TYPE_PARAMETER)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemoveTypeParameter", "E"};
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
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddClassBound.java");
		// TODO: should be ADDED instead of CHANGED
		int[] ids = new int[] {
				getProblemId(IDelta.CHANGED, IDelta.CLASS_BOUND)
			};
			setExpectedProblemIds(ids);
			String[][] args = new String[1][];
			args[0] = new String[]{PACKAGE_PREFIX + "AddClassBound", "E"};
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
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveClassBound.java");
		// TODO: should be removed instead of CHANGED
		int[] ids = new int[] {
				getProblemId(IDelta.CHANGED, IDelta.CLASS_BOUND)
			};
			setExpectedProblemIds(ids);
			String[][] args = new String[1][];
			args[0] = new String[]{PACKAGE_PREFIX + "RemoveClassBound", "E"};
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
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddInterfaceBound.java");
		int[] ids = new int[] {
				getProblemId(IDelta.ADDED, IDelta.INTERFACE_BOUND),
				getProblemId(IDelta.REMOVED, IDelta.CLASS_BOUND) // TODO: should only be added interface
			};
			setExpectedProblemIds(ids);
			String[][] args = new String[2][];
			args[0] = new String[]{PACKAGE_PREFIX + "AddInterfaceBound", "E", PACKAGE_PREFIX + "IBound"};
			args[1] = new String[]{PACKAGE_PREFIX + "AddInterfaceBound", "E"}; // TODO: remove 
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
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveInterfaceBound.java");
		int[] ids = new int[] {
				getProblemId(IDelta.REMOVED, IDelta.INTERFACE_BOUND), 
				getProblemId(IDelta.ADDED, IDelta.CLASS_BOUND) // TODO: should only be removed interface
			};
			setExpectedProblemIds(ids);
			String[][] args = new String[2][];
			args[0] = new String[]{PACKAGE_PREFIX + "RemoveInterfaceBound", "E", PACKAGE_PREFIX + "IBound"};
			args[1] = new String[]{PACKAGE_PREFIX + "RemoveInterfaceBound", "E"}; // TODO: remove
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
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveSecondInterfaceBound.java");
		int[] ids = new int[] {
				getProblemId(IDelta.REMOVED, IDelta.INTERFACE_BOUND)
			};
			setExpectedProblemIds(ids);
			String[][] args = new String[1][];
			args[0] = new String[]{PACKAGE_PREFIX + "RemoveSecondInterfaceBound", "E", PACKAGE_PREFIX + "IBoundTwo"};
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
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ChangeClassBound.java");
		int[] ids = new int[] {
				getProblemId(IDelta.CHANGED, IDelta.CLASS_BOUND)
			};
			setExpectedProblemIds(ids);
			String[][] args = new String[1][];
			args[0] = new String[]{PACKAGE_PREFIX + "ChangeClassBound", "E"};
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
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ChangeInterfaceBound.java");
		int[] ids = new int[] {
				getProblemId(IDelta.CHANGED, IDelta.INTERFACE_BOUND)
			};
			setExpectedProblemIds(ids);
			String[][] args = new String[1][];
			args[0] = new String[]{PACKAGE_PREFIX + "ChangeInterfaceBound", "E", PACKAGE_PREFIX + "IBound"};
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
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ConvertToAnnotation.java");
		int[] ids = new int[] {
				getProblemId(IDelta.CHANGED, IDelta.TYPE_CONVERSION)
			};
			setExpectedProblemIds(ids);
			String[][] args = new String[1][];
			args[0] = new String[]{PACKAGE_PREFIX + "ConvertToAnnotation",
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
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ConvertToEnum.java");
		int[] ids = new int[] {
				getProblemId(IDelta.CHANGED, IDelta.TYPE_CONVERSION)
			};
			setExpectedProblemIds(ids);
			String[][] args = new String[1][];
			args[0] = new String[]{PACKAGE_PREFIX + "ConvertToEnum",
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
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ConvertToInterface.java");
		int[] ids = new int[] {
				getProblemId(IDelta.CHANGED, IDelta.TYPE_CONVERSION)
			};
			setExpectedProblemIds(ids);
			String[][] args = new String[1][];
			args[0] = new String[]{PACKAGE_PREFIX + "ConvertToInterface",
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
