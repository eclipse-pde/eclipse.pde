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
package org.eclipse.pde.internal.core.osgi;
import java.io.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.osgi.bundle.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.feature.WorkspaceFeatureModel;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.osgi.bundle.*;
import org.eclipse.pde.internal.core.osgi.bundle.WorkspaceBundleModel;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.core.site.*;
import org.eclipse.team.core.RepositoryProvider;

public class OSGiWorkspaceModelManager
	implements IWorkspaceModelManager, IResourceChangeListener, IResourceDeltaVisitor {

	private Hashtable models = new Hashtable();
	private Vector listeners = new Vector();
	private Vector workspaceModels = null;
	private Vector workspaceFragmentModels = null;
	private Vector workspaceFeatureModels = null;
	private Vector modelChanges = null;
	private static final boolean DEBUG = false;
	private boolean modelsLocked=false;

	class ModelChange {
		IModel model;
		int type;
		
		public ModelChange(IModel model, boolean added) {
			this.model = model;
			this.type = added?IModelProviderEvent.MODELS_ADDED:
							 IModelProviderEvent.MODELS_REMOVED;
		}
		public ModelChange(IModel model) {
			this.model = model;
			this.type = IModelProviderEvent.MODELS_CHANGED;
		}
	}

	class ModelInfo {
		int count;
		IModel model;
		IModel readOnlyModel;
		Object consumer;
		public boolean isExclusiveAccess() {
			return true;
		}
	}
	private boolean initialized;

	public OSGiWorkspaceModelManager() {
		super();
	}
	
	public boolean isInitialized() {
		return initialized;
	}
	
	public boolean isLocked() {
		return modelsLocked;
	}
	public void addModelProviderListener(IModelProviderListener listener) {
		listeners.add(listener);
	}
	private void addWorkspaceModel(IModel model) {
		if (workspaceModels == null)
			initializeWorkspacePluginModels();
		if (model instanceof IFragmentModel)
			workspaceFragmentModels.add(model);
		else if (model instanceof IFeatureModel)
			workspaceFeatureModels.add(model);
		else
			workspaceModels.add(model);
		PDECore.getDefault().getTracingOptionsManager().reset();
		if (modelChanges != null)
			modelChanges.add(new ModelChange(model, true));
	}
	private void checkTracing(IFile file) {
		if (file.getName().equals(".options")) {
			PDECore.getDefault().getTracingOptionsManager().reset();
		}
	}
	public void connect(Object element, Object consumer) {
		this.connect(element, consumer, true);
	}
	public void connect(Object element, Object consumer, boolean editable) {
		ModelInfo info = (ModelInfo) models.get(element);
		if (info == null) {
			info = new ModelInfo();
			info.count = 0;
			models.put(element, info);
		}
		info.count++;
		if (info.model != null && info.consumer != null) {
			if (info.consumer != consumer)
				verifyConsumer(info);
		}
		if (info.model == null && editable) {
			// create editable copy and register the exclusive owner
			info.model = createModel(element);
			info.consumer = consumer;
		} else {
			// editable model already created or not editable - use read only
			info.readOnlyModel = createModel(element, false);
		}
	}
	protected IModel createModel(Object element) {
		return createModel(element, true);
	}
	private boolean isBundleManifestFile(IFile file) {
		IPath path = file.getProjectRelativePath();
		return (
			path.segmentCount() == 2
				&& path.segment(0).equals("META-INF")
				&& path.segment(1).equals("MANIFEST.MF"));
	}
	protected IModel createModel(Object element, boolean editable) {
		if (element instanceof IFile) {
			IFile file = (IFile) element;
			String name = file.getName().toLowerCase();

			if (isBundleManifestFile(file)) {
				WorkspaceBundleModel model = new WorkspaceBundleModel(file);
				model.setEditable(editable && model.isEditable());
				return model;
			}

			if (name.equals("plugin.xml")) {
				WorkspacePluginModel model = new WorkspacePluginModel(file);
				model.setEditable(editable && model.isEditable());
				return model;
			}
			if (name.equals("extensions.xml")) {
				WorkspaceExtensionsModel model =
					new WorkspaceExtensionsModel(file);
				model.setEditable(editable && model.isEditable());
				return model;
			}
			if (name.equals("fragment.xml")) {
				WorkspaceFragmentModel model = new WorkspaceFragmentModel(file);
				model.setEditable(editable && model.isEditable());
				return model;
			}
			if (name.equals("build.properties")) {
				WorkspaceBuildModel model = new WorkspaceBuildModel(file);
				model.setEditable(editable && model.isEditable());
				return model;
			}
			if (name.equals("feature.xml")) {
				WorkspaceFeatureModel model = new WorkspaceFeatureModel(file);
				model.setEditable(editable && model.isEditable());
				return model;
			}
			if (name.equals("site.xml")) {
				WorkspaceSiteModel model = new WorkspaceSiteModel(file);
				model.setEditable(editable && model.isEditable());
				return model;
			}
			if (name.equals(PDECore.SITEBUILD_PROPERTIES)) {
				WorkspaceSiteBuildModel model =
					new WorkspaceSiteBuildModel(file);
				model.setEditable(editable && model.isEditable());
				return model;
			}
		}
		return null;
	}

	private IPluginModelBase createWorkspacePluginModel(IFile pluginFile) {
		if (pluginFile.exists() == false)
			return null;
		connect(pluginFile, null, false);
		IPluginModelBase model = (IPluginModelBase) getModel(pluginFile, null);
		loadWorkspaceModel(model);
		return model;
	}
	private IFile getBundleFile(IProject project) {
		IPath path = new Path("META-INF/MANIFEST.MF");
		return project.getFile(path);
	}
	
	private IModel createWorkspacePluginModel(IProject project) {
		IPath pluginPath;
		IFile pluginFile;
		if (isBundleProject(project)) {
			pluginFile = getBundleFile(project);
			return createWorkspaceBundleModel(pluginFile);
		} else {
			pluginPath = project.getFullPath().append("plugin.xml");
			pluginFile = project.getWorkspace().getRoot().getFile(pluginPath);
		}
		if (pluginFile.exists() == false) {
			pluginPath = project.getFullPath().append("fragment.xml");
			pluginFile = project.getWorkspace().getRoot().getFile(pluginPath);
		}
		if (pluginFile.exists()) {
			return createWorkspacePluginModel(pluginFile);
		}
		return createWorkspaceFeatureModel(project);
	}

	private IFeatureModel createWorkspaceFeatureModel(IFile featureFile) {
		if (featureFile.exists() == false)
			return null;
		connect(featureFile, null, false);
		IFeatureModel model = (IFeatureModel) getModel(featureFile, null);
		loadModel(model);
		return model;
	}

	private IBundlePluginModelBase createWorkspaceBundleModel(IFile bundleFile) {
		if (bundleFile.exists() == false)
			return null;
		connect(bundleFile, null, false);
		IBundleModel model = (IBundleModel) getModel(bundleFile, null);
		loadModel(model);
		IBundlePluginModelBase bmodel = new BundlePluginModelBase();
		bmodel.setEnabled(true);
		bmodel.setBundleModel(model);
		
		IFile efile = bundleFile.getProject().getFile("extensions.xml");
		if (efile.exists()) {
			connect(efile, null, false);
			IExtensionsModel emodel = (IExtensionsModel)getModel(efile, null);
			loadModel(emodel);
			bmodel.setExtensionsModel(emodel);
		}
		return bmodel;
	}
	
	private IExtensionsModel createWorkspaceExtensionsModel(IFile extensionsFile) {
		if (extensionsFile.exists() == false)
			return null;
		connect(extensionsFile, null, false);
		IExtensionsModel model = (IExtensionsModel) getModel(extensionsFile, null);
		loadModel(model);
		return model;
	}	

	private IFeatureModel createWorkspaceFeatureModel(IProject project) {
		IFile featureFile = project.getFile("feature.xml");
		if (featureFile.exists()) {
			return createWorkspaceFeatureModel(featureFile);
		}
		return null;
	}

	public void disconnect(Object element, Object consumer) {
		ModelInfo info = (ModelInfo) models.get(element);
		if (info != null) {
			info.count--;
			if (info.consumer != null && info.consumer.equals(consumer)) {
				// editable copy can go
				info.model.dispose();
				info.model = null;
			}
			if (info.count == 0) {
				if (info.model != null)
					info.model.dispose();
				info.readOnlyModel = null;
				models.remove(element);
			}
		}
	}

	private void fireModelProviderEvent(ModelProviderEvent event) {
		for (Iterator iter = listeners.iterator(); iter.hasNext();) {
			((IModelProviderListener) iter.next()).modelsChanged(event);
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

	public boolean getAllEditableModelsUnused(Class modelClass) {
		for (Enumeration enum = models.elements(); enum.hasMoreElements();) {
			ModelInfo info = (ModelInfo) enum.nextElement();
			if (info.model != null
				&& info.model.getClass().isInstance(modelClass)) {
				return false;
			}
		}
		return true;
	}
	public IFragment[] getFragmentsFor(String pluginId, String version) {
		Vector result = new Vector();
		if (workspaceFragmentModels == null)
			initializeWorkspacePluginModels();
		validate(workspaceFragmentModels);
		for (int i = 0; i < workspaceFragmentModels.size(); i++) {
			IFragmentModel model =
				(IFragmentModel) workspaceFragmentModels.elementAt(i);
			IFragment fragment = model.getFragment();
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
		IFragment[] array = new IFragment[result.size()];
		result.copyInto(array);
		return array;
	}

	public IModel getModel(Object element, Object consumer) {
		ModelInfo info = (ModelInfo) models.get(element);
		if (info != null) {
			if (info.consumer != null && info.consumer.equals(consumer))
				return info.model;
			else {
				return info.readOnlyModel;
			}
		}
		return null;
	}
	public IFragmentModel[] getFragmentModels() {
		if (workspaceFragmentModels == null) {
			initializeWorkspacePluginModels();
		}
		validate(workspaceFragmentModels);
		IFragmentModel[] result =
			new IFragmentModel[workspaceFragmentModels.size()];
		workspaceFragmentModels.copyInto(result);
		return result;
	}
	public IFeatureModel[] getFeatureModels() {
		if (workspaceFeatureModels == null) {
			initializeWorkspacePluginModels();
		}
		validate(workspaceFeatureModels);
		IFeatureModel[] result =
			new IFeatureModel[workspaceFeatureModels.size()];
		workspaceFeatureModels.copyInto(result);
		return result;
	}

	private IModel getWorkspaceModel(IFile file) {
		return getWorkspaceModel(file, true);
	}

	private IModel getWorkspaceModel(IFile file, boolean validate) {
		String name = file.getName().toLowerCase();
		Vector models = null;
		if (validate)
			validate();

		if (name.equals("plugin.xml") || isBundleManifestFile(file))
			models = workspaceModels;
		else if (name.equals("fragment.xml"))
			models = workspaceFragmentModels;
		else if (name.equals("feature.xml"))
			models = workspaceFeatureModels;
		return getWorkspaceModel(file.getProject(), models);
	}
	public IModel getWorkspaceModel(IProject project) {
		return getWorkspaceModel(project, true);
	}
	public IModel getWorkspaceModel(IProject project, boolean validate) {
		if (validate) validate();

		if (isBundleProject(project))
			return getWorkspaceModel(project, workspaceModels);
		IPath filePath = project.getFullPath().append("plugin.xml");
		IFile file = project.getWorkspace().getRoot().getFile(filePath);
		if (file.exists()) {
			return getWorkspaceModel(project, workspaceModels);
		}
		filePath = project.getFullPath().append("fragment.xml");
		file = project.getWorkspace().getRoot().getFile(filePath);
		if (file.exists()) {
			return getWorkspaceModel(project, workspaceFragmentModels);
		}
		file = project.getFile("feature.xml");
		if (file.exists()) {
			return getWorkspaceModel(project, workspaceFeatureModels);
		}
		return null;
	}
	private IModel getWorkspaceModel(IProject project, Vector models) {
		if (models == null)
			return null;
		for (int i = 0; i < models.size(); i++) {
			IModel model = (IModel) models.elementAt(i);
			IFile file = (IFile) model.getUnderlyingResource();
			if (file.getProject().equals(project)) {
				return model;
			}
		}
		return null;
	}
	public IPluginModel[] getPluginModels() {
		if (workspaceModels == null) {
			initializeWorkspacePluginModels();
		}
		validate(workspaceModels);
		ArrayList pluginModels = new ArrayList();
		for (int i = 0; i < workspaceModels.size(); i++) {
			IPluginModelBase model = (IPluginModelBase) workspaceModels.get(i);
			if (model instanceof IPluginModel)
				pluginModels.add(model);
		}
		return (IPluginModel[]) pluginModels.toArray(
			new IPluginModel[pluginModels.size()]);
	}

	public IPluginModelBase[] getAllModels() {
		if (workspaceModels == null) {
			initializeWorkspacePluginModels();
		}
		validate();
		ArrayList result = new ArrayList();
		for (int i = 0; i < workspaceModels.size(); i++) {
			result.add(workspaceModels.get(i));
		}
		for (int i = 0; i < workspaceFragmentModels.size(); i++) {
			result.add(workspaceFragmentModels.get(i));
		}
		return (IPluginModelBase[]) result.toArray(
			new IPluginModelBase[result.size()]);
	}
	
	private IBundlePluginModelBase getBundleModel(IProject project, boolean validate) {
		IModel model = getWorkspaceModel(project, validate);
		if (model!=null && model instanceof IBundlePluginModelBase)
			return (IBundlePluginModelBase)model;
		return null;
	}
	
	private IModel getExtensionsModel(IProject project, boolean validate) {
		IBundlePluginModelBase bmodel = getBundleModel(project, validate);
		if (bmodel!=null) return bmodel.getExtensionsModel();
		else return null;
	}

	private void handleFileDelta(IResourceDelta delta) {
		IFile file = (IFile) delta.getResource();
		checkTracing(file);
		if (isSupportedFile(file) == false)
			return;
		String fileName = file.getName().toLowerCase();

		if (delta.getKind() == IResourceDelta.ADDED) {
			// manifest added - add the model
			IModel model;
			if (fileName.equals("extensions.xml")) {
				// find the model in the aggregate
				model = getExtensionsModel(file.getProject(), true);
			}
			else 
				model = getWorkspaceModel(file);
			if (model == null) {
				if (isBundleManifestFile(file)) {
					model = createWorkspaceBundleModel(file);
				}
				else if (fileName.equals("extensions.xml")) {
					IBundlePluginModelBase bmodel = getBundleModel(file.getProject(), true);
					if (bmodel!=null) {
						model = createWorkspaceExtensionsModel(file);
						bmodel.setExtensionsModel((IExtensionsModel)model);
						if (modelChanges != null)
							modelChanges.add(new ModelChange(bmodel));
					}
					return;
				}
				else if (fileName.equals("feature.xml"))
					model = createWorkspaceFeatureModel(file);
				else if (isBundleManifestFile(file))
					model = createWorkspaceBundleModel(file);
				else
					model = createWorkspacePluginModel(file);
				if (model instanceof IBundlePluginModelBase) {
					switchToBundleMode((IBundlePluginModelBase)model);
				}
				addWorkspaceModel(model);
			} else if (model instanceof IExtensionsModel)
				reloadExtensionsModel((IExtensionsModel)model);
			else if (model instanceof IFeatureModel)
				reloadModel(model);
			else {
				//see if we need to switch to a budle
				if (isBundleManifestFile(file)) {
					if (model instanceof IBundlePluginModelBase) {
						reloadBundleModel((IBundlePluginModelBase)model);
					}
					else {
						//the current model is not a bundle
						IBundlePluginModelBase bmodel = createWorkspaceBundleModel(file);
						switchToBundleMode(bmodel);
						addWorkspaceModel(bmodel);
					}
				}
				else
					reloadWorkspaceModel((IPluginModelBase) model);
			}
		} else {
			if (delta.getKind() == IResourceDelta.REMOVED) {
				IModel model;
				
				if (isBundleManifestFile(file)) {
					model = getBundleModel(file.getProject(), false);
				}
				else if (fileName.equals("extensions.xml")) {
					// find the model in the aggregate
					model = getExtensionsModel(file.getProject(), false);
				}
				else 
					model = getWorkspaceModel(file, false);
				// manifest has been removed - ditch the model
				if (model != null)
					removeWorkspaceModel(model);
				if (model instanceof IBundlePluginModelBase) {
					switchToPluginMode((IBundlePluginModelBase)model);
				}
			} else if (delta.getKind() == IResourceDelta.CHANGED) {
				IModel model = getWorkspaceModel(file);
				if (model == null)
					return;
				if ((IResourceDelta.CONTENT & delta.getFlags()) != 0) {
					// file content modified - sync up
					if (model instanceof IBundlePluginModelBase)
						reloadBundleModel((IBundlePluginModelBase)model);
					else if (model instanceof IExtensionsModel)
						reloadExtensionsModel((IExtensionsModel)model);
					else if (model instanceof IFeatureModel)
						reloadModel(model);
					else
						reloadWorkspaceModel((IPluginModelBase) model);
				}
			}
		}
	}

	private void handleProjectClosing(IProject project) {
		// not reason to keep it around if it is closed
		handleProjectToBeDeleted(project);
	}

	private void handleProjectDelta(IResourceDelta delta) {
		IProject project = (IProject) delta.getResource();
		int kind = delta.getKind();

		if (project.isOpen() == false) {
			return;
		}

		if (kind == IResourceDelta.CHANGED
			&& (delta.getFlags() | IResourceDelta.DESCRIPTION) != 0) {
			// Project description changed. Test if this
			// is now a PDE project and act
			if (isPluginProject(project)) {
				ensureModelExists(project);
				validateBinaryStatus(project);
			} else if (isFeatureProject(project)) {
				ensureModelExists(project);
			}
		}
	}

	private void validateBinaryStatus(IProject project) {
		boolean shared = false;

		RepositoryProvider provider = RepositoryProvider.getProvider(project);
		if (provider != null
			&& !(provider instanceof BinaryRepositoryProvider))
			shared = true;
		if (shared) {
			try {
				String binary =
					project.getPersistentProperty(
						PDECore.EXTERNAL_PROJECT_PROPERTY);
				if (binary != null) {
					// The project contents has been replaced by
					// core - this is not a binary project any more
					project.setPersistentProperty(
						PDECore.EXTERNAL_PROJECT_PROPERTY,
						null);
				}
			} catch (CoreException e) {
				PDECore.logException(e);
			}
		}
	}

	private void handleProjectToBeDeleted(IProject project) {
		if (!isPluginProject(project) && !isFeatureProject(project)) {
			return;
		}
		IModel model = getWorkspaceModel(project);
		if (model != null) {
			removeWorkspaceModel(model);
		}
	}
	private void handleResourceDelta(IResourceDelta delta) {
		try {
			delta.accept(this);
		} catch (CoreException e) {
			PDECore.logException(e);
		}
	}
	private void initializeWorkspacePluginModels() {
		long start = System.currentTimeMillis();
		modelsLocked=true;
		workspaceModels = new Vector();
		workspaceFragmentModels = new Vector();
		workspaceFeatureModels = new Vector();
		IWorkspace workspace = PDECore.getWorkspace();
		IProject[] projects = workspace.getRoot().getProjects();
		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			if (!project.isOpen())
				continue;
			if (isBundleProject(project)) {
				IModel model = createWorkspacePluginModel(project);
				workspaceModels.add(model);
			}
			else if (isPluginProject(project)) {
				IModel model = createWorkspacePluginModel(project);
				if (model != null) {
					if (model instanceof IFragmentModel)
						workspaceFragmentModels.add(model);
					else
						workspaceModels.add(model);
				}
			} else if (isFeatureProject(project)) {
				IFeatureModel model = createWorkspaceFeatureModel(project);
				workspaceFeatureModels.add(model);
			}
		}
		workspace.addResourceChangeListener(
			this,
			IResourceChangeEvent.PRE_CLOSE
				| IResourceChangeEvent.PRE_DELETE
				| IResourceChangeEvent.PRE_AUTO_BUILD);
		initialized = true;
		modelsLocked=false;
		long stop = System.currentTimeMillis();
		if (DEBUG)
			System.out.println(
				"Workspace plugins loaded in " + (stop - start) + "ms");
	}

	public static boolean isPluginProject(IProject project) {
		if (project.isOpen() == false)
			return false;
		return project.exists(new Path("plugin.xml"))
			|| project.exists(new Path("fragment.xml"));
	}
	public static boolean isBundleProject(IProject project) {
		if (project.isOpen() == false)
			return false;
		return project.exists(new Path("META-INF/MANIFEST.MF"));
	}
	public static boolean isFeatureProject(IProject project) {
		if (project.isOpen() == false)
			return false;
		return project.exists(new Path("feature.xml"));
	}

	private static boolean isJavaProject(IProject project) {
		try {
			if (!project.hasNature(JavaCore.NATURE_ID))
				return false;
		} catch (CoreException e) {
			PDECore.logException(e);
			return false;
		}
		return true;
	}

	public static boolean isJavaPluginProject(IProject project) {
		if (!isPluginProject(project))
			return false;
		return isJavaProject(project);
	}

	public static boolean isJavaBundleProject(IProject project) {
		if (!isBundleProject(project))
			return false;
		return isJavaProject(project);
	}

	public static boolean isJavaPluginProjectWithSource(IProject project) {
		if (!isJavaPluginProject(project))
			return false;
		try {
			IJavaProject jProject = JavaCore.create(project);
			IPackageFragmentRoot[] roots = jProject.getPackageFragmentRoots();

			for (int i = 0; i < roots.length; i++) {
				if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE)
					return true;
			}
		} catch (JavaModelException e) {
		}
		return false;
	}

	private static boolean isBinaryProject(IProject project) {
		try {
			String binary =
				project.getPersistentProperty(
					PDECore.EXTERNAL_PROJECT_PROPERTY);
			if (binary != null) {
				RepositoryProvider provider =
					RepositoryProvider.getProvider(project);
				return provider == null
					|| provider instanceof BinaryRepositoryProvider;
			}
		} catch (CoreException e) {
			PDECore.logException(e);
		}
		return false;		
	}
	
	public static boolean isBinaryPluginProject(IProject project) {
		if (!isPluginProject(project))
			return false;
		return isBinaryProject(project);
	}	
	
	public static boolean isBinaryBundleProject(IProject project) {
		if (!isBundleProject(project))
			return false;
		return isBinaryProject(project);
	}

	public static boolean isUnsharedPluginProject(IProject project) {
		return RepositoryProvider.getProvider(project) == null
			|| isBinaryPluginProject(project);
	}
	
	public static boolean isUnsharedBundleProject(IProject project) {
		return RepositoryProvider.getProvider(project) == null
			|| isBinaryBundleProject(project);
	}

	public static boolean isBinaryFeatureProject(IProject project) {
		if (!isFeatureProject(project))
			return false;
		try {
			String binary =
				project.getPersistentProperty(
					PDECore.EXTERNAL_PROJECT_PROPERTY);
			if (binary != null) {
				RepositoryProvider provider =
					RepositoryProvider.getProvider(project);
				return provider == null
					|| provider instanceof BinaryRepositoryProvider;
			}
		} catch (CoreException e) {
			PDECore.logException(e);
		}
		return false;
	}

	private void ensureModelExists(IProject pluginProject) {
		if (!initialized)
			return;
		IModel model = getWorkspaceModel(pluginProject);
		if (model == null) {
			model = createWorkspacePluginModel(pluginProject);
			if (model != null) {
				addWorkspaceModel(model);
			}
		}
	}

	private boolean isSupportedFile(IFile file) {
		if (isBundleManifestFile(file)) return true;
		String name = file.getName().toLowerCase();

		if (!name.equals("plugin.xml")
			&& !name.equals("fragment.xml")
			&& !name.equals("feature.xml")
			&& !name.equals("extensions.xml"))
			return false;
		IPath expectedPath = file.getProject().getFullPath().append(name);
		// Supported files must be directly under the project
		return expectedPath.equals(file.getFullPath());
	}

	private void loadWorkspaceModel(IPluginModelBase model) {
		IFile file = (IFile) model.getUnderlyingResource();
		InputStream stream = null;
		boolean outOfSync = false;
		try {
			stream = file.getContents(false);
		} catch (CoreException e) {
			outOfSync = true;
		}
		if (outOfSync) {
			try {
				stream = file.getContents(true);
			} catch (CoreException e) {
				// cannot get file contents - something is
				// seriously wrong
				IPluginBase base = model.getPluginBase(true);
				try {
					base.setId(file.getProject().getName());
					base.setName(base.getId());
					base.setVersion("0.0.0");
					PDECore.log(e);
				} catch (CoreException ex) {
					PDECore.logException(ex);
				}
				return;
			}
		}
		try {
			model.load(stream, outOfSync);
			stream.close();
		} catch (CoreException e) {
			// errors in loading, but we will still
			// initialize.
		} catch (IOException e) {
			PDECore.logException(e);
		}
	}
	private void loadModel(IModel model) {
		IFile file = (IFile) model.getUnderlyingResource();
		InputStream stream = null;
		boolean outOfSync = false;
		try {
			stream = file.getContents(false);
		} catch (CoreException e) {
			outOfSync = true;
		}
		if (outOfSync) {
			try {
				stream = file.getContents(true);
			} catch (CoreException e) {
				return;
			}
		}
		try {
			model.load(stream, outOfSync);
			stream.close();
		} catch (CoreException e) {
			// errors in loading, but we will still
			// initialize.
		} catch (IOException e) {
			PDECore.logException(e);
		}
	}

	private void reloadWorkspaceModel(IPluginModelBase model) {
		loadWorkspaceModel(model);
		fireModelsChanged(new IModel[] { model });
		PDECore.getDefault().getTracingOptionsManager().reset();
	}
	private void reloadModel(IModel model) {
		loadModel(model);
		fireModelsChanged(new IModel[] { model });
	}
	private void reloadBundleModel(IBundlePluginModelBase bmodel) {
		IBundleModel model = bmodel.getBundleModel();
		if (model!=null) {
			loadModel(model);
			//bmodel.reset();
			fireModelsChanged(new IModel[] { bmodel });
		}
	}
	
	private void reloadExtensionsModel(IExtensionsModel model) {
		loadModel(model);
		IModel bmodel = getBundleModel(model.getUnderlyingResource().getProject(), true);
		if (bmodel!=null)
			fireModelsChanged(new IModel[] { bmodel });
	}
	public void removeModelProviderListener(IModelProviderListener listener) {
		listeners.remove(listener);
	}
	private void removeWorkspaceModel(IModel model) {
		// remove
		if (model instanceof IExtensionsModel) {
			IBundlePluginModelBase bmodel = getBundleModel(model.getUnderlyingResource().getProject(), false);
			if (bmodel!=null) {
				bmodel.setExtensionsModel(null);
				if (modelChanges!=null)
					modelChanges.add(new ModelChange(bmodel));
			}
		}
		else if (model instanceof IFragmentModel) {
			if (workspaceFragmentModels != null)
				workspaceFragmentModels.remove(model);
		}
		else if (model instanceof IFeatureModel) {
			if (workspaceFeatureModels != null)
				workspaceFeatureModels.remove(model);
		} else {
			if (workspaceModels != null)
				workspaceModels.remove(model);
		}
		if (modelChanges != null && !(model instanceof IExtensionsModel))
			modelChanges.add(new ModelChange(model, false));
		// disconnect
		IResource element = model.getUnderlyingResource();
		disconnect(element, null);
		if (model instanceof IBundlePluginModelBase) {
			IBundlePluginModelBase bmodel = (IBundlePluginModelBase)model;
			IExtensionsModel emodel = bmodel.getExtensionsModel();
			if (emodel!=null) {
				IResource efile = emodel.getUnderlyingResource();
				disconnect(efile, null);
			}
		}
		if (!(model instanceof IExtensionsModel)) 
			PDECore.getDefault().getTracingOptionsManager().reset();
		model.dispose();
	}
	public void reset() {
		initializeWorkspacePluginModels();
	}

	public void resourceChanged(IResourceChangeEvent event) {
		// No need to do anything if nobody has the models
		if (workspaceModels == null)
			return;

		switch (event.getType()) {
			case IResourceChangeEvent.PRE_AUTO_BUILD :
				if (modelChanges == null)
					modelChanges = new Vector();
				handleResourceDelta(event.getDelta());
				processModelChanges();
				break;
			case IResourceChangeEvent.PRE_CLOSE :
				if (modelChanges == null)
					modelChanges = new Vector();
				// project about to close
				handleProjectClosing((IProject) event.getResource());
				processModelChanges();
				break;
		}
	}

	private void processModelChanges() {
		if (modelChanges.size() == 0) {
			modelChanges = null;
			return;
		}

		Vector added = new Vector();
		Vector removed = new Vector();
		Vector changed = new Vector();
		for (int i = 0; i < modelChanges.size(); i++) {
			ModelChange change = (ModelChange) modelChanges.get(i);
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
		modelChanges = null;
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

	public void shutdown() {
		if (!initialized)
			return;
		IWorkspace workspace = PDECore.getWorkspace();
		workspace.removeResourceChangeListener(this);
		for (Iterator iter = models.values().iterator(); iter.hasNext();) {
			ModelInfo info = (ModelInfo) iter.next();
			if (info.model != null)
				info.model.dispose();
			if (info.readOnlyModel != null)
				info.readOnlyModel.dispose();
			info = null;
		}
		models.clear();
		workspaceModels = null;
		initialized = false;
	}
	private void verifyConsumer(ModelInfo info) {
		//		Object consumer = info.consumer;
		//		if (consumer instanceof PDEMultiPageEditor) {
		//			PDEMultiPageEditor editor = (PDEMultiPageEditor) consumer;
		//			if (isEditorOpened(editor) == false) { // stale reference
		//				info.consumer = null;
		//				info.model.dispose();
		//				info.model = null;
		//			}
		//		}
	}
	public boolean visit(IResourceDelta delta) throws CoreException {
		if (delta != null) {
			IResource resource = delta.getResource();
			if (resource instanceof IProject) {
				handleProjectDelta(delta);
				IProject project = (IProject) resource;
				// If the project is removed, recurse into files
				// to pick up manifest removal
				if (delta.getKind() == IResourceDelta.REMOVED)
					return true;
				return (isPluginProject(project) || isFeatureProject(project));
			} else if (resource instanceof IFile) {
				handleFileDelta(delta);
			}
		}
		return true;
	}

	private void validate() {
		// let's be paranoid - see if the underlying resources
		// are still valid
		validate(workspaceModels);
		validate(workspaceFragmentModels);
		validate(workspaceFeatureModels);
	}

	private void validate(Vector models) {
		if (models == null)
			return;
		Object[] entries = models.toArray();
		for (int i = 0; i < entries.length; i++) {
			IModel model = (IModel) entries[i];
			if (!isValid(model)) {
				// drop it
				models.remove(model);
			}
		}
	}
	private boolean isValid(IModel model) {
		IResource resource = model.getUnderlyingResource();
		// Must have the resource handle
		if (resource == null)
			return false;
		// Must have a resource handle that exists
		if (resource.exists() == false)
			return false;
		// The project must not be closed
		IProject project = resource.getProject();
		if (project == null)
			return false;
		if (project.isOpen() == false)
			return false;
		return hasRootObject(model) && model.isValid();
	}

	private boolean hasRootObject(IModel model) {
		if (model instanceof IPluginModelBase)
			return hasRootObject((IPluginModelBase) model);
		if (model instanceof IFeatureModel)
			return hasRootObject((IFeatureModel) model);
		return false;
	}
	private boolean hasRootObject(IPluginModelBase model) {
		IPluginBase plugin = model.getPluginBase();
		return plugin != null;
	}
	private boolean hasRootObject(IFeatureModel model) {
		IFeature feature = model.getFeature();
		return feature != null;
	}
	private void switchToBundleMode(IBundlePluginModelBase bmodel) {
		// bundle model has been added - remove the plug-in
		// model for the same project
		IProject project = bmodel.getUnderlyingResource().getProject();
		IModel model = getWorkspaceModel(project);
		if (model instanceof IPluginModel || model instanceof IFragmentModel) {
			removeWorkspaceModel(model);
		}
	}
	private void switchToPluginMode(IBundlePluginModelBase bmodel) {
		// bundle model has been removed - add the plug-in
		// model for the same project
		IProject project = bmodel.getUnderlyingResource().getProject();
		IModel model = createWorkspacePluginModel(project);
		if (model!=null) {
			addWorkspaceModel(model);
		}
	}
}