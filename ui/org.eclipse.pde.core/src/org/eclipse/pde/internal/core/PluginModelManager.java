/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *     Les Jones <lesojones@gmail.com> - bug 191365
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.osgi.service.resolver.BundleDelta;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.service.resolver.PlatformAdmin;
import org.eclipse.osgi.service.resolver.StateDelta;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelProviderEvent;
import org.eclipse.pde.core.IModelProviderListener;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.LoadTargetDefinitionJob;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.internal.core.target.P2TargetUtils;

public class PluginModelManager implements IModelProviderListener {
	private static final String fExternalPluginListFile = "SavedExternalPluginList.txt"; //$NON-NLS-1$
	private static PluginModelManager fModelManager;

	/**
	 * Job to update class path containers asynchronously. Avoids blocking the UI thread
	 * while saving the manifest editor. The job is given a workspace lock so other jobs can't
	 * run on a stale classpath.
	 */
	class UpdateClasspathsJob extends Job {

		private final List<IJavaProject> fProjects = new ArrayList<>();
		private final List<IClasspathContainer> fContainers = new ArrayList<>();

		/**
		 * Constructs a new job.
		 */
		public UpdateClasspathsJob() {
			super(PDECoreMessages.PluginModelManager_1);
			// The job is given a workspace lock so other jobs can't run on a stale classpath (bug 354993)
			setRule(ResourcesPlugin.getWorkspace().getRoot());
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				boolean more = false;
				do {
					IJavaProject[] projects = null;
					IClasspathContainer[] containers = null;
					synchronized (fProjects) {
						projects = fProjects.toArray(new IJavaProject[fProjects.size()]);
						containers = fContainers.toArray(new IClasspathContainer[fContainers.size()]);
						fProjects.clear();
						fContainers.clear();
					}
					JavaCore.setClasspathContainer(PDECore.REQUIRED_PLUGINS_CONTAINER_PATH, projects, containers, monitor);
					synchronized (fProjects) {
						more = !fProjects.isEmpty();
					}
				} while (more);

			} catch (JavaModelException e) {
				return e.getStatus();
			}
			return Status.OK_STATUS;
		}

