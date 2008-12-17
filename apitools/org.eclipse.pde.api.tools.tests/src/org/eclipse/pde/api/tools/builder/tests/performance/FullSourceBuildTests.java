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
package org.eclipse.pde.api.tools.builder.tests.performance;

import junit.framework.Test;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.test.performance.Dimension;

/**
 * Performance tests for full source workspace build
 * 
 * @since 1.0
 */
public class FullSourceBuildTests extends PerformanceTest {	

	/**
	 * Constructor
	 * @param name
	 */
	public FullSourceBuildTests(String name) {
		super(name);
	}
	
	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(FullSourceBuildTests.class);
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
	 * Tests a full build of a 3.4 workspace with source from debug.core and pre-reqs
	 * against a baseline of 3.3 binary plug-ins.
	 * 
	 * @throws Exception
	 */
	public void testFullBuild() throws Exception {
		tagAsSummary("Full Build", Dimension.ELAPSED_PROCESS);
		
		// get everything built
		fullBuild();
		IProject[] projects = getEnv().getProjectBuildOrder();
		
		// WARM-UP
		for (int j = 0; j < 2; j++) {
			orderedBuild(projects);
		}
		
		// TEST
		for (int j = 0; j < 15; j++) {
			startMeasuring();
			
			// *** build each project ***
			for (int i = 0; i < projects.length; i++) {
				projects[i].build(IncrementalProjectBuilder.FULL_BUILD, ApiPlugin.BUILDER_ID, null, null);
			}
			
			stopMeasuring();
		}
		
		commitMeasurements();
		assertPerformance();
	}
	
	/**
	 * Tests a clean and full build of a 3.4 workspace with source from debug.core and pre-reqs
	 * against a baseline of 3.3 binary plug-ins.
	 * 
	 * @throws Exception
	 */
	public void testCleanFullBuild() throws Exception {
		tagAsSummary("Clean & Full Build", Dimension.ELAPSED_PROCESS);
		
		// get everything built
		fullBuild();
		IProject[] projects = getEnv().getProjectBuildOrder();

		// WARM-UP
		for (int j = 0; j < 2; j++) {
			orderedBuild(projects);
		}
		
		// TEST
		for (int j = 0; j < 15; j++) {
			startMeasuring();
			
			// *** build each project ***
			for (int i = 0; i < projects.length; i++) {
				projects[i].build(IncrementalProjectBuilder.CLEAN_BUILD, ApiPlugin.BUILDER_ID, null, null);
				projects[i].build(IncrementalProjectBuilder.FULL_BUILD, ApiPlugin.BUILDER_ID, null, null);
			}
			
			stopMeasuring();
		}
		
		commitMeasurements();
		assertPerformance();
	}	
	
	
}
