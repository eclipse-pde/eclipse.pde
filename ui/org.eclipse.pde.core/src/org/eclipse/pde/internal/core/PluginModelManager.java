package org.eclipse.pde.internal.core;

import java.util.*;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.plugin.*;

public class PluginModelManager implements IAdaptable {
	private IModelProviderListener providerListener;
	private ExternalModelManager externalManager;
	private WorkspaceModelManager workspaceManager;
	private ArrayList listeners;

	private Hashtable entries;

	public PluginModelManager() {
		providerListener = new IModelProviderListener() {
			public void modelsChanged(IModelProviderEvent e) {
				if (entries!=null)
					handleModelsChanged(e);
			}
		};
		listeners = new ArrayList();
	}
	
	public Object getAdapter(Class key) {
		return null;
	}
	
	public void addPluginModelListener(IPluginModelListener listener) {
		if (!listeners.contains(listener))
			listeners.add(listener);
	}
	
	public void removePluginModelListener(IPluginModelListener listener) {
		if (listeners.contains(listener))
			listeners.remove(listener);
	}
	
	public boolean isEmpty() {
		if (entries == null)
			initializeTable();
		return entries.isEmpty();
	}

	public ModelEntry[] getEntries() {
		if (entries == null)
			initializeTable();
		Collection values = entries.values();
		return (ModelEntry[]) values.toArray(new ModelEntry[values.size()]);
	}

	public IPluginModelBase [] getPlugins() {
		if (entries == null)
			initializeTable();
		Collection values = entries.values();
		IPluginModelBase [] plugins = new IPluginModelBase[values.size()];
		int i=0;
		for (Iterator iter=values.iterator(); iter.hasNext();) {
			ModelEntry entry = (ModelEntry)iter.next();
			plugins[i++] = entry.getActiveModel();
		}
		return plugins;
	}
	
	public IPluginModelBase findPlugin(String id, String version, int match) {
		if (entries == null) initializeTable();
		ModelEntry entry = (ModelEntry)entries.get(id);
		if (entry==null) return null;
		return entry.getActiveModel();
	}
	
	public ModelEntry findEntry(IProject project) {
		if (entries==null) initializeTable();
		IModel model = workspaceManager.getWorkspaceModel(project);
		if (model==null) return null;
		if (!(model instanceof IPluginModelBase))
			return null;
		IPluginModelBase modelBase = (IPluginModelBase)model;
		String id = modelBase.getPluginBase().getId();
		return (ModelEntry)entries.get(id);
	}

	private void handleModelsChanged(IModelProviderEvent e) {
		PluginModelDelta delta = new PluginModelDelta();
		ArrayList changedPlugins = new ArrayList();

		if ((e.getEventTypes() & IModelProviderEvent.MODELS_REMOVED) != 0) {
			IModel[] removed = e.getRemovedModels();
			for (int i = 0; i < removed.length; i++) {
				if (!(removed[i] instanceof IPluginModelBase)) continue;
				IPluginModelBase model = (IPluginModelBase) removed[i];
				IPluginBase plugin = model.getPluginBase();
				updateTable(plugin.getId(), model, false, delta);
				changedPlugins.add(plugin);
			}
		}
		if ((e.getEventTypes() & IModelProviderEvent.MODELS_ADDED) != 0) {
			IModel[] added = e.getAddedModels();
			for (int i = 0; i < added.length; i++) {
				if (!(added[i] instanceof IPluginModelBase)) continue;
				IPluginModelBase model = (IPluginModelBase) added[i];
				IPluginBase plugin = model.getPluginBase();
				updateTable(plugin.getId(), model, true, delta);
				changedPlugins.add(plugin);
			}
		}
		if ((e.getEventTypes() & IModelProviderEvent.MODELS_CHANGED) != 0) {
			IModel[] changed = e.getChangedModels();
			for (int i = 0; i < changed.length; i++) {
				if (!(changed[i] instanceof IPluginModelBase)) continue;
				IPluginModelBase model = (IPluginModelBase) changed[i];
				IPluginBase plugin = model.getPluginBase();
				ModelEntry entry = (ModelEntry)entries.get(plugin.getId());
				delta.addEntry(entry, PluginModelDelta.CHANGED);
				changedPlugins.add(plugin);
			}
		}
		updateAffectedEntries((IPluginBase[])changedPlugins.toArray(new IPluginBase[changedPlugins.size()]));
		fireDelta(delta);
	}

	private void updateTable(
		String id,
		IPluginModelBase model,
		boolean added,
		PluginModelDelta delta) {
		boolean workspace = model.getUnderlyingResource()!=null;
		ModelEntry entry = (ModelEntry) entries.get(id);
		int kind = 0;
		if (added && entry == null) {
			entry = new ModelEntry(id);
			entries.put(id, entry);
			kind = PluginModelDelta.ADDED;
			try {
				entry.updateClasspathContainer(false);
			}
			catch (CoreException e) {
			}
		}
		if (added) {
			if (workspace)
				entry.setWorkspaceModel(model);
			else
				entry.setExternalModel(model);
		} else if (entry != null) {
			if (workspace) entry.setWorkspaceModel(null);
			else
				entry.setExternalModel(null);
			if (entry.isEmpty()) {
				entries.remove(id);
				kind = PluginModelDelta.REMOVED;
			}
		}
		if (kind==0) kind = PluginModelDelta.CHANGED;
		delta.addEntry(entry, kind);
	}
	
	private void updateAffectedEntries(IPluginBase [] changedPlugins) {
		// Reset classpath containers for affected entries
		ModelEntry [] entries = getEntries();
		
		for (int i=0; i<entries.length; i++) {
			ModelEntry entry = entries[i];
			if (entry.isAffected(changedPlugins)) {
				try {
					entry.updateClasspathContainer(true);
				}
				catch (CoreException e) {
				}
			}
		}
	}
	
	private void initializeTable() {
		entries = new Hashtable();
		IPluginModel[] models = workspaceManager.getWorkspacePluginModels();
		IFragmentModel[] fmodels = workspaceManager.getWorkspaceFragmentModels();
		addToTable(models, fmodels, true);
		models = externalManager.getModels();
		fmodels = externalManager.getFragmentModels(null);
		addToTable(models, fmodels, false);
	}

	private void addToTable(
		IPluginModel[] pmodels,
		IFragmentModel[] fmodels,
		boolean workspace) {
		for (int i = 0; i < pmodels.length; i++) {
			addToTable(pmodels[i], workspace);
		}
		for (int i = 0; i < fmodels.length; i++) {
			addToTable(fmodels[i], workspace);
		}
	}

	private void addToTable(IPluginModelBase model, boolean workspace) {
		String id = model.getPluginBase().getId();
		ModelEntry entry = (ModelEntry) entries.get(id);
		if (entry == null) {
			entry = new ModelEntry(id);
			entries.put(id, entry);
		}
		if (workspace)
			entry.setWorkspaceModel(model);
		else
			entry.setExternalModel(model);
	}
	
	private void fireDelta(PluginModelDelta delta) {
		Object [] entries = listeners.toArray();
		for (int i=0; i<entries.length; i++) {
			((IPluginModelListener)entries[i]).modelsChanged(delta);
		}
	}

	public void connect(WorkspaceModelManager wm, ExternalModelManager em) {
		externalManager = em;
		workspaceManager = wm;
		externalManager.addModelProviderListener(providerListener);
		workspaceManager.addModelProviderListener(providerListener);
	}
	public void shutdown() {
		workspaceManager.removeModelProviderListener(providerListener);
		externalManager.removeModelProviderListener(providerListener);
	}
}