		/**
		 * Queues more projects/containers.
		 *
		 * @param projects
		 * @param containers
		 */
		void add(IJavaProject[] projects, IClasspathContainer[] containers) {
			synchronized (fProjects) {
				for (int i = 0; i < containers.length; i++) {
					fProjects.add(projects[i]);
					fContainers.add(containers[i]);
				}
			}
		}

	}

	/**
	 * Job used to update class path containers.
	 */
	private final UpdateClasspathsJob fUpdateJob = new UpdateClasspathsJob();

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
			if (model.getUnderlyingResource() != null) {
				fWorkspaceEntries.add(model);
			} else {
				fExternalEntries.add(model);
			}
		}

		/**
		 * Removes the given model for the workspace list if the model is associated
		 * with workspace resource.  Otherwise, it is removed from the external list.
		 *
		 * @param model  model to be removed from the model entry
		 */
		public void removeModel(IPluginModelBase model) {
			if (model.getUnderlyingResource() != null) {
				fWorkspaceEntries.remove(model);
			} else {
				fExternalEntries.remove(model);
			}
		}
	}

	private final ExternalModelManager fExternalManager; // keeps track of changes in target models
	private final WorkspacePluginModelManager fWorkspaceManager; // keeps track of changes in the workspace
	private PDEState fState; // keeps the combined view of the target and workspace

	private Map<String, LocalModelEntry> fEntries; // a master table keyed by plugin ID and the value is a ModelEntry
	private ArrayList<IPluginModelListener> fListeners; // a list of listeners interested in changes to the plug-in models
	private ArrayList<IStateDeltaListener> fStateListeners; // a list of listeners interested in changes to the PDE/resolver State
	private boolean fCancelled = false;

	/**
	 * Initialize the workspace and external (target) model manager
	 * and add listeners to each one
	 */
	private PluginModelManager() {
		fWorkspaceManager = new WorkspacePluginModelManager();
		fExternalManager = new ExternalModelManager();
		fExternalManager.addModelProviderListener(this);
		fWorkspaceManager.addModelProviderListener(this);
	}

	/**
	 * Provides the instance of {@link PluginModelManager}. If one doesn't exists already than a new one is created and
	 * the workspace and external (target) model manager are initialized with listeners added to each one
	 */
	public static synchronized PluginModelManager getInstance() {
		if (fModelManager == null) {
			fModelManager = new PluginModelManager();
		}
		return fModelManager;
	}

	/**
	 * Shuts down the instance of {@link PluginModelManager} if it exists.
	 */
	public static synchronized void shutdownInstance() {
		if (fModelManager != null) {
			fModelManager.shutdown();
		}
	}

	/**
	 * React to changes in plug-ins in the workspace and/or target
	 */
	@Override
	public void modelsChanged(IModelProviderEvent e) {
		PluginModelDelta delta = new PluginModelDelta();

		// Removes from the master table and the state all workspace plug-ins that have been
		// removed (project closed/deleted) from the workspace.
		// Also if the target location changes, all models from the old target are removed
		if ((e.getEventTypes() & IModelProviderEvent.MODELS_REMOVED) != 0) {
			IModel[] removed = e.getRemovedModels();
			for (IModel element : removed) {
				IPluginModelBase model = (IPluginModelBase) element;
				String id = model.getPluginBase().getId();
				if (id != null) {
					handleRemove(id, model, delta);
				}
			}
		}

		Set<String> addedBSNs = new HashSet<>();
		// Adds to the master table and the state newly created plug-ins in the workspace
		// (ie. new plug-in project or a closed project that has just been re-opened).
		// Also, if the target location changes, we add all plug-ins from the new target
		if ((e.getEventTypes() & IModelProviderEvent.MODELS_ADDED) != 0) {
			IModel[] added = e.getAddedModels();
			for (IModel element : added) {
				IPluginModelBase model = (IPluginModelBase) element;
				String id = model.getPluginBase().getId();
				if (id != null) {
					handleAdd(id, model, delta);
					addedBSNs.add(id);
				}
			}
		}

		// Update the bundle description of plug-ins whose state has changed.
		// A plug-in changes state if the MANIFEST.MF has been touched.
		// or if a plug-in on the Target Platform has changed state (from checked to unchecked,
		// and vice versa.
		if ((e.getEventTypes() & IModelProviderEvent.MODELS_CHANGED) != 0) {
			IModel[] changed = e.getChangedModels();
			for (IModel element : changed) {
				handleChange((IPluginModelBase) element, delta);
			}
		}

		if (fState != null) {
			// if the target location has not changed, incrementally re-resolve the state after processing all the add/remove/modify changes
			// Otherwise, the state is in a good resolved state
			StateDelta stateDelta = null;
			if (addedBSNs.isEmpty()) {
				// resolve incrementally
				stateDelta = fState.resolveState(true);
			} else {
				// resolve based on added bundles, in case there are multiple versions of the added bundles
				stateDelta = fState.resolveState(addedBSNs.toArray(new String[addedBSNs.size()]));
			}
			// trigger a classpath update for all workspace plug-ins affected by the
			// processed batch of changes, run asynch for manifest changes
			updateAffectedEntries(stateDelta, (e.getEventTypes() & IModelProviderEvent.MODELS_CHANGED) != 0);
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
	 * 				changes, may be <code>null</code> to indicate the entire target has changed
	 * @param runAsynch whether classpath updates should be done in an asynchronous job
	 */
	private void updateAffectedEntries(StateDelta delta, boolean runAsynch) {
		Map<IJavaProject, RequiredPluginsClasspathContainer> map = new HashMap<>();
		if (delta == null) {
			// if the delta is null, then the entire target changed.
			// Therefore, we should update the classpath for all workspace plug-ins.
			IPluginModelBase[] models = getWorkspaceModels();
			for (IPluginModelBase model : models) {
				IProject project = model.getUnderlyingResource().getProject();
				try {
					if (project.hasNature(JavaCore.NATURE_ID)) {
						map.put(JavaCore.create(project), new RequiredPluginsClasspathContainer(model));
					}
				} catch (CoreException e) {
				}
			}
		} else {
			BundleDelta[] deltas = delta.getChanges();
			for (BundleDelta bundleDelta : deltas) {
				try {
					// update classpath for workspace plug-ins that are housed in a
					// Java project hand have been affected by the processd model changes.
					IPluginModelBase model = findModel(bundleDelta.getBundle());
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
			for (IPluginModelBase model : models) {
				IProject project = model.getUnderlyingResource().getProject();
				try {
					if (!project.hasNature(JavaCore.NATURE_ID)) {
						continue;
					}
					IJavaProject jProject = JavaCore.create(project);
					if (map.containsKey(jProject)) {
						continue;
					}
					IBuild build = ClasspathUtilCore.getBuild(model);
					if (build != null && build.getEntry(IBuildEntry.SECONDARY_DEPENDENCIES) != null) {
						map.put(jProject, new RequiredPluginsClasspathContainer(model, build));
					}
				} catch (CoreException e) {
				}
			}
		}

		if (!map.isEmpty()) {
			// update class path for all affected workspace plug-ins in one operation
			Iterator<Entry<IJavaProject, RequiredPluginsClasspathContainer>> iterator = map.entrySet().iterator();
			IJavaProject[] projects = new IJavaProject[map.size()];
			IClasspathContainer[] containers = new IClasspathContainer[projects.length];
			int index = 0;
			while (iterator.hasNext()) {
				Entry<IJavaProject, RequiredPluginsClasspathContainer> entry = iterator.next();
				projects[index] = entry.getKey();
				containers[index] = entry.getValue();
				index++;
			}
			// TODO Consider always running in a job - better reporting and cancellation options
			if (runAsynch) {
				// We may be in the UI thread, so the classpath is updated in a job to avoid blocking (bug 376135)
				fUpdateJob.add(projects, containers);
				fUpdateJob.schedule();
			} else {
				// else update synchronously
				try {
					JavaCore.setClasspathContainer(PDECore.REQUIRED_PLUGINS_CONTAINER_PATH, projects, containers, null);
				} catch (JavaModelException e) {
				}
			}
		}
	}

	/**
	 * Notify all interested listeners in changes made to the master table
	 *
	 * @param delta  the delta of changes
	 */
	private void fireDelta(PluginModelDelta delta) {
		if (fListeners != null) {
			for (int i = 0; i < fListeners.size(); i++) {
				fListeners.get(i).modelsChanged(delta);
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
			ListIterator<IStateDeltaListener> li = fStateListeners.listIterator();
			while (li.hasNext()) {
				li.next().stateResolved(delta);
			}
		}
	}

	/**
	 * Notify all interested listeners the cached PDEState has changed
	 *
	 * @param newState	the new PDEState.
	 */
	private void fireStateChanged(PDEState newState) {
		if (fStateListeners != null) {
			ListIterator<IStateDeltaListener> li = fStateListeners.listIterator();
			while (li.hasNext()) {
				li.next().stateChanged(newState.getState());
			}
		}
	}

	/**
	 * Add a listener to the model manager
	 *
	 * @param listener  the listener to be added
	 */
	public void addPluginModelListener(IPluginModelListener listener) {
		if (fListeners == null) {
			fListeners = new ArrayList<>();
		}
		if (!fListeners.contains(listener)) {
			fListeners.add(listener);
		}
	}

	/**
	 * Add a StateDelta listener to model manager
	 *
	 * @param listener	the listener to be added
	 */
	public void addStateDeltaListener(IStateDeltaListener listener) {
		if (fStateListeners == null) {
			fStateListeners = new ArrayList<>();
		}
		if (!fStateListeners.contains(listener)) {
			fStateListeners.add(listener);
		}
	}

	/**
	 * Remove a listener from the model manager
	 *
	 * @param listener the listener to be removed
	 */
	public void removePluginModelListener(IPluginModelListener listener) {
		if (fListeners != null) {
			fListeners.remove(listener);
		}
	}

	/**
	 * Remove a StateDelta listener from the model manager
	 *
	 * @param listener the listener to be removed
	 */
	public void removeStateDeltaListener(IStateDeltaListener listener) {
		if (fStateListeners != null) {
			fStateListeners.remove(listener);
		}
	}

	/**
	 * Returns <code>true</code> if neither the workspace nor target contains plug-ins;
	 * <code>false</code> otherwise.
	 *
	 * @return <code>true</code> if neither the workspace nor target contains plug-ins;
	 * 		<code>false</code> otherwise.
	 */
	public boolean isEmpty() {
		return getEntryTable().isEmpty();
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
	 * Returns whether the model initialization was cancelled by the user.
	 * Other initializations, such as FeatureModelManager should use this
	 * setting to avoid resolving the target platform when the user has chosen
	 * to cancel it previously.
	 *
	 * @return <code>true</code> if the user cancelled the initialization the last time it was run;
	 * 		<code>false</code> otherwise
	 */
	public boolean isCancelled() {
		return fCancelled;
	}

	/**
	 * Clears all existing models and recreates them
	 */
	public void targetReloaded(IProgressMonitor monitor) {
		fEntries = null;
		initializeTable(monitor);
	}

	/**
	 * Allow access to the table only through this getter.
	 * It always calls initialize to make sure the table is initialized.
	 * If more than one thread tries to read the table at the same time,
	 * and the table is not initialized yet, thread2 would wait.
	 * This way there are no partial reads.
	 */
	private Map<String, LocalModelEntry> getEntryTable() {
		initializeTable(null);
		return fEntries;
	}

	/**
	 *
	 * This method must be synchronized so that only one thread
	 * initializes the table, and the rest would block until
	 * the table is initialized.
	 *
	 */
	private synchronized void initializeTable(IProgressMonitor monitor) {
		if (fEntries != null) {
			return;
		}

		// Check if PlatformAdmin service is available (Bug 413450)
		PlatformAdmin pAdmin = Platform.getPlatformAdmin();
		if (pAdmin == null) {
			PDECore.logErrorMessage(PDECoreMessages.PluginModelManager_PlatformAdminMissingErrorMessage);
			fEntries = Collections.emptyMap();
			return;
		}

		SubMonitor subMon = SubMonitor.convert(monitor, PDECoreMessages.PluginModelManager_InitializingPluginModels, 100);
		if (PDECore.DEBUG_MODEL) {
			if (fState == null) {
				System.out.println("\nInitializing PDE models"); //$NON-NLS-1$
			} else {
				System.out.println("\nTarget changed, recreating PDE models"); //$NON-NLS-1$
			}
		}

		PDEState oldState = fState;
		long startTime = System.currentTimeMillis();

		// Cannot assign to fEntries here - will create a race condition with isInitialized()
		Map<String, LocalModelEntry> entries = Collections.synchronizedMap(new TreeMap<String, LocalModelEntry>());
		fCancelled = false;

		ITargetDefinition unresolvedRepoBasedtarget = null;
		try {
			unresolvedRepoBasedtarget = TargetPlatformHelper.getUnresolvedRepositoryBasedWorkspaceTarget();
		} catch (CoreException e) {
			PDECore.log(e);
		}
		if (unresolvedRepoBasedtarget != null && !P2TargetUtils.isProfileValid(unresolvedRepoBasedtarget)) {
			//Workspace target contains unresolved p2 repositories,
			//set empty fState, fExternalManager, fEntries- scheduling target platform resolve
			fState = new PDEState(new URI[0], true, true, subMon);
			fExternalManager.setModels(new IPluginModelBase[0]);
			fEntries = entries;
			LoadTargetDefinitionJob.load(unresolvedRepoBasedtarget);
			return;
		}

		long startTargetModels = System.currentTimeMillis();
		// Target models
		URI[] externalUris = getExternalBundles(subMon.split(40));
		if (subMon.isCanceled()) {
			// If target resolution is cancelled, externalUrls will be empty. Log warning so user knows how to reload the target.
			if (PDECore.DEBUG_MODEL) {
				System.out.println("Target platform initialization cancelled by user"); //$NON-NLS-1$
			}
			PDECore.log(new Status(IStatus.WARNING, PDECore.PLUGIN_ID, PDECoreMessages.PluginModelManager_TargetInitCancelledLog));
			// Set a flag so the feature model manager can avoid starting the target resolve again
			fCancelled = true;
		}

		fState = new PDEState(externalUris, true, true, subMon.split(15));
		fExternalManager.setModels(fState.getTargetModels());
		addToTable(entries, fExternalManager.getAllModels());

		// Check if the saved external bundle list has changed, if so target contents is different and projects should be rebuilt
		boolean externalPluginsChanged = isSavedExternalPluginListDifferent(externalUris);
		saveExternalPluginList(externalUris);

		if (PDECore.DEBUG_MODEL) {
			System.out.println(fState.getTargetModels().length + " target models created in  " + (System.currentTimeMillis() - startTargetModels) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		// Workspace models
		IPluginModelBase[] models = fWorkspaceManager.getPluginModels();
		addToTable(entries, models);
		long startWorkspaceAdditions = System.currentTimeMillis();
		// add workspace plug-ins to the state
		// and remove their target counterparts from the state.
		for (IPluginModelBase model : models) {
			addWorkspaceBundleToState(entries, model);
		}
		subMon.split(15);

		if (PDECore.DEBUG_MODEL) {
			System.out.println(fWorkspaceManager.getModels().length + " workspace models created in  " + (System.currentTimeMillis() - startWorkspaceAdditions) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		// Resolve the state for all external and workspace models
		fState.resolveState(true);
		subMon.split(5);

		fEntries = entries;
		// flush the extension registry cache since workspace data (BundleDescription id's) have changed.
		PDECore.getDefault().getExtensionsRegistry().targetReloaded();
		if (oldState != null) {
			// Need to update classpath entries
			updateAffectedEntries(null, true);
		}

		// Fire a state change event to touch all projects if the target content has changed since last model init
		if (externalPluginsChanged) {
			fireStateChanged(fState);
			if (PDECore.DEBUG_MODEL) {
				System.out.println("Loaded target models differ from saved list, PDE builder will run on all projects."); //$NON-NLS-1$
			}
		}

		subMon.split(25);
		if (PDECore.DEBUG_MODEL) {
			long time = System.currentTimeMillis() - startTime;
			System.out.println("PDE plug-in model initialization complete: " + time + " ms"); //$NON-NLS-1$//$NON-NLS-2$
		}

	}

	/**
	 * Returns an array of URI plug-in locations for external bundles loaded from the
	 * current target platform.
	 *
	 * @param monitor progress monitor
	 * @return array of URLs for external bundles
	 */
	private URI[] getExternalBundles(IProgressMonitor monitor) {
		ITargetDefinition target = null;
		try {
			target = TargetPlatformHelper.getWorkspaceTargetResolved(monitor);
		} catch (CoreException e) {
			PDECore.log(e);
			return new URI[0];
		}

		// Resolution was cancelled
		if (target == null) {
			return new URI[0];
		}

		// Log any known issues with the target platform to warn user
		if (target.isResolved()) {
			if (target.getStatus().getSeverity() == IStatus.ERROR) {
				PDECore.log(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.PluginModelManager_CurrentTargetPlatformContainsErrors, new CoreException(target.getStatus())));
				if (target.getStatus() instanceof MultiStatus) {
					MultiStatus multiStatus = (MultiStatus) target.getStatus();
					for (IStatus childStatus : multiStatus.getChildren()) {
						PDECore.log(childStatus);
					}
				}
			}
		}

		URI[] externalURIs = new URI[0];
		TargetBundle[] bundles = target.getBundles();
		if (bundles != null) {
			List<URI> uris = new ArrayList<>(bundles.length);
			for (TargetBundle bundle : bundles) {
				if (bundle.getStatus().isOK()) {
					uris.add(bundle.getBundleInfo().getLocation());
				}
			}
			externalURIs = uris.toArray(new URI[uris.size()]);
		}

		return externalURIs;

	}

	/**
	 * Adds the given models to the corresponding ModelEntry in the master table
	 *
	 * @param models  the models to be added to the master table
	 */
	private void addToTable(Map<String, LocalModelEntry> entries, IPluginModelBase[] models) {
		for (IPluginModelBase model : models) {
			String id = model.getPluginBase().getId();
			if (id == null) {
				continue;
			}
			LocalModelEntry entry = entries.get(id);
			// create a new entry for the given ID if none already exists
			if (entry == null) {
				entry = new LocalModelEntry(id);
				entries.put(id, entry);
			}
			// add the model to the entry
			entry.addModel(model);
		}
	}

	/**
	 * Add a workspace bundle to the state
	 *
	 * @param model  the workspace model
	 */
	private synchronized void addWorkspaceBundleToState(IPluginModelBase model) {
		addWorkspaceBundleToState(fEntries, model);
	}

	private synchronized void addWorkspaceBundleToState(Map<String, LocalModelEntry> entries, IPluginModelBase model) {
		String id = model.getPluginBase().getId();
		if (id == null) {
			return;
		}

		// update target models by the same ID from the state, if any
		PDEPreferencesManager prefs = PDECore.getDefault().getPreferencesManager();
		boolean preferWorkspaceBundle = prefs.getBoolean(ICoreConstants.WORKSPACE_PLUGINS_OVERRIDE_TARGET);
		ModelEntry entry = entries.get(id);
		if (entry != null) {
			for (IPluginModelBase externalModel : entry.getExternalModels()) {
				if (preferWorkspaceBundle) {
					fState.removeBundleDescription(externalModel.getBundleDescription());
				} else {
					fState.updateBundleDescription(externalModel.getBundleDescription());
				}
			}
		}

		// add new bundle to the state
		fState.addBundle(model, false);

		BundleDescription desc = model.getBundleDescription();
		if (desc != null) {
			// refresh host if a fragment is added to the state.
			// this is necessary because the state will not re-resolve dynamically added fragments
			// on its own
			HostSpecification spec = desc.getHost();
			if (spec != null && ("true".equals(System.getProperty("pde.allowCycles")) //$NON-NLS-1$ //$NON-NLS-2$
					|| isPatchFragment(entries, desc) || desc.getImportPackages().length > 0 || desc.getRequiredBundles().length > 0)) {
				BundleDescription host = (BundleDescription) spec.getSupplier();
				if (host != null) {
					ModelEntry hostEntry = entries.get(host.getName());
					if (hostEntry != null) {
						fState.addBundle(hostEntry.getModel(host), true);
					}
				}
			}
		}
	}

	// Cannot directly call ClasspathUtilCore.isPatchFragment(BundleDescription) since it would cause a loop in our initialization.
	private boolean isPatchFragment(Map<String, LocalModelEntry> entries, BundleDescription desc) {
		ModelEntry entry = entries.get(desc.getSymbolicName());
		if (entry != null) {
			IPluginModelBase base = entry.getModel(desc);
			if (base == null) {
				return false;
			}
			return ClasspathUtilCore.isPatchFragment(base);
		}
		return false;
	}

	/**
	 * Saves the given list of external plugin uris to a file in the metadata folder
	 * @param uris url list to save
	 */
	private void saveExternalPluginList(URI[] uris) {
		File dir = new File(PDECore.getDefault().getStateLocation().toOSString());
		File saveLocation = new File(dir, fExternalPluginListFile);
		try (FileWriter fileWriter = new FileWriter(saveLocation, false)) {
			fileWriter.write("# List of external plug-in models previously loaded. Timestamp: " + System.currentTimeMillis() + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
			for (URI uri : uris) {
				fileWriter.write(uri.toString());
				fileWriter.write("\n"); //$NON-NLS-1$
			}
			fileWriter.flush();
		} catch (IOException e) {
			PDECore.log(e);
		}
	}

	/**
	 * Returns whether the saved list of external plugins is different from the given list of external plugins.
	 *
	 * @param newUris the uris to compare against the saved list
	 * @return <code>true</code> if the two plug-in lists differ, <code>false</code> if they match
	 */
	private boolean isSavedExternalPluginListDifferent(URI[] newUris) {
		Set<String> newExternal = new LinkedHashSet<>();
		for (URI newUri : newUris) {
			newExternal.add(newUri.toString());
		}

		File dir = new File(PDECore.getDefault().getStateLocation().toOSString());
		File saveLocation = new File(dir, fExternalPluginListFile);

		if (!saveLocation.exists()) {
			// If the external list has never been saved, but the target platform has nothing in it, there is no need to build
			return !newExternal.isEmpty();
		}

		Set<String> previousExternal = new LinkedHashSet<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(saveLocation));) {
			while (reader.ready()) {
				String url = reader.readLine();
				if (url != null && !url.trim().isEmpty() && !url.startsWith("#")) { //$NON-NLS-1$
					previousExternal.add(url.trim());
				}
			}
			if (previousExternal.size() != newExternal.size()) {
				return true;
			}
			Iterator<String> iter = previousExternal.iterator();
			while (iter.hasNext()) {
				if (!newExternal.remove(iter.next())) {
					return true;
				}
			}
			if (!newExternal.isEmpty()) {
				return true;
			}
		} catch (IOException e) {
			PDECore.log(e);
		}
		return false;
	}

	/**
	 * Adds a model to the master table and state
	 *
	 * @param id the key
	 * @param model  the model being added
	 */
	private void handleAdd(String id, IPluginModelBase model, PluginModelDelta delta) {
		LocalModelEntry entry = getEntryTable().get(id);

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
			if (desc.getContainingState().equals(fState.fState)) {
				fState.addBundleDescription(desc);
			}
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
		LocalModelEntry entry = getEntryTable().get(id);
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
			} else if (model.getUnderlyingResource() != null && !entry.hasWorkspaceModels()) {
				// re-add enabled external counterparts to the state, if the last workspace
				// plug-in with a particular symbolic name is removed
				IPluginModelBase[] external = entry.getExternalModels();
				for (IPluginModelBase element : external) {
					if (element.isEnabled()) {
						fState.addBundleDescription(element.getBundleDescription());
					}
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
		if (oldID == null && newID == null) {
			return;
		}

		// if the model used to lack a Bundle-SymbolicName header and now it has one,
		// treat it as a regular model addition
		if (oldID == null && newID != null) {
			handleAdd(newID, model, delta);
		} else if (oldID != null && newID == null) {
			// if the model used to have a Bundle-SymbolicName header and now it lost it,
			// treat it as a regular model removal
			handleRemove(oldID, model, delta);
			model.setBundleDescription(null);
		} else if (oldID != null && oldID.equals(newID)) {
			// if the workspace bundle's MANIFEST.MF was touched or
			// if the a target plug-in has now become enabled/checked, update the model
			// in the state
			if (model.isEnabled()) {
				// if the state of an inactive bundle changes (external model un/checked that has an
				// equivalent workspace bundle), then take no action.  We don't want to add the external
				// model to the state when it is enabled if we have a workspace bundle already in the state.
				ModelEntry entry = getEntryTable().get(oldID);
				IPluginModelBase[] activeModels = entry.getActiveModels();
				boolean isActive = false;
				for (IPluginModelBase activeModel : activeModels) {
					if (activeModel == model) {
						isActive = true;
						break;
					}
				}
				if (isActive) {
					// refresh everything related to this bundle model id
					fEntries.remove(newID);
					fState.removeBundleDescription(desc);
					for (int i = 0; i < fExternalManager.getAllModels().length; i++) {
						IPluginModelBase modelExternal = fExternalManager.getAllModels()[i];
						if (modelExternal.getPluginBase().getId() != null) {
							if (modelExternal.getPluginBase().getId().equals(newID)) {
								addToTable(fEntries, new IPluginModelBase[] { modelExternal });
							}
						}
					}
					IPluginModelBase[] models = fWorkspaceManager.getPluginModels();
					for (IPluginModelBase modelWorkspace : models) {
						if (modelWorkspace.getPluginBase().getId() != null) {
							if (modelWorkspace.getPluginBase().getId().equals(newID)) {
								addToTable(fEntries, new IPluginModelBase[] { modelWorkspace });
								addWorkspaceBundleToState(fEntries, modelWorkspace);
							}
						}
					}
				}
			} else {
				// if the target plug-in has become disabled/unchecked, remove its bundle
				// description from the state
				fState.removeBundleDescription(model.getBundleDescription());
			}
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
		if ("system.bundle".equals(id)) { //$NON-NLS-1$
			id = getSystemBundleId();
		}
		return id == null ? null : (ModelEntry) getEntryTable().get(id);
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
		initializeTable(null);
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
		ModelEntry entry = (desc != null) ? findEntry(desc.getSymbolicName()) : null;
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
		ArrayList<IPluginModelBase> result = new ArrayList<>(size);
		Iterator<LocalModelEntry> iter = getEntryTable().values().iterator();
		while (iter.hasNext()) {
			ModelEntry entry = iter.next();
			IPluginModelBase[] models = entry.getActiveModels();
			for (IPluginModelBase model : models) {
				if (model instanceof IPluginModel || includeFragments) {
					result.add(model);
				}
			}
		}
		return result.toArray(new IPluginModelBase[result.size()]);
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
		ArrayList<IPluginModelBase> result = new ArrayList<>(size);
		Iterator<LocalModelEntry> iter = getEntryTable().values().iterator();
		while (iter.hasNext()) {
			ModelEntry entry = iter.next();
			IPluginModelBase[] models = entry.hasWorkspaceModels() ? entry.getWorkspaceModels() : entry.getExternalModels();
			for (IPluginModelBase model : models) {
				if (model instanceof IPluginModel || includeFragments) {
					result.add(model);
				}
			}
		}
		return result.toArray(new IPluginModelBase[result.size()]);
	}

	/**
	 * Returns all plug-in models in the target platform
	 *
	 * @return  all plug-ins in the target platform
	 */
	public IPluginModelBase[] getExternalModels() {
		initializeTable(null);
		return fExternalManager.getAllModels();
	}

	/**
	 * Returns all plug-in models in the workspace
	 *
	 * @return all plug-in models in the workspace
	 */
	public IPluginModelBase[] getWorkspaceModels() {
		initializeTable(null);
		return fWorkspaceManager.getPluginModels();
	}

	/**
	 * Return the model manager that keeps track of plug-ins in the target platform
	 *
	 * @return  the model manager that keeps track of plug-ins in the target platform
	 */
	public ExternalModelManager getExternalModelManager() {
		initializeTable(null);
		return fExternalManager;
	}

	/**
	 * Returns the state containing bundle descriptions for workspace plug-ins and target plug-ins
	 * that form the current PDE state
	 */
	public PDEState getState() {
		initializeTable(null);
		return fState;
	}

	/**
	 * Returns the id of the system bundle currently in the resolver state
	 *
	 * @return a String with the id of the system.bundle
	 */
	public String getSystemBundleId() {
		return getState().getSystemBundle();
	}

	/**
	 * Perform cleanup upon shutting down
	 */
	protected void shutdown() {
		fWorkspaceManager.shutdown();
		fExternalManager.shutdown();

		if (fListeners != null) {
			fListeners.clear();
		}
		if (fStateListeners != null) {
			fStateListeners.clear();
		}
	}

	public void addExtensionDeltaListener(IExtensionDeltaListener listener) {
		fWorkspaceManager.addExtensionDeltaListener(listener);
	}

	public void removeExtensionDeltaListener(IExtensionDeltaListener listener) {
		fWorkspaceManager.removeExtensionDeltaListener(listener);
	}

	/**
	 * Called when the bundle root for a project is changed.
	 *
	 * @param project
	 */
	public void bundleRootChanged(IProject project) {
		fWorkspaceManager.initialize();
		fWorkspaceManager.removeModel(project);
		if (fWorkspaceManager.isInterestingProject(project)) {
			fWorkspaceManager.createModel(project, false);
			Object model = fWorkspaceManager.getModel(project);
			fWorkspaceManager.addChange(model, IModelProviderEvent.MODELS_CHANGED);
		}
		fWorkspaceManager.processModelChanges();
	}

}
