/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.ui.tests.dependencies;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.ui.search.dependencies.GatherUnusedDependenciesOperation;
import org.eclipse.pde.ui.tests.project.ProjectCreationTests;
import org.eclipse.pde.ui.tests.runtime.TestUtils;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

public class GatherUnusedDependenciesOperationTest {

	@ClassRule
	public static final TestRule clearWorkspace = ProjectUtils.DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER;

	@Rule
	public final TestRule clearCreatedProjects = ProjectUtils.DELETE_CREATED_WORKSPACE_PROJECTS_AFTER;

	@Test
	@SuppressWarnings("removal")
	public void testFindUnusedDependency() throws Exception {
		// Create a project that depends on org.eclipse.core.runtime but doesn't use it
		IProject project = ProjectUtils.createPluginProject("test.unused.dep", "test.unused.dep", "1.0.0", (description, service) -> {
			// Add an unused dependency
			org.eclipse.pde.core.project.IRequiredBundleDescription req = service.newRequiredBundle("org.eclipse.core.runtime", null, false, false);
			description.setRequiredBundles(new org.eclipse.pde.core.project.IRequiredBundleDescription[] { req });
			description.setExecutionEnvironments(new String[] { "JavaSE-11" });
		});

		IPluginModelBase model = PluginRegistry.findModel(project);

		GatherUnusedDependenciesOperation operation = new GatherUnusedDependenciesOperation(model);
		operation.run(new NullProgressMonitor());

		List<Object> unused = operation.getList();

		Assert.assertNotNull("Result list should not be null", unused);
		Assert.assertFalse("Should find unused dependencies", unused.isEmpty());

		boolean found = false;
		for (Object obj : unused) {
			if (obj instanceof IPluginImport) {
				if (((IPluginImport)obj).getId().equals("org.eclipse.core.runtime")) {
					found = true;
					break;
				}
			}
		}
		Assert.assertTrue("Should find org.eclipse.core.runtime as unused", found);
	}

	@Test
	@SuppressWarnings("removal")
	public void testUsedDependencyNotReported() throws Exception {
		IJavaProject javaProject = ProjectUtils.createPluginProject("test.used.dep", (IExecutionEnvironment) null);
		IProject project = javaProject.getProject();

		// Add dependency using BundleProjectService to ensure classpath is updated
		org.eclipse.pde.core.project.IBundleProjectService service = ProjectCreationTests.getBundleProjectService();
		org.eclipse.pde.core.project.IBundleProjectDescription description = service.getDescription(project);
		org.eclipse.pde.core.project.IRequiredBundleDescription req = service.newRequiredBundle("org.eclipse.core.runtime", null, false, false);
		description.setRequiredBundles(new org.eclipse.pde.core.project.IRequiredBundleDescription[] { req });
		description.apply(new NullProgressMonitor());

		// Add a class that uses org.eclipse.core.runtime
		String packageName = "test.used.dep";
		IFolder folder = project.getFolder(new Path("src").append(packageName.replace('.', '/')));
		createFolder(folder);

		IFile sourceFile = folder.getFile("Activator.java");
		String content = "package test.used.dep;\n" +
				"import org.eclipse.core.runtime.Plugin;\n" +
				"public class Activator extends Plugin {\n" +
				"}";
		if (!sourceFile.exists()) {
			sourceFile.create(new ByteArrayInputStream(content.getBytes()), true, null);
		}

		project.build(IncrementalProjectBuilder.FULL_BUILD, null);
		TestUtils.waitForJobs("Build", 100, 1000);

		IMarker[] markers = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
		StringBuilder problems = new StringBuilder();
		for (IMarker marker : markers) {
			if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR) {
				problems.append(marker.getAttribute(IMarker.MESSAGE, "")).append("\n");
			}
		}
		Assert.assertEquals("Project has build errors:\n" + problems.toString(), 0, problems.length());

		IPluginModelBase model = PluginRegistry.findModel(project);

		GatherUnusedDependenciesOperation operation = new GatherUnusedDependenciesOperation(model);
		operation.run(new NullProgressMonitor());

		List<Object> unused = operation.getList();

