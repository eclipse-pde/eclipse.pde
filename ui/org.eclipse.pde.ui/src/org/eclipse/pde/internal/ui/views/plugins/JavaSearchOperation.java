/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.plugins;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.SearchablePluginsManager;
import org.eclipse.pde.internal.core.util.CoreUtility;

public class JavaSearchOperation implements IRunnableWithProgress {
	
	private IPluginModelBase[] fModels;
	private boolean fAdd;

	public JavaSearchOperation(IPluginModelBase[] models, boolean add) {
		fModels = models;
		fAdd = add;
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException {
		try {
			createProxyProject(monitor);
			SearchablePluginsManager manager = PDECore.getDefault().getSearchablePluginsManager();
			if (fAdd)
				manager.addToJavaSearch(fModels);
			else
				manager.removeFromJavaSearch(fModels);
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		} finally {
			monitor.done();
		}
	}

	public IProject createProxyProject(IProgressMonitor monitor) throws CoreException {
		IWorkspaceRoot root = PDECore.getWorkspace().getRoot();
		IProject project = root.getProject(SearchablePluginsManager.PROXY_PROJECT_NAME);
		if (project.exists())
			return project;
		
		monitor.beginTask("", 5); //$NON-NLS-1$
		project.create(new SubProgressMonitor(monitor, 1));
		project.open(new SubProgressMonitor(monitor, 1));
		CoreUtility.addNatureToProject(project, JavaCore.NATURE_ID, new SubProgressMonitor(monitor, 1));
		IJavaProject jProject = JavaCore.create(project);
		jProject.setOutputLocation(project.getFullPath(), new SubProgressMonitor(monitor, 1));
		computeClasspath(jProject, new SubProgressMonitor(monitor, 1));
		return project;
	}

	private void computeClasspath(IJavaProject project, IProgressMonitor monitor) throws CoreException {
		IClasspathEntry[] classpath = new IClasspathEntry[2];
		classpath[0] = JavaCore.newContainerEntry(JavaRuntime.newDefaultJREContainerPath());
		classpath[1] = JavaCore.newContainerEntry(PDECore.JAVA_SEARCH_CONTAINER_PATH);
		try {
			project.setRawClasspath(classpath, monitor);
		} catch (JavaModelException e) {
		}
	}

}
