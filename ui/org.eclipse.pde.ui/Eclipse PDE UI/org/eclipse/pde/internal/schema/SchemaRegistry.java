package org.eclipse.pde.internal.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.core.runtime.IPluginRegistry;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.pde.internal.base.model.*;
import java.util.*;
import org.eclipse.pde.internal.*;
import org.eclipse.core.runtime.Platform;


public class SchemaRegistry implements IModelProviderListener, IResourceChangeListener, IResourceDeltaVisitor {
	public static final String PLUGIN_POINT = "schemaMap";
	public static final String TAG_MAP = "map";
	private Hashtable descriptors;
	private Vector dirtyWorkspaceModels;

public SchemaRegistry() {
}
private void addExtensionPoint(IFile file) {
	FileSchemaDescriptor sd = new FileSchemaDescriptor(file);
	descriptors.put(sd.getPointId(), sd);
}
public ISchema getSchema(String extensionPointId) {
	if (descriptors == null) {
		initializeDescriptors();
	}
	if (dirtyWorkspaceModels!=null && dirtyWorkspaceModels.size()>0) {
		updateWorkspaceDescriptors();
	}
	AbstractSchemaDescriptor descriptor =
		(AbstractSchemaDescriptor) descriptors.get(extensionPointId);
	if (descriptor==null) return null;
	return descriptor.getSchema();
}
private void initializeDescriptors() {
	descriptors = new Hashtable();
	// Check workspace plug-ins
	loadWorkspaceDescriptors();
	// Check external plug-ins
	loadExternalDescriptors();
	// Now read the registry and accept schema maps
	loadMappedDescriptors();
	// Register for further changes
	PDEPlugin.getDefault().getWorkspaceModelManager().addModelProviderListener(this);
	PDEPlugin.getWorkspace().addResourceChangeListener(this);
}
private void loadExternalDescriptors() {
	ExternalModelManager registry = PDEPlugin.getDefault().getExternalModelManager();
	for (int i = 0; i < registry.getPluginCount(); i++) {
		IPlugin pluginInfo = registry.getPlugin(i);
		IPluginExtensionPoint[] points = pluginInfo.getExtensionPoints();
		for (int j = 0; j < points.length; j++) {
			IPluginExtensionPoint point = points[j];
			if (point.getSchema() != null) {
				ExtensionPointSchemaDescriptor desc = new ExtensionPointSchemaDescriptor(point);
				descriptors.put(point.getFullId(), desc);
			}
		}

	}
}
private void loadMappedDescriptors() {
	IWorkspace workspace = PDEPlugin.getWorkspace();
	IPluginRegistry registry = Platform.getPluginRegistry();
	org.eclipse.core.runtime.IExtensionPoint point =
		registry.getExtensionPoint(PDEPlugin.getPluginId(), PLUGIN_POINT);
	if (point == null)
		return;

	IExtension[] extensions = point.getExtensions();
	for (int i = 0; i < extensions.length; i++) {
		IConfigurationElement[] elements = extensions[i].getConfigurationElements();
		for (int j = 0; j < elements.length; j++) {
			IConfigurationElement config = elements[j];
			processMapElement(config);
		}
	}
}
private void loadWorkspaceDescriptor(IPluginModelBase model) {
	IPluginBase pluginInfo = model.getPluginBase();
	IPluginExtensionPoint[] points = pluginInfo.getExtensionPoints();
	for (int j = 0; j < points.length; j++) {
		IPluginExtensionPoint point = points[j];
		if (point.getSchema() != null) {
			IFile pluginFile = (IFile) model.getUnderlyingResource();
			IPath path = pluginFile.getProject().getFullPath();
			path = path.append(point.getSchema());
			IFile schemaFile = pluginFile.getWorkspace().getRoot().getFile(path);
			if (schemaFile.exists()) {
				FileSchemaDescriptor desc = new FileSchemaDescriptor(schemaFile);
				descriptors.put(point.getFullId(), desc);
			}
		}
	}
}
private void loadWorkspaceDescriptors() {
	WorkspaceModelManager manager =
		PDEPlugin.getDefault().getWorkspaceModelManager();
	IPluginModel[] models = manager.getWorkspacePluginModels();
	for (int i = 0; i < models.length; i++) {
		IPluginModel model = models[i];
		loadWorkspaceDescriptor(model);
	}
	IFragmentModel[] fmodels = manager.getWorkspaceFragmentModels();
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
			IFile pluginFile = (IFile) model.getUnderlyingResource();
			IPath path = pluginFile.getProject().getFullPath();
			path = path.append(point.getSchema());
			IFile schemaFile = pluginFile.getWorkspace().getRoot().getFile(path);
			if (schemaFile.exists()) {
				FileSchemaDescriptor desc = new FileSchemaDescriptor(schemaFile);
				descriptors.put(point.getFullId(), desc);
			}
		}
	}
}
public void modelsChanged(IModelProviderEvent e) {
	IPluginModelBase model = (IPluginModelBase)e.getAffectedModel();
	switch (e.getEventType()) {
		case IModelProviderEvent.POST_MODEL_ADDED :
			loadWorkspaceDescriptors(model);
			break;
		case IModelProviderEvent.PRE_MODEL_REMOVED :
			removeWorkspaceDescriptors(model);
			break;
		case IModelProviderEvent.MODEL_CHANGED :
			if (dirtyWorkspaceModels==null) dirtyWorkspaceModels = new Vector();
			dirtyWorkspaceModels.add(model);
			break;
	}
}
private void processMapElement(IConfigurationElement element) {
	String tag = element.getName();
	if (tag.equals(TAG_MAP)) {
		String point = element.getAttribute(MappedSchemaDescriptor.ATT_POINT);
		String schema = element.getAttribute(MappedSchemaDescriptor.ATT_SCHEMA);
		if (point == null || schema == null) {
			PDEHackFinder.fixMe("Hard-coded error message");
			System.out.println("Schema map: point or schema null");
			return;
		}
		if (descriptors.get(point) == null) {
			MappedSchemaDescriptor desc = new MappedSchemaDescriptor(element);
			descriptors.put(point, desc);
		}
	}
}
private void removeExtensionPoint(IFile file) {
	for (Enumeration enum=descriptors.keys(); enum.hasMoreElements();) {
		String pointId = (String)enum.nextElement();
		Object desc = descriptors.get(pointId);
		if (desc instanceof FileSchemaDescriptor) {
			FileSchemaDescriptor fd = (FileSchemaDescriptor)desc;
			if (fd.getFile().equals(file)) {
				descriptors.remove(pointId);
				fd.getSchema().dispose();
			}
		}
	}
}
private void removeWorkspaceDescriptors(IPluginModelBase model) {
	IPluginBase pluginInfo = model.getPluginBase();
	IPluginExtensionPoint[] points = pluginInfo.getExtensionPoints();
	for (int i = 0; i < points.length; i++) {
		IPluginExtensionPoint point = points[i];
		Object descObj = descriptors.get(point.getFullId());
		if (descObj != null && descObj instanceof FileSchemaDescriptor) {
			FileSchemaDescriptor desc = (FileSchemaDescriptor)descObj;
			IFile schemaFile = desc.getFile();
			if (model.getUnderlyingResource().getProject().equals(schemaFile.getProject())) {
				// same project - remove
				descriptors.remove(point.getFullId());
			}
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
				PDEPlugin.logException(e);
			}
		}
	}
}
public void shutdown() {
	if (descriptors==null) return;
	for (Iterator iter=descriptors.values().iterator(); iter.hasNext();) {
		AbstractSchemaDescriptor desc = (AbstractSchemaDescriptor)iter.next();
		desc.dispose();
	}
	descriptors.clear();
	descriptors = null;
	dirtyWorkspaceModels = null;
	PDEPlugin.getDefault().getWorkspaceModelManager().removeModelProviderListener(this);
	PDEPlugin.getWorkspace().removeResourceChangeListener(this);
}
private void updateExtensionPoint(IFile file) {
	for (Iterator iter = descriptors.values().iterator(); iter.hasNext();) {
		AbstractSchemaDescriptor sd = (AbstractSchemaDescriptor)iter.next();
		if (sd instanceof FileSchemaDescriptor) {
			IFile schemaFile = ((FileSchemaDescriptor)sd).getFile();
			if (schemaFile.equals(file)) {
				sd.dispose();
				break;
			}
		}
	}
}
private void updateWorkspaceDescriptors() {
	for (int i=0; i<dirtyWorkspaceModels.size(); i++) {
		IPluginModel model = (IPluginModel)dirtyWorkspaceModels.elementAt(i);
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
		if (file.getName().toLowerCase().endsWith(".xsd")==false) return true;
		if (file.getProject().hasNature(PDEPlugin.PLUGIN_NATURE)==false) return true;
		if (delta.getKind() == IResourceDelta.CHANGED) {
			if ((IResourceDelta.CONTENT & delta.getFlags()) != 0) {
				updateExtensionPoint(file);
			}
		}
		else if (delta.getKind() == IResourceDelta.ADDED) {
			addExtensionPoint(file);
		}
		else if (delta.getKind() == IResourceDelta.REMOVED) {
			removeExtensionPoint(file);
		}
	}
	return true;
}
}
