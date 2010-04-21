/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.markers;

import java.util.HashSet;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;

import com.ibm.icu.text.MessageFormat;

/**
 * Marker resolution for adding an API filter for the specific member the marker appears on
 * 
 * @since 1.0.0
 */
public class FilterProblemResolution extends WorkbenchMarkerResolution {

	protected IMarker fBackingMarker = null;
	protected IJavaElement fResolvedElement = null;
	protected String fCategory = null;
	boolean plural = false;
	
	/**
	 * Constructor
	 * @param marker the backing marker for the resolution
	 */
	public FilterProblemResolution(IMarker marker) {
		fBackingMarker = marker;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution2#getDescription()
	 */
	public String getDescription() {
		try {
			String value = (String) fBackingMarker.getAttribute(IApiMarkerConstants.MARKER_ATTR_MESSAGE_ARGUMENTS);
			String[] args = new String[0];
			if(value != null) {
				args = value.split("#"); //$NON-NLS-1$
			}
			int id = fBackingMarker.getAttribute(IApiMarkerConstants.MARKER_ATTR_PROBLEM_ID, 0);
			return ApiProblemFactory.getLocalizedMessage(ApiProblemFactory.getProblemMessageId(id), args);
		} catch (CoreException e) {}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution2#getImage()
	 */
	public Image getImage() {
		return ApiUIPlugin.getSharedImage(IApiToolsConstants.IMG_ELCL_FILTER);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution#getLabel()
	 */
	public String getLabel() {
		if(plural) {
			return MarkerMessages.FilterProblemResolution_create_filters_for_problems;
		}
		else {
			IJavaElement element = resolveElementFromMarker();
			if(element != null) {
				return MessageFormat.format(MarkerMessages.FilterProblemResolution_0, new String[] {JavaElementLabels.getTextLabel(element, JavaElementLabels.M_PARAMETER_TYPES), resolveCategoryName()});
			}
			else {
				IResource res = fBackingMarker.getResource();
				return MessageFormat.format(MarkerMessages.FilterProblemResolution_0, new String[] {res.getFullPath().removeFileExtension().lastSegment(), resolveCategoryName()});
			}
		}
	}
	
	/**
	 * Returns the category name from the problem id contained in the 
	 * backing marker.
	 * @return the name of the category from the markers' problem id
	 */
	protected String resolveCategoryName() {
		if(fCategory == null) {
			int problemid = fBackingMarker.getAttribute(IApiMarkerConstants.MARKER_ATTR_PROBLEM_ID, -1);
			int category = ApiProblemFactory.getProblemCategory(problemid);
			switch(category) {
				case IApiProblem.CATEGORY_COMPATIBILITY: {
					fCategory = MarkerMessages.FilterProblemResolution_compatible;
					break;
				}
				case IApiProblem.CATEGORY_API_BASELINE: {
					fCategory = MarkerMessages.FilterProblemResolution_default_profile;
					break;
				}
				case IApiProblem.CATEGORY_API_COMPONENT_RESOLUTION: {
					fCategory = MarkerMessages.FilterProblemResolution_api_component;
					break;
				}
				case IApiProblem.CATEGORY_SINCETAGS: {
					fCategory = MarkerMessages.FilterProblemResolution_since_tag;
					break;
				}
				case IApiProblem.CATEGORY_USAGE: {
					fCategory = MarkerMessages.FilterProblemResolution_usage;
					break;
				}
				case IApiProblem.CATEGORY_VERSION: {
					fCategory = MarkerMessages.FilterProblemResolution_version_number;
				}
			}
		}
		return fCategory;
	}
	
	/**
	 * Resolves the {@link IJavaElement} from the infos in the marker.
	 * 
	 * @return the associated {@link IJavaElement} for the infos in the {@link IMarker}
	 */
	protected IJavaElement resolveElementFromMarker() {
		if(fResolvedElement == null) {
			try {
				String handle = (String) fBackingMarker.getAttribute(IApiMarkerConstants.MARKER_ATTR_HANDLE_ID);
				if(handle != null) {
					fResolvedElement = JavaCore.create(handle);
				}
			}
			catch(CoreException ce) {
				ApiUIPlugin.log(ce);
			}
		}
		return fResolvedElement;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.markers.WorkbenchMarkerResolution#run(org.eclipse.core.resources.IMarker[], org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IMarker[] markers, IProgressMonitor monitor) {
		CreateApiFilterOperation op = new CreateApiFilterOperation(markers, false);
		op.setSystem(true);
		op.schedule();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution#run(org.eclipse.core.resources.IMarker)
	 */
	public void run(IMarker marker) {
		run(new IMarker[] {marker}, null);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.markers.WorkbenchMarkerResolution#findOtherMarkers(org.eclipse.core.resources.IMarker[])
	 */
	public IMarker[] findOtherMarkers(IMarker[] markers) {
		HashSet mset = new HashSet(markers.length);
		for (int i = 0; i < markers.length; i++) {
			try {
				if(Util.isApiProblemMarker(markers[i]) &&
						!fBackingMarker.equals(markers[i]) &&
						!markers[i].getType().equals(IApiMarkerConstants.UNUSED_FILTER_PROBLEM_MARKER)) {
					mset.add(markers[i]);
				}
			}
			catch(CoreException ce) {
				//do nothing just don't add the filter
			}
		}
		int size = mset.size();
		plural = size > 0;
		return (IMarker[]) mset.toArray(new IMarker[size]);
	}
}
