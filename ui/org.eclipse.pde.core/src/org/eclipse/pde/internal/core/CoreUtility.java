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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;


public class CoreUtility {
	
public static void addNatureToProject(
	IProject proj,
	String natureId,
	IProgressMonitor monitor)
	throws CoreException {
	IProjectDescription description = proj.getDescription();
	String[] prevNatures = description.getNatureIds();
	String[] newNatures = new String[prevNatures.length + 1];
	System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
	newNatures[prevNatures.length] = natureId;
	description.setNatureIds(newNatures);
	proj.setDescription(description, monitor);
}
public static void createFolder(
	IFolder folder,
	boolean force,
	boolean local,
	IProgressMonitor monitor)
	throws CoreException {
	if (!folder.exists()) {
		IContainer parent = folder.getParent();
		if (parent instanceof IFolder) {
			createFolder((IFolder) parent, force, local, monitor);
		}
		folder.create(force, local, monitor);
	}
}

public static void createProject(IProject project, IPath location, IProgressMonitor monitor) 
							throws CoreException {
	IPath defaultLocation = Platform.getLocation();
	if (defaultLocation.equals(location)==false) {
		IProjectDescription desc = project.getWorkspace().
		                   newProjectDescription(project.getName());
		desc.setLocation(location.append(project.getFullPath()));
		project.create(desc, monitor);
	}
	else project.create(monitor);
}
}
