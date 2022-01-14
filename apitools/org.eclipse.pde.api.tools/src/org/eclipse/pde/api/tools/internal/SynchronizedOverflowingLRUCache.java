/*******************************************************************************
 * Copyright (c) 2022 Andrey Loskutov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov (loskutov@gmx.de) - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.eclipse.jdt.internal.core.OverflowingLRUCache;

/**
 * The partly synchronized variant of {@link OverflowingLRUCache}. Only
 * public/protected methods overridden in this class are synchronized.
 */
public abstract class SynchronizedOverflowingLRUCache<K, V> extends OverflowingLRUCache<K, V> {

	public SynchronizedOverflowingLRUCache(int size) {
		super(size);
	}

	public SynchronizedOverflowingLRUCache(int size, int overflow) {
		super(size, overflow);
	}

	/**
	 * Returns if the cache has any elements in it or not
	 *
	 * @return true if the cache has no entries, false otherwise
	 */
	public synchronized boolean isEmpty() {
		return !super.keys().hasMoreElements();
	}

	@Override
	public synchronized V put(K key, V value) {
		return super.put(key, value);
	}

	@Override
	public synchronized V get(K key) {
		return super.get(key);
	}

	@Override
	public synchronized void flush() {
		super.flush();
	}

	@Override
	public synchronized V remove(K key) {
		return super.remove(key);
	}

	/**
	 * @deprecated Should not be used, because iteration over keys is not guaranteed
	 *             to return valid results if the cache is structurally modified
	 *             while enumerating. Use {@link #keysSnapshot()} instead.
	 */
	@Deprecated
	@Override
	public synchronized Enumeration<K> keys() {
		return super.keys();
	}

	/**
	 * @return MT-safe snapshot of the keys in the cache.
	 */
	public synchronized List<K> keysSnapshot() {
		Enumeration<K> keys = super.keys();
		return Collections.list(keys);
	}

	/**
	 * @deprecated Should not be used, because iteration over elements is not
	 *             guaranteed to return valid results if the cache is structurally
	 *             modified while enumerating. Use {@link #elementsSnapshot()}
	 *             instead.
	 */
	@Deprecated
	@Override
	public synchronized Enumeration<V> elements() {
		return super.elements();
	}

	/**
	 * @return MT-safe snapshot of the elements in the cache.
	 */
	public synchronized List<V> elementsSnapshot() {
		return Collections.list(super.elements());
	}

	@Override
	public synchronized void setSpaceLimit(int limit) {
		super.setSpaceLimit(limit);
	}

}
