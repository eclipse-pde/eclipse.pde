/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.builders;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

public class JavaModelInterface {
	protected static JavaModelInterface fgJavaModelInterface;
	
	public static final int SEARCHPATH_SOURCE= 0x01;
	public static final int SEARCHPATH_REFERENCED= 0x02;
	public static final int SEARCHPATH_EXTERNAL= 0x04;
	public static final int SEARCHPATH_FULL= 
		SEARCHPATH_SOURCE | SEARCHPATH_REFERENCED | SEARCHPATH_EXTERNAL;
	
	protected static final IFolder[] EMPTY_FOLDER_ARRAY= new IFolder[0];
	protected static final IFile[] EMPTY_FILE_ARRAY= new IFile[0];
	protected static final IType[] EMPTY_TYPE_ARRAY= new IType[0];
	
	
	
	public static JavaModelInterface getJavaModelInterface() {
		if (fgJavaModelInterface == null) {
			fgJavaModelInterface= new JavaModelInterface();
		}
		return fgJavaModelInterface;
	}
	
	private JavaModelInterface() {
	}
	
	public IClasspathEntry[] getClasspathEntries(IProject project, int searchPath) {
		IJavaProject jp= JavaCore.create(project);
		if (jp != null) {
			try {
				return filterClasspathEntries(jp.getResolvedClasspath(true), jp, searchPath);
			} catch (JavaModelException ex) {
			}
		}
		
		return new IClasspathEntry[0];
	}
	
	IClasspathEntry[] filterClasspathEntries(IClasspathEntry[] cpe, IJavaProject jp, int searchPath) {
		ArrayList list= new ArrayList();
		if (searchPath == SEARCHPATH_FULL)
			return cpe;
		
		for (int i=0; i<cpe.length; i++) {
			switch (cpe[i].getEntryKind()) {
				case IClasspathEntry.CPE_SOURCE:
					if ((searchPath & SEARCHPATH_SOURCE) == SEARCHPATH_SOURCE)
						list.add(cpe[i]);
					break;
				case IClasspathEntry.CPE_PROJECT:
					if ((searchPath & SEARCHPATH_REFERENCED) == SEARCHPATH_REFERENCED)
						list.add(cpe[i]);
					break;
				case IClasspathEntry.CPE_LIBRARY:
				case IClasspathEntry.CPE_VARIABLE:
					if ((searchPath & SEARCHPATH_EXTERNAL) == SEARCHPATH_EXTERNAL)
						list.add(cpe[i]);
					break;
				case IClasspathEntry.CPE_CONTAINER:
					try {
						IClasspathContainer container= JavaCore.getClasspathContainer(cpe[i].getPath(), jp);
						if (container == null) break;
						
						IClasspathEntry[] containerEntries= container.getClasspathEntries();
						if (containerEntries == null) break;
						
						// container was bound
						for (int j=0; j<containerEntries.length; j++) {
							IClasspathEntry containerEntry= containerEntries[j];
							switch (containerEntry.getEntryKind()) {
								case IClasspathEntry.CPE_PROJECT:
									if ((searchPath & SEARCHPATH_REFERENCED) == SEARCHPATH_REFERENCED)
										list.add(containerEntry);
									break;
								case IClasspathEntry.CPE_LIBRARY:
									if ((searchPath & SEARCHPATH_EXTERNAL) == SEARCHPATH_EXTERNAL)
										list.add(containerEntry);
									break;
							}
						}
					} catch (JavaModelException ex) {
					}
					break;
			}
		}
		
		IClasspathEntry[] paths= new IClasspathEntry[list.size()];
		list.toArray(paths);
		return paths;
	}
	
	/*
	 * Return an array of the interfaces implemented by
	 * the given class in the context of the given project.
	 * The result can be further scoped to include only those
	 * interfaces which are contained in source folders.
	 */
	public IType[] getInterfacesImplementedBy(IType type, IProject project, int searchPath) {
		try {
			ITypeHierarchy th= type.newSupertypeHierarchy(null);
			IType[] types= th.getAllSuperInterfaces(type);
			if ((searchPath != SEARCHPATH_FULL) && (types.length > 0)) {
				List results= new ArrayList(types.length);
				for (int i=0; i<types.length; i++)
					results.add(types[i]);
				results= filterToSearchPath(results, project, searchPath);
				if (results.size() != types.length) {
					types= new IType[results.size()];
					results.toArray(types);
				}
			}
			return types;
		} catch (JavaModelException ex) {
			//ex.printStackTrace();
		}
		
		return EMPTY_TYPE_ARRAY;
	}
	
	public IType findTypeOnClasspath(String qualifiedName, IProject project, int searchPath) {
		// WARNING: This will find the first type with the specified name.
		IType type=null;
		try {
			type = JavaCore.create(project).findType(qualifiedName);			
		} catch (JavaModelException e) {
		}
		return type;
	}	
	
	/*
	 * Return an array of the classes which implement the
	 * given interface in the context of the given project.
	 * The result can be further scoped to include only those
	 * classes which are contained in source folders.
	 */
	public IType[] getImplementorsOf(IType type, IProject project, int searchPath) {
		try {
			IJavaProject jp= JavaCore.create(project);
			ITypeHierarchy th= type.newTypeHierarchy(jp, null);
			IType[] types= th.getAllSubtypes(type);
			if (types.length > 0) {
				if (searchPath == SEARCHPATH_FULL) {
					IType[] sortedTypes = new IType[types.length];
					int count = 0;
					for (int i = 0; i < types.length; i++) {
						if (types[i].getJavaProject().equals(jp)) {
							sortedTypes[count] = types[i];
							count++;
							types[i] = null;
						}
					}
					for (int i = 0; i < types.length; i++) {
						if (types[i] != null) {
							sortedTypes[count] = types[i];
							count++;
						}
					}					
					return sortedTypes;
				}	
				List results= new ArrayList(types.length);
				for (int i=0; i<types.length; i++) {
					if (types[i].isClass())
						results.add(types[i]);
				}
				results= filterToSearchPath(results, project, searchPath);
				if (results.size() != types.length) {
					types= new IType[results.size()];
					results.toArray(types);
				}
			}
			return types;
		} catch (JavaModelException ex) {
			//ex.printStackTrace();
		}
		
		return EMPTY_TYPE_ARRAY;
	}
	
	
	List filterToSearchPath(List elements, IProject project, int searchPath) {
		if (project == null)
			return elements;
		
		// Due to a bug in JavaSearchScope, this
		// optimization currently cannot be used.
		//if (searchPath == SEARCHPATH_FULL)
		//	return elements;
		
		List results= new ArrayList();
		IClasspathEntry[] classpath= getClasspathEntries(project, searchPath);
		for (int i=0; i<classpath.length; i++) {
			IPath path= classpath[i].getPath();
			Iterator it= elements.iterator();
			while (it.hasNext()) {
				try {
					IJavaElement je= (IJavaElement) it.next();
					IResource res= je.getUnderlyingResource();
					if (res != null) {
						if (path.isPrefixOf(res.getFullPath()))
							results.add(je);
					} else {
						IJavaElement parent= je;
						while (parent != null) {
							if (path.equals(new Path(parent.getElementName())))
								results.add(je);
							parent= parent.getParent();
						}
					}
				} catch (JavaModelException ex) {
				}
			}
		}
		
		return results;
	}
}
