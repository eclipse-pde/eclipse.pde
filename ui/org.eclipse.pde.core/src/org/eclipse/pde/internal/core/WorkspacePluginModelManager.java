/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.core.IModelProviderEvent;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ISharedExtensionsModel;
import org.eclipse.pde.internal.core.bundle.BundleFragmentModel;
import org.eclipse.pde.internal.core.bundle.BundlePluginModel;
import org.eclipse.pde.internal.core.bundle.WorkspaceBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.plugin.WorkspaceExtensionsModel;
import org.eclipse.pde.internal.core.plugin.WorkspaceFragmentModel;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModel;

public class WorkspacePluginModelManager extends WorkspaceModelManager {

	protected boolean isInterestingProject(IProject project) {
		return isPluginProject(project);
	}

	protected void createModel(IProject project, boolean notify) {
		IPluginModelBase model = null;
		if (project.exists(ICoreConstants.MANIFEST_PATH)) {
			WorkspaceBundleModel bmodel = new WorkspaceBundleModel(project.getFile(ICoreConstants.MANIFEST_PATH));
			loadModel(bmodel, false);		
			if (bmodel.isFragmentModel())
				model = new BundleFragmentModel();
			else
				model = new BundlePluginModel();
			model.setEnabled(true);
			((IBundlePluginModelBase)model).setBundleModel(bmodel);
			
			IFile efile = project.getFile(bmodel.isFragmentModel() 
							? ICoreConstants.FRAGMENT_PATH : ICoreConstants.PLUGIN_PATH); 
			if (efile.exists()) {
				WorkspaceExtensionsModel extModel = new WorkspaceExtensionsModel(efile);
				loadModel(extModel, false);
				((IBundlePluginModelBase)model).setExtensionsModel(extModel);
				extModel.setBundleModel((IBundlePluginModelBase)model);
			}
			
		} else if (project.exists(ICoreConstants.PLUGIN_PATH)) {
			model = new WorkspacePluginModel(project.getFile(ICoreConstants.PLUGIN_PATH), true);
			loadModel(model, false);
		} else if (project.exists(ICoreConstants.FRAGMENT_PATH)) {
			model = new WorkspaceFragmentModel(project.getFile(ICoreConstants.FRAGMENT_PATH), true);
			loadModel(model, false);
		}
		
		if (project.getFile(".options").exists()) //$NON-NLS-1$
			PDECore.getDefault().getTracingOptionsManager().reset();

		if (model != null) {
			if (fModels == null) 
				fModels = new HashMap();
			fModels.put(project, model);
			if (notify)
				addChange(model, IModelProviderEvent.MODELS_ADDED);
		}
	}
	
	protected void handleFileDelta(IResourceDelta delta) {
		IFile file = (IFile)delta.getResource();
		String filename = file.getName();
		if (filename.equals(".options")) { //$NON-NLS-1$
			PDECore.getDefault().getTracingOptionsManager().reset();
		} else if (filename.equals("build.properties")) { //$NON-NLS-1$
			Object model = getModel(file.getProject());
			if (model != null)
				addChange(model, IModelProviderEvent.MODELS_CHANGED);
		} else if (file.getProjectRelativePath().equals(ICoreConstants.PLUGIN_PATH)
					|| file.getProjectRelativePath().equals(ICoreConstants.FRAGMENT_PATH)){
			handleExtensionFileDelta(file, delta);
		} else if (file.getProjectRelativePath().equals(ICoreConstants.MANIFEST_PATH)) {
			handleBundleManifestDelta(file, delta);
		}
	}
	
