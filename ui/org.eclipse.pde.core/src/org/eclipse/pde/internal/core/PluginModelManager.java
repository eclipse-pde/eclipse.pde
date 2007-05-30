/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.osgi.service.resolver.BundleDelta;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.service.resolver.StateDelta;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelProviderEvent;
import org.eclipse.pde.core.IModelProviderListener;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ModelEntry;

public class PluginModelManager implements IModelProviderListener {
	
	/**
	 * Subclass of ModelEntry
	 * It adds methods that add/remove model from the entry.
	 * These methods must not be on ModelEntry itself because
	 * ModelEntry is an API class and we do not want clients to manipulate
	 * the ModelEntry
	 * 
	 */
	private class LocalModelEntry extends ModelEntry {
		
		/**
		 * Constructs a model entry that will keep track
		 * of all bundles in the workspace and target that share the same ID.
		 * 
		 * @param id  the bundle ID
		 */
		public LocalModelEntry(String id) {
			super(id);
		}
		
		/**
		 * Adds a model to the entry.  
		 * An entry keeps two lists: one for workspace models 
		 * and one for target (external) models.
		 * If the model being added is associated with a workspace resource,
		 * it is added to the workspace list; otherwise, it is added to the external list.
		 * 
		 * @param model  model to be added to the entry
		 */
		public void addModel(IPluginModelBase model) {
			if (model.getUnderlyingResource() != null)
				fWorkspaceEntries.add(model);
			else
				fExternalEntries.add(model);
		}

		/**
		 * Removes the given model for the workspace list if the model is associated
		 * with workspace resource.  Otherwise, it is removed from the external list.
		 * 
		 * @param model  model to be removed from the model entry
		 */
		public void removeModel(IPluginModelBase model) {
			if (model.getUnderlyingResource() != null)
				fWorkspaceEntries.remove(model);
			else
				fExternalEntries.remove(model);
		}
	}

	private ExternalModelManager fExternalManager;  // keeps track of changes in target models
	private WorkspacePluginModelManager fWorkspaceManager;  // keeps track of changes in the workspace
	private PDEState fState;  // keeps the combined view of the target and workspace
	
	private Map fEntries;  // a master table keyed by plugin ID and the value is a ModelEntry
	private ArrayList fListeners; // a list of listeners interested in changes to the plug-in models
	private ArrayList fStateListeners; // a list of listeners interested in changes to the PDE/resolver State

	/**
	 * Initialize the workspace and external (target) model manager
	 * and add listeners to each one
	 */
	public PluginModelManager() {
		fWorkspaceManager = new WorkspacePluginModelManager();
		fExternalManager = new ExternalModelManager();
		fExternalManager.addModelProviderListener(this);
		fWorkspaceManager.addModelProviderListener(this);
	}
		
