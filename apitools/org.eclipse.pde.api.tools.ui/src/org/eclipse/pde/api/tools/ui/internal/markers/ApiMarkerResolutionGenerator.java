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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.api.tools.internal.ApiBaselineManager;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
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
			case IApiMarkerConstants.API_USAGE_MARKER_ID : {
				int problemid = marker.getAttribute(IApiMarkerConstants.MARKER_ATTR_PROBLEM_ID, -1);
				int flags  = ApiProblemFactory.getProblemFlags(problemid);
				switch(ApiProblemFactory.getProblemKind(problemid)) {
					case IApiProblem.API_LEAK: {
						if(flags == IApiProblem.LEAK_METHOD_PARAMETER || 
						 flags == IApiProblem.LEAK_METHOD_PARAMETER ||
						 flags == IApiProblem.LEAK_RETURN_TYPE) {
							return new IMarkerResolution[] {new FilterProblemResolution(marker), new AddNoReferenceTagResolution(marker)};
						}
						break;
					}
					default: {
						return new IMarkerResolution[] {new FilterProblemResolution(marker)};
					}
				}
				return NO_RESOLUTIONS;
			}
			case IApiMarkerConstants.COMPATIBILITY_MARKER_ID : {
				return new IMarkerResolution[] {new FilterProblemResolution(marker)};
			}
			case IApiMarkerConstants.DEFAULT_API_PROFILE_MARKER_ID : {
				return new IMarkerResolution[] {new DefaultApiProfileResolution()};
			}
			case IApiMarkerConstants.SINCE_TAG_MARKER_ID : {
				return new IMarkerResolution[] {new SinceTagResolution(marker), new FilterProblemResolution(marker)};
			}
			case IApiMarkerConstants.VERSION_NUMBERING_MARKER_ID : {
				return new IMarkerResolution[] {new VersionNumberingResolution(marker), new FilterProblemResolution(marker)};
			}
			case IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID: {
				return new IMarkerResolution[] {new UnsupportedTagResolution(marker)};
			}
			case IApiMarkerConstants.DUPLICATE_TAG_MARKER_ID: {
				return new IMarkerResolution[] {new DuplicateTagResolution(marker)};
			}
			case IApiMarkerConstants.API_COMPONENT_RESOLUTION_MARKER_ID: {
				return new IMarkerResolution[] {new UpdateProjectSettingResolution(marker)};
			}
			case IApiMarkerConstants.UNUSED_PROBLEM_FILTER_MARKER_ID: {
				IApiProblemFilter filter = resolveFilter(marker);
				if(filter != null) {
					return new IMarkerResolution[] {
							new RemoveFilterProblemResolution(filter),
							new OpenPropertyPageResolution(
									MarkerMessages.ApiMarkerResolutionGenerator_api_problem_filters,
									IApiToolsConstants.ID_FILTERS_PROP_PAGE,
									marker.getResource().getProject())};
				}
				else {
					return new IMarkerResolution[] {
							new OpenPropertyPageResolution(
									MarkerMessages.ApiMarkerResolutionGenerator_api_problem_filters,
									IApiToolsConstants.ID_FILTERS_PROP_PAGE,
									marker.getResource().getProject())};
				}
			}
			default : return NO_RESOLUTIONS;
		}
	}
	
	/**
	 * resolves the {@link IApiProblemFilter} for the given marker
	 * @param marker
	 */
	private IApiProblemFilter resolveFilter(IMarker marker) {
		try {
			String filterhandle = marker.getAttribute(IApiMarkerConstants.MARKER_ATTR_FILTER_HANDLE_ID, null);
			String[] values = filterhandle.split("%]"); //$NON-NLS-1$
			IProject project = marker.getResource().getProject();
			IApiComponent component = ApiBaselineManager.getManager().getWorkspaceBaseline().getApiComponent(project.getName());
			if(component != null) {
				IApiFilterStore store = component.getFilterStore();
				IPath path = new Path(values[1]);
				IResource resource = project.findMember(path);
				if(resource == null) {
					resource = project.getFile(path);
				}
				int hashcode = computeProblemHashcode(filterhandle);
				IApiProblemFilter[] filters = store.getFilters(resource);
				for (int i = 0; i < filters.length; i++) {
					if(filters[i].getUnderlyingProblem().hashCode() == hashcode) {
						return filters[i];
					}
				}
			}
		}
		catch(CoreException ce) {}
		return null;
	}
	
	/**
	 * Computes the hashcode of the {@link IApiProblem} from the api filter handle
	 * @param filterhandle
	 * @return the hashcode of the {@link IApiProblem} that the given filter handle is for
	 */
	private int computeProblemHashcode(String filterhandle) {
		if(filterhandle == null) {
			return -1;
		}
		String[] args = filterhandle.split("%]"); //$NON-NLS-1$
		int hashcode = 0;
		try {
			//the problem id
			hashcode += Integer.parseInt(args[0]);
			//the resource path
			hashcode += args[1].hashCode();
			//the type name
			hashcode += args[2].hashCode();
			//the message arguments
			String[] margs = args[3].split(","); //$NON-NLS-1$
			hashcode += argumentsHashcode(margs);
		}
		catch(Exception e) {}
		return hashcode;
	}
	
	/**
	 * Returns the deep hash code of the complete listing of message arguments
	 * @param arguments
	 * @return the hash code of the message arguments
	 */
	private int argumentsHashcode(String[] arguments) {
		if(arguments == null) {
			return 0;
		}
		int hashcode = 0;
		for(int i = 0; i < arguments.length; i++) {
			hashcode += arguments[i].hashCode();
		}
		return hashcode;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolutionGenerator2#hasResolutions(org.eclipse.core.resources.IMarker)
	 */
	public boolean hasResolutions(IMarker marker) {
		return marker.getAttribute(IApiMarkerConstants.API_MARKER_ATTR_ID, -1) > 0;
	}
}
