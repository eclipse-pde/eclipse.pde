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

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.core.plugin.IPluginModelBase;

public class ModelEntry extends PlatformObject {
	private String id;
	private IPluginModelBase fWorkspaceModel;
	private IPluginModelBase fExternalModel;
	private RequiredPluginsClasspathContainer fClasspathContainer;
	private boolean fInJavaSearch;
	private PluginModelManager fManager;

	public ModelEntry(PluginModelManager manager, String id) {
		this.fManager = manager;
		this.id = id;
	}

	public IPluginModelBase getActiveModel() {
		return (fWorkspaceModel != null) ? fWorkspaceModel : fExternalModel;
	}

	public String getId() {
		return id;
	}

	public Object[] getChildren() {
		if (fWorkspaceModel == null && fExternalModel != null) {
			File file = new File(fExternalModel.getInstallLocation());
			if (!file.isFile()) {
				FileAdapter adapter =
					new EntryFileAdapter(
						this,
						file,
						fManager.getFileAdapterFactory());
				return adapter.getChildren();
			}
		}
		return new Object[0];
	}

	public boolean isInJavaSearch() {
		return fInJavaSearch;
	}

	void setInJavaSearch(boolean value) {
		this.fInJavaSearch = value;
	}

	public void setWorkspaceModel(IPluginModelBase model) {
		this.fWorkspaceModel = model;
		fClasspathContainer = null;
	}

	public void setExternalModel(IPluginModelBase model) {
		this.fExternalModel = model;
		fClasspathContainer = null;
	}
	public IPluginModelBase getWorkspaceModel() {
		return fWorkspaceModel;
	}

	public IPluginModelBase getExternalModel() {
		return fExternalModel;
	}
	public boolean isEmpty() {
		return fWorkspaceModel == null && fExternalModel == null;
	}
	public RequiredPluginsClasspathContainer getClasspathContainer(boolean reset) {
		if (reset)
			fClasspathContainer = null;
		
		if (fClasspathContainer == null)
			fClasspathContainer =
				new RequiredPluginsClasspathContainer(fWorkspaceModel);
		return fClasspathContainer;
	}
	public void updateClasspathContainer(boolean doCheckClasspath)
		throws CoreException {
		if (shouldUpdateClasspathContainer(doCheckClasspath)) {
			IProject project = fWorkspaceModel.getUnderlyingResource().getProject();
			IJavaProject[] javaProjects = new IJavaProject[] { JavaCore.create(project)};
			IClasspathContainer[] containers =
				new IClasspathContainer[] { getClasspathContainer(true)};
			IPath path = new Path(PDECore.CLASSPATH_CONTAINER_ID);
			try {
				JavaCore.setClasspathContainer(path, javaProjects, containers, null);
			} catch (OperationCanceledException e) {
				getClasspathContainer(false).reset();
				throw e;
			}
		}
	}
	
	public boolean shouldUpdateClasspathContainer(boolean doCheckClasspath) throws CoreException  {
		if (fWorkspaceModel == null)
			return false;
		
		IProject project = fWorkspaceModel.getUnderlyingResource().getProject();
		if (!project.hasNature(JavaCore.NATURE_ID))
			return false;
		
		if (doCheckClasspath && (!fWorkspaceModel.isLoaded()))
			return false;
		
		return true;	
	}
	
	public static void updateUnknownClasspathContainer(IJavaProject javaProject)
		throws CoreException {
		if (javaProject == null)
			return;
		IPath path = new Path(PDECore.CLASSPATH_CONTAINER_ID);
		JavaCore.setClasspathContainer(
			path,
			new IJavaProject[] { javaProject },
			new IClasspathContainer[] {
				 new RequiredPluginsClasspathContainer(null)},
			null);
	}
	
}