	/**
	 * React to changes in plug-ins in the workspace and/or target
	 */
	public void modelsChanged(IModelProviderEvent e) {
		PluginModelDelta delta = new PluginModelDelta();
		
		// Removes from the master table and the state all workspace plug-ins that have been
		// removed (project closed/deleted) from the workspace.
		// Also if the taget location changes, all models from the old target are removed
		if ((e.getEventTypes() & IModelProviderEvent.MODELS_REMOVED) != 0) {
			IModel[] removed = e.getRemovedModels();
			for (int i = 0; i < removed.length; i++) {
				IPluginModelBase model = (IPluginModelBase) removed[i];
				String id = model.getPluginBase().getId();
				if (id != null)
					handleRemove(id, model, delta);	
			}
		}
		
		// reset the state
		if ((e.getEventTypes() & IModelProviderEvent.TARGET_CHANGED) != 0) {
			Object newState = e.getEventSource();
			if (newState instanceof PDEState) {
				fState = (PDEState)newState;
			}
		}
		
		// Adds to the master table and the state newly created plug-ins in the workspace
		// (ie. new plug-in project or a closed project that has just been re-opened).
		// Also, if the target location changes, we add all plug-ins from the new target
		if ((e.getEventTypes() & IModelProviderEvent.MODELS_ADDED) != 0) {
			IModel[] added = e.getAddedModels();
			for (int i = 0; i < added.length; i++) {
				IPluginModelBase model = (IPluginModelBase) added[i];
				String id = model.getPluginBase().getId();
				if (id != null)
					handleAdd(id, model, delta);
			}
		}
		
		// add workspace plug-ins to the new state
		// and remove their target counterparts from the state.
		if ((e.getEventTypes() & IModelProviderEvent.TARGET_CHANGED) != 0) {
			IPluginModelBase[] models = fWorkspaceManager.getPluginModels();		
			for (int i = 0; i < models.length; i++) {
				addWorkspaceBundleToState(models[i]);
			}	
			if (models.length > 0)
				fState.resolveState(true);
		}
		
		// Update the bundle description of plug-ins whose state has changed.
		// A plug-in changes state if the MANIFEST.MF has been touched.
		// or if a plug-in on the Target Platform has changed state (from checked to unchecked,
		// and vice versa.
		if ((e.getEventTypes() & IModelProviderEvent.MODELS_CHANGED) != 0) {
			IModel[] changed = e.getChangedModels();
			for (int i = 0; i < changed.length; i++)
				handleChange((IPluginModelBase)changed[i], delta);
		}
		
		if (fState != null) {
			// if the target location has not changed, incrementally re-resolve the state after processing all the add/remove/modify changes
			// Otherwise, the state is in a good resolved state
			StateDelta stateDelta =	(e.getEventTypes() & IModelProviderEvent.TARGET_CHANGED) != 0 
									? null 
									: fState.resolveState((e.getEventTypes() & ICoreConstants.ENVIRONMENT_CHANGED) != 0 ? false : true);
			// trigger a classpath update for all workspace plug-ins affected by the
			// processed batch of changes
			updateAffectedEntries(stateDelta);
			fireStateDelta(stateDelta);
			
		}
		
		// notify all interested listeners in the changes made to the master table of entries
		fireDelta(delta);
	}
	
	/**
	 * Trigger a classpath update for all workspace plug-ins affected by the processed
	 * model changes
	 * 
	 * @param delta  a state delta containing a list of bundles affected by the processed
	 * 				changes
	 */
	private void updateAffectedEntries(StateDelta delta) {
		Map map = new HashMap();
		if (delta == null) {
			// if the delta is null, then the entire target changed.
			// Therefore, we should update the classpath for all workspace plug-ins.
			IPluginModelBase[] models = getWorkspaceModels();
			for (int i = 0; i < models.length; i++) {
				IProject project = models[i].getUnderlyingResource().getProject();
				try {
					if (project.hasNature(JavaCore.NATURE_ID)) {
						map.put(JavaCore.create(project), 
								new RequiredPluginsClasspathContainer(models[i]));
					}
				} catch (CoreException e) {
				}
			}
		} else {
			BundleDelta[] deltas = delta.getChanges();
			for (int i = 0; i < deltas.length; i++) {
				try {
					// update classpath for workspace plug-ins that are housed in a 
					// Java project hand have been affected by the processd model changes.
					IPluginModelBase model = findModel(deltas[i].getBundle());
					IResource resource = model == null ? null : model.getUnderlyingResource();
					if (resource != null) {
						IProject project = resource.getProject();
						if (project.hasNature(JavaCore.NATURE_ID)) {
							IJavaProject jProject = JavaCore.create(project);
							if (!map.containsKey(jProject)) {
								map.put(jProject, new RequiredPluginsClasspathContainer(model));
							}
						}
					}
				} catch (CoreException e) {
				}
			}
			// do secondary dependencies
			IPluginModelBase[] models = getWorkspaceModels();
			for (int i = 0; i < models.length; i++) {
				IProject project = models[i].getUnderlyingResource().getProject();
				try {
					if (!project.hasNature(JavaCore.NATURE_ID))
						continue;
					IJavaProject jProject = JavaCore.create(project);
					if (map.containsKey(jProject))
						continue;			
					IBuild build = ClasspathUtilCore.getBuild(models[i]);
					if (build != null && build.getEntry(IBuildEntry.SECONDARY_DEPENDENCIES) != null) {
						map.put(jProject, 
								new RequiredPluginsClasspathContainer(models[i], build));
					}
				} catch (CoreException e) {
				}
			}
		}	
		
		if (map.size() > 0) {
			try {
				// update classpath for all affected workspace plug-ins in one operation
				IJavaProject[] jProjects = (IJavaProject[])map.keySet().toArray(new IJavaProject[map.size()]);
				IClasspathContainer[] containers = (IClasspathContainer[])map.values().toArray(new IClasspathContainer[map.size()]);
				JavaCore.setClasspathContainer(
					PDECore.REQUIRED_PLUGINS_CONTAINER_PATH, jProjects, containers, null);
			} catch (JavaModelException e) {
			}
		}		
	}
	
