/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lars Vogel - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.tests.internal;

import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.ui.tests.util.ConcurrencyUtil;
import org.junit.Test;

/**
 * Verifies that scheduling the model initialization never waits for the lock
 * that is held while the target platform is resolved.
 */
public class ModelManagerBackgroundInitializationTest {

	private static final long TEST_TIMEOUT_MS = 60_000;

	/**
	 * init() holds the instance monitor while it reads the external features.
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testFeatureManagerSchedulesWithoutTakingTheInitMonitor() throws Exception {
		FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
		ConcurrencyUtil.assertReturnsWhileLockIsHeld("initializeInBackground waited for the lock to be released", //$NON-NLS-1$
				manager, () -> manager.initializeInBackground(() -> {
				}));
		// the scheduled job resolves the target platform, finish it here so it
		// does not race the target of whatever test comes next
		manager.getModels();
	}

	/**
	 * initializeTable() holds fEntriesSynchronizer while it resolves the target.
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testPluginManagerSchedulesWithoutTakingTheEntriesLock() throws Exception {
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		Object entriesLock = ConcurrencyUtil.readLock(manager, "fEntriesSynchronizer"); //$NON-NLS-1$
		ConcurrencyUtil.assertReturnsWhileLockIsHeld("initializeInBackground waited for the lock to be released", //$NON-NLS-1$
				entriesLock, () -> manager.initializeInBackground(() -> {
				}));
		// the scheduled job resolves the target platform, finish it here so it
		// does not race the target of whatever test comes next
		manager.getAllModels();
	}
}
