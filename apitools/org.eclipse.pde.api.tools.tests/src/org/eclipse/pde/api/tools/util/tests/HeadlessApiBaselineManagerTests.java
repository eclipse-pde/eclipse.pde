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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.ApiBaselineManager;
import org.eclipse.pde.api.tools.internal.model.ApiModelFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.tests.AbstractApiTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that the {@link ApiBaselineManager} is usable in a predictable way in a headless
 * environment
 */
public class HeadlessApiBaselineManagerTests extends AbstractApiTest {

	private final ApiBaselineManager fManager = ApiBaselineManager.getManager();

	@Override
	@AfterEach
	public void tearDown() throws Exception {
		fManager.stop();
		super.tearDown();
	}

	/**
	 * Tests that we can get an API baseline that exists from the manager
	 */
	@Test
	public void testGetApiProfile() {
		IApiBaseline baseline = ApiModelFactory.newApiBaseline("test1"); //$NON-NLS-1$
		fManager.addApiBaseline(baseline);
		baseline = fManager.getApiBaseline("test1"); //$NON-NLS-1$
		assertNotNull(baseline, "the test1 baseline must exist in the manager"); //$NON-NLS-1$
		assertTrue(baseline.getName().equals("test1"), "the found baseline must be test1"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests that looking up a baseline that does not exist in the manager returns null
	 */
	@Test
	public void testGetNonExistantProfile() {
		IApiBaseline baseline = fManager.getApiBaseline("foobaseline"); //$NON-NLS-1$
		assertNull(baseline, "There should be no baseline found"); //$NON-NLS-1$
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
		assertNotNull(baseline, "the default baseline should not be null"); //$NON-NLS-1$
		assertTrue(baseline.getName().equals("test2"), "the default baselines' name should be test2"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests that setting the default baseline to one that does not exist in the manager will return null
	 * when asked for the default.
	 */
	@Test
	public void testGetWrongDefault() {
		fManager.setDefaultApiBaseline("foobaseline"); //$NON-NLS-1$
		IApiBaseline baseline = fManager.getDefaultApiBaseline();
		assertNull(baseline, "the default baseline should be null for a non-existant id"); //$NON-NLS-1$
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
		assertEquals(2, baselines.length, "there should be 2 baselines"); //$NON-NLS-1$
	}

	/**
	 * Tests removing an existing baseline from the manager
	 */
	@Test
	public void testRemoveApiProfile() {
		IApiBaseline baseline = ApiModelFactory.newApiBaseline("test2"); //$NON-NLS-1$
		fManager.addApiBaseline(baseline);
		boolean result = fManager.removeApiBaseline("test2"); //$NON-NLS-1$
		assertTrue(result, "the baseline test2 should have been removed from the manager"); //$NON-NLS-1$
		assertEquals(0, fManager.getApiBaselines().length, "There should only be 0 baselines left"); //$NON-NLS-1$
	}

	/**
	 * Tests that isExistingProfileName(..) returns return true when expected to
	 */
	@Test
	public void testIsExistingName() {
		IApiBaseline baseline = ApiModelFactory.newApiBaseline("test1"); //$NON-NLS-1$
		fManager.addApiBaseline(baseline);
		boolean result = fManager.isExistingProfileName("test1"); //$NON-NLS-1$
		assertTrue(result, "the name test1 should be an existing name"); //$NON-NLS-1$
	}

	/**
	 * Tests that isExistingProfileName returns false when asked about an non-existent name
	 */
	@Test
	public void testisExistingName2() {
		boolean result = fManager.isExistingProfileName("foobaseline"); //$NON-NLS-1$
		assertFalse(result, "foobaseline is not an existing name"); //$NON-NLS-1$
	}

	/**
	 * Tests that calling the saving(..) method on the manager in headless mode
	 * does not fail
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
			assertNotNull(baseline, "the workspace baseline must not be null with the framework running"); //$NON-NLS-1$
		}
		else {
			assertNull(baseline, "the workspace baseline must be null in headless mode"); //$NON-NLS-1$
		}
	}

	/**
	 * Tests that calling the stop method does not fail, and works
	 */
	@Test
	public void testStop() {
		fManager.stop();
		assertEquals(0, fManager.getApiBaselines().length, "There should be no api baselines in the manager"); //$NON-NLS-1$
		// stop it again to free the memory from the map
		fManager.stop();
	}
}