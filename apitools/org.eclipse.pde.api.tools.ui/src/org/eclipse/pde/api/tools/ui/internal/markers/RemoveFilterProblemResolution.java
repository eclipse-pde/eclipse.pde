/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.ApiBaselineManager;
import org.eclipse.pde.api.tools.internal.model.ProjectComponent;
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
 * Resolution that removes the selected
 * {@link org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter}
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
	 *
	 * @param filter the original associated problem filter
	 * @param marker the original marker this quick fix was opened on
	 */
	public RemoveFilterProblemResolution(IApiProblemFilter filter, IMarker marker) {
		fFilter = filter;
		fMarker = marker;
	}

	@Override
	public String getDescription() {
		return NLS.bind(MarkerMessages.RemoveFilterProblemResolution_removes_selected_problem_filter, MarkerUtilities.getMessage(fMarker));
	}

	@Override
	public Image getImage() {
		return ApiUIPlugin.getSharedImage(IApiToolsConstants.IMG_ELCL_REMOVE);
	}

	@Override
	public String getLabel() {
		if (plural) {
			return MarkerMessages.RemoveFilterProblemResolution_remove_unused_filters;
		}
		return MarkerMessages.RemoveFilterProblemResolution_remove_unused_filter;
	}

	@Override
	public void run(IMarker[] markers, IProgressMonitor monitor) {
		SubMonitor localmonitor = SubMonitor.convert(monitor, getLabel(), (markers.length * 2) + 1);
		try {
			IApiProblemFilter filter = fFilter;
			IApiComponent component = null;
			HashMap<IApiComponent, Set<IApiProblemFilter>> map = new HashMap<>();
			Set<IApiProblemFilter> filters = null;
			HashSet<IResource> resources = new HashSet<>(markers.length);
			IResource resource = null;
			// collate the filters by IApiComponent
			for (IMarker marker : markers) {
				localmonitor.split(1);
				filter = ApiMarkerResolutionGenerator.resolveFilter(marker);
				if (filter == null) {
					continue;
				}
				resource = marker.getResource();
				component = ApiBaselineManager.getManager().getWorkspaceBaseline().getApiComponent(resource.getProject());
				if (component instanceof ProjectComponent) {
					filters = map.get(component);
					if (filters == null) {
						filters = new HashSet<>();
						map.put(component, filters);
					}
					filters.add(filter);
					resources.add(resource);
				}
			}
			// batch remove the filters
			localmonitor.setWorkRemaining(map.size());
			for (Entry<IApiComponent, Set<IApiProblemFilter>> entry : map.entrySet()) {
				try {
					component = entry.getKey();
					filters = entry.getValue();
					IApiFilterStore store = component.getFilterStore();
					store.removeFilters(filters.toArray(new IApiProblemFilter[filters.size()]));
				} catch (CoreException ce) {
					ApiPlugin.log(ce);
				}
				localmonitor.split(1);
			}
			// touch resources to mark them as needing build
			HashSet<IProject> pjs = new HashSet<>();
			for (Iterator<IResource> iter = resources.iterator(); iter.hasNext();) {
				try {
					resource = iter.next();
					pjs.add(resource.getProject());
					(resource).touch(localmonitor.split(1));
				} catch (CoreException ce) {
				}
			}
			if (pjs.size() > 0) {
				if (!ResourcesPlugin.getWorkspace().isAutoBuilding()) {
					IProject[] projects = pjs.toArray(new IProject[map.size()]);
					Util.getBuildJob(projects, IncrementalProjectBuilder.INCREMENTAL_BUILD).schedule();
				}
			}
		} finally {
			localmonitor.done();
		}
	}

	@Override
	public void run(IMarker marker) {
		run(new IMarker[] { marker }, null);
	}

	@Override
	public IMarker[] findOtherMarkers(IMarker[] markers) {
		HashSet<IMarker> mset = new HashSet<>(markers.length);
		for (int i = 0; i < markers.length; i++) {
			try {
				if (markers[i].getType().equals(IApiMarkerConstants.UNUSED_FILTER_PROBLEM_MARKER) && !fMarker.equals(markers[i])) {
					mset.add(markers[i]);
				}
			} catch (CoreException ce) {
				// ignore, just don't consider the marker
			}
		}
		int size = mset.size();
		plural = size > 0;
		return mset.toArray(new IMarker[size]);
	}
}
