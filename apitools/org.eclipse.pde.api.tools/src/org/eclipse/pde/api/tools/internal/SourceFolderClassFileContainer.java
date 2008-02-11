/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.api.tools.internal.provisional.ClassFileContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.eclipse.pde.api.tools.internal.provisional.IClassFileContainer;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * A class file container corresponding to a source folder
 * (package fragment root) in a project.
 * 
 * @since 1.0
 */
public class SourceFolderClassFileContainer implements IClassFileContainer {
	
	/**
	 * Associated package fragment root
	 */
	private IPackageFragmentRoot fRoot;
	
	/**
	 * Output location, or <code>null</code> if does not exist
	 */
	private IContainer fOutputLocation;
	
	private String fOrigin;

	/**
	 * Constructs a new class file container on the given package
	 * fragment root.
	 * 
	 * @param root package fragment root
	 * @param origin id of the component that creates this class file container
	 */
	public SourceFolderClassFileContainer(IPackageFragmentRoot root, String origin) {
		fRoot = root;
		fOrigin = origin;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IClassFileContainer#accept(org.eclipse.pde.api.tools.internal.provisional.ClassFileContainerVisitor)
	 */
	public void accept(ClassFileContainerVisitor visitor) throws CoreException {
		IContainer outputLocation = getOutputLocation();
		if (outputLocation != null) {
			IJavaElement[] children = fRoot.getChildren();
			for (int i = 0; i < children.length; i++) {
				IPackageFragment fragment = (IPackageFragment) children[i];
				String pkg = fragment.getElementName();
				IResource folder = null;
				if (pkg.length() > 0) {
					folder = outputLocation.findMember(fragment.getElementName().replace('.', IPath.SEPARATOR));
				} else {
					folder = outputLocation;
				}
				if (folder instanceof IContainer) {
					IContainer container = (IContainer) folder;
					IResource[] members = container.members();
					if (members.length > 0) {
						if (visitor.visitPackage(pkg)) {
							for (int j = 0; j < members.length; j++) {
								IResource res = members[j];
								if (res.getType() == IResource.FILE) {
									IFile file = (IFile) res;
									if (Util.isClassFile(file.getName())) {
										StringBuffer typeName = new StringBuffer();
										if(pkg.length() != 0) {
											typeName.append(pkg);
											typeName.append('.');
										}
										String fileName = file.getName();
										String className = fileName.substring(0, fileName.length() - Util.DOT_CLASS_SUFFIX.length());
										typeName.append(className);
										IClassFile cf = new ResourceClassFile(file, typeName.toString());
										visitor.visit(pkg, cf);
										visitor.end(pkg, cf);
									}
								}
							}
						}
						visitor.endVisitPackage(pkg);
					}
				}
			}			
		}		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IClassFileContainer#close()
	 */
	public synchronized void close() throws CoreException {
		fOutputLocation = null;
	}
	
	/**
	 * Returns the output location for this source folder as a
	 * container, or <code>null</code> if none.
	 * 
	 * @return output location or <code>null</code>
	 */
	private synchronized IContainer getOutputLocation() {
		if (this.fOutputLocation == null) {
			try {
				switch(fRoot.getKind()) {
					case IPackageFragmentRoot.K_SOURCE :
						IPath location = fRoot.getRawClasspathEntry().getOutputLocation();
						if (location == null) {
							location = fRoot.getJavaProject().getOutputLocation();
						}
						this.fOutputLocation = ResourcesPlugin.getWorkspace().getRoot().getFolder(location);
						if (!fOutputLocation.exists()) {
							fOutputLocation = null;
						}
						break;
					case IPackageFragmentRoot.K_BINARY :
						IResource resource = fRoot.getUnderlyingResource();
						if (resource != null) {
							switch(resource.getType()) {
								case IResource.PROJECT :
									this.fOutputLocation = (IProject) resource;
									break;
								case IResource.FOLDER :
									this.fOutputLocation = (IFolder) resource;
									break;
							}
						}
				}
			} catch (JavaModelException e) {
			}
		}
		return this.fOutputLocation;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IClassFileContainer#findClassFile(java.lang.String)
	 */
	public IClassFile findClassFile(String qualifiedName) throws CoreException {
		IContainer outputLocation = getOutputLocation();
		if (outputLocation != null) {
			int index = qualifiedName.lastIndexOf('.');
			String pkg = ""; //$NON-NLS-1$
			if (index > 0) {
				pkg = qualifiedName.substring(0, index);
			}
			IPackageFragment fragment = fRoot.getPackageFragment(pkg);
			if (fragment.exists()) {
				StringBuffer name = new StringBuffer();
				name.append(qualifiedName.replace('.', IPath.SEPARATOR));
				name.append(Util.DOT_CLASS_SUFFIX);
				IResource member = outputLocation.findMember(name.toString());
				if (member != null && member.getType() == IResource.FILE) {
					return new ResourceClassFile((IFile) member, qualifiedName);
				}
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IClassFileContainer#getPackageNames()
	 */
	public String[] getPackageNames() throws CoreException {
		IJavaElement[] elements = fRoot.getChildren();
		List names = new ArrayList();
		for (int i = 0; i < elements.length; i++) {
			IPackageFragment fragment = (IPackageFragment)elements[i];
			if (fragment.hasChildren()) {
				names.add(fragment.getElementName());
			}
		}
		return (String[]) names.toArray(new String[names.size()]);
	}

	public String getOrigin() {
		return this.fOrigin;
	}
	
	public IClassFile findClassFile(String qualifiedName, String id) throws CoreException {
		return findClassFile(qualifiedName);
	}
}
