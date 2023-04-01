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

import aQute.bnd.build.Container;
import aQute.bnd.build.Container.TYPE;
import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.annotations.OSGiAnnotationsClasspathContributor;
import org.eclipse.pde.internal.core.natures.BndProject;

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
					Project bnd = new Project(getWorkspace(), projectFolder);
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
		String buildPath = bnd.getProperty(Constants.BUILDPATH);
		Stream<String> enhnacedBuildPath = OSGiAnnotationsClasspathContributor.annotations()
				.map(p -> p.getPluginBase().getId());
		if (buildPath != null) {
			enhnacedBuildPath = Stream.concat(Stream.of(buildPath), enhnacedBuildPath);
		}
		bnd.setProperty(Constants.BUILDPATH, enhnacedBuildPath.collect(Collectors.joining(DELIMITER)));
	}

	static synchronized Workspace getWorkspace() throws Exception {
		Processor run = new Processor();
		run.setProperty(Constants.STANDALONE, TRUE);
		IPath path = PDECore.getDefault().getStateLocation().append(Project.BNDCNF);
		if (workspace == null) {
			workspace = Workspace.createStandaloneWorkspace(run, path.toFile().toURI());
			workspace.addBasicPlugin(TargetRepository.getTargetRepository());
			workspace.refresh();
		}
		return workspace;
	}

	static void publishContainerEntries(List<IClasspathEntry> entries, Collection<Container> containers,
			boolean isTest) {
		for (Container container : containers) {
			TYPE type = container.getType();
			if (type == TYPE.PROJECT) {
				continue;
			}
			File file = container.getFile();
			if (file.exists()) {
				Path path = new Path(file.getAbsolutePath());
				IClasspathAttribute[] attributes;
				if (isTest) {
					attributes = new IClasspathAttribute[] {
							JavaCore.newClasspathAttribute(IClasspathAttribute.TEST, TRUE) };
				} else {
					attributes = new IClasspathAttribute[0];
				}
				entries.add(JavaCore.newLibraryEntry(path, null, Path.ROOT, new IAccessRule[0], attributes, false));
			}
		}
	}

	public static List<IClasspathEntry> getClasspathEntries(Project project, IWorkspaceRoot root) throws Exception {
		List<IClasspathEntry> entries = new ArrayList<>();
		for (Project dep : project.getBuildDependencies()) {
			File base = dep.getBase();
			@SuppressWarnings("deprecation")
			IContainer[] containers = root.findContainersForLocation(new Path(base.getAbsolutePath()));
			for (IContainer container : containers) {
				if (container instanceof IProject) {
					IProject p = (IProject) container;
					entries.add(JavaCore.newProjectEntry(p.getFullPath()));
				}
			}
		}
		publishContainerEntries(entries, project.getBootclasspath(), false);
		publishContainerEntries(entries, project.getBuildpath(), false);
		publishContainerEntries(entries, project.getClasspath(), false);
		publishContainerEntries(entries, project.getTestpath(), true);
		return entries;
	}

}
