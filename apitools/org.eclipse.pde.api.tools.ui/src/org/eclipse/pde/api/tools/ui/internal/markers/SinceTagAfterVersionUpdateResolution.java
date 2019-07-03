/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.ui.internal.markers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;

public class SinceTagAfterVersionUpdateResolution extends SinceTagResolution {

	IMarker markerVersion = null;
	public SinceTagAfterVersionUpdateResolution(IMarker marker) {
		super(marker);
		updateVersionMarker();

	}

	private void updateVersionMarker() {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		try {
			IMarker[] findMarkers = root.findMarkers(IApiMarkerConstants.VERSION_NUMBERING_PROBLEM_MARKER, false,
					IResource.DEPTH_INFINITE);
			if (findMarkers.length == 1) {
				markerVersion = findMarkers[0];
			}
		} catch (CoreException e) {

		}
	}

	@Override
	public String getLabel() {
		return NLS.bind(MarkerMessages.SinceTagResolution_add_since_tag_after_version_update, markerVersion.getAttribute(IApiMarkerConstants.MARKER_ATTR_VERSION, null));
	}
	@Override
	public void run(final IMarker marker) {



		if (markerVersion != null) {
			new VersionNumberingResolution(markerVersion).run(markerVersion);
			this.newVersionValue = markerVersion.getAttribute(IApiMarkerConstants.MARKER_ATTR_VERSION, null);
		}
		super.run(marker);
	}

}
