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

import java.util.HashMap;

import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;

/**
 * Cache of {@link IApiElement} infos, API descriptions and {@link IApiProfile}s
 * 
 * @since 1.0.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class ApiToolsCache {
	
	private static final int DEFAULT_CACHE_SIZE = 20;
	private static final int DEFAULT_MEMBER_CACHE_SIZE = 100;
	
	/**
	 * The cache of {@link IApiDescription}s
	 */
	private ApiElementCache fDescriptionCache = new ApiElementCache(DEFAULT_CACHE_SIZE);
	
	/**
	 * The cache of {@link IApiComponent} infos
	 */
	private ApiElementCache fComponentCache = new ApiElementCache(DEFAULT_CACHE_SIZE);
	
	/**
	 * The cache of {@link IApiMember} infos
	 */
	private HashMap fMemberCache = new HashMap(DEFAULT_MEMBER_CACHE_SIZE);

	/**
	 * Returns the cached {@link IApiDescription} for the given component or <code>null</code>
	 * if there is no {@link IApiDescription} cached for the given component.
	 * 
	 * @param component
	 * @return the cached {@link IApiDescription} for the given component or <code>null</code> if none.
	 */
	public IApiDescription getApiDescription(IApiComponent component) {
		return (IApiDescription) fDescriptionCache.get(component);
	}
	
	/**
	 * Adds a new cached {@link IApiDescription} for the given component
	 * @param component
	 * @param description
	 */
	public void addApiDescription(IApiComponent component, IApiDescription description) {
		fDescriptionCache.put(component, description);
	}
	
	/**
	 * Removes the cached {@link IApiDescription} for the given component and returns 
	 * the cached description, or <code>null</code> if nothing was removed
	 * @param component
	 * @return the removed {@link IApiDescription} or <code>null</code> if nothing was removed
	 */
	public IApiDescription removeApiDescription(IApiComponent component) {
		return (IApiDescription) fDescriptionCache.remove(component);
	}	
}
