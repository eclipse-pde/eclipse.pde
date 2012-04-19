/*******************************************************************************
 *  Copyright (c) 2005, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.*;

public class PDEClasspathContainer {

	public class Rule {
		IPath path;
		boolean discouraged;

		public boolean equals(Object other) {
			if (!(other instanceof Rule))
				return false;
			return discouraged == ((Rule) other).discouraged && path.equals(((Rule) other).path);
		}

		public String toString() {
			return discouraged ? path.toString() + " [discouraged]" : path.toString(); //$NON-NLS-1$
		}
	}

	private static HashMap ACCESSIBLE_RULES = new HashMap();
	private static HashMap DISCOURAGED_RULES = new HashMap();

	private static final IAccessRule EXCLUDE_ALL_RULE = JavaCore.newAccessRule(new Path("**/*"), IAccessRule.K_NON_ACCESSIBLE | IAccessRule.IGNORE_IF_BETTER); //$NON-NLS-1$

	protected void addProjectEntry(IProject project, Rule[] rules, ArrayList entries) throws CoreException {
		if (project.hasNature(JavaCore.NATURE_ID)) {
			IClasspathEntry entry = null;
			if (rules != null) {
				IAccessRule[] accessRules = getAccessRules(rules);
				entry = JavaCore.newProjectEntry(project.getFullPath(), accessRules, true, new IClasspathAttribute[0], false);
			} else {
				entry = JavaCore.newProjectEntry(project.getFullPath());
			}
			if (!entries.contains(entry))
				entries.add(entry);
		}
	}

	public static IClasspathEntry[] getExternalEntries(IPluginModelBase model) {
		ArrayList entries = new ArrayList();
		addExternalPlugin(model, new Rule[0], entries);
		return (IClasspathEntry[]) entries.toArray(new IClasspathEntry[entries.size()]);
	}

	protected static void addExternalPlugin(IPluginModelBase model, Rule[] rules, ArrayList entries) {
		if (new File(model.getInstallLocation()).isFile()) {
			IPath srcPath = ClasspathUtilCore.getSourceAnnotation(model, "."); //$NON-NLS-1$
			if (srcPath == null)
				srcPath = new Path(model.getInstallLocation());
			addLibraryEntry(new Path(model.getInstallLocation()), srcPath, rules, getClasspathAttributes(model), entries);

			// If the jarred plugin contains any jarred libraries they must be extracted as the compiler can't handle nested jar files
			File[] extractedLibraries = PDECore.getDefault().getModelManager().getExternalModelManager().getExtractedLibraries(model);
			for (int i = 0; i < extractedLibraries.length; i++) {
				Path path = new Path(extractedLibraries[i].getAbsolutePath());
				addLibraryEntry(path, path, rules, getClasspathAttributes(model), entries);
			}
		} else {
			IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
			if (libraries.length == 0) {
				// If there are no libraries, assume the root of the plug-in is the library '.'
				IPath srcPath = ClasspathUtilCore.getSourceAnnotation(model, "."); //$NON-NLS-1$
				if (srcPath == null)
					srcPath = new Path(model.getInstallLocation());
				addLibraryEntry(new Path(model.getInstallLocation()), srcPath, rules, getClasspathAttributes(model), entries);
			} else {
				for (int i = 0; i < libraries.length; i++) {
					if (IPluginLibrary.RESOURCE.equals(libraries[i].getType()))
						continue;
					model = (IPluginModelBase) libraries[i].getModel();
					String name = libraries[i].getName();
					String expandedName = ClasspathUtilCore.expandLibraryName(name);
					IPath path = ClasspathUtilCore.getPath(model, expandedName);
					if (path == null && !model.isFragmentModel() && ClasspathUtilCore.containsVariables(name)) {
						model = resolveLibraryInFragments(model, expandedName);
						if (model != null && model.isEnabled())
							path = ClasspathUtilCore.getPath(model, expandedName);
					}
					if (path != null)
						addLibraryEntry(path, ClasspathUtilCore.getSourceAnnotation(model, expandedName), rules, getClasspathAttributes(model), entries);
				}
			}
		}
	}

	protected static void addLibraryEntry(IPath path, IPath srcPath, Rule[] rules, IClasspathAttribute[] attributes, ArrayList entries) {
		IClasspathEntry entry = null;
		if (rules != null) {
			entry = JavaCore.newLibraryEntry(path, srcPath, null, getAccessRules(rules), attributes, false);
		} else {
			entry = JavaCore.newLibraryEntry(path, srcPath, null, new IAccessRule[0], attributes, false);
		}
		if (!entries.contains(entry)) {
			entries.add(entry);
		}
	}

	protected static IAccessRule[] getAccessRules(Rule[] rules) {
		IAccessRule[] accessRules = new IAccessRule[rules.length + 1];
		for (int i = 0; i < rules.length; i++) {
			Rule rule = rules[i];
			accessRules[i] = rule.discouraged ? getDiscouragedRule(rule.path) : getAccessibleRule(rule.path);
		}
		accessRules[rules.length] = EXCLUDE_ALL_RULE;
		return accessRules;
	}

	private static synchronized IAccessRule getAccessibleRule(IPath path) {
		IAccessRule rule = (IAccessRule) ACCESSIBLE_RULES.get(path);
		if (rule == null) {
			rule = JavaCore.newAccessRule(path, IAccessRule.K_ACCESSIBLE);
			ACCESSIBLE_RULES.put(path, rule);
		}
		return rule;
	}

	private static IClasspathAttribute[] getClasspathAttributes(IPluginModelBase model) {
		JavadocLocationManager manager = PDECore.getDefault().getJavadocLocationManager();
		String location = manager.getJavadocLocation(model);
		if (location == null)
			return new IClasspathAttribute[0];
		return new IClasspathAttribute[] {JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, location)};
	}

	private static synchronized IAccessRule getDiscouragedRule(IPath path) {
		IAccessRule rule = (IAccessRule) DISCOURAGED_RULES.get(path);
		if (rule == null) {
			rule = JavaCore.newAccessRule(path, IAccessRule.K_DISCOURAGED);
			DISCOURAGED_RULES.put(path, rule);
		}
		return rule;
	}

	protected static IPluginModelBase resolveLibraryInFragments(IPluginModelBase model, String libraryName) {
		BundleDescription desc = model.getBundleDescription();
		if (desc != null) {
			BundleDescription[] fragments = desc.getFragments();
			for (int i = 0; i < fragments.length; i++) {
				if (new File(fragments[i].getLocation(), libraryName).exists())
					return PluginRegistry.findModel(fragments[i]);
			}
		}
		return null;
	}

}
