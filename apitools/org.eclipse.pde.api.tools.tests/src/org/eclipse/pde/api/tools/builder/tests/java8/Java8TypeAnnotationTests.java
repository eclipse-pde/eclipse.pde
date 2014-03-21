/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.builder.tests.java8;

import java.io.File;

import junit.framework.Test;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest;
import org.eclipse.pde.api.tools.builder.tests.ApiProblem;
import org.eclipse.pde.api.tools.builder.tests.ApiTestingEnvironment;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;

/**
 * Tests classes with type annotations in them do not cause any problems.
 * <br><br>
 * This test class reuses the Java 8 project used for tag checking
 */
public class Java8TypeAnnotationTests extends ApiBuilderTest {

	public Java8TypeAnnotationTests(String name) {
		super(name);
	}

	/**
	 * @return the test class for this suite
	 */
	public static Test suite() {
		return buildTestSuite(Java8TypeAnnotationTests.class);
	}
	@Override
	protected void setBuilderOptions() {
		enableUnsupportedTagOptions(true);
		enableBaselineOptions(true);
		enableCompatibilityOptions(true);
		enableLeakOptions(true);
		enableSinceTagOptions(true);
		enableUsageOptions(true);
		enableVersionNumberOptions(true);
	}
	
	@Override
	protected String getTestCompliance() {
		return JavaCore.VERSION_1_8;
	}

	@Override
	protected IPath getTestSourcePath() {
		return new Path("java8").append("annotations").append("types"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	@Override
	protected int getDefaultProblemId() {
		return 0;
	}

	@Override
	protected String getTestingProjectName() {
		return "java8tags"; //$NON-NLS-1$
	}
	
	/**
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		// If we have an existing environment, set it to revert rather than delete the workspace to improve performance
		ApiTestingEnvironment env = getEnv();
		if (env != null) {
			env.setRevert(true);
			env.setRevertSourcePath(null);
		}
		super.setUp();
	
		IProject project = getEnv().getWorkspace().getRoot().getProject(getTestingProjectName());
		if (!project.exists()) {
			IPath path = TestSuiteHelper.getPluginDirectoryPath().append(TEST_SOURCE_ROOT).append("tagprojects").append(getTestingProjectName()); //$NON-NLS-1$
			File dir = path.toFile();
			assertTrue("Test data directory does not exist for: " + path.toOSString(), dir.exists()); //$NON-NLS-1$
			createExistingProject(dir, true, true);
		}
	}
	
	/**
	 * Runs the test using the default testing env
	 * @param typename
	 * @param incremental
	 */
	void deployTest(String typename, boolean incremental, boolean hasproblems) {
		try {
			IPath typepath = new Path(getTestingProjectName()).append("src").append(typename).addFileExtension("java"); //$NON-NLS-1$ //$NON-NLS-2$
			createWorkspaceFile(typepath, TestSuiteHelper.getPluginDirectoryPath().append(TEST_SOURCE_ROOT).append(getTestSourcePath()).append(typename).addFileExtension("java")); //$NON-NLS-1$
			if(incremental) {
				incrementalBuild();
			}
			else {
				fullBuild();
			}
			if(!hasproblems) {
				expectingNoJDTProblemsFor(typepath);
				expectingNoProblemsFor(typepath);
			} else {
				ApiProblem[] problems = getEnv().getProblemsFor(typepath, null);
				assertProblems(problems);
			}
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
	}
	
	public void test1I() throws Exception {
		x1(true);
	}
	
	public void test1F() throws Exception {
		x1(false);
	}
	/**
	 * Tests there are no problems with type annotations in method decls
	 * @param inc
	 * @throws Exception
	 */
	void x1(boolean inc) throws Exception{
		deployTest("test1", inc, false); //$NON-NLS-1$
	}
	
	public void test2I() throws Exception {
		x2(true);
	}
	
	public void test2F() throws Exception {
		x2(false);
	}
	/**
	 * Tests there are no problems with type annotations in field decls
	 * @param inc
	 * @throws Exception
	 */
	void x2(boolean inc) throws Exception{
		deployTest("test2", inc, false); //$NON-NLS-1$
	}
	
	public void test3I() throws Exception {
		x3(true);
	}
	
	public void test3F() throws Exception {
		x3(false);
	}
	/**
	 * Tests there are no problems with type annotations in type decls
	 * @param inc
	 * @throws Exception
	 */
	void x3(boolean inc) throws Exception{
		deployTest("test3", inc, false); //$NON-NLS-1$
	}
	
	public void test4I() throws Exception {
		x4(true);
	}
	
	public void test4F() throws Exception {
		x4(false);
	}
	/**
	 * Tests there are problems with API tools annotations used in type annotation case in method decls
	 * @param inc
	 * @throws Exception
	 */
	void x4(boolean inc) throws Exception{
		deployTest("test4", inc, true); //$NON-NLS-1$
	}
	
	public void test5I() throws Exception {
		x5(true);
	}
	
	public void test5F() throws Exception {
		x5(false);
	}
	/**
	 * Tests there are problems with API tools annotations used in type annotation case in field decls
	 * @param inc
	 * @throws Exception
	 */
	void x5(boolean inc) throws Exception{
		deployTest("test5", inc, true); //$NON-NLS-1$
	}
	
	public void test6I() throws Exception {
		x6(true);
	}
	
	public void test6F() throws Exception {
		x6(false);
	}
	/**
	 * Tests there are problems with API tools annotations used in type annotation case in type decls
	 * @param inc
	 * @throws Exception
	 */
	void x6(boolean inc) throws Exception{
		deployTest("test6", inc, true); //$NON-NLS-1$
	}
}
