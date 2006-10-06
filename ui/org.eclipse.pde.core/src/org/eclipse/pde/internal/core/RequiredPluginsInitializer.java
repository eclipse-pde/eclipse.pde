/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.pde.core.plugin.IPluginModelBase;

/**
 * 
 */
public class RequiredPluginsInitializer extends ClasspathContainerInitializer {

	/**
	 * Constructor for RequiredPluginsInitializer.
	 */
	public RequiredPluginsInitializer() {
		super();
	}

	/**
	 * @see org.eclipse.jdt.core.ClasspathContainerInitializer#initialize(IPath, IJavaProject)
	 */
	public void initialize(IPath containerPath, IJavaProject javaProject)
		throws CoreException {
		IProject project = javaProject.getProject();
		ModelEntry entry =
			PDECore.getDefault().getModelManager().findEntry(project);
		if (entry == null) {
			ModelEntry.updateUnknownClasspathContainer(javaProject);
		} else {
			// check to make sure project in ModelEntry object corresponds to the IProject we are initializing (bug 159989)
			IPluginModelBase base = entry.getWorkspaceModel();
			if (base != null) {
				IResource res = base.getUnderlyingResource();
				if (res != null) {
					if (project.equals(res.getProject()))
						entry.updateClasspathContainer(false);
				}
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jdt.core.ClasspathContainerInitializer#getComparisonID(org.eclipse.core.runtime.IPath, org.eclipse.jdt.core.IJavaProject)
	 */
	public Object getComparisonID(IPath containerPath, IJavaProject project) {
		if (containerPath == null || project == null)
			return null;
			
		return containerPath.segment(0) + "/" + project.getPath().segment(0); //$NON-NLS-1$
	}

	public String getDescription(IPath containerPath, IJavaProject project) {
		return PDECoreMessages.RequiredPluginsClasspathContainer_description;
	}
}
