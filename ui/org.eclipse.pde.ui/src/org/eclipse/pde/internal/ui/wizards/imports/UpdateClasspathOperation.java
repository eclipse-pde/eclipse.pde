/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.ui.wizards.imports;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;

import org.eclipse.jdt.core.*;
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.pde.internal.core.TargetPlatform;

public class UpdateClasspathOperation implements IRunnableWithProgress {

	private IPluginModelBase[] models;

	public UpdateClasspathOperation(IPluginModelBase[] models) {
		this.models = models;
	}

	private static IPath getSourceAttachmentPath(
		IProject project,
		IPath jarPath) {
		IPath sourcePath = getSourcePath(jarPath);
		if (sourcePath == null)
			return null;
		IWorkspaceRoot root = project.getWorkspace().getRoot();
		if (root.findMember(sourcePath) != null) {
			return sourcePath;
		}
		return null;
	}

	static IPath getExpandedPath(IPath path) {
		String first = path.segment(0);
		if (first != null) {
			IPath rest = path.removeFirstSegments(1);
			if (first.equals("$ws$")) {
				path =
					new Path("ws").append(TargetPlatform.getWS()).append(rest);
			} else if (first.equals("$os$")) {
				path =
					new Path("os").append(TargetPlatform.getOS()).append(rest);
			} else if (first.equals("$nl$")) {
				path =
					new Path("nl").append(TargetPlatform.getNL()).append(rest);
			} else if (first.equals("$arch$")) {
				path =
					new Path("arch").append(TargetPlatform.getOSArch()).append(
						rest);
			}
		}
		return path;
	}

	static IPath getSourcePath(IPath jarPath) {
		jarPath = getExpandedPath(jarPath);
		String libName = jarPath.lastSegment();
		int idx = libName.lastIndexOf('.');
		if (idx != -1) {
			String srcName = libName.substring(0, idx) + "src.zip";
			IPath path = jarPath.removeLastSegments(1).append(srcName);
			return path;
		} else
			return null;
	}

	private static IPath getLibraryPath(
		IProject project,
		IPluginLibrary curr) {
		return getLibraryPath(project, curr.getName());
	}

	private static IPath getLibraryPath(IProject project, String libraryName) {
		IPath path = new Path(libraryName);
		path = getExpandedPath(path);
		return project.getFullPath().append(path);
	}

	public static IClasspathEntry getLibraryEntry(
		IProject project,
		IPluginLibrary library,
		boolean exported) {
		return getLibraryEntry(project, library.getName(), exported);
	}

	private static IClasspathEntry getLibraryEntry(
		IProject project,
		String libraryName,
		boolean exported) {
		IPath jarPath = getLibraryPath(project, libraryName);
		IPath srcAttach = getSourceAttachmentPath(project, jarPath);
		IPath srcRoot = srcAttach != null ? Path.EMPTY : null;
		return JavaCore.newLibraryEntry(jarPath, srcAttach, srcRoot, exported);
	}
	/**
	 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor)
		throws InvocationTargetException, InterruptedException {
			try {
				UpdateClasspathAction.doUpdateClasspath(monitor,models);
			} catch (CoreException e) {
			}
	}

}