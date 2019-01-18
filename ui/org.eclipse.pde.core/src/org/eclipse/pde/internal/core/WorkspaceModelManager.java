/*******************************************************************************
 *  Copyright (c) 2003, 2018 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.ListIterator;
import java.util.Map;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelProviderEvent;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.team.core.RepositoryProvider;

public abstract class WorkspaceModelManager extends AbstractModelManager implements IResourceChangeListener, IResourceDeltaVisitor {

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

	class ModelChange {
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

	protected Map<IProject, IModel> fModels = null;
	private ArrayList<ModelChange> fChangedModels;

	protected synchronized void initialize() {
		if (fModels != null) {
			return;
		}

		fModels = Collections.synchronizedMap(new LinkedHashMap<IProject, IModel>());
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

	protected abstract void addListeners();

	protected Object getModel(IProject project) {
		initialize();
		return fModels.get(project);
	}

	protected Object[] getModels() {
		initialize();
		return fModels.values().toArray();
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
			if (!resource.isDerived()) {
				switch (resource.getType()) {

					case IResource.ROOT :
						return true;
					case IResource.PROJECT : {
						IProject project = (IProject) resource;
						if (isInterestingProject(project) && (delta.getKind() == IResourceDelta.ADDED || (delta.getFlags() & IResourceDelta.OPEN) != 0)) {
							createModel(project, true);
							return false;
						} else if (delta.getKind() == IResourceDelta.REMOVED) {
							removeModel(project);
							return false;
						}
						return true;
					}
					case IResource.FOLDER :
						return isInterestingFolder((IFolder) resource);
					case IResource.FILE :
						// do not process
						if (isContentChange(delta)) {
							handleFileDelta(delta);
							return false;
						}
				}
			}
		}
		return false;
	}

	private boolean isContentChange(IResourceDelta delta) {
		int kind = delta.getKind();
		return (kind == IResourceDelta.ADDED || kind == IResourceDelta.REMOVED || (kind == IResourceDelta.CHANGED && (delta.getFlags() & IResourceDelta.CONTENT) != 0));
	}

	protected boolean isInterestingFolder(IFolder folder) {
		return false;
	}

	protected abstract void handleFileDelta(IResourceDelta delta);

	protected Object removeModel(IProject project) {
		Object model = fModels != null ? fModels.remove(project) : null;
		addChange(model, IModelProviderEvent.MODELS_REMOVED);
		return model;
	}

	protected void addChange(Object model, int eventType) {
		if (model instanceof IModel) {
			if (fChangedModels == null) {
				fChangedModels = new ArrayList<>();
			}
			ModelChange change = new ModelChange((IModel) model, eventType);
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
