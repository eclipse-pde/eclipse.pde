/**********************************************************************
 * Copyright (c) 2004, 2006 Eclipse Foundation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - Initial API and implementation
 *     IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.build.IFetchFactory;

/**
 * A registry for acessing fetch task factories.
 * @since 3.2
 */
public class FetchTaskFactoriesRegistry implements IPDEBuildConstants {

	// Map of registered factories. key: factoryId, value: configuration element or corresponding instance
	private Map factories;
	
	public FetchTaskFactoriesRegistry() {
		factories = new HashMap();
		initializeRegistry();
	}

	/**
	 * Returns the factory instance with the specified id.
	 * <p>
	 * The instance is not cached. Each time this method is called, a new
	 * instance is created.
	 * </p>
	 * 
	 * @param id
	 * @return the factory instance (maybe <code>null</code>)
	 */
	public IFetchFactory getFactory(String id) {
		Object result = factories.get(id);
		if (result instanceof IFetchFactory)
			return (IFetchFactory) result;
		
		IConfigurationElement extension = (IConfigurationElement) factories.get(id);
		if (null != extension) {
			try {
				IFetchFactory toCache = (IFetchFactory) extension.createExecutableExtension(ATTR_CLASS);
				factories.put(id, toCache);
				return toCache;
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
	public Collection getFactoryIds() {
		return factories.keySet();
	}

	/**
	 * Initializes the registry
	 */
	private void initializeRegistry() {
		IConfigurationElement[] extensions = Platform.getExtensionRegistry().getConfigurationElementsFor(EXT_FETCH_TASK_FACTORIES);
		for (int i = 0; i < extensions.length; i++) {
			IConfigurationElement extension = extensions[i];
			if (ELEM_FACTORY.equals(extension.getName())) {
				String id = extension.getAttribute(ATTR_ID);
				if (null != id && id.trim().length() > 0) {
					factories.put(id, extension);
				}
			}
		}
	}
}
