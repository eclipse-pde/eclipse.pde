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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;

/**
 * Returns the listing of applicable {@link IMarkerResolution}s given a certain kind of marker.
 * @since 1.0.0
 */
public class ApiMarkerResolutionGenerator implements IMarkerResolutionGenerator {

	/**
	 * Default empty listing of {@link IMarkerResolution}s
	 */
	private final IMarkerResolution[] NO_RESOLUTIONS = new IMarkerResolution[0];
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolutionGenerator#getResolutions(org.eclipse.core.resources.IMarker)
	 */
	public IMarkerResolution[] getResolutions(IMarker marker) {
		try {
			String type = marker.getType();
			if(type.equals(ApiPlugin.API_USAGE_PROBLEM_MARKER)) {
				return new IMarkerResolution[] {new FilterProblemResolution(marker), new ParentFilterProblemResolution(marker)};
			}
			else if(type.equals(ApiPlugin.BINARY_COMPATIBILITY_PROBLEM_MARKER)) {
				return new IMarkerResolution[] {new FilterProblemResolution(marker), new ParentFilterProblemResolution(marker)};
			}
			else if(type.equals(ApiPlugin.DEFAULT_API_PROFILE_PROBLEM_MARKER)) {
				return new IMarkerResolution[] {new DefaultApiProfileResolution()};
			}
			else if(type.equals(ApiPlugin.VERSION_NUMBERING_PROBLEM_MARKER)) {
//				return new IMarkerResolution[] {new VersionNumberingResolution(marker)};
			}
			else if(type.equals(ApiPlugin.SINCE_TAGS_PROBLEM_MARKER)) {
				return new IMarkerResolution[] {new SinceTagResolution(marker)};
			}
		}
		catch(CoreException e) {
			ApiUIPlugin.log(e);
		}
		return NO_RESOLUTIONS;
	}

}
