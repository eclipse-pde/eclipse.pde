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
package org.eclipse.pde.internal.core;

import java.util.*;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;

public class PluginModelManager implements IAdaptable {
	private static final String OSGI_RUNTIME ="org.eclipse.core.runtime.compatibility";
	private IModelProviderListener providerListener;
	private IExternalModelManager externalManager;
	private IWorkspaceModelManager workspaceManager;
	private SearchablePluginsManager searchablePluginsManager;
	private ArrayList listeners;
	private boolean osgiRuntime;

	private TreeMap entries;

	public PluginModelManager() {
		providerListener = new IModelProviderListener() {
			public void modelsChanged(IModelProviderEvent e) {
				if (entries!=null)
					handleModelsChanged(e);
			}
		};
		listeners = new ArrayList();
		searchablePluginsManager = new SearchablePluginsManager(this);
	}
	/*
	 * Returns true if OSGi runtime is currently present either in
	 * the workspace or in the target platform.
	 */

	public boolean isOSGiRuntime() {
		if (entries == null)
			initializeTable();
		return osgiRuntime;
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
		if (entries==null) return true;
		return entries.isEmpty();
	}

	public ModelEntry[] getEntries() {
		if (entries == null)
			initializeTable();
		if (entries==null) return new ModelEntry[0];
		Collection values = entries.values();
		return (ModelEntry[]) values.toArray(new ModelEntry[values.size()]);
	}

	public IPluginModelBase[] getPlugins() {
		if (entries == null)
			initializeTable();
		if (entries == null)
			return new IPluginModelBase[0];
		Collection values = entries.values();
		ArrayList result = new ArrayList();
		for (Iterator iter = values.iterator(); iter.hasNext();) {
			ModelEntry entry = (ModelEntry) iter.next();
			IPluginModelBase model = entry.getActiveModel();
			if (model.isEnabled())
				result.add(model);
		}
		return (IPluginModelBase[])result.toArray(new IPluginModelBase[result.size()]);
	}
	
	public IPluginModelBase [] getPluginsOnly() {
		if (entries == null)
			initializeTable();
		if (entries == null)
			return new IPluginModelBase[0];
		Collection values = entries.values();
		ArrayList result = new ArrayList();
		for (Iterator iter = values.iterator(); iter.hasNext();) {
			ModelEntry entry = (ModelEntry) iter.next();
			IPluginModelBase model = entry.getActiveModel();
			if (model.isEnabled() && model instanceof IPluginModel)
				result.add(model);
		}
		return (IPluginModelBase[])result.toArray(new IPluginModelBase[result.size()]);
	}
	
	public ModelEntry findEntry(IProject project) {
		if (entries==null) initializeTable();
		if (entries==null) return null;
		IModel model = workspaceManager.getWorkspaceModel(project);
		if (model==null) return null;
		if (!(model instanceof IPluginModelBase))
			return null;
		IPluginModelBase modelBase = (IPluginModelBase)model;
		String id = modelBase.getPluginBase().getId();
		return (ModelEntry)entries.get(id);
	}
	
	public ModelEntry findEntry(String id) {	
		return findEntry(id, null);
	}
	
	public ModelEntry findEntry(String id, String version) {
		return findEntry(id, version, IMatchRules.PERFECT);
	}
	
	public ModelEntry findEntry(String id, String version, int match) {
		if (entries == null)
			initializeTable();
		if (entries == null)
			return null;
		return (ModelEntry) entries.get(id);
	}
	
	public IPluginModelBase findPlugin(String id, String version, int match) {
		if (entries == null)
			initializeTable();
		ModelEntry entry = findEntry(id, version, match);
		if (entry == null)
			return null;
		return entry.getActiveModel();
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
		if (id == null)
			return;
		ModelEntry entry = (ModelEntry) entries.get(id);
		int kind = 0;
		if (added && entry == null) {
			entry = new ModelEntry(this, id);
			entries.put(id, entry);
			if (id.equals(OSGI_RUNTIME)) {
				osgiRuntime=true;
			}
			kind = PluginModelDelta.ADDED;
			try {
				entry.updateClasspathContainer(false, true);
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
				if (id.equals(OSGI_RUNTIME)) {
					osgiRuntime=false;
				}
			}
		}
		if (kind==0) kind = PluginModelDelta.CHANGED;
		delta.addEntry(entry, kind);
	}

