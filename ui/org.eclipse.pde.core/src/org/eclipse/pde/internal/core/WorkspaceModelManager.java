/*******************************************************************************
 *  Copyright (c) 2003, 2022 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Hannes Wellmann - react to changes of Bundle-Root setting and in derived folders
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelProviderEvent;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.team.core.RepositoryProvider;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public abstract class WorkspaceModelManager<T> extends AbstractModelManager
		implements IResourceChangeListener, IResourceDeltaVisitor {

	public static boolean isPluginProject(IProject project) {
		if (project == null) {
			return false;
		}
		if (project.isOpen()) {
			return PDEProject.getManifest(project).exists() || PDEProject.getPluginXml(project).exists() || PDEProject.getFragmentXml(project).exists();
		}
		return false;
	}

	public static boolean isFeatureProject(IProject project) {
		return project.isOpen() && PDEProject.getFeatureXml(project).exists();
	}

	public static boolean isBinaryProject(IProject project) {
		try {
			if (project.isOpen()) {
				String binary = project.getPersistentProperty(PDECore.EXTERNAL_PROJECT_PROPERTY);
				if (binary != null) {
					RepositoryProvider provider = RepositoryProvider.getProvider(project);
					return provider == null || provider instanceof BinaryRepositoryProvider;
				}
			}
		} catch (CoreException e) {
			PDECore.logException(e);
		}
		return false;
	}

	public static boolean isUnsharedProject(IProject project) {
		return RepositoryProvider.getProvider(project) == null || isBinaryProject(project);
	}

	static class ModelChange {
		IModel model;
		int type;

		public ModelChange(IModel model, int type) {
			this.model = model;
			this.type = type;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ModelChange) {
				ModelChange change = (ModelChange) obj;
				IProject project = change.model.getUnderlyingResource().getProject();
				int type = change.type;
				return model.getUnderlyingResource().getProject().equals(project) && this.type == type;
			}
			return false;
		}
	}

	private Map<IProject, T> fModels = null;
	private ArrayList<ModelChange> fChangedModels;
	private final IPreferenceChangeListener bundleRootChangedListener = createBundleRootChangeListener();

	protected Map<IProject, T> getModelsMap() {
		ensureModelsMapCreated();
		return fModels;
	}

	private void ensureModelsMapCreated() {
		if (fModels == null) {
			fModels = Collections.synchronizedMap(new LinkedHashMap<>());
		}
	}

	protected synchronized void initialize() {
		if (fModels != null) {
			return;
		}

		ensureModelsMapCreated();

		IProject[] projects = PDECore.getWorkspace().getRoot().getProjects();
		for (IProject project : projects) {
			if (isInterestingProject(project)) {
				createModel(project, false);
			}
		}
		addListeners();
	}

	protected abstract boolean isInterestingProject(IProject project);

	protected abstract void createModel(IProject project, boolean notify);

	protected abstract T removeModel(IProject project);

	protected void addListeners() {
		IWorkspace workspace = PDECore.getWorkspace();
		workspace.addResourceChangeListener(this, IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.POST_CHANGE);
		if (bundleRootChangedListener != null) {
			Arrays.stream(workspace.getRoot().getProjects()).forEach(this::addBundleRootChangedListener);
		}
	}

	@Override
	protected void removeListeners() {
		PDECore.getWorkspace().removeResourceChangeListener(this);
		super.removeListeners();
	}

	protected T getModel(IProject project) {
		initialize();
		return fModels.get(project);
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		switch (event.getType()) {
			case IResourceChangeEvent.POST_CHANGE :
				handleResourceDelta(event.getDelta());
				processModelChanges();
				break;
			case IResourceChangeEvent.PRE_CLOSE :
				removeModel((IProject) event.getResource());
				processModelChanges();
				break;
		}
	}

	private void handleResourceDelta(IResourceDelta delta) {
		try {
			delta.accept(this);
		} catch (CoreException e) {
			PDECore.logException(e);
		}
	}

	@Override
	public boolean visit(IResourceDelta delta) throws CoreException {
		if (delta != null) {
			final IResource resource = delta.getResource();
			switch (resource.getType())
				{
				case IResource.ROOT:
					return true;
				case IResource.PROJECT: {
					IProject project = (IProject) resource;
					boolean addedOrOpened = delta.getKind() == IResourceDelta.ADDED
							|| (delta.getFlags() & IResourceDelta.OPEN) != 0;
					if (addedOrOpened && bundleRootChangedListener != null) {
						addBundleRootChangedListener(project);
					}
					if (isInterestingProject(project) && addedOrOpened) {
						createModel(project, true);
						return false;
					} else if (delta.getKind() == IResourceDelta.REMOVED) {
						removeModel(project);
						return false;
					}
					return true;
				}
				case IResource.FOLDER:
					return isInterestingFolder((IFolder) resource);
				case IResource.FILE:
					// do not process
					if (isContentChange(delta)) {
						handleFileDelta(delta);
						return false;
					}
				}
		}
		return false;
	}

	private void addBundleRootChangedListener(IProject project) {
		IEclipsePreferences pdeNode = new ProjectScope(project).getNode(PDECore.PLUGIN_ID);
		// Always add the same listener instance to not add multiple listeners
		// in case of repetitive project opening/closing
		pdeNode.addPreferenceChangeListener(bundleRootChangedListener);
	}

	protected IPreferenceChangeListener createBundleRootChangeListener() {
		return e -> {
			if (PDEProject.BUNDLE_ROOT_PATH.equals(e.getKey()) && !isInRemovedBranch(e.getNode())) {
				String projectName = Path.forPosix(e.getNode().absolutePath()).segment(1);
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);

				// bundle-root changed (null value means default): try to
				// (re-)load model from new bundle-root path (may delete it)
				if (getModel(project) != null) {
					removeModel(project);
				}
				createModel(project, true);
			}
		};
	}

	private static boolean isInRemovedBranch(Preferences node) {
		return !Stream.iterate(node, Objects::nonNull, Preferences::parent).allMatch(n -> {
			try { // Returns true if existing node is about to be removed
				return n.nodeExists(""); //$NON-NLS-1$
			} catch (BackingStoreException e1) {
				return false;
			}
		});
	}

	private boolean isContentChange(IResourceDelta delta) {
		int kind = delta.getKind();
		return (kind == IResourceDelta.ADDED || kind == IResourceDelta.REMOVED || (kind == IResourceDelta.CHANGED && (delta.getFlags() & IResourceDelta.CONTENT) != 0));
	}

	protected boolean isInterestingFolder(IFolder folder) {
		return false;
	}

	protected abstract void handleFileDelta(IResourceDelta delta);

	protected void addChange(IModel model, int eventType) {
		if (model != null) {
			if (fChangedModels == null) {
				fChangedModels = new ArrayList<>();
			}
			ModelChange change = new ModelChange(model, eventType);
			if (!fChangedModels.contains(change)) {
				fChangedModels.add(change);
			}
		}
	}

	protected void processModelChanges() {
		processModelChanges("org.eclipse.pde.core.IModelProviderEvent", fChangedModels); //$NON-NLS-1$
		fChangedModels = null;
	}

	protected void processModelChanges(String changeId, ArrayList<ModelChange> changedModels) {
		if (changedModels == null) {
			return;
		}

		if (changedModels.isEmpty()) {
			return;
		}

		ArrayList<IModel> added = new ArrayList<>();
		ArrayList<IModel> removed = new ArrayList<>();
		ArrayList<IModel> changed = new ArrayList<>();
		for (ListIterator<ModelChange> li = changedModels.listIterator(); li.hasNext();) {
			ModelChange change = li.next();
			switch (change.type) {
				case IModelProviderEvent.MODELS_ADDED :
					added.add(change.model);
					break;
				case IModelProviderEvent.MODELS_REMOVED :
					removed.add(change.model);
					break;
				case IModelProviderEvent.MODELS_CHANGED :
					changed.add(change.model);
			}
		}

		int type = 0;
		if (!added.isEmpty()) {
			type |= IModelProviderEvent.MODELS_ADDED;
		}
		if (!removed.isEmpty()) {
			type |= IModelProviderEvent.MODELS_REMOVED;
		}
		if (!changed.isEmpty()) {
			type |= IModelProviderEvent.MODELS_CHANGED;
		}

		if (type != 0) {
			createAndFireEvent(changeId, type, added, removed, changed);
		}
	}

	protected void loadModel(IModel model, boolean reload) {
		IFile file = (IFile) model.getUnderlyingResource();
		try (InputStream stream = new BufferedInputStream(file.getContents(true));) {

			if (reload) {
				model.reload(stream, false);
			} else {
				model.load(stream, false);
			}
		} catch (CoreException | IOException e) {
			PDECore.logException(e);
		}
	}

	protected void createAndFireEvent(String eventId, int type, Collection<IModel> added, Collection<IModel> removed, Collection<IModel> changed) {
		if (eventId.equals("org.eclipse.pde.core.IModelProviderEvent")) { //$NON-NLS-1$
			final ModelProviderEvent event = new ModelProviderEvent(this, type, added.toArray(new IModel[added.size()]), removed.toArray(new IModel[removed.size()]), changed.toArray(new IModel[changed.size()]));
			fireModelProviderEvent(event);
		}
	}
}
