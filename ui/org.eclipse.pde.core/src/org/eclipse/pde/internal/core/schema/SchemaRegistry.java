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
package org.eclipse.pde.internal.core.schema;

import java.io.*;
import java.net.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ischema.*;

public class SchemaRegistry
	implements IModelProviderListener, IResourceChangeListener, IResourceDeltaVisitor {
	public static final String PLUGIN_POINT = "schemaMap";
	public static final String TAG_MAP = "map";
	private Hashtable workspaceDescriptors;
	private Hashtable externalDescriptors;
	private Vector dirtyWorkspaceModels;

	public SchemaRegistry() {
	}
	
	private void addExtensionPoint(IFile file) {
		IProject project = file.getProject();
		IModel model = PDECore.getDefault().getWorkspaceModelManager().getWorkspaceModel(project);
		if (model==null) return;
		if (!(model instanceof IPluginModelBase)) return;
		IPluginModelBase modelBase = (IPluginModelBase)model;
		IPluginExtensionPoint [] points = modelBase.getPluginBase().getExtensionPoints();
		if (points.length==0) return;

		for (int i=0; i<points.length; i++) {
			IPluginExtensionPoint point = points[i];
			IPath path = project.getFullPath();
			String schemaArg = point.getSchema();
			if (schemaArg==null) continue;
			path = path.append(schemaArg);
			IFile schemaFile = file.getWorkspace().getRoot().getFile(path);
			if (file.equals(schemaFile)) {
				// The extension point is referencing this
				// file and it is now added - OK to
				// add the descriptor
				FileSchemaDescriptor sd = new FileSchemaDescriptor(file);
				workspaceDescriptors.put(point.getFullId(), sd);
				return;
			}
		}
	}

	private AbstractSchemaDescriptor getSchemaDescriptor(String extensionPointId) {
		ensureCurrent();
		AbstractSchemaDescriptor descriptor =
			(AbstractSchemaDescriptor) workspaceDescriptors.get(
				extensionPointId);
		if (descriptor == null) {
			// try external
			descriptor =
				(AbstractSchemaDescriptor) externalDescriptors.get(
					extensionPointId);
		}
		if (descriptor != null && descriptor.isEnabled())
			return descriptor;
		return null;
	}

	private void ensureCurrent() {
		if (workspaceDescriptors == null) {
			initializeDescriptors();
		}
		if (dirtyWorkspaceModels != null && dirtyWorkspaceModels.size() > 0) {
			updateWorkspaceDescriptors();
		}
	}

	public ISchema getSchema(String extensionPointId) {
		AbstractSchemaDescriptor descriptor =
			getSchemaDescriptor(extensionPointId);
		if (descriptor == null)
			return null;
		return descriptor.getSchema();
	}

	public ISchema getIncludedSchema(
		ISchemaDescriptor parent,
		String schemaLocation) {
		ensureCurrent();
		Hashtable descriptors = null;

		if (parent instanceof FileSchemaDescriptor)
			descriptors = workspaceDescriptors;
		else if (parent instanceof ExternalSchemaDescriptor)
			descriptors = externalDescriptors;
		if (descriptors == null)
			return null;
		try {
			URL url =
				IncludedSchemaDescriptor.computeURL(
					parent.getSchemaURL(),
					schemaLocation);
			String key = url.toString();
			ISchemaDescriptor desc = (ISchemaDescriptor) descriptors.get(key);
			if (desc == null) {
				desc = new IncludedSchemaDescriptor(parent, schemaLocation);
				descriptors.put(key, desc);
			}
			return desc.getSchema();
		} catch (MalformedURLException e) {
		}
		return null;
	}

	private void initializeDescriptors() {
		workspaceDescriptors = new Hashtable();
		externalDescriptors = new Hashtable();
		// Check workspace plug-ins
		loadWorkspaceDescriptors();
		// Check external plug-ins
		loadExternalDescriptors();

		// Register for further changes
		PDECore
			.getDefault()
			.getWorkspaceModelManager()
			.addModelProviderListener(
			this);
		PDECore.getWorkspace().addResourceChangeListener(this);
	}
	private void loadExternalDescriptors() {
		IExternalModelManager registry =
			PDECore.getDefault().getExternalModelManager();
		IPluginModel[] models = registry.getPluginModels();
		for (int i = 0; i < models.length; i++) {
			IPlugin pluginInfo = models[i].getPlugin();
			IPluginExtensionPoint[] points = pluginInfo.getExtensionPoints();
			for (int j = 0; j < points.length; j++) {
				IPluginExtensionPoint point = points[j];
				if (point.getSchema() != null) {
					ExternalSchemaDescriptor desc =
						new ExternalSchemaDescriptor(point);
					externalDescriptors.put(point.getFullId(), desc);
				}
			}

		}
	}

	private void loadWorkspaceDescriptor(IPluginModelBase model) {
		IPluginBase pluginInfo = model.getPluginBase();
		IPluginExtensionPoint[] points = pluginInfo.getExtensionPoints();
		for (int j = 0; j < points.length; j++) {
			IPluginExtensionPoint point = points[j];
			if (point.getSchema() != null) {
				Object schemaFile = getSchemaFile(point);

				if (schemaFile instanceof IFile) {
					FileSchemaDescriptor desc =
						new FileSchemaDescriptor((IFile) schemaFile);
					workspaceDescriptors.put(point.getFullId(), desc);
				} else if (schemaFile instanceof File) {
					ExternalSchemaDescriptor desc =
						new ExternalSchemaDescriptor(
							(File) schemaFile,
							point.getFullId(),
							true);
					workspaceDescriptors.put(point.getFullId(), desc);
				}
			}
		}
	}
	private void loadWorkspaceDescriptors() {
		NewWorkspaceModelManager manager =
			PDECore.getDefault().getWorkspaceModelManager();
		IPluginModel[] models = manager.getPluginModels();
		for (int i = 0; i < models.length; i++) {
			IPluginModel model = models[i];
			loadWorkspaceDescriptor(model);
		}
		IFragmentModel[] fmodels = manager.getFragmentModels();
		for (int i = 0; i < fmodels.length; i++) {
			IFragmentModel fmodel = fmodels[i];
			loadWorkspaceDescriptor(fmodel);
		}
	}
	private void loadWorkspaceDescriptors(IPluginModelBase model) {
		IPluginBase pluginInfo = model.getPluginBase();
		IPluginExtensionPoint[] points = pluginInfo.getExtensionPoints();
		for (int j = 0; j < points.length; j++) {
			IPluginExtensionPoint point = points[j];
			if (point.getSchema() != null) {
				Object schemaFile = getSchemaFile(point);

				if (schemaFile instanceof IFile) {
					FileSchemaDescriptor desc =
						new FileSchemaDescriptor((IFile) schemaFile);
					workspaceDescriptors.put(point.getFullId(), desc);
				} else if (schemaFile instanceof File) {
					ExternalSchemaDescriptor desc =
						new ExternalSchemaDescriptor(
							(File) schemaFile,
							point.getFullId(),
							true);
					workspaceDescriptors.put(point.getFullId(), desc);
				}
			}
		}
	}

	private Object getSchemaFile(IPluginExtensionPoint point) {
		if (point.getSchema()==null) return null;
		IPluginModelBase model = point.getPluginModel();
		IFile pluginFile = (IFile) model.getUnderlyingResource();
		IPath path = pluginFile.getProject().getFullPath();
		path = path.append(point.getSchema());
		IFile schemaFile = pluginFile.getWorkspace().getRoot().getFile(path);
		if (schemaFile.exists())
			return schemaFile;
		// Does not exist in the plug-in itself - try source location
		SourceLocationManager sourceManager =
			PDECore.getDefault().getSourceLocationManager();
		return sourceManager.findSourceFile(
			model.getPluginBase(),
			new Path(point.getSchema()));
	}

	public void modelsChanged(IModelProviderEvent e) {

		int type = e.getEventTypes();

		if ((type & IModelProviderEvent.MODELS_ADDED) != 0) {
			IModel[] added = e.getAddedModels();
			for (int i = 0; i < added.length; i++) {
				IModel model = added[i];
				if (!(model instanceof IPluginModelBase))
					continue;
				loadWorkspaceDescriptors((IPluginModelBase) model);
			}
		}
		if ((type & IModelProviderEvent.MODELS_REMOVED) != 0) {
			IModel[] removed = e.getRemovedModels();

			for (int i = 0; i < removed.length; i++) {
				IModel model = removed[i];
				if (!(model instanceof IPluginModelBase))
					continue;
				removeWorkspaceDescriptors((IPluginModelBase) model);
			}
		}
		if ((type & IModelProviderEvent.MODELS_CHANGED) != 0) {
			IModel[] changed = e.getChangedModels();
			if (dirtyWorkspaceModels == null)
				dirtyWorkspaceModels = new Vector();
			for (int i = 0; i < changed.length; i++) {
				IModel model = changed[i];
				if (!(model instanceof IPluginModelBase))
					continue;
				dirtyWorkspaceModels.add((IPluginModelBase) model);
			}
		}
	}

	private void removeExtensionPoint(IFile file) {
		for (Enumeration enum = workspaceDescriptors.keys();
			enum.hasMoreElements();
			) {
			String key = (String) enum.nextElement();
			Object desc = workspaceDescriptors.get(key);
			if (desc instanceof FileSchemaDescriptor) {
				FileSchemaDescriptor fd = (FileSchemaDescriptor) desc;
				if (fd.getFile().equals(file)) {
					workspaceDescriptors.remove(key);
					fd.dispose();
					return;
				}
			}
			if (desc instanceof IncludedSchemaDescriptor) {
				IncludedSchemaDescriptor id = (IncludedSchemaDescriptor) desc;
				if (file.equals(id.getFile())) {
					workspaceDescriptors.remove(key);
					id.dispose();
					return;
				}
			}
		}
	}
	
	private void removeWorkspaceDescriptors(IPluginModelBase model) {
		IPluginBase pluginInfo = model.getPluginBase();
		IProject project = model.getUnderlyingResource().getProject();
		IPluginExtensionPoint[] points = pluginInfo.getExtensionPoints();
		for (int i = 0; i < points.length; i++) {
			IPluginExtensionPoint point = points[i];
			Object descObj = workspaceDescriptors.get(point.getFullId());
			if (descObj != null && descObj instanceof FileSchemaDescriptor) {
				FileSchemaDescriptor desc = (FileSchemaDescriptor) descObj;
				IFile schemaFile = desc.getFile();
				if (project.equals(schemaFile.getProject())) {
					// same project - remove
					workspaceDescriptors.remove(point.getFullId());
				}
			}
		}
		// Also remove all included descriptors from the same project
		for (Enumeration enum = workspaceDescriptors.keys();
			enum.hasMoreElements();
			) {
			String key = (String) enum.nextElement();
			Object desc = workspaceDescriptors.get(key);
			if (desc instanceof IncludedSchemaDescriptor) {
				IncludedSchemaDescriptor id = (IncludedSchemaDescriptor) desc;
				IFile file = id.getFile();
				if (file != null && file.getProject().equals(project))
					workspaceDescriptors.remove(key);
				id.dispose();
			}
		}
	}

	public void resourceChanged(IResourceChangeEvent event) {
		if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
			IResourceDelta delta = event.getDelta();
			if (delta != null) {
				try {
					delta.accept(this);
				} catch (CoreException e) {
					PDECore.logException(e);
				}
			}
		}
	}
	public void shutdown() {
		if (workspaceDescriptors == null)
			return;
		disposeDescriptors(workspaceDescriptors);
		disposeDescriptors(externalDescriptors);
		workspaceDescriptors = null;
		externalDescriptors = null;
		dirtyWorkspaceModels = null;
		PDECore
			.getDefault()
			.getWorkspaceModelManager()
			.removeModelProviderListener(
			this);
		PDECore.getWorkspace().removeResourceChangeListener(this);
	}

	private void disposeDescriptors(Hashtable descriptors) {
		for (Iterator iter = descriptors.values().iterator();
			iter.hasNext();
			) {
			AbstractSchemaDescriptor desc =
				(AbstractSchemaDescriptor) iter.next();
			desc.dispose();
		}
		descriptors.clear();
	}

	private void updateExtensionPoint(IFile file) {
		for (Iterator iter = workspaceDescriptors.values().iterator();
			iter.hasNext();
			) {
			AbstractSchemaDescriptor sd =
				(AbstractSchemaDescriptor) iter.next();
			IFile schemaFile = null;
			if (sd instanceof FileSchemaDescriptor) {
				schemaFile = ((FileSchemaDescriptor) sd).getFile();
			} else if (sd instanceof IncludedSchemaDescriptor) {
				schemaFile = ((IncludedSchemaDescriptor) sd).getFile();
			}
			if (schemaFile != null && schemaFile.equals(file)) {
				sd.dispose();
				break;
			}
		}
	}
	private void updateWorkspaceDescriptors() {
		for (int i = 0; i < dirtyWorkspaceModels.size(); i++) {
			IPluginModelBase model =
				(IPluginModelBase) dirtyWorkspaceModels.elementAt(i);
			updateWorkspaceDescriptors(model);
		}
		dirtyWorkspaceModels.clear();
	}
	private void updateWorkspaceDescriptors(IPluginModelBase model) {
		removeWorkspaceDescriptors(model);
		loadWorkspaceDescriptors(model);
	}
	public boolean visit(IResourceDelta delta) throws CoreException {
		IResource resource = delta.getResource();
		if (resource instanceof IFile) {
			IFile file = (IFile) resource;
			String fileName = file.getName().toLowerCase();
			if (!(fileName.endsWith(".exsd") || fileName.endsWith(".mxsd")))
				return true;
			if (NewWorkspaceModelManager.isPluginProject(file.getProject())
				== false)
				return true;
			if (delta.getKind() == IResourceDelta.CHANGED) {
				if ((IResourceDelta.CONTENT & delta.getFlags()) != 0) {
					updateExtensionPoint(file);
				}
			} else if (delta.getKind() == IResourceDelta.ADDED) {
				addExtensionPoint(file);
			} else if (delta.getKind() == IResourceDelta.REMOVED) {
				removeExtensionPoint(file);
			}
		}
		return true;
	}
}
