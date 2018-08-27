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
package org.eclipse.pde.api.tools.builder.tests.leak;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.tests.junit.extension.TestCase;
import org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest;
import org.eclipse.pde.api.tools.builder.tests.ApiProblem;
import org.eclipse.pde.api.tools.builder.tests.ApiTestingEnvironment;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests the builder to make sure it correctly reports non-API type leaks
 *
 * @since 1.0
 */
public abstract class LeakTest extends ApiBuilderTest {

	protected static IPath WORKSPACE_PATH = new Path("leakproject/src/x/y/z"); //$NON-NLS-1$

	protected final String TESTING_INTERNAL_CLASS_NAME = "internal"; //$NON-NLS-1$
	protected final String TESTING_INTERNAL_INTERFACE_NAME = "Iinternal"; //$NON-NLS-1$

	public LeakTest(String name) {
		super(name);
	}

	@Override
	protected void setBuilderOptions() {
		enableUnsupportedTagOptions(false);
		enableUnsupportedAnnotationOptions(false);
		enableBaselineOptions(false);
		enableCompatibilityOptions(false);
		enableLeakOptions(true);
		enableSinceTagOptions(false);
		enableUsageOptions(false);
		enableVersionNumberOptions(false);
	}

	@Override
	protected IPath getTestSourcePath() {
		return new Path("leak"); //$NON-NLS-1$
	}

	@Override
	protected String getTestingProjectName() {
		return "leakproject"; //$NON-NLS-1$
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		TestSuite suite = new TestSuite(LeakTest.class.getName());
		collectTests(suite);
		return suite;
	}

	/**
	 * Collects tests from the getAllTestClasses() method into the given suite
	 *
	 * @param suite
	 */
	private static void collectTests(TestSuite suite) {
		// Hack to load all classes before computing their suite of test cases
		// this allow to reset test cases subsets while running all Builder
		// tests...
		Class<?>[] classes = getAllTestClasses();

		// Reset forgotten subsets of tests
		TestCase.TESTS_PREFIX = null;
		TestCase.TESTS_NAMES = null;
		TestCase.TESTS_NUMBERS = null;
		TestCase.TESTS_RANGE = null;
		TestCase.RUN_ONLY_ID = null;

		/* tests */
		for (int i = 0, length = classes.length; i < length; i++) {
			Class<?> clazz = classes[i];
			Method suiteMethod;
			try {
				suiteMethod = clazz.getDeclaredMethod("suite"); //$NON-NLS-1$
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
				continue;
			}
			Object test;
			try {
				test = suiteMethod.invoke(null);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				continue;
			} catch (InvocationTargetException e) {
				e.printStackTrace();
				continue;
			}
			suite.addTest((Test) test);
		}
	}

	/**
	 * @return all of the child test classes of this class
	 */
	private static Class<?>[] getAllTestClasses() {
		Class<?>[] classes = new Class[] {
				ClassExtendsLeak.class, ClassImplementsLeak.class,
				InterfaceExtendsLeak.class, ConstructorParameterLeak.class,
				MethodParameterLeak.class, MethodReturnTypeLeak.class,
				FieldTypeLeak.class };
		return classes;
	}

	/**
	 * Verifies that the project has no problems.
	 */
	@Override
	protected void expectingNoProblems() {
		IProject project = getEnv().getWorkspace().getRoot().getProject(getTestingProjectName());
		expectingNoProblemsFor(project.getFullPath());
	}

	/**
	 * Deploys a leak test
	 *
	 * @param sourcename
	 *            the name of the source files to create in the testing project.
	 * @param incremental
	 *            if an incremental build should be performed
	 */
	protected void deployLeakTest(String sourcename, boolean incremantal) {
		try {
			IPath path = WORKSPACE_PATH.append(sourcename);
			createWorkspaceFile(path, TestSuiteHelper.getPluginDirectoryPath().append(TEST_SOURCE_ROOT).append(getTestSourcePath()).append(sourcename));
			if (incremantal) {
				incrementalBuild();
			} else {
				fullBuild();
			}
			expectingNoJDTProblemsFor(path);
			ApiProblem[] problems = getEnv().getProblemsFor(path, null);
			assertProblems(problems);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Override
	protected void setUp() throws Exception {
		ApiTestingEnvironment env = getEnv();
		if (env != null) {
			env.setRevert(true);
			env.setRevertSourcePath(null);
		}
		super.setUp();
		IProject project = getEnv().getWorkspace().getRoot().getProject(getTestingProjectName());
		if (!project.exists()) {
			// populate the workspace with initial plug-ins/projects
			createExistingProjects("leakprojects", true, true, true); //$NON-NLS-1$
		}
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		ApiTestingEnvironment env = getEnv();
		if (env != null) {
			env.setRevert(false);
		}
	}
}
