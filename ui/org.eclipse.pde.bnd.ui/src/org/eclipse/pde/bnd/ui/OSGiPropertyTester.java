/*******************************************************************************
 *  Copyright (c) 2024 Christoph Läubrich and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.bnd.ui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.osgi.framework.Constants;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;

public class OSGiPropertyTester extends PropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		IProject project = getProject(receiver);
		if (project != null) {
			if ("isOSGiClasspathProject".equals(property)) {
				return isOSGiClasspathProject(project);
			}
		}
		return false;
	}

	public static boolean isOSGiClasspathProject(IProject project) {
		// only java projects can have a classpath...
		if (isJavaNature(project)) {
			// if it already has some well known nature we can assume its true
			if (isPdeNature(project) || isBndToolsNature(project)) {
				return true;
			}
			// otherwise we need to dig a bit deeper
			if (isAutomaticManifest(project) || isManifestFirst(project)) {
				return true;
			}
			if (isBndWithClasspath(project)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isAutomaticManifest(IProject project) {
		return project.getFile("pde.bnd").exists();
	}

	protected static boolean isBndWithClasspath(IProject project) {
		IFile bndFile = project.getFile(Project.BNDFILE);
		if (bndFile.exists()) {
			// it might be a BND project with classpath ...
			IPath location = project.getLocation();
			if (location != null) {
				File projectDir = location.toFile();
				if (projectDir != null) {
					try {
						Project plainBnd = Workspace.getProject(projectDir);
						if (!plainBnd.getBuildpath().isEmpty() || !plainBnd.getTestpath().isEmpty()) {
							return true;
						}
					} catch (Exception e) {
						// no way to tell...
					}
				}
			}
		}
		return false;
	}

	protected static boolean isManifestFirst(IProject project) {
		IFile manifestFile = project.getFile(JarFile.MANIFEST_NAME);
		if (manifestFile.exists()) {
			// This seems to be a "manifest-first" project...
			try (InputStream contents = manifestFile.getContents()) {
				Manifest manifest = new Manifest(contents);
				return manifest.getMainAttributes().getValue(Constants.BUNDLE_MANIFESTVERSION) != null;
			} catch (IOException | CoreException e) {
				// we can't make further assumptions here...
			}
		}
		return false;
	}

	private static boolean isBndToolsNature(IProject project) {
		try {
			return project.hasNature("bndtools.core.bndnature");
		} catch (CoreException e) {
			// we can't know then...
		}
		return false;
	}

	private static boolean isPdeNature(IProject project) {
		try {
			return project.hasNature("org.eclipse.pde.PluginNature");
		} catch (CoreException e) {
			// we can't know then...
		}
		return false;
	}

	private static boolean isJavaNature(IProject project) {
		try {
			return project.hasNature("org.eclipse.jdt.core.javanature");
		} catch (CoreException e) {
			// we can't know then...
		}
		return false;
	}

	private IProject getProject(Object receiver) {
		if (receiver instanceof IProject project) {
			return project;
		}
		if (receiver instanceof IJavaProject java) {
			return java.getProject();
		}
		return null;
	}

}
