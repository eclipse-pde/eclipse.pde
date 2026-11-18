/*******************************************************************************
 * Copyright (c) 2026 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.tests.internal.classpath;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.internal.core.BundleClasspathEntryCache;
import org.junit.jupiter.api.Test;

/**
 * Tests the concurrency contract of {@link BundleClasspathEntryCache}, see
 * https://github.com/eclipse-pde/eclipse.pde/issues/1715
 */
public class BundleClasspathEntryCacheTest {

	private static final long TIMEOUT_MS = 10_000;
	private static final String BUNDLE = "bundle";

	private final BundleClasspathEntryCache cache = new BundleClasspathEntryCache();

	@Test
	public void cachedValueIsReused() {
		AtomicInteger loads = new AtomicInteger();
		List<IClasspathEntry> first = cache.get(BUNDLE, id -> {
			loads.incrementAndGet();
			return entries();
		});
		List<IClasspathEntry> second = cache.get(BUNDLE, id -> {
			loads.incrementAndGet();
			return entries();
		});
		assertSame(first, second);
		assertEquals(1, loads.get());
	}

	@Test
	public void invalidateAllDoesNotWaitForRunningLoader() throws Exception {
		assertInvalidationDoesNotWaitForRunningLoader(BundleClasspathEntryCache::invalidateAll);
	}

	@Test
	public void invalidateDoesNotWaitForRunningLoader() throws Exception {
		assertInvalidationDoesNotWaitForRunningLoader(c -> c.invalidate(BUNDLE));
	}

	@Test
	public void invalidateAllDuringLoadDropsResult() throws Exception {
		assertInvalidationDuringLoadDropsResult(BundleClasspathEntryCache::invalidateAll);
	}

	@Test
	public void invalidateDuringLoadDropsResult() throws Exception {
		assertInvalidationDuringLoadDropsResult(c -> c.invalidate(BUNDLE));
	}

	/**
	 * The cycle of #1715: the loader waits for a lock (the model manager's lock)
	 * that is held by a thread that invalidates the cache.
	 */
	private void assertInvalidationDoesNotWaitForRunningLoader(Consumer<BundleClasspathEntryCache> invalidation)
			throws InterruptedException {
		Object lock = new Object();
		CountDownLatch lockHeld = new CountDownLatch(1);
		CountDownLatch loaderEntered = new CountDownLatch(1);
		AtomicReference<Throwable> failure = new AtomicReference<>();

		Thread loading = newThread("loading", failure, () -> cache.get(BUNDLE, id -> {
			loaderEntered.countDown();
			synchronized (lock) {
				return entries();
			}
		}));
		Thread invalidating = newThread("invalidating", failure, () -> {
			synchronized (lock) {
				lockHeld.countDown();
				await(loaderEntered);
				awaitBlocked(loading);
				invalidation.accept(cache);
			}
		});

		invalidating.start();
		await(lockHeld);
		loading.start();

		invalidating.join(TIMEOUT_MS);
		assertFalse(invalidating.isAlive(), "Invalidation waits for the running loader (deadlock)");
		loading.join(TIMEOUT_MS);
		assertFalse(loading.isAlive(), "Loader did not finish");
		assertNoFailure(failure);
	}

	/**
	 * An invalidation while a loader runs must win: the loaded value must not stay
	 * in the cache.
	 */
	private void assertInvalidationDuringLoadDropsResult(Consumer<BundleClasspathEntryCache> invalidation)
			throws InterruptedException {
		CountDownLatch loaderEntered = new CountDownLatch(1);
		CountDownLatch releaseLoader = new CountDownLatch(1);
		AtomicInteger loads = new AtomicInteger();
		AtomicReference<Throwable> failure = new AtomicReference<>();
		AtomicReference<List<IClasspathEntry>> loaded = new AtomicReference<>();

		Thread loading = newThread("loading", failure, () -> loaded.set(cache.get(BUNDLE, id -> {
			loads.incrementAndGet();
			loaderEntered.countDown();
			// wait longer than the test waits for the invalidation
			await(releaseLoader, 3 * TIMEOUT_MS);
			return entries();
		})));
		Thread invalidating = newThread("invalidating", failure, () -> invalidation.accept(cache));

		loading.start();
		await(loaderEntered);
		invalidating.start();
		invalidating.join(TIMEOUT_MS);
		boolean invalidationFinished = !invalidating.isAlive();
		releaseLoader.countDown();
		assertTrue(invalidationFinished, "Invalidation waits for the running loader");
		loading.join(TIMEOUT_MS);
		assertFalse(loading.isAlive(), "Loader did not finish");
		assertNoFailure(failure);
		assertEquals(1, loaded.get().size(), "The caller of the invalidated load gets its result");

		cache.get(BUNDLE, id -> {
			loads.incrementAndGet();
			return entries();
		});
		assertEquals(2, loads.get(), "A result invalidated during its load was kept in the cache");
	}

	private static List<IClasspathEntry> entries() {
		return List.of(JavaCore.newLibraryEntry(new Path("/" + BUNDLE + ".jar"), null, null));
	}

	private static Thread newThread(String name, AtomicReference<Throwable> failure, Runnable runnable) {
		Thread thread = new Thread(() -> {
			try {
				runnable.run();
			} catch (Throwable t) {
				failure.compareAndSet(null, t);
			}
		}, BundleClasspathEntryCacheTest.class.getSimpleName() + '-' + name);
		// a deadlocked thread must not keep the test VM alive
		thread.setDaemon(true);
		return thread;
	}

	private static void await(CountDownLatch latch) {
		await(latch, TIMEOUT_MS);
	}

	private static void await(CountDownLatch latch, long timeoutMs) {
		try {
			if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
				throw new AssertionError("Timeout while waiting for a latch");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError(e);
		}
	}

	private static void awaitBlocked(Thread thread) {
		long deadline = System.currentTimeMillis() + TIMEOUT_MS;
		while (thread.getState() != Thread.State.BLOCKED) {
			if (System.currentTimeMillis() > deadline) {
				throw new AssertionError("Thread did not block: " + thread.getState());
			}
			LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
		}
	}

	private static void assertNoFailure(AtomicReference<Throwable> failure) {
		Throwable t = failure.get();
		if (t != null) {
			fail(t);
		}
	}
}
