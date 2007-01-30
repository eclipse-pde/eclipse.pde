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

	/**
	 * The workspace plug-in model manager is only interested
	 * in changes to plug-in projects.
	 */
	protected boolean isInterestingProject(IProject project) {
		return isPluginProject(project);
	}

	/**
	 * Creates a plug-in model based on the project structure.
	 * <p>
	 * A bundle model is created if the project has a MANIFEST.MF file and optionally 
	 * a plugin.xml/fragment.xml file.
	 * </p>
	 * <p>
	 * An old-style plugin model is created if the project only has a plugin.xml/fragment.xml
	 * file.
	 * </p>
	 */
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
	
	/**
	 * Reacts to changes in files of interest to PDE
	 */
	protected void handleFileDelta(IResourceDelta delta) {
		IFile file = (IFile)delta.getResource();
		String filename = file.getName();
		if (filename.equals(".options")) { //$NON-NLS-1$
			PDECore.getDefault().getTracingOptionsManager().reset();
		} else if (filename.endsWith(".properties")) {	 //$NON-NLS-1$
			// change in build.properties should trigger a Classpath Update
			// we therefore fire a notification
			//TODO this is inefficient.  we could do better.
			 if (filename.equals("build.properties")) { //$NON-NLS-1$
				Object model = getModel(file.getProject());
				if (model != null)
					addChange(model, IModelProviderEvent.MODELS_CHANGED);
			 } else {
				 // reset bundle resource if localization file has changed.
				 IPluginModelBase model = getPluginModel(file.getProject());
				 String localization = null;
				 if (model instanceof IBundlePluginModelBase) {
					localization = ((IBundlePluginModelBase)model).getBundleLocalization();			 
				 } else if (model != null) {
					 localization = "plugin"; //$NON-NLS-1$
				 }
				 if (localization != null && filename.startsWith(localization)) {
					((AbstractNLModel)model).resetNLResourceHelper();					 
				 }
			 }
		} else {
			IPath path = file.getProjectRelativePath();
			if (path.equals(ICoreConstants.PLUGIN_PATH) 
					|| path.equals(ICoreConstants.FRAGMENT_PATH)){
				handleExtensionFileDelta(file, delta);
			} else if (path.equals(ICoreConstants.MANIFEST_PATH)) {
				handleBundleManifestDelta(file, delta);
			}
		}
	}
	
	/**
	 * Reacts to changes in the plugin.xml or fragment.xml file.
	 * <ul>
	 * <li>If the file has been deleted and the project has a MANIFEST.MF file,
	 * then this deletion only affects extensions and extension points.</li>
	 * <li>If the file has been deleted and the project does not have a MANIFEST.MF file,
	 * then it's an old-style plug-in and the entire model must be removed from the table.</li>
	 * <li>If the file has been added and the project already has a MANIFEST.MF, then
	 * this file only contributes extensions and extensions.  No need to send a notification
	 * to trigger update classpath of dependent plug-ins</li>
	 * <li>If the file has been added and the project does not have a MANIFEST.MF, then
	 * an old-style plug-in has been created.</li>
	 * <li>If the file has been modified and the project already has a MANIFEST.MF,
	 * then reload the extensions model but do not send out notifications</li>
	 * </li>If the file has been modified and the project has no MANIFEST.MF, then
	 * it's an old-style plug-in, reload and send out notifications to trigger a classpath update
	 * for dependent plug-ins</li>
	 * </ul>
	 * @param file the manifest file
	 * @param delta the resource delta
	 */
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
	
	/**
	 * Reacts to changes in the MANIFEST.MF file.
	 * <ul>
	 * <li>If the file has been deleted, switch to the old-style plug-in if a plugin.xml file exists</li>
	 * <li>If the file has been added, create a new bundle model</li>
	 * <li>If the file has been modified, reload the model, reset the resource bundle
	 * if the localization has changed and fire a notification that the model has changed</li>
	 * </ul>
	 * 
	 * @param file the manifest file that was modified
	 * @param delta the resource delta
	 */
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
	
	/**
	 * Removes the model associated with the given project from the table,
	 * if the given project is a plug-in project
	 */
	protected Object removeModel(IProject project) {
		Object model = super.removeModel(project);
		if (model != null && project.exists(new Path(".options"))) //$NON-NLS-1$
			PDECore.getDefault().getTracingOptionsManager().reset();
		return model;
	}
	
	/**
	 * Returns a plug-in model associated with the given project, or <code>null</code>
	 * if the project is not a plug-in project or the manifest file is missing vital data
	 * such as a symbolic name or version
	 * 
	 * @param project the given project
	 * 
	 * @return a plug-in model associated with the given project or <code>null</code>
	 * if no such valid model exists
	 */
	protected IPluginModelBase getPluginModel(IProject project) {
		return (IPluginModelBase)getModel(project);
	}
	
	/**
	 * Returns a list of all workspace plug-in models
	 * 
	 * @return an array of workspace plug-in models
	 */
	protected IPluginModelBase[] getPluginModels() {
		initialize();
		return (IPluginModelBase[])fModels.values().toArray(new IPluginModelBase[fModels.size()]);
	}
	
	/**
	 * Adds listeners to the workspace and to the java model
	 * to be notified of PRE_CLOSE events and POST_CHANGE events.
	 */
	protected void addListeners() {
		IWorkspace workspace = PDECore.getWorkspace();
		workspace.addResourceChangeListener(this, IResourceChangeEvent.PRE_CLOSE);
		// PDE must process the POST_CHANGE events before the Java model
		// for the PDE container classpath update to proceed smoothly
		JavaCore.addPreProcessingResourceChangedListener(this, IResourceChangeEvent.POST_CHANGE);
	}

	/**
	 * Removes listeners that the model manager attached on others, 
	 * as well as listeners attached on the model manager
	 */
	protected void removeListeners() {
		PDECore.getWorkspace().removeResourceChangeListener(this);
		JavaCore.removePreProcessingResourceChangedListener(this);
		super.removeListeners();
	}
	
	/**
	 * Returns true if the folder being visited is of interest to PDE.
	 * In this case, PDE is only interested in META-INF folders at the root of a plug-in project
	 * 
	 * @return <code>true</code> if the folder (and its children) is of interest to PDE;
	 * <code>false</code> otherwise.
	 * 
	 */
	protected boolean isInterestingFolder(IFolder folder) {
		return folder.getName().equals("META-INF") && folder.getParent() instanceof IProject; //$NON-NLS-1$;
	}
	
	/**
	 * This method is called when workspace models are read and initialized
	 * from the cache.  No need to read the workspace plug-ins from scratch.
	 * 
	 * @param models  the workspace plug-in models
	 */
	protected void initializeModels(IPluginModelBase[] models) {
		fModels = Collections.synchronizedMap(new HashMap());		
		for (int i = 0; i < models.length; i++) {
			IProject project = models[i].getUnderlyingResource().getProject();
			fModels.put(project, models[i]);
		}
		addListeners();
	}
	
	/**
	 * Return URLs to projects in the workspace that have a manifest file (MANIFEST.MF
	 * or plugin.xml)
	 * 
	 * @return an array of URLs to workspace plug-ins
	 */
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
