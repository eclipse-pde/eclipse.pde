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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.project.IBundleClasspathEntry;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.core.project.IBundleProjectService;
import org.eclipse.pde.core.project.IPackageExportDescription;
import org.eclipse.pde.core.project.IRequiredBundleDescription;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.search.dependencies.GatherUnusedDependenciesOperation;
import org.eclipse.pde.ui.tests.runtime.TestUtils;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.eclipse.pde.ui.tests.util.TargetPlatformUtil;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.VersionRange;

public class GatherUnusedDependenciesOperationTest {

	@ClassRule
	public static final TestRule RESTORE_TARGET_DEFINITION = TargetPlatformUtil.RESTORE_CURRENT_TARGET_DEFINITION_AFTER;
	@ClassRule
	public static final TestRule CLEAR_WORKSPACE = ProjectUtils.DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER;

	@BeforeClass
	public static void setUpTargetPlatform() throws Exception {
		// The tests require bundles of the target platform to be present, so
		// that they must not depend on whatever target definition another test
		// class happens to have left behind.
		TargetPlatformUtil.setRunningPlatformAsTarget();
	}

	@Test
	public void testDirectlyUsedDependencyReexportedByOtherDependencyIsNotFlaggedAsUnused() throws Exception {
		// Bundle C: exports a package that is used directly by bundle B
		String bundleC = "bundle.c";
		String packageC = bundleC + ".pkg";
		IProject projectC = createJavaPluginProject(bundleC);
		addExportedPackage(projectC, packageC);
		createJavaSource(projectC, packageC, "C", """
				public class C {
				}
				""");

		// Bundle A: requires and reexports C, but is otherwise unrelated to B
		String bundleA = "bundle.a";
		IProject projectA = createManifestOnlyPluginProject(bundleA);
		addReexportedBundle(projectA, bundleC);

		// Bundle B: directly requires both A and C, and directly uses C's API
		String bundleB = "bundle.b";
		IProject projectB = createJavaPluginProject(bundleB);
		addRequiredBundle(projectB, bundleA);
		addRequiredBundle(projectB, bundleC);
		createJavaSource(projectB, bundleB, "UsesC", """
				public class UsesC {
					%s.C field;
				}
				""".formatted(packageC));

		buildProjects();
		List<String> unusedPlugins = gatherUnusedDependencies(projectB);
		assertFalse(
				"Direct, used dependency to bundle C must not be flagged as unused just because bundle A reexports it",
				unusedPlugins.contains(bundleC));
	}

	@Test
	public void testTargetBundleDependencyUsedOnlyInByteCodeIsNotFlaggedAsUnused() throws Exception {
		// Dependencies to bundles of the target platform are resolvable as jars
		// during the analysis, unlike those to bundles of the workspace, so that
		// they are subject to manifest calculation rules that do not apply here.
		// The referred type IEclipseContext of org.eclipse.e4.core.contexts is
		// only used as the return type of a method of another bundle, so that
		// the reference is present in the byte code but in no source file.
		String contextsBundle = "org.eclipse.e4.core.contexts";
		String bundle = "targetplatform.bundle";
		IProject project = createJavaPluginProject(bundle);
		addRequiredBundle(project, "org.eclipse.e4.ui.model.workbench");
		addRequiredBundle(project, "org.eclipse.e4.ui.workbench");
		addRequiredBundle(project, contextsBundle);
		createJavaSource(project, bundle, "UsesContext", """
				import org.eclipse.e4.ui.model.application.ui.basic.MPart;
				import org.eclipse.e4.ui.workbench.modeling.EModelService;

				public class UsesContext {
					public EModelService getModelService(MPart part) {
						return part.getContext().get(EModelService.class);
					}
				}
				""");

		buildProjects();
		List<String> unusedPlugins = gatherUnusedDependencies(project);
		assertFalse("Dependency to target platform bundle " + contextsBundle + " must not be flagged as unused "
				+ "although its type is only referred to by the byte code", unusedPlugins.contains(contextsBundle));
	}

