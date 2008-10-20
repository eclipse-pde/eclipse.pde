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
package org.eclipse.pde.api.tools.internal.model.cache;

/**
 * A cache of loaded {@link IApiElement} infos
 * 
 * @since 1.0.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class ApiElementCache extends OverflowingLRUCache {

	/**
	 * Constructor
	 * @param size
	 */
	public ApiElementCache(int size) {
		super(size);
	}

	/**
	 * Constructor
	 * @param size
	 * @param overflow
	 */
	public ApiElementCache(int size, int overflow) {
		super(size, overflow);
	}
	
	/**
	 * @see org.eclipse.pde.api.tools.internal.model.OverflowingLRUCache#close(org.eclipse.jdt.internal.core.util.LRUCache.LRUCacheEntry)
	 */
	protected boolean close(LRUCacheEntry entry) {
		return true;
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.model.OverflowingLRUCache#newInstance(int, int)
	 */
	protected LRUCache newInstance(int size, int newOverflow) {
		return new ApiElementCache(size, newOverflow);
	}

}