	private void updateAffectedEntries(IPluginBase [] changedPlugins) {
		// Reset classpath containers for affected entries
		ModelEntry [] entries = getEntries();
		Map map = new HashMap();
		for (int i=0; i<entries.length; i++) {
			ModelEntry entry = entries[i];

			if (entry.isAffected(changedPlugins)) {
				try {
					if (entry.shouldUpdateClasspathContainer(true, true)) {
						IProject proj = entry.getWorkspaceModel().getUnderlyingResource().getProject();
						map.put(JavaCore.create(proj), entry.getClasspathContainer());
					}
				}
				catch (CoreException e) {
				}
			}
		}
		if (map.size() > 0) {
			try {
				IJavaProject[] jProjects = (IJavaProject[])map.keySet().toArray(new IJavaProject[map.size()]);
				IClasspathContainer[] containers = (IClasspathContainer[])map.values().toArray(new IClasspathContainer[map.size()]);
				JavaCore.setClasspathContainer(
					new Path(PDECore.CLASSPATH_CONTAINER_ID),
					jProjects,
					containers,
					null);
			} catch (JavaModelException e) {
			}
		}
	}
	
	private void initializeTable() {
		if (workspaceManager.isLocked()) return;
		entries = new TreeMap();
		IPluginModelBase[] models = workspaceManager.getAllModels();
		addToTable(models, true);
		models = externalManager.getAllModels();
		addToTable(models, false);
		searchablePluginsManager.initialize();
	}

	private void addToTable(
		IPluginModelBase[] pmodels,
		boolean workspace) {
		for (int i = 0; i < pmodels.length; i++) {
			addToTable(pmodels[i], workspace);
		}
	}

	private void addToTable(IPluginModelBase model, boolean workspace) {
		String id = model.getPluginBase().getId();
		if (id == null)
			return;
		ModelEntry entry = (ModelEntry) entries.get(id);
		if (entry == null) {
			entry = new ModelEntry(this, id);
			entries.put(id, entry);
		}
		if (workspace)
			entry.setWorkspaceModel(model);
		else
			entry.setExternalModel(model);
		if (id.equals(OSGI_RUNTIME)) {
			osgiRuntime=true;
		}
	}
	
	private void fireDelta(PluginModelDelta delta) {
		Object [] entries = listeners.toArray();
		for (int i=0; i<entries.length; i++) {
			((IPluginModelListener)entries[i]).modelsChanged(delta);
		}
	}

	public void connect(IWorkspaceModelManager wm, IExternalModelManager em) {
		externalManager = em;
		workspaceManager = wm;
		externalManager.addModelProviderListener(providerListener);
		workspaceManager.addModelProviderListener(providerListener);
	}
	public void shutdown() {
		if (workspaceManager != null)	
			workspaceManager.removeModelProviderListener(providerListener);
		if (externalManager != null)
			externalManager.removeModelProviderListener(providerListener);
		searchablePluginsManager.shutdown();
	}
	
	public void setInJavaSearch(ModelEntry [] entries, boolean value, IProgressMonitor monitor) throws CoreException {
		PluginModelDelta delta = new PluginModelDelta();
		for (int i=0; i<entries.length; i++) {
			ModelEntry entry = entries[i];
			if (entry.isInJavaSearch()!=value) {
				entry.setInJavaSearch(value);
				delta.addEntry(entry, PluginModelDelta.CHANGED);
			}
		}
		if (delta.getKind()!=0) {
			searchablePluginsManager.persistStates( monitor);
			fireDelta(delta);
		}
	}
	 
	void searchablePluginsRemoved() {
		ModelEntry [] entries = getEntries();
		PluginModelDelta delta = new PluginModelDelta();
		
		for (int i=0; i<entries.length; i++) {
			ModelEntry entry = entries[i];
			if (entry.isInJavaSearch()) {
				entry.setInJavaSearch(false);
				delta.addEntry(entry, PluginModelDelta.CHANGED);
			}
		}
		if (delta.getKind()!=0)
			fireDelta(delta);
	}
	
	public IFileAdapterFactory getFileAdapterFactory() {
		return searchablePluginsManager;
	}
}