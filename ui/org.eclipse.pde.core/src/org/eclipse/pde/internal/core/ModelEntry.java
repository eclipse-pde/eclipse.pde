/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.plugin.*;

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
		}
		return (mode == WORKSPACE) ? workspaceModel : externalModel;
	}

	public String getId() {
		return id;
	}

	public Object[] getChildren() {
		if (workspaceModel == null && externalModel != null) {
			File file = new File(externalModel.getInstallLocation());
			if (!file.isFile()) {
				FileAdapter adapter =
					new EntryFileAdapter(
						this,
						file,
						manager.getFileAdapterFactory());
				return adapter.getChildren();
			}
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
			try {
				getClasspathContainer().reset();
				JavaCore.setClasspathContainer(path, javaProjects, containers, null);
			} catch (OperationCanceledException e) {
				getClasspathContainer().reset();
				throw e;
			}
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
		/*IClasspathEntry[] entries = jProject.getRawClasspath();
		for (int i = 0; i < entries.length; i++) {
			IClasspathEntry entry = entries[i];
			if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER
				&& entry.getPath().equals(new Path(PDECore.CLASSPATH_CONTAINER_ID)))
				return true;
		}*/
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

	public boolean isAffected(IPluginBase[] changedPlugins, ArrayList oldIds) {
		if (workspaceModel == null || !workspaceModel.isLoaded())
			return false;
		IPluginBase plugin = workspaceModel.getPluginBase();
		for (int i = 0; i < changedPlugins.length; i++) {
			IPluginBase changedPlugin = changedPlugins[i];
			String id = changedPlugin.getId();
			if (id == null)
				return false;
			if (plugin.getId().equals(id))
				return true;
			if (isRequired(plugin, changedPlugin))
				return true;
		}
		for (int i=0; i<oldIds.size(); i++) {
			String oldId = (String)oldIds.get(i);
			if (plugin.getId().equals(oldId))
				return true;
			if (isRequired(plugin, oldId))
				return true;
		}
		return false;
	}
	
	private boolean isRequired(IPluginBase plugin, IPluginBase changedPlugin) {
		if (changedPlugin instanceof IFragment)
			return false;
		return getRequiredIds(plugin).contains(changedPlugin.getId());
	}
	
	private boolean isRequired(IPluginBase plugin, String changedId) {
		return getRequiredIds(plugin).contains(changedId);
	}
	
	private HashSet getRequiredIds(IPluginBase plugin) {
		HashSet set = new HashSet();
		if (plugin instanceof IFragment) {
			addParentPlugin(((IFragment)plugin).getPluginId(), set);
		}
		IPluginImport[] imports = plugin.getImports();
		for (int i = 0; i < imports.length; i++) {
			addDependency(imports[i].getId(), set);				
		}
		String id = plugin.getId();
		if (!manager.isOSGiRuntime()
			&& !id.startsWith("org.eclipse.swt") //$NON-NLS-1$
			&& !id.equals("org.eclipse.core.boot") //$NON-NLS-1$
			&& !id.equals("org.apache.xerces")) //$NON-NLS-1$
			set.add("org.eclipse.core.boot"); //$NON-NLS-1$
			set.add("org.eclipse.core.runtime"); //$NON-NLS-1$
		try {
			IBuild build = ClasspathUtilCore.getBuild(plugin.getPluginModel());
			IBuildEntry entry = (build == null) ? null : build.getEntry(IBuildEntry.JARS_EXTRA_CLASSPATH);
			if (entry != null) {
				String[] tokens = entry.getTokens();
				for (int i = 0; i < tokens.length; i++) {
					IPath path = new Path(tokens[i]);
					String device = path.getDevice();
					if (device == null) {
						if (path.segmentCount() > 1 && path.segment(0).equals("..")) //$NON-NLS-1$
							set.add(path.segment(1));
					} else if (device.equals("platform:")) { //$NON-NLS-1$
						if (path.segmentCount() > 1 && path.segment(0).equals("plugin")) { //$NON-NLS-1$
							set.add(path.segment(1));
						}
					}					
				}
			}
			
		} catch (CoreException e) {
			
		}
		return set;
	}
	
	private void addDependency(String id, HashSet set) {
		if (id == null || !set.add(id))
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
	
	private void addParentPlugin(String id, HashSet set) {
		if (id == null || !set.add(id))
			return;
		
		ModelEntry entry = manager.findEntry(id);
		if (entry != null) {
			IPluginBase plugin = entry.getActiveModel().getPluginBase();
			IPluginImport[] imports = plugin.getImports();
			for (int i = 0; i < imports.length; i++) {
				addDependency(imports[i].getId(), set);
			}
		}
	}
	
}