	/**
	 * Notify all interested listeners in changes made to the master table
	 * 
	 * @param delta  the delta of changes
	 */
	private void fireDelta(PluginModelDelta delta) {
		if (fListeners !=  null) {
			for (int i = 0; i < fListeners.size(); i++) {
				((IPluginModelListener)fListeners.get(i)).modelsChanged(delta);
			}
		}
	}
	
	/**
	 * Notify all interested listeners in changes made to the resolver State
	 * 
	 * @param delta	the delta from the resolver State.
	 */
	private void fireStateDelta(StateDelta delta) {
		if (fStateListeners != null) {
			ListIterator li = fStateListeners.listIterator();
			while (li.hasNext())
				((IStateDeltaListener)li.next()).stateResolved(delta);
		}
	}
	
	/**
	 * Notify all interested listeners the cached PDEState has changed
	 * 
	 * @param newState	the new PDEState.
	 */
	private void fireStateChanged(PDEState newState) {
		if (fStateListeners != null) {
			ListIterator li = fStateListeners.listIterator();
			while (li.hasNext())
				((IStateDeltaListener)li.next()).stateChanged(newState.getState());
		}
	}

	/**
	 * Add a listener to the model manager
	 * 
	 * @param listener  the listener to be added
	 */
	public void addPluginModelListener(IPluginModelListener listener) {
		if (fListeners == null)
			fListeners = new ArrayList();
		if (!fListeners.contains(listener))
			fListeners.add(listener);
	}
	
	/**
	 * Add a StateDelta listener to model manager
	 * 
	 * @param listener	the listener to be added
	 */
	public void addStateDeltaListener(IStateDeltaListener listener) {
		if (fStateListeners == null)
			fStateListeners = new ArrayList();
		if (!fStateListeners.contains(listener))
			fStateListeners.add(listener);
	}	
	
	/**
	 * Remove a listener from the model manager
	 * 
	 * @param listener the listener to be removed
	 */
	public void removePluginModelListener(IPluginModelListener listener) {
		if (fListeners != null)
			fListeners.remove(listener);
	}
	
	/**
	 * Remove a StateDelta listener from the model manager
	 * 
	 * @param listener the listener to be removed
	 */
	public void removeStateDeltaListener(IStateDeltaListener listener) {
		if (fStateListeners != null)
			fStateListeners.remove(listener);
	}
	
	/**
	 * Returns <code>true</code> if neither the workspace nor target contains plug-ins;
	 * <code>false</code> otherwise.
	 *
	 * @return <code>true</code> if neither the workspace nor target contains plug-ins;
	 * 		<code>false</code> otherwise.
	 */
	public boolean isEmpty() {
		return getEntryTable().size() == 0;
	}
	
	/**
	 * Returns <code>true</code> if the master table has been initialized;
	 * <code>false</code> otherwise.
	 *
	 * @return <code>true</code> if the master table has been initialized;
	 * 		<code>false</code> otherwise.
	 */
	public boolean isInitialized() {
		return fEntries != null;
	}
	
	/**
	 * Allow access to the table only through this getter.
	 * It always calls initialize to make sure the table is initialized.
	 * If more than one thread tries to read the table at the same time,
	 * and the table is not initialized yet, thread2 would wait. 
	 * This way there are no partial reads.
	 */
	private Map getEntryTable() {
		initializeTable();
		return fEntries;
	}

