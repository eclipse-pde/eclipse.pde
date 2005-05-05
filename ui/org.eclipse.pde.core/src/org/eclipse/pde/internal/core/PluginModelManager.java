/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ibundle.*;

public class PluginModelManager implements IAdaptable {
	private static final String OSGI_RUNTIME ="org.eclipse.osgi"; //$NON-NLS-1$
	private IModelProviderListener fProviderListener;
	private ExternalModelManager fExternalManager;
	private WorkspaceModelManager fWorkspaceManager;
	private SearchablePluginsManager fSearchablePluginsManager;
	private ArrayList fListeners;
	private Map fEntries;
	
	private PDEState fState;

	public PluginModelManager(WorkspaceModelManager wm, ExternalModelManager em) {
		fProviderListener = new IModelProviderListener() {
			public void modelsChanged(IModelProviderEvent e) {
				handleModelsChanged(e);
			}
		};
		fWorkspaceManager = wm;
		fExternalManager = em;
		fExternalManager.addModelProviderListener(fProviderListener);
		fWorkspaceManager.addModelProviderListener(fProviderListener);
		fListeners = new ArrayList();
		fSearchablePluginsManager = new SearchablePluginsManager(this);
	}
	
	/*
	 * Returns true if OSGi runtime is currently present either in
	 * the workspace or in the target platform.
	 */
	public boolean isOSGiRuntime() {
		return findEntry(OSGI_RUNTIME) != null;
	}
	
	public String getTargetVersion() {
		ModelEntry entry = findEntry(OSGI_RUNTIME); 
		if (entry == null) 
			return ICoreConstants.TARGET21;
		
		IPluginModelBase model = entry.getActiveModel();
		String version = model.getPluginBase().getVersion();
		if (PluginVersionIdentifier.validateVersion(version).getSeverity() == IStatus.OK) {
			PluginVersionIdentifier id = new PluginVersionIdentifier(version);
			int major = id.getMajorComponent();
			int minor = id.getMinorComponent();
			if (major == 3 && minor == 0)
				return ICoreConstants.TARGET30;
		}
				
		return ICoreConstants.TARGET31;	
	}

	public Object getAdapter(Class key) {
		return null;
	}
	
	public void addPluginModelListener(IPluginModelListener listener) {
		if (!fListeners.contains(listener))
			fListeners.add(listener);
	}
	
	public void removePluginModelListener(IPluginModelListener listener) {
		if (fListeners.contains(listener))
			fListeners.remove(listener);
	}
	
	public boolean isEmpty() {
		return getEntryTable().isEmpty();
	}

	public ModelEntry[] getEntries() {
		Collection values = getEntryTable().values();
		return (ModelEntry[]) values.toArray(new ModelEntry[values.size()]);
	}
	
	/*
	 * @return enabled plug-ins only
	 */
	public IPluginModelBase[] getPlugins() {
		Collection values = getEntryTable().values();
		ArrayList result = new ArrayList();
		for (Iterator iter = values.iterator(); iter.hasNext();) {
			ModelEntry entry = (ModelEntry) iter.next();
			IPluginModelBase model = entry.getActiveModel();
			if (model != null && model.isEnabled())
				result.add(model);
		}
		return (IPluginModelBase[])result.toArray(new IPluginModelBase[result.size()]);
	}
	
	/*
	 * @return all plug-ins (enabled and disabled)
	 */
	public IPluginModelBase[] getAllPlugins() {
		Collection values = getEntryTable().values();
		ArrayList result = new ArrayList();
		for (Iterator iter = values.iterator(); iter.hasNext();) {
			ModelEntry entry = (ModelEntry) iter.next();
			result.add(entry.getActiveModel());
		}
		return (IPluginModelBase[])result.toArray(new IPluginModelBase[result.size()]);
	}
	
	public IPluginModel[] getPluginsOnly() {
		Collection values = getEntryTable().values();
		ArrayList result = new ArrayList();
		for (Iterator iter = values.iterator(); iter.hasNext();) {
			ModelEntry entry = (ModelEntry) iter.next();
			IPluginModelBase model = entry.getActiveModel();
			if (model.isEnabled() && model instanceof IPluginModel)
				result.add(model);
		}
		return (IPluginModel[])result.toArray(new IPluginModel[result.size()]);
	}
	
