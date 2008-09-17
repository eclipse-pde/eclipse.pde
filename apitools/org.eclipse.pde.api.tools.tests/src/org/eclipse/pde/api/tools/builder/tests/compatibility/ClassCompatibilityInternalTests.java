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
import org.eclipse.pde.api.tools.builder.tests.ApiProblem;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * Tests that the builder correctly reports compatibility problems
 * for constructors in classes.
 * 
 * @since 1.0
 */
public class ClassCompatibilityInternalTests extends ClassCompatibilityTests {
	
	/**
	 * Workspace relative path classes in bundle/project A
	 */
	protected static IPath WORKSPACE_CLASSES_PACKAGE_A = new Path("bundle.a/src/a/classes/internal");
	
	protected static IPath WORKSPACE_CLASSES_PACKAGE_HIER = new Path("bundle.a/src/a/classes/hierarchy");

	/**
	 * Package prefix for test classes
	 */
	protected static String PACKAGE_PREFIX = "a.classes.internal.";
	
	/**
	 * Constructor
	 * @param name
	 */
	public ClassCompatibilityInternalTests(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("internal");
	}
	
	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(ClassCompatibilityInternalTests.class);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getDefaultProblemId()
	 */
	protected int getDefaultProblemId() {
		return ApiProblemFactory.createProblemId(
				IApiProblem.CATEGORY_COMPATIBILITY,
				IDelta.CLASS_ELEMENT_TYPE,
				IDelta.REMOVED,
				IDelta.CONSTRUCTOR);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestingProjectName()
	 */
	protected String getTestingProjectName() {
		return "classcompat";
	}
	
	/**
	 * Tests the removal of a constructor from a non-API class.
	 */
	private void xRemoveInternalConstructor(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveInternalConstructor.java");
		// no problems expected
		performCompatibilityTest(filePath, incremental);
	}
	
	public void testRemoveInternalConstructorI() throws Exception {
		xRemoveInternalConstructor(true);
	}
	
	public void testRemoveInternalConstructorF() throws Exception {
		xRemoveInternalConstructor(false);
	}
	
	/**
	 * Tests the removal of a method from a non-API class, subclassed by an API class.
	 */
	private void xRemoveInternalMethod(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveInternalMethod.java");
		// no problems expected on internal method removal
		performCompatibilityTest(filePath, incremental);
		// TODO: should be problem on existing API class
		IPath path = WORKSPACE_CLASSES_PACKAGE_HIER.append("SubclassInternalClass.java");
		ApiProblem[] problems = getEnv().getProblemsFor(path, null);
		assertProblems(problems);
	}
	
	public void testRemoveInternalMethodI() throws Exception {
		xRemoveInternalMethod(true);
	}
	
	public void testRemoveInternalMethodF() throws Exception {
		xRemoveInternalMethod(false);
	}
	
}