		boolean found = false;
		for (Object obj : unused) {
			if (obj instanceof IPluginImport) {
				if (((IPluginImport)obj).getId().equals("org.eclipse.core.runtime")) {
					found = true;
					break;
				}
			}
		}
		Assert.assertFalse("Should NOT report org.eclipse.core.runtime as unused", found);
	}

	@Test
	@SuppressWarnings("removal")
	public void testMinimizeRemovesMandatoryIfOptionalReexports() throws Exception {
		// 1. Create BundleB
		IJavaProject javaProjectB = ProjectUtils.createPluginProject("BundleB", (IExecutionEnvironment) null);
		IProject projectB = javaProjectB.getProject();
		org.eclipse.pde.core.project.IBundleProjectService service = ProjectCreationTests.getBundleProjectService();

		org.eclipse.pde.core.project.IBundleProjectDescription descB = service.getDescription(projectB);
		org.eclipse.pde.core.project.IPackageExportDescription exportB = service.newPackageExport("b", null, true, List.of());
		descB.setPackageExports(new org.eclipse.pde.core.project.IPackageExportDescription[] { exportB });
		org.eclipse.pde.core.project.IRequiredBundleDescription reqOsgiB = service.newRequiredBundle("org.eclipse.osgi", null, false, false);
		descB.setRequiredBundles(new org.eclipse.pde.core.project.IRequiredBundleDescription[] { reqOsgiB });
		descB.apply(new NullProgressMonitor());

		createClass(projectB, "b", "ClassB", "public class ClassB {}");
		projectB.build(IncrementalProjectBuilder.FULL_BUILD, null);

		// 2. Create BundleA (re-exports BundleB)
		IJavaProject javaProjectA = ProjectUtils.createPluginProject("BundleA", (IExecutionEnvironment) null);
		IProject projectA = javaProjectA.getProject();

		org.eclipse.pde.core.project.IBundleProjectDescription descA = service.getDescription(projectA);
		org.eclipse.pde.core.project.IPackageExportDescription exportA = service.newPackageExport("a", null, true, List.of());
		descA.setPackageExports(new org.eclipse.pde.core.project.IPackageExportDescription[] { exportA });

		org.eclipse.pde.core.project.IRequiredBundleDescription reqB_inA = service.newRequiredBundle("BundleB", null, false, true); // Re-export
		org.eclipse.pde.core.project.IRequiredBundleDescription reqOsgiA = service.newRequiredBundle("org.eclipse.osgi", null, false, false);
		descA.setRequiredBundles(new org.eclipse.pde.core.project.IRequiredBundleDescription[] { reqB_inA, reqOsgiA });
		descA.apply(new NullProgressMonitor());

		createClass(projectA, "a", "ClassA", "public class ClassA {}");
		projectA.build(IncrementalProjectBuilder.FULL_BUILD, null);

		// 3. Create Test Project
		IJavaProject javaProject = ProjectUtils.createPluginProject("test.minimize.dep", (IExecutionEnvironment) null);
		IProject project = javaProject.getProject();

		org.eclipse.pde.core.project.IBundleProjectDescription description = service.getDescription(project);

		// BundleA (Optional) re-exports BundleB
		org.eclipse.pde.core.project.IRequiredBundleDescription reqA = service.newRequiredBundle("BundleA", null, true, false);
		// BundleB (Mandatory)
		org.eclipse.pde.core.project.IRequiredBundleDescription reqB = service.newRequiredBundle("BundleB", null, false, false);

		org.eclipse.pde.core.project.IRequiredBundleDescription reqOsgi = service.newRequiredBundle("org.eclipse.osgi", null, false, false);
		org.eclipse.pde.core.project.IRequiredBundleDescription reqCore = service.newRequiredBundle("org.eclipse.core.runtime", null, false, false);

		description.setRequiredBundles(new org.eclipse.pde.core.project.IRequiredBundleDescription[] { reqA, reqB, reqOsgi, reqCore });
		description.apply(new NullProgressMonitor());

		// Add code using both
		createClass(project, "test", "Client", "package test; import a.ClassA; import b.ClassB; public class Client { ClassA a; ClassB b; }");

		project.build(IncrementalProjectBuilder.FULL_BUILD, null);
		TestUtils.waitForJobs("Build", 100, 1000);

		// Verify no build errors
		IMarker[] markers = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
		StringBuilder problems = new StringBuilder();
		for (IMarker marker : markers) {
			if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR) {
				problems.append(marker.getAttribute(IMarker.MESSAGE, "")).append("\n");
			}
		}
		Assert.assertEquals("Project has build errors:\n" + problems.toString(), 0, problems.length());

		IPluginModelBase model = PluginRegistry.findModel(project);

		GatherUnusedDependenciesOperation operation = new GatherUnusedDependenciesOperation(model);
		operation.run(new NullProgressMonitor());

		List<Object> unused = operation.getList();

		boolean found = false;
		for (Object obj : unused) {
			if (obj instanceof IPluginImport) {
				if (((IPluginImport)obj).getId().equals("BundleB")) {
					found = true;
					break;
				}
			}
		}
		Assert.assertFalse("Should NOT remove mandatory BundleB even if optional BundleA re-exports it", found);
	}

	private void createClass(IProject project, String packageName, String className, String content) throws CoreException {
		IFolder folder = project.getFolder(new Path("src").append(packageName.replace('.', '/')));
		createFolder(folder);
		IFile file = folder.getFile(className + ".java");
		if (!file.exists()) {
			file.create(new ByteArrayInputStream(content.getBytes()), true, null);
		}
	}

	private void createFolder(IFolder folder) throws CoreException {
		if (!folder.exists()) {
			IContainer parent = folder.getParent();
			if (parent instanceof IFolder) {
				createFolder((IFolder) parent);
			}
			folder.create(true, true, null);
		}
	}
}
