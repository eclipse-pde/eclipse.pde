/**********************************************************************
 * Copyright (c) 2004, 2021 Eclipse Foundation and others.
 *
 *   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Gunnar Wagenknecht - Initial API and implementation
 *     IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.build.IFetchFactory;

/**
 * A registry for accessing fetch task factories.
 * @since 3.2
 */
public class FetchTaskFactoriesRegistry implements IPDEBuildConstants {

	// Map of registered factories. key: factoryId, value: configuration element or corresponding instance
	private final Map<String, IConfigurationElement> factories;
	private final Map<String, IFetchFactory> cache;

	public FetchTaskFactoriesRegistry() {
		factories = new HashMap<>();
		cache = new HashMap<>();
		initializeRegistry();
	}

	/**
	 * Returns the factory instance with the specified id.
	 * <p>
	 * The instance is cached, subsequent calls with the same id will 
	 * return the same factory instance.
	 * </p>
	 * @return the factory instance (maybe <code>null</code>)
	 */
	public IFetchFactory getFactory(String id) {
		Object result = cache.get(id);
		if (result != null) {
			return (IFetchFactory) result;
		}

		IFetchFactory toCache = newFactory(id);
		if (toCache != null) {
			cache.put(id, toCache);
			return toCache;
		}
		return null;
	}

	/**
	 * Creates a new factory instance with the specified id
	 *  <p>
	 * The instance is not cached. Each time this method is called, a new
	 * instance is created.
	 * </p>
	 * @return the factory instance (maybe <code>null</code>)
	 */
	public IFetchFactory newFactory(String id) {
		IConfigurationElement extension = factories.get(id);
		if (null != extension) {
			try {
				IFetchFactory factory = (IFetchFactory) extension.createExecutableExtension(ATTR_CLASS);
				return factory;
			} catch (CoreException e) {
				BundleHelper.getDefault().getLog().log(e.getStatus());
			}
		}
		return null;
	}

	/**
	 * Returns a collection of registered factory ids.
	 * 
	 * @return a collection of registered factory ids
	 */
	public Collection<String> getFactoryIds() {
		return factories.keySet();
	}

	/**
	 * Initializes the registry
	 */
	private void initializeRegistry() {
		IConfigurationElement[] extensions = Platform.getExtensionRegistry().getConfigurationElementsFor(EXT_FETCH_TASK_FACTORIES);
		for (IConfigurationElement extension : extensions) {
			if (ELEM_FACTORY.equals(extension.getName())) {
				String id = extension.getAttribute(ATTR_ID);
				if (null != id && id.trim().length() > 0) {
					factories.put(id, extension);
				}
			}
		}
	}
}
