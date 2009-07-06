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

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.ui.internal.preferences.PreferenceMessages;
import org.eclipse.ui.progress.UIJob;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Operation for setting "resolution errors" problem to ignore.
 * 
 * @see IApiProblem
 * @see IApiProblemFilter
 * @see IApiFilterStore
 * 
 * @since 1.1
 */
public class UpdateProjectSettingsOperation extends UIJob {

	private IMarker fBackingMarker = null;
	
	/**
	 * Constructor
	 * @param element the element to create the filter for (method, field, class, enum, etc)
	 * @param kind the kind of filter to create
	 * 
	 * @see IApiProblemFilter#getKinds()
	 */
	public UpdateProjectSettingsOperation(IMarker marker) {
		super(NLS.bind(MarkerMessages.UpdateProjectSettingsOperation_0, PreferenceMessages.ReportApiComponentResolutionFailureDescription));
		fBackingMarker = marker;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.progress.UIJob#runInUIThread(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus runInUIThread(IProgressMonitor monitor) {
		boolean notNull = monitor != null;
		if (notNull && monitor.isCanceled()) return Status.CANCEL_STATUS;
		if (notNull) {
			monitor.beginTask(
					NLS.bind(MarkerMessages.UpdateProjectSettingsOperation_title, PreferenceMessages.ReportApiComponentResolutionFailureDescription),
					3);
		}
		try{
			if (notNull) {
				monitor.worked(1);
			}
			IResource resource = fBackingMarker.getResource();
			IProject project = resource.getProject();
			if(project == null) {
				return Status.CANCEL_STATUS;
			}
			IApiComponent component = ApiPlugin.getDefault().getApiBaselineManager().getWorkspaceBaseline().getApiComponent(project.getName());
			if(component == null) {
				return Status.CANCEL_STATUS;
			}
			IEclipsePreferences inode = new ProjectScope(project).getNode(ApiPlugin.PLUGIN_ID);
			inode.put(IApiProblemTypes.REPORT_RESOLUTION_ERRORS_API_COMPONENT, ApiPlugin.VALUE_IGNORE);
			try {
				inode.flush();
			} catch (BackingStoreException e) {
				ApiPlugin.log(e);
			}
			if (notNull) {
				monitor.worked(1);
			}
			Util.getBuildJob(new IProject[] {project}).schedule();
			if (notNull) {
				monitor.worked(1);
			}
		} finally {
			if (notNull) {
				monitor.done();
			}
		}		
		return Status.OK_STATUS;
	}
}
