/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.builder.tests.performance;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import junit.framework.Test;
import junit.framework.TestSuite;

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

/**
 * Test the performance of an incremental build of the Debug Core plug-in
 * when a source with many dependents has been changed
 * 
 * @since 1.0.0
 */
public class IncrementalBuildTests extends PerformanceTest {

	protected static final String DEBUG_CORE = "org.eclipse.debug.core";
	protected static final String CHANGED = "changed";
	protected static final String REVERT = "revert";
	
	/**
	 * Constructor
	 * @param name
	 */
	public IncrementalBuildTests(String name) {
		super(name);
	}
	
	/**
	 * @return all of the child test classes of this class
	 */
	private static Class[] getAllTestClasses() {
		Class[] classes = new Class[] {
			EnumIncrementalBuildTests.class,
			AnnotationIncrementalBuildTests.class
		};
		return classes;
	}

	/**
	 * Collects tests from the getAllTestClasses() method into the given suite
	 * @param suite
	 */
	private static void collectTests(TestSuite suite) {
		// Hack to load all classes before computing their suite of test cases
		// this allow to reset test cases subsets while running all Builder tests...
		Class[] classes = getAllTestClasses();

		// Reset forgotten subsets of tests
		TestCase.TESTS_PREFIX = null;
		TestCase.TESTS_NAMES = null;
		TestCase.TESTS_NUMBERS = null;
		TestCase.TESTS_RANGE = null;
		TestCase.RUN_ONLY_ID = null;

		/* tests */
		for (int i = 0, length = classes.length; i < length; i++) {
			Class clazz = classes[i];
			Method suiteMethod;
			try {
				suiteMethod = clazz.getDeclaredMethod("suite", new Class[0]);
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
				continue;
			}
			Object test;
			try {
				test = suiteMethod.invoke(null, new Object[0]);
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
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.performance.PerformanceTest#getBaselineLocation()
	 */
	protected String getBaselineLocation() {
		return getTestSourcePath().append("bin-baseline.zip").toOSString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.performance.PerformanceTest#getWorkspaceLocation()
	 */
	protected String getWorkspaceLocation() {
		return getTestSourcePath().append("source-ws.zip").toOSString();
	}
	
	/**
	 * Returns the path the file that will be reverted
	 * @param filename
	 * @return
	 */
	protected IPath getRevertFilePath(String testname, String filename) {
		return getTestSourcePath().append("incremental").append(testname).append(REVERT).append(filename);
	}
	
	/**
	 * Gets the 
	 * @param testname
	 * @param filename
	 * @return
	 */
	protected IPath getUpdateFilePath(String testname, String filename) {
		return getTestSourcePath().append("incremental").append(testname).append(CHANGED).append(filename);
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
	 * Tests the incremental build performance for a variety
	 * of problems in a class that has many dependents
	 * - kind of a worst-case build scenario
	 * <br>
	 * This test uses <code>org.eclipse.debug.core.Launch</code>
	 * 
	 * @throws Exception if something bad happens, or if unexpected problems are found after a build
	 */
	public void testIncrementalBuildAll() throws Exception {
		int[] problems = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_USAGE, 
						IElementDescriptor.TYPE, 
						IApiProblem.UNSUPPORTED_TAG_USE, 
						IApiProblem.NO_FLAGS),
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_USAGE, 
						IElementDescriptor.TYPE, 
						IApiProblem.ILLEGAL_EXTEND, 
						IApiProblem.NO_FLAGS),
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_USAGE, 
						IElementDescriptor.METHOD, 
						IApiProblem.API_LEAK, 
						IApiProblem.LEAK_RETURN_TYPE),
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_COMPATIBILITY, 
						IDelta.CLASS_ELEMENT_TYPE, 
						IDelta.REMOVED, 
						IDelta.METHOD),
		};
		deployIncrementalPerformanceTest(
				"Incremental Build - All", 
				"test1",
				DEBUG_CORE, 
				DEBUG_CORE+".Launch",
				problems, 500);
	}
	
	/**
	 * Incremental build for structural change to an API type.
	 * 
	 * @throws Exception
	 */
	public void _testApiStructuralChange() throws Exception {
		deployIncrementalPerformanceTest(
				"Incremental - API Structural Change",
				"api-struc-change",
				"org.eclipse.core.jobs",
				"org.eclipse.core.runtime.jobs.Job",
				new int[0], 550);
	}
	
