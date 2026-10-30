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
package org.eclipse.pde.junit.runtime.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.assertj.core.api.SoftAssertionsProvider.ThrowingRunnable;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.core.project.IBundleProjectService;
import org.eclipse.pde.core.project.IRequiredBundleDescription;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.ui.tests.runtime.TestUtils;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.VersionRange;

@RunWith(Parameterized.class)
public class JUnitServiceLoaderTest {

	@ClassRule
	public static final TestRule CLEAR_WORKSPACE = ProjectUtils.DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER;

	@BeforeClass
	public static void setupProjects() throws Exception {
		Bundle bundle = FrameworkUtil.getBundle(JUnitServiceLoaderTest.class);
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		for (URL resource : Collections.list(bundle.findEntries("test-bundles", "serviceloader.tests.*", false))) {
			Path copy = copyIntoWorkspace(URIUtil.toFile(URIUtil.toURI(FileLocator.toFileURL(resource))).toPath());
			importProject(IPath.fromPath(copy));
		}
		TestUtils.waitForJobs(JUnitServiceLoaderTest.class + ".setupProjects() before build", 100, 10_000);
		workspaceRoot.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
		TestExecutionUtil.waitForAutoBuild();
		TestUtils.waitForJobs(JUnitServiceLoaderTest.class + ".setupProjects() after build", 100, 10_000);
	}

