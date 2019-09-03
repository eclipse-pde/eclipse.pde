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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.osgi.framework.Version;

public class SinceTagAfterVersionUpdateResolution extends SinceTagResolution {

	IMarker markerVersion = null;
	public SinceTagAfterVersionUpdateResolution(IMarker markerVer ,IMarker marker) {
		super(marker);
		markerVersion = markerVer;

	}

	@Override
	public String getLabel() {
		return NLS.bind(MarkerMessages.SinceTagResolution_add_since_tag_after_version_update, markerVersion.getAttribute(IApiMarkerConstants.MARKER_ATTR_VERSION, null));
	}
	@Override
	public void run(final IMarker marker) {
		if (markerVersion != null) {
			new VersionNumberingResolution(markerVersion).run(markerVersion);
			String componentVersionString = markerVersion.getAttribute(IApiMarkerConstants.MARKER_ATTR_VERSION, null);
			StringBuilder buffer = new StringBuilder();
			Version componentVersion = new Version(componentVersionString);
			buffer.append(componentVersion.getMajor()).append('.').append(componentVersion.getMinor());
			try {
				markerVersion.setAttribute(IApiMarkerConstants.MARKER_ATTR_VERSION, buffer.toString());
			} catch (CoreException e) {
				ApiUIPlugin.log(e);
			}
			this.newVersionValue = markerVersion.getAttribute(IApiMarkerConstants.MARKER_ATTR_VERSION, null);
		}
		super.run(marker);
	}

	@Override
	public String getDescription() {
		return NLS.bind(MarkerMessages.SinceTagResolution_add_since_tag_after_version_update,
				markerVersion.getAttribute(IApiMarkerConstants.MARKER_ATTR_VERSION, null));
	}

}
