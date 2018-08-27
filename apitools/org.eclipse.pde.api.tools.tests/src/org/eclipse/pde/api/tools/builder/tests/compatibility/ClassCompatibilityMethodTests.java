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
 * for classes.
 *
 * @since 31.0
 */
public class ClassCompatibilityMethodTests extends ClassCompatibilityTests {

	/**
	 * Workspace relative path classes in bundle/project A
	 */
	protected static IPath WORKSPACE_CLASSES_PACKAGE_A = new Path("bundle.a/src/a/classes/methods"); //$NON-NLS-1$

	/**
	 * Package prefix for test classes
	 */
	protected static String PACKAGE_PREFIX = "a.classes.methods."; //$NON-NLS-1$

	public ClassCompatibilityMethodTests(String name) {
		super(name);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("methods"); //$NON-NLS-1$
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(ClassCompatibilityMethodTests.class);
//		junit.framework.TestSuite suite = new junit.framework.TestSuite();
//		suite.addTest(new ClassCompatibilityMethodTests("testAddNooverrideRemoveNoextendI"));
//		suite.addTest(new ClassCompatibilityMethodTests("testAddNooverrideRemoveNoextendF"));
//		return suite;
	}

	@Override
	protected int getDefaultProblemId() {
		return ApiProblemFactory.createProblemId(
				IApiProblem.CATEGORY_COMPATIBILITY,
				IDelta.CLASS_ELEMENT_TYPE,
				IDelta.REMOVED,
				IDelta.METHOD);
	}

	@Override
	protected String getTestingProjectName() {
		return "classcompat"; //$NON-NLS-1$
	}

	/**
	 * Tests the removal of a public method from an API class.
	 */
	private void xRemovePublicAPIMethod(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemovePublicMethod.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getDefaultProblemId()
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemovePublicMethod", "publicMethod(String)"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemovePublicAPIMethodI() throws Exception {
		xRemovePublicAPIMethod(true);
	}

	public void testRemovePublicAPIMethodF() throws Exception {
		xRemovePublicAPIMethod(false);
	}

	public void testRemoveTwoPublicAPIMethodsI() throws Exception {
		xRemoveTwoPublicAPIMethods(true);
	}

	public void testRemoveTwoPublicAPIMethodsF() throws Exception {
		xRemoveTwoPublicAPIMethods(false);
	}

	public void testAddNooverrideRemoveNoextendI() throws Exception {
		xAddNooverrideRemoveNoextendI(true);
	}

	public void testAddNooverrideRemoveNoextendF() throws Exception {
		xAddNooverrideRemoveNoextendI(false);
	}

