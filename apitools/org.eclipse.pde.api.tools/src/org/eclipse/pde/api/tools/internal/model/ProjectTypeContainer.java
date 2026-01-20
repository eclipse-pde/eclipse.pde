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
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
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
	private String[] fPackageNames;

	/**
	 * Package fragment roots for JDT-based package discovery. Multiple roots
	 * may exist when several source folders share the same output location.
	 */
	private final List<IPackageFragmentRoot> fPackageFragmentRoots = new CopyOnWriteArrayList<>();

	/**
	 * Constructs an {@link IApiTypeContainer} rooted at the location with an
	 * optional package fragment root for package discovery.
	 *
	 * @param parent the {@link IApiElement} parent for this container
	 * @param container folder in the workspace
	 * @param packageFragmentRoot optional package fragment root for JDT-based
	 *            package discovery, may be <code>null</code>
	 * @since 1.3.300
	 */
	public ProjectTypeContainer(IApiElement parent, IContainer container, IPackageFragmentRoot packageFragmentRoot) {
		super(parent, IApiElement.API_TYPE_CONTAINER, container.getName());
		this.fRoot = container;
		if (packageFragmentRoot != null) {
			this.fPackageFragmentRoots.add(packageFragmentRoot);
		}
	}

	/**
	 * Adds an additional package fragment root to this container. This is used
	 * when multiple source folders share the same output location.
	 *
	 * @param root the package fragment root to add
	 * @since 1.3.400
	 */
	public void addPackageFragmentRoot(IPackageFragmentRoot root) {
		if (root != null && !fPackageFragmentRoots.contains(root)) {
			fPackageFragmentRoots.add(root);
			// Clear cached package names so they will be recomputed
			fPackageNames = null;
		}
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
			for (IPackageFragmentRoot root : fPackageFragmentRoots) {
				if (root.exists()) {
					collectPackageNames(names, root);
				}
			}
			fPackageNames = names.toArray(String[]::new);
		}
		return fPackageNames;
	}

	/**
	 * Collects package names using JDT's package fragment root API.
	 *
	 * @param collector set to collect package names
	 * @param root package fragment root to traverse
	 * @throws CoreException if unable to traverse the package fragment root
	 */
	private static void collectPackageNames(Set<String> collector, IPackageFragmentRoot root) throws CoreException {
		IJavaElement[] children = root.getChildren();
		if (children != null) {
			for (IJavaElement element : children) {
				if (element instanceof IPackageFragment fragment) {
					String name = fragment.getElementName();
					if (name.length() == 0) {
						name = Util.DEFAULT_PACKAGE_NAME;
					}
					collector.add(name);
				}
			}
		}
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
