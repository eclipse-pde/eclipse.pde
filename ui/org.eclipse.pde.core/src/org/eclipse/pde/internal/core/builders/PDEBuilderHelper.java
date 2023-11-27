/*******************************************************************************
 *  Copyright (c) 2007, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.core.natures.FeatureProject;
import org.eclipse.pde.internal.core.natures.PluginProject;
import org.eclipse.pde.internal.core.natures.SiteProject;

public class PDEBuilderHelper {

	public static String[] getUnlistedClasspaths(List<IBuildEntry> sourceEntries, IProject project, IClasspathEntry[] cpes) {
		String[] unlisted = new String[cpes.length];
		int index = 0;
		for (IClasspathEntry entry : cpes) {
			if (entry.getEntryKind() != IClasspathEntry.CPE_SOURCE || entry.isTest()) {
				continue;
			}
			IPath path = entry.getPath();
			boolean found = false;
			for (int j = 0; j < sourceEntries.size(); j++) {
				IBuildEntry be = sourceEntries.get(j);
				String[] tokens = be.getTokens();
				for (String token : tokens) {
					IResource res = project.findMember(token);
					if (res == null) {
						continue;
					}
					IPath ipath = res.getFullPath();
					if (ipath.equals(path)) {
						found = true;
					}
				}
			}
			if (!found) {
				unlisted[index++] = path.removeFirstSegments(1).addTrailingSeparator().toString();
			}
		}
		return unlisted;
	}

	public static ArrayList<String> getSourceEntries(IBuild build) {
		ArrayList<String> sourceEntryKeys = new ArrayList<>();
		IBuildEntry[] entries = build.getBuildEntries();
		for (IBuildEntry buildEntry : entries) {
			String name = buildEntry.getName();
			if (name.startsWith(IBuildPropertiesConstants.PROPERTY_SOURCE_PREFIX)) {
				// splice the entry
				String entry = name.substring(IBuildPropertiesConstants.PROPERTY_SOURCE_PREFIX.length(), name.length());
				sourceEntryKeys.add(entry);
			}
		}
		return sourceEntryKeys;
	}

	public static boolean isPDEProject(IProject project) {
		return project != null && project.isAccessible() && //
				(PluginProject.isPluginProject(project) || FeatureProject.isFeatureProject(project)
						|| SiteProject.isSiteProject(project));
	}

	public static boolean hasManifestBuilder(IProject project) {
		if (project != null && project.isAccessible()) {
			try {
				return Arrays.stream(project.getDescription().getBuildSpec())
						.anyMatch(cmd -> PluginProject.MANIFEST_BUILDER_ID.equals(cmd.getBuilderName()));
			} catch (CoreException e) {
				// must assume then that it has not the required builder
			}
		}
		return false;
	}

}