	/**
	 * Tests the removal of a public methods from an API class - incremental.
	 */
	private void xAddNooverrideRemoveNoextendI(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddNooverrideRemoveNoextend.java"); //$NON-NLS-1$
		performCompatibilityTest(filePath, incremental);
	}
	/**
	 * Tests the removal of a public methods from an API class - incremental.
	 */
	private void xRemoveTwoPublicAPIMethods(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveTwoPublicMethods.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getDefaultProblemId(),
			getDefaultProblemId()
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[2][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemoveTwoPublicMethods", "methodOne(String)"}; //$NON-NLS-1$ //$NON-NLS-2$
		args[1] = new String[]{PACKAGE_PREFIX + "RemoveTwoPublicMethods", "methodTwo(int)"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	/**
	 * Tests the removal of a protected method from an API class.
	 */
	private void xRemoveProtectedAPIMethod(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveProtectedMethod.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getDefaultProblemId()
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemoveProtectedMethod", "protectedMethod(String)"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemoveProtectedAPIMethodI() throws Exception {
		xRemoveProtectedAPIMethod(true);
	}

	public void testRemoveProtectedAPIMethodF() throws Exception {
		xRemoveProtectedAPIMethod(false);
	}

	/**
	 * Tests the removal of a private method from an API class.
	 */
	private void xRemovePrivateAPIMethod(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemovePrivateMethod.java"); //$NON-NLS-1$
		// there are no expected problems
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemovePrivateAPIMethodI() throws Exception {
		xRemovePrivateAPIMethod(true);
	}

	public void testRemovePrivateAPIMethodF() throws Exception {
		xRemovePrivateAPIMethod(false);
	}

	/**
	 * Tests the removal of a package protected method from an API class.
	 */
	private void xRemovePackageMethod(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemovePackageMethod.java"); //$NON-NLS-1$
		// there are no expected problems
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemovePackageMethodI() throws Exception {
		xRemovePackageMethod(true);
	}

	public void testRemovePackageMethodF() throws Exception {
		xRemovePackageMethod(false);
	}

	/**
	 * Tests the removal of a public method from an API class annotated as noextend - incremental.
	 */
	private void xRemovePublicAPIMethodNoExtend(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemovePublicMethodNoExtend.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getDefaultProblemId()
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemovePublicMethodNoExtend", "publicMethod(String)"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemovePublicAPIMethodNoExtendI() throws Exception {
		xRemovePublicAPIMethodNoExtend(true);
	}

	public void testRemovePublicAPIMethodNoExtendF() throws Exception {
		xRemovePublicAPIMethodNoExtend(false);
	}

	/**
	 * Tests the removal of a protected method from an API class annotated as noextend.
	 */
	private void xRemoveProtectedAPIMethodNoExtend(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveProtectedMethodNoExtend.java"); //$NON-NLS-1$
		// no problems expected since the method is not accessible
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemoveProtectedAPIMethodNoExtendI() throws Exception {
		xRemoveProtectedAPIMethodNoExtend(true);
	}

	public void testRemoveProtectedAPIMethodNoExtendF() throws Exception {
		xRemoveProtectedAPIMethodNoExtend(false);
	}


	/**
	 * Tests the removal of a protected method from an API class annotated as noextend.
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=261176
	 */
	private void xRemoveProtectedAPIMethodNoExtend2(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveProtectedMethodNoExtend2.java"); //$NON-NLS-1$
		// no problems expected since the method is not accessible
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemoveProtectedAPIMethodNoExtend2I() throws Exception {
		xRemoveProtectedAPIMethodNoExtend2(true);
	}

	public void testRemoveProtectedAPIMethodNoExtend2F() throws Exception {
		xRemoveProtectedAPIMethodNoExtend2(false);
	}

	/**
	 * Tests the removal of a public method from an API class annotated as noinstantiate - incremental.
	 */
	private void xRemovePublicAPIMethodNoInstantiate(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemovePublicMethodNoInstantiate.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getDefaultProblemId()
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemovePublicMethodNoInstantiate", "publicMethod(String)"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemovePublicAPIMethodNoInstantiateI() throws Exception {
		xRemovePublicAPIMethodNoInstantiate(true);
	}

	public void testRemovePublicAPIMethodNoInstantiateF() throws Exception {
		xRemovePublicAPIMethodNoInstantiate(false);
	}

	/**
	 * Tests the removal of a protected method from an API class annotated as noinstantiate.
	 */
	private void xRemoveProtectedAPIMethodNoInstantiate(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveProtectedMethodNoInstantiate.java"); //$NON-NLS-1$
		int[] ids = new int[] {
				getDefaultProblemId()
			};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemoveProtectedMethodNoInstantiate", "protectedMethod(String)"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemoveProtectedAPIMethodNoInstantiateI() throws Exception {
		xRemoveProtectedAPIMethodNoInstantiate(true);
	}

	public void testRemoveProtectedAPIMethodNoInstantiateF() throws Exception {
		xRemoveProtectedAPIMethodNoInstantiate(false);
	}

	/**
	 * Tests the removal of a public method from an API class annotated as
	 * noextend and noinstantiate.
	 */
	private void xRemovePublicAPIMethodNoExtendNoInstatiate(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemovePublicMethodNoExtendNoInstantiate.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			getDefaultProblemId()
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemovePublicMethodNoExtendNoInstantiate", "publicMethod(String)"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemovePublicAPIMethodNoExtendNoInstantiateI() throws Exception {
		xRemovePublicAPIMethodNoExtendNoInstatiate(true);
	}

	public void testRemovePublicAPIMethodNoExtendNoInstantiateF() throws Exception {
		xRemovePublicAPIMethodNoExtendNoInstatiate(false);
	}

	/**
	 * Tests the removal of a protected method from an API class annotated as
	 * noextend and noinstantiate.
	 */
	private void xRemoveProtectedAPIMethodNoExtendNoInstatiate(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveProtectedMethodNoExtendNoInstantiate.java"); //$NON-NLS-1$
		// no problems expected due to noextend
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemoveProtectedAPIMethodNoExtendNoInstantiateI() throws Exception {
		xRemoveProtectedAPIMethodNoExtendNoInstatiate(true);
	}

	public void testRemoveProtectedAPIMethodNoExtendNoInstantiateF() throws Exception {
		xRemoveProtectedAPIMethodNoExtendNoInstatiate(false);
	}

	/**
	 * Tests the removal of a public method from an API class tagged noreference.
	 */
	private void xRemovePublicAPIMethodNoReference(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemovePublicMethodNoReference.java"); //$NON-NLS-1$
		// no problems since no references allowed
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemovePublicAPIMethodNoReferenceI() throws Exception {
		xRemovePublicAPIMethodNoReference(true);
	}

	public void testRemovePublicAPIMethodNoReferencF() throws Exception {
		xRemovePublicAPIMethodNoReference(false);
	}

	/**
	 * Tests the removal of a protected method from an API class tagged noreference.
	 */
	private void xRemoveProtectedAPIMethodNoReference(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveProtectedMethodNoReference.java"); //$NON-NLS-1$
		// no problems since no references allowed
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemoveProtectedAPIMethodNoReferenceI() throws Exception {
		xRemoveProtectedAPIMethodNoReference(true);
	}

	public void testRemoveProtectedAPIMethodNoReferencF() throws Exception {
		xRemoveProtectedAPIMethodNoReference(false);
	}

	/**
	 * Tests the removal of a public method from an API class tagged no override.
	 */
	private void xRemovePublicAPIMethodNoOverride(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemovePublicMethodNoOverride.java"); //$NON-NLS-1$
		int[] ids = new int[] {
				getDefaultProblemId()
			};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemovePublicMethodNoOverride", "publicMethod(String)"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemovePublicAPIMethodNoOverrideI() throws Exception {
		xRemovePublicAPIMethodNoOverride(true);
	}

	public void testRemovePublicAPIMethodNoOverrideF() throws Exception {
		xRemovePublicAPIMethodNoOverride(false);
	}

	/**
	 * Tests the removal of a protected method from an API class tagged no override.
	 */
	private void xRemoveProtectedAPIMethodNoOverride(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveProtectedMethodNoOverride.java"); //$NON-NLS-1$
		int[] ids = new int[] {
				getDefaultProblemId()
			};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemoveProtectedMethodNoOverride", "protectedMethod(String)"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testRemoveProtectedAPIMethodNoOverrideI() throws Exception {
		xRemoveProtectedAPIMethodNoOverride(true);
	}

	public void testRemoveProtectedAPIMethodNoOverrideF() throws Exception {
		xRemoveProtectedAPIMethodNoOverride(false);
	}

	/**
	 * Tests the addition of a private method in an API class.
	 */
	private void xAddPrivateAPIMethod(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddPrivateMethod.java"); //$NON-NLS-1$
		// there are no expected problems
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddPrivateAPIMethodI() throws Exception {
		xAddPrivateAPIMethod(true);
	}

	public void testAddPrivateAPIMethodF() throws Exception {
		xAddPrivateAPIMethod(false);
	}

	/**
	 * Tests the addition of a protected method in an API class.
	 */
	private void xAddProtectedAPIMethod(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddProtectedMethod.java"); //$NON-NLS-1$
		// there are no expected problems
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddProtectedAPIMethodI() throws Exception {
		xAddProtectedAPIMethod(true);
	}

	public void testAddProtectedAPIMethodF() throws Exception {
		xAddProtectedAPIMethod(false);
	}

	/**
	 * Tests the addition of a protected method in an API class.
	 */
	private void xAddPublicAPIMethod(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddPublicMethod.java"); //$NON-NLS-1$
		// there are no expected problems
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddPublicAPIMethodI() throws Exception {
		xAddPublicAPIMethod(true);
	}

	public void testAddPublicAPIMethodF() throws Exception {
		xAddPublicAPIMethod(false);
	}

	/**
	 * Tests the addition of an abstract method in an API class that can be extended
	 */
	private void xAddAbstractMethod(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddAbstractMethod.java"); //$NON-NLS-1$
		int[] ids = new int[] {
			ApiProblemFactory.createProblemId(
				IApiProblem.CATEGORY_COMPATIBILITY,
				IDelta.CLASS_ELEMENT_TYPE,
				IDelta.ADDED,
				IDelta.METHOD)
			};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "AddAbstractMethod", "method()"}; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddAbstractMethodI() throws Exception {
		xAddAbstractMethod(true);
	}

	public void testAddAbstractMethodF() throws Exception {
		xAddAbstractMethod(false);
	}

	/**
	 * Tests the addition of an abstract method in an API class that *cannot* be extended
	 */
	private void xAddAbstractMethodNoExtend(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddAbstractMethodNoExtend.java"); //$NON-NLS-1$
		// no problems
		performCompatibilityTest(filePath, incremental);
	}

	public void testAddAbstractMethodNoExtendI() throws Exception {
		xAddAbstractMethodNoExtend(true);
	}

	public void testAddAbstractMethodNoExtendF() throws Exception {
		xAddAbstractMethodNoExtend(false);
	}
}
