/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.apiusescan.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.ApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.search.IReferenceCollection;
import org.eclipse.pde.api.tools.internal.search.IReferenceDescriptor;
import org.eclipse.pde.api.tools.internal.search.UseScanManager;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;
import org.junit.Before;
import org.junit.Test;

public class ReferenceCountTests {

	private IApiBaseline fBaseline;
	private UseScanManager fUseScanManager;

	@Before
	public void setUp() throws Exception {
		IProject setupProject = ExternalDependencyTestUtils.setupProject();
		if (setupProject == null) {
			fail("Unable to setup the project. Can not run the test cases"); //$NON-NLS-1$
			return;
		}
		fUseScanManager = UseScanManager.getInstance();
		fBaseline = ApiPlugin.getDefault().getApiBaselineManager().getWorkspaceBaseline();
	}

	@Test
	public void testReferenceCountReportAll() {
		String location = ExternalDependencyTestUtils.setupReport("reportAll", true); //$NON-NLS-1$
		if (location == null) {
			fail("Could not setup the report : reportAll.zip"); //$NON-NLS-1$
		}
		IApiComponent apiComponent = fBaseline.getApiComponent(ExternalDependencyTestUtils.PROJECT_NAME);
		String[][] apiUseTpes = new String[][] {
				{"tests.apiusescan.coretestproject.ClassWithInnerType"},  //$NON-NLS-1$
				{"tests.apiusescan.coretestproject.ClassWithInnerType",  //$NON-NLS-1$
					"tests.apiusescan.coretestproject.IConstants"},  //$NON-NLS-1$
				{"tests.apiusescan.coretestproject.ITestInterface"},  //$NON-NLS-1$
				{"tests.apiusescan.coretestproject.TestInterfaceImpl"} //$NON-NLS-1$
		};
		int[] expectedResult = new int[] {7, 9, 5, 6};
		verifyReferenceCount(apiComponent, apiUseTpes, expectedResult);
	}

	public void verifyReferenceCount(IApiComponent apiComponent, String[][] apiUseTpes, int[] expectedResult) {
		String errorMessage = "Incorrect number of references for the set {0}"; //$NON-NLS-1$
		for (int i = 0; i < apiUseTpes.length; i++) {
			IReferenceDescriptor[] dependencies = UseScanManager.getInstance().getExternalDependenciesFor(apiComponent, apiUseTpes[i], new NullProgressMonitor());
			assertEquals(NLS.bind(errorMessage, i + 1), expectedResult[i], dependencies.length);
		}
		fUseScanManager.clearCache();
	}

	@Test
	public void testReferenceCountReportOne() {
		String location = ExternalDependencyTestUtils.setupReport("reportOne", false); //$NON-NLS-1$
		if (location == null) {
			fail("Could not setup the report : reportOne.zip"); //$NON-NLS-1$
		}
		IApiComponent apiComponent = fBaseline.getApiComponent(ExternalDependencyTestUtils.PROJECT_NAME);
		String[][] apiUseTpes = new String[][] {
				{"tests.apiusescan.coretestproject.ClassWithInnerType"},  //$NON-NLS-1$
				{"tests.apiusescan.coretestproject.IConstants"},  //$NON-NLS-1$
				{"tests.apiusescan.coretestproject.ITestInterface"},  //$NON-NLS-1$
				{"tests.apiusescan.coretestproject.TestInterfaceImpl"} //$NON-NLS-1$
		};
		int[] expectedResult = new int[] {7, 1, 3, 2};
		verifyReferenceCount(apiComponent, apiUseTpes, expectedResult);
	}

	@Test
	public void testReferenceCountReportTwo() {
		String location = ExternalDependencyTestUtils.setupReport("reportTwo", false);		 //$NON-NLS-1$
		if (location == null) {
			fail("Could not setup the report : reportTwo.zip"); //$NON-NLS-1$
		}
		IApiComponent apiComponent = fBaseline.getApiComponent(ExternalDependencyTestUtils.PROJECT_NAME);
		String[][] apiUseTpes = new String[][] {
				{"tests.apiusescan.coretestproject.ClassWithInnerType"},  //$NON-NLS-1$
				{"tests.apiusescan.coretestproject.IConstants",  //$NON-NLS-1$
					"tests.apiusescan.coretestproject.ITestInterface",  //$NON-NLS-1$
					"tests.apiusescan.coretestproject.TestInterfaceImpl"},  //$NON-NLS-1$
				{}
		};
		int[] expectedResult = new int[] {0, 7, 7};
		verifyReferenceCount(apiComponent, apiUseTpes, expectedResult);
	}

	@Test
	public void testCacheSize() {
		fUseScanManager.clearCache();
		fUseScanManager.setCacheSize(15);

		String reportLocation = ExternalDependencyTestUtils.setupReport("PDEApiUseScanReport", true); //$NON-NLS-1$
		if (reportLocation == null) {
			fail("Could not setup the report : PDEApiUseScanReport.zip"); //$NON-NLS-1$
		}
		IApiComponent apiComponent = TestSuiteHelper.createTestingApiComponent("org.eclipse.equinox.app", "org.eclipse.equinox.app", new ApiDescription(null)); //$NON-NLS-1$ //$NON-NLS-2$

		IReferenceDescriptor[] dependencies = fUseScanManager.getExternalDependenciesFor(apiComponent, null, null);
		assertEquals("Incorrect number of references for org.eclipse.equinox.app", 13, dependencies.length); //$NON-NLS-1$

		IReferenceCollection useScanRefs =  apiComponent.getExternalDependencies();
		assertTrue("References for org.eclipse.equinox.app.IApplication not found in cache",  //$NON-NLS-1$
				useScanRefs.hasReferencesTo("org.eclipse.equinox.app.IApplication")); //$NON-NLS-1$

		apiComponent = TestSuiteHelper.createTestingApiComponent("org.eclipse.equinox.p2.operations", "org.eclipse.equinox.p2.operations", new ApiDescription(null)); //$NON-NLS-1$ //$NON-NLS-2$
		dependencies = fUseScanManager.getExternalDependenciesFor(apiComponent, null, null);
		assertEquals("Incorrect number of references for org.eclipse.equinox.p2.operations", 17, dependencies.length); //$NON-NLS-1$

		useScanRefs =  apiComponent.getExternalDependencies();
		assertTrue("References for org.eclipse.equinox.p2.operations.InstallOperation not found in cache", //$NON-NLS-1$
				useScanRefs.hasReferencesTo("org.eclipse.equinox.p2.operations.InstallOperation")); //$NON-NLS-1$

		assertFalse("References for org.eclipse.equinox.app.IApplication should have been purged from the cache", //$NON-NLS-1$
				useScanRefs.hasReferencesTo("org.eclipse.equinox.app.IApplication")); //$NON-NLS-1$
	}
}
