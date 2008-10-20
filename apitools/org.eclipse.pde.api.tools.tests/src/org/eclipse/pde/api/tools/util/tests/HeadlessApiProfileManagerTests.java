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

import org.eclipse.pde.api.tools.internal.model.ApiBaselineManager;
import org.eclipse.pde.api.tools.internal.model.ApiModelFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.tests.AbstractApiTest;

/**
 * Tests that the {@link ApiProfileManager} is usable in a predictable way in a headless
 * environment
 */
public class HeadlessApiProfileManagerTests extends AbstractApiTest {
	
	private ApiBaselineManager fManager = ApiBaselineManager.getManager();
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		fManager.stop();
		super.tearDown();
	}
	
	/**
	 * Tests that we can get an API profile that exists from the manager 
	 */
	public void testGetApiProfile() {
		IApiBaseline profile = ApiModelFactory.newApiBaseline("test1");
		fManager.addApiBaseline(profile);
		profile = fManager.getApiBaseline("test1");
		assertNotNull("the test1 profile must exist in the manager", profile);
		assertTrue("the found profile must be test1", profile.getName().equals("test1"));
	}
	
	/**
	 * Tests that looking up a profile that does not exist in the manager returns null
	 */
	public void testGetNonExistantProfile() {
		IApiBaseline profile = fManager.getApiBaseline("fooprofile");
		assertNull("There should be no profile found", profile);
	}
	
	/**
	 * Tests that setting the default profile works
	 */
	public void testSetDefaultProfile() {
		IApiBaseline profile = ApiModelFactory.newApiBaseline("test2");
		fManager.addApiBaseline(profile);
		fManager.setDefaultApiBaseline(profile.getName());
		profile = fManager.getDefaultApiBaseline();
		assertNotNull("the default profile should not be null", profile);
		assertTrue("the default profiles' name should be test2", profile.getName().equals("test2"));
	}
	
	/**
	 * Tests that setting the default profile to one that does not exist in the manager will return null
	 * when asked for the default.
	 */
	public void testGetWrongDefault() {
		fManager.setDefaultApiBaseline("fooprofile");
		IApiBaseline profile = fManager.getDefaultApiBaseline();
		assertNull("the default profile should be null for a non-existant id", profile);
	}
	
	/**
	 * Tests getting all profiles from the manager
	 */
	public void testGetAllProfiles() {
		IApiBaseline profile = ApiModelFactory.newApiBaseline("test1");
		fManager.addApiBaseline(profile);
		profile = ApiModelFactory.newApiBaseline("test2");
		fManager.addApiBaseline(profile);
		IApiBaseline[] profiles = fManager.getApiBaselines();
		assertEquals("there should be 2 profiles", 2, profiles.length);
	}
	
	/**
	 * Tests removing an existing profile from the manager
	 */
	public void testRemoveApiProfile() {
		IApiBaseline profile = ApiModelFactory.newApiBaseline("test2");
		fManager.addApiBaseline(profile);
		boolean result = fManager.removeApiBaseline("test2");
		assertTrue("the profile test2 should have been removed from the manager", result);
		assertTrue("There should only be 0 profiles left", fManager.getApiBaselines().length == 0);
	}
	
	/**
	 * Tests that isExistingProfileName(..) returns return true when expected to 
	 */
	public void testIsExistingName() {
		IApiBaseline profile = ApiModelFactory.newApiBaseline("test1");
		fManager.addApiBaseline(profile);
		boolean result = fManager.isExistingProfileName("test1");
		assertTrue("the name test1 should be an existing name", result);
	}
	
	/**
	 * Tests that isExistingProfileName returns false when asked about an non-existent name
	 */
	public void testisExistingName2() {
		boolean result = fManager.isExistingProfileName("fooprofile");
		assertFalse("fooprofile is not an existing name", result);
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
	 * Tests that the workspace profile is null in headless mode
	 */
	public void testGetWorkspaceProfile() {
		IApiBaseline profile = fManager.getWorkspaceBaseline();
		if(ApiPlugin.isRunningInFramework()) {
			assertNotNull("the workspace profile must not be null with the framework running", profile);
		}
		else {
			assertNull("the workspace profile must be null in headless mode", profile);
		}
	}
	
	/**
	 * Tests that calling the stop method does not fail, and works
	 */
	public void testStop() {
		try {
			fManager.stop();
			assertTrue("There should be no api profiles in the manager", fManager.getApiBaselines().length == 0);
			//stop it again to free the memory from the map
			fManager.stop();
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
	}
}