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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.assertj.core.api.SoftAssertionsProvider.ThrowingRunnable;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
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

@RunWith(Parameterized.class)
public class JUnitServiceLoaderTest {

	@ClassRule
	public static final TestRule CLEAR_WORKSPACE = ProjectUtils.DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER;

	@BeforeClass
	public static void setupProjects() throws Exception {
		Bundle bundle = FrameworkUtil.getBundle(JUnitServiceLoaderTest.class);
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		for (URL resource : Collections.list(bundle.findEntries("test-bundles", "serviceloader.tests.*", false))) {
			ProjectUtils.importTestProject(FileLocator.toFileURL(resource));
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
		return new Object[][] {
				{ "JUnit6", getJProject("serviceloader.tests.junit6"), "JUnit6Test",
						getJProject("serviceloader.tests.engine.junit6"), "DummyJUnit6TestEngine" },
				{ "JUnit5", getJProject("serviceloader.tests.junit5"), "JUnit5Test",
						getJProject("serviceloader.tests.engine.junit5"), "DummyJUnit5TestEngine" } };
	}

	@Parameter(0)
	public String testCaseName; // Just for display
	@Parameter(1)
	public IJavaProject testProject;
	@Parameter(2)
	public String testClassName;
	@Parameter(3)
	public IJavaProject engineProject;
	@Parameter(4)
	public String engineClassName;

	/**
	 * Asserts that the correct custom test engine implementation was loaded for a
	 * started JUnit5/6 test run by listening to the log outputs produced by either
	 * {@literal DummyJUnit6TestEngine} or {@literal DummyJUnit5TestEngine} upon
	 * instantiation.
	 */
	@Test
	public void testCustomEngineLoad() throws Exception {
		IType testClass = findType(testProject, testClassName);
		List<String> logs = executeWithOutputLogging(() -> TestExecutionUtil.runTest(testClass));
		assertThat(logs).anyMatch(log -> log.trim().equals(String.format("Called %s", engineClassName)));
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
}
