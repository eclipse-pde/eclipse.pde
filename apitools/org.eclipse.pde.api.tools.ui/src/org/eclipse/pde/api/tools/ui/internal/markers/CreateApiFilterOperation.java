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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.IApiProblemFilter;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.ui.progress.UIJob;

/**
 * Operation for creating a new API problem filter
 * 
 * @see IApiProblem
 * @see IApiProblemFilter
 * @see IApiFilterStore
 * 
 * @since 1.0.0
 */
public class CreateApiFilterOperation extends UIJob {

	private IMarker fBackingMarker = null;
	
	/**
	 * Constructor
	 * @param element the element to create the filter for (method, field, class, enum, etc)
	 * @param kind the kind of filter to create
	 * 
	 * @see IApiProblemFilter#getKinds()
	 */
	public CreateApiFilterOperation(IMarker marker) {
		super(MarkerMessages.CreateApiFilterOperation_0);
		fBackingMarker = marker;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.progress.UIJob#runInUIThread(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus runInUIThread(IProgressMonitor monitor) {
		try {
			IResource resource = fBackingMarker.getResource();
			IProject project = resource.getProject();
			if(project == null) {
				return Status.CANCEL_STATUS;
			}
			IApiComponent component = ApiPlugin.getDefault().getApiProfileManager().getWorkspaceProfile().getApiComponent(project.getName());
			if(component == null) {
				return Status.CANCEL_STATUS;
			}
			IApiFilterStore store = component.getFilterStore();
			store.addFilter(Factory.newApiProblem(resource, 
					fBackingMarker.getAttribute(IMarker.MESSAGE, IApiToolsConstants.EMPTY_STRING),
					fBackingMarker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING),
					fBackingMarker.getAttribute(IApiMarkerConstants.MARKER_ATTR_CATEGORY, 0),
					fBackingMarker.getAttribute(IApiMarkerConstants.MARKER_ATTR_KIND, 0), 
					fBackingMarker.getAttribute(IApiMarkerConstants.MARKER_ATTR_FLAGS, 0)));
			cleanupMarkers(false);
			return Status.OK_STATUS;
		}
		catch(CoreException ce) {
			ApiUIPlugin.log(ce);
		}
		return Status.CANCEL_STATUS;
	}
	
	/**
	 * Cleans up all of the marker this operation is acting on, and optionally 
	 * removes similar markers from the child resources
	 * @param childmarkers if child markers should also be cleaned 
	 * @throws CoreException
	 */
	private void cleanupMarkers(boolean childmarkers) throws CoreException {
		fBackingMarker.delete();
		if(childmarkers) {
			IResource res = fBackingMarker.getResource();
			int backingkind = fBackingMarker.getAttribute(IApiMarkerConstants.MARKER_ATTR_KIND, -1);
			if(backingkind == -1) {
				//nothing we can do if there are no kinds to compare: we don not ever want to just remove all 
				//markers of the same marker id
				return;
			}
			int backingflag = fBackingMarker.getAttribute(IApiMarkerConstants.MARKER_ATTR_FLAGS, -1);
			IMarker[] children = res.findMarkers(fBackingMarker.getType(), true, IResource.DEPTH_INFINITE);
			IMarker marker = null;
			for(int i = 0; i < children.length; i++) {
				marker = children[i];
				if (backingkind == marker.getAttribute(IApiMarkerConstants.MARKER_ATTR_KIND, -1) &&
						backingflag == marker.getAttribute(IApiMarkerConstants.MARKER_ATTR_FLAGS, -1)) {
					marker.delete();
				}
			}
		}
	}
}
