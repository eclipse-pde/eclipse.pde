/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.core.IModelProviderEvent;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ISharedExtensionsModel;
import org.eclipse.pde.internal.core.builders.SchemaTransformer;
import org.eclipse.pde.internal.core.bundle.*;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaDescriptor;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.schema.SchemaDescriptor;

public class WorkspacePluginModelManager extends WorkspaceModelManager {

	private ArrayList fExtensionListeners = new ArrayList();
	private ArrayList fChangedExtensions = null;

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
		IFile manifest = PDEProject.getManifest(project);
		IFile pluginXml = PDEProject.getPluginXml(project);
		IFile fragmentXml = PDEProject.getFragmentXml(project);
		if (manifest.exists()) {
			WorkspaceBundleModel bmodel = new WorkspaceBundleModel(manifest);
			loadModel(bmodel, false);
			if (bmodel.isFragmentModel())
				model = new BundleFragmentModel();
			else
				model = new BundlePluginModel();
			model.setEnabled(true);
			bmodel.setEditable(false);
			((IBundlePluginModelBase) model).setBundleModel(bmodel);

			IFile efile = bmodel.isFragmentModel() ? fragmentXml : pluginXml;
			if (efile.exists()) {
				WorkspaceExtensionsModel extModel = new WorkspaceExtensionsModel(efile);
				extModel.setEditable(false);
				loadModel(extModel, false);
				((IBundlePluginModelBase) model).setExtensionsModel(extModel);
				extModel.setBundleModel((IBundlePluginModelBase) model);
			}

		} else if (pluginXml.exists()) {
			model = new WorkspacePluginModel(pluginXml, true);
			loadModel(model, false);
		} else if (fragmentXml.exists()) {
			model = new WorkspaceFragmentModel(fragmentXml, true);
			loadModel(model, false);
		}

