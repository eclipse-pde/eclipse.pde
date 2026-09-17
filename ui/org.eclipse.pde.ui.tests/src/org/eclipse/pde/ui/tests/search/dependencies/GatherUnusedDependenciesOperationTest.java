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
import org.eclipse.pde.core.project.IPackageImportDescription;
import org.eclipse.pde.core.project.IRequiredBundleDescription;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageObject;
import org.eclipse.pde.internal.ui.search.dependencies.GatherUnusedDependenciesOperation;
import org.eclipse.pde.ui.tests.runtime.TestUtils;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.VersionRange;

public class GatherUnusedDependenciesOperationTest {

	@ClassRule
	public static final TestRule CLEAR_WORKSPACE = ProjectUtils.DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER;

	/** Keeps the projects of one test case from interfering with the others. */
	@Rule
	public final TestRule clearCreatedProjects = ProjectUtils.DELETE_CREATED_WORKSPACE_PROJECTS_AFTER;

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
	public void testDependencyUsedFromPackageProvidedByBothBundlesIsNotFlaggedAsUnused() throws Exception {
		// Bundle A: exports a package that bundle B provides as well
		String bundleA = "common.bundle.a";
		String commonPackage = "common.pkg";
		IProject projectA = createJavaPluginProject(bundleA);
		addExportedPackage(projectA, commonPackage);
		createJavaSource(projectA, commonPackage, "A", """
				public class A {
				}
				""");

		// Bundle B: requires A and uses its API from within the common package,
		// so that the reference requires no package import at all
		String bundleB = "common.bundle.b";
		IProject projectB = createJavaPluginProject(bundleB);
		addRequiredBundle(projectB, bundleA);
		createJavaSource(projectB, commonPackage, "B", """
				public class B {
					A field;
				}
				""");

		buildProjects();
		List<String> unusedPlugins = gatherUnusedDependencies(projectB);
		assertFalse("Dependency to bundle A must not be flagged as unused although it is only used from a package "
				+ "that bundle B provides as well", unusedPlugins.contains(bundleA));
	}

	@Test
	public void testUnusedDependencyProvidingPackageProvidedByBothBundlesIsNotFlaggedAsUnused() throws Exception {
		// Bundle A: exports a package that bundle B provides as well
		String bundleA = "unused.bundle.a";
		String commonPackage = "unused.pkg";
		IProject projectA = createJavaPluginProject(bundleA);
		addExportedPackage(projectA, commonPackage);
		createJavaSource(projectA, commonPackage, "A", """
				public class A {
				}
				""");

		// Bundle B: requires A but only uses its own types of the common
		// package, so that A is not required although the package is. Whether a
		// type of that package comes from A or from B is invisible in the
		// computed packages, so the dependency is deliberately retained.
		String bundleB = "unused.bundle.b";
		IProject projectB = createJavaPluginProject(bundleB);
		addRequiredBundle(projectB, bundleA);
		createJavaSource(projectB, commonPackage, "B1", """
				public class B1 {
				}
				""");
		createJavaSource(projectB, commonPackage, "B2", """
				public class B2 {
					B1 field;
				}
				""");

		buildProjects();
		List<String> unusedPlugins = gatherUnusedDependencies(projectB);
		assertFalse("Dependency to bundle A must not be flagged as unused although it is in fact unused: this "
				+ "imprecision is accepted in exchange for never proposing to remove a dependency that is needed "
				+ "because it is used from a package that bundle B provides as well",
				unusedPlugins.contains(bundleA));
	}

	@Test
	public void testDependencyUsedViaInlinedConstantInPackageProvidedByBothBundlesIsNotFlaggedAsUnused()
			throws Exception {
		// Bundle A: exports a package that bundle B provides as well and that
		// contains a compile-time constant
		String bundleA = "constant.bundle.a";
		String commonPackage = "constant.pkg";
		IProject projectA = createJavaPluginProject(bundleA);
		addExportedPackage(projectA, commonPackage);
		createJavaSource(projectA, commonPackage, "A", """
				public class A {
					public static final String NAME = "name";
				}
				""");

		// Bundle B: requires A and uses only its constant, whose value the
		// compiler inlines, so that the byte code retains no reference to A at
		// all. Removing the dependency would thus break the compilation of B.
		String bundleB = "constant.bundle.b";
		IProject projectB = createJavaPluginProject(bundleB);
		addRequiredBundle(projectB, bundleA);
		createJavaSource(projectB, commonPackage, "B", """
				public class B {
					public String name() {
						return A.NAME;
					}
				}
				""");

		buildProjects();
		List<String> unusedPlugins = gatherUnusedDependencies(projectB);
		assertFalse("Dependency to bundle A must not be flagged as unused although its inlined constant is used",
				unusedPlugins.contains(bundleA));
	}

