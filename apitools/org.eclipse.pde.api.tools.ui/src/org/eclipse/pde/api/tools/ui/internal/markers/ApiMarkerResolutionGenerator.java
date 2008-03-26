/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.markers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;

/**
 * Returns the listing of applicable {@link IMarkerResolution}s given a certain kind of marker.
 * @since 1.0.0
 */
public class ApiMarkerResolutionGenerator implements IMarkerResolutionGenerator2 {

	/**
	 * Default empty listing of {@link IMarkerResolution}s
	 */
	private final IMarkerResolution[] NO_RESOLUTIONS = new IMarkerResolution[0];
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolutionGenerator#getResolutions(org.eclipse.core.resources.IMarker)
	 */
	public IMarkerResolution[] getResolutions(IMarker marker) {
		if (!hasResolutions(marker)) {
			return NO_RESOLUTIONS;
		}
		switch(marker.getAttribute(IApiMarkerConstants.API_MARKER_ATTR_ID, -1)) {
			case IApiMarkerConstants.API_USAGE_MARKER_ID :
				return new IMarkerResolution[] {new FilterProblemResolution(marker)};
			case IApiMarkerConstants.COMPATIBILITY_MARKER_ID :
				return new IMarkerResolution[] {new FilterProblemResolution(marker)};
			case IApiMarkerConstants.DEFAULT_API_PROFILE_MARKER_ID :
				return new IMarkerResolution[] {new DefaultApiProfileResolution()};
			case IApiMarkerConstants.SINCE_TAG_MARKER_ID :
				return new IMarkerResolution[] {new SinceTagResolution(marker), new FilterProblemResolution(marker)};
			case IApiMarkerConstants.VERSION_NUMBERING_MARKER_ID :
				return new IMarkerResolution[] {new VersionNumberingResolution(marker), new FilterProblemResolution(marker)};
			default :
				return NO_RESOLUTIONS;
		}
	}

	public boolean hasResolutions(IMarker marker) {
		return marker.getAttribute(IApiMarkerConstants.API_MARKER_ATTR_ID, -1) > 0;
	}

}
