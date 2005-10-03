/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.StandardSourcePathProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;
/**
 * Generates a source lookup path for Runtime Workbench launch configurations.
 */
public class WorkbenchSourcePathProvider extends StandardSourcePathProvider {
	/**
	 * @see org.eclipse.jdt.launching.IRuntimeClasspathProvider#computeUnresolvedClasspath(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public IRuntimeClasspathEntry[] computeUnresolvedClasspath(
			ILaunchConfiguration configuration) throws CoreException {
		boolean defaultPath = configuration.getAttribute(
				IJavaLaunchConfigurationConstants.ATTR_DEFAULT_SOURCE_PATH,
				true);
		if (!defaultPath) {
			return recoverRuntimePath(configuration,
					IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH);
		}
		List sourcePath = new ArrayList();
		// first on the source lookup path, goes the class libraries for the
		// JRE
		String vmInstallName = configuration.getAttribute(
				IPDELauncherConstants.VMINSTALL, VMHelper
						.getDefaultVMInstallName());
		IVMInstall[] vmInstallations = VMHelper.getAllVMInstances();
		IVMInstall jre = null;
		for (int i = 0; i < vmInstallations.length; i++) {
			if (vmInstallName.equals(vmInstallations[i].getName())) {
				jre = vmInstallations[i];
				break;
			}
		}
		if (jre != null) {
			// add container that corresponds to JRE
			IPath containerPath = new Path(JavaRuntime.JRE_CONTAINER);
			containerPath = containerPath
					.append(jre.getVMInstallType().getId());
			containerPath = containerPath.append(jre.getName());
			IRuntimeClasspathEntry entry = JavaRuntime
					.newRuntimeContainerClasspathEntry(containerPath,
							IRuntimeClasspathEntry.BOOTSTRAP_CLASSES);
			sourcePath.add(entry);
		}
		IProject[] projects = getJavaProjects(configuration);
		for (int i = 0; i < projects.length; i++) {
			sourcePath.add(JavaRuntime
					.newProjectRuntimeClasspathEntry(JavaCore.create(projects[i])));
		}
		return (IRuntimeClasspathEntry[]) sourcePath
				.toArray(new IRuntimeClasspathEntry[sourcePath.size()]);
	}
	/**
	 * Converts plugin models to java projects
	 */
	private IProject[] getJavaProjects(ILaunchConfiguration configuration)
			throws CoreException {
		IProject[] projects = LaunchPluginValidator.getAffectedProjects(configuration);
		return PDEPlugin.getWorkspace().computeProjectOrder(projects).projects;
	}
	/**
	 * @see IRuntimeClasspathProvider#resolveClasspath(IRuntimeClasspathEntry[],
	 *      ILaunchConfiguration)
	 */
	public IRuntimeClasspathEntry[] resolveClasspath(
			IRuntimeClasspathEntry[] entries, ILaunchConfiguration configuration)
			throws CoreException {
		List all = new ArrayList(entries.length);
		for (int i = 0; i < entries.length; i++) {
			if (entries[i].getType() == IRuntimeClasspathEntry.PROJECT) {
				// a project resolves to itself for source lookup (rather than
				// the class file output locations)
				all.add(entries[i]);
				// also add non-JRE libraries
				IResource resource = entries[i].getResource();
				if (resource instanceof IProject) {
					IJavaProject project = JavaCore.create((IProject) resource);
					IPackageFragmentRoot[] roots = project
							.getPackageFragmentRoots();
					for (int j = 0; j < roots.length; j++) {
						if (roots[j].getKind() == IPackageFragmentRoot.K_BINARY && !isJRELibrary(roots[j])) {
							IRuntimeClasspathEntry rte = JavaRuntime
									.newArchiveRuntimeClasspathEntry(roots[j]
											.getPath());
							IPath path = roots[j].getSourceAttachmentPath();
							if (path != null) {
								rte.setSourceAttachmentPath(path);
								rte.setSourceAttachmentRootPath(roots[j]
										.getSourceAttachmentRootPath());
							}
							if (!all.contains(rte))
								all.add(rte);
						}
					}
				}
			} else {
				IRuntimeClasspathEntry[] resolved = JavaRuntime
						.resolveRuntimeClasspathEntry(entries[i], configuration);
				for (int j = 0; j < resolved.length; j++) {
					all.add(resolved[j]);
				}
			}
		}
		return (IRuntimeClasspathEntry[]) all
				.toArray(new IRuntimeClasspathEntry[all.size()]);
	}
	private boolean isJRELibrary(IPackageFragmentRoot root) {
		try {
			IPath path = root.getRawClasspathEntry().getPath();
			if (path.equals(new Path(JavaRuntime.JRE_CONTAINER))
					|| path.equals(new Path(JavaRuntime.JRELIB_VARIABLE))) {
				return true;
			}
		} catch (JavaModelException e) {
		}
		return false;
	}
}