	/**
	 * Incremental build for non-structural change to an API type.
	 * 
	 * @throws Exception
	 */
	public void _testApiNonStructuralChange() throws Exception {
		deployIncrementalPerformanceTest(
				"Incremental - API Non-Structural Change",
				"api-non-struc-change",
				"org.eclipse.core.resources",
				"org.eclipse.core.resources.ResourcesPlugin",
				new int[0], 500);
	}
	
	/**
	 * Incremental build for an API description change to an API type.
	 * 
	 * @throws Exception
	 */
	public void _testApiDescriptionChange() throws Exception {
		deployIncrementalPerformanceTest(
				"Incremental - API Description Change",
				"api-desc-change",
				"org.eclipse.core.resources",
				"org.eclipse.core.resources.IResource",
				new int[0], 500);
	}
	
	/**
	 * Incremental build for structural change to an internal type.
	 * 
	 * @throws Exception
	 */
	public void _testInternalStructuralChange() throws Exception {
		deployIncrementalPerformanceTest(
				"Incremental - Internal Structural Change",
				"non-api-struc-change",
				"org.eclipse.core.resources",
				"org.eclipse.core.internal.resources.Resource",
				new int[0], 500);
	}
	
	/**
	 * Incremental build for non-structural change to an internal type.
	 * 
	 * @throws Exception
	 */
	public void _testInternalNonStructuralChange() throws Exception {
		deployIncrementalPerformanceTest(
				"Incremental - Internal Non-Structural Change",
				"non-api-non-struc-change",
				"org.eclipse.core.resources",
				"org.eclipse.core.internal.resources.File",
				new int[0], 600);
	}
	
	/**
	 * Incremental build for an API description change to an internal type.
	 * 
	 * @throws Exception
	 */
	public void _testInternalDescriptionChange() throws Exception {
		deployIncrementalPerformanceTest(
				"Incremental - Internal Description Change",
				"non-api-desc-change",
				"org.eclipse.core.jobs",
				"org.eclipse.core.internal.jobs.InternalJob",
				new int[0], 500);
	}	
	