	private void handleExtensionFileDelta(IFile file, IResourceDelta delta) {
		int kind = delta.getKind();
		IPluginModelBase model = (IPluginModelBase)getModel(file.getProject());
		if (kind == IResourceDelta.REMOVED) {
			if (model instanceof IBundlePluginModelBase) {
				((IBundlePluginModelBase)model).setExtensionsModel(null);
			} else {
				removeModel(file.getProject());
			}
		} else if (kind == IResourceDelta.ADDED) {
			if (model instanceof IBundlePluginModelBase){
				WorkspaceExtensionsModel extensions = new WorkspaceExtensionsModel(file);
				((IBundlePluginModelBase)model).setExtensionsModel(extensions);
				extensions.setBundleModel((IBundlePluginModelBase)model);
				loadModel(extensions, false);				
			} else {
				createModel(file.getProject(), true);
			}
		} else if (kind == IResourceDelta.CHANGED 
				    && (IResourceDelta.CONTENT & delta.getFlags()) != 0) {
			if (model instanceof IBundlePluginModelBase) {
				ISharedExtensionsModel extensions = ((IBundlePluginModelBase)model).getExtensionsModel();
				boolean reload = extensions != null;
				if (extensions == null) {
					extensions = new WorkspaceExtensionsModel(file);
					((IBundlePluginModelBase)model).setExtensionsModel(extensions);
					((WorkspaceExtensionsModel)extensions).setBundleModel((IBundlePluginModelBase)model);
				}
				loadModel(extensions, reload);				
			} else if (model != null) {
				loadModel(model, true);
				addChange(model, IModelProviderEvent.MODELS_CHANGED);
			}
		}
	}
	
	private void handleBundleManifestDelta(IFile file, IResourceDelta delta) {
		int kind = delta.getKind();
		IProject project = file.getProject();
		Object model = getModel(project);
		if (kind == IResourceDelta.REMOVED && model != null) {
			removeModel(project);
			// switch to legacy plugin structure, if applicable
			createModel(project, true);		
		} else if (kind == IResourceDelta.ADDED || model == null) {
			createModel(project, true);
		} else if (kind == IResourceDelta.CHANGED 
				    && (IResourceDelta.CONTENT & delta.getFlags()) != 0) {
			if (model instanceof IBundlePluginModelBase) {
				// check to see if localization changed (bug 146912)
				String oldLocalization = ((IBundlePluginModelBase)model).getBundleLocalization();
				loadModel(((IBundlePluginModelBase)model).getBundleModel(), true);
				String newLocalization = ((IBundlePluginModelBase)model).getBundleLocalization();
				if (model instanceof AbstractNLModel && 
						(oldLocalization != null && (newLocalization == null || !oldLocalization.equals(newLocalization))) ||
						(newLocalization != null && (oldLocalization == null || !newLocalization.equals(oldLocalization))))
					((AbstractNLModel)model).resetNLResourceHelper();
				
				addChange(model, IModelProviderEvent.MODELS_CHANGED);
			} 
		}		
	}
	
	protected Object removeModel(IProject project) {
		Object model = super.removeModel(project);
		if (model != null && project.exists(new Path(".options"))) //$NON-NLS-1$
			PDECore.getDefault().getTracingOptionsManager().reset();
		return model;
	}
	
	protected IPluginModelBase getPluginModel(IProject project) {
		return (IPluginModelBase)getModel(project);
	}
	
	protected IPluginModelBase[] getPluginModels() {
		initialize();
		return (IPluginModelBase[])fModels.values().toArray(new IPluginModelBase[fModels.size()]);
	}
	
	protected void addListeners() {
		IWorkspace workspace = PDECore.getWorkspace();
		workspace.addResourceChangeListener(this, IResourceChangeEvent.PRE_CLOSE);
		JavaCore.addPreProcessingResourceChangedListener(this, IResourceChangeEvent.POST_CHANGE);
	}

	protected void removeListeners() {
		PDECore.getWorkspace().removeResourceChangeListener(this);
		JavaCore.removePreProcessingResourceChangedListener(this);
		super.removeListeners();
	}
	
	protected boolean isInterestingFolder(IFolder folder) {
		return folder.getName().equals("META-INF") && folder.getParent() instanceof IProject; //$NON-NLS-1$;
	}
	
	protected void initializeModels(IPluginModelBase[] models) {
		fModels = Collections.synchronizedMap(new HashMap());		
		for (int i = 0; i < models.length; i++) {
			IProject project = models[i].getUnderlyingResource().getProject();
			fModels.put(project, models[i]);
		}
		addListeners();
	}
	
	protected URL[] getPluginPaths() {
		ArrayList list = new ArrayList();
		IProject[] projects = PDECore.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projects.length; i++) {
			if (isPluginProject(projects[i])) {			
				try {
					IPath path = projects[i].getLocation();
					if (path != null) {
						list.add(path.toFile().toURL());
					}
				} catch (MalformedURLException e) {
				}
			}
		}
		return (URL[])list.toArray(new URL[list.size()]);
	}
	
}