	public IFragmentModel[] getFragments() {
		Collection values = getEntryTable().values();
		ArrayList result = new ArrayList();
		for (Iterator iter = values.iterator(); iter.hasNext();) {
			ModelEntry entry = (ModelEntry) iter.next();
			IPluginModelBase model = entry.getActiveModel();
			if (model instanceof IFragmentModel)
				result.add(model);
		}
		return (IFragmentModel[])result.toArray(new IFragmentModel[result.size()]);
		
	}
	
	public ModelEntry findEntry(IProject project) {
		initializeTable();
		IPluginModelBase model = fWorkspaceManager.getWorkspacePluginModel(project);
		return model == null ? null : findEntry(model.getPluginBase().getId());
	}
	
	public IPluginModelBase findModel(IProject project) {
		ModelEntry entry = findEntry(project);
		return (entry != null) ? entry.getActiveModel() : null;
	}
	
	public IPluginModelBase findModel(BundleDescription desc) {
		return desc == null ? null : findModel(desc.getSymbolicName());
	}
	
	public ModelEntry findEntry(String id) {
		return id == null ? null : (ModelEntry) getEntryTable().get(id);
	}
	
	public IPluginModelBase findModel(String id) {
		ModelEntry entry = findEntry(id);
		return (entry == null) ?  null : entry.getActiveModel();
	}
	
	public IPluginModelBase findPlugin(String id, String version, int match) {
		return findModel(id);
	}
	
	public IPluginModel findPluginModel(String id) {
		IPluginModelBase model = findModel(id);
		return (model != null && model instanceof IPluginModel) ? (IPluginModel)model : null;
	}
	
	public IFragmentModel findFragmentModel(String id) {
		IPluginModelBase model = findModel(id);
		return (model != null && model instanceof IFragmentModel) ? (IFragmentModel)model : null;
	}
	
	private void handleModelsChanged(IModelProviderEvent e) {
		PluginModelDelta delta = new PluginModelDelta();
		ArrayList changedPlugins = new ArrayList();
		ArrayList oldIds=new ArrayList();
		boolean javaSearchAffected=false;

		if ((e.getEventTypes() & IModelProviderEvent.MODELS_REMOVED) != 0) {
			IModel[] removed = e.getRemovedModels();
			for (int i = 0; i < removed.length; i++) {
				if (!(removed[i] instanceof IPluginModelBase)) continue;
				IPluginModelBase model = (IPluginModelBase) removed[i];
				IPluginBase plugin = model.getPluginBase();
				ModelEntry entry = updateTable(plugin.getId(), model, false, delta);
				if (entry!=null) {
					if (model.getUnderlyingResource()!=null || entry.isInJavaSearch())
						javaSearchAffected=true;
				}
				changedPlugins.add(plugin);
			}
		}
		if ((e.getEventTypes() & IModelProviderEvent.MODELS_ADDED) != 0) {
			IModel[] added = e.getAddedModels();
			for (int i = 0; i < added.length; i++) {
				if (!(added[i] instanceof IPluginModelBase)) continue;
				IPluginModelBase model = (IPluginModelBase) added[i];
				IPluginBase plugin = model.getPluginBase();
				ModelEntry entry = updateTable(plugin.getId(), model, true, delta);
				if (entry!=null) {
					if (model.getUnderlyingResource()!=null || entry.isInJavaSearch())
						javaSearchAffected=true;
				}
				changedPlugins.add(plugin);
			}
		}
		if ((e.getEventTypes() & IModelProviderEvent.MODELS_CHANGED) != 0) {
			IModel[] changed = e.getChangedModels();
			for (int i = 0; i < changed.length; i++) {
				if (!(changed[i] instanceof IPluginModelBase)) continue;
				IPluginModelBase model = (IPluginModelBase) changed[i];
				boolean workspace = model.getUnderlyingResource()!=null;
				fState.addBundle(model, true);
				IPluginBase plugin = model.getPluginBase();
				String id = plugin.getId();
				if (id != null) {
					ModelEntry entry = (ModelEntry)getEntryTable().get(plugin.getId());
					String oldId=null;

					if (entry!=null) {
						if (workspace && model!=entry.getWorkspaceModel()) {
							//Two possible cases:
							// 1) wrong slot - id changed
							// 2) correct slot but plugin->bundle change (e vice versa)
							if (isBundlePluginSwap(model, entry)) {
								if (entry.isInJavaSearch())
									javaSearchAffected=true;
								delta.addEntry(entry, PluginModelDelta.CHANGED);					
							}
							else
								oldId=handleIdChange(id, model, entry, delta);
						}
						else {
							if (workspace || entry.isInJavaSearch())
								javaSearchAffected=true;
							delta.addEntry(entry, PluginModelDelta.CHANGED);
						}
						changedPlugins.add(plugin);
					}
					else if (workspace) {
						// model change, entry does not exist - must be
						// id change
						oldId=handleIdChange(id, model, null, delta);
						changedPlugins.add(plugin);
					}
					if (oldId!=null)
						oldIds.add(oldId);
				}
			}
		}
		
		if (changedPlugins.size() > 0)
			fState.resolveState(true);
		updateAffectedEntries((IPluginBase[])changedPlugins.toArray(new IPluginBase[changedPlugins.size()]), oldIds);
		if (javaSearchAffected)
			fSearchablePluginsManager.updateClasspathContainer();
		fireDelta(delta);
	}
	