		if (PDEProject.getOptionsFile(project).exists())
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
		IFile file = (IFile) delta.getResource();
		IProject project = file.getProject();
		String filename = file.getName();
		if (file.equals(PDEProject.getOptionsFile(project))) {
			PDECore.getDefault().getTracingOptionsManager().reset();
		} else if (file.equals(PDEProject.getBuildProperties(project))) {
			// change in build.properties should trigger a Classpath Update
			// we therefore fire a notification
			//TODO this is inefficient.  we could do better.
			Object model = getModel(project);
			if (model != null) {
				addChange(model, IModelProviderEvent.MODELS_CHANGED);
			}
		} else if (file.equals(PDEProject.getLocalizationFile(project))) {
			// reset bundle resource if localization file has changed.
			IPluginModelBase model = getPluginModel(project);
			if (model != null) {
				((AbstractNLModel) model).resetNLResourceHelper();
			}
		} else if (filename.endsWith(".exsd")) { //$NON-NLS-1$
			handleEclipseSchemaDelta(file, delta);
		} else {
			if (file.equals(PDEProject.getPluginXml(project)) || file.equals(PDEProject.getFragmentXml(project))) {
				handleExtensionFileDelta(file, delta);
			} else if (file.equals(PDEProject.getManifest(project))) {
				handleBundleManifestDelta(file, delta);
			}
		}
	}

	/**
	 * @param file
	 * @param delta
	 */
	private void handleEclipseSchemaDelta(IFile schemaFile, IResourceDelta delta) {
		// Get the kind of resource delta
		int kind = delta.getKind();
		// We are only interested in schema files whose contents have changed
		if (kind != IResourceDelta.CHANGED) {
			return;
		} else if ((IResourceDelta.CONTENT & delta.getFlags()) == 0) {
			return;
		}
		// Get the schema preview file session property
		Object property = null;
		try {
			property = schemaFile.getSessionProperty(PDECore.SCHEMA_PREVIEW_FILE);
		} catch (CoreException e) {
			// Ignore
			return;
		}
		// Check if the schema file has an associated HTML schema preview file
		// (That is, whether a show description action has been executed before)
		// Property set in
		// org.eclipse.pde.internal.ui.search.ShowDescriptionAction.linkPreviewFileToSchemaFile()
		if (property == null) {
			return;
		} else if ((property instanceof File) == false) {
			return;
		}
		File schemaPreviewFile = (File) property;
		// Ensure the file exists and is writable
		if (schemaPreviewFile.exists() == false) {
			return;
		} else if (schemaPreviewFile.isFile() == false) {
			return;
		} else if (schemaPreviewFile.canWrite() == false) {
			return;
		}
		// Get the schema model object
		ISchemaDescriptor descriptor = new SchemaDescriptor(schemaFile, false);
		ISchema schema = descriptor.getSchema(false);

		try {
			// Re-generate the schema preview file contents in order to reflect
			// the changes in the schema
			recreateSchemaPreviewFileContents(schemaPreviewFile, schema);
		} catch (IOException e) {
			// Ignore
		}
	}

	/**
	 * @param schemaPreviewFile
	 * @param schema
	 * @throws IOException
	 */
	private void recreateSchemaPreviewFileContents(File schemaPreviewFile, ISchema schema) throws IOException {
		SchemaTransformer transformer = new SchemaTransformer();
		OutputStream os = new FileOutputStream(schemaPreviewFile);
		PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(os, ICoreConstants.UTF_8), true);
		transformer.transform(schema, printWriter);
		os.flush();
		os.close();
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
		IPluginModelBase model = (IPluginModelBase) getModel(file.getProject());
		if (kind == IResourceDelta.REMOVED) {
			if (model instanceof IBundlePluginModelBase) {
				((IBundlePluginModelBase) model).setExtensionsModel(null);
				addExtensionChange(model, IModelProviderEvent.MODELS_REMOVED);
			} else {
				removeModel(file.getProject());
			}
		} else if (kind == IResourceDelta.ADDED) {
			if (model instanceof IBundlePluginModelBase) {
				WorkspaceExtensionsModel extensions = new WorkspaceExtensionsModel(file);
				extensions.setEditable(false);
				((IBundlePluginModelBase) model).setExtensionsModel(extensions);
				extensions.setBundleModel((IBundlePluginModelBase) model);
				loadModel(extensions, false);
				addExtensionChange(model, IModelProviderEvent.MODELS_ADDED);
			} else {
				createModel(file.getProject(), true);
			}
		} else if (kind == IResourceDelta.CHANGED && (IResourceDelta.CONTENT & delta.getFlags()) != 0) {
			if (model instanceof IBundlePluginModelBase) {
				ISharedExtensionsModel extensions = ((IBundlePluginModelBase) model).getExtensionsModel();
				boolean reload = extensions != null;
				if (extensions == null) {
					extensions = new WorkspaceExtensionsModel(file);
					((WorkspaceExtensionsModel) extensions).setEditable(false);
					((IBundlePluginModelBase) model).setExtensionsModel(extensions);
					((WorkspaceExtensionsModel) extensions).setBundleModel((IBundlePluginModelBase) model);
				}
				loadModel(extensions, reload);
			} else if (model != null) {
				loadModel(model, true);
				addChange(model, IModelProviderEvent.MODELS_CHANGED);
			}
			addExtensionChange(model, IModelProviderEvent.MODELS_CHANGED);
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
		} else if (kind == IResourceDelta.CHANGED && (IResourceDelta.CONTENT & delta.getFlags()) != 0) {
			if (model instanceof IBundlePluginModelBase) {
				// check to see if localization changed (bug 146912)
				String oldLocalization = ((IBundlePluginModelBase) model).getBundleLocalization();
				IBundleModel bmodel = ((IBundlePluginModelBase) model).getBundleModel();
				boolean wasFragment = bmodel.isFragmentModel();
				loadModel(bmodel, true);
				String newLocalization = ((IBundlePluginModelBase) model).getBundleLocalization();

				// Fragment-Host header was added or removed
				if (wasFragment != bmodel.isFragmentModel()) {
					removeModel(project);
					createModel(project, true);
				} else {
					if (model instanceof AbstractNLModel && (oldLocalization != null && (newLocalization == null || !oldLocalization.equals(newLocalization))) || (newLocalization != null && (oldLocalization == null || !newLocalization.equals(oldLocalization))))
						((AbstractNLModel) model).resetNLResourceHelper();
					addChange(model, IModelProviderEvent.MODELS_CHANGED);
				}
			}
		}
	}

	/**
	 * Removes the model associated with the given project from the table,
	 * if the given project is a plug-in project
	 */
	protected Object removeModel(IProject project) {
		Object model = super.removeModel(project);
		if (model != null && PDEProject.getOptionsFile(project).exists())
			PDECore.getDefault().getTracingOptionsManager().reset();
		if (model instanceof IPluginModelBase) {
			// PluginModelManager will remove IPluginModelBase form ModelEntry before triggering IModelChangedEvent
			// Therefore, if we want to track a removed model we need to create an entry for it in the ExtensionDeltaEvent
			//			String id = ((IPluginModelBase)model).getPluginBase().getId();
			//			ModelEntry entry = PluginRegistry.findEntry(id);
			//			if (entry.getWorkspaceModels().length + entry.getExternalModels().length < 2)
			addExtensionChange((IPluginModelBase) model, IModelProviderEvent.MODELS_REMOVED);
		}
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
		return (IPluginModelBase) getModel(project);
	}

	/**
	 * Returns a list of all workspace plug-in models
	 * 
	 * @return an array of workspace plug-in models
	 */
	protected IPluginModelBase[] getPluginModels() {
		initialize();
		return (IPluginModelBase[]) fModels.values().toArray(new IPluginModelBase[fModels.size()]);
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
		if (fExtensionListeners.size() > 0)
			fExtensionListeners.clear();
		super.removeListeners();
	}

	/**
	 * Returns true if the folder being visited is of interest to PDE.
	 * In this case, PDE is only interested in META-INF folders at the root of a plug-in project
	 * We are also interested in schema folders
	 * 
	 * @return <code>true</code> if the folder (and its children) is of interest to PDE;
	 * <code>false</code> otherwise.
	 * 
	 */
	protected boolean isInterestingFolder(IFolder folder) {
		IContainer root = PDEProject.getBundleRoot(folder.getProject());
		if (folder.getProjectRelativePath().isPrefixOf(root.getProjectRelativePath())) {
			return true;
		}
		String folderName = folder.getName();
		if (("META-INF".equals(folderName) || "OSGI-INF".equals(folderName) || "schema".equals(folderName)) && folder.getParent().equals(root)) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			return true;
		}
		if ("OSGI-INF/l10n".equals(folder.getProjectRelativePath().toString())) { //$NON-NLS-1$
			return true;
		}
		return false;
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
		IProject[] projects = PDECore.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projects.length; i++) {
			// if any projects contained Manifest files and were not included in the PDEState,
			// we should create models for them now
			if (!fModels.containsKey(projects[i]) && isInterestingProject(projects[i]))
				createModel(projects[i], false);
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
		return (URL[]) list.toArray(new URL[list.size()]);
	}

	void addExtensionDeltaListener(IExtensionDeltaListener listener) {
		if (!fExtensionListeners.contains(listener))
			fExtensionListeners.add(listener);
	}

	void removeExtensionDeltaListener(IExtensionDeltaListener listener) {
		fExtensionListeners.remove(listener);
	}

	public void fireExtensionDeltaEvent(IExtensionDeltaEvent event) {
		for (ListIterator li = fExtensionListeners.listIterator(); li.hasNext();) {
			((IExtensionDeltaListener) li.next()).extensionsChanged(event);
		}
	}

	protected void processModelChanges() {
		// process model changes first so model manager is accurate when we process extension events - bug 209155
		super.processModelChanges();
		processModelChanges("org.eclipse.pde.internal.core.IExtensionDeltaEvent", fChangedExtensions); //$NON-NLS-1$
		fChangedExtensions = null;
	}

	protected void createAndFireEvent(String eventId, int type, Collection added, Collection removed, Collection changed) {
		if (eventId.equals("org.eclipse.pde.internal.core.IExtensionDeltaEvent")) { //$NON-NLS-1$
			IExtensionDeltaEvent event = new ExtensionDeltaEvent(type, (IPluginModelBase[]) added.toArray(new IPluginModelBase[added.size()]), (IPluginModelBase[]) removed.toArray(new IPluginModelBase[removed.size()]), (IPluginModelBase[]) changed.toArray(new IPluginModelBase[changed.size()]));
			fireExtensionDeltaEvent(event);
		} else
			super.createAndFireEvent(eventId, type, added, removed, changed);
	}

	protected void addExtensionChange(IPluginModelBase plugin, int type) {
		if (fChangedExtensions == null)
			fChangedExtensions = new ArrayList();
		ModelChange change = new ModelChange(plugin, type);
		fChangedExtensions.add(change);
	}
}
