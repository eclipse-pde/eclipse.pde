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
package org.eclipse.pde.api.tools.internal.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.api.tools.internal.provisional.model.ApiTypeContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * An {@link IApiTypeRoot} rooted at a container in the workspace.
 * 
 * @since 1.0.0
 */
public class FolderApiTypeContainer extends ApiElement implements IApiTypeContainer {
	
	/**
	 * Root directory of the {@link IApiTypeContainer}
	 */
	private IContainer fRoot;
	
	/**
	 * Constructs an {@link IApiTypeContainer} rooted at the location.
	 * 
	 * @param parent the {@link IApiElement} parent for this container
	 * @param container folder in the workspace
	 */
	public FolderApiTypeContainer(IApiElement parent, IContainer container) {
		super(parent, IApiElement.API_TYPE_CONTAINER, container.getName());
		this.fRoot = container;
	}
	
	/**
	 * @see org.eclipse.pde.api.tools.internal.AbstractApiTypeContainer#accept(org.eclipse.pde.api.tools.internal.provisional.ApiTypeContainerVisitor)
	 */
	public void accept(ApiTypeContainerVisitor visitor) throws CoreException {
		doVisit(fRoot, Util.DEFAULT_PACKAGE_NAME, visitor);
	}
	
	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiTypeContainer#close()
	 */
	public void close() throws CoreException {}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buff = new StringBuffer();
		buff.append("Folder Class File Container: "+getName()); //$NON-NLS-1$
		return buff.toString();
	}
	
	/**
	 * Visits the given {@link IContainer}
	 * @param container
	 * @param pkgName
	 * @param visitor
	 * @throws CoreException
	 */
	private void doVisit(IContainer container, String pkgName, ApiTypeContainerVisitor visitor) throws CoreException {
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
					ResourceApiTypeRoot cf = new ResourceApiTypeRoot(this, (IFile) file, typeName);
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

	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiTypeContainer#findTypeRoot(java.lang.String)
	 */
	public IApiTypeRoot findTypeRoot(String qualifiedName) throws CoreException {
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
				return new ResourceApiTypeRoot(this, file, qualifiedName);
			}
		}
		return null;
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.AbstractApiTypeContainer#getPackageNames()
	 */
	public String[] getPackageNames() throws CoreException {
		List names = new ArrayList();
		collectPackageNames(names, Util.DEFAULT_PACKAGE_NAME, fRoot);
		String[] result = new String[names.size()];
		names.toArray(result);
		Arrays.sort(result);
		return result;
	}
	
	/**
	 * Traverses a directory to determine if it has {@link IApiTypeRoot}s and
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

	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiTypeContainer#findTypeRoot(java.lang.String, java.lang.String)
	 */
	public IApiTypeRoot findTypeRoot(String qualifiedName, String id) throws CoreException {
		return findTypeRoot(qualifiedName);
	}
}
