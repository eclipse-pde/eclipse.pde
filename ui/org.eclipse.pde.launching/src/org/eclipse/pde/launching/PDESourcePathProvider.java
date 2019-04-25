/*******************************************************************************
 *  Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.pde.launching;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.launching.*;
import org.eclipse.pde.internal.core.util.PDEJavaHelper;
import org.eclipse.pde.internal.launching.PDELaunchingPlugin;
import org.eclipse.pde.internal.launching.launcher.LaunchPluginValidator;
import org.eclipse.pde.internal.launching.launcher.VMHelper;

/**
 * Generates a source lookup path for all PDE-based launch configurations
 * <p>
 * Clients may subclass this class.
 * </p>
 * <p>
 * This class originally existed in 3.3 as
 * <code>org.eclipse.pde.ui.launcher.PDESourcePathProvider</code>.
 * </p>
 * @since 3.6
 */
public class PDESourcePathProvider extends StandardSourcePathProvider {

	/**
	 * The ID of this source lookup path provider
	 */
	public static final String ID = "org.eclipse.pde.ui.workbenchClasspathProvider"; //$NON-NLS-1$

	@Override
	public IRuntimeClasspathEntry[] computeUnresolvedClasspath(ILaunchConfiguration configuration) throws CoreException {
		List<IRuntimeClasspathEntry> sourcePath = new ArrayList<>();
		sourcePath.add(VMHelper.getJREEntry(configuration));
		IProject[] projects = getJavaProjects(configuration);
		for (IProject project : projects) {
			sourcePath.add(JavaRuntime.newProjectRuntimeClasspathEntry(JavaCore.create(project)));
		}
		return sourcePath.toArray(new IRuntimeClasspathEntry[sourcePath.size()]);
	}

	/**
	 * Returns an array of sorted plug-in projects that represent plug-ins participating
	 * in the launch
	 *
	 * @param configuration
	 * 			the launch configuration
	 * @return an array of ordered projects
	 * @throws CoreException
	 * 			if unable to retrieve attributes from the launch configuration or if
	 * 			an error occurs when checking the nature of the project
	 *
	 */
	private IProject[] getJavaProjects(ILaunchConfiguration configuration) throws CoreException {
		IProject[] projects = LaunchPluginValidator.getAffectedProjects(configuration, false);
		return PDELaunchingPlugin.getWorkspace().computeProjectOrder(projects).projects;
	}

	@Override
	public IRuntimeClasspathEntry[] resolveClasspath(IRuntimeClasspathEntry[] entries, ILaunchConfiguration configuration) throws CoreException {
		List<IRuntimeClasspathEntry> all = new ArrayList<>(entries.length);
		for (IRuntimeClasspathEntry entrie : entries) {
			if (entrie.getType() == IRuntimeClasspathEntry.PROJECT) {
				// a project resolves to itself for source lookup (rather than
				// the class file output locations)
				all.add(entrie);
				// also add non-JRE libraries
				IResource resource = entrie.getResource();
				if (resource instanceof IProject) {
					addBinaryPackageFragmentRoots(JavaCore.create((IProject) resource), all);
				}
			} else {
				IRuntimeClasspathEntry[] resolved = JavaRuntime.resolveRuntimeClasspathEntry(entrie, configuration);
				for (IRuntimeClasspathEntry element : resolved) {
					all.add(element);
				}
			}
		}
		return all.toArray(new IRuntimeClasspathEntry[all.size()]);
	}

	/**
	 * Adds runtime classpath entries for binary package fragment roots contained within
	 * the project
	 *
	 * @param jProject
	 * 			the Java project whose roots are to be enumerated
	 * @param all
	 * 			a list of accumulated runtime classpath entries
	 * @throws CoreException
	 * 			if unable to evaluate the package fragment roots
	 */
	private void addBinaryPackageFragmentRoots(IJavaProject jProject, List<IRuntimeClasspathEntry> all) throws CoreException {
		IPackageFragmentRoot[] roots = jProject.getPackageFragmentRoots();
		for (int j = 0; j < roots.length; j++) {
			if (roots[j].getKind() == IPackageFragmentRoot.K_BINARY && !PDEJavaHelper.isJRELibrary(roots[j])) {
				IRuntimeClasspathEntry rte = JavaRuntime.newArchiveRuntimeClasspathEntry(roots[j].getPath());
				IPath path = roots[j].getSourceAttachmentPath();
				if (path != null) {
					rte.setSourceAttachmentPath(path);
					rte.setSourceAttachmentRootPath(roots[j].getSourceAttachmentRootPath());
				}
				if (!all.contains(rte))
					all.add(rte);
			}
		}

	}
}