	@Test
	public void testDependencyUsedViaSecondaryTypeInPackageProvidedByBothBundlesIsNotFlaggedAsUnused()
			throws Exception {
		// Bundle A: exports a package that bundle B provides as well and that
		// contains a secondary type next to the primary one
		String bundleA = "secondary.bundle.a";
		String commonPackage = "secondary.pkg";
		IProject projectA = createJavaPluginProject(bundleA);
		addExportedPackage(projectA, commonPackage);
		createJavaSource(projectA, commonPackage, "A", """
				public class A {
				}

				class SecondaryA {
				}
				""");

		// Bundle B: requires A and uses only its secondary type. The compiler
		// permits that because it only considers the package name, although the
		// separate class loaders would not permit it at runtime. The dependency
		// is thus required to compile bundle B and must not be reported.
		String bundleB = "secondary.bundle.b";
		IProject projectB = createJavaPluginProject(bundleB);
		addRequiredBundle(projectB, bundleA);
		createJavaSource(projectB, commonPackage, "B", """
				public class B {
					SecondaryA field;
				}
				""");

		buildProjects();
		List<String> unusedPlugins = gatherUnusedDependencies(projectB);
		assertFalse("Dependency to bundle A must not be flagged as unused although only its secondary type is used",
				unusedPlugins.contains(bundleA));
	}

	@Test
	public void testDependencyUsedViaNestedTypeInPackageProvidedByBothBundlesIsNotFlaggedAsUnused() throws Exception {
		// Bundle A: exports a package that bundle B provides as well and that
		// contains a nested type
		String bundleA = "nested.bundle.a";
		String commonPackage = "nested.pkg";
		IProject projectA = createJavaPluginProject(bundleA);
		addExportedPackage(projectA, commonPackage);
		createJavaSource(projectA, commonPackage, "A", """
				public class A {
					public static class Nested {
					}
				}
				""");

		// Bundle B: requires A and uses only its nested type, which the byte
		// code refers to as A$Nested while the Java model provides A
		String bundleB = "nested.bundle.b";
		IProject projectB = createJavaPluginProject(bundleB);
		addRequiredBundle(projectB, bundleA);
		createJavaSource(projectB, commonPackage, "B", """
				public class B {
					A.Nested field;
				}
				""");

		buildProjects();
		List<String> unusedPlugins = gatherUnusedDependencies(projectB);
		assertFalse("Dependency to bundle A must not be flagged as unused although only its nested type is used",
				unusedPlugins.contains(bundleA));
	}

	@Test
	public void testDependencyUsedViaMethodReturnTypeInPackageProvidedByBothBundlesIsNotFlaggedAsUnused()
			throws Exception {
		// Bundle A: exports a package that bundle C provides as well
		String bundleA = "returntype.bundle.a";
		String commonPackage = "returntype.pkg";
		IProject projectA = createJavaPluginProject(bundleA);
		addExportedPackage(projectA, commonPackage);
		createJavaSource(projectA, commonPackage, "A", """
				public class A {
					public void use() {
					}
				}
				""");

		// Bundle B: exports a method returning the type of bundle A
		String bundleB = "returntype.bundle.b";
		String packageB = bundleB + ".pkg";
		IProject projectB = createJavaPluginProject(bundleB);
		addExportedPackage(projectB, packageB);
		addRequiredBundle(projectB, bundleA);
		createJavaSource(projectB, packageB, "B", """
				public class B {
					public %s.A provide() {
						return null;
					}
				}
				""".formatted(commonPackage));

		// Bundle C: uses A's type only as the return type of B's method and
		// from within the package that it provides as well, so that the usage
		// appears neither in the sources nor in the computed packages
		String bundleC = "returntype.bundle.c";
		IProject projectC = createJavaPluginProject(bundleC);
		addRequiredBundle(projectC, bundleA);
		addRequiredBundle(projectC, bundleB);
		createJavaSource(projectC, commonPackage, "C", """
				public class C {
					public void use() {
						new %s.B().provide().use();
					}
				}
				""".formatted(packageB));

		buildProjects();
		List<String> unusedPlugins = gatherUnusedDependencies(projectC);
		assertFalse("Dependency to bundle A must not be flagged as unused although its type is only used as the "
				+ "return type of a method of bundle B", unusedPlugins.contains(bundleA));
	}

