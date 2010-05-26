/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
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
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.api.tools.internal.provisional.model.ApiTypeContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * An {@link IApiTypeRoot} rooted at a project output container in the workspace.
 * 
 * @since 1.0.0
 */
public class ProjectTypeContainer extends ApiElement implements IApiTypeContainer {
	
	/**
	 * Proxy visitor for collecting package names, etc for our 
	 * type containers
	 * 
	 * @since 1.1
	 */
	class ContainerVisitor implements IResourceProxyVisitor {
		
		List collector = null;
		int segmentcount = 0;
		
		/**
		 * Constructor
		 * @param collector
		 * @param root
		 */
		public ContainerVisitor(List collector, IContainer root) {
			this.collector = collector;
			this.segmentcount = root.getFullPath().segmentCount();
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.core.resources.IResourceProxyVisitor#visit(org.eclipse.core.resources.IResourceProxy)
		 */
		public boolean visit(IResourceProxy proxy) throws CoreException {
			if(proxy.getType() == IResource.FOLDER) {
				String path = proxy.requestFullPath().removeFirstSegments(this.segmentcount).toString();
				return this.collector.add(path.replace(IPath.SEPARATOR, '.'));
			}
			return false;
		}
	}
	
	/**
	 * Root directory of the {@link IApiTypeContainer}
	 */
	private IContainer fRoot;
	private String[] fPackageNames = null;
	
	/**
	 * Constructs an {@link IApiTypeContainer} rooted at the location.
	 * 
	 * @param parent the {@link IApiElement} parent for this container
	 * @param container folder in the workspace
	 */
	public ProjectTypeContainer(IApiElement parent, IContainer container) {
		super(parent, IApiElement.API_TYPE_CONTAINER, container.getName());
		this.fRoot = container;
	}
	
	/**
	 * @see org.eclipse.pde.api.tools.internal.AbstractApiTypeContainer#accept(org.eclipse.pde.api.tools.internal.provisional.ApiTypeContainerVisitor)
	 */
	public void accept(ApiTypeContainerVisitor visitor) throws CoreException {
		if(visitor.visit(this)) {
			doVisit(fRoot, Util.DEFAULT_PACKAGE_NAME, visitor);
		}
		visitor.end(this);
	}
	
	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiTypeContainer#close()
	 */
	public void close() throws CoreException {
		fPackageNames = null;
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buff = new StringBuffer();
		buff.append("Project Class File Container: "+getName()); //$NON-NLS-1$
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
		if(fPackageNames == null) {
			List names = new ArrayList();
			collectPackageNames(names, Util.DEFAULT_PACKAGE_NAME, fRoot);
			fPackageNames = (String[]) names.toArray(new String[names.size()]);
			Arrays.sort(fPackageNames);
		}
		return fPackageNames;
	}
	
	/**
	 * Traverses a directory to determine if it has {@link IApiTypeRoot}s and
	 * then visits sub-directories.
	 * 
	 * @param packageName package name of directory being visited
	 * @param dir directory being visited
	 */
	private void collectPackageNames(List names, String packageName, IContainer dir) throws CoreException {
		dir.accept(new ContainerVisitor(names, dir), IResource.NONE);
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiTypeContainer#findTypeRoot(java.lang.String, java.lang.String)
	 */
	public IApiTypeRoot findTypeRoot(String qualifiedName, String id) throws CoreException {
		return findTypeRoot(qualifiedName);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer#getContainerType()
	 */
	public int getContainerType() {
		return FOLDER;
	}
}
