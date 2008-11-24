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
import org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;
import org.eclipse.test.performance.Dimension;

/**
 * Performance tests for API descriptions
 * 
 * @since 1.0
 */
public class ApiDescriptionTests extends PerformanceTest {
	
	/**
	 * Constructor
	 * @param name
	 */
	public ApiDescriptionTests(String name) {
		super(name);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.performance.PerformanceTest#getWorkspaceLocation()
	 */
	protected String getWorkspaceLocation() {
		return TestSuiteHelper.getPluginDirectoryPath().append(TEST_SOURCE_ROOT).append("perf").append("jdtui-source.zip").toOSString();
	}
	
	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(ApiDescriptionTests.class);
	}
	
	/**
	 * Tests a clean and visit jdt-ui source project. Populates the entire API description.
	 * 
	 * @throws Exception
	 */
	public void testCleanVisit() throws Exception {
		tagAsGlobalSummary("Build API description from source tags", Dimension.ELAPSED_PROCESS);
		
		// WARM-UP
		IProject proj = getEnv().getWorkspace().getRoot().getProject("org.eclipse.jdt.ui");	
		ApiDescriptionVisitor visitor = new ApiDescriptionVisitor() {};
		for (int j = 0; j < 2; j++) {
			// *** clean & visit API description ***
			proj.build(IncrementalProjectBuilder.CLEAN_BUILD, ApiPlugin.BUILDER_ID, null, null);
			IApiBaseline profile = ApiPlugin.getDefault().getApiProfileManager().getWorkspaceBaseline();
			IApiComponent component = profile.getApiComponent(proj.getName());
			component.getApiDescription().accept(visitor);
		}
		
		// TEST
		for (int j = 0; j < 15; j++) {
			
			// *** clean API description ***
			proj.build(IncrementalProjectBuilder.CLEAN_BUILD, ApiPlugin.BUILDER_ID, null, null);
			IApiBaseline profile = ApiPlugin.getDefault().getApiProfileManager().getWorkspaceBaseline();
			IApiComponent component = profile.getApiComponent(proj.getName());
				
			// ** Visit API description ***
			startMeasuring();
			component.getApiDescription().accept(visitor);
			stopMeasuring();
		}
		
		commitMeasurements();
		assertPerformance();
	}	
}
