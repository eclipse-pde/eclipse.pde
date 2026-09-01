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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExternalDependencyPerfTests extends PerformanceTest {

	@Override
	protected void setUp() throws Exception {
		enableExternalDependencyCheckOptions(true);
		String location = ExternalDependencyTestUtils.setupReport("api-ws", true); //$NON-NLS-1$
		if (location == null) {
			Assertions.fail("Could not setup the report : api-ws.zip"); //$NON-NLS-1$
		}
		super.setUp();
	}

	@Override
	protected String getWorkspaceLocation() {
		return getTestSourcePath().append("source-ws.zip").toOSString(); //$NON-NLS-1$
	}

	@Test

	public void testIncrementalBuildTests() throws Exception {
		IncrementalBuildTests incBuildTests = new IncrementalBuildTests();
		incBuildTests.setUp();
		incBuildTests.testIncrementalBuildAll();
	}

	@Test

	public void testEnumIncrementalBuildTests() throws Exception {
		EnumIncrementalBuildTests enumIncBuildTests = new EnumIncrementalBuildTests();
		enumIncBuildTests.setUp();
		enumIncBuildTests.testIncremantalEnum();
	}

	@Test

	public void testAnnotationIncrementalBuildTests() throws Exception {
		AnnotationIncrementalBuildTests annotIncBuildTests = new AnnotationIncrementalBuildTests();
		annotIncBuildTests.setUp();
		annotIncBuildTests.testIncrementalAnnot();
	}

	@Test

	public void testFullSourceBuildTests() throws Exception {
		FullSourceBuildTests fullSrcBuildTests = new FullSourceBuildTests();
		fullSrcBuildTests.setUp();
		fullSrcBuildTests.testCleanFullBuild();
		fullSrcBuildTests.testFullBuild();
	}

}
