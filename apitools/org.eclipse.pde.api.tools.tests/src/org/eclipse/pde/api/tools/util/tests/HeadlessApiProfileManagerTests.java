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

import org.eclipse.pde.api.tools.internal.ApiProfileManager;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.tests.AbstractApiTest;

/**
 * Tests that the {@link ApiProfileManager} is usable in a predictable way in a headless
 * environment
 */
public class HeadlessApiProfileManagerTests extends AbstractApiTest {
	
	private ApiProfileManager fManager = ApiProfileManager.getManager();
	
	/**
	 * Tests that we can get an API profile that exists from the manager 
	 */
	public void testGetApiProfile() {
		IApiProfile profile = Factory.newApiProfile("test1");
		fManager.addApiProfile(profile);
		profile = fManager.getApiProfile("test1");
		assertNotNull("the test1 profile must exist in the manager", profile);
		assertTrue("the found profile must be test1", profile.getName().equals("test1"));
	}
	
	/**
	 * Tests that looking up a profile that does not exist in the manager returns null
	 */
	public void testGetNonExistantProfile() {
		IApiProfile profile = fManager.getApiProfile("fooprofile");
		assertNull("There should be no profile found", profile);
	}
	
	/**
	 * Tests that setting the default profile works
	 */
	public void testSetDefaultProfile() {
		IApiProfile profile = Factory.newApiProfile("test2");
		fManager.addApiProfile(profile);
		fManager.setDefaultApiProfile(profile.getName());
		profile = fManager.getDefaultApiProfile();
		assertNotNull("the default profile should not be null", profile);
		assertTrue("the default profiles' name should be test2", profile.getName().equals("test2"));
	}
	
	/**
	 * Tests that setting the default profile to one that does not exist in the manager will return null
	 * when asked for the default.
	 */
	public void testGetWrongDefault() {
		fManager.setDefaultApiProfile("fooprofile");
		IApiProfile profile = fManager.getDefaultApiProfile();
		assertNull("the default profile should be null for a non-existant id", profile);
	}
	
	/**
	 * Tests getting all profiles from the manager
	 */
	public void testGetAllProfiles() {
		IApiProfile[] profiles = fManager.getApiProfiles();
		assertTrue("there should be 2 profiles", profiles.length == 2);
	}
	
	/**
	 * Tests removing an existing profile from the manager
	 */
	public void testRemoveApiProfile() {
		boolean result = fManager.removeApiProfile("test2");
		assertTrue("the profile test2 should have been removed from the manager", result);
		assertTrue("There should only be 1 profile left", fManager.getApiProfiles().length == 1);
	}
	
	/**
	 * Tests trying to remove an non-existent profile from the manager
	 */
	public void testRemoveNonExistentProfile() {
		boolean result = fManager.removeApiProfile("fooprofile");
		assertFalse("no profiles should have been removed", result);
		assertTrue("There should still be 1 profile left", fManager.getApiProfiles().length == 1);
	}
	
	/**
	 * Tests that isExistingProfileName(..) returns return true when expected to 
	 */
	public void testIsExistingName() {
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
		try {
			fManager.saving(null);
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests that calling the doneSaving(..) method on the manager does not fail in 
	 * headless mode
	 */
	public void testDoneSavingCall() {
		try {
			fManager.doneSaving(null);
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests that calling preparingToSave(..) does not fail in headless mode
	 */
	public void testPreparingToSave() {
		try {
			fManager.prepareToSave(null);
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests that calling rollback(..) does not fail in headless mode
	 */
	public void testRollback() {
		try {
			fManager.rollback(null);
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests that a call to the resourceChanged(..) method does not fail in 
	 * headless mode
	 */
	public void testResourceChanged() {
		try {
			fManager.resourceChanged(null);
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests that a call to elementChanged(..) does not fail in headless mode
	 */
	public void testElementChanged() {
		try {
			fManager.elementChanged(null);
		}
		catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests that the workspace profile is null in headless mode
	 */
	public void testGetWorkspaceProfile() {
		IApiProfile profile = fManager.getWorkspaceProfile();
		assertNull("the workspace profile must be null in headless mode", profile);
	}
	
	/**
	 * Tests that calling the stop method does not fail, and works
	 */
	public void testStop() {
		try {
			fManager.stop();
			assertTrue("There should be no api profiles in the manager", fManager.getApiProfiles().length == 0);
			//stop it again to free the memory from the map
			fManager.stop();
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
	}
}