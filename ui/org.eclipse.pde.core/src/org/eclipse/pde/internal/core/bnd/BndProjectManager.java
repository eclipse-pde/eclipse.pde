/*******************************************************************************
 *  Copyright (c) 2023 Christoph Läubrich and others.
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
package org.eclipse.pde.internal.core.bnd;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.annotations.OSGiAnnotationsClasspathContributor;
import org.eclipse.pde.internal.core.natures.BndProject;

import aQute.bnd.build.Container;
import aQute.bnd.build.Container.TYPE;
import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;

public class BndProjectManager {

	private static final String DELIMITER = ","; //$NON-NLS-1$
	private static final String TRUE = "true"; //$NON-NLS-1$
	private static Workspace workspace;

	public static Optional<Project> getBndProject(IProject project) throws Exception {
		if (BndProject.isBndProject(project)) {
			IPath projectPath = project.getLocation();
			if (projectPath != null) {
				File projectFolder = projectPath.toFile();
				if (projectFolder != null) {
					Project bnd = new Project(getWorkspace(), projectFolder,
							new File(projectFolder, BndProject.INSTRUCTIONS_FILE));
					bnd.setBase(projectFolder);
					setupProject(bnd, project);
					return Optional.of(bnd);
				}
			}
		}
		return Optional.empty();
	}

	private static void setupProject(Project bnd, IProject project) throws CoreException {
		IPath base = project.getFullPath();
		if (project.hasNature(JavaCore.NATURE_ID)) {
			IJavaProject javaProject = JavaCore.create(project);
			IClasspathEntry[] classpath = javaProject.getRawClasspath();
			List<String> src = new ArrayList<>(1);
			for (IClasspathEntry cpe : classpath) {
				if (cpe.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					src.add(cpe.getPath().makeRelativeTo(base).toString());
				}
			}
			String outputLocation = javaProject.getOutputLocation().makeRelativeTo(base).toString();
			bnd.setProperty(Constants.DEFAULT_PROP_SRC_DIR, src.stream().collect(Collectors.joining(DELIMITER)));
			bnd.setProperty(Constants.DEFAULT_PROP_BIN_DIR, outputLocation);
			bnd.setProperty(Constants.DEFAULT_PROP_TARGET_DIR, outputLocation);
		}
		String buildPath = bnd.getProperty(Constants.BUILDPATH);
		Stream<String> enhnacedBuildPath = OSGiAnnotationsClasspathContributor.annotations()
				.map(p -> p.getPluginBase().getId());
		if (buildPath != null) {
			enhnacedBuildPath = Stream.concat(Stream.of(buildPath), enhnacedBuildPath);
		}
		bnd.setProperty(Constants.BUILDPATH, enhnacedBuildPath.collect(Collectors.joining(DELIMITER)));
	}

	static synchronized Workspace getWorkspace() throws Exception {
		if (workspace == null) {
			try (Processor run = new Processor()) {
				run.setProperty(Constants.STANDALONE, TRUE);
				IPath path = PDECore.getDefault().getStateLocation().append(Project.BNDCNF);
				workspace = Workspace.createStandaloneWorkspace(run, path.toFile().toURI());
				workspace.addBasicPlugin(TargetRepository.getTargetRepository());
				workspace.refresh();
			}
		}
		return workspace;
	}

	static void publishContainerEntries(List<IClasspathEntry> entries, Collection<Container> containers,
			boolean isTest, Set<Project> mapped) {
		for (Container container : containers) {
			TYPE type = container.getType();
			if (type == TYPE.PROJECT) {
				Project project = container.getProject();
				if (mapped.contains(project)) {
					// it is already mapped as a project dependency...
					continue;
				}
				mapped.add(project);
			}
			File file = container.getFile();
			if (file.exists()) {
				IPath path = IPath.fromOSString(file.getAbsolutePath());
				IClasspathAttribute[] attributes;
				if (isTest) {
					attributes = new IClasspathAttribute[] {
							JavaCore.newClasspathAttribute(IClasspathAttribute.TEST, TRUE) };
				} else {
					attributes = new IClasspathAttribute[0];
				}
				entries.add(JavaCore.newLibraryEntry(path, null, IPath.ROOT, new IAccessRule[0], attributes, false));
			}
		}
	}

	public static List<IClasspathEntry> getClasspathEntries(Project project, IWorkspaceRoot root) throws Exception {
		List<IClasspathEntry> entries = new ArrayList<>();
		Set<Project> mapped = new HashSet<>();
		for (Project dep : project.getBuildDependencies()) {
			File base = dep.getBase();
			@SuppressWarnings("deprecation")
			IContainer[] containers = root.findContainersForLocation(IPath.fromOSString(base.getAbsolutePath()));
			for (IContainer container : containers) {
				if (container instanceof IProject p) {
					entries.add(JavaCore.newProjectEntry(p.getFullPath()));
					mapped.add(dep);
				}
			}
		}
		publishContainerEntries(entries, project.getBootclasspath(), false, mapped);
		publishContainerEntries(entries, project.getBuildpath(), false, mapped);
		publishContainerEntries(entries, project.getClasspath(), false, mapped);
		publishContainerEntries(entries, project.getTestpath(), true, mapped);
		return entries;
	}

}
