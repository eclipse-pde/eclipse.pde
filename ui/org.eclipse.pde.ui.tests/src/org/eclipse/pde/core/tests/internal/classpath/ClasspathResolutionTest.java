/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.pde.core.tests.internal.classpath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.RequiredPluginsClasspathContainer;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.junit.*;
import org.osgi.framework.ServiceReference;

public class ClasspathResolutionTest {

	private ITargetPlatformService tpService;
	private ITargetDefinition initialTarget;
	private IProject project;

	@Before
	public void setup() throws CoreException {
		ServiceReference<ITargetPlatformService> ref = PDECore.getDefault().getBundleContext()
				.getServiceReference(ITargetPlatformService.class);
		tpService = PDECore.getDefault().getBundleContext().getService(ref);
		initialTarget = tpService.getWorkspaceTargetDefinition();
		new LoadTargetDefinitionJob(initialTarget).runInWorkspace(new NullProgressMonitor());
	}

	@After
	public void tearDown() throws CoreException {
		new LoadTargetDefinitionJob(initialTarget).runInWorkspace(new NullProgressMonitor());
		if (project != null) {
			project.delete(false, false, null);
			project = null;
		}
	}

	@Test
	public void testImportSystemPackageDoesntAddExtraBundleJava11() throws Exception {
		loadTargetPlatform("org.w3c.dom.events");
		project = ProjectUtils.importTestProject("tests/projects/demoMissedSystemModulePackage");
		project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		IJavaProject javaProject = (IJavaProject) project.getNature(JavaCore.NATURE_ID);
		RequiredPluginsClasspathContainer container = new RequiredPluginsClasspathContainer(
				PDECore.getDefault().getModelManager().findModel(project));
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
		loadTargetPlatform("javax.annotation");
		project = ProjectUtils.importTestProject("tests/projects/demoMissedExternalPackage");
		project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		// In Java 11, javax.annotation is not present, so the bundle *must* be
		// part of classpath
		RequiredPluginsClasspathContainer container = new RequiredPluginsClasspathContainer(
				PDECore.getDefault().getModelManager().findModel(project));
		assertTrue("javax.annotation is missing from required bundle",
				Arrays.stream(container.getClasspathEntries()).map(IClasspathEntry::getPath).map(IPath::lastSegment)
				.anyMatch(fileName -> fileName.contains("javax.annotation")));
	}

	@Test
	public void testImportExternalPreviouslySystemPackageAddsExtraBundle_missingBREE() throws Exception {
		loadTargetPlatform("javax.annotation");
		project = ProjectUtils.importTestProject("tests/projects/demoMissedExternalPackageNoBREE");

		IExecutionEnvironment java11 = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment("JavaSE-11");
		assertThat(JavaRuntime.getVMInstall(JavaCore.create(project))).isIn(Arrays.asList(java11.getCompatibleVMs()));

		project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		// In Java 11, javax.annotation is not present, so the bundle *must* be
		// part of classpath, even if no BREE is specified
		RequiredPluginsClasspathContainer container = new RequiredPluginsClasspathContainer(
				PDECore.getDefault().getModelManager().findModel(project));
		assertTrue("javax.annotation is missing from required bundle",
				Arrays.stream(container.getClasspathEntries()).map(IClasspathEntry::getPath).map(IPath::lastSegment)
				.anyMatch(fileName -> fileName.contains("javax.annotation")));
	}

	@Test
	public void testImportSystemPackageDoesntAddExtraBundleJava8() throws Exception {
		loadTargetPlatform("javax.annotation");
		project = ProjectUtils.importTestProject("tests/projects/demoMissedSystemPackageJava8");
		project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		// In Java 8, javax.annotation is present, so the bundle must *NOT* be
		// part of classpath
		RequiredPluginsClasspathContainer container = new RequiredPluginsClasspathContainer(
				PDECore.getDefault().getModelManager().findModel(project));
		assertTrue("javax.annotations shouldn't be present in required bundles",
				Arrays.stream(container.getClasspathEntries()).map(IClasspathEntry::getPath).map(IPath::lastSegment)
				.noneMatch(fileName -> fileName.contains("javax.annotation")));
	}

	@Test
	public void testImportSystemPackageDoesntAddExtraBundleJava8_osgiEERequirement() throws Exception {
		loadTargetPlatform("javax.annotation");
		project = ProjectUtils.importTestProject("tests/projects/demoMissedSystemPackageJava8OsgiEERequirement");
		project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		// bundle is build with java 11, but declares java 8 requirement via
		// Require-Capability
		// --> javax.annotation bundle must not be on the classpath
		RequiredPluginsClasspathContainer container = new RequiredPluginsClasspathContainer(
				PDECore.getDefault().getModelManager().findModel(project));
		assertTrue("javax.annotations shouldn't be present in required bundles",
				Arrays.stream(container.getClasspathEntries()).map(IClasspathEntry::getPath).map(IPath::lastSegment)
				.noneMatch(fileName -> fileName.contains("javax.annotation")));
	}

	private void loadTargetPlatform(String bundleName) throws Exception {
		ITargetDefinition javaxXmlTarget = tpService.newTarget();
		javaxXmlTarget.setName("Target containing " + bundleName);
		Collection<String> bundleNames = List.of(bundleName, "org.eclipse.osgi");
		Set<File> directories = new HashSet<>(2);
		for (String name : bundleNames) {
			directories.add(FileLocator.getBundleFile(Platform.getBundle(name)).getParentFile());
		}
		javaxXmlTarget.setTargetLocations(directories.stream().map(File::getAbsolutePath)
				.map(tpService::newDirectoryLocation).toArray(ITargetLocation[]::new));
		javaxXmlTarget.setIncluded(bundleNames.stream().map(name -> new NameVersionDescriptor(name, null))
				.toArray(NameVersionDescriptor[]::new));
		new LoadTargetDefinitionJob(javaxXmlTarget).runInWorkspace(new NullProgressMonitor());
	}
}
