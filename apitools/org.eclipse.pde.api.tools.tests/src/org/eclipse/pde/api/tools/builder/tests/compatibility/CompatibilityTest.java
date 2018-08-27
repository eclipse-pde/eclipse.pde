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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.jar.JarFile;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.tests.junit.extension.TestCase;
import org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest;
import org.eclipse.pde.api.tools.builder.tests.ApiProblem;
import org.eclipse.pde.api.tools.builder.tests.ApiTestingEnvironment;
import org.eclipse.pde.api.tools.internal.model.ApiModelFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiBaselineManager;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.tests.ApiTestsPlugin;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Base class for binary compatibility tests
 *
 * @since 1.0
 */
public abstract class CompatibilityTest extends ApiBuilderTest {

	public CompatibilityTest(String name) {
		super(name);
	}

	@Override
	protected IPath getTestSourcePath() {
		return new Path("compat"); //$NON-NLS-1$
	}

	@Override
	protected void setBuilderOptions() {
		enableUnsupportedTagOptions(false);
		enableUnsupportedAnnotationOptions(false);
		enableBaselineOptions(false);
		enableCompatibilityOptions(true);
		enableLeakOptions(false);
		enableSinceTagOptions(false);
		enableUsageOptions(false);
		enableVersionNumberOptions(false);
	}

	/**
	 * @return all of the child test classes of this class
	 */
	private static Class<?>[] getAllTestClasses() {
		Class<?>[] classes = new Class[] {
				ProjectTypeContainerTests.class,
				BundleCompatibilityTests.class,
				AnnotationCompatibilityTests.class,
				InterfaceCompatibilityTests.class,
				EnumCompatibilityTests.class, ClassCompatibilityTests.class,
				FieldCompatibilityTests.class, MethodCompatibilityTests.class,
				ConstructorCompatibilityTests.class, SinceTagTest.class,
				VersionTest.class, BundleMergeSplitTests.class,
				BundleVersionTests.class, };
		return classes;
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
		TestSuite suite = new TestSuite(CompatibilityTest.class.getName());
		collectTests(suite);
		return suite;
	}

	/*
	 * Ensure a baseline has been created to compare against.
	 *
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		// for the first compatibility test create workspace projects
		ApiTestingEnvironment env = getEnv();
		if (env != null) {
			env.setRevert(true);
		}
		super.setUp();
		IProject[] projects = getEnv().getWorkspace().getRoot().getProjects();
		if (projects.length == 0) {
			// populate the workspace with initial plug-ins/projects
			createExistingProjects(BASELINE, true, true, false);
		} else {
			// build after revert
			incrementalBuild();
		}
		IApiBaselineManager manager = ApiPlugin.getDefault().getApiBaselineManager();
		IApiBaseline baseline = manager.getDefaultApiBaseline();
		if (baseline == null) {
			// create the API baseline
			projects = getEnv().getWorkspace().getRoot().getProjects();
			IPath baselineLocation = ApiTestsPlugin.getDefault().getStateLocation().append(BASELINE);
			IApiComponent component = null;
			for (int i = 0; i < projects.length; i++) {
				component = manager.getWorkspaceComponent(projects[i].getName());
				assertNotNull("The project was not found in the workspace baseline: " + projects[i].getName(), component); //$NON-NLS-1$
				exportApiComponent(projects[i], component, baselineLocation);
			}
			baseline = ApiModelFactory.newApiBaseline("API-baseline"); //$NON-NLS-1$
			IApiComponent[] components = new IApiComponent[projects.length];
			for (int i = 0; i < projects.length; i++) {
				IProject project = projects[i];
				IPath location = baselineLocation.append(project.getName());
				components[i] = ApiModelFactory.newApiComponent(baseline, location.toOSString());
			}
			baseline.addApiComponents(components);
			manager.addApiBaseline(baseline);
			manager.setDefaultApiBaseline(baseline.getName());
		}
		getEnv().setRevertSourcePath(new Path(BASELINE));
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		getEnv().setRevert(false);
	}

	/**
	 * Performs a compatibility test. The workspace file at the specified (full
	 * workspace path) location is updated with a corresponding file from test
	 * data. A build is performed and problems are compared against the expected
	 * problems for the associated resource.
	 *
	 * @param workspaceFile file to update
	 * @param incremental whether to perform an incremental (<code>true</code>)
	 *            or full (<code>false</code>) build
	 * @throws Exception
	 */
	protected void performCompatibilityTest(IPath workspaceFile, boolean incremental) throws Exception {
		updateWorkspaceFile(workspaceFile, getUpdateFilePath(workspaceFile.lastSegment()));
		if (incremental) {
			incrementalBuild();
		} else {
			fullBuild();
		}
		expectingNoJDTProblemsFor(workspaceFile);
		ApiProblem[] problems = getEnv().getProblemsFor(workspaceFile, null);
		assertProblems(problems);
	}

	/**
	 * Performs a compatibility test. The workspace file at the specified (full
	 * workspace path) location is updated with a corresponding file from test
	 * data. A build is performed and problems are compared against the expected
	 * problems for the associated resource.
	 *
	 * @param workspaceFile file to update
	 * @param incremental whether to perform an incremental (<code>true</code>)
	 *            or full (<code>false</code>) build
	 * @throws Exception
	 */
	protected void performVersionTest(IPath workspaceFile, boolean incremental) throws Exception {
		updateWorkspaceFile(workspaceFile, getUpdateFilePath(workspaceFile.lastSegment()));
		if (incremental) {
			incrementalBuild();
		} else {
			fullBuild();
		}
		ApiProblem[] problems = getEnv().getProblemsFor(new Path(workspaceFile.segment(0)).append(JarFile.MANIFEST_NAME), null);
		assertProblems(problems);
	}

	/**
	 * Performs a compatibility test. The workspace file at the specified (full
	 * workspace path) location is deleted. A build is performed and problems
	 * are compared against the expected problems for the associated resource.
	 *
	 * @param workspaceFile file to update
	 * @param incremental whether to perform an incremental (<code>true</code>)
	 *            or full (<code>false</code>) build
	 * @throws Exception
	 */
	protected void performDeletionCompatibilityTest(IPath workspaceFile, boolean incremental) throws Exception {
		deleteWorkspaceFile(workspaceFile, true);
		if (incremental) {
			incrementalBuild();
		} else {
			fullBuild();
		}
		ApiProblem[] problems = getEnv().getProblems();
		assertProblems(problems);
	}

	/**
	 * Performs a compatibility test. The workspace file at the specified (full
	 * workspace path) location is created. A build is performed and problems
	 * are compared against the expected problems for the associated resource.
	 *
	 * @param workspaceFile file to update
	 * @param incremental whether to perform an incremental (<code>true</code>)
	 *            or full (<code>false</code>) build
	 * @throws Exception
	 */
	protected void performCreationCompatibilityTest(IPath workspaceFile, boolean incremental) throws Exception {
		createWorkspaceFile(workspaceFile, getUpdateFilePath(workspaceFile.lastSegment()));
		if (incremental) {
			incrementalBuild();
		} else {
			fullBuild();
		}
		ApiProblem[] problems = getEnv().getProblems();
		assertProblems(problems);
	}
}
