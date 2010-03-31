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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.ui.PlatformUI;
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

	private IMarker[] fMarkers = null;
	private boolean fAddingComment = false;
	
	/**
	 * Constructor
	 * @param element the element to create the filter for (method, field, class, enum, etc)
	 * @param kind the kind of filter to create
	 * 
	 * @see IApiProblemFilter#getKinds()
	 */
	public CreateApiFilterOperation(IMarker[] markers, boolean addingcomments) {
		super(MarkerMessages.CreateApiFilterOperation_0);
		fMarkers = markers;
		this.fAddingComment = addingcomments;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.progress.UIJob#runInUIThread(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus runInUIThread(IProgressMonitor monitor) {
		try {
			HashMap map = new HashMap(fMarkers.length);
			IResource resource = null;
			IProject project = null;
			String comment = null;
			HashSet projects = new HashSet();
			if(fAddingComment) {
				InputDialog dialog = new InputDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), 
						MarkerMessages.CreateApiFilterOperation_add_filter_comment, 
						MarkerMessages.CreateApiFilterOperation_filter_comment, 
						null, 
						null);
				if(dialog.open() == IDialogConstants.OK_ID) {
					comment = dialog.getValue();
					if(comment != null && comment.length() < 1) {
						comment = null;
					}
				}
			}
			IMarker marker = null;
			IApiProblem problem = null;
			HashSet filters = null;
			IApiComponent component = null;
			for (int i = 0; i < fMarkers.length; i++) {
				marker = fMarkers[i];
				resource = marker.getResource();
				project = resource.getProject();
				if(project == null) {
					return Status.CANCEL_STATUS;
				}
				component = ApiPlugin.getDefault().getApiBaselineManager().getWorkspaceBaseline().getApiComponent(project);
				if(component == null) {
					return Status.CANCEL_STATUS;
				}
				projects.add(project);
				filters = (HashSet) map.get(component);
				if(filters == null) {
					filters = new HashSet();
					map.put(component, filters);
				}
				String typeNameFromMarker = Util.getTypeNameFromMarker(marker);
				problem = ApiProblemFactory.newApiProblem(resource.getProjectRelativePath().toPortableString(),
						typeNameFromMarker,
						getMessageArgumentsFromMarker(marker), 
						null,
						null,
						marker.getAttribute(IMarker.LINE_NUMBER, -1), 
						marker.getAttribute(IMarker.CHAR_START, -1),
						marker.getAttribute(IMarker.CHAR_END, -1), 
						marker.getAttribute(IApiMarkerConstants.MARKER_ATTR_PROBLEM_ID, 0));
				filters.add(ApiProblemFactory.newProblemFilter(component.getSymbolicName(), problem, comment));
				Util.touchCorrespondingResource(project, resource, typeNameFromMarker);
			}
			for (Iterator iter = map.keySet().iterator(); iter.hasNext();) {
				component = (IApiComponent) iter.next();
				IApiFilterStore store = component.getFilterStore();
				filters = (HashSet) map.get(component);
				if(filters == null) {
					continue;
				}
				store.addFilters((IApiProblemFilter[]) filters.toArray(new IApiProblemFilter[filters.size()]));
			}
			if(!ResourcesPlugin.getWorkspace().isAutoBuilding()) {
				Util.getBuildJob((IProject[]) projects.toArray(new IProject[projects.size()]), IncrementalProjectBuilder.INCREMENTAL_BUILD).schedule();
			}
			return Status.OK_STATUS;
		}
		catch(CoreException ce) {
			ApiUIPlugin.log(ce);
		}
		return Status.CANCEL_STATUS;
	}
	
	/**
	 * @return the listing of message arguments from the marker.
	 */
	private String[] getMessageArgumentsFromMarker(IMarker marker) {
		ArrayList args = new ArrayList();
		String arguments = marker.getAttribute(IApiMarkerConstants.MARKER_ATTR_MESSAGE_ARGUMENTS, null);
		if(arguments != null) {
			return arguments.split("#"); //$NON-NLS-1$
		}
		return (String[]) args.toArray(new String[args.size()]);
	}
}
