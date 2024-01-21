/*******************************************************************************
 *  Copyright (c) 2005, 2012 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import static org.eclipse.jdt.core.JavaCore.newClasspathAttribute;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.osgi.resource.Resource;

public class PDEClasspathContainer {

	static record Rule(IPath path, boolean discouraged) {
		@Override
		public String toString() {
			return discouraged ? path.toString() + " [discouraged]" : path.toString(); //$NON-NLS-1$
		}
	}

	private static final Map<IPath, IAccessRule> ACCESSIBLE_RULES = new ConcurrentHashMap<>();
	private static final Map<IPath, IAccessRule> DISCOURAGED_RULES = new ConcurrentHashMap<>();

	private static final IAccessRule EXCLUDE_ALL_RULE = JavaCore.newAccessRule(IPath.fromOSString("**/*"), IAccessRule.K_NON_ACCESSIBLE | IAccessRule.IGNORE_IF_BETTER); //$NON-NLS-1$

	protected void addProjectEntry(IProject project, List<Rule> rules, boolean exportsExternalAnnotations,
			List<IClasspathEntry> entries) throws CoreException {
		if (project.hasNature(JavaCore.NATURE_ID)) {
			IAccessRule[] accessRules = rules != null ? getAccessRules(rules) : null;
			IClasspathAttribute[] extraAttribs = getClasspathAttributesForProject(project, exportsExternalAnnotations);
			IClasspathEntry entry = JavaCore.newProjectEntry(project.getFullPath(), accessRules, true, extraAttribs, false);
			if (!entries.contains(entry)) {
				entries.add(entry);
			}
		}
	}

	private IClasspathAttribute[] getClasspathAttributesForProject(IProject project, boolean exportsExternalAnnotations)
			throws JavaModelException {
		if (exportsExternalAnnotations) {
			String annotationPath = JavaCore.create(project).getOutputLocation().toString();
			return new IClasspathAttribute[] {
					JavaCore.newClasspathAttribute(IClasspathAttribute.EXTERNAL_ANNOTATION_PATH, annotationPath) };
		}
		return new IClasspathAttribute[0];
	}

	public static IClasspathEntry[] getExternalEntries(IPluginModelBase model) {
		List<IClasspathEntry> entries = new ArrayList<>();
		addExternalPlugin(model, List.of(), entries);
		return entries.toArray(new IClasspathEntry[entries.size()]);
	}

	protected static void addExternalPlugin(IPluginModelBase model, List<Rule> rules, List<IClasspathEntry> entries) {
		boolean isJarShape = new File(model.getInstallLocation()).isFile();
		if (isJarShape) {
			IPath srcPath = ClasspathUtilCore.getSourceAnnotation(model, ".", isJarShape); //$NON-NLS-1$
			if (srcPath == null) {
				srcPath = IPath.fromOSString(model.getInstallLocation());
			}
			addLibraryEntry(IPath.fromOSString(model.getInstallLocation()), srcPath, rules, getClasspathAttributes(model), entries);

			// If the jarred plugin contains any jarred libraries they must be extracted as the compiler can't handle nested jar files
			File[] extractedLibraries = PDECore.getDefault().getModelManager().getExternalModelManager().getExtractedLibraries(model);
			for (File libraryFile : extractedLibraries) {
				IPath path = IPath.fromOSString(libraryFile.getAbsolutePath());
				addLibraryEntry(path, path, rules, getClasspathAttributes(model), entries);
			}
		} else {
			IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
			if (libraries.length == 0) {
				// If there are no libraries, assume the root of the plug-in is the library '.'
				IPath srcPath = ClasspathUtilCore.getSourceAnnotation(model, ".", isJarShape); //$NON-NLS-1$
				if (srcPath == null) {
					srcPath = IPath.fromOSString(model.getInstallLocation());
				}
				addLibraryEntry(IPath.fromOSString(model.getInstallLocation()), srcPath, rules, getClasspathAttributes(model), entries);
			} else {
				for (IPluginLibrary library : libraries) {
					if (IPluginLibrary.RESOURCE.equals(library.getType())) {
						continue;
					}
					model = (IPluginModelBase) library.getModel();
					String name = library.getName();
					String expandedName = ClasspathUtilCore.expandLibraryName(name);
					IPath path = ClasspathUtilCore.getPath(model, expandedName, isJarShape);
					if (path == null && !model.isFragmentModel() && ClasspathUtilCore.containsVariables(name)) {
						model = resolveLibraryInFragments(model, expandedName);
						if (model != null && model.isEnabled()) {
							path = ClasspathUtilCore.getPath(model, expandedName, isJarShape);
						}
					}
					if (path != null) {
						addLibraryEntry(path, ClasspathUtilCore.getSourceAnnotation(model, expandedName, isJarShape),
								rules, getClasspathAttributes(model), entries);
					}
				}
			}
		}
	}

	protected static void addLibraryEntry(IPath path, IPath srcPath, List<Rule> rules, IClasspathAttribute[] attributes,
			List<IClasspathEntry> entries) {
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

	protected static IAccessRule[] getAccessRules(List<Rule> rules) {
		IAccessRule[] accessRules = new IAccessRule[rules.size() + 1];
		int i = 0;
		for (Rule rule : rules) {
			IPath path = rule.path;
			accessRules[i++] = rule.discouraged
					? DISCOURAGED_RULES.computeIfAbsent(path, p -> JavaCore.newAccessRule(p, IAccessRule.K_DISCOURAGED))
					: ACCESSIBLE_RULES.computeIfAbsent(path, p -> JavaCore.newAccessRule(p, IAccessRule.K_ACCESSIBLE));
		}
		accessRules[rules.size()] = EXCLUDE_ALL_RULE;
		return accessRules;
	}

	private static IClasspathAttribute[] getClasspathAttributes(IPluginModelBase model) {
		List<IClasspathAttribute> attributes = new ArrayList<>();
		JavadocLocationManager manager = PDECore.getDefault().getJavadocLocationManager();
		String location = manager.getJavadocLocation(model);
		if (location != null) {
			attributes.add(newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, location));
		}
		if (model.getPluginBase().exportsExternalAnnotations()) {
			String installLocation = model.getInstallLocation();
			attributes.add(newClasspathAttribute(IClasspathAttribute.EXTERNAL_ANNOTATION_PATH, installLocation));
		}
		return attributes.toArray(IClasspathAttribute[]::new);
	}


	protected static IPluginModelBase resolveLibraryInFragments(IPluginModelBase model, String libraryName) {
		BundleDescription desc = model.getBundleDescription();
		if (desc != null) {
			BundleDescription[] fragments = desc.getFragments();
			for (BundleDescription fragment : fragments) {
				if (new File(fragment.getLocation(), libraryName).exists()) {
					return PluginRegistry.findModel((Resource) fragment);
				}
			}
		}
		return null;
	}

}
