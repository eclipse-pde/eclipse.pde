/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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
package org.eclipse.pde.internal.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.ListIterator;
import java.util.Map;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelProviderEvent;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ISharedExtensionsModel;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.core.builders.SchemaTransformer;
import org.eclipse.pde.internal.core.bundle.BundleFragmentModel;
import org.eclipse.pde.internal.core.bundle.BundlePluginModel;
import org.eclipse.pde.internal.core.bundle.WorkspaceBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaDescriptor;
import org.eclipse.pde.internal.core.plugin.WorkspaceExtensionsModel;
import org.eclipse.pde.internal.core.plugin.WorkspaceFragmentModel;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModel;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.schema.SchemaDescriptor;
import org.osgi.framework.Constants;

public class WorkspacePluginModelManager extends WorkspaceModelManager {

	@SuppressWarnings("deprecation")
	private static final Collection<String> RELEVANT_HEADERS = Collections
			.unmodifiableCollection(new HashSet<>(Arrays.asList( //
					Constants.BUNDLE_MANIFESTVERSION, //
					Constants.BUNDLE_SYMBOLICNAME, //
					ICoreConstants.AUTOMATIC_MODULE_NAME, //
					Constants.BUNDLE_VERSION, //
					Constants.FRAGMENT_HOST, //
					IPDEBuildConstants.EXTENSIBLE_API, //
					IPDEBuildConstants.PATCH_FRAGMENT, //
					Constants.REQUIRE_BUNDLE, //
					Constants.IMPORT_PACKAGE, //
					Constants.EXPORT_PACKAGE, //
					ICoreConstants.PROVIDE_PACKAGE, //
					ICoreConstants.ECLIPSE_JREBUNDLE, //
					Constants.BUNDLE_CLASSPATH, //
					Constants.PROVIDE_CAPABILITY, //
					Constants.REQUIRE_CAPABILITY, //
					ICoreConstants.ECLIPSE_GENERIC_CAPABILITY, //
					ICoreConstants.ECLIPSE_GENERIC_REQUIRED, //
					Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, //
					IPDEBuildConstants.ECLIPSE_PLATFORM_FILTER, //
					ICoreConstants.ECLIPSE_SYSTEM_BUNDLE, //
					ICoreConstants.ECLIPSE_SOURCE_BUNDLE)));

	private final ArrayList<IExtensionDeltaListener> fExtensionListeners = new ArrayList<>();
	private ArrayList<ModelChange> fChangedExtensions = null;

	/**
	 * The workspace plug-in model manager is only interested
	 * in changes to plug-in projects.
	 */
	@Override
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
	@Override
	protected void createModel(IProject project, boolean notify) {
		IPluginModelBase model = null;
		IFile manifest = PDEProject.getManifest(project);
		IFile pluginXml = PDEProject.getPluginXml(project);
		IFile fragmentXml = PDEProject.getFragmentXml(project);
		if (manifest.exists()) {
			WorkspaceBundleModel bmodel = new WorkspaceBundleModel(manifest);
			loadModel(bmodel, false);
			if (bmodel.isFragmentModel()) {
				model = new BundleFragmentModel();
			} else {
				model = new BundlePluginModel();
			}
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

		if (PDEProject.getOptionsFile(project).exists()) {
			PDECore.getDefault().getTracingOptionsManager().reset();
		}

		if (model != null) {
			if (fModels == null) {
				fModels = new LinkedHashMap<>();
			}
			fModels.put(project, model);
			if (notify) {
				addChange(model, IModelProviderEvent.MODELS_ADDED);
			}
		}
	}

	/**
	 * Reacts to changes in files of interest to PDE
	 */
	@Override
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
	 * @param schemaFile
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
		try (OutputStream os = new FileOutputStream(schemaPreviewFile)) {
			PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8), true);
			transformer.transform(schema, printWriter);
			os.flush();
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
	 * <li>If the file has been deleted, switch to the old-style plug-in if a
	 * plugin.xml file exists</li>
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
				Map<String, IManifestHeader> oldHeaders = bmodel.getBundle().getManifestHeaders();

				loadModel(bmodel, true);
				String newLocalization = ((IBundlePluginModelBase) model).getBundleLocalization();

