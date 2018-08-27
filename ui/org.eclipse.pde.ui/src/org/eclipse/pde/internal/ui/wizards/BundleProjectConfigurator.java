/*******************************************************************************
 * Copyright (c) 2014, 2017 Red Hat Inc., and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Mickael Istria (Red Hat Inc.) - initial API and implementation
 ******************************************************************************/
package org.eclipse.pde.internal.ui.wizards;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.Manifest;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.internal.ui.wizards.importer.ProjectWithJavaResourcesImportConfigurator;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.internal.core.ClasspathComputer;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.wizards.datatransfer.ProjectConfigurator;
import org.osgi.framework.Constants;

public class BundleProjectConfigurator implements ProjectConfigurator {

	@Override
	public boolean canConfigure(IProject project, Set<IPath> ignoredDirectories, IProgressMonitor monitor) {
		IFile manifestFile = PDEProject.getManifest(project);
		if (manifestFile != null && manifestFile.exists()) {
			for (IPath ignoredDirectory : ignoredDirectories) {
				if (ignoredDirectory.isPrefixOf(manifestFile.getLocation())) {
					return false;
				}
			}
		}
		return hasOSGiManifest(project);
	}

	@Override
	public void configure(IProject project, Set<IPath> ignoredDirectories, IProgressMonitor monitor) {
		if (PDE.hasPluginNature(project)) {
			// already configured, nothing else to do
			return;
		}
		try {
			CoreUtility.addNatureToProject(project, PDE.PLUGIN_NATURE, monitor);
			if (project.hasNature(JavaCore.NATURE_ID)) {
				return;
			}
		} catch (CoreException ex) {
			PDEPlugin.log(ex);
			return;
		}

		// configure Java & Classpaht
		IFile buildPropertiesFile = PDEProject.getBuildProperties(project);
		Properties buildProperties = new Properties();
		if (buildPropertiesFile.exists()) {
			try (InputStream stream = buildPropertiesFile.getContents()) {
				buildProperties.load(stream);
			} catch (IOException | CoreException ex) {
				PDEPlugin.log(ex);
				return;
			}
		}

		boolean hasSourceFolder = false;
		for (String entry : buildProperties.stringPropertyNames()) {
			hasSourceFolder |= (entry.startsWith("src.") || entry.startsWith(IBuildEntry.JAR_PREFIX)); //$NON-NLS-1$
		}
		if (!hasSourceFolder) {
			// Nothing for Java
			return;
		}

		try {
			CoreUtility.addNatureToProject(project, JavaCore.NATURE_ID, monitor);
			IJavaProject javaProject = JavaCore.create(project);
			Set<IClasspathEntry> classpath = new HashSet<>();
			for (Entry<?, ?> entry : buildProperties.entrySet()) {
				String entryKey = (String)entry.getKey();
				if (entryKey.startsWith("src.") || entryKey.startsWith(IBuildEntry.JAR_PREFIX)) { //$NON-NLS-1$
					for (String token : ((String) entry.getValue()).split(",")) { //$NON-NLS-1$
						token = token.trim();
						if (token.endsWith("/")) { //$NON-NLS-1$
							token = token.substring(0, token.length() - 1);
						}
						if (token != null && !token.isEmpty() && !token.equals(".")) { //$NON-NLS-1$
							IFolder folder = project.getFolder(token);
							if (folder.exists()) {
								classpath.add(JavaCore.newSourceEntry(folder.getFullPath()));
							}
						}
					}
				} else if (entryKey.equals(IBuildEntry.OUTPUT_PREFIX + '.')) {
					javaProject.setOutputLocation(project.getFolder(((String)entry.getValue()).trim()).getFullPath(), monitor);
				}
			}
			// TODO select container according to BREE
			classpath.add(JavaRuntime.getDefaultJREContainerEntry());
			classpath.add(ClasspathComputer.createContainerEntry());
			javaProject.setRawClasspath(classpath.toArray(new IClasspathEntry[classpath.size()]), monitor);
		} catch (CoreException ex) {
			PDEPlugin.log(ex);
		}
	}

	@Override
	public boolean shouldBeAnEclipseProject(IContainer container, IProgressMonitor monitor) {
		return hasOSGiManifest(container);
	}

	private boolean hasOSGiManifest(IContainer container) {
		IFile manifestResource = container.getFile(new Path(ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR));
		if (manifestResource.exists()) {
			Manifest manifest = new Manifest();
			try (InputStream stream = manifestResource.getContents()) {
				manifest.read(stream);
				return manifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME) != null;
			} catch (CoreException | IOException ex) {
				PDEPlugin.log(ex);
			}
		}
		return false;
	}

	@Override
	public Set<IFolder> getFoldersToIgnore(IProject project, IProgressMonitor monitor) {
		Set<IFolder> res = new HashSet<>();
		res.addAll(new ProjectWithJavaResourcesImportConfigurator().getFoldersToIgnore(project, monitor));
		IFile buildPropertiesFile = PDEProject.getBuildProperties(project);
		Properties buildProperties = new Properties();
		if (!buildPropertiesFile.exists()) {
			return Collections.emptySet();
		}
		try (InputStream stream = buildPropertiesFile.getContents()) {
			buildProperties.load(stream);
			for (Entry<?, ?> entry : buildProperties.entrySet()) {
				String entryKey = (String) entry.getKey();
				if (entryKey.startsWith("src.") || entryKey.startsWith(IBuildEntry.JAR_PREFIX) || //$NON-NLS-1$
						entryKey.startsWith("bin.") || entryKey.startsWith(IBuildEntry.OUTPUT_PREFIX)) { //$NON-NLS-1$
					for (String token : ((String) entry.getValue()).split(",")) { //$NON-NLS-1$
						token = token.trim();
						if (token.endsWith("/")) { //$NON-NLS-1$
							token = token.substring(0, token.length() - 1);
						}
						if (token != null && token.length() > 0 && !token.equals(".")) { //$NON-NLS-1$
							IFolder folder = project.getFolder(token);
							if (folder.exists()) {
								res.add(folder);
							}
						}
					}
				}
			}
			return res;
		} catch (CoreException | IOException ex) {
			PDEPlugin.log(ex);
			return Collections.emptySet();
		}
	}

	@Override
	public Set<File> findConfigurableLocations(File root, IProgressMonitor monitor) {
		// Not really easy to spot PDE projects from a given directory
		// Moreover PDE projects are often expected to have a .project, which is supported
		// by EclipseProjectConfigurator
		return null;
	}

}
