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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.model.ITestRunSession;
import org.eclipse.pde.internal.launching.IPDEConstants;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.ui.launcher.JUnitWorkbenchLaunchShortcut;

class TestExecutionUtil {

	public static ITestRunSession runTest(IJavaElement element) throws CoreException {
		IProject project = element.getResource().getProject();
		project.build(IncrementalProjectBuilder.FULL_BUILD, null);
		List<IMarker> errorMarkers = Arrays.stream(project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE)) //
				.filter(m -> m.getAttribute(IMarker.SEVERITY, 0) == IMarker.SEVERITY_ERROR) //
				.collect(toList());
		assertThat("error markers in " + project, errorMarkers, is(empty()));

		TestLaunchShortcut launchShortcut = new TestLaunchShortcut();
		ILaunchConfigurationWorkingCopy launchConfiguration = launchShortcut.createLaunchConfiguration(element);
		launchConfiguration.setAttribute(IPDELauncherConstants.APPLICATION, IPDEConstants.CORE_TEST_APPLICATION);

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
			ITestRunSession result = testResult.poll();
			if (result == null) {
				fail("test was not executed");
			}
			return result;
		} finally {
			JUnitCore.removeTestRunListener(testRunListener);
		}
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

		try {
			while (true) {
				ILaunch terminatedLaunch = terminatedLaunches.poll(5, TimeUnit.MINUTES);
				if (terminatedLaunch == null) {
					fail("test launch didn't terminate");
				}

				if (launch.equals(terminatedLaunch)) {
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

	private static class TestLaunchShortcut extends JUnitWorkbenchLaunchShortcut {
		public ILaunchConfigurationWorkingCopy createLaunchConfiguration(IJavaElement element) throws CoreException {
			return super.createLaunchConfiguration(element);
		}
	}
}
