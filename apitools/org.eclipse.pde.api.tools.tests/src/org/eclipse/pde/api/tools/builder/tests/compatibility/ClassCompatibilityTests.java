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
 * for classes.
 * 
 * @since 3.4
 */
public class ClassCompatibilityTests extends CompatibilityTest {
	
	/**
	 * Workspace relative path classes in bundle/project A
	 */
	protected static IPath WORKSPACE_CLASSES_PACKAGE_A = new Path("org.eclipse.api.tools.tests.compatability.a/src/a/classes");

	/**
	 * Constructor
	 * @param name
	 */
	public ClassCompatibilityTests(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("class");
	}
	
	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(ClassCompatibilityTests.class);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getDefaultProblemId()
	 */
	protected int getDefaultProblemId() {
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestingProjectName()
	 */
	protected String getTestingProjectName() {
		return "classcompat";
	}
	
	/**
	 * Tests the removal of a public method from an API class - incremental.
	 */
	private void xRemovePublicAPIMethod(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemovePublicMethod.java");
		int[] ids = new int[] {
			ApiProblemFactory.createProblemId(
				IApiProblem.CATEGORY_COMPATIBILITY,
				IDelta.CLASS_ELEMENT_TYPE,
				IDelta.REMOVED,
				IDelta.METHOD)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{"a.classes.RemovePublicMethod", "publicMethod(String)"};
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}
	
	/**
	 * Tests the removal of a public method from an API class - incremental.
	 */
	public void testRemovePublicAPIMethodI() throws Exception {
		xRemovePublicAPIMethod(true);
	}	
	
	/**
	 * Tests the removal of a public method from an API class - full.
	 */
	public void testRemovePublicAPIMethodF() throws Exception {
		xRemovePublicAPIMethod(false);
	}
	
	/**
	 * Tests the removal of a protected method from an API class.
	 */
	private void xRemoveProtectedAPIMethod(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveProtectedMethod.java");
		int[] ids = new int[] {
			ApiProblemFactory.createProblemId(
				IApiProblem.CATEGORY_COMPATIBILITY,
				IDelta.CLASS_ELEMENT_TYPE,
				IDelta.REMOVED,
				IDelta.METHOD)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{"a.classes.RemoveProtectedMethod", "protectedMethod(String)"};
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}
	
	/**
	 * Tests the removal of a protected method from an API class - incremental.
	 */
	public void testRemoveProtectedAPIMethodI() throws Exception {
		xRemoveProtectedAPIMethod(true);
	}	
	
	/**
	 * Tests the removal of a protected method from an API class - full.
	 */
	public void testRemoveProtectedAPIMethodF() throws Exception {
		xRemoveProtectedAPIMethod(false);
	}
	
	/**
	 * Tests the removal of a private method from an API class.
	 */
	private void xRemovePrivateAPIMethod(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemovePrivateMethod.java");
		// there are no expected problems
		performCompatibilityTest(filePath, incremental);
	}
	
	/**
	 * Tests the removal of a protected method from an API class - incremental.
	 */
	public void testRemovePrivateAPIMethodI() throws Exception {
		xRemovePrivateAPIMethod(true);
	}	
	
	/**
	 * Tests the removal of a protected method from an API class - full.
	 */
	public void testRemovePrivateAPIMethodF() throws Exception {
		xRemovePrivateAPIMethod(false);
	}	
}