	private boolean isBundlePluginSwap(IPluginModelBase model, ModelEntry entry) {
		IPluginModelBase workspaceModel = entry.getWorkspaceModel();
		if (workspaceModel==null) return false;
		boolean swap=false;
		if (model instanceof IBundlePluginModelBase && !(workspaceModel instanceof IBundlePluginModelBase))
			swap=true;
		else
			if (!(model instanceof IBundlePluginModelBase) && workspaceModel instanceof IBundlePluginModelBase)
			swap=true;
		if (swap)
			entry.setWorkspaceModel(model);
		return swap;
	}
	
	private ModelEntry findOldEntry(IPluginModelBase model) {
		Collection values = getEntryTable().values();

		for (Iterator iter = values.iterator(); iter.hasNext();) {
			ModelEntry entry = (ModelEntry) iter.next();
			IPluginModelBase candidate = entry.getWorkspaceModel();
			if (model == candidate)
				return entry;
		}
		return null;
	}

	private String handleIdChange(String newId, IPluginModelBase model, ModelEntry newEntry, PluginModelDelta delta) {
		ModelEntry oldEntry = findOldEntry(model);
		String oldId=null;
		// we must remove the model from the old entry
		if (oldEntry!=null) {
			oldEntry.setWorkspaceModel(null);
			if (oldEntry.isEmpty()) {
				// remove the old entry completely
				getEntryTable().remove(oldEntry.getId());
				delta.addEntry(oldEntry, PluginModelDelta.REMOVED);
			}
			else {
				// just notify that the old entry has changed
				delta.addEntry(oldEntry, PluginModelDelta.CHANGED);
			}
			oldId = oldEntry.getId();
		}
		// add the model to the new entry; if does not exist, create
		if (newEntry!=null) {
			// change the workspace model of the new entry
			newEntry.setWorkspaceModel(model);
			delta.addEntry(newEntry, PluginModelDelta.CHANGED);
		}
		else {
			// create the new entry
			newEntry = new ModelEntry(this, newId);
			getEntryTable().put(newId, newEntry);
			delta.addEntry(newEntry, PluginModelDelta.ADDED);
		}
		return oldId;
	}

