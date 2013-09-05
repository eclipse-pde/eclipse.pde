/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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
 * Tests that the builder correctly finds and reports problems with annotation
 * compatibility
 * 
 * @since 1.0
 */
public class AnnotationCompatibilityTests extends CompatibilityTest {

	/**
	 * Workspace relative path classes in bundle/project A
	 */
	protected static IPath WORKSPACE_CLASSES_PACKAGE_A = new Path("bundle.a/src/a/annotations"); //$NON-NLS-1$

	/**
	 * Package prefix for test classes
	 */
	protected static String PACKAGE_PREFIX = "a.annotations."; //$NON-NLS-1$

	/**
	 * Constructor
	 * 
	 * @param name
	 */
	public AnnotationCompatibilityTests(String name) {
		super(name);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath
	 * ()
	 */
	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("annotations"); //$NON-NLS-1$
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(AnnotationCompatibilityTests.class);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getDefaultProblemId
	 * ()
	 */
	@Override
	protected int getDefaultProblemId() {
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestingProjectName
	 * ()
	 */
	@Override
	protected String getTestingProjectName() {
		return "annotcompat"; //$NON-NLS-1$
	}

	/**
	 * Tests adding a method with a default value
	 */
	private void xAddMethodwDef(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddMethodwDef.java"); //$NON-NLS-1$
		// no problems expected
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddMethodwDefI() throws Exception {
		xAddMethodwDef(true);
	}

	public void testAddMethodwDefF() throws Exception {
		xAddMethodwDef(false);
	}

	/**
	 * Tests adding a method without a default value
	 */
	private void xAddMethodwoDef(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddMethodwoDef.java"); //$NON-NLS-1$
		int[] ids = new int[] { ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY, IDelta.ANNOTATION_ELEMENT_TYPE, IDelta.ADDED, IDelta.METHOD_WITHOUT_DEFAULT_VALUE) };
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[] { PACKAGE_PREFIX + "AddMethodwoDef", "method()" }; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddMethodwoDefI() throws Exception {
		xAddMethodwoDef(true);
	}

	public void testAddMethodwoDefF() throws Exception {
		xAddMethodwoDef(false);
	}

	/**
	 * Tests removing a default value
	 */
	private void xRemoveDefValue(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveDefValue.java"); //$NON-NLS-1$
		int[] ids = new int[] { ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY, IDelta.METHOD_ELEMENT_TYPE, IDelta.REMOVED, IDelta.ANNOTATION_DEFAULT_VALUE) };
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[] { PACKAGE_PREFIX + "RemoveDefValue", "method()" }; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemoveDefValueI() throws Exception {
		xRemoveDefValue(true);
	}

	public void testRemoveDefValueF() throws Exception {
		xRemoveDefValue(false);
	}

	/**
	 * Tests removing a member type
	 */
	private void xRemoveMemberType(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveMemberType.java"); //$NON-NLS-1$
		int[] ids = new int[] { ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY, IDelta.ANNOTATION_ELEMENT_TYPE, IDelta.REMOVED, IDelta.TYPE_MEMBER) };
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[] {
				PACKAGE_PREFIX + "RemoveMemberType.MemberType", //$NON-NLS-1$
				"bundle.a_1.0.0" }; //$NON-NLS-1$
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
	 * Tests removing a method
	 */
	private void xRemoveMethod(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveMethod.java"); //$NON-NLS-1$
		int[] ids = new int[] { ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY, IDelta.ANNOTATION_ELEMENT_TYPE, IDelta.REMOVED, IDelta.METHOD_WITHOUT_DEFAULT_VALUE) };
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[] { PACKAGE_PREFIX + "RemoveMethod", "method()" }; //$NON-NLS-1$ //$NON-NLS-2$
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
	 * Tests removing a field
	 */
	private void xRemoveField(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveField.java"); //$NON-NLS-1$
		int[] ids = new int[] { ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY, IDelta.ANNOTATION_ELEMENT_TYPE, IDelta.REMOVED, IDelta.FIELD) };
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[] { PACKAGE_PREFIX + "RemoveField", "FIELD" }; //$NON-NLS-1$ //$NON-NLS-2$
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
	 * Tests conversion to a class
	 */
	private void xConvertToClass(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ToClass.java"); //$NON-NLS-1$
		int[] ids = new int[] { ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY, IDelta.ANNOTATION_ELEMENT_TYPE, IDelta.CHANGED, IDelta.TYPE_CONVERSION) };
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[] {
				PACKAGE_PREFIX + "ToClass", //$NON-NLS-1$
				Integer.toString(IDelta.ANNOTATION_ELEMENT_TYPE),
				Integer.toString(IDelta.CLASS_ELEMENT_TYPE) };
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testConvertToClassI() throws Exception {
		xConvertToClass(true);
	}

	public void testConvertToClassF() throws Exception {
		xConvertToClass(false);
	}

	/**
	 * Tests conversion to a enum
	 */
	private void xConvertToEnum(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ToEnum.java"); //$NON-NLS-1$
		int[] ids = new int[] { ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY, IDelta.ANNOTATION_ELEMENT_TYPE, IDelta.CHANGED, IDelta.TYPE_CONVERSION) };
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[] {
				PACKAGE_PREFIX + "ToEnum", //$NON-NLS-1$
				Integer.toString(IDelta.ANNOTATION_ELEMENT_TYPE),
				Integer.toString(IDelta.ENUM_ELEMENT_TYPE) };
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
	 * Tests conversion to an interface
	 */
	private void xConvertToInterface(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("ToInterface.java"); //$NON-NLS-1$
		int[] ids = new int[] { ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY, IDelta.ANNOTATION_ELEMENT_TYPE, IDelta.CHANGED, IDelta.TYPE_CONVERSION) };
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[] {
				PACKAGE_PREFIX + "ToInterface", //$NON-NLS-1$
				Integer.toString(IDelta.ANNOTATION_ELEMENT_TYPE),
				Integer.toString(IDelta.INTERFACE_ELEMENT_TYPE) };
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
