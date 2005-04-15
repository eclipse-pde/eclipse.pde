/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;

public class PDEClasspathContainer {
	
	protected ArrayList fEntries;
	
	private static HashMap ACCESS_RULES = new HashMap();
	private static final IAccessRule EXCLUDE_ALL_RULE = 
		JavaCore.newAccessRule(new Path("**/*"), IAccessRule.K_NON_ACCESSIBLE);

	protected void addProjectEntry(IProject project, boolean isExported, IPath[] inclusions) throws CoreException {
		if (project.hasNature(JavaCore.NATURE_ID)) {
			IClasspathEntry entry = null;
			if (inclusions != null) {
				IAccessRule[] accessRules = getAccessRules(inclusions);
				entry = JavaCore.newProjectEntry(
							project.getFullPath(), 
							accessRules, 
							false, 
							new IClasspathAttribute[0], 
							isExported);
			} else {
				entry = JavaCore.newProjectEntry(project.getFullPath(), isExported);
			}
			if (!fEntries.contains(entry))
				fEntries.add(entry);
		}
	}
	
	protected void addExternalPlugin(IPluginModelBase model, boolean isExported, IPath[] inclusions) throws CoreException {
		if (new File(model.getInstallLocation()).isFile()) {
			IPath srcPath = ClasspathUtilCore.getSourceAnnotation(model, "."); //$NON-NLS-1$
			if (srcPath == null)
				srcPath = new Path(model.getInstallLocation());			
			addLibraryEntry(new Path(model.getInstallLocation()), srcPath, isExported, inclusions);			
		} else {
			IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
			for (int i = 0; i < libraries.length; i++) {
				if (IPluginLibrary.RESOURCE.equals(libraries[i].getType()))
					continue;
				model = (IPluginModelBase)libraries[i].getModel();
				String name = libraries[i].getName();
				String expandedName = ClasspathUtilCore.expandLibraryName(name);
				IPath path = getPath(model, expandedName);
				if (path == null && !model.isFragmentModel() && ClasspathUtilCore.containsVariables(name)) {
					model = resolveLibraryInFragments(model, expandedName);
					if (model != null)
						path = getPath(model, expandedName);
				}
				if (path != null)
					addLibraryEntry(path, ClasspathUtilCore.getSourceAnnotation(model, expandedName), isExported, inclusions);
			}		
		}
	}
	
	protected void addLibraryEntry(IPath path, IPath srcPath, boolean isExported, IPath[] inclusions) {
		IClasspathEntry entry = null;
		if (inclusions != null) {
			entry = JavaCore.newLibraryEntry(
						path, 
						srcPath, 
						null,
						getAccessRules(inclusions),
						new IClasspathAttribute[0],
						isExported);
		} else {
			entry = JavaCore.newLibraryEntry(path, srcPath, null, isExported);
		}
		if (!fEntries.contains(entry)) {
			fEntries.add(entry);
		}
	}
	
	protected IAccessRule[] getAccessRules(IPath[] inclusionPatterns) {
		IAccessRule[] accessRules = new IAccessRule[inclusionPatterns.length + 1];
		for (int i = 0; i < inclusionPatterns.length; i++) {
			IAccessRule rule = (IAccessRule)ACCESS_RULES.get(inclusionPatterns[i]);
			if (rule == null) {
				rule = JavaCore.newAccessRule(inclusionPatterns[i], IAccessRule.K_ACCESSIBLE);
				ACCESS_RULES.put(inclusionPatterns[i], rule);
			}
			accessRules[i] = rule;
		}
		accessRules[inclusionPatterns.length] = EXCLUDE_ALL_RULE;
		return accessRules;
	}
	
	protected IPath getPath(IPluginModelBase model, String libraryName) {
		IResource resource = model.getUnderlyingResource();
		if (resource != null) {
			IResource jarFile = resource.getProject().findMember(libraryName);
			return (jarFile != null) ? jarFile.getFullPath() : null;
		} 

		File file = new File(model.getInstallLocation(), libraryName);
		return file.exists() ? new Path(file.getAbsolutePath()) : null;
	}
	
	protected IPluginModelBase resolveLibraryInFragments(IPluginModelBase model, String libraryName) {
		BundleDescription desc = model.getBundleDescription();
		if (desc != null) {
			BundleDescription[] fragments = desc.getFragments();
			for (int i = 0; i < fragments.length; i++) {
				if (new File(fragments[i].getLocation(), libraryName).exists())
					return PDECore.getDefault().getModelManager().findModel(fragments[i]);
			}
		}
		return null;
	}
	
}
