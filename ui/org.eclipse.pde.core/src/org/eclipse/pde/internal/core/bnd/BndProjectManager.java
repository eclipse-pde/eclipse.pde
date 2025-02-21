/*******************************************************************************
 *  Copyright (c) 2023, 2024 Christoph Läubrich and others.
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
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
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
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.annotations.OSGiAnnotationsClasspathContributor;
import org.eclipse.pde.internal.core.natures.BndProject;
import org.eclipse.pde.internal.core.natures.PluginProject;

import aQute.bnd.build.Container;
import aQute.bnd.build.Container.TYPE;
import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import biz.aQute.resolve.Bndrun;

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
		if (PluginProject.isJavaProject(project)) {
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
		Stream<String> enhnacedBuildPath = OSGiAnnotationsClasspathContributor.annotations();
		if (buildPath != null) {
			enhnacedBuildPath = Stream.concat(Stream.of(buildPath), enhnacedBuildPath);
		}
		bnd.setProperty(Constants.BUILDPATH, enhnacedBuildPath.collect(Collectors.joining(DELIMITER)));
	}

	static synchronized Workspace getWorkspace() throws Exception {
		if (workspace == null) {
			try (Processor run = new Processor()) {
				run.setProperty(Constants.STANDALONE, TRUE);
				IPath path = PDECore.getDefault().getStateLocation().append(Constants.DEFAULT_BND_EXTENSION)
						.append(Project.BNDCNF);
				File file = path.toFile();
				file.mkdirs();
				workspace = Workspace.createStandaloneWorkspace(run, file.toURI());
				workspace.setBase(file.getParentFile());
				workspace.setBuildDir(file);
				workspace.set("workspaceName", Messages.BndProjectManager_WorkspaceName); //$NON-NLS-1$
				workspace.set("workspaceDescription", Messages.BndProjectManager_WorkspaceDescription); //$NON-NLS-1$
				workspace.addBasicPlugin(TargetRepository.getTargetRepository());
				workspace.addBasicPlugin(new JobProgress());
				workspace.addBasicPlugin(new SupplierClipboard(() -> PDECore.getDefault().getClipboardPlugin()));
				workspace.addBasicPlugin(
						new DelegateRepositoryListener(() -> PDECore.getDefault().getRepositoryListenerPlugins()));
				workspace.refresh();
			}
		}
		return workspace;
	}

	static void publishContainerEntries(List<IClasspathEntry> entries, Collection<Container> containers, boolean isTest,
			Set<Project> mapped) {
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

	public static Optional<Bndrun> createBndrun(IProject project) throws Exception {
		if (BndProject.isBndProject(project) || PluginProject.isPluginProject(project)) {
			IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(project);
			if (model == null) {
				return Optional.empty();
			}
			Workspace workspace = getWorkspace();
			Path base = workspace.getBase().toPath();
			String name = project.getName();
			Path file = Files.createTempFile(base, name, Constants.DEFAULT_BNDRUN_EXTENSION);
			file.toFile().deleteOnExit();
			Files.createDirectories(file.getParent());
			Properties properties = new Properties();
			BundleDescription description = model.getBundleDescription();
			properties.setProperty(Constants.RUNREQUIRES, String.format("bnd.identity;id=%s;version=%s", //$NON-NLS-1$
					description.getSymbolicName(), description.getVersion()));
			try (OutputStream stream = Files.newOutputStream(file)) {
				properties.store(stream,
						String.format("Bndrun generated by PDE for project %s", name)); //$NON-NLS-1$
			}
			Bndrun bndrun = new PdeBndrun(workspace, file.toFile(), String.format("Bndrun for %s", name)); //$NON-NLS-1$
			bndrun.addClose(new AutoCloseable() {

				@Override
				public void close() throws Exception {
					Files.delete(file);
				}
			});
			return Optional.of(bndrun);
		}
		return Optional.empty();
	}

}
