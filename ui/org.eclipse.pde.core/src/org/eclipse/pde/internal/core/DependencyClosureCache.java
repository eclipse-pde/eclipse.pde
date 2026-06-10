/*******************************************************************************
 *  Copyright (c) 2026 Lars Vogel and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Lars Vogel <Lars.Vogel@vogella.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.eclipse.osgi.service.resolver.BundleDescription;

/**
 * Shares the computation of per-bundle dependency closures between the
 * classpath computations of multiple projects. Cached closures are only valid
 * for one resolved target platform state, so a cache must not outlive the
 * operation it is created for.
 */
public final class DependencyClosureCache {

	private final Map<BundleDescription, Set<BundleDescription>> closures = new ConcurrentHashMap<>();

	Set<BundleDescription> closureOf(BundleDescription bundle,
			Function<BundleDescription, Set<BundleDescription>> computation) {
		return closures.computeIfAbsent(bundle, computation);
	}
}
