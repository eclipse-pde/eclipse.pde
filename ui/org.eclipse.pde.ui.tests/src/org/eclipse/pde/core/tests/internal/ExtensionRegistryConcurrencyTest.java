/*******************************************************************************
 *  Copyright (c) 2026 Vector Informatik GmbH and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.pde.core.tests.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.IExtension;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDEExtensionRegistry;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.ui.tests.util.ConcurrencyUtil;
import org.junit.Before;
import org.junit.Test;

/**
 * Verifies that the PDE extension registry cannot deadlock with the model
 * change listeners of the {@link PluginModelManager}. Those listeners hold the
 * lock of the PluginModelManager while they ask for the registry, which takes
 * the registry's monitor. So the registry must never wait for the models, which
 * takes that lock, while holding its monitor.
 */
public class ExtensionRegistryConcurrencyTest {

	private static final long TEST_TIMEOUT_MS = 60_000;
	private static final long WAIT_TIMEOUT_MS = 15_000;
	private static final String EXTENSION_POINT = PDECore.PLUGIN_ID + ".source"; //$NON-NLS-1$

	private PluginModelManager manager;
	private PDEExtensionRegistry registry;

	@Before
	public void setUp() {
		manager = PDECore.getDefault().getModelManager();
		// initialize the models up front, so that collecting them only waits for
		// the lock instead of resolving the target platform
		manager.getAllModels();
		registry = PDECore.getDefault().getExtensionsRegistry();
	}

	@Test(timeout = TEST_TIMEOUT_MS)
	public void testCreatingRegistryDoesNotHoldMonitorWhileWaitingForModels() throws Exception {
		// discard the registry, so that the next access creates it again
		registry.targetReloaded();
		AtomicReference<IExtension[]> found = new AtomicReference<>();
		Thread accessor = newAccessor(found);
		ConcurrencyUtil.assertReturnsWhileLockIsHeld("the registry's monitor was held while waiting for the models", //$NON-NLS-1$
				entriesLock(), () -> {
					accessor.start();
					waitUntilBlockedIn(accessor, PluginModelManager.class);
					// what a model change listener asking for the registry needs
					synchronized (registry) {
					}
				});
		assertFinished(accessor);
		assertNotNull(found.get());
	}

	/**
	 * Reloading the target stops the registry while holding the registry's
	 * monitor, so it must not wait for the models, which takes the lock of the
	 * PluginModelManager.
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testReloadingTargetDoesNotWaitForModels() throws Exception {
		// make sure there is a registry to stop
		registry.findExtensions(EXTENSION_POINT, false);
		ConcurrencyUtil.assertReturnsWhileLockIsHeld("reloading the target waited for the models", entriesLock(), //$NON-NLS-1$
				registry::targetReloaded);
	}

	/**
	 * A target reload after a creation collected its models, but before it built
	 * the registry from them, makes the creation collect the models again, so the
	 * registry is built from the reloaded models.
	 */
	@Test(timeout = TEST_TIMEOUT_MS)
	public void testCreationStartsOverAfterTargetReload() throws Exception {
		registry.targetReloaded();
		Object entriesLock = entriesLock();
		AtomicReference<IExtension[]> found = new AtomicReference<>();
		Thread accessor = newAccessor(found);
		CountDownLatch acquired = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		AtomicBoolean stillHeld = new AtomicBoolean();
		Thread holder = new Thread(() -> {
			synchronized (entriesLock) {
				stillHeld.set(true);
				acquired.countDown();
				try {
					release.await(WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				stillHeld.set(false);
			}
		}, "lock holder"); //$NON-NLS-1$
		holder.setDaemon(true);
		try {
			synchronized (registry) {
				// let the creation collect the models and wait for the monitor
				accessor.start();
				waitUntilBlockedIn(accessor, PDEExtensionRegistry.class);
				registry.targetReloaded();
				// hold the model lock, so that a creation that starts over has to wait for it
				holder.start();
				assertTrue("the holder thread never acquired the lock", //$NON-NLS-1$
						acquired.await(WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
			}
			waitUntilBlockedIn(accessor, PluginModelManager.class);
			synchronized (registry) {
				assertTrue("the creation held the registry's monitor while collecting the models again", //$NON-NLS-1$
						stillHeld.get());
			}
		} finally {
			release.countDown();
			holder.join(WAIT_TIMEOUT_MS);
		}
		assertFinished(accessor);
		assertNotNull(found.get());
	}

	private Object entriesLock() throws ReflectiveOperationException {
		return ConcurrencyUtil.readLock(manager, "fEntriesSynchronizer"); //$NON-NLS-1$
	}

	/**
	 * Creates, but does not start, a thread that looks up extensions and thereby
	 * creates the registry if there is none.
	 */
	private Thread newAccessor(AtomicReference<IExtension[]> found) {
		Thread accessor = new Thread(() -> found.set(registry.findExtensions(EXTENSION_POINT, false)),
				"registry accessor"); //$NON-NLS-1$
		accessor.setDaemon(true);
		return accessor;
	}

	private static void assertFinished(Thread thread) throws InterruptedException {
		thread.join(WAIT_TIMEOUT_MS);
		assertFalse(thread.getName() + " did not finish", thread.isAlive()); //$NON-NLS-1$
	}

	/**
	 * Waits until the given thread waits for a monitor in a method of the given
	 * type.
	 */
	private static void waitUntilBlockedIn(Thread thread, Class<?> type) {
		long deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MS;
		while (!isBlockedIn(thread, type)) {
			if (!thread.isAlive()) {
				fail(thread.getName() + " finished without waiting for a lock in " + type.getSimpleName()); //$NON-NLS-1$
			}
			if (System.currentTimeMillis() > deadline) {
				fail(thread.getName() + " never waited for a lock in " + type.getSimpleName()); //$NON-NLS-1$
			}
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				fail("interrupted"); //$NON-NLS-1$
			}
		}
	}

	private static boolean isBlockedIn(Thread thread, Class<?> type) {
		if (thread.getState() != Thread.State.BLOCKED) {
			return false;
		}
		// the top frame is the method that waits for the monitor
		StackTraceElement[] stack = thread.getStackTrace();
		return stack.length > 0 && stack[0].getClassName().equals(type.getName());
	}
}
