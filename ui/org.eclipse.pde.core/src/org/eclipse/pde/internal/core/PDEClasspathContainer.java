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
	
	class Rule {
		IPath path;
		boolean discouraged;
		public boolean equals(Object other) {
			if (!(other instanceof Rule))
				return false;
			return discouraged == ((Rule)other).discouraged && path.equals(((Rule)other).path);
		}
	}
	
	private static HashMap ACCESSIBLE_RULES = new HashMap();
	private static HashMap DISCOURAGED_RULES = new HashMap();
	
	private static final IAccessRule EXCLUDE_ALL_RULE = 
		JavaCore.newAccessRule(new Path("**/*"), IAccessRule.K_NON_ACCESSIBLE); //$NON-NLS-1$

	protected void addProjectEntry(IProject project, Rule[] rules, ArrayList entries) throws CoreException {
		if (project.hasNature(JavaCore.NATURE_ID)) {
			IClasspathEntry entry = null;
			if (rules != null) {
				IAccessRule[] accessRules = getAccessRules(rules);
				entry = JavaCore.newProjectEntry(
							project.getFullPath(), 
							accessRules, 
							true, 
							new IClasspathAttribute[0],
							false);
			} else {
				entry = JavaCore.newProjectEntry(project.getFullPath());
			}
			if (!entries.contains(entry))
				entries.add(entry);
		}
	}
	
	protected void addExternalPlugin(IPluginModelBase model, Rule[] rules, ArrayList entries) throws CoreException {
		if (new File(model.getInstallLocation()).isFile()) {
			IPath srcPath = ClasspathUtilCore.getSourceAnnotation(model, "."); //$NON-NLS-1$
			if (srcPath == null)
				srcPath = new Path(model.getInstallLocation());			
			addLibraryEntry(new Path(model.getInstallLocation()), srcPath, rules, getClasspathAttributes(model), entries);			
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
				if (path != null && !path.toFile().isDirectory())
					addLibraryEntry(path, ClasspathUtilCore.getSourceAnnotation(model, expandedName), rules, getClasspathAttributes(model), entries);
			}		
		}
	}
	
	protected void addLibraryEntry(IPath path, IPath srcPath, Rule[] rules, IClasspathAttribute[] attributes, ArrayList entries) {
		IClasspathEntry entry = null;
		if (rules != null) {
			entry = JavaCore.newLibraryEntry(
						path, 
						srcPath, 
						null,
						getAccessRules(rules),
						attributes,
						false);
		} else {
			entry = JavaCore.newLibraryEntry(path, srcPath, null, new IAccessRule[0], attributes, false);
		}
		if (!entries.contains(entry)) {
			entries.add(entry);
		}
	}
	
	protected IAccessRule[] getAccessRules(Rule[] rules) {
		IAccessRule[] accessRules = new IAccessRule[rules.length + 1];
		for (int i = 0; i < rules.length; i++) {
			Rule rule = rules[i];
			accessRules[i] = rule.discouraged ? getDiscouragedRule(rule.path) : getAccessibleRule(rule.path);
		}
		accessRules[rules.length] = EXCLUDE_ALL_RULE;
		return accessRules;
	}
	
	private static synchronized IAccessRule getAccessibleRule(IPath path) {
		IAccessRule rule = (IAccessRule)ACCESSIBLE_RULES.get(path);
		if (rule == null) {
			rule = JavaCore.newAccessRule(path, IAccessRule.K_ACCESSIBLE);
			ACCESSIBLE_RULES.put(path, rule);
		}
		return rule;
	}
	
	private IClasspathAttribute[] getClasspathAttributes(IPluginModelBase model) {
		JavadocLocationManager manager = PDECore.getDefault().getJavadocLocationManager();
		String location = manager.getJavadocLocation(model);
		if (location == null)
			return new IClasspathAttribute[0];
		return new IClasspathAttribute[] {JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, location)};
	}
	
	private static synchronized IAccessRule getDiscouragedRule(IPath path) {
		IAccessRule rule = (IAccessRule)DISCOURAGED_RULES.get(path);
		if (rule == null) {
			rule = JavaCore.newAccessRule(path, IAccessRule.K_DISCOURAGED);
			DISCOURAGED_RULES.put(path, rule);
		}
		return rule;		
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
