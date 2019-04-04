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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

/**
 * Operation for creating a new API problem filter
 *
 * @see IApiProblem
 * @see IApiProblemFilter
 * @see IApiFilterStore
 *
 * @since 1.0.0
 */
public class CreateApiFilterOperation extends Job {

	private IMarker[] fMarkers = null;
	private boolean fAddingComment = false;
	String comment = null;
	boolean cancel = false;

	/**
	 * Constructor
	 *
	 * @param element the element to create the filter for (method, field,
	 *            class, enum, etc)
	 * @param kind the kind of filter to create
	 *
	 * @see IApiProblemFilter#getKinds()
	 */
	public CreateApiFilterOperation(IMarker[] markers, boolean addingcomments) {
		super(MarkerMessages.CreateApiFilterOperation_0);
		fMarkers = markers;
		this.fAddingComment = addingcomments;
	}

	public static Display getDisplay() {
		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}
		return display;
	}
	@Override
	public IStatus run(IProgressMonitor monitor) {
		try {
			HashMap<IApiComponent, HashSet<IApiProblemFilter>> map = new HashMap<>(fMarkers.length);
			IResource resource = null;
			IProject project = null;

			HashSet<IProject> projects = new HashSet<>();

			if (fAddingComment) {
				getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
						InputDialog dialog = new InputDialog(
								PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
								MarkerMessages.CreateApiFilterOperation_add_filter_comment,
								MarkerMessages.CreateApiFilterOperation_filter_comment, null, null);
						int okCancel = dialog.open();
						if (okCancel == IDialogConstants.OK_ID) {
							comment = dialog.getValue();
							if (comment != null && comment.length() < 1) {
								comment = null;
							}
						}
						if (okCancel == IDialogConstants.CANCEL_ID) {
							cancel = true;
						}
					}
				});

			}

			if (cancel) {
				return Status.CANCEL_STATUS;
			}

			IMarker marker = null;
			IApiProblem problem = null;
			HashSet<IApiProblemFilter> filters = null;
			IApiComponent component = null;
			for (IMarker fMarker : fMarkers) {
				marker = fMarker;
				resource = marker.getResource();
				project = resource.getProject();
				if (project == null) {
					return Status.CANCEL_STATUS;
				}
				component = ApiPlugin.getDefault().getApiBaselineManager().getWorkspaceBaseline().getApiComponent(project);
				if (component == null) {
					return Status.CANCEL_STATUS;
				}
				projects.add(project);
				filters = map.get(component);
				if (filters == null) {
					filters = new HashSet<>();
					map.put(component, filters);
				}
				String typeNameFromMarker = Util.getTypeNameFromMarker(marker);
				problem = ApiProblemFactory.newApiProblem(resource.getProjectRelativePath().toPortableString(), typeNameFromMarker, getMessageArgumentsFromMarker(marker), null, null, marker.getAttribute(IMarker.LINE_NUMBER, -1), marker.getAttribute(IMarker.CHAR_START, -1), marker.getAttribute(IMarker.CHAR_END, -1), marker.getAttribute(IApiMarkerConstants.MARKER_ATTR_PROBLEM_ID, 0));
				filters.add(ApiProblemFactory.newProblemFilter(component.getSymbolicName(), problem, comment));
				Util.touchCorrespondingResource(project, resource, typeNameFromMarker);
			}
			for (Entry<IApiComponent, HashSet<IApiProblemFilter>> entry : map.entrySet()) {
				component = entry.getKey();
				IApiFilterStore store = component.getFilterStore();
				filters = entry.getValue();
				if (filters == null) {
					continue;
				}
				store.addFilters(filters.toArray(new IApiProblemFilter[filters.size()]));
			}
			if (!ResourcesPlugin.getWorkspace().isAutoBuilding()) {
				Util.getBuildJob(projects.toArray(new IProject[projects.size()]), IncrementalProjectBuilder.INCREMENTAL_BUILD).schedule();
			}
			return Status.OK_STATUS;
		} catch (CoreException ce) {
			ApiUIPlugin.log(ce);
		}
		return Status.CANCEL_STATUS;
	}

	/**
	 * @return the listing of message arguments from the marker.
	 */
	private String[] getMessageArgumentsFromMarker(IMarker marker) {
		ArrayList<String> args = new ArrayList<>();
		String arguments = marker.getAttribute(IApiMarkerConstants.MARKER_ATTR_MESSAGE_ARGUMENTS, null);
		if (arguments != null) {
			return arguments.split("#"); //$NON-NLS-1$
		}
		return args.toArray(new String[args.size()]);
	}
}
