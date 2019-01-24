/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal.model;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.core.OverflowingLRUCache;
import org.eclipse.jdt.internal.core.util.LRUCache;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
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
	static class Cache<K, V> extends OverflowingLRUCache<K, V> {

		/**
		 * Constructor
		 *
		 * @param size
		 * @param overflow
		 */
		public Cache(int size, int overflow) {
			super(size, overflow);
		}

		@Override
		protected boolean close(LRUCacheEntry<K, V> entry) {
			return true;
		}

		@Override
		protected LRUCache<K, V> newInstance(int size, int newOverflow) {
			return new Cache<>(size, newOverflow);
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

	static final int DEFAULT_CACHE_SIZE = 1500;
	static final int DEFAULT_OVERFLOW = (int) (DEFAULT_CACHE_SIZE * 0.1f);
	static ApiModelCache fInstance = null;

	Cache<String, Cache<String, Cache<String, IApiElement>>> fRootCache;
	Cache<String, Cache<String, ApiType>> fMemberTypeCache;

	/**
	 * Constructor - no instantiation
	 */
	private ApiModelCache() {
	}

	/**
	 * Returns the singleton instance of this cache
	 *
	 * @return the cache
	 */
	public static synchronized ApiModelCache getCache() {
		if (fInstance == null) {
			fInstance = new ApiModelCache();
		}
		return fInstance;
	}

	/**
	 * Returns the key to use in a cache. The key is of the form:
	 * <code>[baselineid].[componentid].[typename]</code><br>
	 *
	 * @param baseline
	 * @param component
	 * @param typename
	 * @return the member type cache key to use
	 */
	private String getCacheKey(String baseline, String component, String typename) {
		StringBuilder buffer = new StringBuilder();
		buffer.append(baseline).append('.').append(component).append('.').append(typename);
		return buffer.toString();
	}

	/**
	 * Caches the given {@link IApiElement} in the correct cache based on its
	 * type.
	 *
	 * @param element the element to cache
	 * @throws CoreException if there is a problem accessing any of the
	 *             {@link IApiElement} info in order to cache it - pass the
	 *             exception along.
	 */
	public void cacheElementInfo(IApiElement element) throws CoreException {
		switch (element.getType()) {
			case IApiElement.TYPE: {
				if (fRootCache == null) {
					fRootCache = new Cache<>(DEFAULT_CACHE_SIZE / 150, DEFAULT_OVERFLOW / 150);
				}
				IApiComponent comp = element.getApiComponent();
				if (comp != null) {
					IApiBaseline baseline = comp.getBaseline();
					String id = comp.getSymbolicName();
					if (id == null) {
						return;
					}
					Cache<String, Cache<String, IApiElement>> compcache = fRootCache.get(baseline.getName());
					if (compcache == null) {
						compcache = new Cache<>(DEFAULT_CACHE_SIZE / 2, DEFAULT_OVERFLOW / 2);
						fRootCache.put(baseline.getName(), compcache);
					}
					Cache<String, IApiElement> typecache = compcache.get(id);
					if (typecache == null) {
						typecache = new Cache<>(DEFAULT_CACHE_SIZE, DEFAULT_OVERFLOW);
						compcache.put(comp.getSymbolicName(), typecache);
					}
					ApiType type = (ApiType) element;
					if (type.isMemberType() || isMemberType(type.getName()) /*
																			 * cache
																			 * even
																			 * a
																			 * root
																			 * type
																			 * with
																			 * a
																			 * '$'
																			 * in
																			 * its
																			 * name
																			 * here
																			 * as
																			 * well
																			 */) {
						if (this.fMemberTypeCache == null) {
							this.fMemberTypeCache = new Cache<>(DEFAULT_CACHE_SIZE, DEFAULT_OVERFLOW);
						}
						String key = getCacheKey(baseline.getName(), id, getRootName(type.getName()));
						Cache<String, ApiType> mcache = this.fMemberTypeCache.get(key);
						if (mcache == null) {
							mcache = new Cache<>(DEFAULT_CACHE_SIZE, DEFAULT_OVERFLOW);
							this.fMemberTypeCache.put(key, mcache);
						}
						mcache.put(type.getName(), type);
					} else {
						typecache.put(element.getName(), element);
					}
				}
				break;
			}
			default:
				break;
		}
	}

	/**
	 * Returns the root type name assuming that the '$' char is a member type
	 * boundary
	 *
	 * @param typename
	 * @return the pruned name or the original name
	 */
	private String getRootName(String typename) {
		int idx = typename.indexOf('$');
		if (idx > -1) {
			return typename.substring(0, idx);
		}
		return typename;
	}

	/**
	 * Method to see if the type boundary char appears in the type name
	 *
	 * @param typename
	 * @return true if the type name contains '$' false otherwise
	 */
	private boolean isMemberType(String typename) {
		return typename.indexOf('$') > -1;
	}

	/**
	 * Returns the {@link IApiElement} infos for the element referenced by the
	 * given identifier and of the given type.
	 *
	 * @param baselineid the id of the baseline the component + element belongs
	 *            to
	 * @param componentid the id of the {@link IApiComponent} the element
	 *            resides in
	 * @param identifier for example the qualified name of the type or the id of
	 *            an API component
	 * @param type the kind of the element to look for info for
	 *
	 * @return the cached {@link IApiElement} or <code>null</code> if no such
	 *         element is cached
	 */
	public IApiElement getElementInfo(String baselineid, String componentid, String identifier, int type) {
		String updatedIdentifier = (identifier != null && identifier.startsWith("classes.java.")) ? identifier.substring(8) : identifier; //$NON-NLS-1$
		if (baselineid == null || componentid == null) {
			return null;
		}
		switch (type) {
			case IApiElement.TYPE: {
				if (isMemberType(updatedIdentifier)) {
					if (this.fMemberTypeCache != null) {
						Cache<String, ApiType> mcache = this.fMemberTypeCache.get(getCacheKey(baselineid, componentid, getRootName(updatedIdentifier)));
						if (mcache != null) {
							return mcache.get(updatedIdentifier);
						}
					}
				} else {
					if (this.fRootCache != null) {
						Cache<String, Cache<String, IApiElement>> compcache = fRootCache.get(baselineid);
						if (compcache != null) {
							Cache<String, IApiElement> typecache = compcache.get(componentid);
							if (typecache != null && updatedIdentifier != null) {
								return typecache.get(updatedIdentifier);
							}
						}
					}
				}
				break;
			}
			default:
				break;
		}
		return null;
	}

	/**
	 * Removes the {@link IApiElement} from the given component (given its id)
	 * with the given identifier and of the given type.
	 *
	 * @param componentid the id of the component the element resides in
	 * @param identifier the id (name) of the element to remove
	 * @param type the type of the element (TYPE, METHOD, FIELD, etc)
	 *
	 * @return true if the element was removed, false otherwise
	 */
	public boolean removeElementInfo(String baselineid, String componentid, String identifier, int type) {
		if (baselineid == null) {
			return false;
		}
		switch (type) {
			case IApiElement.TYPE: {
				if (componentid != null && identifier != null) {
					boolean removed = true;
					// clean member type cache
					if (this.fMemberTypeCache != null) {
						if (isMemberType(identifier)) {
							Cache<String, ApiType> mcache = this.fMemberTypeCache.get(getCacheKey(baselineid, componentid, getRootName(identifier)));
							if (mcache != null) {
								return mcache.remove(identifier) != null;
							}
						} else {
							this.fMemberTypeCache.remove(getCacheKey(baselineid, componentid, getRootName(identifier)));
						}
					}
					if (fRootCache != null) {
						Cache<String, Cache<String, IApiElement>> compcache = fRootCache.get(baselineid);
						if (compcache != null) {
							Cache<String, IApiElement> typecache = compcache.get(componentid);
							if (typecache != null) {
								removed &= typecache.remove(identifier) != null;
								if (typecache.isEmpty()) {
									removed &= compcache.remove(componentid) != null;
								}
								if (compcache.isEmpty()) {
									removed &= fRootCache.remove(baselineid) != null;
								}
								return removed;
							}

						}
					} else {
						return false;
					}
				}
				break;
			}
			case IApiElement.COMPONENT: {
				flushMemberCache();
				if (fRootCache != null && componentid != null) {
					Cache<String, Cache<String, IApiElement>> compcache = fRootCache.get(baselineid);
					if (compcache != null) {
						boolean removed = compcache.remove(componentid) != null;
						if (compcache.isEmpty()) {
							removed &= fRootCache.remove(baselineid) != null;
						}
						return removed;
					}
				}
				break;
			}
			case IApiElement.BASELINE: {
				flushMemberCache();
				if (fRootCache != null) {
					return fRootCache.remove(baselineid) != null;
				}
				break;
			}
			default:
				break;
		}
		return false;
	}

	/**
	 * Removes the given {@link IApiElement} info from the cache and returns it
	 * if present
	 *
	 * @param element
	 * @return true if the {@link IApiElement} was removed false otherwise
	 * @throws CoreException if there is a problem accessing any of the
	 *             {@link IApiElement} info in order to remove it from the cache
	 *             - pass the exception along.
	 */
	public boolean removeElementInfo(IApiElement element) {
		if (element == null) {
			return false;
		}
		switch (element.getType()) {
			case IApiElement.COMPONENT:
			case IApiElement.TYPE: {
				if (fRootCache != null) {
					IApiComponent comp = element.getApiComponent();
					if (comp != null) {
						try {
							IApiBaseline baseline = comp.getBaseline();
							return removeElementInfo(baseline.getName(), comp.getSymbolicName(), element.getName(), element.getType());
						} catch (CoreException ce) {
							ApiPlugin.log("Failed to remove element info for " + comp.getName(), ce); //$NON-NLS-1$
						}
					}
				}
				break;
			}
			case IApiElement.BASELINE: {
				flushMemberCache();
				if (fRootCache != null) {
					IApiBaseline baseline = (IApiBaseline) element;
					return fRootCache.remove(baseline.getName()) != null;
				}
				break;
			}
			default:
				break;
		}
		return false;
	}

	/**
	 * Clears out all cached information.
	 */
	public void flushCaches() {
		if (fRootCache != null) {
			fRootCache.flush();
		}
		flushMemberCache();
	}

	/**
	 * Flushes the cache of member types
	 */
	private void flushMemberCache() {
		if (this.fMemberTypeCache != null) {
			this.fMemberTypeCache.flush();
		}
	}

	/**
	 * Returns if the cache has any elements in it or not
	 *
	 * @return true if the cache has no entries, false otherwise
	 */
	public boolean isEmpty() {
		boolean empty = true;
		if (fRootCache != null) {
			empty &= fRootCache.isEmpty();
		}
		if (this.fMemberTypeCache != null) {
			empty &= this.fMemberTypeCache.isEmpty();
		}
		return empty;
	}
}
