/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal.builder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IProject;

/**
 * Stores relative build time stamps for each project.
 */
public class BuildStamps {

	private static final Map<IProject, Long> fStamps = new ConcurrentHashMap<>();

	/**
	 * Returns the current build time stamp for the given project.
	 *
	 * @param project project
	 * @return relative build time stamp
	 */
	public static long getBuildStamp(IProject project) {
		return fStamps.getOrDefault(project, 0L);
	}

	/**
	 * Increments the build time stamp for the given project. Only to be called
	 * by the builder.
	 *
	 * @param project project being built
	 */
	public static void incBuildStamp(IProject project) {
		fStamps.compute(project, (p, v) -> v == null ? 1L : v + 1L);
	}
}