	/**
	 * This method must be synchronized so that only one thread
	 * initializes the table, and the rest would block until
	 * the table is initialized.
	 * 
	 */
	private synchronized void initializeTable() {
		if (fEntries != null) return;
		fEntries = Collections.synchronizedMap(new TreeMap());
		
		// Create a state that contains all bundles from the target and workspace
		// If a workspace bundle has the same symbolic name as a target bundle,
		// the target counterpart is subsequently removed from the state.
		fState = new PDEState(
						fWorkspaceManager.getPluginPaths(),
						fExternalManager.getPluginPaths(),
						true,
						new NullProgressMonitor());
		
		// initialize the enabled/disabled state of target models
		// based on whether the bundle is checked/unchecked on the Target Platform
		// preference page.
		fExternalManager.initializeModels(fState.getTargetModels());
		
		// add target models to the master table
		boolean statechanged = addToTable(fExternalManager.getAllModels());
		
		// a state is combined only if the workspace plug-ins have not changed
		// since the last shutdown of the workbench
		if (fState.isCombined()) {
			IPluginModelBase[] models = fState.getWorkspaceModels();
			// initialize the workspace models from the cached state
			fWorkspaceManager.initializeModels(models);
			// add workspace models to the master table
			addToTable(models);
			// resolve the state incrementally if some target models
			// were removed
			if (statechanged)
				fState.resolveState(true);
		} else {
			// if we have no good cached state of workspace plug-ins,
			// re-parse all/any workspace plug-ins
			IPluginModelBase[] models = fWorkspaceManager.getPluginModels();
			
			// add workspace plug-ins to the master table
			addToTable(models);	
			
			// add workspace plug-ins to the state
			// and remove their target counterparts from the state.
			for (int i = 0; i < models.length; i++) {
				addWorkspaceBundleToState(models[i]);
			}
			
			// resolve the state incrementally if any workspace plug-ins were found
			if (models.length > 0)
				fState.resolveState(true);
		}
	}
	
	/**
	 * Adds the given models to the corresponding ModelEntry in the master table
	 * 
	 * @param models  the models to be added to the master tabl
	 * 
	 * @return <code>true</code> if changes were made to the state; <code>false</code> otherwise
	 */
	private boolean addToTable(IPluginModelBase[] models) {
		boolean stateChanged = false;
		for (int i = 0; i < models.length; i++) {
			String id = models[i].getPluginBase().getId();
			if (id == null)
				continue;
			LocalModelEntry entry = (LocalModelEntry)fEntries.get(id);
			// create a new entry for the given ID if none already exists
			if (entry == null) {
				entry = new LocalModelEntry(id);
				fEntries.put(id, entry);
			}
			// add the model to the entry
			entry.addModel(models[i]);
			
			// if the model is a disabled external (target) model, remove it from 
			// the state and set the stateChanged flag to true
			if (models[i].getUnderlyingResource() == null && !models[i].isEnabled()) {
				fState.removeBundleDescription(models[i].getBundleDescription());
				stateChanged = true;
			}
		}
		return stateChanged;
	}	
	
	/**
	 * Add a workspace bundle to the state
	 * 
	 * @param model  the workspace model
	 */
	private synchronized void addWorkspaceBundleToState(IPluginModelBase model) {
		String id = model.getPluginBase().getId();
		if (id == null)
			return;

		// remove target models by the same ID from the state, if any
		ModelEntry entry = (ModelEntry)fEntries.get(id);
		if (entry != null) {
			IPluginModelBase[] models = entry.getExternalModels();
			for (int i = 0; i < models.length; i++)
				fState.removeBundleDescription(models[i].getBundleDescription());
		}
		
		// add new bundle to the state
		fState.addBundle(model, false);
		
		BundleDescription desc = model.getBundleDescription();
		if (desc != null) {
			// refresh host if a fragment is added to the state.
			// this is necessary because the state will not re-resolve dynamically added fragments
			// on its own
			HostSpecification spec = desc.getHost();
			if (spec != null 
				&& ("true".equals(System.getProperty("pde.allowCycles")) //$NON-NLS-1$ //$NON-NLS-2$
					|| ClasspathUtilCore.isPatchFragment(desc)
					|| desc.getImportPackages().length > 0 
					|| desc.getRequiredBundles().length > 0)) {
				BundleDescription host = (BundleDescription)spec.getSupplier();
				if (host != null) {
					ModelEntry hostEntry = (ModelEntry)fEntries.get(host.getName());
					if (hostEntry != null) {
						fState.addBundle(hostEntry.getModel(host), true);
					}
				}
			}
		}
	}
	
