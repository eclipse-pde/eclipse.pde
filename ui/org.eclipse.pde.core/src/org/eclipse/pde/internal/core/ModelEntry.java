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

import java.io.File;
import java.util.*;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;

public class ModelEntry extends PlatformObject {
	public static final int AUTOMATIC = 0;
	public static final int WORKSPACE = 1;
	public static final int EXTERNAL = 2;
	private String id;
	private IPluginModelBase workspaceModel;
	private IPluginModelBase externalModel;
	private int mode = AUTOMATIC;
	private RequiredPluginsClasspathContainer classpathContainer;
	private boolean inJavaSearch = false;
	private PluginModelManager manager;

	public ModelEntry(PluginModelManager manager, String id) {
		this.manager = manager;
		this.id = id;
	}

	public IPluginModelBase getActiveModel() {
		if (mode == AUTOMATIC) {
			if (workspaceModel != null)
				return workspaceModel;
			return externalModel;
		} else
			return (mode == WORKSPACE) ? workspaceModel : externalModel;
	}

	public String getId() {
		return id;
	}

	public Object[] getChildren() {
		if (workspaceModel == null && externalModel != null) {
			String location = externalModel.getInstallLocation();
			File file = new File(location);
			FileAdapter adapter =
				new EntryFileAdapter(
					this,
					file,
					manager.getFileAdapterFactory());
			return adapter.getChildren();
		}
		return new Object[0];
	}

	public boolean isInJavaSearch() {
		return inJavaSearch;
	}

	void setInJavaSearch(boolean value) {
		this.inJavaSearch = value;
	}

	public void setWorkspaceModel(IPluginModelBase model) {
		this.workspaceModel = model;
		classpathContainer = null;
	}

	public void setExternalModel(IPluginModelBase model) {
		this.externalModel = model;
		classpathContainer = null;
	}
	public IPluginModelBase getWorkspaceModel() {
		return workspaceModel;
	}

	public IPluginModelBase getExternalModel() {
		return externalModel;
	}
	public boolean isEmpty() {
		return workspaceModel == null && externalModel == null;
	}
	public RequiredPluginsClasspathContainer getClasspathContainer() {
		if (classpathContainer == null)
			classpathContainer =
				new RequiredPluginsClasspathContainer(workspaceModel);
		return classpathContainer;
	}
	public void updateClasspathContainer(boolean force, boolean doCheckClasspath)
		throws CoreException {
		if (shouldUpdateClasspathContainer(force, doCheckClasspath)) {
			IProject project = workspaceModel.getUnderlyingResource().getProject();
			IJavaProject[] javaProjects = new IJavaProject[] { JavaCore.create(project)};
			IClasspathContainer[] containers =
				new IClasspathContainer[] { getClasspathContainer()};
			IPath path = new Path(PDECore.CLASSPATH_CONTAINER_ID);
			JavaCore.setClasspathContainer(path, javaProjects, containers, null);
		}
	}
	
	public boolean shouldUpdateClasspathContainer(boolean force, boolean doCheckClasspath) throws CoreException  {
		if (workspaceModel == null)
			return false;
		
		IProject project = workspaceModel.getUnderlyingResource().getProject();
		if (!project.hasNature(JavaCore.NATURE_ID))
			return false;
		
		if (doCheckClasspath && (!workspaceModel.isLoaded() || !usesContainers(JavaCore.create(project))))
			return false;
		
		
		if (force)
			classpathContainer = null;
		RequiredPluginsClasspathContainer container = getClasspathContainer();
		container.reset();
		return true;	
	}
	
	private boolean usesContainers(IJavaProject jProject) throws CoreException {
		IClasspathEntry[] entries = jProject.getRawClasspath();
		for (int i = 0; i < entries.length; i++) {
			IClasspathEntry entry = entries[i];
			if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER
				&& entry.getPath().equals(new Path(PDECore.CLASSPATH_CONTAINER_ID)))
				return true;
		}
		return false;
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

	public boolean isAffected(IPluginBase[] changedPlugins) {
		if (workspaceModel == null || !workspaceModel.isLoaded())
			return false;
		IPluginBase plugin = workspaceModel.getPluginBase();
		for (int i = 0; i < changedPlugins.length; i++) {
			IPluginBase changedPlugin = changedPlugins[i];
			String id = changedPlugin.getId();
			if (plugin.getId().equals(id))
				return true;
			if (isRequired(plugin, changedPlugin))
				return true;
		}
		return false;
	}
	
	private boolean isRequired(IPluginBase plugin, IPluginBase changedPlugin) {
		if (changedPlugin instanceof IFragment)
			return false;		
		return getRequiredIds(plugin).contains(changedPlugin.getId());
	}
	
	private HashSet getRequiredIds(IPluginBase plugin) {
		HashSet set = new HashSet();
		IPluginImport[] imports = plugin.getImports();
		for (int i = 0; i < imports.length; i++) {
			addDependency(imports[i].getId(), set);				
		}
		String id = plugin.getId();
		if (!manager.isOSGiRuntime()
			&& !id.startsWith("org.eclipse.swt")
			&& !id.equals("org.eclipse.core.boot")
			&& !id.equals("org.apache.xerces"))
			set.add("org.eclipse.core.boot");
			set.add("org.eclipse.core.runtime");
		return set;
	}
	
	private void addDependency(String id, HashSet set) {
		if (!set.add(id))
			return;

		ModelEntry entry = manager.findEntry(id);
		if (entry != null) {
			IPluginBase plugin = entry.getActiveModel().getPluginBase();
			IPluginImport[] imports = plugin.getImports();
			for (int i = 0; i < imports.length; i++) {
				if (imports[i].isReexported()) {
					addDependency(imports[i].getId(), set);
				}
			}
		}
	}
	
}