	@Test
	public void testUnusedTargetBundleDependencyIsFlaggedAsUnused() throws Exception {
		// Counterpart to the used dependency to a target platform bundle: not
		// being able to tell the packages of such a bundle apart from those of
		// the other dependencies must not make every one of them appear as used
		String contextsBundle = "org.eclipse.e4.core.contexts";
		String bundle = "targetplatform.bundle.unused";
		IProject project = createJavaPluginProject(bundle);
		addRequiredBundle(project, contextsBundle);
		createJavaSource(project, bundle, "UsesNothing", """
				public class UsesNothing {
				}
				""");

		buildProjects();
		List<String> unusedPlugins = gatherUnusedDependencies(project);
		assertTrue("Unused dependency to target platform bundle " + contextsBundle + " must be flagged as unused",
				unusedPlugins.contains(contextsBundle));
	}

	private static IProject createManifestOnlyPluginProject(String symbolicName) throws Exception {
		IBundleProjectService service = acquireBundleProjectService();
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(symbolicName);
		IBundleProjectDescription description = service.getDescription(project);
		description.setSymbolicName(symbolicName);
		description.setNatureIds(new String[] { IBundleProjectDescription.PLUGIN_NATURE });
		description.apply(null);
		return project;
	}

	private static IProject createJavaPluginProject(String symbolicName) throws Exception {
		IBundleProjectService service = acquireBundleProjectService();
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(symbolicName);
		IBundleProjectDescription description = service.getDescription(project);
		description.setSymbolicName(symbolicName);
		description.setNatureIds(new String[] { IBundleProjectDescription.PLUGIN_NATURE, JavaCore.NATURE_ID });
		IBundleClasspathEntry classpathEntry = service.newBundleClasspathEntry(IPath.fromOSString("src"), null,
				IPath.fromOSString("."));
		description.setBundleClasspath(new IBundleClasspathEntry[] { classpathEntry });
		description.apply(null);
		return project;
	}

	private static void addExportedPackage(IProject project, String packageName) throws Exception {
		IBundleProjectService service = acquireBundleProjectService();
		IBundleProjectDescription description = service.getDescription(project);
		IPackageExportDescription[] presentExports = description.getPackageExports();
		IPackageExportDescription addedExport = service.newPackageExport(packageName, null, true, List.of());
		description.setPackageExports(Stream
				.concat(presentExports != null ? Arrays.stream(presentExports) : Stream.empty(), Stream.of(addedExport))
				.toArray(IPackageExportDescription[]::new));
		description.apply(null);
	}

	private static void addRequiredBundle(IProject project, String symbolicName) throws CoreException {
		addRequiredBundle(project, symbolicName, false);
	}

	private static void addReexportedBundle(IProject project, String symbolicName) throws CoreException {
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

	private static void createJavaSource(IProject project, String packageName, String typeName, String body)
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

	private static IBundleProjectService acquireBundleProjectService() {
		return PDECore.getDefault().acquireService(IBundleProjectService.class);
	}

	private static void buildProjects() throws CoreException {
		ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		boolean timedOut = TestUtils.waitForJobs(GatherUnusedDependenciesOperationTest.class.getName(), 100, 10000);
		assertFalse("Timed out waiting for the build to finish", timedOut);
		assertProjectsCompiledWithoutErrors();
	}

	/**
	 * The analysis derives the used dependencies from the byte code, so a
	 * project whose sources did not compile has no dependency that appears to
	 * be used. Without this check, every test that expects a dependency to be
	 * reported as unused would also pass if nothing had been analyzed at all,
	 * and a test that expects the opposite would fail with a message about the
	 * analysis instead of about the missing byte code.
	 */
	private static void assertProjectsCompiledWithoutErrors() throws CoreException {
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			IMarker[] markers = project.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true,
					IResource.DEPTH_INFINITE);
			List<String> errors = Arrays.stream(markers)
					.filter(marker -> marker.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR)
					.map(marker -> marker.getAttribute(IMarker.MESSAGE, "")).toList();
			assertEquals("Project " + project.getName() + " must compile without errors", List.of(), errors);
		}
	}

	private static List<String> gatherUnusedDependencies(IProject project)
			throws InvocationTargetException, InterruptedException {
		IPluginModelBase model = PluginRegistry.findModel(project);
		assertNotNull("Plug-in model for bundle " + project.getName() + " not found", model);

		GatherUnusedDependenciesOperation operation = new GatherUnusedDependenciesOperation(model);
		operation.run(new NullProgressMonitor());

		return operation.getList().stream().filter(IPluginImport.class::isInstance).map(IPluginImport.class::cast)
				.map(IPluginImport::getId).toList();
	}

}
