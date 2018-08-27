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
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.test.performance.Dimension;

import junit.framework.Test;

/**
 * Performance tests for full source workspace build
 *
 * @since 1.0
 */
public class FullSourceBuildTests extends PerformanceTest {

	public FullSourceBuildTests(String name) {
		super(name);
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(FullSourceBuildTests.class);
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
	 * Tests a full build of a 3.4 workspace with source from debug.core and pre-reqs
	 * against a baseline of 3.3 binary plug-ins.
	 *
	 * @throws Exception
	 */
	public void testFullBuild() throws Exception {
		tagAsSummary("Full Build", Dimension.ELAPSED_PROCESS); //$NON-NLS-1$

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
		tagAsSummary("Clean & Full Build", Dimension.ELAPSED_PROCESS); //$NON-NLS-1$

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
