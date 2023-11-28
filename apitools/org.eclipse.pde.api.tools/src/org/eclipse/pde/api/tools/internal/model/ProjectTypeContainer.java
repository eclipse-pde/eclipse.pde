/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.api.tools.internal.provisional.model.ApiTypeContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * An {@link IApiTypeRoot} rooted at a project output container in the
 * workspace.
 *
 * @since 1.0.0
 */
public class ProjectTypeContainer extends ApiElement implements IApiTypeContainer {

	/**
	 * Root directory of the {@link IApiTypeContainer}
	 */
	private final IContainer fRoot;
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

	@Override
	public void accept(ApiTypeContainerVisitor visitor) throws CoreException {
		if (visitor.visit(this)) {
			doVisit(fRoot, Util.DEFAULT_PACKAGE_NAME, visitor);
		}
		visitor.end(this);
	}

	@Override
	public void close() throws CoreException {
		fPackageNames = null;
	}

	@Override
	public String toString() {
		StringBuilder buff = new StringBuilder();
		buff.append("Project Class File Container: " + getName()); //$NON-NLS-1$
		return buff.toString();
	}

	/**
	 * Visits the given {@link IContainer}
	 */
	private void doVisit(IContainer container, String pkgName, ApiTypeContainerVisitor visitor) throws CoreException {
		IResource[] members = container.members();
		List<IContainer> dirs = new ArrayList<>();
		boolean visitPkg = visitor.visitPackage(pkgName);
		for (IResource file : members) {
			switch (file.getType()) {
				case IResource.FOLDER:
					dirs.add((IContainer) file);
					break;
				case IResource.FILE:
					if (visitPkg && file.getName().endsWith(Util.DOT_CLASS_SUFFIX)) {
						String name = file.getName();
						String typeName = name.substring(0, name.length() - 6);
						if (pkgName.length() > 0) {
							StringBuilder buf = new StringBuilder(pkgName);
							buf.append('.');
							buf.append(typeName);
							typeName = buf.toString();
						}
						ResourceApiTypeRoot cf = new ResourceApiTypeRoot(this, (IFile) file, typeName);
						visitor.visit(pkgName, cf);
						visitor.end(pkgName, cf);
					}
					break;
				default:
					break;
			}
		}
		visitor.endVisitPackage(pkgName);
		for (IContainer child : dirs) {
			String nextName = null;
			if (pkgName.length() == 0) {
				nextName = child.getName();
			} else {
				StringBuilder buffer = new StringBuilder(pkgName);
				buffer.append('.');
				buffer.append(child.getName());
				nextName = buffer.toString();
			}
			doVisit(child, nextName, visitor);
		}
	}

	@Override
	public IApiTypeRoot findTypeRoot(String qualifiedName) throws CoreException {
		int index = qualifiedName.lastIndexOf('.');
		String cfName = qualifiedName;
		String pkg = Util.DEFAULT_PACKAGE_NAME;
		if (index > 0) {
			pkg = qualifiedName.substring(0, index);
			cfName = qualifiedName.substring(index + 1);
		}
		IFolder folder = fRoot.getFolder(IPath.fromOSString(pkg.replace('.', IPath.SEPARATOR)));
		if (folder.exists()) {
			IFile file = folder.getFile(cfName + Util.DOT_CLASS_SUFFIX);
			if (file.exists()) {
				return new ResourceApiTypeRoot(this, file, qualifiedName);
			}
		}
		return null;
	}

	@Override
	public String[] getPackageNames() throws CoreException {
		if (fPackageNames == null) {
			SortedSet<String> names = new TreeSet<>();
			collectPackageNames(names, fRoot);
			fPackageNames = names.toArray(String[]::new);
		}
		return fPackageNames;
	}

	/**
	 * Traverses a directory to determine if it has {@link IApiTypeRoot}s and then
	 * visits sub-directories.
	 *
	 * @param dir directory being visited
	 */
	private static void collectPackageNames(Set<String> collector, IContainer dir) throws CoreException {
		int segmentCount = dir.getFullPath().segmentCount();
		dir.accept(proxy -> {
			if (proxy.getType() == IResource.FOLDER) {
				IPath relativePath = proxy.requestFullPath().removeFirstSegments(segmentCount);
				String packageName = relativePath.toString().replace(IPath.SEPARATOR, '.');
				return collector.add(packageName);
			}
			return false;
		}, IResource.NONE);
	}

	@Override
	public IApiTypeRoot findTypeRoot(String qualifiedName, String id) throws CoreException {
		return findTypeRoot(qualifiedName);
	}

	@Override
	public int getContainerType() {
		return FOLDER;
	}
}
