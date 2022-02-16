/*******************************************************************************
 * Copyright (c) 2013, 2021 OPCoach.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     OPCoach - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.spy.context;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.osgi.framework.BundleContext;

/**
 * A helper class to get information inside context management system. This
 * class uses internal fields or methods defined in EclipseContext Could be
 * updated in the future.
 *
 * @author olivier
 *
 */
public class ContextSpyHelper {

	/**
	 * Get all the contexts created by EclipseContextFactory. It get values from
	 * field introspection. Should be rewritten if internal structure changes
	 *
	 * @return a collection of contexts created by EclipseContextFactory
	 */
	public static Collection<IEclipseContext> getAllBundleContexts() {
		Collection<IEclipseContext> result = Collections.emptyList();
		try {
			// Must use introspection to get the weak hash map (no getter).
			Field f = EclipseContextFactory.class.getDeclaredField("serviceContexts"); //$NON-NLS-1$
			f.setAccessible(true);
			@SuppressWarnings("unchecked")
			Map<BundleContext, IEclipseContext> ctxs = (Map<BundleContext, IEclipseContext>) f.get(null);
			result = ctxs.values();

		} catch (SecurityException | NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}

		return result;

	}

}
