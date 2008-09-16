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
 * Tests that the builder correctly finds and reports problems with 
 * API bundles.
 * 
 * @since 1.0
 */
public class BundleCompatibilityTests extends CompatibilityTest {
	
	/**
	 * Workspace relative path classes in bundle/project A
	 */
	protected static IPath WORKSPACE_CLASSES_PACKAGE_A = new Path("bundle.a/src/a/bundles");
	protected static IPath WORKSPACE_CLASSES_PACKAGE_INTERNAL = new Path("bundle.a/src/a/bundles/internal");

	/**
	 * Package prefix for test classes
	 */
	protected static String PACKAGE_PREFIX = "a.bundles.";	

	/**
	 * Constructor
	 * @param name
	 */
	public BundleCompatibilityTests(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("bundles");
	}
	
	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(BundleCompatibilityTests.class);
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
		return "bundlecompat";
	}
	
	/**
	 * Tests reducing visibility from public to package
	 */
	private void xPublicToPackage(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("PublicToPackageVisibility.java");
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_COMPATIBILITY,
						IDelta.API_COMPONENT_ELEMENT_TYPE,
						IDelta.REMOVED,
						IDelta.API_TYPE)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "PublicToPackageVisibility", "bundle.a_1.0.0"};
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}
	
	public void testPublicToPackageI() throws Exception {
		xPublicToPackage(true);
	}
	
	public void testPublicToPackageF() throws Exception {
		xPublicToPackage(false);
	}
	
	/**
	 * Tests deleting a public class
	 */
	private void xRemovePublicClass(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemovePublicClass.java");
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_COMPATIBILITY,
						IDelta.API_COMPONENT_ELEMENT_TYPE,
						IDelta.REMOVED,
						IDelta.TYPE)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemovePublicClass", "bundle.a_1.0.0"};
		setExpectedMessageArgs(args);
		performDeletionCompatibilityTest(filePath, incremental);
	}
	
	public void testRemovePublicClassI() throws Exception {
		xRemovePublicClass(true);
	}
	
	public void testRemovePublicClassF() throws Exception {
		xRemovePublicClass(false);
	}
	
	/**
	 * Tests deleting a private class
	 */
	private void xRemovePrivateClass(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_INTERNAL.append("RemovePrivateClass.java");
		// no problem expected
		performDeletionCompatibilityTest(filePath, incremental);
	}
	
	public void testRemovePrivateClassI() throws Exception {
		xRemovePrivateClass(true);
	}
	
	public void testRemovePrivateClassF() throws Exception {
		xRemovePrivateClass(false);
	}
	
	/**
	 * Tests deleting a public class and then replacing it
	 */
	private void xDeleteAndReplace(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("DeleteAndReplace.java");
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_COMPATIBILITY,
						IDelta.API_COMPONENT_ELEMENT_TYPE,
						IDelta.REMOVED,
						IDelta.TYPE)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "DeleteAndReplace", "bundle.a_1.0.0"};
		setExpectedMessageArgs(args);
		performDeletionCompatibilityTest(filePath, incremental);
		// now replace the class - no problems expected
		setExpectedProblemIds(null);
		setExpectedMessageArgs(null);
		performCreationCompatibilityTest(filePath, incremental);
	}
	
	public void testDeleteAndReplaceI() throws Exception {
		//TODO: Add this test when bug 247545 is fixed
		//xDeleteAndReplace(true);
	}
	
	public void testDeleteAndReplaceF() throws Exception {
		xDeleteAndReplace(false);
	}	
}
