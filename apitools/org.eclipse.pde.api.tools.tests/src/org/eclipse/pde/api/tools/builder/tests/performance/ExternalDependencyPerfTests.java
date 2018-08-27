/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
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

import org.eclipse.pde.api.tools.apiusescan.tests.ExternalDependencyTestUtils;

import junit.framework.Test;

public class ExternalDependencyPerfTests extends PerformanceTest {

	public ExternalDependencyPerfTests(String name) {
		super(name);
	}

	@Override
	protected void setUp() throws Exception {
		enableExternalDependencyCheckOptions(true);
		String location = ExternalDependencyTestUtils.setupReport("api-ws", true); //$NON-NLS-1$
		if (location == null) {
			fail("Could not setup the report : api-ws.zip"); //$NON-NLS-1$
		}
		super.setUp();
	}


	@Override
	protected String getWorkspaceLocation() {
		return getTestSourcePath().append("source-ws.zip").toOSString(); //$NON-NLS-1$
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(ExternalDependencyPerfTests.class);
	}

	public void testIncrementalBuildTests() throws Exception {
		IncrementalBuildTests incBuildTests = new IncrementalBuildTests("IncrementalBuildTests with External Dependency checks"); //$NON-NLS-1$
		incBuildTests.setUp();
		incBuildTests.testIncrementalBuildAll();
	}

	public void testEnumIncrementalBuildTests() throws Exception {
		EnumIncrementalBuildTests enumIncBuildTests = new EnumIncrementalBuildTests("EnumIncrementalBuildTests with External Dependency check"); //$NON-NLS-1$
		enumIncBuildTests.setUp();
		enumIncBuildTests.testIncremantalEnum();
	}


	public void testAnnotationIncrementalBuildTests() throws Exception {
		AnnotationIncrementalBuildTests annotIncBuildTests = new AnnotationIncrementalBuildTests("AnnotationIncrementalBuildTests with External Dependency check"); //$NON-NLS-1$
		annotIncBuildTests.setUp();
		annotIncBuildTests.testIncrementalAnnot();
	}


	public void testFullSourceBuildTests() throws Exception {
		FullSourceBuildTests fullSrcBuildTests = new FullSourceBuildTests("FullSourceBuildTests with External Dependency check"); //$NON-NLS-1$
		fullSrcBuildTests.setUp();
		fullSrcBuildTests.testCleanFullBuild();
		fullSrcBuildTests.testFullBuild();
	}

}