	private ModelEntry updateTable(
		String id,
		IPluginModelBase model,
		boolean added,
		PluginModelDelta delta) {
		boolean workspace = model.getUnderlyingResource()!=null;
		if (id == null)
			return null;
		Map entries = getEntryTable();
		ModelEntry entry = (ModelEntry) entries.get(id);
		int kind = 0;
		if (added && entry == null) {
			entry = new ModelEntry(this, id);
			entries.put(id, entry);
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
			}
		}
		if (workspace) {
			if (added) {
				addWorkspaceBundleToState(model, true);
			} else {
				removeWorkspaceBundleFromState(model);
			}
		}
		if (kind==0) kind = PluginModelDelta.CHANGED;
		delta.addEntry(entry, kind);
		return entry;
	}

	private void updateAffectedEntries(IPluginBase [] changedPlugins, ArrayList oldIds) {
		// Reset classpath containers for affected entries
		ModelEntry [] entries = getEntries();
		Map map = new HashMap();

		for (int i=0; i<entries.length; i++) {
			ModelEntry entry = entries[i];

			if (entry.isAffected(changedPlugins, oldIds)) {
				try {
					if (entry.shouldUpdateClasspathContainer(true, true)) {
						IProject proj = entry.getWorkspaceModel().getUnderlyingResource().getProject();
						entry.getClasspathContainer().reset();
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
	
	/*
	 * Allow access to the table only through this getter.
	 * It always calls initialize to make sure the table is initialized.
	 * If more than one thread tries to read the table at the same time,
	 *  and the table is not initialized yet, thread2 would wait. 
	 *  This way there are no partial reads.
	 */
	private Map getEntryTable() {
		initializeTable();
		return fEntries;
	}

	/*
	 * This method must be synchronized so that only one thread
	 * initializes the table, and the rest would block until
	 * the table is initialized.
	 * 
	 */
	private synchronized void initializeTable() {
		if (fEntries != null) return;
		fEntries = Collections.synchronizedMap(new TreeMap());
		fState = new PDEState(
						WorkspaceModelManager.getPluginPaths(),
						ExternalModelManager.getPluginPaths(),
						TargetPlatform.getTargetEnvironment(),
						new NullProgressMonitor());
		
		fExternalManager.initializeModels(fState.getTargetModels());	
		addToTable(fExternalManager.getAllModels(), false);
		if (fState.isCombined()) {
			IPluginModelBase[] models = fState.getWorkspaceModels();
			fWorkspaceManager.initializeModels(models);
			addToTable(models, true);
		} else {
			addToTable(fWorkspaceManager.getAllModels(), true);			
			addWorkspaceBundlesToNewState();
		}
		fSearchablePluginsManager.initialize();
	}
	
	public PDEState getState() {
		initializeTable();
		return fState;
	}
	
	public void setState(PDEState state) {
		fState = state;
		fExternalManager.setModels(state.getTargetModels());
		addWorkspaceBundlesToNewState();
	}

	private void addToTable(IPluginModelBase[] models, boolean workspace) {
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase model = models[i];
			String id = model.getPluginBase().getId();
			if (id == null)
				continue;
			ModelEntry entry = (ModelEntry) fEntries.get(id);
			if (entry == null) {
				entry = new ModelEntry(this, id);
				fEntries.put(id, entry);
			}
			if (workspace)
				entry.setWorkspaceModel(model);
			else
				entry.setExternalModel(model);
		}
	}
		
	private void addWorkspaceBundlesToNewState() {
		IPluginModelBase[] models = fWorkspaceManager.getAllModels();
		for (int i = 0; i < models.length; i++) {
			addWorkspaceBundleToState(models[i], false);
		}
		fState.resolveState(true);
	}
	
	private void addWorkspaceBundleToState(IPluginModelBase model, boolean update) {
		ModelEntry entry = findEntry(model.getPluginBase().getId());
		IPluginModelBase external = entry == null ? null : entry.getExternalModel();
		if (external != null) {
			fState.removeBundleDescription(external.getBundleDescription());
		}
		fState.addBundle(model, update);		
	}
	
	private void removeWorkspaceBundleFromState(IPluginModelBase model) {
		BundleDescription description = model.getBundleDescription();
		if (description == null)
			return;
		
		fState.removeBundleDescription(description);
		
		ModelEntry entry = findEntry(model.getPluginBase().getId());
		IPluginModelBase external = entry == null ? null : entry.getExternalModel();
		if (external != null) {
			fState.addBundleDescription(external.getBundleDescription());
		}
	}
	
	private void fireDelta(PluginModelDelta delta) {
		Object [] entries = fListeners.toArray();
		for (int i=0; i<entries.length; i++) {
			((IPluginModelListener)entries[i]).modelsChanged(delta);
		}
	}

	public void shutdown() {
		if (fWorkspaceManager != null)	
			fWorkspaceManager.removeModelProviderListener(fProviderListener);
		if (fExternalManager != null)
			fExternalManager.removeModelProviderListener(fProviderListener);
		if (fState != null)
			fState.shutdown();
		fSearchablePluginsManager.shutdown();
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
			fSearchablePluginsManager.persistStates( monitor);
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
		return fSearchablePluginsManager;
	}
	/**
	 * @return Returns the searchablePluginsManager.
	 */
	public SearchablePluginsManager getSearchablePluginsManager() {
		initializeTable();
		return fSearchablePluginsManager;
	}
	
	public IPluginModelBase[] getExternalModels() {
		initializeTable();
		return fExternalManager.getAllModels();
	}
	
	public IPluginModelBase[] getWorkspaceModels() {
		initializeTable();
		return fWorkspaceManager.getAllModels();
	}
}
