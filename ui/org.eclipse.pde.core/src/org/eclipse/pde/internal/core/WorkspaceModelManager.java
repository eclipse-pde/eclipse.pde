/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.bundle.*;
import org.eclipse.pde.internal.core.feature.*;
import org.eclipse.pde.internal.core.ibundle.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.team.core.*;

public class WorkspaceModelManager
		implements
			IResourceChangeListener,
			IResourceDeltaVisitor {
	
	class ModelChange {
		IModel model;
		int type;		
		public ModelChange(IModel model, boolean added) {
			this.model = model;
			this.type = added
					? IModelProviderEvent.MODELS_ADDED
					: IModelProviderEvent.MODELS_REMOVED;
		}
		public ModelChange(IModel model) {
			this.model = model;
			this.type = IModelProviderEvent.MODELS_CHANGED;
		}
		
		public boolean equals(Object obj) {
			if (obj instanceof ModelChange) {
				IProject project = ((ModelChange)obj).model.getUnderlyingResource().getProject();
				return model.getUnderlyingResource().getProject().equals(project);
			}
			return false;
		}
	}
	
	private Map fModels;
	private Map fFragmentModels;	
	private Map fFeatureModels;	
	private ArrayList fChangedModels;	
	private ArrayList fListeners = new ArrayList();
	private boolean fInitialized = false;
	private boolean fModelsLocked;
	
	public static boolean isPluginProject(IProject project) {
		if (project.isOpen())
			return hasBundleManifest(project) || hasPluginManifest(project)
			|| hasFragmentManifest(project);
		return false;
	}
	
	public static boolean hasBundleManifest(IProject project) {
		return project.exists(new Path("META-INF/MANIFEST.MF")); //$NON-NLS-1$
	}
	
	public static boolean hasPluginManifest(IProject project) {
		return project.exists(new Path("plugin.xml")); //$NON-NLS-1$
	}
	
	public static boolean hasFragmentManifest(IProject project) {
		return project.exists(new Path("fragment.xml")); //$NON-NLS-1$
	}
	
	public static boolean hasFeatureManifest(IProject project) {
		return project.exists(new Path("feature.xml")); //$NON-NLS-1$
	}
	
	public static boolean isFeatureProject(IProject project) {
		if (project.isOpen())
			return project.exists(new Path("feature.xml")); //$NON-NLS-1$
		return false;
	}
	
	public static boolean isBinaryFeatureProject(IProject project) {
		if (isFeatureProject(project)){
			try {
				String binary = project.getPersistentProperty(PDECore.EXTERNAL_PROJECT_PROPERTY);
				if (binary != null) {
					RepositoryProvider provider = RepositoryProvider.getProvider(project);
					return provider==null || provider instanceof BinaryRepositoryProvider;
				}
			} catch (CoreException e) {
				PDECore.logException(e);
			}
		}
		return false;
	}
	
	public static boolean isBinaryPluginProject(IProject project) {
		if (isPluginProject(project)){
			try {
				String binary = project.getPersistentProperty(PDECore.EXTERNAL_PROJECT_PROPERTY);
				if (binary != null) {
					RepositoryProvider provider = RepositoryProvider.getProvider(project);
					return provider==null || provider instanceof BinaryRepositoryProvider;
				}
			} catch (CoreException e) {
				PDECore.logException(e);
			}
		}
		return false;
	}
	
	public static boolean isNonBinaryPluginProject(IProject project) {
		return isPluginProject(project) && !isBinaryPluginProject(project);
	}
	
	public static boolean isJavaPluginProject(IProject project) {
		if (isPluginProject(project)) {
			try {
				if (project.hasNature(JavaCore.NATURE_ID))
					return true;
			} catch (CoreException e) {
			}			
		}
		return false;
	}
	
	public static boolean isUnsharedPluginProject(IProject project) {
		return RepositoryProvider.getProvider(project) == null
				|| isBinaryPluginProject(project);
	}
	
	public static URL[] getPluginPaths() {
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
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWorkspaceModelManager#getWorkspaceModel(org.eclipse.core.resources.IProject)
	 */
	private IModel getWorkspaceModel(IProject project) {
		initializeWorkspaceModels();
		
		if (hasFeatureManifest(project)){
			return (IModel)fFeatureModels.get(project);
		}
		
		if (hasBundleManifest(project)) {
			IModel model = (IModel)fModels.get(project);
			return (model != null) ? model : (IModel)fFragmentModels.get(project);
		}
		
		if (hasPluginManifest(project))
			return (IModel)fModels.get(project);
			
		if (hasFragmentManifest(project))
				return (IModel)fFragmentModels.get(project);
		
		return null;
	}
	
	protected IPluginModelBase getWorkspacePluginModel(IProject project) {
		IModel model = getWorkspaceModel(project);
		return (model instanceof IPluginModelBase) ? (IPluginModelBase)model : null;
	}
	
	public IFeatureModel getFeatureModel(IProject project) {
		IModel model = getWorkspaceModel(project);
		return (model instanceof IFeatureModel) ? (IFeatureModel)model : null;
	}
	
	private void handleFileDelta(IResourceDelta delta) {
		IFile file = (IFile)delta.getResource();
		if (file.getName().equals(".options")) { //$NON-NLS-1$
			PDECore.getDefault().getTracingOptionsManager().reset();
			return;
		}
		if (file.getName().equals("build.properties") && isPluginProject(file.getProject())) { //$NON-NLS-1$
			if (fChangedModels == null)
				fChangedModels = new ArrayList();
			IModel model = getWorkspaceModel(file.getProject());
			if (model != null) {
				ModelChange change = new ModelChange(model);
				if (!fChangedModels.contains(change))
					fChangedModels.add(change);
			}
			return;
		}
		
		if (file.getName().equals("plugin.properties")) { //$NON-NLS-1$
			IProject project = file.getProject();
			if (isPluginProject(project) && !isBinaryPluginProject(project)) {
				IPluginModelBase model = getWorkspacePluginModel(project);
				if (model != null && model instanceof AbstractModel) {
					((AbstractModel)model).resetNLResourceHelper();
				}
			}
		}
		
		if (file.getName().equals("feature.properties")) { //$NON-NLS-1$
			IProject project = file.getProject();
			if (isFeatureProject(project)) {
				IFeatureModel model = getFeatureModel(project);
				if (model != null && model instanceof AbstractModel) {
					((AbstractModel)model).resetNLResourceHelper();
				}
			}
		}
		
		if (!isSupportedFile(file))
			return;
		
		int kind = delta.getKind();
		switch (kind) {
			case IResourceDelta.REMOVED:
				handleFileRemoved(file);
				break;
			case IResourceDelta.CHANGED:
				handleFileChanged(file, delta);
				break;
			case IResourceDelta.ADDED:
				handleFileChanged(file, delta);
		}		
	}
	
	private void handleFileRemoved(IFile file) {
		IModel model = getWorkspaceModel(file);
		String fileName = file.getName().toLowerCase();
		if (model != null) {
			if (model instanceof IBundlePluginModelBase) {
				IBundlePluginModelBase bModel = (IBundlePluginModelBase)model;
				if (fileName.equals("plugin.xml") || fileName.equals("fragment.xml")) { //$NON-NLS-1$ //$NON-NLS-2$
					bModel.setExtensionsModel(null);
				} else {
					removeWorkspaceModel(file.getProject());
					if (bModel.getExtensionsModel() != null)
						switchToPluginMode(bModel);
				}					
			} else {
				removeWorkspaceModel(file.getProject());
			}
		}
	}
	
	private void switchToPluginMode(IBundlePluginModelBase bModel) {
		IPluginModelBase model = null;
		IProject project = bModel.getUnderlyingResource().getProject();
		if (bModel instanceof IBundlePluginModel) {
			model = createWorkspacePluginModel(project.getFile("plugin.xml")); //$NON-NLS-1$
		} else {
			model = createWorkspaceFragmentModel(project.getFile("fragment.xml")); //$NON-NLS-1$
		}
			
		if (model != null && model.getPluginBase().getId() != null) {
			if (model instanceof IPluginModel) {
				fModels.put(project, model);
			} else {
				fFragmentModels.put(project, model);
			}
			if (fChangedModels == null)
				fChangedModels = new ArrayList();
			fChangedModels.add(new ModelChange(model, true));
		}
	}
	
	private void handleFileChanged(IFile file, IResourceDelta delta) {
		IModel model = getWorkspaceModel(file);
		if (model == null
				|| (model instanceof WorkspacePluginModelBase && file.getName().equals("MANIFEST.MF"))) {
			addWorkspaceModel(file.getProject(), true);
			return;
		}
		if ((IResourceDelta.CONTENT & delta.getFlags()) != 0) {
			if (model instanceof IBundlePluginModelBase) {
				if (isBundleManifestFile(file)) {
					loadModel(((IBundlePluginModelBase)model).getBundleModel(), true);
				} else {
					ISharedExtensionsModel extensions = ((IBundlePluginModelBase)model).getExtensionsModel();
					if (extensions == null) {
						extensions = new WorkspaceExtensionsModel(file);
						((IBundlePluginModelBase)model).setExtensionsModel(extensions);
					}
					loadModel(extensions, true);
					// no need to fire any notifications if the plugin.xml/fragment.xml
					// of a bundle is modified.
					return;
				}
			} else {
				loadModel(model, true);
			}
			if (fChangedModels == null)
				fChangedModels = new ArrayList();
			ModelChange change = new ModelChange(model);
			if (!fChangedModels.contains(change))
				fChangedModels.add(change);
		}		
	}
	
	private boolean isSupportedFile(IFile file) {
		if (isBundleManifestFile(file))
			return true;
		String name = file.getName().toLowerCase();
		if (!name.equals("plugin.xml") && !name.equals("fragment.xml") //$NON-NLS-1$ //$NON-NLS-2$
				&& !name.equals("feature.xml")) //$NON-NLS-1$
			return false;
		IPath expectedPath = file.getProject().getFullPath().append(name);
		return expectedPath.equals(file.getFullPath());
	}

	
	private boolean isBundleManifestFile(IFile file) {
		IPath path = file.getProjectRelativePath();
		return (
			path.segmentCount() == 2
				&& path.segment(0).equals("META-INF") //$NON-NLS-1$
				&& path.segment(1).equals("MANIFEST.MF")); //$NON-NLS-1$
	}
	
	private IModel getWorkspaceModel(IFile file) {
		IProject project = file.getProject();
		if (isBundleManifestFile(file)) {
			IModel model = (IModel)fModels.get(project);
			return (model != null) ? model : (IModel)fFragmentModels.get(project);
		}		
		IPath path = file.getProjectRelativePath();
		if (path.equals(new Path("plugin.xml"))) //$NON-NLS-1$
			return (IModel)fModels.get(project);
		if (path.equals(new Path("fragment.xml"))) //$NON-NLS-1$
			return (IModel)fFragmentModels.get(project);
		if (path.equals(new Path("feature.xml"))) //$NON-NLS-1$
			return (IModel)fFeatureModels.get(project);
		return null;		
	}
	
	protected void initializeModels(IPluginModelBase[] models) {
		fModels = Collections.synchronizedMap(new HashMap());
		fFragmentModels = Collections.synchronizedMap(new HashMap());
		fFeatureModels = Collections.synchronizedMap(new HashMap());
		
		for (int i = 0; i < models.length; i++) {
			IProject project = models[i].getUnderlyingResource().getProject();
			if (models[i] instanceof IPluginModel) {
				fModels.put(project, models[i]);
			} else {
				fFragmentModels.put(project, models[i]);
			}
		}
				fFeatureModels = Collections.synchronizedMap(new HashMap());
		
		IWorkspace workspace = PDECore.getWorkspace();
		IProject[] projects = workspace.getRoot().getProjects();
		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			if (!isFeatureProject(project))
				continue;
			addWorkspaceModel(project, false);			
		}

		PDECore.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.PRE_CLOSE);
		JavaCore.addPreProcessingResourceChangedListener(this);
		fInitialized = true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelManager#getAllModels()
	 */
	protected IPluginModelBase[] getAllModels() {
		initializeWorkspaceModels();
		
		ArrayList result = new ArrayList();
		Iterator iter = fModels.values().iterator();
		while (iter.hasNext()) {
			result.add(iter.next());
		}
		
		iter = fFragmentModels.values().iterator();
		while (iter.hasNext()) {
			result.add(iter.next());
		}
		return (IPluginModelBase[]) result.toArray(
			new IPluginModelBase[result.size()]);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelProvider#removeModelProviderListener(org.eclipse.pde.core.IModelProviderListener)
	 */
	public void removeModelProviderListener(IModelProviderListener listener) {
		fListeners.remove(listener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelProvider#addModelProviderListener(org.eclipse.pde.core.IModelProviderListener)
	 */
	public void addModelProviderListener(IModelProviderListener listener) {
		fListeners.add(listener);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		switch (event.getType()) {
			case IResourceChangeEvent.POST_CHANGE :
				handleResourceDelta(event.getDelta());
				processModelChanges();
				break;
			case IResourceChangeEvent.PRE_CLOSE :
				IProject project = (IProject)event.getResource();
				removeWorkspaceModel(project);
				processModelChanges();
				break;
		}
	}
	/**
	 * @param project
	 */
	private void removeWorkspaceModel(IProject project) {
		IModel model = null;
		if (fModels.containsKey(project)) {
			model = (IModel)fModels.remove(project);
		} else if (fFragmentModels.containsKey(project)) {
			model = (IModel)fFragmentModels.remove(project);
		} else {
			model = (IModel)fFeatureModels.remove(project);
		}
		if (model != null) {
			if (model instanceof IPluginModelBase) {
				PDECore.getDefault().getTracingOptionsManager().reset();
			}
			if (fChangedModels == null)
				fChangedModels = new ArrayList();
			fChangedModels.add(new ModelChange(model, false));
		}
	}

	/**
	 * @param delta
	 */
	private void handleResourceDelta(IResourceDelta delta) {
		try {
			delta.accept(this);
		} catch (CoreException e) {
			PDECore.logException(e);
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
	 */
	public boolean visit(IResourceDelta delta) throws CoreException {
		if (delta != null) {
			IResource resource = delta.getResource();
			if (resource instanceof IProject) {
				IProject project = (IProject) resource;
				if (delta.getKind() == IResourceDelta.ADDED || (project.isOpen() && (delta.getFlags()&IResourceDelta.OPEN) != 0)) {
					addWorkspaceModel(project, true);
					return false;
				} else if (delta.getKind() == IResourceDelta.REMOVED) {
					removeWorkspaceModel(project);
					return false;
				}
				return true;
			} else if (resource instanceof IFile) {
				handleFileDelta(delta);
			} else if (resource instanceof IFolder) {
				return resource.getName().equals("META-INF"); //$NON-NLS-1$
			}
		}
		return true;
	}
	
	private synchronized void initializeWorkspaceModels() {
		if (fInitialized || fModelsLocked)
			return;
		fModelsLocked = true;
		fModels = Collections.synchronizedMap(new HashMap());
		fFragmentModels = Collections.synchronizedMap(new HashMap());
		fFeatureModels = Collections.synchronizedMap(new HashMap());
		
		IWorkspace workspace = PDECore.getWorkspace();
		IProject[] projects = workspace.getRoot().getProjects();
		
		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			if (isPluginProject(project) || isFeatureProject(project))
				addWorkspaceModel(project, false);			
		}
		
		workspace.addResourceChangeListener(this, IResourceChangeEvent.PRE_CLOSE);
		JavaCore.addPreProcessingResourceChangedListener(this);
		
		fModelsLocked = false;
		fInitialized = true;
	}

	/**
	 * @param project
	 * @return
	 */
	private IFeatureModel createFeatureModel(IFile file) {
		if (!file.exists())
			return null;
		
		WorkspaceFeatureModel model = new WorkspaceFeatureModel(file);
		loadModel(model, false);
		return model;
	}

	private IPluginModelBase createPluginModel(IProject project) {
		if (hasBundleManifest(project))
			return createWorkspaceBundleModel(project.getFile("META-INF/MANIFEST.MF")); //$NON-NLS-1$
		
		if (hasPluginManifest(project))
			return createWorkspacePluginModel(project.getFile("plugin.xml")); //$NON-NLS-1$
		
		return createWorkspaceFragmentModel(project.getFile("fragment.xml")); //$NON-NLS-1$
	}

	/**
	 * @param file
	 * @return
	 */
	private IPluginModelBase createWorkspacePluginModel(IFile file) {
		if (!file.exists())
			return null;
		
		WorkspacePluginModel model = new WorkspacePluginModel(file, true);
		loadModel(model, false);
		return model;
	}
	
	/**
	 * @param file
	 * @return
	 */
	private IPluginModelBase createWorkspaceFragmentModel(IFile file) {
		if (!file.exists())
			return null;
		
		WorkspaceFragmentModel model = new WorkspaceFragmentModel(file, true);
		loadModel(model, false);
		return model;
	}

	/**
	 * @param file
	 * @return
	 */
	private IPluginModelBase createWorkspaceBundleModel(IFile file) {
		if (!file.exists())
			return null;
		
		WorkspaceBundleModel model = new WorkspaceBundleModel(file);
		loadModel(model, false);
		
		IBundlePluginModelBase bmodel = null;
		boolean fragment = model.isFragmentModel();
		if (fragment)
			bmodel = new BundleFragmentModel();
		else
			bmodel = new BundlePluginModel();
		bmodel.setEnabled(true);
		bmodel.setBundleModel(model);
		
		IFile efile = file.getProject().getFile(fragment ? "fragment.xml" : "plugin.xml"); //$NON-NLS-1$ //$NON-NLS-2$
		if (efile.exists()) {
			WorkspaceExtensionsModel extModel = new WorkspaceExtensionsModel(efile);
			loadModel(extModel, false);
			bmodel.setExtensionsModel(extModel);
			extModel.setBundleModel(bmodel);
		}
		return bmodel;
	}
	
	private void loadModel(IModel model, boolean reload) {
		IFile file = (IFile) model.getUnderlyingResource();
		try {
			InputStream stream = file.getContents(true);
			if (reload)
				model.reload(stream, false);
			else
				model.load(stream, false);
			stream.close();
		} catch (CoreException e) {
			PDECore.logException(e);
			return;
		} catch (IOException e) {
		}
	}

	/**
	 * @param project
	 */
	private void addWorkspaceModel(IProject project, boolean notify) {
		IModel model = null;
		if (isPluginProject(project)) {
			model = createPluginModel(project);
			if (model != null) {			
				if (model instanceof IFragmentModel)
					fFragmentModels.put(project, model);
				else
					fModels.put(project, model);
				if (notify) {
					if (fChangedModels == null)
						fChangedModels = new ArrayList();
					fChangedModels.add(new ModelChange(model, true));					
				}
				if (project.getFile(".options").exists()) //$NON-NLS-1$
					PDECore.getDefault().getTracingOptionsManager().reset();
			}
		} else if (isFeatureProject(project)) {
			model = createFeatureModel(project.getFile("feature.xml")); //$NON-NLS-1$
			if (model != null) {
				fFeatureModels.put(project, model);
				if (notify) {
					if (fChangedModels == null)
						fChangedModels = new ArrayList();
					fChangedModels.add(new ModelChange(model, true));
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelManager#getFeatureModels()
	 */
	public IFeatureModel[] getFeatureModels() {
		initializeWorkspaceModels();
		return (IFeatureModel[]) fFeatureModels.values().toArray(new IFeatureModel[fFeatureModels.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelManager#shutdown()
	 */
	public void shutdown() {
		PDECore.getWorkspace().removeResourceChangeListener(this);
		JavaCore.removePreProcessingResourceChangedListener(this);
	}

	private void processModelChanges() {
		if (fChangedModels == null)
			return;
		
		if (fChangedModels.size() == 0) {
			fChangedModels = null;
			return;
		}

		Vector added = new Vector();
		Vector removed = new Vector();
		Vector changed = new Vector();
		for (int i = 0; i < fChangedModels.size(); i++) {
			ModelChange change = (ModelChange) fChangedModels.get(i);
			switch (change.type) {
				case IModelProviderEvent.MODELS_ADDED:
					added.add(change.model);
					break;
				case IModelProviderEvent.MODELS_REMOVED:
					removed.add(change.model);
					break;
				case IModelProviderEvent.MODELS_CHANGED:
					changed.add(change.model);
			}
		}
		IModel[] addedArray =
			added.size() > 0
				? (IModel[]) added.toArray(new IModel[added.size()])
				: (IModel[]) null;
		IModel[] removedArray =
			removed.size() > 0
				? (IModel[]) removed.toArray(new IModel[removed.size()])
				: (IModel[]) null;
		IModel[] changedArray = 
			changed.size() > 0
			? (IModel[]) changed.toArray(new IModel[changed.size()])
			: (IModel[]) null;
		int type = 0;
		if (addedArray != null)
			type |= IModelProviderEvent.MODELS_ADDED;
		if (removedArray != null)
			type |= IModelProviderEvent.MODELS_REMOVED;
		if (changedArray != null)
			type |= IModelProviderEvent.MODELS_CHANGED;

		fChangedModels = null;
		if (type != 0) {
			final ModelProviderEvent event =
				new ModelProviderEvent(
					this,
					type,
					addedArray,
					removedArray,
					changedArray);
			fireModelProviderEvent(event);
		}
	}
	
	private void fireModelProviderEvent(ModelProviderEvent event) {
		for (Iterator iter = fListeners.iterator(); iter.hasNext();) {
			((IModelProviderListener) iter.next()).modelsChanged(event);
		}
	}

}
