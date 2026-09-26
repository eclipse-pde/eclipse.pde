/*******************************************************************************
 *  Copyright (c) 2026 Vector Informatik GmbH and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.pde.ui.tests.search.dependencies;

import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.project.IBundleClasspathEntry;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.core.project.IBundleProjectService;
import org.eclipse.pde.core.project.IPackageExportDescription;
import org.eclipse.pde.core.project.IPackageImportDescription;
import org.eclipse.pde.core.project.IRequiredBundleDescription;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageObject;
import org.eclipse.pde.internal.ui.search.dependencies.GatherUnusedDependenciesOperation;
import org.eclipse.pde.ui.tests.runtime.TestUtils;
import org.osgi.framework.VersionRange;

/**
 * Creates the plug-in projects that the tests of the unused dependency analysis
 * operate on and runs that analysis on them.
 */
public final class GatherUnusedDependenciesTestUtils {

	private GatherUnusedDependenciesTestUtils() { // static utility
	}

	public static IProject createManifestOnlyPluginProject(String symbolicName) throws Exception {
		IBundleProjectService service = acquireBundleProjectService();
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(symbolicName);
		IBundleProjectDescription description = service.getDescription(project);
		description.setSymbolicName(symbolicName);
		description.setNatureIds(new String[] { IBundleProjectDescription.PLUGIN_NATURE });
		description.apply(null);
		return project;
	}

	public static IProject createJavaPluginProject(String symbolicName) throws Exception {
		return createJavaPluginProject(symbolicName, null);
	}

	public static IProject createJavaPluginProject(String symbolicName, String hostSymbolicName) throws Exception {
		IBundleProjectService service = acquireBundleProjectService();
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(symbolicName);
		IBundleProjectDescription description = service.getDescription(project);
		description.setSymbolicName(symbolicName);
		if (hostSymbolicName != null) {
			description.setHost(service.newHost(hostSymbolicName, (VersionRange) null));
		}
		description.setNatureIds(new String[] { IBundleProjectDescription.PLUGIN_NATURE, JavaCore.NATURE_ID });
		IBundleClasspathEntry classpathEntry = service.newBundleClasspathEntry(IPath.fromOSString("src"), null,
				IPath.fromOSString("."));
		description.setBundleClasspath(new IBundleClasspathEntry[] { classpathEntry });
		description.apply(null);
		return project;
	}

	public static void addExportedPackage(IProject project, String packageName) throws Exception {
		IBundleProjectService service = acquireBundleProjectService();
		IBundleProjectDescription description = service.getDescription(project);
		IPackageExportDescription[] presentExports = description.getPackageExports();
		IPackageExportDescription addedExport = service.newPackageExport(packageName, null, true, List.of());
		description.setPackageExports(Stream
				.concat(presentExports != null ? Arrays.stream(presentExports) : Stream.empty(), Stream.of(addedExport))
				.toArray(IPackageExportDescription[]::new));
		description.apply(null);
	}

	public static void addImportedPackage(IProject project, String packageName) throws CoreException {
		IBundleProjectService service = acquireBundleProjectService();
		IBundleProjectDescription description = service.getDescription(project);
		IPackageImportDescription[] presentImports = description.getPackageImports();
		IPackageImportDescription addedImport = service.newPackageImport(packageName, (VersionRange) null, false);
		description.setPackageImports(Stream
				.concat(presentImports != null ? Arrays.stream(presentImports) : Stream.empty(), Stream.of(addedImport))
				.toArray(IPackageImportDescription[]::new));
		description.apply(null);
	}

	public static void addRequiredBundle(IProject project, String symbolicName) throws CoreException {
		addRequiredBundle(project, symbolicName, false);
	}

	public static void addReexportedBundle(IProject project, String symbolicName) throws CoreException {
		addRequiredBundle(project, symbolicName, true);
	}

	private static void addRequiredBundle(IProject project, String symbolicName, boolean reexported)
			throws CoreException {
		IBundleProjectService service = acquireBundleProjectService();
		IBundleProjectDescription description = service.getDescription(project);
		IRequiredBundleDescription[] presentBundles = description.getRequiredBundles();
		IRequiredBundleDescription addedBundle = service.newRequiredBundle(symbolicName, (VersionRange) null, false,
				reexported);
		description.setRequiredBundles(Stream
				.concat(presentBundles != null ? Arrays.stream(presentBundles) : Stream.empty(), Stream.of(addedBundle))
				.toArray(IRequiredBundleDescription[]::new));
		description.apply(null);
	}

	public static void createJavaSource(IProject project, String packageName, String typeName, String body)
			throws CoreException {
		IPath packagePath = IPath.fromOSString("src").append(packageName.replace('.', '/'));
		IFolder packageFolder = project.getFolder(packagePath);
		if (!packageFolder.exists()) {
			IFolder parent = project.getFolder(IPath.fromOSString("src"));
			for (String segment : packageName.split("\\.")) {
				parent = parent.getFolder(segment);
				if (!parent.exists()) {
					parent.create(true, true, null);
				}
			}
		}
		IFile javaFile = packageFolder.getFile(typeName + ".java");
		String content = """
				package %s;

				%s""".formatted(packageName, body);
		javaFile.create(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), true, null);
	}

	public static void createFile(IProject project, IPath path, String content) throws CoreException {
		IContainer container = project;
		for (String segment : path.removeLastSegments(1).segments()) {
			IFolder folder = container.getFolder(IPath.fromOSString(segment));
			if (!folder.exists()) {
				folder.create(true, true, null);
			}
			container = folder;
		}
		IFile file = container.getFile(IPath.fromOSString(path.lastSegment()));
		file.create(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), true, null);
	}

	private static IBundleProjectService acquireBundleProjectService() {
		return PDECore.getDefault().acquireService(IBundleProjectService.class);
	}

	public static void buildProjects() throws CoreException {
		ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		TestUtils.waitForJobs(GatherUnusedDependenciesTestUtils.class.getName(), 100, 10000);
	}

	public static List<String> gatherUnusedDependencies(IProject project)
			throws InvocationTargetException, InterruptedException {
		return gatherUnusedElements(project).stream().filter(IPluginImport.class::isInstance)
				.map(IPluginImport.class::cast).map(IPluginImport::getId).toList();
	}

	public static List<String> gatherUnusedPackageImports(IProject project)
			throws InvocationTargetException, InterruptedException {
		return gatherUnusedElements(project).stream().filter(ImportPackageObject.class::isInstance)
				.map(ImportPackageObject.class::cast).map(ImportPackageObject::getName).toList();
	}

	public static List<Object> gatherUnusedElements(IProject project)
			throws InvocationTargetException, InterruptedException {
		IPluginModelBase model = PluginRegistry.findModel(project);
		assertNotNull("Plug-in model for bundle " + project.getName() + " not found", model);

		GatherUnusedDependenciesOperation operation = new GatherUnusedDependenciesOperation(model);
		operation.run(new NullProgressMonitor());

		return operation.getList();
	}

}
