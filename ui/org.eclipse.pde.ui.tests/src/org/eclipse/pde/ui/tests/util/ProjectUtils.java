/*******************************************************************************
 * Copyright (c) 2008, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Hannes Wellmann - Bug 577629 - Unify project creation/deletion in tests
 *******************************************************************************/
package org.eclipse.pde.ui.tests.util;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.eclipse.pde.internal.ui.wizards.IProjectProvider;
import org.eclipse.pde.internal.ui.wizards.plugin.NewProjectCreationOperation;
import org.eclipse.pde.internal.ui.wizards.plugin.PluginFieldData;
import org.eclipse.pde.ui.IBundleContentWizard;
import org.eclipse.pde.ui.templates.AbstractNewPluginTemplateWizard;
import org.eclipse.pde.ui.templates.ITemplateSection;
import org.eclipse.pde.ui.tests.project.ProjectCreationTests;
import org.eclipse.pde.ui.tests.runtime.TestUtils;
import org.junit.rules.TestRule;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;

/**
 * Utility class for project related operations
 */
public class ProjectUtils {

	/**
	 * Used to create projects
	 */
	static class TestProjectProvider implements IProjectProvider {
		private String fProjectName;

		TestProjectProvider(String projectName) {
			fProjectName = projectName;
		}

		@Override
		public IPath getLocationPath() {
			return ResourcesPlugin.getWorkspace().getRoot().getLocation();
		}

		@Override
		public IProject getProject() {
			return ResourcesPlugin.getWorkspace().getRoot().getProject(fProjectName);
		}

		@Override
		public String getProjectName() {
			return fProjectName;
		}

	}

	/**
	 * Fake wizard
	 */
	static class TestBundleWizard extends AbstractNewPluginTemplateWizard {

		@Override
		protected void addAdditionalPages() {
		}

		@Override
		public ITemplateSection[] getTemplateSections() {
			return new ITemplateSection[0];
		}

	}

	/**
	 * Constant representing the name of the output directory for a project.
	 * Value is: <code>bin</code>
	 */
	public static final String BIN_FOLDER = "bin";

	/**
	 * Constant representing the name of the source directory for a project.
	 * Value is: <code>src</code>
	 */
	public static final String SRC_FOLDER = "src";

	/**
	 * Create a plugin project with the given name and execution environment.
	 *
	 * @param projectName
	 * @param env
	 *            environment for build path or <code>null</code> if default
	 *            system JRE
	 * @return a new plugin project
	 * @throws CoreException
	 */
	public static IJavaProject createPluginProject(String projectName, IExecutionEnvironment env) throws Exception {
		PluginFieldData data = new PluginFieldData();
		data.setName(projectName);
		data.setId(projectName);
		data.setLegacy(false);
		data.setHasBundleStructure(true);
		data.setSimple(false);
		data.setProvider("IBM");
		data.setLibraryName(".");
		data.setVersion("1.0.0");
		data.setTargetVersion("3.5");
		data.setOutputFolderName(BIN_FOLDER);
		data.setSourceFolderName(SRC_FOLDER);
		if (env != null) {
			data.setExecutionEnvironment(env.getId());
		}
		data.setDoGenerateClass(true);
		data.setClassname(projectName + ".Activator");
		data.setEnableAPITooling(false);
		data.setRCPApplicationPlugin(false);
		data.setUIPlugin(false);
		IProjectProvider provider = new TestProjectProvider(projectName);
		IBundleContentWizard wizard = new TestBundleWizard();
		NewProjectCreationOperation operation = new NewProjectCreationOperation(data, provider, wizard);
		operation.run(new NullProgressMonitor());
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		IJavaProject javaProject = JavaCore.create(project);
		TestUtils.waitForJobs("ProjectUtils.createPluginProject " + projectName, 100, 10000);
		return javaProject;
	}

	private static final Set<IProject> IMPORTED_PROJECTS = ConcurrentHashMap.newKeySet();

