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

import junit.framework.Test;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
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
		return buildTestSuite(IncrementalBuildTests.class);
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
				problems);
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
				problems);
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
				problems);
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
				problems);
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
				problems);
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
				problems);
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
	protected void deployIncrementalPerformanceTest(String summary, String testname, String projectname, String typename, int[] problemids) throws Exception {
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
		for(int i = 0; i < 500; i++) {
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