	@Test
	public void testDependencyProvidingUsedTypeOnlyInNonExportedPackageIsFlaggedAsUnused() throws Exception {
		// Bundle A: exports the common package and provides the used type
		String bundleA = "internal.bundle.a";
		String commonPackage = "internal.pkg";
		IProject projectA = createJavaPluginProject(bundleA);
		addExportedPackage(projectA, commonPackage);
		createJavaSource(projectA, commonPackage, "A", """
				public class A {
				}
				""");

		// Bundle B: provides a type of the same name in the same package, but
		// does not export that package, so that its type cannot be referred to
		String bundleB = "internal.bundle.b";
		IProject projectB = createJavaPluginProject(bundleB);
		createJavaSource(projectB, commonPackage, "A", """
				public class A {
				}
				""");

		// Bundle C: requires both and uses the type of A from within the common
		// package. A is required before B, so that the type is resolved from the
		// bundle that exports the package instead of yielding an access
		// restriction.
		String bundleC = "internal.bundle.c";
		IProject projectC = createJavaPluginProject(bundleC);
		addRequiredBundle(projectC, bundleA);
		addRequiredBundle(projectC, bundleB);
		createJavaSource(projectC, commonPackage, "C", """
				public class C {
					A field;
				}
				""");

		buildProjects();
		List<String> unusedPlugins = gatherUnusedDependencies(projectC);
		assertTrue("Unused dependency to bundle B must be flagged as unused although it provides a type of the same "
				+ "name in the same package as the used bundle A", unusedPlugins.contains(bundleB));
		assertFalse("Dependency to bundle A must not be flagged as unused as it provides the used type",
				unusedPlugins.contains(bundleA));
	}

	@Test
	public void testUnusedDependencyExportingSuperPackageOfOwnPackageIsFlaggedAsUnused() throws Exception {
		// Bundle A: exports a package of which bundle B provides a subpackage
		String bundleA = "superpackage.bundle.a";
		String exportedPackage = "superpackage.pkg";
		IProject projectA = createJavaPluginProject(bundleA);
		addExportedPackage(projectA, exportedPackage);
		createJavaSource(projectA, exportedPackage, "A", """
				public class A {
				}
				""");

		// Bundle B: requires A but provides only a subpackage of the package
		// that A exports, which is an unrelated package and thus no reason to
		// consider A used
		String bundleB = "superpackage.bundle.b";
		IProject projectB = createJavaPluginProject(bundleB);
		addRequiredBundle(projectB, bundleA);
		createJavaSource(projectB, exportedPackage + ".sub", "B", """
				public class B {
				}
				""");

		buildProjects();
		List<String> unusedPlugins = gatherUnusedDependencies(projectB);
		assertTrue("Unused dependency to bundle A must be flagged as unused although bundle B provides a subpackage "
				+ "of the package that bundle A exports", unusedPlugins.contains(bundleA));
	}

	@Test
	public void testUnusedImportedPackageOfWhichOwnPackageIsSubpackageIsFlaggedAsUnused() throws Exception {
		// Bundle A: exports the package that bundle B imports
		String bundleA = "importedsuperpackage.bundle.a";
		String importedPackage = "importedsuperpackage.pkg";
		IProject projectA = createJavaPluginProject(bundleA);
		addExportedPackage(projectA, importedPackage);
		createJavaSource(projectA, importedPackage, "A", """
				public class A {
				}
				""");

		// Bundle B: imports that package without using it and provides only a
		// subpackage of it, which is an unrelated package and thus no reason to
		// consider the import used
		String bundleB = "importedsuperpackage.bundle.b";
		IProject projectB = createJavaPluginProject(bundleB);
		addImportedPackage(projectB, importedPackage);
		createJavaSource(projectB, importedPackage + ".sub", "B", """
				public class B {
				}
				""");

		buildProjects();
		List<String> unusedPackages = gatherUnusedPackageImports(projectB);
		assertTrue("Unused imported package must be flagged as unused although bundle B provides a subpackage of it",
				unusedPackages.contains(importedPackage));
	}

