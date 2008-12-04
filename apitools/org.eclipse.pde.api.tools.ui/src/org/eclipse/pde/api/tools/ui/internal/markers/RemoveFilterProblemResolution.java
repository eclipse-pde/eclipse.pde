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
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.ApiBaselineManager;
import org.eclipse.pde.api.tools.internal.model.PluginProjectApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.progress.UIJob;

/**
 * Resolution that removes the selected {@link org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter}
 * 
 * @since 1.0.0
 */
public class RemoveFilterProblemResolution implements IMarkerResolution2 {

	/**
	 * The {@link IApiProblemFilter} to remove
	 */
	private IApiProblemFilter fFilter = null;
	
	/**
	 * Constructor
	 * @param filter
	 */
	public RemoveFilterProblemResolution(IApiProblemFilter filter) {
		fFilter = filter;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution2#getDescription()
	 */
	public String getDescription() {
		return NLS.bind(MarkerMessages.RemoveFilterProblemResolution_removes_selected_problem_filter, fFilter.getUnderlyingProblem().getMessage());
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
		return MarkerMessages.RemoveFilterProblemResolution_remove_unused_filter;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution#run(org.eclipse.core.resources.IMarker)
	 */
	public void run(IMarker marker) {
		final IMarker markerref = marker;
		UIJob job = new UIJob(MarkerMessages.RemoveFilterProblemResolution_remove_unused_filter_job_name) {
			public IStatus runInUIThread(IProgressMonitor monitor) {
				IApiComponent component = ApiBaselineManager.getManager().getWorkspaceBaseline().getApiComponent(fFilter.getComponentId());
				if(component instanceof PluginProjectApiComponent) {
					try {
						IApiFilterStore store = component.getFilterStore();
						store.removeFilters(new IApiProblemFilter[] {fFilter});
						markerref.getResource().touch(monitor);
						if(!ResourcesPlugin.getWorkspace().isAutoBuilding()) {
							IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(fFilter.getComponentId());
							if(project != null) {
								Util.getBuildJob(new IProject[] {project}, IncrementalProjectBuilder.INCREMENTAL_BUILD).schedule();
							}
						}
					}
					catch(CoreException ce) {
						ApiPlugin.log(ce);
					}
				}
				return Status.OK_STATUS;
			};
		};
		job.setSystem(true);
		job.setPriority(Job.INTERACTIVE);
		job.schedule();
	}
}
