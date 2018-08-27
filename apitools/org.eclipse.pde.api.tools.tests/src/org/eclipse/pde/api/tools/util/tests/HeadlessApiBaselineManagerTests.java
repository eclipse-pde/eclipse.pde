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
package org.eclipse.pde.api.tools.util.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.ApiBaselineManager;
import org.eclipse.pde.api.tools.internal.model.ApiModelFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.tests.AbstractApiTest;
import org.junit.After;
import org.junit.Test;

/**
 * Tests that the {@link ApiBaselineManager} is usable in a predictable way in a headless
 * environment
 */
public class HeadlessApiBaselineManagerTests extends AbstractApiTest {

	private ApiBaselineManager fManager = ApiBaselineManager.getManager();

	@After
	public void tearDown() throws Exception {
		fManager.stop();
	}

	/**
	 * Tests that we can get an API baseline that exists from the manager
	 */
	@Test
	public void testGetApiProfile() {
		IApiBaseline baseline = ApiModelFactory.newApiBaseline("test1"); //$NON-NLS-1$
		fManager.addApiBaseline(baseline);
		baseline = fManager.getApiBaseline("test1"); //$NON-NLS-1$
		assertNotNull("the test1 baseline must exist in the manager", baseline); //$NON-NLS-1$
		assertTrue("the found baseline must be test1", baseline.getName().equals("test1")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests that looking up a baseline that does not exist in the manager returns null
	 */
	@Test
	public void testGetNonExistantProfile() {
		IApiBaseline baseline = fManager.getApiBaseline("foobaseline"); //$NON-NLS-1$
		assertNull("There should be no baseline found", baseline); //$NON-NLS-1$
	}

	/**
	 * Tests that setting the default baseline works
	 */
	@Test
	public void testSetDefaultProfile() {
		IApiBaseline baseline = ApiModelFactory.newApiBaseline("test2"); //$NON-NLS-1$
		fManager.addApiBaseline(baseline);
		fManager.setDefaultApiBaseline(baseline.getName());
		baseline = fManager.getDefaultApiBaseline();
		assertNotNull("the default baseline should not be null", baseline); //$NON-NLS-1$
		assertTrue("the default baselines' name should be test2", baseline.getName().equals("test2")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests that setting the default baseline to one that does not exist in the manager will return null
	 * when asked for the default.
	 */
	@Test
	public void testGetWrongDefault() {
		fManager.setDefaultApiBaseline("foobaseline"); //$NON-NLS-1$
		IApiBaseline baseline = fManager.getDefaultApiBaseline();
		assertNull("the default baseline should be null for a non-existant id", baseline); //$NON-NLS-1$
	}

	/**
	 * Tests getting all baselines from the manager
	 */
	@Test
	public void testGetAllProfiles() {
		IApiBaseline baseline = ApiModelFactory.newApiBaseline("test1"); //$NON-NLS-1$
		fManager.addApiBaseline(baseline);
		baseline = ApiModelFactory.newApiBaseline("test2"); //$NON-NLS-1$
		fManager.addApiBaseline(baseline);
		IApiBaseline[] baselines = fManager.getApiBaselines();
		assertEquals("there should be 2 baselines", 2, baselines.length); //$NON-NLS-1$
	}

	/**
	 * Tests removing an existing baseline from the manager
	 */
	@Test
	public void testRemoveApiProfile() {
		IApiBaseline baseline = ApiModelFactory.newApiBaseline("test2"); //$NON-NLS-1$
		fManager.addApiBaseline(baseline);
		boolean result = fManager.removeApiBaseline("test2"); //$NON-NLS-1$
		assertTrue("the baseline test2 should have been removed from the manager", result); //$NON-NLS-1$
		assertEquals("There should only be 0 baselines left", 0, fManager.getApiBaselines().length); //$NON-NLS-1$
	}

	/**
	 * Tests that isExistingProfileName(..) returns return true when expected to
	 */
	@Test
	public void testIsExistingName() {
		IApiBaseline baseline = ApiModelFactory.newApiBaseline("test1"); //$NON-NLS-1$
		fManager.addApiBaseline(baseline);
		boolean result = fManager.isExistingProfileName("test1"); //$NON-NLS-1$
		assertTrue("the name test1 should be an existing name", result); //$NON-NLS-1$
	}

	/**
	 * Tests that isExistingProfileName returns false when asked about an non-existent name
	 */
	@Test
	public void testisExistingName2() {
		boolean result = fManager.isExistingProfileName("foobaseline"); //$NON-NLS-1$
		assertFalse("foobaseline is not an existing name", result); //$NON-NLS-1$
	}

	/**
	 * Tests that calling the saving(..) method on the manager in headless mode
	 * does not fail
	 * 
	 * @throws CoreException
	 */
	@Test
	public void testSavingCall() throws CoreException {
		if (!ApiPlugin.isRunningInFramework()) {
			fManager.saving(null);
		}
	}

	/**
	 * Tests that calling the doneSaving(..) method on the manager does not fail in
	 * headless mode
	 */
	@Test
	public void testDoneSavingCall() {
		if (!ApiPlugin.isRunningInFramework()) {
			fManager.doneSaving(null);
		}
	}

	/**
	 * Tests that calling preparingToSave(..) does not fail in headless mode
	 *
	 * @throws CoreException
	 */
	@Test
	public void testPreparingToSave() throws CoreException {
		if (!ApiPlugin.isRunningInFramework()) {
			fManager.prepareToSave(null);
		}
	}

	/**
	 * Tests that calling rollback(..) does not fail in headless mode
	 */
	@Test
	public void testRollback() {
		if (!ApiPlugin.isRunningInFramework()) {
			fManager.rollback(null);
		}
	}

	/**
	 * Tests that the workspace baseline is null in headless mode
	 */
	@Test
	public void testGetWorkspaceProfile() {
		IApiBaseline baseline = fManager.getWorkspaceBaseline();
		if(ApiPlugin.isRunningInFramework()) {
			assertNotNull("the workspace baseline must not be null with the framework running", baseline); //$NON-NLS-1$
		}
		else {
			assertNull("the workspace baseline must be null in headless mode", baseline); //$NON-NLS-1$
		}
	}

	/**
	 * Tests that calling the stop method does not fail, and works
	 */
	@Test
	public void testStop() {
		fManager.stop();
		assertEquals("There should be no api baselines in the manager", 0, fManager.getApiBaselines().length); //$NON-NLS-1$
		// stop it again to free the memory from the map
		fManager.stop();
	}
}