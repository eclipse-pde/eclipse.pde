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
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.assertj.core.api.SoftAssertionsProvider.ThrowingRunnable;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.eclipse.pde.launching.IPDELauncherConstants;
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
	 * Asserts that the correct service implementation was loaded for a started
	 * JUnit5/6 test run by listening to the log outputs produced by the
	 * corresponding service upon instantiation.
	 */
	@Test
	public void testCustomServiceImplementationsDiscovered() throws Exception {
		TestUtils.waitForJobs("Updating required bundles...", 500, 5000);
		ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

		IType testClass = findType(testProject, testClassName);
		List<String> logs = executeWithOutputLogging(() -> {
			TestExecutionUtil.runTest(testClass, launchConfiguration -> {
				addToSelectedBundles(launchConfiguration, serviceProject.getProject());
				// LauncherInterceptors are only loaded if explicitly enabled
				addVmArg(launchConfiguration, "-Djunit.platform.launcher.interceptors.enabled=true");
			});
		}).stream().map(String::trim).collect(Collectors.toList());
		assertThat(logs).anyMatch(getLogEntryForServiceInstantiation(serviceImplementationClassName)::equals);

		// Assert that none of the other service implementations was loaded
		Set<String> otherServiceImplementationClassNames = new HashSet<>();
		for (Object[] parameters : parameters()) {
			otherServiceImplementationClassNames.add((String) parameters[4]);
		}
		assertTrue("wrong parameter", otherServiceImplementationClassNames.remove(serviceImplementationClassName));
		for (String otherServiceImplementationClassName : otherServiceImplementationClassNames) {
			assertThat(logs).noneMatch(getLogEntryForServiceInstantiation(otherServiceImplementationClassName)::equals);
		}
	}

	static void addToSelectedBundles(ILaunchConfigurationWorkingCopy launchConfiguration, IProject project)
			throws CoreException {
		Set<String> bundles = launchConfiguration.getAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES,
				Collections.emptySet());
		IPluginModelBase model = PluginRegistry.findModel(project);
		IPluginBase pluginBase = model.getPluginBase();

		StringBuilder builder = new StringBuilder();
		builder.append(pluginBase.getId());
		builder.append(BundleLauncherHelper.VERSION_SEPARATOR);
		builder.append(pluginBase.getVersion());

		bundles.add(builder.toString());
		launchConfiguration.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES, bundles);
	}

	static void addVmArg(ILaunchConfigurationWorkingCopy launchConfiguration, String vmArg) throws CoreException {
		String existingVmArgs = launchConfiguration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS,
				"");
		launchConfiguration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS,
				existingVmArgs.isBlank() ? vmArg : existingVmArgs + " " + vmArg);
	}

	static String getLogEntryForServiceInstantiation(String serviceImplementationClassName) {
		return String.format("Called %s", serviceImplementationClassName);
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
