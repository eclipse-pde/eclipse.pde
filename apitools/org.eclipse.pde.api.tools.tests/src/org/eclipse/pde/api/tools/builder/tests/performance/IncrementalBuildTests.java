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
package org.eclipse.pde.api.tools.builder.tests.performance;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.tests.junit.extension.TestCase;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.test.performance.Dimension;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test the performance of an incremental build of the Debug Core plug-in when a
 * source with many dependents has been changed
 *
 * @since 1.0.0
 */
public class IncrementalBuildTests extends PerformanceTest {

	protected static final String DEBUG_CORE = "org.eclipse.debug.core"; //$NON-NLS-1$
	protected static final String CHANGED = "changed"; //$NON-NLS-1$
	protected static final String REVERT = "revert"; //$NON-NLS-1$

	public IncrementalBuildTests(String name) {
		super(name);
	}

	/**
	 * @return all of the child test classes of this class
	 */
	private static Class<?>[] getAllTestClasses() {
		Class<?>[] classes = new Class[] {
				EnumIncrementalBuildTests.class,
				AnnotationIncrementalBuildTests.class };
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

	@Override
	protected String getBaselineLocation() {
		return getTestSourcePath().append("bin-baseline.zip").toOSString(); //$NON-NLS-1$
	}

	@Override
	protected String getWorkspaceLocation() {
		return getTestSourcePath().append("source-ws.zip").toOSString(); //$NON-NLS-1$
	}

	/**
	 * Returns the path the file that will be reverted
	 *
	 * @param filename
	 * @return
	 */
	protected IPath getRevertFilePath(String testname, String filename) {
		return getTestSourcePath().append("incremental").append(testname).append(REVERT).append(filename); //$NON-NLS-1$
	}

	/**
	 * Gets the
	 *
	 * @param testname
	 * @param filename
	 * @return
	 */
	protected IPath getUpdateFilePath(String testname, String filename) {
		return getTestSourcePath().append("incremental").append(testname).append(CHANGED).append(filename); //$NON-NLS-1$
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		TestSuite suite = (TestSuite) buildTestSuite(IncrementalBuildTests.class);
		collectTests(suite);
		return suite;
	}

	/**
	 * Tests the incremental build performance for a variety of problems in a
	 * class that has many dependents - kind of a worst-case build scenario <br>
	 * This test uses <code>org.eclipse.debug.core.Launch</code>
	 *
	 * @throws Exception if something bad happens, or if unexpected problems are
	 *             found after a build
	 */
	public void testIncrementalBuildAll() throws Exception {
		int[] problems = new int[] {
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.UNSUPPORTED_TAG_USE, IApiProblem.NO_FLAGS),
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_EXTEND, IApiProblem.NO_FLAGS),
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.METHOD, IApiProblem.API_LEAK, IApiProblem.LEAK_RETURN_TYPE),
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY, IDelta.CLASS_ELEMENT_TYPE, IDelta.REMOVED, IDelta.METHOD), };
		deployIncrementalPerformanceTest("Incremental Build - All", //$NON-NLS-1$
				"test1", //$NON-NLS-1$
				DEBUG_CORE, DEBUG_CORE + ".Launch", //$NON-NLS-1$
				problems, 500);
	}

	/**
	 * Incremental build for structural change to an API type.
	 *
	 * @throws Exception
	 */
	public void _testApiStructuralChange() throws Exception {
		deployIncrementalPerformanceTest("Incremental - API Structural Change", //$NON-NLS-1$
				"api-struc-change", //$NON-NLS-1$
				"org.eclipse.core.jobs", //$NON-NLS-1$
				"org.eclipse.core.runtime.jobs.Job", //$NON-NLS-1$
				new int[0], 550);
	}

	/**
	 * Incremental build for non-structural change to an API type.
	 *
	 * @throws Exception
	 */
	public void _testApiNonStructuralChange() throws Exception {
		deployIncrementalPerformanceTest("Incremental - API Non-Structural Change", //$NON-NLS-1$
				"api-non-struc-change", //$NON-NLS-1$
				"org.eclipse.core.resources", //$NON-NLS-1$
				"org.eclipse.core.resources.ResourcesPlugin", //$NON-NLS-1$
				new int[0], 500);
	}

	/**
	 * Incremental build for an API description change to an API type.
	 *
	 * @throws Exception
	 */
	public void _testApiDescriptionChange() throws Exception {
		deployIncrementalPerformanceTest("Incremental - API Description Change", //$NON-NLS-1$
				"api-desc-change", //$NON-NLS-1$
				"org.eclipse.core.resources", //$NON-NLS-1$
				"org.eclipse.core.resources.IResource", //$NON-NLS-1$
				new int[0], 500);
	}

	/**
	 * Incremental build for structural change to an internal type.
	 *
	 * @throws Exception
	 */
	public void _testInternalStructuralChange() throws Exception {
		deployIncrementalPerformanceTest("Incremental - Internal Structural Change", //$NON-NLS-1$
				"non-api-struc-change", //$NON-NLS-1$
				"org.eclipse.core.resources", //$NON-NLS-1$
				"org.eclipse.core.internal.resources.Resource", //$NON-NLS-1$
				new int[0], 500);
	}

	/**
	 * Incremental build for non-structural change to an internal type.
	 *
	 * @throws Exception
	 */
	public void _testInternalNonStructuralChange() throws Exception {
		deployIncrementalPerformanceTest("Incremental - Internal Non-Structural Change", //$NON-NLS-1$
				"non-api-non-struc-change", //$NON-NLS-1$
				"org.eclipse.core.resources", //$NON-NLS-1$
				"org.eclipse.core.internal.resources.File", //$NON-NLS-1$
				new int[0], 600);
	}

	/**
	 * Incremental build for an API description change to an internal type.
	 *
	 * @throws Exception
	 */
	public void _testInternalDescriptionChange() throws Exception {
		deployIncrementalPerformanceTest("Incremental - Internal Description Change", //$NON-NLS-1$
				"non-api-desc-change", //$NON-NLS-1$
				"org.eclipse.core.jobs", //$NON-NLS-1$
				"org.eclipse.core.internal.jobs.InternalJob", //$NON-NLS-1$
				new int[0], 500);
	}

	/**
	 * Tests the incremental build performance for a single compatibility
	 * problem in an interface that has many dependents. <br>
	 * This test uses <code>org.eclipse.debug.core.model.IDebugElement</code>
	 *
	 * @throws Exception if something bad happens, or if unexpected problems are
	 *             found after a build
	 */
	public void _testIncrementalBuildCompat() throws Exception {
		int[] problems = new int[] { ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY, IDelta.INTERFACE_ELEMENT_TYPE, IDelta.REMOVED, IDelta.METHOD) };
		deployIncrementalPerformanceTest("Incremental Build - Interface Compatibility", //$NON-NLS-1$
				"test2", //$NON-NLS-1$
				DEBUG_CORE, DEBUG_CORE + ".model.IDebugElement", //$NON-NLS-1$
				problems, 500);
	}

	/**
	 * Tests the incremental build performance for a single compatibility
	 * problem in a class that has many dependents. <br>
	 * This tests uses <code>org.eclipse.debug.core.DebugException</code>
	 *
	 * @throws Exception if something bad happens, or if unexpected problems are
	 *             found after a build
	 */
	public void _testIncremetalBuildClassCompat() throws Exception {
		int[] problems = new int[] { ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY, IDelta.CLASS_ELEMENT_TYPE, IDelta.REMOVED, IDelta.CONSTRUCTOR) };
		deployIncrementalPerformanceTest("Incremental Build - Class Compatibility", //$NON-NLS-1$
				"test3", //$NON-NLS-1$
				DEBUG_CORE, DEBUG_CORE + ".DebugException", //$NON-NLS-1$
				problems, 500);
	}

	/**
	 * Tests the incremental build performance for a single usage problem in
	 * source that has many dependents. <br>
	 * This test uses <code>org.eclipse.debug.core.model.DebugElement</code>
	 *
	 * @throws Exception if something bad happens, or if unexpected problems are
	 *             found after a build
	 */
	public void _testIncrementalBuildUsage() throws Exception {
		int[] problems = new int[] { ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_EXTEND, IApiProblem.NO_FLAGS) };
		deployIncrementalPerformanceTest("Incremental Build - Usage", //$NON-NLS-1$
				"test4", //$NON-NLS-1$
				DEBUG_CORE, DEBUG_CORE + ".model.DebugElement", //$NON-NLS-1$
				problems, 500);
	}

	/**
	 * Tests the incremental build performance for a single leak problem in
	 * source that has many dependents. <br>
	 * This test uses <code>org.eclipse.debug.core.model.Breakpoint</code>
	 *
	 * @throws Exception if something bad happens, or if unexpected problems are
	 *             found after a build
	 */
	public void _testIncrementalBuildLeak() throws Exception {
		int[] problems = new int[] { ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.METHOD, IApiProblem.API_LEAK, IApiProblem.LEAK_RETURN_TYPE) };
		deployIncrementalPerformanceTest("Incremental Build - Leak", //$NON-NLS-1$
				"test5", //$NON-NLS-1$
				DEBUG_CORE, DEBUG_CORE + ".model.Breakpoint", //$NON-NLS-1$
				problems, 500);
	}

	/**
	 * Tests the incremental build performance for a single unsupported tag
	 * problem in source that has many dependents. In this test is used. <br>
	 * This test uses <code>org.eclipse.debug.core.model.RuntimeProcess</code>
	 *
	 * @throws Exception if something bad happens, or if unexpected problems are
	 *             found after a build
	 */
	public void _testIncrementalBuildTags() throws Exception {
		int[] problems = new int[] {
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.UNSUPPORTED_TAG_USE, IApiProblem.NO_FLAGS),
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_TAG_USE, IApiProblem.NO_FLAGS),
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_TAG_USE, IApiProblem.NO_FLAGS)

		};
		deployIncrementalPerformanceTest("Incremental Build - Unsupported Tags", //$NON-NLS-1$
				"test6", //$NON-NLS-1$
				DEBUG_CORE, DEBUG_CORE + ".model.RuntimeProcess", //$NON-NLS-1$
				problems, 500);
	}

	/**
	 * Updates the workspace file and builds it incrementally. Overrides the
	 * default implementation to also do an incremental build with the Java
	 * builder after the file has been updated.
	 *
	 * @param project
	 * @param workspaceLocation
	 * @param replacementLocation
	 * @throws Exception if something bad happens, or if unexpected problems are
	 *             found after a build
	 */
	protected void updateWorkspaceFile(IProject project, IPath workspaceLocation, IPath replacementLocation) throws Exception {
		updateWorkspaceFile(workspaceLocation, replacementLocation);
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
	}

	/**
	 * Deploys an incremental build tests with the given summary, changing the
	 * type in the given project.
	 *
	 * @param summary human readable summary for the test
	 * @param testname the name of the test, used to find the source
	 * @param projectname the name of the project to deploy to incremental test
	 *            to
	 * @param typename the fully qualified name of the type to replace
	 * @param problemids array of expected problem ids
	 *
	 * @throws Exception if something bad happens, or if unexpected problems are
	 *             found after a build
	 */
	protected void deployIncrementalPerformanceTest(String summary, String testname, String projectname, String typename, int[] problemids, int iterations) throws Exception {
		tagAsSummary(summary, Dimension.ELAPSED_PROCESS);

		// WARM-UP, must do full build with Java build to get the state
		System.out.println("Warm-up clean builds..."); //$NON-NLS-1$
		for (int i = 0; i < 2; i++) {
			cleanBuild();
			fullBuild();
		}

		// TEST
		System.out.println("Testing incremental build: [" + summary + "]..."); //$NON-NLS-1$ //$NON-NLS-2$
		long start = System.currentTimeMillis();
		IProject proj = getEnv().getWorkspace().getRoot().getProject(projectname);
		IType type = JavaCore.create(proj).findType(typename);
		IPath file = type.getPath();
		for (int i = 0; i < iterations; i++) {
			startMeasuring();
			updateWorkspaceFile(proj, file, getUpdateFilePath(testname, file.lastSegment()));
			stopMeasuring();
			// dispose the workspace baseline
			proj.touch(null);
			updateWorkspaceFile(proj, file, getRevertFilePath(testname, file.lastSegment()));
		}
		commitMeasurements();
		assertPerformance();
		System.out.println("done in: " + ((System.currentTimeMillis() - start) / 1000) + " seconds"); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