	static IJavaProject getJProject(String projectName) {
		return JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getProject(projectName));
	}

	@Parameters(name = "{0}")
	public static Object[][] parameters() {
		return new Object[][] { //
				{ "JUnit6 TestEngine", //
						getJProject("serviceloader.tests.junit6"), "JUnit6Test", //
						getJProject("serviceloader.tests.engine.junit6"), "DummyJUnit6TestEngine" //
				}, //
				{ "JUnit5 TestEngine", //
						getJProject("serviceloader.tests.junit5"), "JUnit5Test", //
						getJProject("serviceloader.tests.engine.junit5"), "DummyJUnit5TestEngine" //
				}, //
				{ "JUnit6 LauncherDiscoveryListener", //
						getJProject("serviceloader.tests.junit6"), "JUnit6Test", //
						getJProject("serviceloader.tests.launcherdiscoverylistener.junit6"),
						"DummyJUnit6LauncherDiscoveryListener" //
				}, //
				{ "JUnit5 LauncherDiscoveryListener", //
						getJProject("serviceloader.tests.junit5"), "JUnit5Test", //
						getJProject("serviceloader.tests.launcherdiscoverylistener.junit5"),
						"DummyJUnit5LauncherDiscoveryListener" //
				}, //
				{ "JUnit6 LauncherInterceptor", //
						getJProject("serviceloader.tests.junit6"), "JUnit6Test", //
						getJProject("serviceloader.tests.launcherinterceptor.junit6"), "DummyJUnit6LauncherInterceptor" //
				}, //
				{ "JUnit5 LauncherInterceptor", //
						getJProject("serviceloader.tests.junit5"), "JUnit5Test", //
						getJProject("serviceloader.tests.launcherinterceptor.junit5"), "DummyJUnit5LauncherInterceptor" //
				}, //
				{ "JUnit6 LauncherSessionListener", //
						getJProject("serviceloader.tests.junit6"), "JUnit6Test", //
						getJProject("serviceloader.tests.launchersessionlistener.junit6"),
						"DummyJUnit6LauncherSessionListener" //
				}, //
				{ "JUnit5 LauncherSessionListener", //
						getJProject("serviceloader.tests.junit5"), "JUnit5Test", //
						getJProject("serviceloader.tests.launchersessionlistener.junit5"),
						"DummyJUnit5LauncherSessionListener" //
				}, //
				{ "JUnit6 PostDiscoveryFilter", //
						getJProject("serviceloader.tests.junit6"), "JUnit6Test", //
						getJProject("serviceloader.tests.postdiscoveryfilter.junit6"), "DummyJUnit6PostDiscoveryFilter" //
				}, //
				{ "JUnit5 PostDiscoveryFilter", //
						getJProject("serviceloader.tests.junit5"), "JUnit5Test", //
						getJProject("serviceloader.tests.postdiscoveryfilter.junit5"), "DummyJUnit5PostDiscoveryFilter" //
				}, //
				{ "JUnit6 TestExecutionListener", //
						getJProject("serviceloader.tests.junit6"), "JUnit6Test", //
						getJProject("serviceloader.tests.testexecutionlistener.junit6"),
						"DummyJUnit6TestExecutionListener" //
				}, //
				{ "JUnit5 TestExecutionListener", //
						getJProject("serviceloader.tests.junit5"), "JUnit5Test", //
						getJProject("serviceloader.tests.testexecutionlistener.junit5"),
						"DummyJUnit5TestExecutionListener" //
				}, //
		};
	}

	@Parameter(0)
	public String testCaseName; // Just for display
	@Parameter(1)
	public IJavaProject testProject;
	@Parameter(2)
	public String testClassName;
	@Parameter(3)
	public IJavaProject serviceProject;
	@Parameter(4)
	public String serviceImplementationClassName;

	/**
	 * Asserts that the correct JUnit service implementation was loaded for a
	 * started JUnit5/6 test run by listening to the log outputs produced by
	 * corresponding service implementation (inside the
	 * {@literal test-bundles/serviceloader.tests.*} bundles) upon instantiation.
	 */
	@Test
	public void testCustomServiceImplementationsDiscovered() throws Exception {
		IBundleProjectService bundleProjectService = PDECore.getDefault().acquireService(IBundleProjectService.class);
		IBundleProjectDescription testBundleDescription = bundleProjectService.getDescription(testProject.getProject());
		IRequiredBundleDescription[] oldRequiredBundles = addRequiredBundle(bundleProjectService, testBundleDescription,
				serviceProject.getElementName());
		try {
			TestUtils.waitForJobs("Updating required bundles...", 500, 5000);
			ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

			IType testClass = findType(testProject, testClassName);
			List<String> logs = executeWithOutputLogging(() -> {
				// LauncherInterceptors are only loaded if explicitly enabled
				TestExecutionUtil.runTest(testClass, "-Djunit.platform.launcher.interceptors.enabled=true");
			});
			assertThat(logs.stream().map(String::trim))
					.anyMatch(String.format("Called %s", serviceImplementationClassName)::equals);
		} finally {
			testBundleDescription.setRequiredBundles(oldRequiredBundles);
			testBundleDescription.apply(null);
			TestUtils.waitForJobs("Updating required bundles...", 500, 5000);
		}
	}

	static IRequiredBundleDescription[] addRequiredBundle(IBundleProjectService bundleProjectService,
			IBundleProjectDescription bundleDescription, String newRequiredBundleName) throws CoreException {
		IRequiredBundleDescription[] oldRequiredBundles = bundleDescription.getRequiredBundles();
		IRequiredBundleDescription[] newRequiredBundles = new IRequiredBundleDescription[oldRequiredBundles.length + 1];
		System.arraycopy(oldRequiredBundles, 0, newRequiredBundles, 0, oldRequiredBundles.length);
		newRequiredBundles[newRequiredBundles.length - 1] = bundleProjectService
				.newRequiredBundle(newRequiredBundleName, (VersionRange) null, false, false);
		bundleDescription.setRequiredBundles(newRequiredBundles);
		bundleDescription.apply(null);
		return oldRequiredBundles;
	}

	static List<String> executeWithOutputLogging(ThrowingRunnable runnable) throws Exception {
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ProcessOutputListener listener = new ProcessOutputListener();
		launchManager.addLaunchListener(listener);
		try {
			runnable.run();
		} finally {
			launchManager.removeLaunchListener(listener);
		}
		return listener.outputLogs;
	}

	static IType findType(IJavaProject project, String name) throws JavaModelException {
		IType type = project.findType(project.getElementName(), name);
		assertNotNull(type);
		Assert.assertTrue(type.exists());
		return type;
	}

	static class ProcessOutputListener implements ILaunchListener {

		List<String> outputLogs = new ArrayList<String>();

		@Override
		public void launchRemoved(ILaunch launch) {
		}

		@Override
		public void launchAdded(ILaunch launch) {
		}

		@Override
		public void launchChanged(ILaunch launch) {
			for (IProcess process : launch.getProcesses()) {
				process.getStreamsProxy().getOutputStreamMonitor().addListener((text, monitor) -> {
					outputLogs.add(text);
				});
			}
		}
	}

	static Path copyIntoWorkspace(Path source) throws IOException {
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		Path target = workspaceRoot.getLocation().toPath().resolve(source.getFileName());
		Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				Path targetdir = target.resolve(source.relativize(dir));
				Files.createDirectories(targetdir);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Path targetFile = target.resolve(source.relativize(file));
				Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
				return FileVisitResult.CONTINUE;
			}
		});
		return target;
	}

	static IProject importProject(IPath path) throws CoreException {
		IPath projectFile = path.append(IProjectDescription.DESCRIPTION_FILE_NAME);
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProjectDescription projectDescription = workspace.loadProjectDescription(projectFile);
		IProject project = workspace.getRoot().getProject(projectDescription.getName());
		project.create(projectDescription, null);
		project.open(null);
		return project;
	}
}
