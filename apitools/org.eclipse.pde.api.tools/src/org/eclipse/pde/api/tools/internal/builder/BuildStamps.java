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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;

/**
 * Stores relative build time stamps for each project.
 */
public class BuildStamps {

	private static Map<IProject, long[]> fStamps = new HashMap<>();

	/**
	 * Returns the current build time stamp for the given project.
	 *
	 * @param project project
	 * @return relative build time stamp
	 */
	public static synchronized long getBuildStamp(IProject project) {
		long[] stamp = fStamps.get(project);
		if (stamp != null) {
			return stamp[0];
		}
		return 0L;
	}

	/**
	 * Increments the build time stamp for the given project. Only to be called
	 * by the builder.
	 *
	 * @param project project being built
	 */
	public static synchronized void incBuildStamp(IProject project) {
		long[] stamp = fStamps.get(project);
		if (stamp != null) {
			stamp[0] = stamp[0]++;
		} else {
			fStamps.put(project, new long[] { 1L });
		}
	}
}
