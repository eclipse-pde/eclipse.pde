/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.builder.tests.annotations;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.tests.junit.extension.TestCase;
import org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest;
import org.eclipse.pde.api.tools.builder.tests.ApiProblem;
import org.eclipse.pde.api.tools.builder.tests.ApiTestingEnvironment;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;
import org.eclipse.pde.api.tools.tests.util.ProjectUtils;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Root test for annotation tests
 *
 * @since 1.0.400
 */
public abstract class AnnotationTest extends ApiBuilderTest {

	// reuse the Javadoc tag project
	protected static IPath WORKSPACE_PATH = new Path("src/a/b/c"); //$NON-NLS-1$
	protected static IPath WORKSPACE_PATH_DEFAULT = new Path("src"); //$NON-NLS-1$

	/**
	 * Constructor
	 *
	 * @param name
	 */
	public AnnotationTest(String name) {
		super(name);
	}

	/**
	 * Sets the message arguments we are expecting for the given test, the
	 * number of times denoted by count
	 *
	 * @param tagname
	 * @param context
	 * @param count
	 */
	protected void setExpectedMessageArgs(String tagname, String context, int count) {
		String[][] args = new String[count][];
		for (int i = 0; i < count; i++) {
			args[i] = new String[] { tagname, context };
		}
		setExpectedMessageArgs(args);
	}

	/**
	 * @return all of the child test classes of this class
	 */
	private static Class<?>[] getAllTestClasses() {
		ArrayList<Class<?>> classes = new ArrayList<>();
		classes.add(InvalidAnnotationAnnotationsTests.class);
		classes.add(ValidAnnotationAnnotationsTests.class);
		classes.add(InvalidClassAnnotationsTests.class);
		classes.add(ValidClassAnnotationsTests.class);
		classes.add(InvalidInterfaceAnnotationTests.class);
		classes.add(ValidInterfaceAnnotationTests.class);
		classes.add(InvalidDuplicateAnnotationTests.class);
		classes.add(InvalidEnumAnnotationsTests.class);
		classes.add(ValidEnumAnnotationsTests.class);
		classes.add(FieldAnnotationTest.class);
		classes.add(MethodAnnotationTest.class);
		if (ProjectUtils.isJava8Compatible()) {
			classes.add(InvalidJava8InterfaceAnnotationTests.class);
			classes.add(ValidJava8InterfaceAnnotationTests.class);
			classes.add(Java8TypeAnnotationTests.class);
		}

		return classes.toArray(new Class<?>[classes.size()]);
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
	 * @return the tests for this class
	 */
	public static Test suite() {
		TestSuite suite = new TestSuite(AnnotationTest.class.getName());
		collectTests(suite);
		return suite;
	}

	@Override
	protected String getTestCompliance() {
		return JavaCore.VERSION_1_5;
	}

	@Override
	protected void setBuilderOptions() {
		// only care about unsupported tags
		enableUnsupportedTagOptions(false);
		enableUnsupportedAnnotationOptions(true);

		// disable the rest
		enableBaselineOptions(false);
		enableCompatibilityOptions(false);
		enableLeakOptions(false);
		enableSinceTagOptions(false);
		enableUsageOptions(false);
		enableVersionNumberOptions(false);
	}

	/**
	 * Returns an array composed only of the specified number of
	 * {@link #PROBLEM_ID}
	 *
	 * @param problemcount
	 * @return an array of {@link #PROBLEM_ID} of the specified size, or an
	 *         empty array if the specified size is < 1
	 */
	protected int[] getDefaultProblemSet(int problemcount) {
		if (problemcount < 1) {
			return new int[0];
		}
		int[] array = new int[problemcount];
		int defaultproblem = getDefaultProblemId();
		for (int i = 0; i < problemcount; i++) {
			array[i] = defaultproblem;
		}
		return array;
	}

	@Override
	protected IPath getTestSourcePath() {
		return new Path("annotations"); //$NON-NLS-1$
	}

	@Override
	protected String getTestingProjectName() {
		return "tagproject"; //$NON-NLS-1$
	}

	/**
	 * Deploys a build test for API Javadoc tags using the given source file,
	 * looking for problems specified from {@link #getExpectedProblemIds()()}
	 *
	 * @param sourcename
	 * @param incremental if an incremental build should take place
	 * @param usedefault if the default package should be used or not
	 */
	protected void deployAnnotationTest(String sourcename, boolean incremental, boolean usedefault) {
		try {
			IPath path = new Path(getTestingProjectName()).append(WORKSPACE_PATH).append(sourcename);
			if (usedefault) {
				path = new Path(getTestingProjectName()).append(WORKSPACE_PATH_DEFAULT).append(sourcename);
			}
			createWorkspaceFile(path, TestSuiteHelper.getPluginDirectoryPath().append(TEST_SOURCE_ROOT).append(getTestSourcePath()).append(sourcename));
			if (incremental) {
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

	/**
	 * Deploys the annotations test and allows it to proceed if there are JDT
	 * errors.
	 *
	 * @param sourcename
	 * @param incremental
	 * @param usedefault
	 */
	protected void deployAnnotationTestWithErrors(String sourcename, boolean incremental, boolean usedefault) {
		try {
			IPath path = new Path(getTestingProjectName()).append(WORKSPACE_PATH).append(sourcename);
			if (usedefault) {
				path = new Path(getTestingProjectName()).append(WORKSPACE_PATH_DEFAULT).append(sourcename);
			}
			createWorkspaceFile(path, TestSuiteHelper.getPluginDirectoryPath().append(TEST_SOURCE_ROOT).append(getTestSourcePath()).append(sourcename));
			if (incremental) {
				incrementalBuild();
			} else {
				fullBuild();
			}
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
			createExistingProjects("tagprojects", true, true, false); //$NON-NLS-1$
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
