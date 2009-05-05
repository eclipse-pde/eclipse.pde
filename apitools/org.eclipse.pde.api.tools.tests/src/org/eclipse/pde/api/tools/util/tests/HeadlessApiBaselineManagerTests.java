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
package org.eclipse.pde.api.tools.util.tests;

import org.eclipse.pde.api.tools.internal.ApiBaselineManager;
import org.eclipse.pde.api.tools.internal.model.ApiModelFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.tests.AbstractApiTest;

/**
 * Tests that the {@link ApiBaselineManager} is usable in a predictable way in a headless
 * environment
 */
public class HeadlessApiBaselineManagerTests extends AbstractApiTest {
	
	private ApiBaselineManager fManager = ApiBaselineManager.getManager();
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		fManager.stop();
		super.tearDown();
	}
	
	/**
	 * Tests that we can get an API baseline that exists from the manager 
	 */
	public void testGetApiProfile() {
		IApiBaseline baseline = ApiModelFactory.newApiBaseline("test1");
		fManager.addApiBaseline(baseline);
		baseline = fManager.getApiBaseline("test1");
		assertNotNull("the test1 baseline must exist in the manager", baseline);
		assertTrue("the found baseline must be test1", baseline.getName().equals("test1"));
	}
	
	/**
	 * Tests that looking up a baseline that does not exist in the manager returns null
	 */
	public void testGetNonExistantProfile() {
		IApiBaseline baseline = fManager.getApiBaseline("foobaseline");
		assertNull("There should be no baseline found", baseline);
	}
	
	/**
	 * Tests that setting the default baseline works
	 */
	public void testSetDefaultProfile() {
		IApiBaseline baseline = ApiModelFactory.newApiBaseline("test2");
		fManager.addApiBaseline(baseline);
		fManager.setDefaultApiBaseline(baseline.getName());
		baseline = fManager.getDefaultApiBaseline();
		assertNotNull("the default baseline should not be null", baseline);
		assertTrue("the default baselines' name should be test2", baseline.getName().equals("test2"));
	}
	
	/**
	 * Tests that setting the default baseline to one that does not exist in the manager will return null
	 * when asked for the default.
	 */
	public void testGetWrongDefault() {
		fManager.setDefaultApiBaseline("foobaseline");
		IApiBaseline baseline = fManager.getDefaultApiBaseline();
		assertNull("the default baseline should be null for a non-existant id", baseline);
	}
	
	/**
	 * Tests getting all baselines from the manager
	 */
	public void testGetAllProfiles() {
		IApiBaseline baseline = ApiModelFactory.newApiBaseline("test1");
		fManager.addApiBaseline(baseline);
		baseline = ApiModelFactory.newApiBaseline("test2");
		fManager.addApiBaseline(baseline);
		IApiBaseline[] baselines = fManager.getApiBaselines();
		assertEquals("there should be 2 baselines", 2, baselines.length);
	}
	
	/**
	 * Tests removing an existing baseline from the manager
	 */
	public void testRemoveApiProfile() {
		IApiBaseline baseline = ApiModelFactory.newApiBaseline("test2");
		fManager.addApiBaseline(baseline);
		boolean result = fManager.removeApiBaseline("test2");
		assertTrue("the baseline test2 should have been removed from the manager", result);
		assertTrue("There should only be 0 baselines left", fManager.getApiBaselines().length == 0);
	}
	
	/**
	 * Tests that isExistingProfileName(..) returns return true when expected to 
	 */
	public void testIsExistingName() {
		IApiBaseline baseline = ApiModelFactory.newApiBaseline("test1");
		fManager.addApiBaseline(baseline);
		boolean result = fManager.isExistingBaselineName("test1");
		assertTrue("the name test1 should be an existing name", result);
	}
	
	/**
	 * Tests that isExistingProfileName returns false when asked about an non-existent name
	 */
	public void testisExistingName2() {
		boolean result = fManager.isExistingBaselineName("foobaseline");
		assertFalse("foobaseline is not an existing name", result);
	}
	
	/**
	 * Tests that calling the saving(..) method on the manager in headless mode does not fail
	 */
	public void testSavingCall() {
		if(!ApiPlugin.isRunningInFramework()) {
			try {
				fManager.saving(null);
			}
			catch(Exception e) {
				fail(e.getMessage());
			}
		}
	}
	
	/**
	 * Tests that calling the doneSaving(..) method on the manager does not fail in 
	 * headless mode
	 */
	public void testDoneSavingCall() {
		if(!ApiPlugin.isRunningInFramework()) {
			try {
				fManager.doneSaving(null);
			}
			catch(Exception e) {
				fail(e.getMessage());
			}
		}
	}
	
	/**
	 * Tests that calling preparingToSave(..) does not fail in headless mode
	 */
	public void testPreparingToSave() {
		if(!ApiPlugin.isRunningInFramework()) {
			try {
				fManager.prepareToSave(null);
			}
			catch(Exception e) {
				fail(e.getMessage());
			}
		}
	}
	
	/**
	 * Tests that calling rollback(..) does not fail in headless mode
	 */
	public void testRollback() {
		if(!ApiPlugin.isRunningInFramework()) {
			try {
				fManager.rollback(null);
			}
			catch(Exception e) {
				fail(e.getMessage());
			}
		}
	}
	
	/**
	 * Tests that a call to the resourceChanged(..) method does not fail in 
	 * headless mode
	 */
	public void testResourceChanged() {
		if(!ApiPlugin.isRunningInFramework()) {
			try {
				fManager.resourceChanged(null);
			}
			catch(Exception e) {
				fail(e.getMessage());
			}
		}
	}
	
	/**
	 * Tests that a call to elementChanged(..) does not fail in headless mode
	 */
	public void testElementChanged() {
		if(!ApiPlugin.isRunningInFramework()) {
			try {
				fManager.elementChanged(null);
			}
			catch (Exception e) {
				fail(e.getMessage());
			}
		}
	}
	
	/**
	 * Tests that the workspace baseline is null in headless mode
	 */
	public void testGetWorkspaceProfile() {
		IApiBaseline baseline = fManager.getWorkspaceBaseline();
		if(ApiPlugin.isRunningInFramework()) {
			assertNotNull("the workspace baseline must not be null with the framework running", baseline);
		}
		else {
			assertNull("the workspace baseline must be null in headless mode", baseline);
		}
	}
	
	/**
	 * Tests that calling the stop method does not fail, and works
	 */
	public void testStop() {
		try {
			fManager.stop();
			assertTrue("There should be no api baselines in the manager", fManager.getApiBaselines().length == 0);
			//stop it again to free the memory from the map
			fManager.stop();
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
	}
}