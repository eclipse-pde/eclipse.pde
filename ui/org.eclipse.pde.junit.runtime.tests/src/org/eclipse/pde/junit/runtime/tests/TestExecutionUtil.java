/*******************************************************************************
 *  Copyright (c) 2019 Julian Honnen
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Julian Honnen <julian.honnen@vector.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.junit.runtime.tests;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.model.ITestRunSession;
import org.eclipse.pde.internal.launching.IPDEConstants;
import org.eclipse.pde.internal.launching.launcher.LaunchListener;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.ui.launcher.JUnitWorkbenchLaunchShortcut;

class TestExecutionUtil {

	public static void waitForAutoBuild() {
		boolean wasInterrupted = false;
		do {
			try {
				Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
				wasInterrupted = false;
			} catch (OperationCanceledException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				wasInterrupted = true;
			}
		} while (wasInterrupted);
	}

	public static ITestRunSession runTest(IJavaElement element) throws CoreException {
		IProject project = element.getResource().getProject();
		project.build(IncrementalProjectBuilder.FULL_BUILD, null);
		List<IMarker> errorMarkers = Arrays.stream(project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE)) //
				.filter(m -> m.getAttribute(IMarker.SEVERITY, 0) == IMarker.SEVERITY_ERROR) //
				.collect(toList());
		assertThat(errorMarkers).as("error markers in " + project).isEmpty();

		TestLaunchShortcut launchShortcut = new TestLaunchShortcut();
		ILaunchConfigurationWorkingCopy launchConfiguration = launchShortcut.createLaunchConfiguration(element);
		launchConfiguration.setAttribute(IPDELauncherConstants.APPLICATION, IPDEConstants.CORE_TEST_APPLICATION);
		setupDirectories(launchConfiguration, element);

		BlockingQueue<ITestRunSession> testResult = new LinkedBlockingDeque<>();
		TestRunListener testRunListener = new TestRunListener() {
			@Override
			public void sessionFinished(ITestRunSession session) {
				testResult.add(session);
			}
		};
		JUnitCore.addTestRunListener(testRunListener);

		try {
			launchAndWaitForTermination(launchConfiguration);
			ITestRunSession result = testResult.poll(30, TimeUnit.SECONDS);
			if (result == null) {
				fail("test was not executed");
			}
			return result;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("test interrupted", e);
		} finally {
			JUnitCore.removeTestRunListener(testRunListener);
		}
	}

	private static void setupDirectories(ILaunchConfigurationWorkingCopy launchConfiguration, IJavaElement element) {
		String dir = element.getJavaProject().getElementName() + "_" + launchConfiguration.getName();
		IPath testLocation = Platform.getLocation().append(dir);

		launchConfiguration.setAttribute(IPDELauncherConstants.LOCATION, testLocation.append("workspace").toOSString());
		launchConfiguration.setAttribute(IPDELauncherConstants.CONFIG_LOCATION,
				testLocation.append("configuration").toOSString());
		launchConfiguration.setAttribute(IPDELauncherConstants.CONFIG_USE_DEFAULT_AREA, false);
	}

	private static void launchAndWaitForTermination(ILaunchConfiguration launchConfiguration) throws CoreException {
		BlockingQueue<ILaunch> terminatedLaunches = new LinkedBlockingDeque<>();
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchesListener2 listener = new ILaunchesListener2() {
			@Override
			public void launchesRemoved(ILaunch[] launches) {
				terminatedLaunches.addAll(Arrays.asList(launches));
			}

			@Override
			public void launchesChanged(ILaunch[] launches) {
			}

			@Override
			public void launchesAdded(ILaunch[] launches) {
			}

			@Override
			public void launchesTerminated(ILaunch[] launches) {
				terminatedLaunches.addAll(Arrays.asList(launches));
			}
		};
		launchManager.addLaunchListener(listener);

		ILaunch launch = DebugUITools.buildAndLaunch(launchConfiguration, ILaunchManager.RUN_MODE,
				new NullProgressMonitor());

		launch.getProcesses()[0].getStreamsProxy().getOutputStreamMonitor().addListener((text, m) -> {
			System.out.println("[test] " + text);
		});
		launch.getProcesses()[0].getStreamsProxy().getErrorStreamMonitor().addListener((text, m) -> {
			System.err.println("[test] " + text);
		});

		try {
			while (true) {
				ILaunch terminatedLaunch = terminatedLaunches.poll(5, TimeUnit.MINUTES);
				if (terminatedLaunch == null) {
					fail("test launch didn't terminate");
				}

				if (launch.equals(terminatedLaunch)) {
					checkExitValueAndDumpLog(launch);
					break;
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("test interrupted", e);
		} finally {
			launchManager.removeLaunchListener(listener);
			if (!launch.isTerminated()) {
				launch.terminate();
			}
		}
	}

	private static void checkExitValueAndDumpLog(ILaunch launch) throws DebugException, CoreException {
		IProcess process = launch.getProcesses()[0];
		int exitValue = process.getExitValue();
		String logFile = readLogFile(launch.getLaunchConfiguration());
		if (exitValue == 13) {
			fail("test application could not start:\n\n" + logFile);
		} else {
			System.out.println(MessageFormat.format(
					"test process terminated with exit value {0}\ncommand line: {1}\nlog file: \n\n{2}", exitValue,
					process.getAttribute(IProcess.ATTR_CMDLINE), logFile));
		}
	}

	private static String readLogFile(ILaunchConfiguration launchConfiguration) throws CoreException {
		File logFile = LaunchListener.getMostRecentLogFile(launchConfiguration);
		if (logFile == null) {
			return "no log file for: " + launchConfiguration;
		}
		try {
			return String.join("\n", Files.readAllLines(logFile.toPath()));
		} catch (IOException e) {
			return "could not read log: " + e.toString();
		}
	}

	private static class TestLaunchShortcut extends JUnitWorkbenchLaunchShortcut {
		@Override
		public ILaunchConfigurationWorkingCopy createLaunchConfiguration(IJavaElement element) throws CoreException {
			return super.createLaunchConfiguration(element);
		}
	}
}
