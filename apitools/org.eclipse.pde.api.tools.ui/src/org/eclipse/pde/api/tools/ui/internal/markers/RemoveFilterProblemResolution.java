/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.ApiBaselineManager;
import org.eclipse.pde.api.tools.internal.model.PluginProjectApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;

/**
 * Resolution that removes the selected {@link org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter}
 * 
 * @since 1.0.0
 */
public class RemoveFilterProblemResolution extends WorkbenchMarkerResolution {

	/**
	 * The {@link IApiProblemFilter} to remove
	 */
	IApiProblemFilter fFilter = null;
	private IMarker fMarker = null;
	boolean plural = false;
	
	
	/**
	 * Constructor
	 * @param filter the original associated problem filter
	 * @param marker the original marker this quick fix was opened on
	 */
	public RemoveFilterProblemResolution(IApiProblemFilter filter, IMarker marker) {
		fFilter = filter;
		fMarker = marker;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution2#getDescription()
	 */
	public String getDescription() {
		return NLS.bind(MarkerMessages.RemoveFilterProblemResolution_removes_selected_problem_filter, MarkerUtilities.getMessage(fMarker));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution2#getImage()
	 */
	public Image getImage() {
		return ApiUIPlugin.getSharedImage(IApiToolsConstants.IMG_ELCL_REMOVE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution#getLabel()
	 */
	public String getLabel() {
		if(plural) {
			return MarkerMessages.RemoveFilterProblemResolution_remove_unused_filters;
		}
		return MarkerMessages.RemoveFilterProblemResolution_remove_unused_filter;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.markers.WorkbenchMarkerResolution#run(org.eclipse.core.resources.IMarker[], org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IMarker[] markers, IProgressMonitor monitor) {
		SubMonitor localmonitor = SubMonitor.convert(monitor, getLabel(), markers.length);
		try {
			HashSet pjs = new HashSet(markers.length);
			IApiProblemFilter filter = fFilter;
			IApiComponent component = null;
			for (int i = 0; i < markers.length; i++) {
				if(!fMarker.equals(markers[i])) {
					filter = ApiMarkerResolutionGenerator.resolveFilter(markers[i]);
				}
				if(filter == null) {
					localmonitor.worked(1);
					continue;
				}
				component = ApiBaselineManager.getManager().getWorkspaceBaseline().getApiComponent(filter.getComponentId());
				if(component instanceof PluginProjectApiComponent) {
					try {
						IApiFilterStore store = component.getFilterStore();
						store.removeFilters(new IApiProblemFilter[] {filter});
						IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(filter.getComponentId());
						if(project != null) {
							Util.touchCorrespondingResource(project, markers[i].getResource(), Util.getTypeNameFromMarker(markers[i]));
							pjs.add(project);
						}
					}
					catch(CoreException ce) {
						ApiPlugin.log(ce);
					}
				}
				localmonitor.worked(1);
			}
			//build affected projects
			if(pjs.size() > 0) {
				if(!ResourcesPlugin.getWorkspace().isAutoBuilding()) {
					IProject[] projects = (IProject[]) pjs.toArray(new IProject[pjs.size()]);
					Util.getBuildJob(projects, IncrementalProjectBuilder.INCREMENTAL_BUILD).schedule();
				}
			}
		}
		finally {
			localmonitor.done();
		}
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
				if(markers[i].getType() == IApiMarkerConstants.UNUSED_FILTER_PROBLEM_MARKER &&
						!fMarker.equals(markers[i])) {
					mset.add(markers[i]);
				}
			}
			catch(CoreException ce) {
				//ignore, just don't consider the marker
			}
		}
		int size = mset.size();
		plural = size > 0;
		return (IMarker[]) mset.toArray(new IMarker[size]);
	}
}
