/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.site;

import java.util.ArrayList;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class StateListener
	implements IResourceChangeListener, IResourceDeltaVisitor {
	private ArrayList dirtyProjects = new ArrayList();
	private ISiteModel model;
	
	private boolean active=false;
	
	public boolean isActive() {
		return active;
	}
	
	public void setActive(boolean active) {
		this.active = active;
	}

	public StateListener(ISiteModel siteModel) {
		this.model = siteModel;
		PDEPlugin.getWorkspace().addResourceChangeListener(
			this,
			IResourceChangeEvent.PRE_AUTO_BUILD);
	}

	public void resourceChanged(IResourceChangeEvent e) {
		if (e.getType() == IResourceChangeEvent.PRE_AUTO_BUILD) {
			try {
				e.getDelta().accept(this);
			} catch (CoreException ex) {
				PDEPlugin.logException(ex);
			}
		}
	}

	public void setBuilt(ISiteBuildFeature sbfeature) {
		IProject sbproject = getProject(sbfeature);
		if (sbproject != null)
			removeProject(sbproject);
	}

	public void removeProject(IProject project) {
		if (dirtyProjects.contains(project))
			dirtyProjects.remove(project);
	}

	public void removeAllProjects() {
		dirtyProjects.clear();
	}

	public boolean visit(IResourceDelta delta) throws CoreException {
		IResource resource = delta.getResource();

		if (resource instanceof IProject) {
			handleDirtyProject((IProject) resource);
			return false;
		}
		return true;
	}

	private void handleDirtyProject(IProject project) {
		IProject relatedProject = getRelatedProject(project);
		if (relatedProject!=null) {
			if (!dirtyProjects.contains(relatedProject))
				dirtyProjects.add(relatedProject);
		}
	}

	public boolean isDirty(ISiteBuildFeature sbfeature) {
		IProject sbproject = getProject(sbfeature);
		if (sbproject != null)
			return dirtyProjects.contains(sbproject);
		else
			return false;
	}
	private IProject getRelatedProject(IProject project) {
		ISiteBuildModel buildModel = model.getBuildModel();
		ISiteBuild siteBuild = buildModel.getSiteBuild();
		ISiteBuildFeature[] features = siteBuild.getFeatures();
		for (int i = 0; i < features.length; i++) {
			ISiteBuildFeature sbfeature = features[i];
			IProject sbproject = getProject(sbfeature);
			if (sbproject != null && sbproject.equals(project))
				return sbproject;
			if (isReferencedPluginProject(sbfeature, project))
				return sbproject;
		}
		return null;
	}

	private boolean isReferencedPluginProject(
		ISiteBuildFeature sbfeature,
		IProject project) {
		IFeature referencedFeature = sbfeature.getReferencedFeature();
		if (referencedFeature != null) {
			IFeaturePlugin[] plugins = referencedFeature.getPlugins();
			for (int i = 0; i < plugins.length; i++) {
				IFeaturePlugin plugin = plugins[i];
				String id = plugin.getId();
				String version = plugin.getVersion();
				boolean fragment = plugin.isFragment();
				IPluginModelBase model =
					PDECore.getDefault().getModelManager().findPlugin(
						id,
						version,
						IMatchRules.PERFECT);
				if (model != null && model.isFragmentModel() == fragment) {
					IResource resource = model.getUnderlyingResource();
					if (resource != null
						&& resource.getProject().equals(project))
						return true;
				}
			}
		}
		return false;
	}

	private IProject getProject(ISiteBuildFeature sbfeature) {
		IFeature referencedFeature = sbfeature.getReferencedFeature();
		if (referencedFeature != null) {
			IResource resource =
				referencedFeature.getModel().getUnderlyingResource();
			if (resource != null)
				return resource.getProject();
		}
		return null;
	}
	public void dispose() {
		PDEPlugin.getWorkspace().removeResourceChangeListener(this);
	}
}
