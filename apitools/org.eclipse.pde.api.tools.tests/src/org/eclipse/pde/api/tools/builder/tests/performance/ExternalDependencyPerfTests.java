/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
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

import org.eclipse.pde.api.tools.apiusescan.tests.ExternalDependencyTestUtils;

public class ExternalDependencyPerfTests extends PerformanceTest {

	/**
	 * @param name
	 */
	public ExternalDependencyPerfTests(String name) {
		super(name);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.performance.PerformanceTest#setUp()
	 */
	protected void setUp() throws Exception {
		enableExternalDependencyCheckOptions(true);
		String location = ExternalDependencyTestUtils.setupReport("api-ws", true);
		if (location == null) {
			fail("Could not setup the report : api-ws.zip");
		}
		super.setUp();
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.performance.PerformanceTest#getWorkspaceLocation()
	 */
	protected String getWorkspaceLocation() {
		return getTestSourcePath().append("source-ws.zip").toOSString();
	}
	
	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(ExternalDependencyPerfTests.class);
	}
	
	public void testIncrementalBuildTests() throws Exception {
		IncrementalBuildTests incBuildTests = new IncrementalBuildTests("IncrementalBuildTests with External Dependency checks");
		incBuildTests.setUp();
		incBuildTests.testIncrementalBuildAll();
	}
	
	public void testEnumIncrementalBuildTests() throws Exception {
		EnumIncrementalBuildTests enumIncBuildTests = new EnumIncrementalBuildTests("EnumIncrementalBuildTests with External Dependency check");
		enumIncBuildTests.setUp();
		enumIncBuildTests.testIncremantalEnum();
	}
	
	
	public void testAnnotationIncrementalBuildTests() throws Exception {
		AnnotationIncrementalBuildTests annotIncBuildTests = new AnnotationIncrementalBuildTests("AnnotationIncrementalBuildTests with External Dependency check");
		annotIncBuildTests.setUp();
		annotIncBuildTests.testIncrementalAnnot();
	}
	
	
	public void testFullSourceBuildTests() throws Exception {
		FullSourceBuildTests fullSrcBuildTests = new FullSourceBuildTests("FullSourceBuildTests with External Dependency check");
		fullSrcBuildTests.setUp();
		fullSrcBuildTests.testCleanFullBuild();
		fullSrcBuildTests.testFullBuild();
	}

}
