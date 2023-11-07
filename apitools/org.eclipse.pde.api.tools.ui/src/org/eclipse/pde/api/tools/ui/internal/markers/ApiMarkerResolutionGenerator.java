/*******************************************************************************
 * Copyright (c) 2008, 2021 IBM Corporation and others.
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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.api.tools.internal.ApiBaselineManager;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFilter;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;

/**
 * Returns the listing of applicable {@link IMarkerResolution}s given a certain
 * kind of marker.
 *
 * @since 1.0.0
 */
public class ApiMarkerResolutionGenerator implements IMarkerResolutionGenerator2 {

	/**
	 * Default empty listing of {@link IMarkerResolution}s
	 */
	private final IMarkerResolution[] NO_RESOLUTIONS = new IMarkerResolution[0];

	private final DefaultApiProfileResolution profileResolution = new DefaultApiProfileResolution();

	@Override
	public IMarkerResolution[] getResolutions(IMarker marker) {
		if (!hasResolutions(marker)) {
			return NO_RESOLUTIONS;
		}
		switch (marker.getAttribute(IApiMarkerConstants.API_MARKER_ATTR_ID, -1)) {
			case IApiMarkerConstants.API_USAGE_MARKER_ID: {
			return new IMarkerResolution[] { new ConfigureProblemSeverityForAPIToolsResolution(marker),
						new FilterProblemResolution(marker),
						new FilterProblemWithCommentResolution(marker) };
			}
			case IApiMarkerConstants.COMPATIBILITY_MARKER_ID: {
			if (ApiMarkerResolutionGenerator.hasExplainProblemResolution(marker)) {
				return new IMarkerResolution[] { new ProblemExplainIncompatibilityResolution(marker),
						new FilterProblemResolution(marker), new FilterProblemWithCommentResolution(marker),
						new ConfigureProblemSeverityForAPIToolsResolution(marker) };
			}
			return new IMarkerResolution[] { new ConfigureProblemSeverityForAPIToolsResolution(marker),
						new FilterProblemResolution(marker),
						new FilterProblemWithCommentResolution(marker) };
			}
			case IApiMarkerConstants.DEFAULT_API_BASELINE_MARKER_ID: {
			return new IMarkerResolution[] { profileResolution,
					new ConfigureProblemSeverityForAPIToolsResolution(marker) };
			}
			case IApiMarkerConstants.SINCE_TAG_MARKER_ID: {
				IMarker markerVersion = getVersionProblems(marker);
				IMarkerResolution[] res = (markerVersion != null) ?
				new IMarkerResolution[] {
					new ConfigureProblemSeverityForAPIToolsResolution(marker),
					new SinceTagAfterVersionUpdateResolution(markerVersion, marker),
					new SinceTagResolution(marker),
					new FilterProblemResolution(marker),
					new FilterProblemWithCommentResolution(marker) }:
				new IMarkerResolution[] {
					new ConfigureProblemSeverityForAPIToolsResolution(marker),
					new SinceTagResolution(marker),
					new FilterProblemResolution(marker),
					new FilterProblemWithCommentResolution(marker)
				};
				return res;
			}
			case IApiMarkerConstants.VERSION_NUMBERING_MARKER_ID: {
			return new IMarkerResolution[] {
					new ConfigureProblemSeverityForAPIToolsResolution(marker),
						new VersionNumberingResolution(marker),
						new FilterProblemResolution(marker),
						new FilterProblemWithCommentResolution(marker) };
			}
			case IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID: {
			return new IMarkerResolution[] { new VersionNumberingResolution(marker),
					new UnsupportedTagResolution(marker), new ConfigureProblemSeverityForAPIToolsResolution(marker) };
			}
			case IApiMarkerConstants.DUPLICATE_TAG_MARKER_ID: {
				return new IMarkerResolution[] { new DuplicateTagResolution(marker) };
			}
			case IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID: {
			return new IMarkerResolution[] { new VersionNumberingResolution(marker),
					new UnsupportedAnnotationResolution(marker),
					new ConfigureProblemSeverityForAPIToolsResolution(marker) };
			}
			case IApiMarkerConstants.DUPLICATE_ANNOTATION_MARKER_ID: {
				return new IMarkerResolution[] { new DuplicateAnnotationResolution(marker) };
			}
			 case IApiMarkerConstants.API_COMPONENT_RESOLUTION_MARKER_ID: {
			return new IMarkerResolution[] { new ConfigureProblemSeverityForAPIToolsResolution(marker) };
			}
			case IApiMarkerConstants.UNUSED_PROBLEM_FILTER_MARKER_ID: {
				IApiProblemFilter filter = resolveFilter(marker);
				if (filter != null) {
					return new IMarkerResolution[] {
					     	new ConfigureProblemSeverityForAPIToolsResolution(marker),
							new RemoveFilterProblemResolution(filter, marker),
							new OpenPropertyPageResolution(MarkerMessages.ApiMarkerResolutionGenerator_api_problem_filters, IApiToolsConstants.ID_FILTERS_PROP_PAGE, marker.getResource().getProject()) };
				} else {
					return new IMarkerResolution[] { new OpenPropertyPageResolution(MarkerMessages.ApiMarkerResolutionGenerator_api_problem_filters, IApiToolsConstants.ID_FILTERS_PROP_PAGE, marker.getResource().getProject()) };
				}
			}
			default:
				return NO_RESOLUTIONS;
		}
	}

