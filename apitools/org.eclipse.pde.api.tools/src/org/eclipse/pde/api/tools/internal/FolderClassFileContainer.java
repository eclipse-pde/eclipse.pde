/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.api.tools.internal.provisional.ClassFileContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.eclipse.pde.api.tools.internal.provisional.IClassFileContainer;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * A class file container rooted at a container in the workspace.
 * 
 * @since 1.0.0
 */
public class FolderClassFileContainer implements IClassFileContainer {
	
	/**
	 * Root directory of the class file container
	 */
	private IContainer fRoot;
	
	/**
	 * Origin of this class file container
	 */
	private String fOrigin;
	
	/**
	 * Constructs a class file container rooted at the location.
	 * 
	 * @param container folder in the workspace
	 * @param origin id of the component that creates this class file container
	 */
	public FolderClassFileContainer(IContainer container, String origin) {
		this.fRoot = container;
		this.fOrigin = origin;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IClassFileContainer#accept(org.eclipse.pde.api.tools.model.component.ClassFileContainerVisitor)
	 */
	public void accept(ClassFileContainerVisitor visitor) throws CoreException {
		doVisit(fRoot, Util.DEFAULT_PACKAGE_NAME, visitor);
	}
	
	private void doVisit(IContainer container, String pkgName, ClassFileContainerVisitor visitor) throws CoreException {
		IResource[] members = container.members();
		List dirs = new ArrayList();
		boolean visitPkg = visitor.visitPackage(pkgName);
		for (int i = 0; i < members.length; i++) {
			IResource file = members[i];
			switch (file.getType()) {
			case IResource.FOLDER:
				dirs.add(file);
				break;
			case IResource.FILE:
				if (visitPkg && file.getName().endsWith(Util.DOT_CLASS_SUFFIX)) {
					String name = file.getName();
					String typeName = name.substring(0, name.length() - 6);
					if (pkgName.length() > 0) {
						StringBuffer buf = new StringBuffer(pkgName);
						buf.append('.');
						buf.append(typeName);
						typeName = buf.toString();
					}
					ResourceClassFile cf = new ResourceClassFile((IFile) file, typeName);
					visitor.visit(pkgName, cf);
					visitor.end(pkgName, cf);
				}
				break;
			}
		}
		visitor.endVisitPackage(pkgName);
		Iterator iterator = dirs.iterator();
		while (iterator.hasNext()) {
			IContainer child = (IContainer)iterator.next();
			String nextName = null;
			if (pkgName.length() == 0) {
				nextName = child.getName();
			} else {
				StringBuffer buffer = new StringBuffer(pkgName);
				buffer.append('.');
				buffer.append(child.getName());
				nextName = buffer.toString();
			}
			doVisit(child, nextName, visitor);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IClassFileContainer#close()
	 */
	public synchronized void close() throws CoreException {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IClassFileContainer#findClassFile(java.lang.String)
	 */
	public IClassFile findClassFile(String qualifiedName) throws CoreException {
		int index = qualifiedName.lastIndexOf('.');
		String cfName = qualifiedName;
		String pkg = Util.DEFAULT_PACKAGE_NAME;
		if (index > 0) {
			pkg = qualifiedName.substring(0, index);
			cfName = qualifiedName.substring(index + 1);
		}
		IFolder folder = fRoot.getFolder(new Path(pkg.replace('.', IPath.SEPARATOR)));
		if (folder.exists()) {
			IFile file = folder.getFile(cfName + Util.DOT_CLASS_SUFFIX);
			if (file.exists()) {
				return new ResourceClassFile(file, qualifiedName);
			}
		}
		return null;
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IClassFileContainer#getPackageNames()
	 */
	public String[] getPackageNames() throws CoreException {
		List names = new ArrayList();
		collectPackageNames(names, Util.DEFAULT_PACKAGE_NAME, fRoot);
		return (String[]) names.toArray(new String[names.size()]);
	}
	
	/**
	 * Traverses a directory to determine if it has class files and
	 * then visits sub-directories.
	 * 
	 * @param packageName package name of directory being visited
	 * @param dir directory being visited
	 */
	private void collectPackageNames(List names, String packageName, IContainer dir) throws CoreException {
		IResource[] members = dir.members();
		boolean hasClassFiles = false;
		List dirs = new ArrayList();
		for (int i = 0; i < members.length; i++) {
			IResource file = members[i];
			switch (file.getType()) {
			case IResource.FOLDER:
				dirs.add(file);
				break;
			case IResource.FILE:
				if (!hasClassFiles && file.getName().endsWith(Util.DOT_CLASS_SUFFIX)) {
					names.add(packageName);
					hasClassFiles = true;
				}
				break;
			}
		}
		Iterator iterator = dirs.iterator();
		while (iterator.hasNext()) {
			IContainer child = (IContainer)iterator.next();
			String nextName = null;
			if (packageName.length() == 0) {
				nextName = child.getName();
			} else {
				StringBuffer buffer = new StringBuffer(packageName);
				buffer.append('.');
				buffer.append(child.getName());
				nextName = buffer.toString();
			}
			collectPackageNames(names, nextName, child);
		}
	}

	public IClassFile findClassFile(String qualifiedName, String id) throws CoreException {
		return findClassFile(qualifiedName);
	}

	public String getOrigin() {
		return this.fOrigin;
	}
}
