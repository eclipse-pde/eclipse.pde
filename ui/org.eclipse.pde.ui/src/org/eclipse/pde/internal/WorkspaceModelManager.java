package org.eclipse.pde.internal;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.model.*;
import org.eclipse.pde.internal.model.build.*;
import org.eclipse.ui.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.pde.internal.model.feature.*;
import java.io.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.model.plugin.*;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.core.resources.*;
import java.util.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.internal.wizards.PluginPathUpdater;
import org.eclipse.pde.internal.wizards.imports.UpdateBuildpathWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.internal.preferences.MainPreferencePage;
import org.eclipse.pde.internal.wizards.project.ConvertedProjectWizard;
import org.eclipse.pde.model.*;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.preference.IPreferenceStore;

public class WorkspaceModelManager
	implements IModelProvider, IResourceChangeListener, IResourceDeltaVisitor {
		
	private static final String KEY_MISSING_NATURE_TITLE = "MissingPDENature.title";
	private static final String KEY_MISSING_NATURE_MESSAGE = "MissingPDENature.message";
	private Hashtable models = new Hashtable();
	private Vector listeners = new Vector();
	private Vector workspaceModels = null;
	private Vector workspaceFragmentModels = null;
	private Vector modelChanges = null;
	private boolean startup = true;
	private boolean missingNature=false;

	class ModelChange {
		IModel model;
		boolean added;
		public ModelChange(IModel model, boolean added) {
			this.model = model;
			this.added = added;
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

	public WorkspaceModelManager() {
		super();
	}
	public void addModelProviderListener(IModelProviderListener listener) {
		listeners.add(listener);
	}
	private void addWorkspaceModel(IModel model) {
		if (workspaceModels == null)
			initializeWorkspacePluginModels();
		if (model instanceof IFragmentModel)
			workspaceFragmentModels.add(model);
		else
			workspaceModels.add(model);
		PDEPlugin.getDefault().getTracingOptionsManager().reset();
		if (modelChanges != null)
			modelChanges.add(new ModelChange(model, true));
	}
	private void checkTracing(IFile file) {
		if (file.getName().equals(".options")) {
			PDEPlugin.getDefault().getTracingOptionsManager().reset();
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
	protected IModel createModel(Object element, boolean editable) {
		if (element instanceof IFile) {
			IFile file = (IFile) element;
			String name = file.getName().toLowerCase();
			if (name.equals("plugin.xml")) {
				WorkspacePluginModel model = new WorkspacePluginModel(file);
				model.setEditable(editable);
				return model;
			}
			if (name.equals("fragment.xml")) {
				WorkspaceFragmentModel model = new WorkspaceFragmentModel(file);
				model.setEditable(editable);
				return model;
			}
			if (name.equals("build.properties")) {
				WorkspaceBuildModel model = new WorkspaceBuildModel(file);
				model.setEditable(editable);
				return model;
			}
			if (name.equals("feature.xml")) {
				WorkspaceFeatureModel model = new WorkspaceFeatureModel(file);
				model.setEditable(editable);
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
	private IPluginModelBase createWorkspacePluginModel(IProject project) {
		IPath pluginPath = project.getFullPath().append("plugin.xml");
		IFile pluginFile = project.getWorkspace().getRoot().getFile(pluginPath);
		if (pluginFile.exists() == false) {
			pluginPath = project.getFullPath().append("fragment.xml");
			pluginFile = project.getWorkspace().getRoot().getFile(pluginPath);
		}
		if (pluginFile.exists()) {
			return createWorkspacePluginModel(pluginFile);
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
			if (info.model != null && info.model.getClass().isInstance(modelClass)) {
				return false;
			}
		}
		return true;
	}
	public IFragment[] getFragmentsFor(String pluginId, String version) {
		Vector result = new Vector();
		if (workspaceFragmentModels == null)
			initializeWorkspacePluginModels();
		validate();
		for (int i = 0; i < workspaceFragmentModels.size(); i++) {
			IFragmentModel model = (IFragmentModel) workspaceFragmentModels.elementAt(i);
			IFragment fragment = model.getFragment();
			if (fragment.getPluginId().equals(pluginId)
				&& fragment.getPluginVersion().equals(version)) {
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
	public IFragmentModel[] getWorkspaceFragmentModels() {
		if (workspaceFragmentModels == null) {
			initializeWorkspacePluginModels();
		}
		validate();
		IFragmentModel[] result = new IFragmentModel[workspaceFragmentModels.size()];
		workspaceFragmentModels.copyInto(result);
		return result;
	}
	private IPluginModelBase getWorkspaceModel(IFile file) {
		String name = file.getName().toLowerCase();
		Vector models = null;
		validate();

		if (name.equals("plugin.xml"))
			models = workspaceModels;
		else if (name.equals("fragment.xml"))
			models = workspaceFragmentModels;
		return getWorkspaceModel(file.getProject(), models);
	}
	public IPluginModelBase getWorkspaceModel(IProject project) {
		validate();
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
		return null;
	}
	private IPluginModelBase getWorkspaceModel(IProject project, Vector models) {
		if (models == null)
			return null;
		for (int i = 0; i < models.size(); i++) {
			IPluginModelBase model = (IPluginModelBase) models.elementAt(i);
			IFile file = (IFile) model.getUnderlyingResource();
			if (file.getProject().equals(project)) {
				return model;
			}
		}
		return null;
	}
	public IPluginModel[] getWorkspacePluginModels() {
		if (workspaceModels == null) {
			initializeWorkspacePluginModels();
		}
		validate();
		IPluginModel[] result = new IPluginModel[workspaceModels.size()];
		workspaceModels.copyInto(result);
		return result;
	}
	private void handleFileDelta(IResourceDelta delta) {
		IFile file = (IFile) delta.getResource();
		checkTracing(file);
		if (isSupportedFile(file) == false)
			return;

		if (delta.getKind() == IResourceDelta.ADDED) {
			// manifest added - add the model
			checkForPDENature(file.getProject());
			IPluginModelBase model = getWorkspaceModel(file);
			if (model == null)
				addWorkspaceModel(createWorkspacePluginModel(file));
			else
				reloadWorkspaceModel(model);
		} else {
			IPluginModelBase model = getWorkspaceModel(file);
			if (delta.getKind() == IResourceDelta.REMOVED) {
				// manifest has been removed - ditch the model
				removeWorkspaceModel(model);
			} else if (delta.getKind() == IResourceDelta.CHANGED) {
				if ((IResourceDelta.CONTENT & delta.getFlags()) != 0) {
					// file content modified - sync up
					reloadWorkspaceModel(model);
				}
			}
		}
	}
	
	private void checkForPDENature(IProject project) {
		try {
			if (!project.hasNature(PDEPlugin.PLUGIN_NATURE)) {
				if (MainPreferencePage.isNoPDENature()) missingNature=true;
			}
		}
		catch (CoreException e) {
		}
	}
	
	private void handleProjectClosing(IProject project) {
		// not reason to keep it around if it is closed
		handleProjectToBeDeleted(project);
	}

	private void handleProjectDelta(IResourceDelta delta) {
		IProject project = (IProject) delta.getResource();
		int kind = delta.getKind();

		if (project.isOpen() == false)
			return;
		
		if (kind == IResourceDelta.CHANGED
			&& (delta.getFlags() | IResourceDelta.DESCRIPTION) != 0) {
			// Project description changed. Test if this
			// is now a PDE project and act
			if (isPluginProject(project)) {
				ensureModelExists(project);
			}
		}
	}

	private void handleProjectToBeDeleted(IProject project) {
		if (isPluginProject(project) == false) {
			return;
		}
		IPluginModelBase model = getWorkspaceModel(project);
		if (model != null) {
			removeWorkspaceModel(model);
		}
	}
	private void handleResourceDelta(IResourceDelta delta) {
		try {
			delta.accept(this);
			if (missingNature) {
				handleMissingNature();
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	private void initializeWorkspacePluginModels() {
		workspaceModels = new Vector();
		workspaceFragmentModels = new Vector();
		IWorkspace workspace = PDEPlugin.getWorkspace();
		IProject[] projects = workspace.getRoot().getProjects();
		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			if (!project.isOpen()) continue;
			try {
				if (project.hasNature(JavaCore.NATURE_ID)) {
					IPluginModelBase model = createWorkspacePluginModel(project);
					if (model != null) {
						if (model.isFragmentModel())
							workspaceFragmentModels.add(model);
						else
							workspaceModels.add(model);
					}
				}
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
		workspace.addResourceChangeListener(
			this,
			IResourceChangeEvent.PRE_CLOSE
				| IResourceChangeEvent.PRE_DELETE
				| IResourceChangeEvent.PRE_AUTO_BUILD);
		//workspace.addResourceChangeListener(this);
		initialized = true;
	}
	private boolean isEditorOpened(PDEMultiPageEditor pdeEditor) {
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
		for (int i = 0; i < windows.length; i++) {
			IWorkbenchWindow window = windows[i];
			IWorkbenchPage[] pages = window.getPages();
			for (int j = 0; j < pages.length; j++) {
				IWorkbenchPage page = pages[j];
				IEditorPart[] editors = page.getEditors();
				for (int k = 0; k < editors.length; k++) {
					IEditorPart editor = editors[k];
					if (editor == pdeEditor) {
						return true;
					}
				}
			}
		}
		return false;
	}
	public static boolean isPluginProject(IProject project) {
		if (project.isOpen() == false)
			return false;
		try {
			if (!project.hasNature(JavaCore.NATURE_ID)) return false;
			
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		return project.exists(new Path("plugin.xml")) || project.exists(new Path("fragment.xml"));
	}

	private void ensureModelExists(IProject pluginProject) {
		if (!initialized)
			return;
		IPluginModelBase model = getWorkspaceModel(pluginProject);
		if (model == null) {
			model = createWorkspacePluginModel(pluginProject);
			if (model != null) {
				addWorkspaceModel(model);
			}
		}
	}

	private boolean isSupportedFile(IFile file) {
		String name = file.getName().toLowerCase();
		if (!name.equals("plugin.xml") && !name.equals("fragment.xml"))
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
		}
		catch (CoreException e) {
			outOfSync = true;
		}
		if (outOfSync) {
			try {
				stream = file.getContents(true);
			}
			catch (CoreException e) {
				// cannot get file contents - something is 
				// seriously wrong
				IPluginBase base = model.getPluginBase(true);
				try {
					base.setId(file.getProject().getName());
					base.setName(base.getId());
					base.setVersion("0.0.0");
					PDEPlugin.log(e);
				}
				catch (CoreException ex) {
					PDEPlugin.logException(ex);
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
			PDEPlugin.logException(e);
		}
	}
	private void reloadWorkspaceModel(IPluginModelBase model) {
		loadWorkspaceModel(model);
		fireModelsChanged(new IModel[] { model });
		PDEPlugin.getDefault().getTracingOptionsManager().reset();
	}
	public void removeModelProviderListener(IModelProviderListener listener) {
		listeners.remove(listener);
	}
	private void removeWorkspaceModel(IPluginModelBase model) {
		// remove
		if (model instanceof IFragmentModel) {
			if (workspaceFragmentModels != null)
				workspaceFragmentModels.remove(model);
		} else {
			if (workspaceModels != null)
				workspaceModels.remove(model);
		}
		if (modelChanges != null)
			modelChanges.add(new ModelChange(model, false));
		// disconnect
		IResource element = model.getUnderlyingResource();
		disconnect(element, null);
		PDEPlugin.getDefault().getTracingOptionsManager().reset();
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
			case IResourceChangeEvent.PRE_DELETE :
				// project about to be deleted
				if (modelChanges == null)
					modelChanges = new Vector();
				handleProjectToBeDeleted((IProject) event.getResource());
				processModelChanges();
				break;
		}
	}

	private void processModelChanges() {
		if (modelChanges.size()==0) {
			modelChanges = null;
			return;
		}
		
		Vector added = new Vector();
		Vector removed = new Vector();
		for (int i = 0; i < modelChanges.size(); i++) {
			ModelChange change = (ModelChange) modelChanges.get(i);
			if (change.added)
				added.add(change.model);
			else
				removed.add(change.model);
		}
		IModel[] addedArray =
			added.size() > 0
				? (IModel[]) added.toArray(new IModel[added.size()])
				: (IModel[]) null;
		IModel[] removedArray =
			removed.size() > 0
				? (IModel[]) removed.toArray(new IModel[removed.size()])
				: (IModel[]) null;
		int type = 0;
		if (addedArray != null)
			type |= IModelProviderEvent.MODELS_ADDED;
		if (removedArray != null)
			type |= IModelProviderEvent.MODELS_REMOVED;
		modelChanges = null;
		if (type != 0) {
			final ModelProviderEvent event =
				new ModelProviderEvent(this, type, addedArray, removedArray, null);
			fireModelProviderEvent(event);
		}
	}

	public void shutdown() {
		if (!initialized)
			return;
		IWorkspace workspace = PDEPlugin.getWorkspace();
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
		Object consumer = info.consumer;
		if (consumer instanceof PDEMultiPageEditor) {
			PDEMultiPageEditor editor = (PDEMultiPageEditor) consumer;
			if (isEditorOpened(editor) == false) { // stale reference
				info.consumer = null;
				info.model.dispose();
				info.model = null;
			}
		}
	}
	public boolean visit(IResourceDelta delta) throws CoreException {
		if (delta != null) {
			IResource resource = delta.getResource();
			if (resource instanceof IProject) {
				handleProjectDelta(delta);
				return isPluginProject((IProject) resource);
			} else if (resource instanceof IFile) {
				handleFileDelta(delta);
			}
		}
		return true;
	}
	
	private void handleMissingNature() {
		if (missingNature) {
			NoPDENatureDialog dialog = new NoPDENatureDialog(PDEPlugin.getActiveWorkbenchShell(),
					PDEPlugin.getResourceString(KEY_MISSING_NATURE_TITLE),
					PDEPlugin.getResourceString(KEY_MISSING_NATURE_MESSAGE));
			dialog.open();
			int result = dialog.getResult();
			if (result==NoPDENatureDialog.STOP_WARNING) {
				IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
				store.setValue(MainPreferencePage.PROP_NO_PDE_NATURE, false);
			}
			else if (result==NoPDENatureDialog.OPEN_WIZARD) {
				ConvertedProjectWizard wizard = new ConvertedProjectWizard();
				wizard.init(PlatformUI.getWorkbench(), new StructuredSelection());
				WizardDialog wdialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
				wdialog.open();
			}
		}
		missingNature = false;
	}
	
	private void validate() {
		// let's be paranoid - see if the underlying resources
		// are still valid
		if (workspaceModels!=null) {
			validate(workspaceModels);
		}
		if (workspaceFragmentModels!=null) {
			validate(workspaceFragmentModels);
		}
	}
	private void validate(Vector models) {
		Object [] entries = models.toArray();
		for (int i=0; i<entries.length; i++) {
			IPluginModelBase model = (IPluginModelBase)entries[i];
			if (!isValid(model)) {
				// drop it
				models.remove(model);
			}
		}
	}
	private boolean isValid(IPluginModelBase model) {
		IResource resource = model.getUnderlyingResource();
		// Must have the resource handle
		if (resource==null) return false;
		// Must have a resource handle that exists
		if (resource.exists()==false) return false;
		// The project must not be closed
		IProject project = resource.getProject();
		if (project==null) return false;
		if (project.isOpen()==false) return false;
		// passed
		return true;
	}
}