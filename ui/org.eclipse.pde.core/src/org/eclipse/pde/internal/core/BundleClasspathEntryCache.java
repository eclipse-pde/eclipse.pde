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
package org.eclipse.pde.internal.core;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import org.eclipse.jdt.core.IClasspathEntry;

/**
 * Caches the classpath entries of bundles by their symbolic name. Loaders run
 * without a lock of this cache held, and an invalidation during a load wins:
 * the loaded entries are not kept.
 */
public final class BundleClasspathEntryCache {

	private final ConcurrentMap<String, List<IClasspathEntry>> entries = new ConcurrentHashMap<>();

	/** Incremented by every invalidation, before the entries are removed. */
	private final AtomicLong generation = new AtomicLong();

	/**
	 * Returns the cached entries of the given bundle, or loads them with the given
	 * loader if they are not cached.
	 *
	 * @param bundleId
	 *            the symbolic name of the bundle
	 * @param loader
	 *            computes the entries; called without a lock of this cache held,
	 *            possibly by several threads at the same time
	 * @return the entries of the bundle
	 */
	public List<IClasspathEntry> get(String bundleId, Function<String, List<IClasspathEntry>> loader) {
		List<IClasspathEntry> cached = entries.get(bundleId);
		if (cached != null) {
			return cached;
		}
		long start = generation.get();
		List<IClasspathEntry> loaded = loader.apply(bundleId);
		// check and publish atomically; an invalidation during the load wins
		List<IClasspathEntry> result = entries.compute(bundleId,
				(id, current) -> current != null || generation.get() != start ? current : loaded);
		return result != null ? result : loaded;
	}

	/**
	 * Removes the cached entries of the given bundle.
	 *
	 * @param bundleId
	 *            the symbolic name of the bundle
	 */
	public void invalidate(String bundleId) {
		// increment before removing, so that a running get() does not publish
		generation.incrementAndGet();
		entries.remove(bundleId);
	}

	/**
	 * Removes all cached entries.
	 */
	public void invalidateAll() {
		// increment before removing, so that a running get() does not publish
		generation.incrementAndGet();
		entries.clear();
	}
}
