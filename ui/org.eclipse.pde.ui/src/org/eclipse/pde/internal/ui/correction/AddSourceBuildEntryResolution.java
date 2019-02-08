/*******************************************************************************
 *  Copyright (c) 2005, 2019 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.correction;

import java.util.ArrayList;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.core.builders.PDEBuilderHelper;
import org.eclipse.pde.internal.core.builders.PDEMarkerFactory;
import org.eclipse.pde.internal.core.text.build.Build;
import org.eclipse.pde.internal.core.text.build.BuildEntry;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class AddSourceBuildEntryResolution extends BuildEntryMarkerResolution {

	public AddSourceBuildEntryResolution(int type, IMarker marker) {
		super(type, marker);
	}

	@Override
	public String getLabel() {
		return NLS.bind(PDEUIMessages.AddSourceBuildEntryResolution_label, fEntry);
	}

	@Override
	protected void createChange(Build build) {
		try {
			fEntry = (String) marker.getAttribute(PDEMarkerFactory.BK_BUILD_ENTRY);
			fToken = (String) marker.getAttribute(PDEMarkerFactory.BK_BUILD_TOKEN);
		} catch (CoreException e) {
		}
		try {
			BuildEntry buildEntry = (BuildEntry) build.getEntry(fEntry);
			boolean unlistedOnly = true;
			if (buildEntry == null) {
				buildEntry = new BuildEntry(fEntry, build.getModel());
				unlistedOnly = false;
			}
			String[] unlisted = getSourcePaths(build, unlistedOnly);
			for (String token : unlisted) {
				if (token == null){
					break;
				}
				buildEntry.addToken(token);
			}
		} catch (CoreException e) {
		}
	}

	private String[] getSourcePaths(Build build, boolean unlistedOnly) {
		IProject project = build.getModel().getUnderlyingResource().getProject();
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				ArrayList<IBuildEntry> sourceEntries = new ArrayList<>();
				IBuildEntry[] entries = build.getBuildEntries();
				if (unlistedOnly) {
					for (IBuildEntry entry : entries) {
						if (entry.getName().startsWith(IBuildPropertiesConstants.PROPERTY_SOURCE_PREFIX)) {
							sourceEntries.add(entry);
						}
					}
				}

				IJavaProject jp = JavaCore.create(project);
				IClasspathEntry[] cpes = jp.getRawClasspath();
				return PDEBuilderHelper.getUnlistedClasspaths(sourceEntries, project, cpes);
			}
		} catch (JavaModelException e) {
		} catch (CoreException e) {
		}
		return null;
	}
}