	@Test
	public void testImportedPackageUsedFromPackageProvidedByBothBundlesIsNotFlaggedAsUnused() throws Exception {
		// Bundle A: exports a package that bundle B provides as well
		String bundleA = "importedcommon.bundle.a";
		String commonPackage = "importedcommon.pkg";
		IProject projectA = createJavaPluginProject(bundleA);
		addExportedPackage(projectA, commonPackage);
		createJavaSource(projectA, commonPackage, "A", """
				public class A {
				}
				""");

		// Bundle B: imports the common package and uses its API from within its
		// own part of that package, so that the reference requires no import
		// statement and is thus invisible in the computed packages
		String bundleB = "importedcommon.bundle.b";
		IProject projectB = createJavaPluginProject(bundleB);
		addImportedPackage(projectB, commonPackage);
		createJavaSource(projectB, commonPackage, "B", """
				public class B {
					A field;
				}
				""");

		buildProjects();
		List<String> unusedPackages = gatherUnusedPackageImports(projectB);
		assertFalse("Imported package must not be flagged as unused although it is only used from the part of it "
				+ "that bundle B provides itself", unusedPackages.contains(commonPackage));
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

	private static void addImportedPackage(IProject project, String packageName) throws CoreException {
		IBundleProjectService service = acquireBundleProjectService();
		IBundleProjectDescription description = service.getDescription(project);
		IPackageImportDescription[] presentImports = description.getPackageImports();
		IPackageImportDescription addedImport = service.newPackageImport(packageName, (VersionRange) null, false);
		description.setPackageImports(Stream
				.concat(presentImports != null ? Arrays.stream(presentImports) : Stream.empty(), Stream.of(addedImport))
				.toArray(IPackageImportDescription[]::new));
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
		// wait for the classpath containers to pick up the bundle dependencies
		// set up by the test before building, as otherwise the build may run
		// against a stale classpath and fail to compile
		TestUtils.waitForJobs(GatherUnusedDependenciesOperationTest.class.getName(), 100, 10000);
		ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		TestUtils.waitForJobs(GatherUnusedDependenciesOperationTest.class.getName(), 100, 10000);
		// the analysis is based on the compiled classes, so a test that does not
		// compile would not test what it is supposed to test
		IMarker[] markers = ResourcesPlugin.getWorkspace().getRoot()
				.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
		List<String> errors = Arrays.stream(markers)
				.filter(marker -> marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR)
				.map(marker -> marker.getResource().getFullPath() + ": " + marker.getAttribute(IMarker.MESSAGE, ""))
				.toList();
		assertEquals("The projects of the test must compile without errors", List.of(), errors);
	}

	private static List<String> gatherUnusedDependencies(IProject project)
			throws InvocationTargetException, InterruptedException {
		return gatherUnusedElements(project).stream().filter(IPluginImport.class::isInstance)
				.map(IPluginImport.class::cast).map(IPluginImport::getId).toList();
	}

	private static List<String> gatherUnusedPackageImports(IProject project)
			throws InvocationTargetException, InterruptedException {
		return gatherUnusedElements(project).stream().filter(ImportPackageObject.class::isInstance)
				.map(ImportPackageObject.class::cast).map(ImportPackageObject::getName).toList();
	}

	private static List<Object> gatherUnusedElements(IProject project)
			throws InvocationTargetException, InterruptedException {
		IPluginModelBase model = PluginRegistry.findModel(project);
		assertNotNull("Plug-in model for bundle " + project.getName() + " not found", model);

		GatherUnusedDependenciesOperation operation = new GatherUnusedDependenciesOperation(model);
		operation.run(new NullProgressMonitor());

		return operation.getList();
	}

}
