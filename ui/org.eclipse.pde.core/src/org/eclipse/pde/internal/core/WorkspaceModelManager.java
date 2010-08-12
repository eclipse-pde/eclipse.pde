/*******************************************************************************
 *  Copyright (c) 2003, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.*;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelProviderEvent;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.team.core.RepositoryProvider;

public abstract class WorkspaceModelManager extends AbstractModelManager implements IResourceChangeListener, IResourceDeltaVisitor {

	public static boolean isPluginProject(IProject project) {
		if (project.isOpen())
			return PDEProject.getManifest(project).exists() || PDEProject.getPluginXml(project).exists() || PDEProject.getFragmentXml(project).exists();
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

	protected Map fModels = null;
	private ArrayList fChangedModels;

	protected synchronized void initialize() {
		if (fModels != null)
			return;

		fModels = Collections.synchronizedMap(new HashMap());
		IProject[] projects = PDECore.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projects.length; i++) {
			if (isInterestingProject(projects[i]))
				createModel(projects[i], false);
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
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

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
	 */
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
			if (fChangedModels == null)
				fChangedModels = new ArrayList();
			ModelChange change = new ModelChange((IModel) model, eventType);
			if (!fChangedModels.contains(change))
				fChangedModels.add(change);
		}
	}

	protected void processModelChanges() {
		processModelChanges("org.eclipse.pde.core.IModelProviderEvent", fChangedModels); //$NON-NLS-1$
		fChangedModels = null;
	}

	protected void processModelChanges(String changeId, ArrayList changedModels) {
		if (changedModels == null)
			return;

		if (changedModels.size() == 0) {
			return;
		}

		ArrayList added = new ArrayList();
		ArrayList removed = new ArrayList();
		ArrayList changed = new ArrayList();
		for (ListIterator li = changedModels.listIterator(); li.hasNext();) {
			ModelChange change = (ModelChange) li.next();
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
		if (added.size() > 0)
			type |= IModelProviderEvent.MODELS_ADDED;
		if (removed.size() > 0)
			type |= IModelProviderEvent.MODELS_REMOVED;
		if (changed.size() > 0)
			type |= IModelProviderEvent.MODELS_CHANGED;

		if (type != 0) {
			createAndFireEvent(changeId, type, added, removed, changed);
		}
	}

	protected void loadModel(IModel model, boolean reload) {
		IFile file = (IFile) model.getUnderlyingResource();
		InputStream stream = null;
		try {
			stream = new BufferedInputStream(file.getContents(true));
			if (reload)
				model.reload(stream, false);
			else
				model.load(stream, false);
		} catch (CoreException e) {
			PDECore.logException(e);
		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (IOException e) {
				PDECore.log(e);
			}
		}
	}

	protected void createAndFireEvent(String eventId, int type, Collection added, Collection removed, Collection changed) {
		if (eventId.equals("org.eclipse.pde.core.IModelProviderEvent")) { //$NON-NLS-1$
			final ModelProviderEvent event = new ModelProviderEvent(this, type, (IModel[]) added.toArray(new IModel[added.size()]), (IModel[]) removed.toArray(new IModel[removed.size()]), (IModel[]) changed.toArray(new IModel[changed.size()]));
			fireModelProviderEvent(event);
		}
	}
}
