/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.service.resolver.BundleDelta;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateDelta;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.IStateDeltaListener;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.osgi.resource.Resource;

public class PluginRebuilder implements IStateDeltaListener, IResourceChangeListener {

	private final Set<String> fProjectNames = new HashSet<>();

	private boolean fTouchWorkspace = false;

	public void start() {
		PDECore.getDefault().getModelManager().addStateDeltaListener(this);
		JavaCore.addPreProcessingResourceChangedListener(this, IResourceChangeEvent.PRE_BUILD);
	}

	public void stop() {
		PDECore.getDefault().getModelManager().removeStateDeltaListener(this);
		JavaCore.removePreProcessingResourceChangedListener(this);
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		if (event.getType() == IResourceChangeEvent.PRE_BUILD) {
			IWorkspaceRoot root = PDECore.getWorkspace().getRoot();
			if (fTouchWorkspace) {
				IProject[] projects = root.getProjects();
				for (IProject project : projects) {
					touchProject(project);
				}
			} else {
				Iterator<String> iter = fProjectNames.iterator();
				while (iter.hasNext()) {
					touchProject(root.getProject(iter.next()));
				}
			}
			fTouchWorkspace = false;
			fProjectNames.clear();
		}
	}

	private void touchProject(IProject project) {
		if (WorkspaceModelManager.isPluginProject(project) && !WorkspaceModelManager.isBinaryProject(project)) {
			try {
				// set session property on project
				// to be read and reset in ManifestConsistencyChecker
				project.setSessionProperty(PDECore.TOUCH_PROJECT, Boolean.TRUE);
				// touch project so that ManifestConsistencyChecker#build(..) gets invoked
				project.touch(new NullProgressMonitor());
			} catch (CoreException e) {
				PDECore.log(e);
			}
		}
	}

	@Override
	public void stateChanged(State newState) {
		fTouchWorkspace = true;
		fProjectNames.clear();
	}

	@Override
	public void stateResolved(StateDelta delta) {
		if (delta == null) {
			// if delta is null, then target has changed
			// prepare all projects for "touching"
			fTouchWorkspace = true;
			fProjectNames.clear();
		} else {
			BundleDelta[] deltas = delta.getChanges();
			for (BundleDelta bundleDelta : deltas) {
				// only interested in workspace plug-ins that are affected by delta
				// but not those who have caused it.
				int type = bundleDelta.getType();
				if ((type & BundleDelta.UPDATED) == BundleDelta.UPDATED || (type & BundleDelta.ADDED) == BundleDelta.ADDED || (type & BundleDelta.REMOVED) == BundleDelta.REMOVED) {
					continue;
				}

				IPluginModelBase model = PluginRegistry.findModel((Resource) bundleDelta.getBundle());
				IResource resource = model == null ? null : model.getUnderlyingResource();
				if (resource != null) {
					fProjectNames.add(resource.getProject().getName());
				}
			}
		}
	}

}