	/**
	 * Adds a model to the master table and state
	 * 
	 * @param id the key 
	 * @param model  the model being added
	 */
	private void handleAdd(String id, IPluginModelBase model, PluginModelDelta delta) {
		LocalModelEntry entry = (LocalModelEntry)getEntryTable().get(id);
		
		// add model to the corresponding ModelEntry.  Create a new entry if necessary
		if (entry == null) {
			entry = new LocalModelEntry(id);
			getEntryTable().put(id, entry);
			delta.addEntry(entry, PluginModelDelta.ADDED);
		} else {
			delta.addEntry(entry, PluginModelDelta.CHANGED);
		}
		entry.addModel(model);
		
		// if the model added is a workspace model, add it to the state and
		// remove all its external counterparts
		if (model.getUnderlyingResource() != null) {
			addWorkspaceBundleToState(model);
		} else if (model.isEnabled() && !entry.hasWorkspaceModels()) {
			// if a target model has went from an unchecked state to a checked state
			// on the target platform preference page, re-add its bundle description
			// to the state
			BundleDescription desc = model.getBundleDescription();
			if (desc.getContainingState().equals(fState))
				fState.addBundleDescription(desc);
		}
	}
	
	/**
	 * Removes the model from the ModelEntry and the state.  The entire model entry is removed
	 * once the last model it retains is removed.
	 * 
	 * @param id   the key
	 * @param model  the model to be removed
	 */
	private void handleRemove(String id, IPluginModelBase model, PluginModelDelta delta) {
		LocalModelEntry entry = (LocalModelEntry)getEntryTable().get(id);
		if (entry != null) {
			// remove model from the entry
			entry.removeModel(model);
			// remove corresponding bundle description from the state
			fState.removeBundleDescription(model.getBundleDescription());
			if (!entry.hasExternalModels() && !entry.hasWorkspaceModels()) {
				// remove entire entry if it has no models left
				getEntryTable().remove(id);
				delta.addEntry(entry, PluginModelDelta.REMOVED);
				return;
			} else if (model.getUnderlyingResource() != null && !entry.hasWorkspaceModels()){
				// re-add enabled external counterparts to the state, if the last workspace
				// plug-in with a particular symbolic name is removed
				IPluginModelBase[] external = entry.getExternalModels();
				for (int i = 0; i < external.length; i++) {
					if (external[i].isEnabled())
						fState.addBundleDescription(external[i].getBundleDescription());
				}
			}
			delta.addEntry(entry, PluginModelDelta.CHANGED);
		}
	}
	
	/**
	 * Update the state and master table to account for the change in the given model
	 * 
	 * @param model the model that has changed
	 */
	private void handleChange(IPluginModelBase model, PluginModelDelta delta) {
		BundleDescription desc = model.getBundleDescription();
		String oldID = desc == null ? null : desc.getSymbolicName();
		String newID = model.getPluginBase().getId();
		
		// if the model still has no symbolic name (ie. a MANIFEST.MF without the
		// Bundle-SymbolicName header), keep ignoring it
		if (oldID == null && newID == null)
			return;
		
		// if the model used to lack a Bundle-SymbolicName header and now it has one,
		// treat it as a regular model addition
		if (oldID == null && newID != null) {
			handleAdd(newID, model, delta);
		} else if (oldID != null && newID == null) {
			// if the model used to have a Bundle-SymbolicName header and now it lost it,
			// treat it as a regular model removal
			handleRemove(oldID, model, delta);
			model.setBundleDescription(null);
		} else if (oldID.equals(newID)) {
			// if the workspace bundle's MANIFEST.MF was touched or
			// if the a target plug-in has now become enabled/checked, update the model
			// in the state
			if (model.isEnabled())
				fState.addBundle(model, true);
			else
				// if the target plug-in has become disabled/unchecked, remove its bundle
				// description from the state
				fState.removeBundleDescription(model.getBundleDescription());
			delta.addEntry(findEntry(oldID), PluginModelDelta.CHANGED);
		} else {
			// if the symbolic name of the bundle has completely changed,
			// remove the model from the old entry, and add the model to the new entry
			handleRemove(oldID, model, delta);
			handleAdd(newID, model, delta);
		}		
	}
		