	/**
	 * Tests the incremental build performance for a single 
	 * compatibility problem in an interface that has many dependents.
	 * <br>
	 * This test uses <code>org.eclipse.debug.core.model.IDebugElement</code>
	 * 
	 * @throws Exception if something bad happens, or if unexpected problems are found after a build
	 */
	public void _testIncrementalBuildCompat() throws Exception {
		int[] problems = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_COMPATIBILITY, 
						IDelta.INTERFACE_ELEMENT_TYPE, 
						IDelta.REMOVED, 
						IDelta.METHOD)
		};
		deployIncrementalPerformanceTest(
				"Incremental Build - Interface Compatibility", 
				"test2",
				DEBUG_CORE, 
				DEBUG_CORE+".model.IDebugElement",
				problems, 500);
	}
	
	/**
	 * Tests the incremental build performance for a single compatibility problem
	 * in a class that has many dependents.
	 * <br>
	 * This tests uses <code>org.eclipse.debug.core.DebugException</code>
	 * 
	 * @throws Exception if something bad happens, or if unexpected problems are found after a build
	 */
	public void _testIncremetalBuildClassCompat() throws Exception {
		int[] problems = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_COMPATIBILITY, 
						IDelta.CLASS_ELEMENT_TYPE, 
						IDelta.REMOVED, 
						IDelta.CONSTRUCTOR)
		};
		deployIncrementalPerformanceTest(
				"Incremental Build - Class Compatibility", 
				"test3",
				DEBUG_CORE, 
				DEBUG_CORE+".DebugException",
				problems, 500);
	}
	
	/**
	 * Tests the incremental build performance for a single 
	 * usage problem in source that has many dependents.
	 * <br>
	 * This test uses <code>org.eclipse.debug.core.model.DebugElement</code>
	 * 
	 * @throws Exception if something bad happens, or if unexpected problems are found after a build
	 */
	public void _testIncrementalBuildUsage() throws Exception {
		int[] problems = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_USAGE, 
						IElementDescriptor.TYPE, 
						IApiProblem.ILLEGAL_EXTEND, 
						IApiProblem.NO_FLAGS)
		};
		deployIncrementalPerformanceTest(
				"Incremental Build - Usage", 
				"test4",
				DEBUG_CORE, 
				DEBUG_CORE+".model.DebugElement",
				problems, 500);
	}
	
	/**
	 * Tests the incremental build performance for a single 
	 * leak problem in source that has many dependents.
	 * <br>
	 * This test uses <code>org.eclipse.debug.core.model.Breakpoint</code>
	 * 
	 * @throws Exception if something bad happens, or if unexpected problems are found after a build
	 */
	public void _testIncrementalBuildLeak() throws Exception {
		int[] problems = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_USAGE, 
						IElementDescriptor.METHOD, 
						IApiProblem.API_LEAK, 
						IApiProblem.LEAK_RETURN_TYPE)
		};
		deployIncrementalPerformanceTest(
				"Incremental Build - Leak", 
				"test5",
				DEBUG_CORE, 
				DEBUG_CORE+".model.Breakpoint",
				problems, 500);
	}
	
	/**
	 * Tests the incremental build performance for a single 
	 * unsupported tag problem in source that has many dependents. 
	 * In this test is used.
	 * <br>
	 * This test uses <code>org.eclipse.debug.core.model.RuntimeProcess</code>
	 * 
	 * @throws Exception if something bad happens, or if unexpected problems are found after a build
	 */
	public void _testIncrementalBuildTags() throws Exception {
		int[] problems = new int[] {
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, 
						IElementDescriptor.TYPE, 
						IApiProblem.UNSUPPORTED_TAG_USE, 
						IApiProblem.NO_FLAGS),
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, 
						IElementDescriptor.METHOD, 
						IApiProblem.UNSUPPORTED_TAG_USE, 
						IApiProblem.NO_FLAGS),
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, 
						IElementDescriptor.METHOD, 
						IApiProblem.UNSUPPORTED_TAG_USE, 
						IApiProblem.NO_FLAGS)
				
		};
		deployIncrementalPerformanceTest(
				"Incremental Build - Unsupported Tags", 
				"test6",
				DEBUG_CORE, 
				DEBUG_CORE+".model.RuntimeProcess",
				problems, 500);
	}
	
	/**
	 * Updates the workspace file and builds it incrementally. Overrides the default implementation
	 * to also do an incremental build with the Java builder after the file has been updated.
	 * @param project
	 * @param workspaceLocation
	 * @param replacementLocation
	 * @throws Exception if something bad happens, or if unexpected problems are found after a build
	 */
	protected void updateWorkspaceFile(IProject project, IPath workspaceLocation, IPath replacementLocation) throws Exception {
		updateWorkspaceFile(workspaceLocation, replacementLocation);
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
	}
	
	/**
	 * Deploys an incremental build tests with the given summary, changing the type in the given project.
	 * @param summary human readable summary for the test
	 * @param testname the name of the test, used to find the source
	 * @param projectname the name of the project to deploy to incremental test to
	 * @param typename the fully qualified name of the type to replace
	 * @param problemids array of expected problem ids
	 * 
	 * @throws Exception if something bad happens, or if unexpected problems are found after a build
	 */
	protected void deployIncrementalPerformanceTest(String summary, String testname, String projectname, String typename, int[] problemids, int iterations) throws Exception {
		tagAsSummary(summary, Dimension.ELAPSED_PROCESS);
		
		//WARM-UP, must do full build with Java build to get the state
		System.out.println("Warm-up clean builds...");
		for(int i = 0; i < 2; i++) {
			cleanBuild();
			fullBuild();
		}
		
		//TEST
		System.out.println("Testing incremental build: ["+summary+"]...");
		long start = System.currentTimeMillis();
		IProject proj = getEnv().getWorkspace().getRoot().getProject(projectname);
		IType type = JavaCore.create(proj).findType(typename);
		IPath file = type.getPath();
		for(int i = 0; i < iterations; i++) {
			startMeasuring();
			updateWorkspaceFile(proj, file, getUpdateFilePath(testname, file.lastSegment()));
			stopMeasuring();
			//dispose the workspace baseline
			proj.touch(null);
			updateWorkspaceFile(proj, file, getRevertFilePath(testname, file.lastSegment()));
		}
		commitMeasurements();
		assertPerformance();
		System.out.println("done in: "+((System.currentTimeMillis()-start)/1000)+" seconds");
	}
}
