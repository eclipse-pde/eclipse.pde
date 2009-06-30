/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.model;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.core.OverflowingLRUCache;
import org.eclipse.jdt.internal.core.util.LRUCache;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;

/**
 * Manages the caches of {@link IApiElement}s
 * 
 * @since 1.0.2
 */
public final class ApiModelCache {

	/**
	 * Cache used for {@link IApiElement}s
	 */
	class Cache extends OverflowingLRUCache {

		/**
		 * Constructor
		 * @param size
		 * @param overflow
		 */
		public Cache(int size, int overflow) {
			super(size, overflow);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.core.OverflowingLRUCache#close(org.eclipse.jdt.internal.core.util.LRUCache.LRUCacheEntry)
		 */
		protected boolean close(LRUCacheEntry entry) {
			return true;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.core.OverflowingLRUCache#newInstance(int, int)
		 */
		protected LRUCache newInstance(int size, int newOverflow) {
			return new Cache(size, newOverflow);
		}
		
		/**
		 * Returns if the cache has any elements in it or not
		 * 
		 * @return true if the cache has no entries, false otherwise
		 */
		public boolean isEmpty() {
			return !keys().hasMoreElements();
		}
	}
	
	static final int DEFAULT_CACHE_SIZE = 100;
	static final int DEFAULT_OVERFLOW = (int)(DEFAULT_CACHE_SIZE * 0.1f);
	static ApiModelCache fInstance = null;
	
	Cache fRootCache = null;
	
	/**
	 * Constructor - no instantiation
	 */
	private ApiModelCache() {}
	
	/**
	 * Returns the singleton instance of this cache
	 * 
	 * @return the cache
	 */
	public static synchronized ApiModelCache getCache() {
		if(fInstance == null) {
			fInstance = new ApiModelCache();
		}
		return fInstance;
	}
	
	/**
	 * Caches the given {@link IApiElement} in the correct cache based on its type.
	 * 
	 * @param element the element to cache
	 * @throws CoreException if there is a problem accessing any of the {@link IApiElement} info
	 * in order to cache it - pass the exception along.
	 */
	public void cacheElementInfo(IApiElement element) throws CoreException {
		switch(element.getType()) {
			case IApiElement.TYPE: {
				if(fRootCache == null) {
					fRootCache = new Cache(DEFAULT_CACHE_SIZE, DEFAULT_OVERFLOW);
				}
				IApiComponent comp = element.getApiComponent();
				if(comp != null) {
					IApiBaseline baseline = comp.getBaseline();
					String id = comp.getId();
					if(id == null) {
						return;
					}
					Cache compcache = (Cache) fRootCache.get(baseline.getName());
					if(compcache == null) {
						compcache = new Cache(DEFAULT_CACHE_SIZE, DEFAULT_OVERFLOW);
						fRootCache.put(baseline.getName(), compcache);
					}
					Cache typecache = (Cache) compcache.get(id);
					if(typecache == null) {
						typecache = new Cache(DEFAULT_CACHE_SIZE, DEFAULT_OVERFLOW);
						compcache.put(comp.getId(), typecache);
					}
					typecache.put(element.getName(), element);
				}
				break;
			}
		}
	}
	
	/**
	 * Returns the {@link IApiElement} infos for the element referenced by the given 
	 * identifier and of the given type.
	 * 
	 * @param baselineid the id of the baseline the component + element belongs to
	 * @param componentid the id of the {@link IApiComponent} the element resides in
	 * @param identifier for example the qualified name of the type or the id of an API component
	 * @param type the kind of the element to look for info for
	 * 
	 * @return the cached {@link IApiElement} or <code>null</code> if no such element is cached
	 */
	public IApiElement getElementInfo(String baselineid, String componentid, String identifier, int type) {
		if(baselineid == null || componentid == null) {
			return null;
		}
		switch(type) {
			case IApiElement.TYPE: {
				if(fRootCache != null) {
					Cache compcache = (Cache) fRootCache.get(baselineid);
					if(compcache != null) {
						Cache typecache = (Cache) compcache.get(componentid);
						if(typecache != null && identifier != null) {
							return (IApiElement) typecache.get(identifier);
						}
					}
				}
				break;
			}
		}
		return null;
	}
	
	/**
	 * Removes the {@link IApiElement} from the given component (given its id) with
	 * the given identifier and of the given type.
	 * 
	 * @param componentid the id of the component the element resides in
	 * @param identifier the id (name) of the element to remove
	 * @param type the type of the element (TYPE, METHOD, FIELD, etc)
	 * 
	 * @return true if the element was removed, false otherwise
	 */
	public boolean removeElementInfo(String baselineid, String componentid, String identifier, int type) {
		if(baselineid == null) {
			return false;
		}
		switch(type) {
			case IApiElement.TYPE: {
				if(fRootCache != null && componentid != null && identifier != null) {
					Cache compcache = (Cache) fRootCache.get(baselineid);
					boolean removed = true;
					if(compcache != null) {
						Cache typecache = (Cache) compcache.get(componentid);
						if(typecache != null) {
							removed &= typecache.remove(identifier) != null;
							if(typecache.isEmpty()) {
								removed &= compcache.remove(componentid) != null;
							}
							if(compcache.isEmpty()) {
								removed &= fRootCache.remove(baselineid) != null;
							}
						}
						return removed;
					}
				}
				break;
			}
			case IApiElement.COMPONENT: {
				if(fRootCache != null && componentid != null) {
					Cache compcache = (Cache) fRootCache.get(baselineid);
					if(compcache != null) {
						boolean removed = compcache.remove(componentid) != null;
						if(compcache.isEmpty()) {
							removed &= fRootCache.remove(baselineid) != null;
						}
						return removed;
					}
				}
				break;
			}
			case IApiElement.BASELINE: {
				if(fRootCache != null) {
					return fRootCache.remove(baselineid) != null;
				}
				break;
			}
		}
		return false;
	}
	
	/**
	 * Removes the given {@link IApiElement} info from the cache and returns it if present
	 * @param element
	 * @return true if the {@link IApiElement} was removed false otherwise
	 * @throws CoreException if there is a problem accessing any of the {@link IApiElement} info
	 * in order to remove it from the cache - pass the exception along.
	 */
	public boolean removeElementInfo(IApiElement element) throws CoreException {
		if(element == null) {
			return false;
		}
 		switch(element.getType()) {
			case IApiElement.COMPONENT:
			case IApiElement.TYPE: {
				if(fRootCache != null) {
					IApiComponent comp = element.getApiComponent();
					if(comp != null) {
						IApiBaseline baseline = comp.getBaseline();
						return removeElementInfo(baseline.getName(), comp.getId(), element.getName(), element.getType());
					}
				}
				break;
			}
			case IApiElement.BASELINE: {
				if(fRootCache != null) {
					IApiBaseline baseline = (IApiBaseline) element;
					return fRootCache.remove(baseline.getName()) != null;
				}
				break;
			}
		}
		return false;
	}
	
	/**
	 * Clears out all cached information.
	 */
	public void flushCaches() {
		if(fRootCache != null) {
			fRootCache.flush();
		}
	}
	
	/**
	 * Returns if the cache has any elements in it or not
	 * 
	 * @return true if the cache has no entries, false otherwise
	 */
	public boolean isEmpty() {
		if(fRootCache != null) {
			return fRootCache.isEmpty();
		}
		return true;
	}
}
