package org.eclipse.pde.internal;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;


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
}
