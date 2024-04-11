/*******************************************************************************
 * Copyright (c) 2020, 2023 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Mickael Istria - initial API and implementation
 *     Hannes Wellmann - Bug 577116: Improve test utility method reusability
 *******************************************************************************/
package org.eclipse.pde.core.tests.internal.classpath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.MinimalState;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.RequiredPluginsClasspathContainer;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.eclipse.pde.ui.tests.util.TargetPlatformUtil;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class ClasspathResolutionTest {

	@ClassRule
	public static final TestRule RESTORE_TARGET_DEFINITION = TargetPlatformUtil.RESTORE_CURRENT_TARGET_DEFINITION_AFTER;

	@ClassRule
	public static final TestRule CLEAR_WORKSPACE = ProjectUtils.DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER;
	@Rule
	public final TestRule deleteCreatedTestProjectsAfter = ProjectUtils.DELETE_CREATED_WORKSPACE_PROJECTS_AFTER;

	private static String jakartaAnnotationProviderBSN;

	@BeforeClass
	public static void getJavaxAnnotationProviderBSN() {
		Bundle bundle = FrameworkUtil.getBundle(jakarta.annotation.PostConstruct.class);
		jakartaAnnotationProviderBSN = bundle.getSymbolicName();
	}

	@Test
	public void testImportSystemPackageDoesntAddExtraBundleJava11() throws Exception {
		loadTargetPlatform("org.w3c.dom.events");
		IProject project = ProjectUtils.importTestProject("tests/projects/demoMissedSystemModulePackage");
		project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		IJavaProject javaProject = JavaCore.create(project);
		RequiredPluginsClasspathContainer container = new RequiredPluginsClasspathContainer(
				PDECore.getDefault().getModelManager().findModel(project), project);
		for (IClasspathEntry entry : container.getClasspathEntries()) {
			if (entry.getPath().lastSegment().contains("org.w3c.dom.events")) {
				fail(entry.getPath() + " erronesously present in container");
			}
		}
		for (IClasspathEntry entry : javaProject.getResolvedClasspath(false)) {
			if (entry.getPath().lastSegment().contains("org.w3c.dom.events")) {
				fail(entry.getPath() + " erronesously present in classpath");
			}
		}
		for (IMarker marker : project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE)) {
			if (marker.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR) {
				String message = marker.getAttribute(IMarker.MESSAGE, "");
				if (message.contains("org.w3c.dom is accessible from more than one module")) {
					fail(message);
				}
			}
		}
	}

	@Test
	public void testImportExternalPreviouslySystemPackageAddsExtraBundle() throws Exception {
		loadTargetPlatform(jakartaAnnotationProviderBSN);
		IProject project = ProjectUtils.importTestProject("tests/projects/demoMissedExternalPackage");
		// In Java 11, jakarta.annotation is not present, so the bundle *must*
		// be part of classpath
		List<String> classpathEntries = getRequiredPluginContainerEntries(project);
		assertThat(classpathEntries).anyMatch(filename -> filename.contains(jakartaAnnotationProviderBSN));
	}

	@Test
	public void testImportExternalPreviouslySystemPackageAddsExtraBundle_missingBREE() throws Exception {
		loadTargetPlatform(jakartaAnnotationProviderBSN);
		IProject project = ProjectUtils.importTestProject("tests/projects/demoMissedExternalPackageNoBREE");

		IExecutionEnvironment java11 = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment("JavaSE-11");
		assertThat(JavaRuntime.getVMInstall(JavaCore.create(project))).isIn(Arrays.asList(java11.getCompatibleVMs()));
		// In Java 11, jakarta.annotation is not present, so the bundle *must*
		// be part of classpath, even if no BREE is specified
		List<String> classpathEntries = getRequiredPluginContainerEntries(project);
		assertThat(classpathEntries).anyMatch(filename -> filename.contains(jakartaAnnotationProviderBSN));
	}

	@Test
	public void testImportSystemPackageDoesntAddExtraBundleJava8() throws Exception {
		loadTargetPlatform(jakartaAnnotationProviderBSN);
		try (var mocked = mockExtraExtraJRESystemPackages("JavaSE-1.8", List.of("jakarta.annotation"))) {
			IProject project = ProjectUtils.importTestProject("tests/projects/demoMissedSystemPackageJava8");
			// In Java 8, jakarta.annotation is present, so the bundle must
			// *NOT* be part of classpath
			List<String> classpathEntries = getRequiredPluginContainerEntries(project);
			assertThat(classpathEntries).noneMatch(filename -> filename.contains(jakartaAnnotationProviderBSN));
		}
	}

	@Test
	public void testImportSystemPackageDoesntAddExtraBundleJava8_osgiEERequirement() throws Exception {
		loadTargetPlatform(jakartaAnnotationProviderBSN);
		try (var mocked = mockExtraExtraJRESystemPackages("JavaSE-1.8", List.of("jakarta.annotation"))) {
			IProject project = ProjectUtils
					.importTestProject("tests/projects/demoMissedSystemPackageJava8OsgiEERequirement");
			// bundle is build with java 11, but declares java 8 requirement via
			// Require-Capability
			// --> jakarta.annotation bundle must not be on the classpath
			List<String> classpathEntries = getRequiredPluginContainerEntries(project);
			assertThat(classpathEntries).noneMatch(filename -> filename.contains(jakartaAnnotationProviderBSN));
		}
	}

	// --- utilitiy methods ---

	private List<String> getRequiredPluginContainerEntries(IProject project) throws CoreException {
		project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(project);
		IClasspathContainer requiredPluginsClasspathContainer = new RequiredPluginsClasspathContainer(model, project);
		return Arrays.stream(requiredPluginsClasspathContainer.getClasspathEntries()).map(IClasspathEntry::getPath)
				.map(IPath::lastSegment).toList();
	}

	private void loadTargetPlatform(String bundleName) throws Exception {
		Set<String> bundleNames = Set.of(bundleName, "org.eclipse.osgi");
		Predicate<Bundle> bundleFilter = b -> bundleNames.contains(b.getSymbolicName());
		TargetPlatformUtil.setRunningPlatformSubSetAsTarget("Target containing " + bundleName, bundleFilter);
	}

	private AutoCloseable mockExtraExtraJRESystemPackages(String eeId, List<String> extraPackages) {
		// We want to test that the packages provided by a Java-1.8 VM are
		// consumed from the VM and not from another bundle in the TP.
		// Unfortunately we don't have a Java-1.8 VM in the CI, it is always a
		// more recent one, but more recent ones don't contain the packages we
		// want anymore. Therefore we have to mock the TP system-properties and
		// pretend the desired package are contained in the VM for the purpose
		// of this test.

		IExecutionEnvironment ee = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment(eeId);
		Properties profileProps = ee.getProfileProperties();
		String mockedSystemPackages = TargetPlatformHelper.getSystemPackages(ee, profileProps)
				+ "," + String.join(",", extraPackages);

		var mockedPDEState = Mockito.mockStatic(TargetPlatformHelper.class, Mockito.CALLS_REAL_METHODS);
		mockedPDEState.when(() -> TargetPlatformHelper.getSystemPackages(ee, profileProps))
		.thenReturn(mockedSystemPackages);
		// Beware that the static mock is thread-local!
		MinimalState.reloadSystemPackagesIntoState();
		mockedPDEState.verify(() -> TargetPlatformHelper.getSystemPackages(ee, profileProps), Mockito.times(1));
		return () -> {
			mockedPDEState.close();
			MinimalState.reloadSystemPackagesIntoState();
		};
	}
}
