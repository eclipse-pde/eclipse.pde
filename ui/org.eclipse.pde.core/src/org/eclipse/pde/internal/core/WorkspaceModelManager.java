package org.eclipse.pde.internal.core;

import java.io.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.osgi.bundle.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.build.*;
import org.eclipse.pde.internal.core.feature.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.osgi.bundle.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.core.site.*;
import org.eclipse.team.core.*;

/**
 * @author melhem
 *
 */
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
	}
	
	private ArrayList fModels;
	private ArrayList fFragmentModels;	
	private ArrayList fFeatureModels;	
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
		return project.exists(new Path("META-INF/MANIFEST.MF"));
	}
	
	public static boolean hasPluginManifest(IProject project) {
		return project.exists(new Path("plugin.xml"));
	}
	
	public static boolean hasFragmentManifest(IProject project) {
		return project.exists(new Path("fragment.xml"));
	}
	
	public static boolean hasFeatureManifest(IProject project) {
		return project.exists(new Path("feature.xml"));
	}
	
	public static boolean isFeatureProject(IProject project) {
		if (project.isOpen())
			return project.exists(new Path("feature.xml"));
		return false;
	}
	
	public static boolean isBinaryPluginProject(IProject project) {
		if (isPluginProject(project)){
			try {
				String binary = project.getPersistentProperty(PDECore.EXTERNAL_PROJECT_PROPERTY);
				if (binary != null) {
					RepositoryProvider provider = RepositoryProvider.getProvider(project);
					return provider instanceof BinaryRepositoryProvider;
				}
			} catch (CoreException e) {
				PDECore.logException(e);
			}
		}
		return false;
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
	
	/**
	 * 
	 */
	public WorkspaceModelManager() {
		super();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWorkspaceModelManager#getAllEditableModelsUnused(java.lang.Class)
	 */
	public boolean getAllEditableModelsUnused(Class modelClass) {
		return false;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWorkspaceModelManager#getWorkspaceModel(org.eclipse.core.resources.IProject)
	 */
	public IModel getWorkspaceModel(IProject project) {
		if (!fInitialized)
			initializeWorkspaceModels();
		
		if (hasFeatureManifest(project))
			return getWorkspaceModel(project, fFeatureModels);
		
		if (hasBundleManifest(project)) {
			IModel model = getWorkspaceModel(project, fModels);
			return (model != null) ? model : getWorkspaceModel(project, fFragmentModels);
		}
		
		if (hasPluginManifest(project))
			return getWorkspaceModel(project, fModels);
			
		if (hasFragmentManifest(project))
				return getWorkspaceModel(project, fFragmentModels);
		
		return null;
	}
	
	private void handleFileDelta(IResourceDelta delta) {
		IFile file = (IFile)delta.getResource();
		if (file.getName().equals(".options")) {
			PDECore.getDefault().getTracingOptionsManager().reset();
			return;
		}
		if (file.getName().equals("build.properties") && isPluginProject(file.getProject())) {
			fireModelsChanged(new IModel[] {getWorkspaceModel(file.getProject())});
			return;
		}
		
		if (!isSupportedFile(file))
			return;
		
		int kind = delta.getKind();
		switch (kind) {
			case IResourceDelta.ADDED:
				handleFileAdded(file);
				break;
			case IResourceDelta.REMOVED:
				handleFileRemoved(file);
				break;
			case IResourceDelta.CHANGED:
				handleFileChanged(file, delta);
				break;
		}		
	}
	
	private void handleFileAdded(IFile file) {
		IModel model = getWorkspaceModel(file);
		if (model != null)
			removeWorkspaceModel(model);
		addWorkspaceModel(file.getProject(), true);
	}
	
	private void handleFileRemoved(IFile file) {
		IModel model = getWorkspaceModel(file);
		String fileName = file.getName().toLowerCase();
		if (model != null) {
			if (model instanceof IBundlePluginModelBase) {
				IBundlePluginModelBase bModel = (IBundlePluginModelBase)model;
				if (fileName.equals("plugin.xml") || fileName.equals("fragment.xml")) {
					bModel.setExtensionsModel(null);
				} else {
					removeWorkspaceModel(bModel);
					if (bModel.getExtensionsModel() != null)
						switchToPluginMode(bModel);
				}					
			} else {
				removeWorkspaceModel(model);
			}
		}
	}
	
	private void switchToPluginMode(IBundlePluginModelBase bModel) {
		IPluginModelBase model = null;
		IProject project = bModel.getUnderlyingResource().getProject();
		if (bModel instanceof IBundlePluginModel) {
			model = createWorkspacePluginModel(project.getFile("plugin.xml"));
		} else {
			model = createWorkspaceFragmentModel(project.getFile("fragment.xml"));
		}
			
		if (model != null && model.getPluginBase().getId() != null) {
			if (model instanceof IPluginModel) {
				fModels.add(model);
			} else {
				fFragmentModels.add(model);
			}
			if (fChangedModels == null)
				fChangedModels = new ArrayList();
			fChangedModels.add(new ModelChange(model, true));
		}
	}
	
	private void handleFileChanged(IFile file, IResourceDelta delta) {
		IModel model = getWorkspaceModel(file);
		if (model == null)
			return;
		if ((IResourceDelta.CONTENT & delta.getFlags()) != 0) {
			if (model instanceof IBundlePluginModelBase) {
				if (isBundleManifestFile(file)) {
					loadModel(((IBundlePluginModelBase)model).getBundleModel(), true);
				} else {
					loadModel(((IBundlePluginModelBase)model).getExtensionsModel(), true);
				}
			} else {
				loadModel(model, true);
			}
			fireModelsChanged(new IModel[] { model });
		}		
	}
	
	public void fireModelsChanged(IModel[] models) {
		ModelProviderEvent event =
			new ModelProviderEvent(
				this,
				IModelProviderEvent.MODELS_CHANGED,
				null,
				null,
				models);
		fireModelProviderEvent(event);
	}
	
	private boolean isSupportedFile(IFile file) {
		if (isBundleManifestFile(file))
			return true;
		String name = file.getName().toLowerCase();
		if (!name.equals("plugin.xml") && !name.equals("fragment.xml")
				&& !name.equals("feature.xml"))
			return false;
		IPath expectedPath = file.getProject().getFullPath().append(name);
		return expectedPath.equals(file.getFullPath());
	}

	
	private boolean isBundleManifestFile(IFile file) {
		IPath path = file.getProjectRelativePath();
		return (
			path.segmentCount() == 2
				&& path.segment(0).equals("META-INF")
				&& path.segment(1).equals("MANIFEST.MF"));
	}
	
	private IModel getWorkspaceModel(IFile file) {
		if (isBundleManifestFile(file)) {
			IModel model = getWorkspaceModel(file.getProject(), fModels);
			return (model != null) ? model : getWorkspaceModel(file.getProject(), fFragmentModels);
		}		
		IPath path = file.getProjectRelativePath();
		if (path.equals(new Path("plugin.xml")))
			return getWorkspaceModel(file.getProject(), fModels);
		if (path.equals(new Path("fragment.xml")))
			return getWorkspaceModel(file.getProject(), fFragmentModels);
		if (path.equals(new Path("feature.xml")))
			return getWorkspaceModel(file.getProject(), fFeatureModels);
		return null;		
	}
	
	private IModel getWorkspaceModel(IProject project, ArrayList models) {
		for (int i = 0; i < models.size(); i++) {
			IModel model = (IModel) models.get(i);
			IFile file = (IFile) model.getUnderlyingResource();
			if (file.getProject().equals(project)) {
				return model;
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelManager#getAllModels()
	 */
	public IPluginModelBase[] getAllModels() {
		if (!fInitialized) 
			initializeWorkspaceModels();
		
		ArrayList result = new ArrayList();
		for (int i = 0; i < fModels.size(); i++) {
			result.add(fModels.get(i));
		}
		for (int i = 0; i < fFragmentModels.size(); i++) {
			result.add(fFragmentModels.get(i));
		}
		return (IPluginModelBase[]) result.toArray(
			new IPluginModelBase[result.size()]);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWorkspaceModelManager#isLocked()
	 */
	public boolean isLocked() {
		return fModelsLocked;
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
	 * @see org.eclipse.pde.core.IWorkspaceModelManager#reset()
	 */
	public void reset() {
		initializeWorkspaceModels();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		switch (event.getType()) {
			case IResourceChangeEvent.PRE_AUTO_BUILD :
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
		IModel model = getWorkspaceModel(project);
		if (model != null) {
			removeWorkspaceModel(model);
		}
	}
	/**
	 * @param model
	 */
	private void removeWorkspaceModel(IModel model) {
		if (model instanceof IFeatureModel) {
			fFeatureModels.remove(model);
		} else if (model instanceof IPluginModelBase){
			if (model instanceof IFragmentModel)
				fFragmentModels.remove(model);
			else
				fModels.remove(model);
			PDECore.getDefault().getTracingOptionsManager().reset();
		}
		if (fChangedModels == null)
			fChangedModels = new ArrayList();
		fChangedModels.add(new ModelChange(model, false));
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
				}
				return true;
			} else if (resource instanceof IFile) {
				handleFileDelta(delta);
			} else if (resource instanceof IFolder) {
				return resource.getName().equals("META-INF");
			}
		}
		return true;
	}
	
	private void initializeWorkspaceModels() {
		if (fInitialized || fModelsLocked)
			return;
		fModelsLocked = true;
		fModels = new ArrayList();
		fFragmentModels = new ArrayList();
		fFeatureModels = new ArrayList();
		
		IWorkspace workspace = PDECore.getWorkspace();
		IProject[] projects = workspace.getRoot().getProjects();
		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			if (!project.isOpen())
				continue;
			addWorkspaceModel(project, false);			
		}
		workspace.addResourceChangeListener(this,
				IResourceChangeEvent.PRE_CLOSE
				| IResourceChangeEvent.PRE_AUTO_BUILD);
		fInitialized = true;
		fModelsLocked = false;
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
			return createWorkspaceBundleModel(project.getFile("META-INF/MANIFEST.MF"));
		
		if (hasPluginManifest(project))
			return createWorkspacePluginModel(project.getFile("plugin.xml"));
		
		return createWorkspaceFragmentModel(project.getFile("fragment.xml"));
	}

	/**
	 * @param file
	 * @return
	 */
	private IPluginModelBase createWorkspacePluginModel(IFile file) {
		if (!file.exists())
			return null;
		
		WorkspacePluginModel model = new WorkspacePluginModel(file);
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
		
		WorkspaceFragmentModel model = new WorkspaceFragmentModel(file);
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
		PDEState state = PDECore.getDefault().getExternalModelManager().getState();
		bmodel.setBundleDescription(state.addBundle(new File(file.getLocation().removeLastSegments(2).toString())));
		
		IFile efile = file.getProject().getFile(fragment ? "fragment.xml" : "plugin.xml");
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
		InputStream stream = null;
		boolean outOfSync = false;
		try {
			stream = file.getContents(false);
		} catch (CoreException e) {
			outOfSync = true;
			try {
				stream = file.getContents(true);
			} catch (CoreException e2) {
				PDECore.logException(e);
				return;
			}
		}
		try {
			if (reload)
				model.reload(stream, outOfSync);
			else
				model.load(stream, outOfSync);
			stream.close();
		} catch (Exception e) {
			PDECore.logException(e);
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
					fFragmentModels.add(model);
				else
					fModels.add(model);
				if (notify) {
					if (fChangedModels == null)
						fChangedModels = new ArrayList();
					fChangedModels.add(new ModelChange(model, true));					
				}
				if (project.getFile(".options").exists())
					PDECore.getDefault().getTracingOptionsManager().reset();
			}
		} else if (isFeatureProject(project)) {
			model = createFeatureModel(project.getFile("feature.xml"));
			if (model != null)
				fFeatureModels.add(model);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelManager#getPluginModels()
	 */
	public IPluginModel[] getPluginModels() {
		if (!fInitialized)
			initializeWorkspaceModels();
		return (IPluginModel[])fModels.toArray(new IPluginModel[fModels.size()]);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelManager#getFragmentModels()
	 */
	public IFragmentModel[] getFragmentModels() {
		if (!fInitialized)
			initializeWorkspaceModels();
		return (IFragmentModel[]) fFragmentModels.toArray(new IFragmentModel[fFragmentModels.size()]);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelManager#getFeatureModels()
	 */
	public IFeatureModel[] getFeatureModels() {
		if (!fInitialized)
			initializeWorkspaceModels();
		return (IFeatureModel[]) fFeatureModels.toArray(new IFeatureModel[fFeatureModels.size()]);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelManager#getFragmentsFor(java.lang.String, java.lang.String)
	 */
	public IFragment[] getFragmentsFor(String pluginId, String version) {
		if (!fInitialized)
			initializeWorkspaceModels();
		ArrayList result = new ArrayList();
		for (int i = 0; i < fFragmentModels.size(); i++) {
			IFragment fragment = ((IFragmentModel)fFragmentModels.get(i)).getFragment();
			if (PDECore
				.compare(
					fragment.getPluginId(),
					fragment.getPluginVersion(),
					pluginId,
					version,
					fragment.getRule())) {
				result.add(fragment);
			}
		}
		return (IFragment[]) result.toArray(new IFragment[result.size()]);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelManager#shutdown()
	 */
	public void shutdown() {
		PDECore.getWorkspace().removeResourceChangeListener(this);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelManager#isInitialized()
	 */
	public boolean isInitialized() {
		return fInitialized;
	}
	public IModel getModel(IFile file) {
		String name = file.getName().toLowerCase();
		IProject project = file.getProject();
		if (isPluginProject(project) && hasBundleManifest(project) && file.getProjectRelativePath().equals(new Path("META-INF/MANIFEST.MF"))) {
			return new WorkspaceBundleModel(file);
		}
		if (name.equals("plugin.xml")) {
			//TODO support this
			//if (hasBundleManifest(project))
				//return new WorkspaceExtensionsModel(file);
			return new WorkspacePluginModel(file);
		}
		if (name.equals("fragment.xml")) {
			//TODO support this
			//if (hasBundleManifest(project))
				//return new WorkspaceExtensionsModel(file);
			return new WorkspaceFragmentModel(file);
		}
		if (name.equals("build.properties")) {
			return new WorkspaceBuildModel(file);
		}
		if (name.equals("feature.xml")) {
			return new WorkspaceFeatureModel(file);
		}
		if (name.equals("site.xml")) {
			return new WorkspaceSiteModel(file);
		}
		if (name.equals(PDECore.SITEBUILD_PROPERTIES)) {
			return new WorkspaceSiteBuildModel(file);
		}
		return null;
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