	/**
	 * Returns a model entry containing all workspace and target plug-ins by the given ID
	 * 
	 * @param id the plug-in ID
	 * 
	 * @return a model entry containing all workspace and target plug-ins by the given ID
	 */
	public ModelEntry findEntry(String id) {
		if ("system.bundle".equals(id)) //$NON-NLS-1$
			id = "org.eclipse.osgi";  //$NON-NLS-1$
		return id == null ? null : (ModelEntry)getEntryTable().get(id);
	}

	/**
	 * Returns the plug-in model for the best match plug-in with the given ID.
	 * A null value is returned if no such bundle is found in the workspace or target platform.
	 * <p>
	 * A workspace plug-in is always preferably returned over a target plug-in.
	 * A plug-in that is checked/enabled on the Target Platform preference page is always
	 * preferably returned over a target plug-in that is unchecked/disabled.
	 * </p>
	 * <p>
	 * In the case of a tie among workspace plug-ins or among target plug-ins,
	 * the plug-in with the highest version is returned.
	 * </p>
	 * <p>
	 * In the case of a tie among more than one suitable plug-in that have the same version, 
	 * one of those plug-ins is randomly returned.
	 * </p>
	 * 
	 * @param id the plug-in ID
	 * @return the plug-in model for the best match plug-in with the given ID
	 */
	public IPluginModelBase findModel(String id) {
		ModelEntry entry = findEntry(id);
		return entry == null ? null : entry.getModel();
	}
	
	/**
	 * Returns the plug-in model corresponding to the given project, or <code>null</code>
	 * if the project does not represent a plug-in project or if it contains a manifest file
	 * that is malformed or missing vital information.
	 * 
	 * @param project the project
	 * @return a plug-in model corresponding to the project or <code>null</code> if the project
	 * 			is not a plug-in project
	 */
	public IPluginModelBase findModel(IProject project) {
		initializeTable();
		return fWorkspaceManager.getPluginModel(project);
	}
	
	/**
	 * Returns a plug-in model associated with the given bundle description
	 * 
	 * @param desc the bundle description
	 * 
	 * @return a plug-in model associated with the given bundle description or <code>null</code>
	 * 			if none exists
	 */
	public IPluginModelBase findModel(BundleDescription desc) {
		ModelEntry entry = findEntry(desc.getSymbolicName());
		return entry == null ? null : entry.getModel(desc);
	}
	
	/**
	 * Returns all plug-ins and fragments in the workspace as well as all plug-ins and fragments that are
	 * checked on the Target Platform preference page.
	 * <p>
	 * If a workspace plug-in/fragment has the same ID as a target plug-in/fragment, the target counterpart
	 * is skipped and not included.
	 * </p>
	 * <p>
	 * Equivalent to <code>getActiveModels(true)</code>
	 * </p>
	 * 
	 * @return   all plug-ins and fragments in the workspace as well as all plug-ins and fragments that are
	 * 			checked on the Target Platform preference page.
	 */
	public IPluginModelBase[] getActiveModels() {
		return getActiveModels(true);
	}

	/**
	 * Returns all plug-ins and (possibly) fragments in the workspace as well as all plug-ins and (possibly)
	 *  fragments that are checked on the Target Platform preference page.
	 * <p>
	 * If a workspace plug-in/fragment has the same ID as a target plug-in, the target counterpart
	 * is skipped and not included.
	 * </p>
	 * <p>
	 * The returned result includes fragments only if <code>includeFragments</code>
	 * is set to true
	 * </p>
	 * @param includeFragments  a boolean indicating if fragments are desired in the returned
	 *							result
	 * @return all plug-ins and (possibly) fragments in the workspace as well as all plug-ins and 
	 * (possibly) fragments that are checked on the Target Platform preference page.
	 */
	public IPluginModelBase[] getActiveModels(boolean includeFragments) {
		int size = getEntryTable().size();
		ArrayList result = new ArrayList(size);
		Iterator iter = getEntryTable().values().iterator();
		while (iter.hasNext()) {
			ModelEntry entry = (ModelEntry)iter.next();
			IPluginModelBase[] models = entry.getActiveModels();
			for (int i = 0; i < models.length; i++) {
				if (models[i] instanceof IPluginModel || includeFragments)
					result.add(models[i]);
			}
		}
		return (IPluginModelBase[])result.toArray(new IPluginModelBase[result.size()]);
	}

