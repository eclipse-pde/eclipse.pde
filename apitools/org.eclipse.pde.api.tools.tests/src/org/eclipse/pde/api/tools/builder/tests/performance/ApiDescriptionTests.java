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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;
import org.eclipse.test.performance.Dimension;

import junit.framework.Test;

/**
 * Performance tests for API descriptions
 *
 * @since 1.0
 */
public class ApiDescriptionTests extends PerformanceTest {

	public ApiDescriptionTests(String name) {
		super(name);
	}

	@Override
	protected String getWorkspaceLocation() {
		return TestSuiteHelper.getPluginDirectoryPath().append(TEST_SOURCE_ROOT).append("perf").append("jdtui-source.zip").toOSString(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(ApiDescriptionTests.class);
	}

	/**
	 * Tests a clean and visit jdt-ui source project. Populates the entire API
	 * description.
	 *
	 * @throws Exception
	 */
	public void testCleanVisit() throws Exception {
		tagAsSummary("Build API description from source tags", Dimension.ELAPSED_PROCESS); //$NON-NLS-1$

		// WARM-UP
		IProject proj = getEnv().getWorkspace().getRoot().getProject("org.eclipse.jdt.ui"); //$NON-NLS-1$
		ApiDescriptionVisitor visitor = new ApiDescriptionVisitor() {
		};
		for (int j = 0; j < 2; j++) {
			// *** clean & visit API description ***
			proj.build(IncrementalProjectBuilder.CLEAN_BUILD, ApiPlugin.BUILDER_ID, null, null);
			IApiBaseline baseline = ApiPlugin.getDefault().getApiBaselineManager().getWorkspaceBaseline();
			IApiComponent component = baseline.getApiComponent(proj.getName());
			component.getApiDescription().accept(visitor, null);
		}

		// TEST
		for (int j = 0; j < 15; j++) {

			// *** clean API description ***
			proj.build(IncrementalProjectBuilder.CLEAN_BUILD, ApiPlugin.BUILDER_ID, null, null);
			IApiBaseline baseline = ApiPlugin.getDefault().getApiBaselineManager().getWorkspaceBaseline();
			IApiComponent component = baseline.getApiComponent(proj.getName());

			// ** Visit API description ***
			startMeasuring();
			component.getApiDescription().accept(visitor, null);
			stopMeasuring();
		}

		commitMeasurements();
		assertPerformance();
	}
}
