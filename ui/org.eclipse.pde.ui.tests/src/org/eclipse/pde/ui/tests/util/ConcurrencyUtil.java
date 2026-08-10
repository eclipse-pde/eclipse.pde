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
package org.eclipse.pde.ui.tests.util;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Helpers for asserting that a call does not wait for a lock that is held while
 * the target platform is resolved.
 */
public class ConcurrencyUtil {

	/**
	 * Releases the held lock even if the call under test blocks, so a regression
	 * fails the test instead of wedging the rest of the suite.
	 */
	private static final long RELEASE_TIMEOUT_MS = 15_000;

	private ConcurrencyUtil() {
	}

	/**
	 * Runs the given call while another thread holds the given lock and asserts
	 * that it returned before that thread released it.
	 */
	public static void assertReturnsWhileLockIsHeld(String message, Object lock, Runnable call)
			throws InterruptedException {
		CountDownLatch acquired = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		AtomicBoolean stillHeld = new AtomicBoolean();
		Thread holder = new Thread(() -> {
			synchronized (lock) {
				stillHeld.set(true);
				acquired.countDown();
				try {
					release.await(RELEASE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				stillHeld.set(false);
			}
		}, "lock holder"); //$NON-NLS-1$
		holder.setDaemon(true);
		holder.start();
		try {
			assertTrue("the holder thread never acquired the lock", //$NON-NLS-1$
					acquired.await(RELEASE_TIMEOUT_MS, TimeUnit.MILLISECONDS));
			call.run();
			assertTrue(message, stillHeld.get());
		} finally {
			release.countDown();
			holder.join();
		}
	}

	/**
	 * Returns the value of the named field, to reach a lock that is not visible
	 * to the test.
	 */
	public static Object readLock(Object owner, String name) throws ReflectiveOperationException {
		Field field = owner.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(owner);
	}
}
