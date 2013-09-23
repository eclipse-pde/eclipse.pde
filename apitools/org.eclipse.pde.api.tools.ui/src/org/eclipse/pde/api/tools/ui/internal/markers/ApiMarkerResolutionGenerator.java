/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFilter;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
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

	private MissingEEDescriptionProblemResolution eeResolution = new MissingEEDescriptionProblemResolution();
	private InstallEEDescriptionProblemResolution installEEResolution = new InstallEEDescriptionProblemResolution();
	private DefaultApiProfileResolution profileResolution = new DefaultApiProfileResolution();

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.ui.IMarkerResolutionGenerator#getResolutions(org.eclipse.
	 * core.resources.IMarker)
	 */
	@Override
	public IMarkerResolution[] getResolutions(IMarker marker) {
		if (!hasResolutions(marker)) {
			return NO_RESOLUTIONS;
		}
		switch (marker.getAttribute(IApiMarkerConstants.API_MARKER_ATTR_ID, -1)) {
			case IApiMarkerConstants.API_USAGE_MARKER_ID: {
				int id = ApiProblemFactory.getProblemId(marker);
				if (id > -1 && ApiProblemFactory.getProblemKind(id) == IApiProblem.MISSING_EE_DESCRIPTIONS) {
					return new IMarkerResolution[] {
							installEEResolution, eeResolution };
				}
				return new IMarkerResolution[] {
						new FilterProblemResolution(marker),
						new FilterProblemWithCommentResolution(marker) };
			}
			case IApiMarkerConstants.COMPATIBILITY_MARKER_ID: {
				return new IMarkerResolution[] {
						new FilterProblemResolution(marker),
						new FilterProblemWithCommentResolution(marker) };
			}
			case IApiMarkerConstants.DEFAULT_API_BASELINE_MARKER_ID: {
				return new IMarkerResolution[] { profileResolution };
			}
			case IApiMarkerConstants.SINCE_TAG_MARKER_ID: {
				return new IMarkerResolution[] {
						new SinceTagResolution(marker),
						new FilterProblemResolution(marker),
						new FilterProblemWithCommentResolution(marker) };
			}
			case IApiMarkerConstants.VERSION_NUMBERING_MARKER_ID: {
				return new IMarkerResolution[] {
						new VersionNumberingResolution(marker),
						new FilterProblemResolution(marker),
						new FilterProblemWithCommentResolution(marker) };
			}
			case IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID: {
				return new IMarkerResolution[] { new UnsupportedTagResolution(marker) };
			}
			case IApiMarkerConstants.DUPLICATE_TAG_MARKER_ID: {
				return new IMarkerResolution[] { new DuplicateTagResolution(marker) };
			}
			case IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID: {
				return new IMarkerResolution[] { new UnsupportedAnnotationResolution(marker) };
			}
			case IApiMarkerConstants.DUPLICATE_ANNOTATION_MARKER_ID: {
				return new IMarkerResolution[] { new DuplicateAnnotationResolution(marker) };
			}
			case IApiMarkerConstants.API_COMPONENT_RESOLUTION_MARKER_ID: {
				return new IMarkerResolution[] { new UpdateProjectSettingResolution(marker) };
			}
			case IApiMarkerConstants.UNUSED_PROBLEM_FILTER_MARKER_ID: {
				IApiProblemFilter filter = resolveFilter(marker);
				if (filter != null) {
					return new IMarkerResolution[] {
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
	 * 
	 * @param marker
	 */
	static IApiProblemFilter resolveFilter(IMarker marker) {
		try {
			String filterhandle = marker.getAttribute(IApiMarkerConstants.MARKER_ATTR_FILTER_HANDLE_ID, null);
			String[] values = filterhandle.split(ApiProblemFilter.HANDLE_DELIMITER);
			IProject project = marker.getResource().getProject();
			IApiComponent component = ApiBaselineManager.getManager().getWorkspaceBaseline().getApiComponent(project);
			if (component != null) {
				IApiFilterStore store = component.getFilterStore();
				IPath path = new Path(values[1]);
				IResource resource = project.findMember(path);
				if (resource == null) {
					resource = project.getFile(path);
				}
				int hashcode = ApiProblemFactory.getProblemHashcode(filterhandle);
				IApiProblemFilter[] filters = store.getFilters(resource);
				for (int i = 0; i < filters.length; i++) {
					if (filters[i].getUnderlyingProblem().hashCode() == hashcode) {
						return filters[i];
					}
				}
			}
		} catch (CoreException ce) {
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.ui.IMarkerResolutionGenerator2#hasResolutions(org.eclipse
	 * .core.resources.IMarker)
	 */
	@Override
	public boolean hasResolutions(IMarker marker) {
		return Util.isApiProblemMarker(marker);
	}
}