				// Fragment-Host header was added or removed
				if (wasFragment != bmodel.isFragmentModel()) {
					removeModel(project);
					createModel(project, true);
				} else {
					if (model instanceof AbstractNLModel && (oldLocalization != null && (newLocalization == null || !oldLocalization.equals(newLocalization))) || (newLocalization != null && (oldLocalization == null || !newLocalization.equals(oldLocalization)))) {
						((AbstractNLModel) model).resetNLResourceHelper();
					}

					Map<String, IManifestHeader> newHeaders = bmodel.getBundle().getManifestHeaders();
					if (hasModelChanged(oldHeaders, newHeaders)) {
						addChange(model, IModelProviderEvent.MODELS_CHANGED);
					}
				}
			}
		}
	}

	private boolean hasModelChanged(Map<String, IManifestHeader> oldHeaders, Map<String, IManifestHeader> newHeaders) {
		oldHeaders.keySet().retainAll(RELEVANT_HEADERS);
		newHeaders.keySet().retainAll(RELEVANT_HEADERS);

		if (oldHeaders.size() != newHeaders.size()) {
			return true;
		}

		for (Map.Entry<String, IManifestHeader> entry : oldHeaders.entrySet()) {
			String key = entry.getKey();
			IManifestHeader oldValue = entry.getValue();
			IManifestHeader newValue = newHeaders.get(key);

			if (newValue == null) {
				return true;
			}

			if (!oldValue.getValue().equals(newValue.getValue())) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Removes the model associated with the given project from the table,
	 * if the given project is a plug-in project
	 */
	@Override
	protected Object removeModel(IProject project) {
		Object model = super.removeModel(project);
		if (model != null && PDEProject.getOptionsFile(project).exists()) {
			PDECore.getDefault().getTracingOptionsManager().reset();
		}
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
		return fModels.values().toArray(new IPluginModelBase[fModels.size()]);
	}

	/**
	 * Adds listeners to the workspace and to the java model
	 * to be notified of PRE_CLOSE events and POST_CHANGE events.
	 */
	@Override
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
	@Override
	protected void removeListeners() {
		PDECore.getWorkspace().removeResourceChangeListener(this);
		JavaCore.removePreProcessingResourceChangedListener(this);
		if (!fExtensionListeners.isEmpty()) {
			fExtensionListeners.clear();
		}
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
	@Override
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
	protected void setModels(IPluginModelBase[] models) {
		fModels = Collections.synchronizedMap(new LinkedHashMap<IProject, IModel>());
		for (IPluginModelBase model : models) {
			IProject project = model.getUnderlyingResource().getProject();
			fModels.put(project, model);
		}
		IProject[] projects = PDECore.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projects.length; i++) {
			// if any projects contained Manifest files and were not included in the PDEState,
			// we should create models for them now
			if (!fModels.containsKey(projects[i]) && isInterestingProject(projects[i])) {
				createModel(projects[i], false);
			}
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
		ArrayList<URL> list = new ArrayList<>();
		IProject[] projects = PDECore.getWorkspace().getRoot().getProjects();
		for (final IProject project : projects) {
			if (isPluginProject(project)) {
				try {
					final IPath path = project.getLocation();
					if (path != null) {
						list.add(path.toFile().toURL());
					}
				} catch (MalformedURLException e) {
				}
			}
		}
		return list.toArray(new URL[list.size()]);
	}

	void addExtensionDeltaListener(IExtensionDeltaListener listener) {
		if (!fExtensionListeners.contains(listener)) {
			fExtensionListeners.add(listener);
		}
	}

	void removeExtensionDeltaListener(IExtensionDeltaListener listener) {
		fExtensionListeners.remove(listener);
	}

	public void fireExtensionDeltaEvent(IExtensionDeltaEvent event) {
		for (ListIterator<IExtensionDeltaListener> li = fExtensionListeners.listIterator(); li.hasNext();) {
			li.next().extensionsChanged(event);
		}
	}

	@Override
	protected void processModelChanges() {
		// process model changes first so model manager is accurate when we process extension events - bug 209155
		super.processModelChanges();
		processModelChanges("org.eclipse.pde.internal.core.IExtensionDeltaEvent", fChangedExtensions); //$NON-NLS-1$
		fChangedExtensions = null;
	}

	@Override
	protected void createAndFireEvent(String eventId, int type, Collection<IModel> added, Collection<IModel> removed, Collection<IModel> changed) {
		if (eventId.equals("org.eclipse.pde.internal.core.IExtensionDeltaEvent")) { //$NON-NLS-1$
			IExtensionDeltaEvent event = new ExtensionDeltaEvent(type, added.toArray(new IPluginModelBase[added.size()]), removed.toArray(new IPluginModelBase[removed.size()]), changed.toArray(new IPluginModelBase[changed.size()]));
			fireExtensionDeltaEvent(event);
		} else {
			super.createAndFireEvent(eventId, type, added, removed, changed);
		}
	}

	protected void addExtensionChange(IPluginModelBase plugin, int type) {
		if (fChangedExtensions == null) {
			fChangedExtensions = new ArrayList<>();
		}
		ModelChange change = new ModelChange(plugin, type);
		fChangedExtensions.add(change);
	}
}
