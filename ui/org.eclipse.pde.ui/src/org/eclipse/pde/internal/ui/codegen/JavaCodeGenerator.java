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
package org.eclipse.pde.internal.ui.codegen;

import java.io.*;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;


public abstract class JavaCodeGenerator {
	private IProject project;
	private IFolder sourceFolder;
	private String qualifiedClassName;

public JavaCodeGenerator(IProject project, String qualifiedClassName) {
	this.project = project;
	this.qualifiedClassName = qualifiedClassName;
}
public JavaCodeGenerator(IProject project, IFolder sourceFolder, String qualifiedClassName) {
	this.project = project;
	this.sourceFolder = sourceFolder;
	this.qualifiedClassName = qualifiedClassName;
}
public static void ensureFolderExist(IWorkspace workspace, IPath folderPath) throws CoreException {
	if (!workspace.getRoot().exists(folderPath)) {
		IFolder folder = workspace.getRoot().getFolder(folderPath);
		folder.create(true, true, null);
	}
}
private void ensureFolderExist(IPath folderPath) throws CoreException {
	IWorkspace workspace = project.getWorkspace();
	if (!workspace.getRoot().exists(folderPath)) {
		IFolder folder = workspace.getRoot().getFolder(folderPath);
		folder.create(true, true, null);
	}
}
private void ensureFoldersExist(String packageName) throws CoreException {
	StringTokenizer stok = new StringTokenizer(packageName, ".");
	IPath fpath=sourceFolder!=null?sourceFolder.getFullPath():project.getFullPath();
	ensureFolderExist(fpath);
	while (stok.hasMoreTokens()) {
		String tok = stok.nextToken();
		fpath = fpath.append(tok);
		ensureFolderExist(fpath);
	}
}
public static void ensureFoldersExist(IProject project, String name, String delimeter) throws CoreException {
	StringTokenizer stok = new StringTokenizer(name, delimeter);
	IPath fpath=project.getFullPath();
	while (stok.hasMoreTokens()) {
		String tok = stok.nextToken();
		fpath = fpath.append(tok);
		ensureFolderExist(project.getWorkspace(), fpath);
	}
}
public IFile generate(IProgressMonitor monitor) throws CoreException {
	int nameloc = qualifiedClassName.lastIndexOf('.');
	String packageName = qualifiedClassName.substring(0, nameloc);
	String className = qualifiedClassName.substring(nameloc + 1);

	String javaFileName = className + ".java";
	ensureFoldersExist(packageName);
	IWorkspace workbench = project.getWorkspace();
	IPath path =
		sourceFolder != null ? sourceFolder.getFullPath() : project.getFullPath();
	path = path.append(packageName.replace('.', '/'));
	IFile file = workbench.getRoot().getFile(path.append(javaFileName));
	StringWriter swriter = new StringWriter();
	PrintWriter writer = new PrintWriter(swriter);
	generateContents(packageName, className, writer);
	writer.flush();
	try {
		swriter.close();
		ByteArrayInputStream stream =
			//new ByteArrayInputStream(swriter.toString().getBytes("UTF8"));
			// we must write Java code in Native encoding, not UTF8
			// bug #320
			new ByteArrayInputStream(swriter.toString().getBytes());
		if (file.exists())
			file.setContents(stream, false, true, monitor);
		else
			file.create(stream, false, monitor);
		stream.close();
	} catch (IOException e) {
	}
	return file;
}
public abstract void generateContents(String packageName, String className, PrintWriter writer);
public IProject getProject() {
	return project;
}
public IFolder getSourceFolder() {
	return sourceFolder;
}
}
