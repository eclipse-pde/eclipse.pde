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
package org.eclipse.pde.api.tools.apiusescan.tests;

import junit.framework.TestCase;

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

public class ReferenceCountTests extends TestCase {

	private IApiBaseline fBaseline;
	private UseScanManager fUseScanManager;

	protected void setUp() throws Exception {
		IProject setupProject = ExternalDependencyTestUtils.setupProject();
		if (setupProject == null) {
			fail("Unable to setup the project. Can not run the test cases");
			return;
		}
		fUseScanManager = UseScanManager.getInstance();
		fBaseline = ApiPlugin.getDefault().getApiBaselineManager().getWorkspaceBaseline();
	}

	public void testReferenceCountReportAll() {
		String location = ExternalDependencyTestUtils.setupReport("reportAll", true);
		if (location == null) {
			fail("Could not setup the report : reportAll.zip");
		}
		IApiComponent apiComponent = fBaseline.getApiComponent(ExternalDependencyTestUtils.PROJECT_NAME);
		String[][] apiUseTpes = new String[][] { 
				{"tests.apiusescan.coretestproject.ClassWithInnerType"}, 
				{"tests.apiusescan.coretestproject.ClassWithInnerType", 
					"tests.apiusescan.coretestproject.IConstants"}, 
				{"tests.apiusescan.coretestproject.ITestInterface"}, 
				{"tests.apiusescan.coretestproject.TestInterfaceImpl"}
		};
		int[] expectedResult = new int[] {7, 9, 5, 6};
		verifyReferenceCount(apiComponent, apiUseTpes, expectedResult);		
	}

	public void verifyReferenceCount(IApiComponent apiComponent, String[][] apiUseTpes, int[] expectedResult) {
		String errorMessage = "Incorrect number of references for the set {0}";
		for (int i = 0; i < apiUseTpes.length; i++) {
			IReferenceDescriptor[] dependencies = UseScanManager.getInstance().getExternalDependenciesFor(apiComponent, apiUseTpes[i], new NullProgressMonitor());
			assertEquals(NLS.bind(errorMessage, i + 1), expectedResult[i], dependencies.length);
		}
		fUseScanManager.clearCache();
	}

	public void testReferenceCountReportOne() {
		String location = ExternalDependencyTestUtils.setupReport("reportOne", false);
		if (location == null) {
			fail("Could not setup the report : reportOne.zip");
		}
		IApiComponent apiComponent = fBaseline.getApiComponent(ExternalDependencyTestUtils.PROJECT_NAME);
		String[][] apiUseTpes = new String[][] { 
				{"tests.apiusescan.coretestproject.ClassWithInnerType"}, 
				{"tests.apiusescan.coretestproject.IConstants"}, 
				{"tests.apiusescan.coretestproject.ITestInterface"}, 
				{"tests.apiusescan.coretestproject.TestInterfaceImpl"}
		};
		int[] expectedResult = new int[] {7, 1, 3, 2};
		verifyReferenceCount(apiComponent, apiUseTpes, expectedResult);
	}

	public void testReferenceCountReportTwo() {
		String location = ExternalDependencyTestUtils.setupReport("reportTwo", false);		
		if (location == null) {
			fail("Could not setup the report : reportTwo.zip");
		}
		IApiComponent apiComponent = fBaseline.getApiComponent(ExternalDependencyTestUtils.PROJECT_NAME);
		String[][] apiUseTpes = new String[][] { 
				{"tests.apiusescan.coretestproject.ClassWithInnerType"}, 
				{"tests.apiusescan.coretestproject.IConstants", 
					"tests.apiusescan.coretestproject.ITestInterface", 
					"tests.apiusescan.coretestproject.TestInterfaceImpl"}, 
				{}
		};
		int[] expectedResult = new int[] {0, 7, 7};
		verifyReferenceCount(apiComponent, apiUseTpes, expectedResult);
	}
	
	public void testCacheSize() {
		fUseScanManager.clearCache();
		fUseScanManager.setCacheSize(15);
		
		String reportLocation = ExternalDependencyTestUtils.setupReport("PDEApiUseScanReport", true);
		if (reportLocation == null) {
			fail("Could not setup the report : PDEApiUseScanReport.zip");
		}
		IApiComponent apiComponent = TestSuiteHelper.createTestingApiComponent("org.eclipse.equinox.app", "org.eclipse.equinox.app", new ApiDescription(null));

		IReferenceDescriptor[] dependencies = fUseScanManager.getExternalDependenciesFor(apiComponent, null, null);
		assertEquals("Incorrect number of references for org.eclipse.equinox.app", 13, dependencies.length);
		
		IReferenceCollection useScanRefs =  (IReferenceCollection) apiComponent.getExternalDependencies();
		assertTrue("References for org.eclipse.equinox.app.IApplication not found in cache", 
				useScanRefs.hasReferencesTo("org.eclipse.equinox.app.IApplication"));
		
		apiComponent = TestSuiteHelper.createTestingApiComponent("org.eclipse.equinox.p2.operations", "org.eclipse.equinox.p2.operations", new ApiDescription(null));
		dependencies = fUseScanManager.getExternalDependenciesFor(apiComponent, null, null);
		assertEquals("Incorrect number of references for org.eclipse.equinox.p2.operations", 17, dependencies.length);
		
		useScanRefs =  (IReferenceCollection) apiComponent.getExternalDependencies();
		assertTrue("References for org.eclipse.equinox.p2.operations.InstallOperation not found in cache",
				useScanRefs.hasReferencesTo("org.eclipse.equinox.p2.operations.InstallOperation"));
		
		assertFalse("References for org.eclipse.equinox.app.IApplication should have been purged from the cache",
				useScanRefs.hasReferencesTo("org.eclipse.equinox.app.IApplication"));
	}
}
