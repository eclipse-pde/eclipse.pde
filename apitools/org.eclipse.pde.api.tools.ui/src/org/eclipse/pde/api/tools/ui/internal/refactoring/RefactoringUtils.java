/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.refactoring;

import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;

/**
 * Helper class to create API tools refactoring changes
 * 
 * @since 1.0.1
 */
public class RefactoringUtils {

	/**
	 * Creates the {@link Change} for updating the filter store
	 * when a type is deleted
	 * 
	 * @param type the type being deleted
	 * @return the change to the filter store
	 */
	static Change createDeleteFilterChanges(IType type) {
		try {
			IResource resource = type.getUnderlyingResource();
			if(resource != null) {
				IApiFilterStore store = resolveFilterStore(resource.getProject());
				if(store == null) {
					return null;
				}
				IApiProblemFilter[] filters = store.getFilters(resource);
				if(filters.length != 0) {
					CompositeChange cchange = new CompositeChange(RefactoringMessages.RefactoringUtils_remove_usused_filters);
					for (int i = 0; i < filters.length; i++) {
						cchange.add(new TypeFilterChange(store, filters[i], null, null, FilterChange.DELETE));
					}
					return cchange;
				}
			}
		}
		catch(CoreException ce) {}
		return null;
	}
	
	/**
	 * Creates the {@link Change} for updating a filter store when a package fragment is deleted
	 * @param fragment the fragment that has been deleted
	 * @return the {@link Change} for the package fragment deletion
	 */
	static Change createDeleteFilterChanges(IPackageFragment fragment) {
		try {
			IResource resource = fragment.getUnderlyingResource();
			if(resource != null) {
				IApiFilterStore store = resolveFilterStore(resource.getProject());
				if(store == null) {
					return null;
				}
				IApiProblemFilter[] filters = collectAllAffectedFilters(store, collectAffectedTypes(fragment));
				if(filters.length != 0) {
					CompositeChange cchange = new CompositeChange(RefactoringMessages.RefactoringUtils_remove_usused_filters);
					for (int i = 0; i < filters.length; i++) {
						cchange.add(new TypeFilterChange(store, filters[i], null, null, FilterChange.DELETE));
					}
					return cchange;
				}
			}
		}
		catch(CoreException ce) {}
		return null;
	}
	
	/**
	 * Collects the complete set of {@link IApiProblemFilter}s for the given collection of {@link IType}s
	 * @param types
	 * @return the complete collection of {@link IApiProblemFilter}s or an empty list, never <code>null</code>
	 */
	static IApiProblemFilter[] collectAllAffectedFilters(IApiFilterStore store, IType[] types) {
		HashSet filters = new HashSet();
		IApiProblemFilter[] fs = null;
		IResource resource = null;
		for (int i = 0; i < types.length; i++) {
			try {
				resource = types[i].getUnderlyingResource();
				if(resource == null) {
					continue;
				}
				fs = store.getFilters(resource);
				for (int j = 0; j < fs.length; j++) {
					filters.add(fs[j]);
				}
			}
			catch(JavaModelException jme) {
				//do nothing 
			}
		}
		return (IApiProblemFilter[]) filters.toArray(new IApiProblemFilter[filters.size()]);
	}
	
	/**
	 * Collects the complete list of {@link IType}s affected from within the {@link IPackageFragment}
	 * @param fragment
	 * @return the complete collection of affected {@link IType}s or an empty list, never <code>null</code>
	 */
	static IType[] collectAffectedTypes(IPackageFragment fragment) {
		HashSet types = new HashSet();
		try {
			if(fragment.containsJavaResources()) {
				ICompilationUnit[] cunits = fragment.getCompilationUnits();
				IType type = null;
				for (int i = 0; i < cunits.length; i++) {
					type = cunits[i].findPrimaryType();
					if(type == null) {
						continue;
					}
					types.add(type);
				}
			}
		}
		catch(JavaModelException jme) {
			//do nothing
		}
		return (IType[]) types.toArray(new IType[types.size()]);
	}
	
	/**
	 * Creates the {@link Change} for updating {@link IApiProblemFilter}s
	 * that are affected by the rename of a type
	 * 
	 * @param type the type being renamed
	 * @param newname the new name to be set on the resource
	 * @return the rename type change for updating filters
	 */
	static Change createRenameFilterChanges(IType type, String newname) {
		return createDeleteFilterChanges(type);
	}
	
	/**
	 * Creates the {@link Change} for {@link IApiProblemFilter}s affected by 
	 * a package fragment rename
	 * @param fragment
	 * @param newname
	 * @return the change for the package fragment rename
	 */
	static Change createRenameFilterChanges(IPackageFragment fragment, String newname) {
		return createDeleteFilterChanges(fragment);
	}
	
	/**
	 * Gets the {@link IApiFilterStore} for the given {@link IProject}
	 * @param project
	 * @return the filter store for the given project or <code>null</code>
	 * @throws CoreException
	 */
	static IApiFilterStore resolveFilterStore(IProject project) throws CoreException {
		IApiComponent component = ApiPlugin.getDefault().getApiBaselineManager().getWorkspaceBaseline().getApiComponent(project);
		if(component != null) {
			return component.getFilterStore();
		}
		return null;
	}
	
	/**
	 * Returns the new fully qualified name for the filter
	 * @param type
	 * @param newname
	 * @return
	 */
	static String getNewQualifiedName(IType type, String newname) {
		IType dtype = type.getDeclaringType();
		String newqname = newname;
		if (dtype == null) {
			IPackageFragment packageFragment = type.getPackageFragment();
			if (!packageFragment.isDefaultPackage()) {
				newqname = packageFragment.getElementName() + '.' + newname;
			}
		} 
		else {
			newqname = dtype.getFullyQualifiedName() + '$' + newname;
		}
		return newqname;
	}
	
	/**
	 * Returns the new type name with the new package qualification
	 * @param newname
	 * @param oldtypename
	 * @return the new fully qualified type name 
	 */
	static String getNewQualifiedName(String newname, String oldtypename) {
		//TODO switch out the package resolution
		return oldtypename;
	}
	
	/**
	 * Returns a new path string given the type name.
	 * This method basically removes the last segment of the path, appends the new name
	 * and resets the extension
	 * @param oldpath
	 * @param typename
	 * @return the new resource path to use
	 */
	static String getNewResourcePath(IPath oldpath, String typename) {
		if(typename.indexOf('$') < 0) {
			String ext = oldpath.getFileExtension();
			IPath newpath = oldpath.removeLastSegments(1).append(typename);
			if(ext != null) {
				return newpath.addFileExtension(ext).toString();
			}
			return newpath.toString();
		}
		return oldpath.toString();
	}
}
