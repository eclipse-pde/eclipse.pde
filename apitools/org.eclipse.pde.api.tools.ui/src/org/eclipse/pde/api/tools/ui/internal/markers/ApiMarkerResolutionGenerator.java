/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.markers;

import java.util.ArrayList;
import java.util.List;

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
 * Returns the listing of applicable {@link IMarkerResolution}s given a certain kind of marker.
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
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolutionGenerator#getResolutions(org.eclipse.core.resources.IMarker)
	 */
	public IMarkerResolution[] getResolutions(IMarker marker) {
		if (!hasResolutions(marker)) {
			return NO_RESOLUTIONS;
		}
		switch(marker.getAttribute(IApiMarkerConstants.API_MARKER_ATTR_ID, -1)) {
			case IApiMarkerConstants.API_USAGE_MARKER_ID : {
				int id = ApiProblemFactory.getProblemId(marker);
				if(id > -1 && ApiProblemFactory.getProblemKind(id) == IApiProblem.MISSING_EE_DESCRIPTIONS) {
					return new IMarkerResolution[] {installEEResolution, eeResolution};
				}
				return new IMarkerResolution[] {new FilterProblemResolution(marker), new FilterProblemWithCommentResolution(marker)};
			}
			case IApiMarkerConstants.COMPATIBILITY_MARKER_ID : {
				return new IMarkerResolution[] {new FilterProblemResolution(marker), new FilterProblemWithCommentResolution(marker)};
			}
			case IApiMarkerConstants.DEFAULT_API_BASELINE_MARKER_ID : {
				return new IMarkerResolution[] {profileResolution};
			}
			case IApiMarkerConstants.SINCE_TAG_MARKER_ID : {
				return new IMarkerResolution[] {new SinceTagResolution(marker), new FilterProblemResolution(marker), new FilterProblemWithCommentResolution(marker)};
			}
			case IApiMarkerConstants.VERSION_NUMBERING_MARKER_ID : {
				return new IMarkerResolution[] {new VersionNumberingResolution(marker), new FilterProblemResolution(marker), new FilterProblemWithCommentResolution(marker)};
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
							new RemoveFilterProblemResolution(filter, marker),
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
	static IApiProblemFilter resolveFilter(IMarker marker) {
		try {
			String filterhandle = marker.getAttribute(IApiMarkerConstants.MARKER_ATTR_FILTER_HANDLE_ID, null);
			String[] values = filterhandle.split(ApiProblemFilter.HANDLE_DELIMITER);
			IProject project = marker.getResource().getProject();
			IApiComponent component = ApiBaselineManager.getManager().getWorkspaceBaseline().getApiComponent(project);
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
	 * Computes the hash code of the {@link IApiProblem} from the api filter handle
	 * @param filterhandle
	 * @return the hash code of the {@link IApiProblem} that the given filter handle is for
	 */
	private static int computeProblemHashcode(String filterhandle) {
		if(filterhandle == null) {
			return -1;
		}
		String[] args = filterhandle.split(ApiProblemFilter.HANDLE_DELIMITER);
		int hashcode = 0;
		try {
			//the problem id
			hashcode += Integer.parseInt(args[0]);
			//the resource path
			hashcode += args[1].hashCode();
			//the type name
			hashcode += args[2].hashCode();
			//the message arguments
			String[] margs = splitHandle(args[3], ApiProblemFilter.HANDLE_ARGUMENTS_DELIMITER);
			hashcode += argumentsHashcode(margs);
		}
		catch(Exception e) {}
		return hashcode;
	}
	
	private static String[] splitHandle(String messageArguments, String delimiter) {
		List matches = null;
		char[] argumentsChars = messageArguments.toCharArray();
		char[] delimiterChars = delimiter.toCharArray();
		int delimiterLength = delimiterChars.length;
		int start = 0;
		int argumentsCharsLength = argumentsChars.length;
		int balance = 0;
		for (int i = 0; i < argumentsCharsLength;) {
			char c = argumentsChars[i];
			switch(c) {
			case '(' :
				balance++;
				break;
			case ')' :
				balance--;
			}
			if (c == delimiterChars[0] && balance == 0) {
				// see if this is a matching delimiter start only if not within parenthesis (balance == 0)
				if (i + delimiterLength < argumentsCharsLength) {
					boolean match = true;
					loop: for (int j = 1; j < delimiterLength; j++) {
						if (argumentsChars[i + j] != delimiterChars[j]) {
							match = false;
							break loop;
						}
					}
					if (match) {
						// record the matching substring and proceed
						if (matches == null) {
							matches = new ArrayList();
						}
						matches.add(messageArguments.substring(start, i));
						start = i + delimiterLength;
						i += delimiterLength;
					} else {
						i++;
					}
				} else {
					i++;
				}
			} else {
				i++;
			}
		}
		if (matches == null) {
			return new String[] { messageArguments };
		} else {
			matches.add(messageArguments.substring(start, argumentsCharsLength));
		}
		return (String[]) matches.toArray(new String[matches.size()]);
	}
	/**
	 * Returns the deep hash code of the complete listing of message arguments
	 * @param arguments
	 * @return the hash code of the message arguments
	 */
	private static int argumentsHashcode(String[] arguments) {
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
		return Util.isApiProblemMarker(marker);
	}
}