	public static IProject importTestProject(String path) throws IOException, CoreException {
		URL entry = FileLocator.toFileURL(FrameworkUtil.getBundle(ProjectUtils.class).getEntry(path));
		if (entry == null) {
			throw new IllegalArgumentException(path + " does not exist");
		}
		IPath projectFile = Path.fromPortableString(entry.getPath()).append(IProjectDescription.DESCRIPTION_FILE_NAME);
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProjectDescription projectDescription = workspace.loadProjectDescription(projectFile);
		IProject project = workspace.getRoot().getProject(projectDescription.getName());
		project.create(projectDescription, null);
		project.open(null);
		IMPORTED_PROJECTS.add(project);
		return project;
	}

	public static List<IProject> createWorkspacePluginProjects(List<NameVersionDescriptor> workspacePlugins)
			throws CoreException {
		List<IProject> projects = new ArrayList<>();
		for (NameVersionDescriptor pluginDescription : workspacePlugins) {
			String bundleSymbolicName = pluginDescription.getId();
			String bundleVersion = pluginDescription.getVersion();
			String projectName = bundleSymbolicName + bundleVersion.replace('.', '_');
			projects.add(createPluginProject(projectName, bundleSymbolicName, bundleVersion));
		}
		return projects;
	}

	public static IProject createPluginProject(String projectName, String bundleSymbolicName, String version)
			throws CoreException {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		IBundleProjectDescription description = ProjectCreationTests.getBundleProjectService().getDescription(project);
		description.setSymbolicName(bundleSymbolicName);
		if (version != null) {
			description.setBundleVersion(Version.parseVersion(version));
		}
		description.apply(null);
		return project;
	}

	/**
	 * An (intended) {@link org.junit.ClassRule} that deletes all projects from
	 * the test-workspace before and after all tests are executed.
	 * <p>
	 * The intention is to ensure that the workspace is empty before the first
	 * method (static or not) of a test-class is called as well it is cleared
	 * after the last method has returned.
	 * </p>
	 * <p>
	 * For projects that are imported via {@link #importTestProject(String)} the
	 * content is not deleted, for other projects the content is deleted. This
	 * assumes that imported projects are resources in this Test-plugin while
	 * other projects are virtual and can safely be deleted.
	 * </p>
	 */
	public static final TestRule DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER = TestUtils.getThrowingTestRule( //
			() -> { // Clean-up garbage of other test-classes
				ProjectUtils.deleteWorkspaceProjects(Set.of());
				return null;
			}, o -> deleteWorkspaceProjects(Set.of()));

	/**
	 * An (intended) {@link org.junit.Rule} that deletes the projects from the
	 * test-workspace , that where created during the test-case execution, after
	 * each test-case.
	 * <p>
	 * The intention is to delete those projects from the workspace that were
	 * created during the execution of a test-case, but to retain those that
	 * existed before (e.g. were created in a static initializer).
	 * </p>
	 * <p>
	 * For projects that are imported via {@link #importTestProject(String)} the
	 * content is not deleted, for other projects the content is deleted. This
	 * assumes that imported projects are resources in this Test-plugin while
	 * other projects are virtual and can safely be deleted.
	 * </p>
	 */
	public static final TestRule DELETE_CREATED_WORKSPACE_PROJECTS_AFTER = TestUtils.getThrowingTestRule( //
			() -> Set.of(ResourcesPlugin.getWorkspace().getRoot().getProjects()), //
			projectsBefore -> deleteWorkspaceProjects(projectsBefore));

	private static void deleteWorkspaceProjects(Set<IProject> retainedProjects) throws CoreException {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : projects) {
			if (!retainedProjects.contains(project)) {
				boolean isImportedProject = IMPORTED_PROJECTS.remove(project);
				// Don't delete content of imported projects because they are
				// resources of this test plug-in and must not be deleted
				project.delete(!isImportedProject, true, null);
			}
		}
		// back-up, should not change anything if everything is properly used
		IMPORTED_PROJECTS.retainAll(retainedProjects);
	}
}