	/**
	 * Returns all plug-ins and fragments in the workspace as well as all target plug-ins and fragments, regardless 
	 * whether or not they are checked or not on the Target Platform preference page.
	 * <p>
	 * If a workspace plug-in/fragment has the same ID as a target plug-in, the target counterpart
	 * is skipped and not included.
	 * </p>
	 * <p>
	 * Equivalent to <code>getAllModels(true)</code>
	 * </p>
	 * 
	 * @return   all plug-ins and fragments in the workspace as well as all target plug-ins and fragments, regardless 
	 * whether or not they are checked on the Target Platform preference page.
	 */
	public IPluginModelBase[] getAllModels() {
		return getAllModels(true);
	}
	
	/**
	 * Returns all plug-ins and (possibly) fragments in the workspace as well as all plug-ins 
	 * and (possibly) fragments, regardless whether or not they are
	 * checked on the Target Platform preference page.
	 * <p>
	 * If a workspace plug-in/fragment has the same ID as a target plug-in/fragment, the target counterpart
	 * is skipped and not included.
	 * </p>
	 * <p>
	 * The returned result includes fragments only if <code>includeFragments</code>
	 * is set to true
	 * </p>
	 * @param includeFragments  a boolean indicating if fragments are desired in the returned
	 *							result
	 * @return ll plug-ins and (possibly) fragments in the workspace as well as all plug-ins 
	 * and (possibly) fragments, regardless whether or not they are
	 * checked on the Target Platform preference page.
	 */	
	public IPluginModelBase[] getAllModels(boolean includeFragments) {
		int size = getEntryTable().size();
		ArrayList result = new ArrayList(size);
		Iterator iter = getEntryTable().values().iterator();
		while (iter.hasNext()) {
			ModelEntry entry = (ModelEntry)iter.next();
			IPluginModelBase[] models =  entry.hasWorkspaceModels() 
											? entry.getWorkspaceModels() 
											: entry.getExternalModels();
			for (int i = 0; i < models.length; i++) {
				if (models[i] instanceof IPluginModel || includeFragments)
					result.add(models[i]);
			}
		}
		return (IPluginModelBase[])result.toArray(new IPluginModelBase[result.size()]);
	}
	
	/**
	 * Returns all plug-in models in the target platform
	 * 
	 * @return  all plug-ins in the target platform
	 */
	public IPluginModelBase[] getExternalModels() {
		initializeTable();
		return fExternalManager.getAllModels();
	}
	
	/**
	 * Returns all plug-in models in the workspace
	 * 
	 * @return all plug-in models in the workspace
	 */
	public IPluginModelBase[] getWorkspaceModels() {
		initializeTable();
		return fWorkspaceManager.getPluginModels();
	}
	
	/**
	 * Return the model manager that keeps track of plug-ins in the target platform
	 * 
	 * @return  the model manager that keeps track of plug-ins in the target platform
	 */
	public ExternalModelManager getExternalModelManager() {
		initializeTable();
		return fExternalManager;
	}

	/**
	 * Returns the state containing bundle descriptions for workspace plug-ins and target plug-ins
	 * that form the current PDE state
	 * @return
	 */
	public PDEState getState() {
		initializeTable();
		return fState;
	}
	
	/**
	 * Sets the PDE state.  This method is meant to be called when the target platform
	 * location changes.
	 * 
	 * @param state  the new state
	 */
	public void resetState(PDEState state) {
		if (fState != null && fState.equals(state))
			return;
		// clear all models and add new ones
		int type = IModelProviderEvent.TARGET_CHANGED;
		IModel[] removed = fState.getTargetModels();
		if (removed.length > 0)
			type |= IModelProviderEvent.MODELS_REMOVED;
		IModel[] added = state.getTargetModels();
		if (added.length > 0)
			type |= IModelProviderEvent.MODELS_ADDED;
		modelsChanged(new ModelProviderEvent(
						state,
						type,
						added,
						removed,
						new IModel[0]));

		fireStateChanged(state);
	}
	
	/**
	 * Perform cleanup upon shutting down
	 */
	public void shutdown() {
		fWorkspaceManager.shutdown();
		fExternalManager.shutdown();
		if (fState != null)
			fState.shutdown();
		if (fListeners != null)
			fListeners.clear();
		if (fStateListeners != null)
			fStateListeners.clear();
	}

}