	/**
	 * resolves the {@link IApiProblemFilter} for the given marker
	 */
	static IApiProblemFilter resolveFilter(IMarker marker) {
		try {
			String filterhandle = marker.getAttribute(IApiMarkerConstants.MARKER_ATTR_FILTER_HANDLE_ID, null);
			String[] values = filterhandle.split(ApiProblemFilter.HANDLE_DELIMITER);
			IProject project = marker.getResource().getProject();
			IApiComponent component = ApiBaselineManager.getManager().getWorkspaceBaseline().getApiComponent(project);
			if (component != null) {
				IApiFilterStore store = component.getFilterStore();
				IPath path = IPath.fromOSString(values[1]);
				IResource resource = project.findMember(path);
				if (resource == null) {
					resource = project.getFile(path);
				}
				int hashcode = ApiProblemFactory.getProblemHashcode(filterhandle);
				IApiProblemFilter[] filters = store.getFilters(resource);
				for (IApiProblemFilter filter : filters) {
					if (filter.getUnderlyingProblem().hashCode() == hashcode) {
						return filter;
					}
				}
			}
		} catch (CoreException ce) {
		}
		return null;
	}

	@Override
	public boolean hasResolutions(IMarker marker) {
		return Util.isApiProblemMarker(marker);
	}

	public static boolean hasExplainProblemResolution(IMarker marker) {
		int id = ApiProblemFactory.getProblemId(marker);
		if (id > -1) {
			if( id == ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY, IDelta.CLASS_ELEMENT_TYPE, IDelta.ADDED, IDelta.FIELD)
			 || id == ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY, IDelta.INTERFACE_ELEMENT_TYPE, IDelta.ADDED, IDelta.FIELD)
					|| id == ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY,
							IDelta.INTERFACE_ELEMENT_TYPE, IDelta.ADDED, IDelta.DEFAULT_METHOD)) {
			return true;
			}
		}
		return false;

	}

	private IMarker getVersionProblems(IMarker marker) {
		IProject project = null;
		IProject project2 = null;
		IResource resource = marker.getResource();
		if (resource != null) {
			project = resource.getProject();
		}
		IMarker markerVersion = null;
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		try {
			IMarker[] findMarkers = root.findMarkers(IApiMarkerConstants.VERSION_NUMBERING_PROBLEM_MARKER, false,
					IResource.DEPTH_INFINITE);
			for (IMarker iMarker : findMarkers) {
				IResource resource2 = iMarker.getResource();
				if (resource2 != null) {
					project2 = resource2.getProject();
					if(project !=null && project2 !=null) {
						if(project.equals(project2)) {
							markerVersion = iMarker;
							break;
						}
					}
				}
			}
		} catch (CoreException e) {
		}
		return markerVersion;
	}
}
