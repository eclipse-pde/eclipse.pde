/*******************************************************************************
 * Copyright (c) 2026 Andrey Loskutov <loskutov@gmx.de> and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Andrey Loskutov <loskutov@gmx.de> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.tests.internal.classpath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ClasspathComputer;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.ui.tests.runtime.TestUtils;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

/**
 * Regression test for classpath resolution of plugin projects. Tests that only
 * the expected bundles are on the classpath, and that errors are reported for
 * missing packages from not accessible bundles, but not for missing packages.
 *
 * See https://github.com/eclipse-pde/eclipse.pde/issues/2244.
 */
public class ClasspathResolutionTest2 {

	@ClassRule
	public static final TestRule CLEAR_WORKSPACE = ProjectUtils.DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER;

	@Rule
	public final TestRule deleteCreatedTestProjectsAfter = ProjectUtils.DELETE_CREATED_WORKSPACE_PROJECTS_AFTER;

	private static IProject projectA;

	private static IClasspathEntry[] classpathEntriesA;

	static final List<String> expectedAccessibleBundles = List.of("B", "G");
	static final List<String> expectedApiPackages = apiPackagesFor(expectedAccessibleBundles);
	static final List<String> expectedInternalPackages = internalPackagesFor(expectedAccessibleBundles);

	static final List<String> notAccessibleBundles = List.of("C", "D", "E", "F", "H");
	static final List<String> notAccessibleApiPackages = apiPackagesFor(notAccessibleBundles);
	static final List<String> notAccessibleInternalPackages = internalPackagesFor(notAccessibleBundles);

	static final List<String> allOtherProjects = Stream
			.concat(expectedAccessibleBundles.stream(), notAccessibleBundles.stream()).sorted().toList();

	@BeforeClass
	public static void setupBeforeClass() throws Exception {
		List<IProject> importedProjects = new ArrayList<>();

		for (String name : allOtherProjects) {
			IProject project = ProjectUtils.importTestProject("tests/projects/" + name);
			importedProjects.add(project);
		}

		// Build all projects in reversed order to ensure that dependencies are
		// built before dependents
		for (IProject project : importedProjects.reversed()) {
			project.open(new NullProgressMonitor());
			project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		}
		TestUtils.processUIEvents(100);

		// Now import and build project A, which depends on all other projects.
		projectA = ProjectUtils.importTestProject("tests/projects/A");
		projectA.open(new NullProgressMonitor());
		projectA.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

		IPluginModelBase modelA = PDECore.getDefault().getModelManager().findModel(projectA);
		classpathEntriesA = ClasspathComputer.computeClasspathEntries(modelA, projectA);

		TestUtils.processUIEvents(100);
	}

	/**
	 * Check that the classpath of plugin A contains exactly the expected
	 * bundles. Checks that "missing type" compilation errors are reported for
	 * all references form not accessible bundles. This bundle classpath is
	 * computed by RequiredPluginsClasspathContainer.
	 */
	@Test
	public void testRequiredPluginsClasspathContainerContract() throws Exception {
		// Check every project except A - they should build without errors
		List<IProject> otherProjects = allOtherProjects.stream().map(ClasspathResolutionTest2::getProject).toList();
		for (IProject project : otherProjects) {
			IMarker[] markers = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
			for (IMarker marker : markers) {
				if (marker.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR) {
					fail("Unexpected error in project " + project.getName() + ": "
							+ marker.getAttribute(IMarker.MESSAGE, ""));
				}
			}
		}

		// Check that project A has errors, and that all errors are related to
		// missing packages from not accessible bundles, and that no error is
		// related to missing packages from expected accessible bundles
		IMarker[] markers = projectA.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);

		List<String> errorMessages = Arrays.asList(markers).stream()
				.filter(marker -> marker.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR)
				.map(marker -> marker.getAttribute(IMarker.MESSAGE, "")).toList();

		for (String message : errorMessages) {
			// Check that no error is related to missing packages from expected
			// accessible bundles
			boolean isRelatedToExpectedAccessibleBundle = false;
			for (String bundle : expectedAccessibleBundles) {
				String pack = bundle.toLowerCase();
				if (message.contains(pack + " cannot be resolved to a type")) {
					isRelatedToExpectedAccessibleBundle = true;
					break;
				}
			}
			assertFalse("Unexpected error in project A: " + message, isRelatedToExpectedAccessibleBundle);

			// and that all errors are related to missing packages from not
			// accessible bundles
			boolean isRelatedToNotAccessibleBundle = false;
			for (String bundle : notAccessibleBundles) {
				String pack = bundle.toLowerCase();
				if (message.contains(pack + " cannot be resolved to a type")) {
					isRelatedToNotAccessibleBundle = true;
					break;
				}
			}
			assertTrue("Unexpected error in project A: " + message, isRelatedToNotAccessibleBundle);
		}

		// There must be at least one error, otherwise the test would not be
		// meaningful
		assertFalse("Expected errors in project A, but found none!", errorMessages.isEmpty());

		// Check that all expected accessible bundles are on the classpath, and
		// only those
		List<String> projectNames = Arrays.asList(classpathEntriesA).stream()
				.map(entry -> entry.getPath().lastSegment()).toList();

		assertThat(projectNames).containsExactlyInAnyOrderElementsOf(expectedAccessibleBundles);

		// Same check using the API of PDECore and ClasspathComputer instead
		List<String> classpathEntries = getRequiredPluginContainerEntries(projectA);
		assertThat(classpathEntries).containsExactlyElementsOf(expectedAccessibleBundles);
	}

	static IProject getProject(String name) {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(name);
	}

	private static List<String> apiPackagesFor(List<String> expectedAccessibleBundles) {
		return expectedAccessibleBundles.stream().flatMap(bundle -> List.of(bundle + ".api").stream()).toList();
	}

	private static List<String> internalPackagesFor(List<String> expectedAccessibleBundles) {
		return expectedAccessibleBundles.stream().flatMap(bundle -> List.of(bundle + ".internal").stream()).toList();
	}

	private List<String> getRequiredPluginContainerEntries(IProject project) throws CoreException {
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(project);
		IClasspathEntry[] computeClasspathEntries = ClasspathComputer.computeClasspathEntries(model, project);
		return Arrays.stream(computeClasspathEntries).map(IClasspathEntry::getPath).map(IPath::lastSegment).toList();
	}
}