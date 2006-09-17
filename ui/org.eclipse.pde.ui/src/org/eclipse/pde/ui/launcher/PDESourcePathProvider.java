/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.launcher;
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
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.StandardSourcePathProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.launcher.LaunchPluginValidator;
import org.eclipse.pde.internal.ui.launcher.VMHelper;
import org.eclipse.pde.internal.ui.util.PDEJavaHelper;

/**
 * Generates a source lookup path for all PDE-based launch configurations
 * <p>
 * Clients may subclass this class.
 * </p>
 * @since 3.3
 */
public class PDESourcePathProvider extends StandardSourcePathProvider {

	public static final String ID = "org.eclipse.pde.ui.workbenchClasspathProvider"; //$NON-NLS-1$
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jdt.launching.StandardSourcePathProvider#computeUnresolvedClasspath(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public IRuntimeClasspathEntry[] computeUnresolvedClasspath(ILaunchConfiguration configuration) throws CoreException {
		List sourcePath = new ArrayList();
		sourcePath.add(getJREEntry(configuration));
		IProject[] projects = getJavaProjects(configuration);
		for (int i = 0; i < projects.length; i++) {
			sourcePath.add(JavaRuntime.newProjectRuntimeClasspathEntry(JavaCore.create(projects[i])));
		}
		return (IRuntimeClasspathEntry[]) sourcePath.toArray(new IRuntimeClasspathEntry[sourcePath.size()]);
	}
	
	/**
	 * Returns a JRE runtime classpath entry
	 * 
	 * @param configuration
	 * 			the launch configuration
	 * @return a JRE runtime classpath entry
	 * @throws CoreException
	 * 			if the JRE associated with the launch configuration cannot be found
	 * 			or if unable to retrieve the launch configuration attributes
	 */
	private IRuntimeClasspathEntry getJREEntry(ILaunchConfiguration configuration) throws CoreException {
		IVMInstall jre = VMHelper.createLauncher(configuration);
		IPath containerPath = new Path(JavaRuntime.JRE_CONTAINER);
		containerPath = containerPath.append(jre.getVMInstallType().getId());
		containerPath = containerPath.append(jre.getName());
		return JavaRuntime.newRuntimeContainerClasspathEntry(containerPath, IRuntimeClasspathEntry.BOOTSTRAP_CLASSES);		
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
		IProject[] projects = LaunchPluginValidator.getAffectedProjects(configuration);
		return PDEPlugin.getWorkspace().computeProjectOrder(projects).projects;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jdt.launching.StandardSourcePathProvider#resolveClasspath(org.eclipse.jdt.launching.IRuntimeClasspathEntry[], org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public IRuntimeClasspathEntry[] resolveClasspath(IRuntimeClasspathEntry[] entries, ILaunchConfiguration configuration)
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
					addBinaryPackageFragmentRoots(JavaCore.create((IProject)resource), all);
				}
			} else {
				IRuntimeClasspathEntry[] resolved = JavaRuntime.resolveRuntimeClasspathEntry(entries[i], configuration);
				for (int j = 0; j < resolved.length; j++) {
					all.add(resolved[j]);
				}
			}
		}
		return (IRuntimeClasspathEntry[]) all.toArray(new IRuntimeClasspathEntry[all.size()]);
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
	private void addBinaryPackageFragmentRoots(IJavaProject jProject, List all) throws CoreException {
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